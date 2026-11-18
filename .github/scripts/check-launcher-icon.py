#!/usr/bin/env python3
"""Regression gate: the approved icon must be selected on Android 8–12 and 13+.

Android's mipmap-anydpi-v33 resources override mipmap-anydpi-v26 on API 33+.
This check prevents a stale v33 launcher icon from silently replacing the user's
approved premium broom art on recent devices.
"""

from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
ANDROID = "{http://schemas.android.com/apk/res/android}"

APPROVED_FOREGROUND = "@drawable/omni_approved_adaptive_foreground"
APPROVED_BACKGROUND = "@drawable/omni_premium_icon_background"
APPROVED_BITMAP = "@drawable/omni_approved_icon"


def attr(root: ET.Element, tag: str) -> str | None:
    child = root.find(tag)
    assert child is not None, f"Missing {tag} in {root.tag}"
    return child.get(ANDROID + "drawable")


def main() -> None:
    art = RES / "drawable-nodpi/omni_approved_icon.webp"
    assert art.is_file() and art.stat().st_size > 7000, "Approved image missing or unexpectedly small"

    for qualifier in ("mipmap-anydpi-v26", "mipmap-anydpi-v33"):
        path = RES / qualifier / "ic_launcher.xml"
        root = ET.parse(path).getroot()
        assert root.tag == "adaptive-icon", path
        assert attr(root, "foreground") == APPROVED_FOREGROUND, path
        assert attr(root, "background") == APPROVED_BACKGROUND, path

    fallback = ET.parse(RES / "mipmap-anydpi/ic_launcher.xml").getroot()
    assert fallback.tag == "layer-list"
    assert any(el.get(ANDROID + "drawable") == APPROVED_BITMAP for el in fallback.findall("item")), (
        "Pre-Android-8 launcher does not use the approved art"
    )

    foreground = ET.parse(RES / "drawable/omni_approved_adaptive_foreground.xml").getroot()
    assert foreground.tag == "inset"
    assert foreground.get(ANDROID + "drawable") == APPROVED_BITMAP

    manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
    app = manifest.find("application")
    assert app is not None
    assert app.get(ANDROID + "icon") == "@mipmap/ic_launcher"
    assert app.get(ANDROID + "roundIcon") == "@mipmap/ic_launcher"

    print("PASS: launcher points to approved broom art on API <26, 26–32 and 33+")


if __name__ == "__main__":
    main()
