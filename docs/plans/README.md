# OmniGart v1 phase plans

Standing owner instruction: after Phase 0 and the health-integration PR, continue autonomously through this roadmap. **One phase per feature branch and PR**, stacked on the previous head if still unmerged. Before each phase, keep/update `docs/plans/phase-N.md`, then implement with tests.

## Order

| # | Plan | Branch pattern | Depends on |
|---|------|----------------|------------|
| 0 | Foundation (this PR / Phase 0) | `cursor/phase-0-foundation-*` | — |
| H | Health integration layer | `cursor/health-integration-layer-*` | Phase 0 |
| 1 | [Strength logging](phase-1.md) | `cursor/phase-1-strength-logging-*` | Health |
| 2 | [Food barcode + water](phase-2.md) | `cursor/phase-2-food-water-*` | Phase 1 |
| 3 | [Readiness + morning check](phase-3.md) | `cursor/phase-3-readiness-*` | Phase 2 |
| 4 | [Calculation settings](phase-4.md) | `cursor/phase-4-calc-settings-*` | Phase 3 |
| 5 | [Encrypted backup](phase-5.md) | `cursor/phase-5-encrypted-backup-*` | Phase 4 |
| 6 | [Achievements](phase-6.md) | `cursor/phase-6-achievements-*` | Phase 5 |
| 7 | [Watch apps deepening](phase-7.md) | `cursor/phase-7-watch-apps-*` | Phase 6 |
| 8 | [Running](phase-8.md) | `cursor/phase-8-running-*` | Phase 7 |
| 9 | [Swimming](phase-9.md) | `cursor/phase-9-swimming-*` | Phase 8 |

## Cross-cutting rules (every phase)

- Feature branches only; never commit to `main`. Cloud Agent prefix `cursor/<name>-2ab3`.
- No co-author / AI / Cursor trailers in commits or PR bodies.
- Detailed PR descriptions; no anti-patterns (`!!`, `GlobalScope`, force-unwraps for recoverable state).
- Latest **stable** libraries as of implementation date.
- Never block: log open decisions in `docs/OPEN_QUESTIONS.md`.
- en-GB + uk strings only; never Russian (`checkNoRuLocale`).
- No competitor product names — neutral category language only. Platform APIs OK (HealthKit, Health Connect, Google Health API, Polar BLE SDK).
- **Bundle-first content** — exercises, templates, locales, evidence, icons, body maps, achievements, exercise images ship in `generated/content-bundle/`; apps offline-capable; server only for signed updates / large media / food packs / remote config (`docs/CONTENT_BUNDLE.md`).
- Real content: formulas, explanations, ⓘ evidence links to verified DOI/PMID only; never invent; log unverifiable claims in `docs/OPEN_QUESTIONS.md`.
- Integrate `design/` tokens/icons and `content/exercises` + `shared/planning` (pull from their branches if still unmerged).
