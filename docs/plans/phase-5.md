# Phase 5 — Encrypted backup

## Goal

Client-side encrypted backup/restore to iCloud Drive, Google Drive, and Nextcloud. No OmniGart server.

## In scope

- Export encrypted blob of SQLDelight DB (+ settings); passphrase / key material in Keychain/Keystore.
- Providers: iCloud (Apple), Drive (Android/Google account), Nextcloud (WebDAV).
- Restore flow with version check; fail closed on tamper/MAC failure.
- Tests: crypto round-trip with fakes; provider adapters mocked.

## Dependencies

- Mature schema from Phases 1–4.
