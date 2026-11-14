"""Build launcher and Play icons from the same unmodified logo symbol.

Requires Pillow. Run from any directory: python tools/prepare_icons.py
The launcher retains the original 1200 px canvas (and thus its safe area).
The Play icon is cropped and scaled separately, not copied from the launcher.
"""

from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
MASTER = ROOT / "branding/cronos_logo_without_wordmark.png"
RES = ROOT / "app/src/main/res"
STORE = ROOT / "play-store-assets/2.0.1"


def main():
    logo = Image.open(MASTER).convert("RGB")
    assert logo.size == (1200, 1200), "Keep the original launcher canvas"
    logo.save(RES / "drawable-nodpi/favicon.png", optimize=True)

    for density, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96,
                          "xxhdpi": 144, "xxxhdpi": 192}.items():
        icon = logo.resize((size, size), Image.Resampling.LANCZOS)
        folder = RES / f"mipmap-{density}"
        icon.save(folder / "ic_launcher.png", optimize=True)
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
        rounded = icon.convert("RGBA")
        rounded.putalpha(mask)
        rounded.save(folder / "ic_launcher_round.png", optimize=True)

    # Ignore almost-white antialiasing when finding the visible symbol.
    diff = ImageChops.difference(logo, Image.new("RGB", logo.size, "white"))
    bounds = diff.convert("L").point(lambda p: 255 if p > 25 else 0).getbbox()
    assert bounds is not None
    x0, y0, x1, y1 = bounds
    symbol = logo.crop((x0 - 4, y0 - 4, x1 + 4, y1 + 4))
    symbol.thumbnail((420, 420), Image.Resampling.LANCZOS)
    play = Image.new("RGB", (512, 512), "white")
    play.paste(symbol, ((512 - symbol.width) // 2, (512 - symbol.height) // 2))
    STORE.mkdir(parents=True, exist_ok=True)
    play.convert("RGBA").save(STORE / "icon_512.png", optimize=True)

    # Feature graphic: original symbol, plain typography, no fabricated UI.
    feature = Image.new("RGB", (1024, 500), "#071420")
    panel = play.resize((270, 270), Image.Resampling.LANCZOS)
    feature.paste(panel, (66, 115))
    fonts = Path("C:/Windows/Fonts")
    title = ImageFont.truetype(str(fonts / "segoeuib.ttf"), 72)
    subtitle = ImageFont.truetype(str(fonts / "segoeui.ttf"), 32)
    body = ImageFont.truetype(str(fonts / "segoeui.ttf"), 26)
    draw = ImageDraw.Draw(feature)
    draw.text((394, 120), "Cronos", font=title, fill="#F4EFE5")
    draw.text((397, 223), "L’hora en català", font=subtitle, fill="#3FA9D9")
    draw.text((397, 282), "Rellotge · Ràdio · Despertador", font=body, fill="#F4EFE5")
    feature.save(STORE / "feature_graphic_1024x500.png", optimize=True)
    print(f"Launcher updated without changing its scale; Play assets: {STORE}")


if __name__ == "__main__":
    main()
