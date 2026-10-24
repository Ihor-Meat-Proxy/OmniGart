# Resistance workout calorie model

OmniGart energy figures are **wellness estimates for coaching and Apple Fitness / Health Connect display**. They are **not medical claims**, not clinical calorimetry, and not a substitute for professional advice.

Owner: Ihor.

## Outputs

Every finished strength session writes:

| Quantity | Meaning |
|----------|---------|
| **Active energy** | Above-basal cost of the work (and a small EPOC term) |
| **Basal energy** | Mifflin–St Jeor BMR prorated over the session clock window |

Platform writes (Apple Health / Health Connect) are **append-only**: active and basal samples are inserted once with the workout, never updated or deleted.

## Decision tree

```text
HR samples over active (non-rest) intervals available and mean ≥ 90 bpm?
  YES → Keytel-style HR formula (sex, age, body mass) × active minutes
  NO  → Work-based mechanical proxy from logged sets + modest EPOC
Basal → always BMR × (session hours / 24)
```

Yoga / Pilates / Zone 2 still use the Compendium MET helpers in `WorkoutEnergyEstimate` when no richer model applies.

## Heart-rate path (Keytel)

Reference: Keytel LR et al. Prediction of energy expenditure from heart rate monitoring during submaximal exercise. *J Sports Sci.* 2005.

The published equations return **kJ/min**; OmniGart converts with ÷ 4.184 → kcal/min, then multiplies by **active** seconds only (rest countdown time is excluded).

| Sex | kJ/min |
|-----|--------|
| Male | −55.0969 + 0.6309·HR + 0.1988·mass(kg) + 0.2017·age |
| Female | −20.4022 + 0.4472·HR − 0.1263·mass(kg) + 0.074·age |

Negative instantaneous rates are clamped to zero.

## Work-based path (no usable HR)

For each completed set with ≥ 1 rep:

1. **Relative intensity** ≈ `load / e1RM` (Epley when e1RM is unknown), capped near 1.2.
2. **Load / body mass** = `loadKg / bodyWeightKg`.
3. **Time under tension** ≈ `reps × secondsPerRep` (default 3 s).
4. **RPE** from the logged RPE, or `10 − RIR` when only RIR is present (default ~6).
5. **Muscle-mass factor** by movement family:

| Kind | Factor |
|------|--------|
| Lower-body compound | 1.00 |
| Upper-body compound | 0.85 |
| Lower-body isolation | 0.55 |
| Upper-body isolation | 0.45 |
| Core | 0.35 |
| Unknown | 0.65 |

Warm-up sets are scaled down (~0.35). Drop sets and failure flags add small intensity bumps.

6. Scale by `WORK_KCAL_SCALE` (7.0) with a mild TUT tempo boost.
7. **Set density** = `TUT / (TUT + rest)` soft-scales continuous metabolic cost.
8. **EPOC** ≈ 7% of active work, plus ~1.2% per RPE point above 5 (clamped ~5–18%).

Scaled sum → active kcal. Empty logs with duration only fall back to a light MET floor.

## Basal energy

```text
basal_kcal = MifflinStJeor(mass, height, age, sex) × (duration_hours / 24)
```

When onboarding has already stored a daily BMR, that value may be passed through instead of recomputing Mifflin–St Jeor.

## Implementation

| Piece | Location |
|-------|----------|
| Resistance model | `shared/.../calc/ResistanceWorkoutEnergy.kt` |
| MET fallbacks | `shared/.../calc/WorkoutEnergyEstimate.kt` |
| Unit tests + sanity ranges | `ResistanceWorkoutEnergyTest` |
| Phone / watch / Wear save path | Local store first, then append-only HealthKit / Health Connect |

## Limitations

- Estimates ignore thermic effect of food, ambient temperature, and individual economy.
- Keytel was derived on submaximal aerobic work; applying it to strength intervals is a pragmatic wellness approximation.
- Muscle-mass factors are coarse priors until the equipment / content library supplies richer metadata.
- Always labelled in-product as an estimate (see `workout_summary_energy_estimate`).
