# Changelog

All notable changes to `AutoMailLogin` are documented in this file.

## v0.1.5

### Added

- Added automated regression tests for `VerificationService`, `SecondFactorService`, and `AuthService`.
- Added an in-memory `StorageProvider` test double for fast service-layer testing.
- Added `release-v0.1.5.json` release metadata for GitHub Releases.

### Changed

- Raised the project baseline to `Minecraft 1.21+`.
- Upgraded the `Paper API` dependency to `1.21.4-R0.1-SNAPSHOT`.
- Updated `plugin.yml` `api-version` to `1.21`.
- Updated Chinese and English README files to reflect `1.21+` maintenance status.

### Verified

- Verified successful plugin `load` / `enable` on local `Paper 1.21.4`.
- Verified default `SQLite` initialization, template generation, and mock mail flow.
- Verified admin commands `doctor`, `reload`, and `testsmtp` in a local Paper test server.

### Compatibility

- Supports `Paper 1.21.4` and is maintained for `Minecraft 1.21+`.
- No longer supports `Paper 1.20.6`; older servers reject the plugin because `api-version=1.21`.

## v0.1.4

### Added

- Added admin operations: `reload`, `previewmail`, and `doctor`.
- Added doctor checks for database connectivity, SMTP connectivity, template completeness, and template variable validation.
- Added SMTP limited retry support to improve production stability.

### Changed

- Strengthened outbound mail security with enforced `STARTTLS`, TLS host verification, and SMTP timeout control.
- Improved forgot-password flow to reduce account enumeration risk.
- Restricted mail-based password reset to accounts that have completed email verification.
- Split verification code cooldown and failure lock settings by purpose.

### Verified

- Verified successful build and normal load/enable on local `Paper 1.20.6`.
