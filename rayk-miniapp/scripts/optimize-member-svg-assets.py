"""Resize PNG payloads embedded in the membership SVG wrappers.

The supplied membership illustrations use an SVG file as a stable frontend
interface, but the SVGs contain large base64 PNG payloads.  This utility keeps
the SVG wrapper and its aspect ratio while reducing the raster payload to the
size used by the mini-program cards.
"""

from __future__ import annotations

import argparse
import base64
import io
import re
import shutil
from pathlib import Path

from PIL import Image


PNG_DATA_RE = re.compile(r"data:image/png;base64,([^\"']+)")


def optimize_svg(source: Path, target: Path, hero_size: int, icon_size: int) -> tuple[int, int]:
    text = source.read_text(encoding="utf-8")
    match = PNG_DATA_RE.search(text)
    if match is None:
        target.write_text(text, encoding="utf-8")
        return source.stat().st_size, target.stat().st_size

    image = Image.open(io.BytesIO(base64.b64decode(match.group(1))))
    max_size = hero_size if "hero" in source.stem else icon_size
    image.thumbnail((max_size, max_size), Image.Resampling.LANCZOS)

    encoded_buffer = io.BytesIO()
    image.save(encoded_buffer, format="PNG", optimize=True, compress_level=9)
    encoded = base64.b64encode(encoded_buffer.getvalue()).decode("ascii")
    optimized_text = text[: match.start(1)] + encoded + text[match.end(1) :]
    target.write_text(optimized_text, encoding="utf-8")
    return source.stat().st_size, target.stat().st_size


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--hero-size", type=int, default=384)
    parser.add_argument("--icon-size", type=int, default=256)
    args = parser.parse_args()

    source = args.source.resolve()
    output = args.output.resolve()
    if not source.is_dir():
        raise SystemExit(f"Source directory does not exist: {source}")
    if output.exists():
        raise SystemExit(f"Output directory already exists: {output}")

    total_before = 0
    total_after = 0
    output.mkdir(parents=True)
    for source_file in source.rglob("*"):
        relative = source_file.relative_to(source)
        target_file = output / relative
        if source_file.is_dir():
            target_file.mkdir(parents=True, exist_ok=True)
            continue
        target_file.parent.mkdir(parents=True, exist_ok=True)
        if source_file.suffix.lower() == ".svg":
            before, after = optimize_svg(source_file, target_file, args.hero_size, args.icon_size)
        else:
            shutil.copy2(source_file, target_file)
            before = source_file.stat().st_size
            after = target_file.stat().st_size
        total_before += before
        total_after += after

    print(f"output={output}")
    print(f"before_bytes={total_before}")
    print(f"after_bytes={total_after}")
    print(f"saved_bytes={total_before - total_after}")


if __name__ == "__main__":
    main()
