# Achievements — design, schema, and evaluation

Data-driven achievements for OmniGart live in `content/achievements/` (catalogue + locales) with a pure Kotlin evaluation engine in `shared/achievements` (no app wiring). CI job **Achievements** validates schema, ids, strings, icons, and the rule DSL.

## Healthy-behaviour design

Gamification can support physical-activity participation, but effects are typically modest and mixed; common elements are goals, progress feedback, and rewards ([Mazeas et al., 2022](https://doi.org/10.2196/26779); [Xu et al., 2022](https://doi.org/10.2196/27794); [Nishi et al., 2024](https://doi.org/10.1016/j.eclinm.2024.102798)). Engagement reviews also highlight self-monitoring, feedback, and rewards as recurring behaviour-change techniques ([Milne-Ives et al., 2023](https://doi.org/10.3389/fpsyg.2023.1227443)).

Diet and fitness tracking is cross-sectionally associated with disordered-eating symptomology and related risks; qualitative work describes fixation on numbers, rigid rules, and guilt when “gamified” goals push restriction ([Moody et al., 2025](https://doi.org/10.1002/erv.70006); [Anderberg et al., 2025](https://doi.org/10.1016/j.bodyim.2024.101836); [Eikey, 2021](https://doi.org/10.1192/bjo.2021.1011); [Honary et al., 2019](https://doi.org/10.2196/14239)). OmniGart therefore:

| Rule | Implementation |
| --- | --- |
| Streaks must not push overtraining | `streak_days` with `qualify: activity_or_planned_rest`, weekly `restDayTolerancePerWeek`, and optional freeze tokens (`freezeTokensAllowed`) |
| Planned rest counts | Metric `planned_rest_honoured` and rest-tolerant streaks; achievements explicitly reward honouring readiness-informed rest |
| No extreme dieting / low-calorie rewards | Nutrition achievements use only `food_days_logged` (logging consistency). Catalogue + CI forbid calorie-restriction metrics |
| No dehydration rewards | Water achievements use only `water_goal_days` (personal goal **met**) |
| No weight-loss speed / body-shape targets | Body achievements require `filters.excludeWeightLossRate: true` and reward measurement **consistency**, not shape outcomes |
| Import back-fill is calm | Evaluation `backfill=true` suppresses per-unlock spam and emits one summary notification |

## Schema

Each achievement JSON object includes:

- `id`, `category`, `type`
- `tiers` — bronze/silver/gold/platinum or `level_1`…`level_5` / `unlocked`
- `rule` — declarative DSL (`metric`, `aggregation`, `window`, optional `threshold` / `filters` / `compare`)
- `iconId` — from `design/icons` or `content/achievements/proposed-icons.json`
- `hidden`, `repeatable`
- `titleKey`, `descriptionKey` — locales **en-GB** and **uk** only (no `ru`)

Categories: `strength`, `running`, `swimming`, `yoga`, `pilates`, `nutrition`, `water`, `recovery`, `body`, `general`.

Types: `streak`, `milestone`, `single_session`, `personal_record`, `challenge`, `discovery`.

## Rule DSL (summary)

```text
metric       ∈ workouts_completed | sets_logged | volume_kg | session_duration_min |
               distance_m | pace_sec_per_km | swim_strokes_distinct | yoga_minutes |
               pilates_minutes | food_days_logged | water_goal_days |
               sleep_consistent_days | measurements_logged | measurement_days |
               personal_record_events | disciplines_tried | planned_rest_honoured |
               freeze_tokens_used | challenge_points

aggregation  ∈ count | sum | max | min | distinct | streak | exists

window.kind  ∈ lifetime | rolling_days | calendar | single_event |
               challenge_period | streak_days

streak_days  → qualify ∈ activity | log | goal_met | activity_or_planned_rest
               + optional restDayTolerancePerWeek, freezeTokensAllowed, maxFreezePerStreak
```

Tier thresholds are authoritative when present; `rule.threshold` is used for single-tier / discovery unlocks.

## Evaluation engine (`shared/achievements`)

- Consumes domain events: workout completed, set logged, food / water logged, sleep imported, measurement added, planned rest honoured, freeze token used
- **Idempotent** on `eventId`
- **Timezone-aware** local day keys per event zone (DST-safe via `kotlinx-datetime` / `kotlin.time.Instant`)
- **Revocation** when an event is marked `deleted` (full recompute of affected progress)
- **Back-fill** mode for Health imports → summary unlock count, no notification spam
- Progress toward the next tier (`TierProgress`)

## Catalogue size

See `content/achievements/catalogue.json` for the live count and per-category breakdown (~120).

## Tooling & CI

```bash
cd content/achievements/tools && npm ci && npm run validate
./gradlew :shared:achievements:jvmTest
```

GitHub Actions workflow `.github/workflows/achievements.yml` (job name **Achievements**) runs the Node validator on Node 24 with SHA-pinned actions, then the Gradle test suite.
