# Achievements: behavioural-science rationale

OmniGart achievements reward **consistency and adherence**, not extremes. The catalog and engine encode that policy in data (`healthyByDesign`) and in rule design.

## Core principles

1. **Habit formation over intensity spikes** — Automaticity grows with repetition in stable contexts; median times are on the order of weeks to months, not overnight.
2. **Rest is part of the plan** — Streaks accept planned rest days and limited streak freezes so recovery is not punished.
3. **Logging ≠ restriction** — Nutrition and body-measurement achievements track *whether* you logged, never calorie deficits or kilograms lost.
4. **Hydration at a personal goal** — Water achievements use the user’s own daily goal; there are no rewards for excess intake or for under-drinking.
5. **Recovery adherence is rewarded** — Taking rest on low readiness unlocks recovery achievements; grinding through poor recovery does not.

## Verified references

| Topic | Citation | Identifier |
| --- | --- | --- |
| Habit formation time course | Lally P, van Jaarsveld CHM, Potts HWW, Wardle J. How are habits formed: Modelling habit formation in the real world. *European Journal of Social Psychology*. 2010;40(6):998–1009. | DOI [10.1002/ejsp.674](https://doi.org/10.1002/ejsp.674) |
| Making health habitual (clinical framing) | Gardner B, Lally P, Wardle J. Making health habitual: the psychology of ‘habit-formation’ and general practice. *British Journal of General Practice*. 2012;62(605):664–666. | DOI [10.3399/bjgp12X659466](https://doi.org/10.3399/bjgp12X659466) |
| Habit and health behaviour review | Gardner B. A review and analysis of the use of ‘habit’ in understanding, predicting and influencing health-related behaviour. *Health Psychology Review*. 2015;9(3):277–295. | DOI [10.1080/17437199.2013.876238](https://doi.org/10.1080/17437199.2013.876238) |
| Exercise habit strength | Phillips LA, Gardner B. Habitual exercise and physical activity as predictors of health: the role of habit strength. (See Gardner 2015 synthesis; exercise-habit measures summarised therein.) | DOI [10.1080/17437199.2013.876238](https://doi.org/10.1080/17437199.2013.876238) |
| Adult physical activity quantity/quality | Garber CE, et al. ACSM position stand: Quantity and quality of exercise. *Med Sci Sports Exerc*. 2011;43(7):1334–1359. | DOI [10.1249/MSS.0b013e318213fefb](https://doi.org/10.1249/MSS.0b013e318213fefb) |
| Resistance training overview | ACSM. Resistance training for healthy adults (overview of reviews lineage used elsewhere in OmniGart). | DOI [10.1249/MSS.0000000000003897](https://doi.org/10.1249/MSS.0000000000003897) |
| Overreaching / recovery caution | Meeusen R, et al. Prevention, diagnosis and treatment of the overtraining syndrome: joint consensus statement. *Med Sci Sports Exerc*. 2013;45(1):186–205. | DOI [10.1249/MSS.0b013e318279a10a](https://doi.org/10.1249/MSS.0b013e318279a10a) |

### How Lally et al. 2010 informs thresholds

Lally and colleagues modelled daily habit performance in everyday life and reported a **median of 66 days** to reach automaticity (range roughly 18–254 days). OmniGart therefore uses **66-day** streak windows (`consistency.streak.66`, `nutrition.days.66`) as *aspirational habit windows*, not as medical guarantees. Shorter bronze/silver streaks (7 / 21 days) scaffold early wins without implying that habit is “done.”

## Explicit non-goals (anti-patterns we refuse to encode)

- Rewards for severe energy restriction, fasting streaks, or “lowest calorie day.”
- Rewards for rapid body-mass reduction or BMI drops.
- Rewards for training when readiness is flagged low (the opposite is rewarded).
- Social leaderboards, public shaming streaks, or competitive dehydration.

## Challenges

Personal challenges are **local-only** time boxes (e.g. 30-day mobility, 100 km month). They reuse the same healthy metrics and never add social comparison loops.
