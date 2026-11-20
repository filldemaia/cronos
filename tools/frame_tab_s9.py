"""Frame the selected tablet screenshots with the front of a Galaxy Tab S9. Requires Pillow."""

from pathlib import Path
from shutil import copyfile

from PIL import Image, ImageDraw, ImageOps

ROOT = Path(__file__).resolve().parents[1]
CAPTURES = ROOT / "docs/captures"
SOURCE = CAPTURES / "tablet"
FRAMED = CAPTURES / "tab-s9"
FASTLANE = ROOT / "fastlane/metadata/android/en-US/images/tabletScreenshots"
PLAY = ROOT / "play-store-assets/2.0.1"
SHOTS = (
    "01_rellotge.png",
    "02_radio.png",
    "03_alarmes.png",
    "04_reproduccio.png",
)

SIZE = (2340, 1540)
# 254.3 × 165.8 mm chassis; centred camera on the long edge and thin bezels.
SCREEN = (91, 98, 2249, 1442)


def frame_tab(capture):
    landscape = capture.width > capture.height
    canvas = Image.new("RGBA", SIZE)
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((1730, 17, 1920, 31), radius=5, fill="#56595d")
    draw.rounded_rectangle((1961, 17, 2057, 31), radius=5, fill="#56595d")
    draw.rounded_rectangle(
        (25, 24, 2314, 1516), radius=96, fill="#414347", outline="#74777b", width=4
    )
    draw.rounded_rectangle((33, 32, 2306, 1508), radius=89, fill="#131518")
    draw.ellipse((1159, 50, 1181, 72), fill="#080b0e")
    draw.ellipse((1167, 57, 1173, 63), fill="#354154")

    x0, y0, x1, y1 = SCREEN
    w, h = x1 - x0, y1 - y0
    if not landscape:
        canvas = canvas.transpose(Image.Transpose.ROTATE_270)
        x0, y0, w, h = SIZE[1] - y1, x0, h, w
    fitted = ImageOps.contain(capture, (w, h), Image.Resampling.LANCZOS)
    screen = Image.new("RGB", (w, h), fitted.getpixel((0, 0)))
    xpos, ypos = (w - fitted.width) // 2, (h - fitted.height) // 2
    screen.paste(fitted, (xpos, ypos))
    if ypos + fitted.height < h:
        screen.paste(fitted.getpixel((0, fitted.height - 1)), (0, ypos + fitted.height, w, h))
    mask = Image.new("L", (w, h))
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, h - 1), radius=32, fill=255)
    canvas.paste(screen, (x0, y0), mask)
    return canvas


def main():
    for directory in (FRAMED, FASTLANE, PLAY / "tablet", PLAY / "tablet-sense-marc"):
        directory.mkdir(parents=True, exist_ok=True)
    for number, name in enumerate(SHOTS, 1):
        original = SOURCE / name
        if not original.is_file():
            raise SystemExit(f"Falta la captura de tauleta: {original}")
        with Image.open(original) as image:
            capture = image.convert("RGB")
        if number == 1:
            reference = capture
            reference_file = original
        elif (capture.width > capture.height) != (reference.width > reference.height):
            raise SystemExit(f"Orientació diferent a {original} respecte a {reference_file}")

        frame = frame_tab(capture)
        out = FRAMED / name
        frame.save(out, optimize=True)
        (FASTLANE / f"{number}.png").write_bytes(out.read_bytes())
        copyfile(original, PLAY / "tablet-sense-marc" / name)

        dims = (1920, 1080) if capture.width > capture.height else (1080, 1920)
        artwork = ImageOps.contain(frame, tuple(d - 48 for d in dims), Image.Resampling.LANCZOS)
        store = Image.new("RGB", dims, "#f9f4eb")
        center = ((store.width - artwork.width) // 2, (store.height - artwork.height) // 2)
        store.paste(artwork, center, artwork)
        store.save(PLAY / "tablet" / name, optimize=True)
        print(f"Galaxy Tab S9: {name}")


if __name__ == "__main__":
    main()
