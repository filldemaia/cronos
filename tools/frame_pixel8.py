"""Frame the selected screenshots with the front of a Pixel 8. Requires Pillow."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parents[1]
CAPTURES = ROOT / "docs/captures"
FRAMED = CAPTURES / "pixel-8"
FASTLANE = ROOT / "fastlane/metadata/android/en-US/images/phoneScreenshots"
PLAY = ROOT / "play-store-assets/2.0.1"
SHOTS = (
    ("rellotge.png", "01_rellotge.png"),
    ("radio.png", "02_radio.png"),
    ("reproduccio.png", "04_reproduccio.png"),
    ("alarmes-programades.png", "03_alarmes.png"),
)

SCALE = 2
SIZE = (744, 1540)
# 70.8 × 150.5 mm chassis; centred punch-hole and right-side controls.
SCREEN = (39, 36, 705, 1504)


def at(values):
    return tuple(SCALE * v for v in values)


def pixel8(capture):
    canvas = Image.new("RGBA", at(SIZE))
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle(at((722, 292, 731, 368)), radius=3 * SCALE, fill="#45484c")
    draw.rounded_rectangle(at((722, 410, 731, 550)), radius=3 * SCALE, fill="#45484c")
    draw.rounded_rectangle(
        at((18, 17, 725, 1522)), radius=77 * SCALE,
        fill="#3b3e41", outline="#6b6d70", width=2 * SCALE,
    )
    draw.rounded_rectangle(at((24, 23, 719, 1516)), radius=72 * SCALE, fill="#101114")

    x0, y0, x1, y1 = SCREEN
    size = at((x1 - x0, y1 - y0))
    pixels = ImageOps.contain(capture, size, Image.Resampling.LANCZOS)
    assert pixels.width == size[0] and pixels.height < size[1]
    y = (size[1] - pixels.height) // 2 + 4 * SCALE
    screen = Image.new("RGB", size, pixels.getpixel((0, 0)))
    screen.paste(pixels, (0, y))
    screen.paste(pixels.getpixel((0, pixels.height - 1)), (0, y + pixels.height, *size))
    mask = Image.new("L", size)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, size[0] - 1, size[1] - 1), radius=58 * SCALE, fill=255,
    )
    canvas.paste(screen, at((x0, y0)), mask)

    # Front camera sits above the captured app content.
    cx = (x0 + x1) // 2
    assert y >= 32 * SCALE
    draw.ellipse(at((cx - 9, y0 + 12, cx + 9, y0 + 30)), fill="#07080a")
    draw.ellipse(at((cx - 3, y0 + 18, cx + 3, y0 + 24)), fill="#252d40")
    return canvas.resize(SIZE, Image.Resampling.LANCZOS)


def main():
    for directory in (FRAMED, FASTLANE, PLAY / "phone", PLAY / "phone-sense-marc"):
        directory.mkdir(parents=True, exist_ok=True)
    for number, (name, play_name) in enumerate(SHOTS, 1):
        with Image.open(CAPTURES / name) as original:
            capture = original.convert("RGB")
        frame = pixel8(capture)
        out = FRAMED / name
        frame.save(out, optimize=True)
        (FASTLANE / f"{number}.png").write_bytes(out.read_bytes())

        store = Image.new("RGB", (1080, 1920), "#f9f4eb")
        device = ImageOps.contain(frame, (1024, 1884), Image.Resampling.LANCZOS)
        center = ((store.width - device.width) // 2, (store.height - device.height) // 2)
        store.paste(device, center, device)
        store.save(PLAY / "phone" / play_name, optimize=True)
        capture.save(PLAY / "phone-sense-marc" / play_name, optimize=True)
        print(f"Pixel 8: {name}")


if __name__ == "__main__":
    main()
