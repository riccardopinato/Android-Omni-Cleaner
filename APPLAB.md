# AppLab integration

Pinned Step 3 baseline:

- repository: `riccardopinato/AppLab`
- version family: **v4.5 Physical Evidence Hub**
- SHA: `dc826264c31dd79fc7617ab7a4a8aee61f9b5aba`

Static Intelligence runs on `main`, `recovery/**`, `audit/**` and PRs.

The parity runtime lane uses a controlled storage fixture and
`.maestro/parity-storage.yaml`.

Authority remains `NON_TRUSTED_SMOKE`; a bounded emulator PASS is not physical
validation.

`applab.project.json` declares real-device validation required for storage,
file picker/SAF and Billing. A target-authored adapter is intent, not evidence.

Trusted AppLab artifact-bound verification and Physical Evidence Hub remain
blocked while the GitHub Actions artifact quota is saturated.
