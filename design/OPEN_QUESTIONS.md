# Open questions & decisions log

Autopilot log while the owner is away. Decisions below are provisional defaults so work never blocks; revisit anytime.

## Decisions (provisional)

| ID | Topic | Decision | Rationale |
|----|-------|----------|-----------|
| D1 | Token tooling | Style Dictionary **5.7.0** + DTCG JSON | Latest stable (2026-10-16); W3C `$type`/`$value` |
| D2 | iOS icon delivery | SVG in asset catalog, **Preserve Vector Data** + template rendering | One SVG source for all platforms; SF Symbols templates deferred until hierarchical/variable colour is required |
| D3 | PNG rasteriser | `@resvg/resvg-js@2.6.2` + `sharp@0.35.5` | Latest **stable** resvg-js (2.7.x still alpha); sharp latest stable |
| D4 | Accent | Teal `#0F766E` | Distinct from common fitness oranges; works on light canvas |
| D5 | HR zone palette | Okabe–Ito-inspired Z1–Z5 | Colourblind-safe; Z3 yellow needs dark labels |
| D6 | Readiness Rest colour | Violet | Separates “rest day” from HR zones and danger red |
| D7 | Scope boundary | Only `design/` + generators; no app projects | Merges beside Phase 0 scaffold PR |
| D8 | Quick-log placement | FAB / control above tab bar, trailing/thumb-biased | One-hand principle |
| D9 | Custom font | System SF Pro / Roboto until brand type ships | Tokens already name OmniGart Sans/Display as aliases |
| D10 | Logo metaphor | Tempered ring + grit cut + spark | Encodes гарт (temper/grit) at small sizes |

## Open questions

| ID | Question | Impact | Default if unanswered |
|----|----------|--------|------------------------|
| Q1 | Final brand typeface? | Marketing + in-app display | System fonts via token aliases |
| Q2 | Tab order: Food first vs Workouts first? | Onboarding & habit loops | Food → Workouts → Recovery → Planner → Profile |
| Q3 | Quick-log: centre FAB vs trailing thumb cluster? | Reachability vs Material guidance | Trailing/thumb-biased control + sheet |
| Q4 | watchOS: independent app vs thin companion? | Scope of watch IA | Companion-first with log set / water / readiness |
| Q5 | Evidence ⓘ deep-link target (papers, in-app explainers)? | Recovery claims UX | In-app explainer sheet stub |
| Q6 | Android icon switch: activity-alias vs third-party launcher APIs? | Picker reliability | activity-alias only |
| Q7 | Should Z3 yellow be darkened for glass captions? | AA on Liquid Glass | Keep Okabe–Ito fill; force dark label token |
| Q8 | Name of Compose multiplatform module for shared icons? | KMP layout | `com.omnigart.design.icons` |
| Q9 | Do we need RTL-mirrored discipline icons? | i18n | Not for v1 line icons (mostly symmetrical) |
| Q10 | App Store / Play listing screenshots style alignment with this DS? | Store presence | Follow light-first DS when Phase 1 marketing lands |

## Follow-ups (non-blocking)

- Wire generated catalogs into Phase 0 Xcode / Gradle modules once that PR merges.
- Optional SF Symbols template export if iOS needs hierarchical layers.
- Contrast audit automation (axe / WCAG contrast CI) against glass samples.
- Wear OS tile mockups beyond phone wireframes.
