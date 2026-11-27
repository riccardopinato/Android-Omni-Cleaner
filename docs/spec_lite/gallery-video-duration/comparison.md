# Existing workflow vs Spec Lite — Omni duration bug

**Baseline:** unit tests already validate `videoTimelineTimestamp(Int)` for long playback; `GalleryMediaItem.formattedDuration` had a separate inconsistent manual formatter without a dedicated one-hour edge-case regression. Source formula on main gives `62:03` for `3723000ms`.

**With FSL:** one explicit FR, four ACs, existing source-of-truth references and source SHA, 4 new JVM tests, shared formatter refactor, unchanged media permission/deletion behavior.

**Measured gate:** record exact-head Gradle CI pass/failure and job duration, code/test diff. Baseline development time, LLM tokens and device FPS are NOT_MEASURED. No claim that sample FSL metadata validates genuine user approval.
