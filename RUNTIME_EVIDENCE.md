# Runtime evidence lanes

## Trusted AppLab runtime

Status: **BLOCKED by GitHub Actions artifact quota**.

This is the authoritative AppLab runtime/certification boundary because build and runtime evidence are bound through immutable artifact identity.

Do not replace it with a weaker lane.

## AppLab Runtime Smoke

Status: **available without artifact upload**.

The repository builds the debug APK and executes the pinned AppLab verifier in one GitHub Actions job on an Android emulator.

Coverage includes:
- APK installation and launch;
- target-process fatal/ANR checks;
- bounded Maestro flow;
- safe interaction crawling;
- AppLab visual runtime checks;
- System UI/permission checks;
- controlled storage fixture.

Authority: `NON_TRUSTED_SMOKE`.

This lane is valid evidence that the reconstructed app launches and survives bounded runtime interaction. It does **not** establish byte-bound trusted runtime identity, donor equivalence, physical-device behavior or store certification.
