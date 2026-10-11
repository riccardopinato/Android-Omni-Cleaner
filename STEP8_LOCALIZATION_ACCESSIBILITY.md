# MAXI STEP 8 — Localization & Accessibility

Factory baseline: `a4a97ca79e85d2faebcdcc9a3ae4a9644f5caf03`  
App baseline entering Step 8: `04fe134826cbd2f1dce70ffbc97d7a754f7329bd`

## Goal

Make Android Omni Cleaner usable across the supported consumer locales and robust for TalkBack, larger font scales and narrow layouts without changing the cleaner safety model.

## Localization

Supported locales:
- English (`en`)
- Italian (`it`)
- Spanish (`es`)
- French (`fr`)
- Portuguese (`pt`)

Rules:
- English remains the resource fallback.
- All five bundles must expose the same string-key set and format placeholders.
- Manual selector supports System / EN / IT / ES / FR / PT.
- System mode follows Android/device configuration.
- The selected manual locale is persisted synchronously so it can be applied in `attachBaseContext` without adding a heavy localization dependency.
- Android `localeConfig` advertises the supported locales to the platform.

## Accessibility

Implemented direction:
- selection rows use a single toggleable semantic target with Checkbox role;
- explicit buttons remain available for Gallery keep/trash so gestures are not the only interaction;
- decorative glyphs are removed from the TalkBack semantics tree;
- storage progress exposes progress semantics;
- fixed action rows that could clip with larger fonts are replaced or made horizontally scrollable;
- text-heavy selection rows allow additional lines instead of one-line truncation.

## Safety invariants

Localization/accessibility changes do not alter:
- storage permission policy;
- cleanup snapshot revalidation;
- duplicate keeper protection;
- Android Trash / Safe Trash semantics;
- billing ownership rules;
- ad-request policy.

## Version

- `0.9.0-recovery`
- versionCode `9`

## Exit gate

Step 8 is COMPLETE only when:
1. EN/IT/ES/FR/PT key parity is exact;
2. format-placeholder parity is exact;
3. Android CI passes lint, JVM tests, debug and release builds;
4. AppLab Intelligence passes;
5. AppLab runtime + Maestro passes;
6. visual journey is PASS/WARN with no high-confidence visual failure;
7. exact-head Step 8 APK prerelease + SHA-256 sidecar is generated.

Physical TalkBack/OEM validation remains part of final physical release certification and is not inferred from emulator evidence.
