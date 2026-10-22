# Exercise libraries & plan builders: market patterns

Research snapshot for OmniGart Phase 0 planning content (Oct 2026). Observational comparison of common product patterns only — do not copy exercise text, media, or proprietary program IP. Competitor product names are intentionally omitted (owner rule: use neutral descriptions).

Sources consulted: public marketing/help materials for strength-logging apps and wearable coaching platforms (exercise libraries, routines, guided trainers, daily suggested workouts, adaptive coach plans, workout catalogs).

---

## Strength-logging apps — common strengths

| Area | Observation |
| --- | --- |
| Exercise library | Hundreds of movements; search + filters for **equipment** and **muscle**; demo animation; setup/execution steps; long-term lift stats (e1RM, best set/session volume). |
| Custom exercises | User can add name, equipment, primary/secondary muscles, tracking type (weight×reps, bodyweight reps, duration); duplicate + pinned notes. |
| Routines / templates | Reusable routines; explore libraries with programs filtered by level / goal / equipment; categories such as home, travel, dumbbell-only, bands, HIIT, gym, bodyweight, suspension. |
| Live logging | Warm-ups, drop sets, failure flags, supersets, rest timers, RPE; previous-performance prompts. |
| Guided planning | Goal-based programs, progressive-overload suggestions, suggested weights/reps from history (often behind a paid tier). |
| Social / export | Feeds, follows, and sync to popular activity networks. |

### Gaps (relative to a multi-discipline, evidence-first product)

- Strength-centric library; yoga / pilates / swim / run technique content is thin or absent as first-class disciplines.
- Filters stop at equipment + muscle — no **movement pattern** (squat/hinge/push/pull/…), mechanics, unilateral flag, or evidence trail.
- Exercise detail is instructional + personal stats, not contraindications, regressions/progressions graph, or cited sources.
- Programs are often static imports the user then edits; guided trainers can be opaque (little “why this set/week”).
- Limited readiness / health-signal day adjustment compared with wearable coaches.
- Equipment model is often per-exercise tags, not a durable **“my equipment” profile** with presets that filter the whole library and templates.

---

## Wearable coaching platforms — common strengths

| Area | Observation |
| --- | --- |
| Exercise / workout library | Large catalogs (public materials often cite **1,500+** exercises) spanning strength, yoga, pilates, cardio, HIIT, swim, run, bike; watch animations for many moves. |
| Custom workouts | Warm-up / main / cooldown structure; download to watch; filter find-a-workout by activity type. |
| Daily suggested sessions | Run/bike suggestions driven by training status, load & load focus, VO₂max, recovery, sleep, recent sessions, HR / LT / FTP; requires recent synced activities; scheduled coach plans usually take priority. |
| Adaptive coach plans | Adaptive run / cycle / strength / triathlon plans; event date + distance; day-to-day adaptation from performance, recovery, health metrics; plan overview + weekly schedule in the companion app. |
| Wearable loop | Readiness/energy, sleep, stress, and HR zones tightly coupled to suggestions — strong **closed-loop** coaching UX. |

### Gaps

- Strength programming is often secondary to endurance coaching; volume landmarks, RIR, and hypertrophy periodisation are not first-class explainer surfaces.
- Library depth is animation/watch-oriented; evidence citations, contraindications, and progression graphs are not prominent.
- Equipment ownership filtering and home/travel gym presets are weak compared with dedicated logging apps.
- Adaptive logic is proprietary and hard to audit; users rarely see a citation-backed “why”.
- Multi-discipline periodisation (e.g. strength + swim technique + yoga mobility in one explainable plan) is not the product’s centre of gravity.
- Open ecosystem / self-hostable AI assist is typically out of scope for closed vendor stacks.

---

## What OmniGart should do differently

1. **Evidence-native exercise objects** — every entry carries i18n name keys, pattern, muscles, equipment, level, unilateral, mechanics, tracking type, technique/setup/breathing/mistakes/safety/contraindications, linked regressions/progressions, media placeholders, and **verified DOI/PMID refs** (or an explicit open question if none).
2. **Discipline parity** — strength, yoga, pilates, swimming, running as first-class filters and plan inputs, not bolted-on workout types.
3. **“My equipment” profile** — home gym / commercial gym / travel / bodyweight / pool presets that drive library search and template eligibility.
4. **Desired workouts + templates** — starter templates with **rationale + refs**, not anonymous program packs.
5. **Deterministic, explainable planning first** — rule engine outputs recommendations each with a `why` + evidence id; volume/frequency/RIR/deload/polarised-run rules documented in `docs/PLANNING_ENGINE.md`.
6. **AI as proposer, rules as safety** — later OpenRouter path: opt-in, minimised payload, `data_collection: deny` + ZDR + EU in-region URL when required; AI JSON validated/clamped by the rule engine; always fall back to rules.
7. **Honest caveats** — e.g. ~10% weekly mileage “rule” has weak evidence; polarised 80/20 is descriptive of many elites, not a universal law — surface this in plan explanations.
8. **Open content** — AGPL-licensed original copy; never scrape third-party libraries or media.

---

## Benchmark takeaway for this PR

Ship a **self-contained content pack + planning package** that merges beside Phase 0 scaffold/design-system PRs: schemas, ~120 original exercises, search/filter + equipment presets, ~15 templates, rule-based plan generator with interfaces for a future OpenRouter generator.
