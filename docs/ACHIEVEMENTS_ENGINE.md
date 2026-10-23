# Achievements engine

Pure-Kotlin module: `shared/achievements`. Content: `content/achievements/`. No app wiring in this PR.

## Design

```
DomainEvent ──► MetricExtractor ──► MetricContribution(s)
                                        │
                                        ▼
                              AchievementEngine.ingest
                                        │
                    ┌───────────────────┼───────────────────┐
                    ▼                   ▼                   ▼
              counter/window         streak(+rest/freeze)  threshold / PR / challenge
                    │                   │                   │
                    └────────────► AchievementStore ◄───────┘
                                        │
                                        ▼
                         ProgressSnapshot (+ next tier in family)
```

### Idempotency

Every event carries a stable `eventId`. The store records processed ids; retries and Health re-imports are no-ops.

### Day boundaries (DST & travel)

Calendar identity is `LocalDate` in the **event’s** IANA `timeZoneId` via kotlinx-datetime. Midnight and DST spring-forward / fall-back are handled by the zone rules — we never use a fixed UTC offset for “days.”

When the user travels, new events should carry the **current** zone. Historical events keep the zone they were logged with, so a workout logged in `Europe/Kyiv` stays on that local day even if the phone later moves to `America/New_York`.

### Streaks

- Consecutive local dates with a matching contribution.
- `PlannedRestDay` continues the streak when `streakPolicy.allowPlannedRest` is true.
- `StreakFreeze` continues the streak within `maxFreezeDaysPer30Days`.
- Out-of-order live events older than the tip are ignored; `backfill` sorts by `occurredAt` first.

### Backfill

`backfill(events)` replays history with `notify = false` and returns `batchSummaryCount` so the UI shows one summary (“You unlocked N achievements from imported history”) instead of N toasts.

### Tier progress

Achievements share a `familyId`. After unlock, `ProgressSnapshot.nextInFamily` reports progress toward the next metal/level in that family.

### Challenges

`ChallengeEngine` instantiates local-only templates (`localOnly: true`, `social: false`). Completing a challenge emits `DomainEvent.ChallengeCompleted` for achievement rules of kind `challenge`.

## Healthy-by-design

Catalog flag object `healthyByDesign` is schema-enforced (`const: true` on safety flags). Banned metrics are rejected by `AchievementContentValidator`. See `content/achievements/BEHAVIOURAL_SCIENCE.md`.
