# Insights metrics (wellness estimates)

OmniGart Insights metrics are **wellness coaching estimates**, not medical diagnoses,
treatments, or clinical advice. Formulas below document how the app derives numbers
shown on the Insights and Health tabs.

Owner: Ihor.

## Training load (session score)

Session training load is computed in priority order:

1. **Heart-rate TRIMP-style score** (preferred when HR samples exist)
2. **Calorie model** via `CalorieEnergyProvider` (interface; stub until the logging /
   calorie-model PR lands)
3. **Duration × session RPE** (Foster-style internal load)

### 1. HR zone TRIMP (Edwards-style weights)

Using resting HR (`HR_rest`) and max HR (`HR_max`) from onboarding (or profile
defaults when missing):

\[
HRR = \frac{HR - HR_{rest}}{HR_{max} - HR_{rest}}
\]

Zones (fraction of heart-rate reserve):

| Zone | HRR range | Weight \(w\) |
|------|-----------|--------------|
| Z1   | &lt; 0.60   | 1 |
| Z2   | 0.60–0.70 | 2 |
| Z3   | 0.70–0.80 | 3 |
| Z4   | 0.80–0.90 | 4 |
| Z5   | ≥ 0.90    | 5 |

For each sample interval of \(t\) minutes spent in a zone:

\[
\mathrm{TRIMP} = \sum_i w_i \cdot t_i
\]

### 2. Calorie fallback

When HR is unavailable, load ≈ `activeKcal / 10` from `CalorieEnergyProvider`
(or workout `activeEnergyKcal` when already stored). This is a coarse proxy only.

### 3. Duration × RPE

When neither HR nor calories are available:

\[
\mathrm{load} = \mathrm{duration\_minutes} \times \mathrm{session\_RPE}
\]

Default session RPE is 5 when the user has not logged one.

## Fitness / Fatigue / Form (Banister EWMA)

Daily load \(L_d\) is the sum of session loads that day (0 on rest days).

- **Fitness (CTL)** — 42-day exponentially weighted moving average of daily load
- **Fatigue (ATL)** — 7-day EWMA of daily load
- **Form (TSB)** — \(CTL - ATL\)

EWMA update (time constant \(\tau\) days):

\[
S_d = S_{d-1} + \frac{L_d - S_{d-1}}{\tau}
\]

with \(\tau_{CTL} = 42\), \(\tau_{ATL} = 7\).

**Confidence band** on the Fitness chart is ± one standard deviation of recent
daily residuals around CTL (wellness visualisation only).

**Projected decay** extends CTL/ATL forward with \(L_d = 0\) (rest), so the curves
decay toward zero — a planning sketch, not a prediction of readiness.

## Training load ratio (ATL / CTL)

\[
\mathrm{ratio} = \frac{ATL}{CTL}
\]

Coloured scale with a soft “sweet spot” band around **0.8–1.3** (acute:chronic
style guide). Outside that band the UI shows a short wellness explanation — not
an injury diagnosis.

## Training load focus (last 4 weeks)

Session minutes (or load shares) are bucketed from HRR when available:

- **Anaerobic** — Z4 + Z5
- **High aerobic** — Z3
- **Low aerobic** — Z1 + Z2

Without HR, duration is treated as low aerobic.

## Health tab metrics

Health cards read **Apple Health / Health Connect** samples as **read-only**.
OmniGart never writes or deletes platform health records from these tabs.

| Card | Source types (examples) | Notes |
|------|-------------------------|-------|
| Active / resting energy | Active energy, basal energy | Cumulative from 00:00 vs average |
| Exercise minutes / steps | Exercise time, step count | Platform ring / minutes when present |
| Resting HR / HRV | Resting HR, HRV SDNN / RMSSD | Wellness display |
| HRV stress index | Baevsky-style index from RR or HRV | **Estimate**; clearly labelled |
| Recovery HRV | Morning / overnight HRV trend | Wellness estimate |
| Blood pressure, SpO₂, respiratory rate, wrist temperature | Matching quantity types | 7-day charts where available |
| Dietary energy / macros | Nutrition records from the writing app | Read-only |
| Sleep | Sleep analysis / sessions | Donut: asleep vs awake |
| Mindful minutes | Mindful session | Read-only |

**Baevsky-style stress index (estimate):** when RR intervals are available,
\(SI \approx AMo / (2 \cdot Mo \cdot MxDMn)\) with mode Mo, amplitude AMo, and
variation range MxDMn. When only RMSSD/SDNN is available, a bounded proxy maps
lower HRV toward a higher index. Always shown as a wellness estimate.

## Caching and performance

Aggregates (daily loads, CTL/ATL series, period totals) are computed off the
main thread and cached in `InsightsAggregateCache` keyed by profile + day range
+ workout revision. Cache invalidates when the local health store workout list
changes.

## Related code

- `com.omnigart.shared.insights.*` — formulas and dashboard builders
- `com.omnigart.shared.health.store.LocalHealthStore` — workout source of truth
- `CalorieEnergyProvider` — calorie-model seam (stub until parallel PR merges)
