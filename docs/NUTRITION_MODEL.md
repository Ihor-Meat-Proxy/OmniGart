# OmniGart adaptive calorie and macro target model

Status: product contract for `shared/nutrition` (`com.omnigart.nutrition`).  
Wording is **wellness only** (“estimated daily energy”, “suggested target”, “not medical or dietary advice”).  
On first goal set: *If you have a health condition, are pregnant or under 18, check with a healthcare professional.*

Diary storage, barcodes, label scan, and food DB live in a separate food-logging surface. This model exposes a small **read** interface for daily intake totals/targets (`DailyIntakeRead` / `DailyTargetRead`).

---

## BMR selection and fallbacks

| Condition | Method | Notes |
| --- | --- | --- |
| Default (weight, height, age) | Mifflin–St Jeor | Men: `10w + 6.25h − 5a + 5`; women: `… − 161` |
| Trusted body fat % | Katch–McArdle | `370 + 21.6 × LBM` |
| Trusted body fat % + athletic | Cunningham | `500 + 22 × LBM` |
| Height or age missing | Henry/Oxford weight-only | Labelled **rough estimate** |
| No weight | — | **No target** |
| Sex unspecified | Average of male and female equations | |

Sources: [Mifflin 1990](https://pubmed.ncbi.nlm.nih.gov/2305711/), [Katch–McArdle](https://pubmed.ncbi.nlm.nih.gov/7191215/), [Cunningham](https://pubmed.ncbi.nlm.nih.gov/15883556/), [Henry/Oxford / FAO](https://www.fao.org/4/y5686e/y5686e00.htm).

---

## Activity (never both)

1. **Measured path:** 7-day mean active energy × **0.80** (adjustable **0.75–0.85**) + TEF **10%** of mean intake.  
2. **Otherwise PAL from steps:** `<5k → 1.2`, `5–7.5k → 1.375`, `7.5–10k → 1.55`, `>10k → 1.725`.  

Never combine measured active energy with PAL. Never add workout kcal on top of daily active energy (dedupe sources). Optional **exercise credit** on the *target* is separate (see below).

---

## Adaptive / energy-balance TDEE

### Classic EWMA form

`adaptiveTDEE = meanIntake − EWMA(α=0.1) weightSlope(kg/day) × 7700 kcal/kg`

Needs ≥14 days span, ≥10 weigh-ins, ≥80% of days fully logged. Blend weight = `days/28` (capped at 1). Change the published target by at most **100 kcal/week**. Ignore empty and incomplete days.

### Energy-balance expenditure (21-day window)

`expenditure ≈ meanIntake − Δstored / days`

Stored energy uses a **variable fat/lean share** by rate of change (slower loss or faster gain → more fat):

| Rate (kg/week) | Fat share |
| --- | --- |
| ≤ −1.0 | 0.65 |
| ≤ −0.5 | 0.75 |
| < 0 | 0.85 |
| < 0.25 | 0.70 |
| < 0.5 | 0.80 |
| else | 0.90 |

`kcal/kg = fatShare×7700 + (1−fatShare)×1800`.

Sources: [Hall et al.](https://pubmed.ncbi.nlm.nih.gov/21872751/) family of energy-balance work; classic 7700 kcal/kg heuristic ([Wishnofsky](https://pubmed.ncbi.nlm.nih.gov/16277825/)).

---

## Data sufficiency

- Food logged on at least **4 of 7** days and at least **1 weigh-in** per week.  
- Skip **empty** days; mark partly logged days **incomplete** and exclude (or ask in weekly check-in).  
- When insufficient: **pause updates** and keep the last estimate; show a **logging break** notice when neither food nor weight was logged.

---

## Goals, taper, and weekly rate

- Loss default **0.5%/wk**, max **1%/wk**; gain **0.25–0.5%/wk**.  
- Weekly-rate energy: **1100 kcal/day per kg/week**.  
- **Taper:** linear from full deficit/surplus at **5 kg** from target weight down to **0** at **1 kg**.  
- Auto-raise suggested target if loss **>1%/wk** for **2** weeks.  
- Exercise calories added to the target (opt-in): count only a fraction (**~0.72** default), scaled lower for higher body fat.

---

## Guardrails

- Floor = `max(BMR, 1200 women / 1500 men)`; deficit ≤ **500 kcal/day**.  
- No deficits under **18**, during pregnancy/breastfeeding, or BMI **&lt; 18.5**.  
- Explicit **check-and-warn** helper for the floor (`Guardrails.checkFloor`).  
- If intake &lt; floor for **7+** days: supportive screen (Beat, NEDA, local helplines) and option to hide calories.  
- **Red** colour only for safety states; otherwise neutral, no-shame wellness wording.

Sources: [NICE / clinical energy floors literature](https://pubmed.ncbi.nlm.nih.gov/23631843/), [AND position](https://pubmed.ncbi.nlm.nih.gov/24222017/).

---

## Target modes

| Mode | Behaviour |
| --- | --- |
| **Guided** | Automatic adaptive target with guardrails |
| **Flexible weekly** | User splits a weekly budget across days; **guardrails stay ON** |
| **Manual** | User sets daily energy; floor check still warns |

Per-weekday targets support **training vs rest** days (training bonus redistributed so the weekly mean is preserved).

---

## Macros and water

- Protein: **1.6 g/kg** maintain; **1.6–2.0** gain; **1.8–2.2** lose (goal or reference weight if obese).  
- Fat ≥ **20%** energy and ≥ **0.5 g/kg**. Carbs = remainder, ≥ **130 g**.  
- Fibre **14 g/1000 kcal** (25 g minimum). Saturated fat and free sugars **&lt;10%** (guidance copy).  
- Water: **2.0 L** women / **2.5 L** men + exercise extra (body weight factor).

Sources: [ISSN protein](https://link.springer.com/article/10.1186/s12970-017-0177-8), [EFSA water](https://doi.org/10.2903/j.efsa.2010.1459), [EFSA carbs](https://doi.org/10.2903/j.efsa.2010.1462), [WHO healthy diet](https://www.who.int/news-room/fact-sheets/detail/healthy-diet), [IOM DRIs](https://nap.nationalacademies.org/catalog/10490).

---

## Body inputs and Health sync

Stored locally: weight; waist, hips, chest, arm, thigh; body fat %.  
**Waist-to-height ratio** is shown and **not** used in the energy formula.  

Read **weight, body fat %, height** from HealthKit / Health Connect (**read-only** for imports).  
User weigh-ins written to the platform are **append-only** (exist-check sync id / `clientRecordId`, insert only; never delete or update). See `docs/HEALTH_DATA_MAPPING.md` and `scripts/check_no_health_platform_mutate.sh`.

---

## Reminders and weekly check-in

Local notifications only.

- Optional morning weigh-in; weekly trend; nudge after **3 days** without a weigh-in.  
- Measurements every **2–4 weeks**.  
- Recompute weekly, or after a **2 kg** change, or a goal change; notify when the target changes.  
- **Pause reminders**.  
- Confidence: **rough / good / personalised**.  
- Configurable **start-of-day** time (default 04:00; Profile → Energy goal).  
- Opt-in **partial exercise credit** (~0.72, scaled by body fat) and **target weight** (drives the 5 kg→1 kg taper).  
- **Weekly check-in** on a user-chosen weekday with skippable sections: incomplete days, weigh-in prompt, fasting vs unlogged, proposed target change (accept/decline).  
- Food-tab target card shows suggested energy, **water goal**, confidence, and change reasons (red only for floor/safety).

---

## Confidence

| Level | Typical signal |
| --- | --- |
| Rough | Missing inputs or weight-only / Henry BMR |
| Good | Several logged days and weigh-ins |
| Personalised | Adaptive or 21-day energy-balance eligible with rich logging |

---

## Source list (approved research)

- https://pubmed.ncbi.nlm.nih.gov/15883556/  
- https://pubmed.ncbi.nlm.nih.gov/23631843/  
- https://pubmed.ncbi.nlm.nih.gov/2305711/  
- https://pubmed.ncbi.nlm.nih.gov/7191215/  
- https://pubmed.ncbi.nlm.nih.gov/16277825/  
- https://www.fao.org/4/y5686e/y5686e00.htm  
- https://pmc.ncbi.nlm.nih.gov/articles/PMC3880593/  
- https://pubmed.ncbi.nlm.nih.gov/17848938/  
- https://pmc.ncbi.nlm.nih.gov/articles/PMC5491979/  
- https://link.springer.com/article/10.1186/s12970-017-0177-8  
- https://pmc.ncbi.nlm.nih.gov/articles/PMC4033492/  
- https://pubmed.ncbi.nlm.nih.gov/26891166/  
- https://nap.nationalacademies.org/catalog/10490  
- https://doi.org/10.2903/j.efsa.2010.1459  
- https://doi.org/10.2903/j.efsa.2010.1462  
- https://www.who.int/news-room/fact-sheets/detail/healthy-diet  
- https://pubmed.ncbi.nlm.nih.gov/24222017/  
- https://pubmed.ncbi.nlm.nih.gov/25521523/  
- https://pubmed.ncbi.nlm.nih.gov/21185970/  
