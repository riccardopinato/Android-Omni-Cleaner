#!/usr/bin/env python3
"""Detect non-static, chromatic video-region pixels in genuine Android screenshots.

No Pillow needed: FFmpeg decodes the PNGs to a fixed-size RGB sample. This is
an emulator visual heuristic, not proof of OEM codecs, A/V sync or battery.
"""
from __future__ import annotations
import subprocess
import sys
from pathlib import Path

WIDTH, HEIGHT = 180, 320


def sample(path: Path) -> bytes:
    if not path.is_file() or path.stat().st_size < 1024:
        raise ValueError(f"missing Android screenshot: {path}")
    cmd = [
        "ffmpeg", "-hide_banner", "-loglevel", "error", "-nostdin",
        "-i", str(path), "-vf", f"scale={WIDTH}:{HEIGHT},format=rgb24",
        "-frames:v", "1", "-f", "rawvideo", "-pix_fmt", "rgb24", "pipe:1",
    ]
    raw = subprocess.check_output(cmd)
    if len(raw) != WIDTH * HEIGHT * 3:
        raise ValueError(f"wrong RGB sample length for {path.name}")
    return raw


def check_motion(first: Path, later: Path, label: str) -> None:
    a, b = sample(first), sample(later)
    changed, colorful, luminous = 0, 0, 0
    # Restrict to the central media viewport; avoid status bar, nav buttons
    # and notification clock which could otherwise fake motion.
    for y in range(int(HEIGHT * 0.20), int(HEIGHT * 0.80)):
        for x in range(int(WIDTH * 0.15), int(WIDTH * 0.85)):
            i = (y * WIDTH + x) * 3
            r, g, blue = b[i], b[i + 1], b[i + 2]
            if max(r, g, blue) - min(r, g, blue) >= 40 and max(r, g, blue) >= 95:
                colorful += 1
            if (r + g + blue) >= 310:
                luminous += 1
            if max(abs(a[i + k] - b[i + k]) for k in range(3)) >= 24:
                changed += 1
    print(f"VIDEO_PIXELS_{label}: changed={changed} colorful={colorful} luminous={luminous}")
    if colorful < 200 or luminous < 250 or changed < 180:
        raise ValueError(f"VIDEO_MOTION_NOT_VERIFIED_{label}: moving/colorful media pixels missing")


def main(paths: list[str]) -> int:
    if len(paths) != 4:
        print("Expected four genuine screenshot paths", file=sys.stderr)
        return 2
    try:
        check_motion(Path(paths[0]), Path(paths[1]), "INLINE")
        check_motion(Path(paths[2]), Path(paths[3]), "FULLSCREEN")
    except (ValueError, subprocess.CalledProcessError) as exc:
        print(str(exc), file=sys.stderr)
        return 2
    print("PASS: inline and fullscreen video screenshot pixels are chromatic and moving")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
