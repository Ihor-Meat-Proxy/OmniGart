# OmniGart planning engine

Design + first implementation notes for the rule-based planner and the later OpenRouter-assisted path. Implementation lives in `shared/planning` (plain Kotlin / KMP-ready; no app wiring in this PR).

---

## Goals

- **Deterministic & explainable**: every recommendation carries a `why` string and an evidence reference id.
- **Safe by default**: injuries, equipment, time, and readiness clamp volume/intensity before a session is emitted.
- **AI later, rules always**: OpenRouter may propose; the rule engine validates and clamps; rules are the fallback.

---

## Inputs (`UserTrainingProfile`)

| Field | Use |
| --- | --- |
| Sex, age, height, body mass | Anthropometrics for relative load heuristics and medical caution flags (not diagnosis). |
| Level per discipline | Self-selected or tested; selects template family and RIR/progression mode. |
| Goals | Hypertrophy / strength / endurance / mobility / race_prep / general_fitness. |
| Available days & session minutes | Frequency and session trimming. |
| Equipment set | Filters exercises/templates (`EquipmentProfile` presets: home_gym, commercial_gym, travel, bodyweight, pool). |
| Injuries / contraindications | Removes or regresses matching movements. |
| HR zones | Endurance intensity distribution targets. |
| Recent load / readiness | Optional Health-derived signals → daily volume/intensity multipliers. |

---

## Rule-based engine (shipped)

### Volume landmarks (resistance)

- Weekly hard-set targets per major muscle are guided by the graded dose–response in Schoenfeld et al. 2016 (higher weekly sets → greater hypertrophy on average; evidence `schoenfeld_volume_2016`) and the fractional-set meta-regressions in Pelland et al. 2025/26 (volume helps with diminishing returns; evidence `pelland_rt_dose_2025`).
- Practical default bands used by the engine (explainable constants, not medical orders):
  - Beginner hypertrophy: **~8–12** hard sets / muscle / week
  - Intermediate: **~10–16**
  - Advanced ceiling caution: **~16–20** before prioritising recovery
- ACSM overview of reviews for RT prescription in healthy adults: evidence `acsm_resistance_training_2026`.
- Garber et al. ACSM quantity/quality guidance: evidence `garber_acsm_quantity_2011`.

### Frequency

- Prefer **≥2 exposures / muscle / week** when volume allows (Schoenfeld et al. 2016 frequency MA favoured ≥2/week vs 1/week when not volume-equated; evidence `schoenfeld_frequency_2016`).
- On a **volume-equated** basis, frequency effects on hypertrophy are small/null (Schoenfeld, Grgic & Krieger 2019; evidence `schoenfeld_frequency_2019`) — the engine therefore uses frequency mainly to **spread volume**, not as a magic lever.
- Pelland et al.: frequency effects clearer for **strength** than hypertrophy under fractional counting.

### Intensity / RIR

| Goal | Working-set RIR band | Notes |
| --- | --- | --- |
| Hypertrophy | 1–3 RIR | Most sets near but not always to failure. |
| Strength | 2–4 RIR on accessories; 1–3 on main lifts | Heavier loads, longer rests. |
| General fitness | 2–4 RIR | Prefer adherence. |
| Deload week | 3–5 RIR | Volume × ~0.5–0.7. |

### Progression

1. **Novice linear**: add reps or small load each session while RIR stays in band.
2. **Double progression**: stay in a rep range (e.g. 8–12); when top of range hit at target RIR, increase load and drop reps.
3. **RIR autoregulation**: if last set RIR > band, nudge load up next time; if RIR < 0 / failure unintended, nudge down.

### Deload

- Every **4–8** accumulated hard weeks, or when readiness is low for ≥3 sessions: cut hard-set volume ~30–50%, keep some intensity practice, raise RIR.

### Running / endurance distribution

- Polarised (~80% easy / ~20% hard) mirrors descriptive elite patterns (Seiler 2010; evidence `seiler_intensity_distribution_2010`) and can work in recreational runners (Muñoz et al.; evidence `munoz_polarized_runners_2014`), but is **not** proven universally superior to all other TIDs.
- **~10% weekly mileage increase**: popular coaching heuristic with **weak trial evidence**; the engine treats it as a soft ceiling with an honest caveat in the `why`, preferring readiness and injury flags over a fixed percent.

### Readiness daily adjustment

| Readiness | Multiplier |
| --- | --- |
| High | 1.0–1.05 volume |
| Normal | 1.0 |
| Low | 0.7–0.85 volume, +1 RIR |
| Very low | Swap to mobility / technique or rest |

### Output shape

```text
PlanRecommendation
  sessions[]
  targets (sets/muscle/week, intensity distribution)
  explanations[] → { code, message, evidenceRefId }
```

`RuleBasedPlanGenerator` is pure and side-effect free.

---

## AI-assisted path (interfaces only; later release)

### Privacy design (verified against OpenRouter docs, Oct 2026)

- **Opt-in** with explicit consent copy; default = rules only.
- **Minimised / pseudonymised payload**: age band, level, goals, equipment enums, injury tags, recent load summary — no raw GPS, no free-text medical notes, no name/email.
- API key: **user-supplied** or **app-managed** vault; never log the key.
- Provider routing on each request:
  - `provider.data_collection = "deny"` (exclude providers that may store/train on prompts)
  - `provider.zdr = true` when Zero Data Retention is required
  - For EU residency of prompts/completions: Business/Enterprise **in-region** base URL `https://eu.openrouter.ai` (routes only to shared EU/EEA/UK/CH endpoints; account/billing metadata may still be US — document honestly)
- Docs: [Provider routing](https://openrouter.ai/docs/guides/routing/provider-selection), [ZDR](https://openrouter.ai/docs/guides/features/zdr), [In-region routing](https://openrouter.ai/docs/guides/features/in-region-routing), [Data collection](https://openrouter.ai/docs/guides/privacy/data-collection).

### Models (initial allow-list; re-check pricing at ship)

Prefer providers that advertise no-training + ZDR endpoints (exact slugs change — resolve at runtime via OpenRouter model APIs). Suggested evaluation set: mid-tier instruction models with JSON mode / structured outputs; avoid free-tier endpoints that permit training unless the user explicitly opts in.

### Structured output

- AI returns JSON matching `AiPlanProposal` schema (sessions, sets, RIR, notes).
- `PlanSafetyValidator` checks: equipment, contraindications, volume ceilings, RIR floors, weekly frequency, deload rules.
- Out-of-bounds fields are **clamped** or the proposal is rejected → `RuleBasedPlanGenerator` fallback.

### Prompts (sketch)

System: “You are a planning assistant for OmniGart. Propose one week of training as JSON. Do not invent exercises outside the provided id list. Respect injuries and equipment. Cite rule codes from the supplied catalogue in each session note.”

User: pseudonymised profile + allowed exercise ids + last week summary + rule catalogue excerpt.

### Cost (order-of-magnitude)

Assume ~2–4k input tokens + ~1–2k output tokens per weekly plan refresh. At typical mid-tier list prices this is low single-digit US cents per refresh; cache rule catalogue; refresh on schedule change, not every app open.

### Kotlin surfaces

```kotlin
interface PlanGenerator {
    fun generate(input: PlanGenerationInput): PlanRecommendation
}

class RuleBasedPlanGenerator(...) : PlanGenerator
class OpenRouterPlanGenerator(...) : PlanGenerator  // stub: throws UnsupportedOperationException / returns rules fallback — no network in this PR
```

---

## Testing

- Unit tests for volume targets, equipment filtering integration, deload triggers, readiness multipliers, explanation evidence ids, and safety clamping of a mock AI proposal.
- No network calls in CI.
