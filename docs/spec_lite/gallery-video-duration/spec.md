# Bug fix specification — Omni Gallery duration

**FR-001:** A media duration beyond one hour must be rendered with hours rather than counting total minutes, with one shared formatter for Gallery and the native scrubber.

- **AC-001:** A 3723000 millisecond video displays 01:02:03 rather than the original 62:03 label.
- **AC-002:** Subhour videos retain MM:SS with the same labels already used in gallery previews.
- **AC-003:** Long durations beyond the maximum signed Int milliseconds remain correctly represented without overflow.
- **AC-004:** Existing Int-based scrubber time labels remain source and regression compatible.

**Source baseline:** `e2959f4680e1c3c6c40720d72d1c1262287c1925`. **Factory:** `f8b9f550c544735237f19cdd723bd2df667ceee6`. Status PROPOSED / NOT_VERIFIED until independently verified by the actual PR-head CI. This document does not approve signing or Store release.
