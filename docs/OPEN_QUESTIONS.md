# Open questions (autopilot log)

Owner is away — decisions below are provisional. Do not block merges on these.

## Content & citations

1. **Exercise-specific EMG/biomechanics refs** — Many movements currently cite NSCA essentials + ACSM RT overview rather than movement-specific trials. Prefer adding verified meta-analyses/EMG papers per pattern family in a later pass; omit rather than invent DOIs.
2. **Yoga / pilates evidence bar** — Pose/exercise descriptions are technique-accurate but mostly backed by ACSM flexibility/neuromotor guidance, not pose-specific RCTs. Should we require discipline-specific systematic reviews before marking a claim “supported”?
3. **Swim stroke coaching cues** — Competing coach systems (e.g. different kick timings). Confirm preferred cue dialect for freestyle catch and breaststroke kick legality notes.
4. **Ukrainian terminology** — Confirm preferred sports terms (e.g. присід vs сідати, станова тяга variants) with a native-speaking coach; current strings are working drafts.
5. **“Russian twist” naming** — English exercise name retained; no `ru` locale. Rename to “seated rotation” for neutrality?

## Schema & product

6. **Media pipeline** — `media.*Key` placeholders assume a future CDN layout. Confirm key scheme with design-system PR.
7. **SQLDelight mapping** — This PR ships JSON + Kotlin models only. Confirm table shapes when Phase 0 scaffold lands.
8. **Equipment model** — Is `none` vs `bodyweight` redundant for filters? Presets currently treat both as always available.
9. **Tracking types** — Need swim-specific (laps × distance × rest) richer than `laps`/`distance`?
10. **Contraindications severity** — Free-text tags now; structured ICD-ish codes later?

## Planning engine

11. **Sex as input** — Included for future absolute strength norms / relative load heuristics; unused in v1 rules beyond profile plumbing. Keep, gate, or remove?
12. **Health/readiness sources** — Apple Health / Health Connect field mapping TBD; readiness is an enum stub.
13. **Deload every 6 weeks** — Heuristic; confirm with owner preference vs readiness-only deloads.
14. **AI model allow-list** — Re-verify OpenRouter ZDR / no-training endpoints at ship time; slugs churn.
15. **EU in-region** — `eu.openrouter.ai` needs Business/Enterprise; confirm billing plan before enabling in production.
16. **Concurrent training interference** — Lift+run template exists; engine does not yet model interference quantitatively.

## Process

17. **Target library size** — Shipped ~129 exercises (≈89 strength). Grow toward Hevy-scale later or keep curated & cited?
18. **Template localisation** — Session names use i18n keys; confirm UK copy review workflow.

## Sensors / Polar (this PR)

19. **H10 offline RR payload shape** — SDK API exposes `SampleType.RR` (interval ignored). Product docs emphasise HR @ 1 s and “only one recording”. Confirm on a real H10 whether RR offline returns RR intervals vs derived HR, and how timestamps are encoded in `PolarExerciseData`.
20. **Artefact filter thresholds** — Shared module uses a Lipponen & Tarvainen (2019)-inspired relative median filter (default 20 %, RR 300–2000 ms). Confirm Kubios-equivalent preset (soft/medium/strong) for readiness vs workout HRV.
21. **Baevsky SI units in product UI** — Implemented as AMo% / (2·Mo_s·MxDMn_s) with 50 ms bins. Confirm display rounding and whether we expose Mo/AMo/MxDMn alongside SI.
22. **Swim merge conflict policy** — Prefer strap within 750 ms of a watch sample. Confirm window and whether optical watch HR should ever win (e.g. strap contact loss).
23. **Health write schema** — `HeartRateSink` writes bpm samples only. Should RR / HRV metrics also land in HealthKit / Health Connect in v1?
24. **Android Polar API surface** — Adapter assumes SDK 8.4.0 coroutine/Flow methods (`startRecording`, `listExercises`, …). Confirm exact suspend vs Flow return types when compiling against the published AAR after AGP lands.
25. **Verity Sense swim path** — Stubbed. Prefer H10 for pool ECG; enable Verity Sense `PolarOfflineRecordingApi` only if owner wants optical swim HR.
26. **PR #2 wiring** — `ios/Packages/Sensors` and `android/sensors` are standalone. Confirm XcodeGen package path and `:android:sensors` include once Phase 0 merges.
