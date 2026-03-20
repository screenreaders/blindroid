import os
import random
from pathlib import Path

import numpy as np
from PIL import Image, ImageEnhance, ImageFilter, ImageDraw

RAW_DIR = Path(os.environ.get("PLN_DATA_DIR", "data/nbp_pln_raw"))
AUG_DIR = Path(os.environ.get("PLN_DATA_AUG", "data/nbp_pln_aug"))
VARIANTS_PER_IMAGE = int(os.environ.get("PLN_AUG_VARIANTS", "12"))
SEED = int(os.environ.get("PLN_AUG_SEED", "42"))

random.seed(SEED)
np.random.seed(SEED)


def random_crop(img: Image.Image) -> Image.Image:
    w, h = img.size
    scale = random.uniform(0.75, 1.0)
    nw, nh = int(w * scale), int(h * scale)
    left = random.randint(0, max(0, w - nw))
    top = random.randint(0, max(0, h - nh))
    cropped = img.crop((left, top, left + nw, top + nh))
    return cropped.resize((w, h), Image.BICUBIC)


def random_rotate(img: Image.Image) -> Image.Image:
    angle = random.uniform(-6.0, 6.0)
    return img.rotate(angle, resample=Image.BICUBIC, expand=False, fillcolor=(0, 0, 0))


def random_color(img: Image.Image) -> Image.Image:
    bright = ImageEnhance.Brightness(img)
    img = bright.enhance(random.uniform(0.7, 1.3))
    contrast = ImageEnhance.Contrast(img)
    img = contrast.enhance(random.uniform(0.8, 1.2))
    color = ImageEnhance.Color(img)
    img = color.enhance(random.uniform(0.8, 1.2))
    return img


def random_blur(img: Image.Image) -> Image.Image:
    if random.random() < 0.35:
        radius = random.uniform(0.2, 1.2)
        img = img.filter(ImageFilter.GaussianBlur(radius=radius))
    return img


def random_noise(img: Image.Image) -> Image.Image:
    if random.random() < 0.5:
        arr = np.array(img).astype(np.float32)
        noise = np.random.normal(0.0, random.uniform(3.0, 12.0), arr.shape)
        arr = np.clip(arr + noise, 0, 255).astype(np.uint8)
        img = Image.fromarray(arr)
    return img


def random_occlusion(img: Image.Image) -> Image.Image:
    if random.random() < 0.35:
        w, h = img.size
        occl_w = int(w * random.uniform(0.08, 0.2))
        occl_h = int(h * random.uniform(0.08, 0.2))
        left = random.randint(0, max(0, w - occl_w))
        top = random.randint(0, max(0, h - occl_h))
        draw = ImageDraw.Draw(img)
        shade = random.randint(0, 40)
        draw.rectangle(
            (left, top, left + occl_w, top + occl_h),
            fill=(shade, shade, shade),
        )
    return img


def augment_image(img: Image.Image) -> Image.Image:
    img = random_crop(img)
    img = random_rotate(img)
    img = random_color(img)
    img = random_blur(img)
    img = random_noise(img)
    img = random_occlusion(img)
    return img


def main() -> None:
    if not RAW_DIR.exists():
        raise SystemExit(f"Missing input directory: {RAW_DIR}")

    AUG_DIR.mkdir(parents=True, exist_ok=True)

    total = 0
    for denom_dir in RAW_DIR.iterdir():
        if not denom_dir.is_dir():
            continue
        out_dir = AUG_DIR / denom_dir.name
        out_dir.mkdir(parents=True, exist_ok=True)

        images = [p for p in denom_dir.iterdir() if p.suffix.lower() in {".jpg", ".jpeg", ".png"}]
        for img_path in images:
            try:
                base = Image.open(img_path).convert("RGB")
            except Exception as exc:
                print(f"Skip {img_path}: {exc}")
                continue

            for idx in range(VARIANTS_PER_IMAGE):
                aug = augment_image(base.copy())
                name = f"{img_path.stem}_aug_{idx:02d}.jpg"
                out_path = out_dir / name
                aug.save(out_path, "JPEG", quality=90)
                total += 1

    print(f"Generated {total} augmented images in {AUG_DIR}")


if __name__ == "__main__":
    main()
