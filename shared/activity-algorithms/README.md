# shared/activity-algorithms

Pure Kotlin Multiplatform activity math: running pace/splits/cadence, HR
time-in-zone, swim accelerometer stroke/lap detection + SWOLF, optional running
power, and selectable VO₂max estimators (Uth–Sørensen, Cooper, ACSM running).

Evidence citations live in `evidence/Citations.kt`. No platform SDKs.

```bash
./gradlew :shared:activity-algorithms:jvmTest
```

See `docs/activity/ALGORITHMS.md` and OPEN_QUESTIONS **§ Activity algorithms** (A1…).
