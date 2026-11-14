"""Inspect the packaged AAB (not a Gradle source/merged manifest).

Uses the AAPT Resources.proto wire format for XmlNode/XmlElement/XmlAttribute.
No third-party Python dependencies. Run: python tools/verify_aab.py <file.aab>
"""

import hashlib
import json
from pathlib import Path
import sys
from zipfile import ZipFile


def varint(data, offset):
    result = shift = 0
    while True:
        byte = data[offset]
        offset += 1
        result |= (byte & 127) << shift
        if byte < 128:
            return result, offset
        shift += 7
        if shift > 63:
            raise ValueError("Invalid protobuf varint")


def fields(data):
    offset = 0
    while offset < len(data):
        tag, offset = varint(data, offset)
        number, wire = tag >> 3, tag & 7
        if wire == 0:
            value, offset = varint(data, offset)
        elif wire == 2:
            size, offset = varint(data, offset)
            value = data[offset:offset + size]
            offset += size
        elif wire in (1, 5):
            size = 8 if wire == 1 else 4
            value = data[offset:offset + size]
            offset += size
        else:
            raise ValueError(f"Unsupported protobuf wire type {wire}")
        yield number, value


def attribute(data):
    attr = dict(fields(data))
    name = attr[2].decode("utf-8")
    if attr.get(3):
        return name, attr[3].decode("utf-8")
    if 6 in attr:
        item = dict(fields(attr[6]))
        for string_field in (2, 3):
            if string_field in item:
                return name, dict(fields(item[string_field]))[1].decode("utf-8")
        if 7 in item:
            primitive = dict(fields(item[7]))
            for scalar_field in (6, 7, 8):
                if scalar_field in primitive:
                    return name, primitive[scalar_field]
    return name, None


def elements(node):
    element = dict(fields(node)).get(1)
    if element is None:
        return
    parts = list(fields(element))
    name = next(value.decode("utf-8") for number, value in parts if number == 3)
    attrs = dict(attribute(value) for number, value in parts if number == 4)
    yield name, attrs
    for number, value in parts:
        if number == 5:
            yield from elements(value)


def inspect(path):
    path = Path(path)
    with ZipFile(path) as bundle:
        manifest = list(elements(bundle.read("base/manifest/AndroidManifest.xml")))
        root = manifest[0][1]
        assert root["package"] == "com.filldemaia.cronos", root
        assert str(root["versionCode"]) == "10", root
        assert root["versionName"] == "2.0.1", root
        app = next(attrs for name, attrs in manifest if name == "application")
        assert str(app.get("debuggable", 0)).lower() in ("0", "false")
        dex = [entry for entry in bundle.infolist() if entry.filename.endswith(".dex")]
        r8_files = [name for name in bundle.namelist() if name.endswith("/r8.json")]
        assert r8_files, "No packaged R8 metadata"
        maps = [name for name in bundle.namelist() if name.endswith("/proguard.map")]
        assert maps, "No packaged deobfuscation mapping"
        metadata = {name: json.loads(bundle.read(name)) for name in r8_files}
        scores = {}
        for data in metadata.values():
            options = data["options"]
            for option in ("isObfuscationEnabled", "isOptimizationsEnabled", "isShrinkingEnabled"):
                assert options[option], f"R8 option disabled: {option}"
            for key, value in data["stats"].items():
                if key.startswith("no") and key.endswith("Percentage"):
                    name = key[2:-10]
                    scores[name] = round(100 - value, 2)
                    assert scores[name] >= 25, f"Below Play threshold: {name}"
        # Match metadata to the actual DEX bytes, not just the build flags.
        actual_dex = {hashlib.sha256(bundle.read(entry)).hexdigest() for entry in dex}
        recorded_dex = {entry["checksum"] for data in metadata.values() for entry in data["dexFiles"]}
        assert recorded_dex <= actual_dex, "R8 checksums do not match packaged DEX"
        report = {
            "artifact": str(path),
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "bundleBytes": path.stat().st_size,
            "package": root["package"],
            "versionName": root["versionName"],
            "versionCode": root["versionCode"],
            "uncompressedDexBytes": sum(entry.file_size for entry in dex),
            "mappingFiles": maps,
            "r8Percentages": scores,
            "r8DexChecksumsMatch": True,
            "r8Metadata": metadata,
        }
        return report


if __name__ == "__main__":
    print(json.dumps(inspect(sys.argv[1]), ensure_ascii=False, indent=2))
