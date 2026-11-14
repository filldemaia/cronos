"""Capture the real optimized test app, never the user's Play installation.

Run after installing releaseTest: python tools/capture_store_screens.py [serial]
Requires Pillow. The raw screenshots stay under app/build; store images are
fitted (not stretched) into a 9:16 canvas if the phone is taller. Only system
status/navigation bars are cropped, using their reported Android insets.
"""

from io import BytesIO
from pathlib import Path
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ADB = Path(os.environ["LOCALAPPDATA"]) / "Android/Sdk/platform-tools/adb.exe"
PACKAGE = "com.filldemaia.cronos.r8test"
REMOTE_DUMP = "/data/local/tmp/cronos-r8test-ui.xml"
OUT = ROOT / "play-store-assets/2.0.1/phone"
RAW = ROOT / "app/build/release-verification/screenshots"


def main():
    serial = sys.argv[1] if len(sys.argv) > 1 else None
    if serial is None:
        devices = subprocess.check_output([str(ADB), "devices"], text=True)
        ready = [line.split()[0] for line in devices.splitlines() if "\tdevice" in line]
        if len(ready) != 1:
            raise SystemExit("Connect and authorize exactly one phone, or pass its serial.")
        serial = ready[0]
    base = [str(ADB), "-s", serial]

    def adb(*args):
        return subprocess.check_output([*base, *args])

    def foreground():
        activities = adb("shell", "dumpsys", "activity", "activities").decode("utf-8", "replace")
        resumed = [line for line in activities.splitlines() if "ResumedActivity" in line]
        if not any(PACKAGE + "/" in line for line in resumed):
            raise RuntimeError("Test app is not in front; unlock the phone and reopen it.")

    def tree():
        foreground()
        adb("shell", "uiautomator", "dump", REMOTE_DUMP)
        root = ET.fromstring(adb("exec-out", "cat", REMOTE_DUMP))
        if not any(node.get("package") == PACKAGE for node in root.iter("node")):
            raise RuntimeError("The test app is covered by another screen; close it before capturing.")
        return root

    def tap(node):
        x0, y0, x1, y1 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
        adb("shell", "input", "tap", str((x0 + x1) // 2), str((y0 + y1) // 2))

    def without_system_bars(image):
        display = adb("shell", "dumpsys", "window", "displays").decode("utf-8", "replace")
        top, bottom = 0, image.height
        for kind, x0, y0, x1, y1 in re.findall(
            r"type=(statusBars|navigationBars) frame=\[(\d+),(\d+)\]\[(\d+),(\d+)\]", display
        ):
            x0, y0, x1, y1 = map(int, (x0, y0, x1, y1))
            if x0 != 0 or x1 != image.width:
                continue
            if kind == "statusBars" and y0 == 0 and y1 < image.height // 8:
                top = max(top, y1)
            if kind == "navigationBars" and y1 == image.height and y0 > image.height * 7 // 8:
                bottom = min(bottom, y0)
        return image.crop((0, top, image.width, bottom))

    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/com.filldemaia.cronos.MainActivity")
    first = tree()
    skip = next((node for node in first.iter("node") if node.get("text") == "Salta"), None)
    if skip is not None:
        tap(skip)  # Initial setup of the separate test installation only.

    OUT.mkdir(parents=True, exist_ok=True)
    RAW.mkdir(parents=True, exist_ok=True)
    tabs = [("Rellotge", "01_rellotge"), ("Ràdio", "02_radio"),
            ("Alarmes", "03_alarmes"), ("Configuració", "04_configuracio")]
    for label, filename in tabs:
        candidates = [node for node in tree().iter("node") if node.get("text") == label]
        if not candidates:
            raise RuntimeError(f"Tab not found: {label}; no screenshots fabricated.")
        # The navigation item is the lowest occurrence, not the screen title.
        tab = max(candidates, key=lambda node: int(re.findall(r"\d+", node.get("bounds"))[1]))
        tap(tab)
        time.sleep(0.7)  # Allow the actual UI's transition/layout to finish.
        tree()
        raw = adb("exec-out", "screencap", "-p")
        (RAW / f"{filename}.png").write_bytes(raw)
        image = Image.open(BytesIO(raw)).convert("RGB")
        image = without_system_bars(image)
        image.thumbnail((1440, 2560), Image.Resampling.LANCZOS)
        canvas = Image.new("RGB", (1440, 2560), "#071420")
        canvas.paste(image, ((1440 - image.width) // 2, (2560 - image.height) // 2))
        canvas.save(OUT / f"{filename}.png", optimize=True)
        print(f"Captured real {label}: {OUT / (filename + '.png')}")


if __name__ == "__main__":
    main()
