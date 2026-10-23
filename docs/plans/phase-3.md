# Phase 3 — Readiness card + morning check

## Goal

Morning readiness surface using ln rMSSD baseline, resting HR, sleep, and training load, plus a short morning check flow.

## In scope

- Readiness score card on home / recovery tab (design readiness colours).
- Inputs from health layer: HRV (rMSSD), resting HR, sleep stages/duration, recent load.
- Baseline: rolling ln(rMSSD) with SWC framing (Phase 0 readiness heuristic + refinements).
- Morning check: subjective markers + auto-filled objective metrics; persist result.
- ⓘ evidence links for HRV/SWC and sleep–load citations (verified only).
- Tests with fake health samples and deterministic baselines.

## Dependencies

- Health integration (reads); Phase 0 calc/readiness code.
