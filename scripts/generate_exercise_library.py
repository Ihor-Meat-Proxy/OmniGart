#!/usr/bin/env python3
"""Generate OmniGart original exercise JSON + locales + templates. Run from repo root."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "content/exercises/data"
LOCALES = ROOT / "content/exercises/locales"
TEMPLATES = ROOT / "content/exercises/templates"
PRESETS = ROOT / "content/exercises/equipment-presets.json"

# Verified refs only
REF_ACSM_RT = {
    "kind": "doi",
    "id": "10.1249/MSS.0000000000003897",
    "label": "ACSM 2026 RT Position Stand (overview of reviews)",
    "supports": ["programming", "strength", "hypertrophy", "safety"],
}
REF_ACSM_GARBER = {
    "kind": "doi",
    "id": "10.1249/MSS.0b013e318213fefb",
    "label": "Garber et al. 2011 ACSM quantity/quality guidance",
    "supports": ["programming", "endurance", "mobility", "safety"],
}
REF_SCHOENFELD_VOL = {
    "kind": "doi",
    "id": "10.1080/02640414.2016.1210197",
    "label": "Schoenfeld et al. 2016 weekly set volume meta-analysis",
    "supports": ["hypertrophy", "programming"],
}
REF_ESCAMILLA_SQUAT = {
    "kind": "doi",
    "id": "10.1097/00005768-200101000-00020",
    "label": "Escamilla 2001 — knee biomechanics of the squat",
    "supports": ["biomechanics", "technique", "safety"],
}
REF_CONTRERAS_HIP = {
    "kind": "doi",
    "id": "10.1123/jab.2014-0301",
    "label": "Contreras et al. — hip thrust vs back squat EMG (glute focus)",
    "supports": ["emg", "biomechanics"],
}
REF_SEILER = {
    "kind": "doi",
    "id": "10.1123/ijspp.5.3.276",
    "label": "Seiler 2010 — endurance intensity distribution",
    "supports": ["endurance", "programming"],
}
REF_NSCA_ESSENTIALS = {
    "kind": "textbook",
    "id": "nsca-essentials-4e",
    "label": "NSCA Essentials of Strength Training and Conditioning (technique/safety norms)",
    "supports": ["technique", "safety", "strength"],
}

en_gb: dict[str, str] = {}
uk: dict[str, str] = {}
exercises: list[dict] = []


def name_keys(eid: str, en: str, uk_name: str) -> dict:
    key = f"exercise.{eid.replace('-', '_')}.name"
    en_gb[key] = en
    uk[key] = uk_name
    return {"enGB": key, "uk": key}


def media(eid: str) -> dict:
    return {
        "illustrationKey": f"media/illustrations/{eid}",
        "videoKey": f"media/video/{eid}",
        "animationKey": f"media/anim/{eid}",
    }


def link(eid: str, note: str) -> dict:
    return {"exerciseId": eid, "note": note}


def ex(
    eid: str,
    en: str,
    uk_name: str,
    discipline: str,
    pattern: str,
    primary: list[str],
    secondary: list[str],
    equipment: list[str],
    level: str,
    unilateral: bool,
    mechanics: str,
    tracking: str,
    technique: list[str],
    setup: list[str],
    breathing: str,
    mistakes: list[str],
    safety: list[str],
    contra: list[str],
    regressions: list[tuple[str, str]] | None = None,
    progressions: list[tuple[str, str]] | None = None,
    refs: list[dict] | None = None,
    aliases: list[str] | None = None,
    tags: list[str] | None = None,
) -> None:
    exercises.append(
        {
            "id": eid,
            "nameKeys": name_keys(eid, en, uk_name),
            "discipline": discipline,
            "movementPattern": pattern,
            "primaryMuscles": primary,
            "secondaryMuscles": secondary,
            "equipment": equipment,
            "level": level,
            "unilateral": unilateral,
            "mechanics": mechanics,
            "trackingType": tracking,
            "technique": technique,
            "setup": setup,
            "breathing": breathing,
            "commonMistakes": mistakes,
            "safety": safety,
            "contraindications": contra,
            "regressions": [link(a, b) for a, b in (regressions or [])],
            "progressions": [link(a, b) for a, b in (progressions or [])],
            "evidenceRefs": refs or [REF_NSCA_ESSENTIALS, REF_ACSM_RT],
            "media": media(eid),
            "aliases": aliases or [],
            "tags": tags or [],
        }
    )


# ---------------------------------------------------------------------------
# STRENGTH (~80)
# ---------------------------------------------------------------------------

# Squat pattern
ex(
    "back-squat", "Back squat", "Присід зі штангою на спині",
    "strength", "squat",
    ["quadriceps", "gluteus_maximus"], ["hamstrings", "erector_spinae", "calves"],
    ["barbell"], "intermediate", False, "compound", "weight_reps",
    [
        "Brace the trunk, set the bar on the upper back, and unlock hips and knees together.",
        "Descend until thighs reach at least parallel while keeping heels down and knees tracking over mid-foot.",
        "Drive up by extending hips and knees without losing lumbar neutrality.",
    ],
    ["Rack height at mid-chest; step under the bar; create upper-back shelf; walk out with short steps."],
    "Inhale and brace before descent; forceful exhale near lockout or use a brief Valsalva on heavy sets.",
    ["Heels rising", "Knees caving in", "Excessive forward torso collapse", "Losing lumbar brace"],
    ["Use collars; clear spotters/safety pins; stop if sharp knee or spine pain."],
    ["Uncontrolled hypertension for maximal Valsalva", "Acute lumbar disc flare without clearance"],
    [("goblet-squat", "Learn depth and brace with goblet load"), ("box-squat", "Depth target with box")],
    [("front-squat", "More upright torso variation"), ("pause-back-squat", "Pause for control")],
    [REF_ESCAMILLA_SQUAT, REF_NSCA_ESSENTIALS, REF_ACSM_RT],
    ["barbell squat"],
    ["compound", "legs"],
)

ex(
    "front-squat", "Front squat", "Фронтальний присід",
    "strength", "squat",
    ["quadriceps"], ["gluteus_maximus", "erector_spinae", "front_deltoid"],
    ["barbell"], "intermediate", False, "compound", "weight_reps",
    [
        "Hold the bar in a front-rack with elbows high; torso stays more upright than a back squat.",
        "Sit between the hips to parallel or below while keeping elbows from dropping.",
        "Stand by driving through mid-foot without elbows collapsing.",
    ],
    ["Clean the bar to front rack or take from rack; set stance roughly shoulder-width."],
    "Brace before descent; exhale as you stand.",
    ["Elbows dropping", "Heels lifting", "Rounding upper back"],
    ["Start light; wrist mobility alternatives (cross-arm) acceptable."],
    ["Painful wrist/shoulder front-rack without modification"],
    [("goblet-squat", "Easier front-loaded pattern"), ("back-squat", "If front rack limits load")],
    [("pause-front-squat", "Pause for positional strength")],
    [REF_ESCAMILLA_SQUAT, REF_NSCA_ESSENTIALS],
)

ex(
    "goblet-squat", "Goblet squat", "Гоблет-присід",
    "strength", "squat",
    ["quadriceps", "gluteus_maximus"], ["core_brace".replace("core_brace", "rectus_abdominis"), "hamstrings"],
    ["dumbbell", "kettlebell"], "beginner", False, "compound", "weight_reps",
    [
        "Hold a dumbbell or kettlebell at the chest with elbows under the weight.",
        "Sit hips down and back to a comfortable depth with chest tall.",
        "Stand tall without shifting onto the toes.",
    ],
    ["Feet roughly shoulder-width; toes slightly out; weight held at sternum."],
    "Inhale down, exhale up.",
    ["Collapsing chest", "Knees drifting far inside toes"],
    ["Choose a depth you can control; elevate heels slightly only if needed for mobility."],
    ["Acute knee effusion"],
    [("sit-to-stand", "Chair-assisted squat pattern")],
    [("back-squat", "Barbell loading"), ("front-squat", "Front-loaded barbell")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

# Fix goblet secondary - I made a mistake with core_brace. Let me fix in post or rewrite.
exercises[-1]["secondaryMuscles"] = ["rectus_abdominis", "hamstrings"]

ex(
    "box-squat", "Box squat", "Присід на ящик",
    "strength", "squat",
    ["quadriceps", "gluteus_maximus"], ["hamstrings", "erector_spinae"],
    ["barbell", "box"], "beginner", False, "compound", "weight_reps",
    [
        "Sit back to a box at parallel while keeping the trunk braced.",
        "Touch lightly without fully relaxing the spine, then stand.",
    ],
    ["Set box height to target depth; same walkout as back squat."],
    "Brace before sitting; exhale to stand.",
    ["Plopping onto the box", "Rocking forward to stand"],
    ["Use a stable box; spot heavy attempts."],
    ["Acute lumbar pain"],
    [("goblet-squat", "Unload the spine")],
    [("back-squat", "Free squat without box")],
    [REF_ESCAMILLA_SQUAT, REF_NSCA_ESSENTIALS],
)

ex(
    "pause-back-squat", "Pause back squat", "Присід з паузою",
    "strength", "squat",
    ["quadriceps", "gluteus_maximus"], ["hamstrings", "erector_spinae"],
    ["barbell"], "advanced", False, "compound", "weight_reps",
    ["Descend to depth, pause 1–3 s without losing brace, then stand without bouncing."],
    ["Identical to back squat setup; use lighter load than touch-and-go."],
    "Hold breath briefly during pause if braced; exhale near lockout.",
    ["Softening brace in the pause", "Rising onto toes"],
    ["Reduce load 10–20% versus normal squat."],
    ["Same as back squat"],
    [("back-squat", "Remove pause")],
    [],
    [REF_ESCAMILLA_SQUAT, REF_NSCA_ESSENTIALS],
)

ex(
    "pause-front-squat", "Pause front squat", "Фронтальний присід з паузою",
    "strength", "squat",
    ["quadriceps"], ["gluteus_maximus", "erector_spinae"],
    ["barbell"], "advanced", False, "compound", "weight_reps",
    ["Front squat pattern with a controlled pause in the bottom."],
    ["Front-rack setup; lighter than normal front squat."],
    "Maintain brace through the pause.",
    ["Elbows dropping in the pause"],
    ["Prioritise position over load."],
    ["Wrist pain in rack"],
    [("front-squat", "Remove pause")],
    [],
    [REF_ESCAMILLA_SQUAT, REF_NSCA_ESSENTIALS],
)

ex(
    "leg-press", "Leg press", "Жим ногами",
    "strength", "squat",
    ["quadriceps", "gluteus_maximus"], ["hamstrings"],
    ["machine"], "beginner", False, "compound", "weight_reps",
    [
        "Place feet mid-platform; unlock knees slightly; lower until thighs approach torso without rounding lumbar spine.",
        "Press through mid-foot to near lockout without slamming the sled.",
    ],
    ["Adjust seat so hips and knees stay comfortable; safety stops set."],
    "Exhale on the press.",
    ["Lumbar rounding at the bottom", "Locking out aggressively"],
    ["Do not allow the lower back to peel off the pad."],
    ["Uncontrolled hypertension for heavy isometric straining"],
    [("goblet-squat", "Free squat pattern")],
    [("back-squat", "Axial loading progression")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_RT],
)

ex(
    "hack-squat-machine", "Hack squat (machine)", "Гак-присід (тренажер)",
    "strength", "squat",
    ["quadriceps"], ["gluteus_maximus", "calves"],
    ["machine"], "intermediate", False, "compound", "weight_reps",
    ["Shoulders under pads; descend with knees tracking over toes; drive up without hips shooting back early."],
    ["Feet shoulder-width on platform; safeties engaged."],
    "Inhale down, exhale up.",
    ["Heels lifting", "Partial reps without intent"],
    ["Control depth; avoid knee valgus crash."],
    ["Acute knee pain"],
    [("leg-press", "More supported"), ("goblet-squat", "Free pattern")],
    [("back-squat", "Barbell loading")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "sit-to-stand", "Sit-to-stand", "Вставання зі стільця",
    "strength", "squat",
    ["quadriceps", "gluteus_maximus"], ["hamstrings"],
    ["bodyweight"], "beginner", False, "compound", "reps",
    ["From a chair, lean slightly forward, stand without using hands if possible, then sit with control."],
    ["Stable chair against a wall; feet under knees."],
    "Exhale to stand.",
    ["Plopping into the seat", "Knees collapsing"],
    ["Use hands on thighs if needed; progress by removing assistance."],
    ["Severe orthostatic symptoms — seek clinical advice"],
    [],
    [("goblet-squat", "Add load")],
    [REF_ACSM_GARBER, REF_NSCA_ESSENTIALS],
)

# Hinge
ex(
    "conventional-deadlift", "Conventional deadlift", "Класична станова тяга",
    "strength", "hinge",
    ["hamstrings", "gluteus_maximus", "erector_spinae"], ["quadriceps", "trapezius", "forearms", "latissimus_dorsi"],
    ["barbell"], "intermediate", False, "compound", "weight_reps",
    [
        "Hip-hinge to the bar with shins close, spine neutral, lats engaged.",
        "Push the floor away, keep the bar close, and lock hips and knees together at the top.",
        "Return by hinging hips back first while keeping the bar over mid-foot.",
    ],
    ["Bar over mid-foot; grip just outside legs; brace before pull."],
    "Big brace before the pull; exhale at lockout or between reps.",
    ["Rounding the lumbar spine", "Jerking the bar", "Hyperextending at lockout"],
    ["Use mixed/hook grip carefully; stop if sharp back pain."],
    ["Acute lumbar injury without clearance"],
    [("romanian-deadlift-dumbbell", "Learn hinge with less load from floor"), ("trap-bar-deadlift", "More neutral grip")],
    [("deficit-deadlift", "Increase range"), ("pause-deadlift", "Positional strength")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_RT],
    ["deadlift"],
)

ex(
    "sumo-deadlift", "Sumo deadlift", "Станова тяга сумо",
    "strength", "hinge",
    ["gluteus_maximus", "adductors", "quadriceps"], ["hamstrings", "erector_spinae", "forearms"],
    ["barbell"], "intermediate", False, "compound", "weight_reps",
    ["Wide stance, toes out, hands inside knees; keep chest up and push floor apart as you stand."],
    ["Bar over mid-foot; shins vertical-ish; brace hard."],
    "Brace before pull; exhale at lockout.",
    ["Hips shooting up early", "Losing toe-knee alignment"],
    ["Mind adductors/groin; warm up thoroughly."],
    ["Acute groin strain"],
    [("trap-bar-deadlift", "Neutral alternative"), ("goblet-squat", "If hinge capacity limited")],
    [("conventional-deadlift", "Narrow stance variant")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "trap-bar-deadlift", "Trap-bar deadlift", "Станова тяга з трап-грифом",
    "strength", "hinge",
    ["quadriceps", "gluteus_maximus", "hamstrings"], ["erector_spinae", "trapezius", "forearms"],
    ["trap_bar"], "beginner", False, "compound", "weight_reps",
    ["Stand inside the trap bar, hinge and grip handles, stand tall by pushing the floor away, then hinge back down."],
    ["High or low handles based on mobility; neutral spine."],
    "Brace, then exhale near lockout.",
    ["Rounding through the lift", "Shrugging early"],
    ["Often friendlier for beginners than straight-bar pulls."],
    ["Acute lumbar pain"],
    [("romanian-deadlift-dumbbell", "Hinge drill"), ("sit-to-stand", "If loading not ready")],
    [("conventional-deadlift", "Straight bar"), ("back-squat", "Squat pattern emphasis")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_RT],
)

ex(
    "romanian-deadlift-barbell", "Romanian deadlift (barbell)", "Румунська тяга (штанга)",
    "strength", "hinge",
    ["hamstrings", "gluteus_maximus"], ["erector_spinae", "forearms"],
    ["barbell"], "intermediate", False, "compound", "weight_reps",
    ["From standing, push hips back with soft knees, slide the bar down the thighs until a strong hamstring stretch, then return."],
    ["Start from a rack or carefully lift off floor to stand; shoulder-blade set."],
    "Inhale as you hinge, exhale as hips return.",
    ["Rounding lumbar spine", "Turning it into a squat"],
    ["Keep bar close; stop range before form breaks."],
    ["Hamstring strain acute phase"],
    [("romanian-deadlift-dumbbell", "Easier to bail"), ("hip-hinge-dowel", "Pattern only")],
    [("conventional-deadlift", "Pull from floor"), ("good-morning", "Bar on back hinge")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "romanian-deadlift-dumbbell", "Romanian deadlift (dumbbell)", "Румунська тяга (гантелі)",
    "strength", "hinge",
    ["hamstrings", "gluteus_maximus"], ["erector_spinae", "forearms"],
    ["dumbbell"], "beginner", False, "compound", "weight_reps",
    ["Hold dumbbells at thighs; hinge hips back; keep weights close to legs; stand by driving hips forward."],
    ["Soft knees; neutral neck."],
    "Inhale hinge, exhale stand.",
    ["Reaching weights far forward", "Locking knees hard"],
    ["Use light loads to groove the pattern."],
    ["Acute low-back flare"],
    [("hip-hinge-dowel", "Unload")],
    [("romanian-deadlift-barbell", "Barbell load"), ("single-leg-rdl", "Unilateral")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

ex(
    "single-leg-rdl", "Single-leg RDL", "Румунська тяга на одній нозі",
    "strength", "hinge",
    ["hamstrings", "gluteus_maximus", "gluteus_medius"], ["erector_spinae"],
    ["dumbbell", "kettlebell"], "intermediate", True, "compound", "weight_reps",
    ["Hinge on the stance leg while the free leg reaches back; keep hips square; return to tall stand."],
    ["Light load; optional support for balance."],
    "Steady breathing; brace lightly.",
    ["Rotating the pelvis open", "Rounding the back"],
    ["Start bodyweight; progress load slowly."],
    ["Uncontrolled ankle instability"],
    [("romanian-deadlift-dumbbell", "Bilateral hinge")],
    [("conventional-deadlift", "Bilateral heavy hinge")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "good-morning", "Good morning", "Гуд-морнінг",
    "strength", "hinge",
    ["hamstrings", "erector_spinae", "gluteus_maximus"], [],
    ["barbell"], "advanced", False, "compound", "weight_reps",
    ["Bar on upper back; soft knees; hinge hips back until torso nears parallel; return by extending hips."],
    ["Light load; braced trunk; careful walkout."],
    "Inhale hinge, exhale extend.",
    ["Squatting instead of hinging", "Cervical hyperextension"],
    ["Conservative loading; this stresses the posterior chain heavily."],
    ["History of lumbar stress injury — clinician clearance"],
    [("romanian-deadlift-barbell", "Safer load path for many")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "hip-thrust", "Hip thrust", "Ліктьовий/плечовий упор — підйом таза (hip thrust)",
    "strength", "hinge",
    ["gluteus_maximus"], ["hamstrings", "quadriceps"],
    ["barbell", "bench"], "beginner", False, "compound", "weight_reps",
    [
        "Upper back on a bench, bar over hips with padding; drive hips to full extension with ribs down.",
        "Lower with control without losing pelvic control.",
    ],
    ["Bench stable; shin vertical at lockout roughly; chin tucked slightly."],
    "Exhale on the thrust.",
    ["Overarching lumbar spine", "Pushing through toes only"],
    ["Pad the bar; avoid cervical strain from looking up hard."],
    ["Acute sacral/pelvic pain"],
    [("glute-bridge", "Floor regression")],
    [("single-leg-hip-thrust", "Unilateral progression")],
    [REF_CONTRERAS_HIP, REF_NSCA_ESSENTIALS],
)

ex(
    "glute-bridge", "Glute bridge", "Ягодичний місток",
    "strength", "hinge",
    ["gluteus_maximus"], ["hamstrings"],
    ["bodyweight"], "beginner", False, "isolation", "reps",
    ["Lie supine, feet flat, drive hips up by squeezing glutes, pause, lower."],
    ["Feet hip-width; arms by sides."],
    "Exhale to lift.",
    ["Lumbar overextension", "Pushing knees inward"],
    ["Keep ribs down; feel glutes more than low back."],
    [],
    [],
    [("hip-thrust", "Add bench/load"), ("single-leg-hip-thrust", "Unilateral")],
    [REF_CONTRERAS_HIP, REF_ACSM_GARBER],
)

ex(
    "single-leg-hip-thrust", "Single-leg hip thrust", "Hip thrust на одній нозі",
    "strength", "hinge",
    ["gluteus_maximus", "gluteus_medius"], ["hamstrings"],
    ["bodyweight", "bench"], "intermediate", True, "compound", "reps",
    ["Same as hip thrust with one foot on the floor; keep hips level."],
    ["Start unloaded."],
    "Exhale to extend.",
    ["Hip drop on working side"],
    ["Reduce range if control fails."],
    [],
    [("glute-bridge", "Bilateral floor"), ("hip-thrust", "Bilateral loaded")],
    [],
    [REF_CONTRERAS_HIP, REF_NSCA_ESSENTIALS],
)

ex(
    "kettlebell-swing", "Kettlebell swing", "Мах гирею",
    "strength", "hinge",
    ["gluteus_maximus", "hamstrings"], ["erector_spinae", "latissimus_dorsi", "forearms"],
    ["kettlebell"], "intermediate", False, "compound", "reps",
    [
        "Hike the bell back high in the hips, then snap hips to standing; bell floats to about chest height.",
        "Let the bell fall; hinge again; repeat rhythmically.",
    ],
    ["Feet shoulder-width; neutral spine; pack shoulders."],
    "Sharp exhale on the hip snap.",
    ["Squatting the swing", "Raising with the arms", "Hyperextending lockout"],
    ["Clear space; use a flat sole shoe; stop if back rounds."],
    ["Acute low-back pain", "Uncontrolled hypertension for ballistic work"],
    [("romanian-deadlift-dumbbell", "Slow hinge"), ("hip-hinge-dowel", "Pattern")],
    [("conventional-deadlift", "Maximal strength hinge")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "hip-hinge-dowel", "Hip hinge (dowel drill)", "Засвоєння тазостегнового згину (з палицею)",
    "strength", "hinge",
    ["hamstrings", "gluteus_maximus"], ["erector_spinae"],
    ["bodyweight"], "beginner", False, "skill", "reps",
    ["Hold a dowel along head–thoracic–sacrum contact; push hips back keeping all three points; return tall."],
    ["Barefoot or flat shoes; soft knees."],
    "Quiet nasal breathing fine.",
    ["Losing dowel contact at low back", "Bending only the spine"],
    ["Teaching drill — no heavy load."],
    [],
    [],
    [("romanian-deadlift-dumbbell", "Add load")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "deficit-deadlift", "Deficit deadlift", "Станова тяга з дефіциту",
    "strength", "hinge",
    ["hamstrings", "gluteus_maximus", "erector_spinae"], ["quadriceps", "forearms"],
    ["barbell"], "advanced", False, "compound", "weight_reps",
    ["Stand on a low plate/deficit; pull with the same cues as conventional, emphasising posture off the floor."],
    ["Small deficit (2–5 cm) first; perfect conventional form required."],
    "Brace hard before the pull.",
    ["Rounding off the floor"],
    ["Use only with solid conventional technique."],
    ["Same as conventional deadlift"],
    [("conventional-deadlift", "Remove deficit")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "pause-deadlift", "Pause deadlift", "Станова тяга з паузою",
    "strength", "hinge",
    ["hamstrings", "erector_spinae", "gluteus_maximus"], ["quadriceps", "forearms"],
    ["barbell"], "advanced", False, "compound", "weight_reps",
    ["Pull to just below the knee, pause while braced, then finish lockout."],
    ["Lighter than full deadlift."],
    "Maintain brace during pause.",
    ["Losing position in the pause"],
    ["Positional tool — not for maximal testing."],
    ["Acute lumbar pain"],
    [("conventional-deadlift", "Remove pause")],
    [],
    [REF_NSCA_ESSENTIALS],
)

# Push
ex(
    "barbell-bench-press", "Barbell bench press", "Жим штанги лежачи",
    "strength", "push",
    ["pectoralis_major", "triceps", "front_deltoid"], ["serratus_anterior"],
    ["barbell", "bench"], "intermediate", False, "compound", "weight_reps",
    [
        "Eyes under bar; set shoulder blades; gentle arch; feet planted.",
        "Unrack, lower bar to lower chest/sternum with forearms vertical at bottom, press to lockout.",
    ],
    ["Spotter or safety arms; wrists stacked; consistent grip width."],
    "Inhale/brace on descent; exhale near lockout.",
    ["Flaring elbows excessively", "Bouncing off chest", "Uneven lockout"],
    ["Never bench heavy without safeties/spotter; keep wrists neutral."],
    ["Acute shoulder impingement without modification"],
    [("push-up", "Bodyweight"), ("dumbbell-bench-press", "More shoulder freedom")],
    [("pause-bench-press", "Pause"), ("close-grip-bench-press", "Triceps emphasis")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_RT],
)

ex(
    "pause-bench-press", "Pause bench press", "Жим лежачи з паузою",
    "strength", "push",
    ["pectoralis_major", "triceps"], ["front_deltoid"],
    ["barbell", "bench"], "advanced", False, "compound", "weight_reps",
    ["Lower to chest, pause 1 s without losing tension, press."],
    ["Same as bench; reduce load."],
    "Hold brace through pause.",
    ["Relaxing on the chest"],
    ["Use safeties."],
    ["Shoulder pain"],
    [("barbell-bench-press", "Touch and go")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "close-grip-bench-press", "Close-grip bench press", "Жим вузьким хватом",
    "strength", "push",
    ["triceps", "pectoralis_major"], ["front_deltoid"],
    ["barbell", "bench"], "intermediate", False, "compound", "weight_reps",
    ["Grip nearer shoulder-width; lower with elbows closer to the torso; press."],
    ["Not ultra-narrow; wrists stay stacked."],
    "Same as bench breathing.",
    ["Wrists bending", "Elbows flaring wide anyway"],
    ["Avoid extreme narrow grip that stresses wrists."],
    ["Wrist pain"],
    [("tricep-pushdown", "Isolation"), ("push-up", "Regression")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "dumbbell-bench-press", "Dumbbell bench press", "Жим гантелей лежачи",
    "strength", "push",
    ["pectoralis_major", "triceps", "front_deltoid"], [],
    ["dumbbell", "bench"], "beginner", False, "compound", "weight_reps",
    ["Press dumbbells from chest level to lockout with wrists neutral; lower with control."],
    ["Kick or hoist dumbbells carefully into position."],
    "Exhale on the press.",
    ["Excessive elbow flare with pain", "Crashing weights together"],
    ["Easier to match shoulder comfort than a bar for many."],
    ["Uncontrolled cuff tear symptoms"],
    [("push-up", "Bodyweight"), ("floor-press-dumbbell", "Limited ROM")],
    [("barbell-bench-press", "Barbell load"), ("dumbbell-incline-press", "Upper chest angle")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

ex(
    "dumbbell-incline-press", "Incline dumbbell press", "Жим гантелей на похилій",
    "strength", "push",
    ["pectoralis_major", "front_deltoid", "triceps"], [],
    ["dumbbell", "bench"], "intermediate", False, "compound", "weight_reps",
    ["Bench ~15–30°; press dumbbells up and slightly together; lower to outer chest."],
    ["Set incline; scapulae supported."],
    "Exhale to press.",
    ["Too steep becoming a shoulder press", "Wrists collapsing"],
    ["Keep incline modest for pectoralis emphasis."],
    ["Anterior shoulder irritation"],
    [("dumbbell-bench-press", "Flat"), ("push-up", "Regression")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "floor-press-dumbbell", "Dumbbell floor press", "Жим гантелей з підлоги",
    "strength", "push",
    ["pectoralis_major", "triceps"], ["front_deltoid"],
    ["dumbbell"], "beginner", False, "compound", "weight_reps",
    ["Lie on floor; lower elbows to touch floor gently; press to lockout."],
    ["Knees bent; shoulders set."],
    "Exhale to press.",
    ["Bouncing elbows hard into the floor"],
    ["Shorter ROM can spare shoulders."],
    [],
    [("push-up", "Different pattern")],
    [("dumbbell-bench-press", "Full ROM")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "push-up", "Push-up", "Відтискання",
    "strength", "push",
    ["pectoralis_major", "triceps", "front_deltoid"], ["serratus_anterior", "rectus_abdominis"],
    ["bodyweight"], "beginner", False, "compound", "bodyweight_reps",
    ["High plank; lower chest toward floor with body rigid; press up without hips sagging."],
    ["Hands under shoulders; neutral neck."],
    "Inhale down, exhale up.",
    ["Hips sagging", "Flaring elbows to 90° with pain", "Partial range without intent"],
    ["Elevate hands to regress; elevate feet to progress."],
    ["Acute wrist pain — use handles/fists if appropriate"],
    [("incline-push-up", "Hands elevated"), ("knee-push-up", "Knees down")],
    [("diamond-push-up", "Closer grip"), ("barbell-bench-press", "Loaded")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

ex(
    "incline-push-up", "Incline push-up", "Відтискання з підвищеною опорою",
    "strength", "push",
    ["pectoralis_major", "triceps"], ["front_deltoid", "serratus_anterior"],
    ["bodyweight", "bench"], "beginner", False, "compound", "bodyweight_reps",
    ["Hands on a bench/rail; perform a rigid push-up."],
    ["Stable surface; body in straight line."],
    "Exhale to press.",
    ["Hips piked high"],
    ["Higher surface = easier."],
    [],
    [("knee-push-up", "Alternative regression")],
    [("push-up", "Floor")],
    [REF_ACSM_GARBER, REF_NSCA_ESSENTIALS],
)

ex(
    "knee-push-up", "Knee push-up", "Відтискання з колін",
    "strength", "push",
    ["pectoralis_major", "triceps"], ["front_deltoid"],
    ["bodyweight"], "beginner", False, "compound", "bodyweight_reps",
    ["From knees, keep a straight line from knees to head; lower and press."],
    ["Pad knees if needed."],
    "Exhale to press.",
    ["Broken hip line", "Neck poking"],
    ["Still brace the trunk."],
    [],
    [],
    [("incline-push-up", "Another regression"), ("push-up", "Full")],
    [REF_ACSM_GARBER],
)

ex(
    "diamond-push-up", "Diamond push-up", "Відтискання вузьким упором",
    "strength", "push",
    ["triceps", "pectoralis_major"], ["front_deltoid"],
    ["bodyweight"], "intermediate", False, "compound", "bodyweight_reps",
    ["Hands close under chest forming a diamond/triangle; perform push-up with elbows tracking nearer the torso."],
    ["High plank; strong brace."],
    "Exhale to press.",
    ["Hips sagging under harder leverage"],
    ["Wrist-friendly hand positions allowed if diamond stresses joints."],
    ["Wrist pain"],
    [("push-up", "Wider"), ("close-grip-bench-press", "Loaded")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "overhead-press-barbell", "Overhead press (barbell)", "Жим штанги стоячи",
    "strength", "push",
    ["front_deltoid", "triceps"], ["trapezius", "pectoralis_major", "rectus_abdominis"],
    ["barbell"], "intermediate", False, "compound", "weight_reps",
    ["Bar at front rack; brace; press vertically while head moves through; lock out overhead; lower with control."],
    ["Stance roughly hip-width; glutes tight; ribs down."],
    "Brace before press; exhale through sticking point.",
    ["Excessive lumbar lean", "Pressing forward around the face"],
    ["Clear overhead mobility first; stop with shoulder pain."],
    ["Uncontrolled hypertension", "Symptomatic shoulder instability"],
    [("overhead-press-dumbbell", "Independent arms"), ("landmine-press", "Angled")],
    [("push-press", "Leg drive assist")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_RT],
)

ex(
    "overhead-press-dumbbell", "Overhead press (dumbbell)", "Жим гантелей стоячи",
    "strength", "push",
    ["front_deltoid", "lateral_deltoid", "triceps"], ["trapezius"],
    ["dumbbell"], "beginner", False, "compound", "weight_reps",
    ["Press dumbbells from shoulder height to lockout without shrugging excessively; lower slowly."],
    ["Seated or standing; braced trunk."],
    "Exhale to press.",
    ["Flared ribs / lumbar arch", "Clanking bells hard"],
    ["Seated version reduces cheat from legs."],
    ["Shoulder pain overhead"],
    [("landmine-press", "Less overhead demand")],
    [("overhead-press-barbell", "Barbell")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

ex(
    "push-press", "Push press", "Швунг жимовий",
    "strength", "push",
    ["front_deltoid", "triceps", "quadriceps"], ["gluteus_maximus", "trapezius"],
    ["barbell"], "intermediate", False, "compound", "weight_reps",
    ["Dip with legs, drive up, and press the bar overhead as legs extend; lock out; lower."],
    ["Front rack; athletic stance."],
    "Exhale on the drive." ,
    ["Pressing before the drive", "Soft lockout"],
    ["Coordinate legs and arms; use for overload carefully."],
    ["Same as overhead press"],
    [("overhead-press-barbell", "Strict")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "landmine-press", "Landmine press", "Жим з лендмайну",
    "strength", "push",
    ["front_deltoid", "pectoralis_major", "triceps"], ["serratus_anterior"],
    ["landmine", "barbell"], "beginner", True, "compound", "weight_reps",
    ["Press the angled bar forward-up from shoulder height; can be single-arm."],
    ["Bar anchored in landmine; athletic stance."],
    "Exhale to press.",
    ["Twisting the torso unintentionally", "Shrugging into the neck"],
    ["Useful when vertical pressing bothers the shoulder."],
    [],
    [("overhead-press-dumbbell", "More vertical")],
    [("overhead-press-barbell", "Vertical barbell")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "dip-parallel-bar", "Parallel-bar dip", "Відтискання на брусах",
    "strength", "push",
    ["pectoralis_major", "triceps", "front_deltoid"], [],
    ["bodyweight"], "intermediate", False, "compound", "bodyweight_reps",
    ["Support on bars; descend by bending elbows until upper arms near parallel; press up."],
    ["Slight forward lean for chest; more upright for triceps."],
    "Exhale to press.",
    ["Dumping into shoulder stretch painfully", "Swinging"],
    ["Use assisted machine/bands if needed; stop with anterior shoulder pain."],
    ["Anterior shoulder instability"],
    [("bench-dip-assisted", "Feet-assisted"), ("incline-push-up", "Regression")],
    [("ring-dip", "Unstable progression")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "bench-dip-assisted", "Bench dip (feet assisted)", "Зворотні відтискання з опорою ніг",
    "strength", "push",
    ["triceps"], ["front_deltoid", "pectoralis_major"],
    ["bench", "bodyweight"], "beginner", False, "isolation", "bodyweight_reps",
    ["Hands on bench behind you, feet on floor; bend elbows to lower, then extend."],
    ["Shoulders away from ears; modest ROM."],
    "Exhale to extend.",
    ["Going too deep into shoulder extension"],
    ["Keep hips close to bench; shallow ROM if shoulders complain."],
    ["Shoulder pain in extension"],
    [],
    [("dip-parallel-bar", "Full dip")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "ring-dip", "Ring dip", "Відтискання на кільцях",
    "strength", "push",
    ["pectoralis_major", "triceps"], ["serratus_anterior", "rectus_abdominis"],
    ["suspension", "bodyweight"], "advanced", False, "compound", "bodyweight_reps",
    ["Stabilise rings, descend under control, press to support with rings turned out slightly."],
    ["Rings set appropriately; hollow body."],
    "Exhale to press.",
    ["Losing ring stability", "Excess depth"],
    ["Master parallel dips first."],
    ["Shoulder instability"],
    [("dip-parallel-bar", "Stable bars")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "cable-chest-fly", "Cable chest fly", "Розведення на блоках",
    "strength", "push",
    ["pectoralis_major"], ["front_deltoid"],
    ["cable"], "beginner", False, "isolation", "weight_reps",
    ["With a soft elbow, bring handles together in an arc; reverse slowly."],
    ["Cables at roughly chest height; stagger stance optional."],
    "Exhale on the squeeze.",
    ["Straight-arm shoulder stress", "Using momentum"],
    ["Keep elbows soft; modest stretch."],
    ["Acute pec strain"],
    [("push-up", "Compound"), ("dumbbell-bench-press", "Compound load")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "lateral-raise", "Lateral raise", "Махи гантелями вбік",
    "strength", "push",
    ["lateral_deltoid"], ["front_deltoid", "trapezius"],
    ["dumbbell"], "beginner", False, "isolation", "weight_reps",
    ["Raise dumbbells to roughly shoulder height with soft elbows; lower slowly; slight forward lead of pinkies optional."],
    ["Standing tall; light weights."],
    "Exhale to raise.",
    ["Shrugging traps to cheat", "Swinging torso"],
    ["Stop at shoulder height; control the eccentric."],
    ["Acute cuff flare"],
    [],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "tricep-pushdown", "Triceps pushdown", "Розгинання рук на блоці",
    "strength", "push",
    ["triceps"], [],
    ["cable"], "beginner", False, "isolation", "weight_reps",
    ["Elbows fixed at sides; extend the elbows against a cable; return without flaring elbows."],
    ["Neutral ribs; light enough to feel triceps."],
    "Exhale to extend.",
    ["Elbows drifting forward", "Shoulder pressing into the move"],
    ["Full extension without elbow pain."],
    [],
    [("bench-dip-assisted", "Another triceps option")],
    [("close-grip-bench-press", "Compound")],
    [REF_NSCA_ESSENTIALS],
)

# Pull
ex(
    "pull-up", "Pull-up", "Підтягування пронованим хватом",
    "strength", "pull",
    ["latissimus_dorsi", "biceps"], ["trapezius", "rhomboids", "rear_deltoid", "forearms"],
    ["pull_up_bar"], "intermediate", False, "compound", "bodyweight_reps",
    ["From dead hang, pull elbows down/back until chin clears bar; lower fully with control."],
    ["Shoulder-width or slightly wider overhand grip; brace lightly."],
    "Exhale on the pull.",
    ["Kipping when aiming for strict strength", "Shrugging without scapular depression"],
    ["Use band/machine assistance; avoid shoulder pain at bottom by not forcing extreme hang."],
    ["Acute shoulder injury"],
    [("lat-pulldown", "Seated load control"), ("assisted-pull-up-band", "Band assist")],
    [("weighted-pull-up", "Add load"), ("chest-to-bar-pull-up", "Higher pull")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_RT],
)

ex(
    "chin-up", "Chin-up", "Підтягування супінованим хватом",
    "strength", "pull",
    ["latissimus_dorsi", "biceps"], ["rhomboids", "forearms"],
    ["pull_up_bar"], "intermediate", False, "compound", "bodyweight_reps",
    ["Underhand grip pull-up; same full ROM standards."],
    ["Hands roughly shoulder-width."],
    "Exhale on the pull.",
    ["Partial ROM", "Excess swing"],
    ["Often slightly easier than pull-ups for many beginners."],
    ["Elbow tendinopathy flare"],
    [("lat-pulldown", "Machine"), ("assisted-pull-up-band", "Assist")],
    [("pull-up", "Overhand"), ("weighted-pull-up", "Load")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "assisted-pull-up-band", "Band-assisted pull-up", "Підтягування з гумою",
    "strength", "pull",
    ["latissimus_dorsi", "biceps"], ["rhomboids"],
    ["pull_up_bar", "bands"], "beginner", False, "compound", "bodyweight_reps",
    ["Loop a band on the bar and kneel/step into it; perform a controlled pull-up."],
    ["Secure band carefully; stable setup."],
    "Exhale on the pull.",
    ["Using too much bounce from the band"],
    ["Progress by using a thinner band."],
    [],
    [("lat-pulldown", "Alternative")],
    [("pull-up", "Unassisted")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

ex(
    "weighted-pull-up", "Weighted pull-up", "Підтягування з обтяженням",
    "strength", "pull",
    ["latissimus_dorsi", "biceps"], ["forearms", "rhomboids"],
    ["pull_up_bar"], "advanced", False, "compound", "weight_reps",
    ["Strict pull-up with a dip belt or held dumbbell; full ROM."],
    ["Only after solid bodyweight sets."],
    "Exhale on the pull.",
    ["Shortening ROM under load"],
    ["Add load conservatively."],
    ["Same as pull-up"],
    [("pull-up", "Unload")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "chest-to-bar-pull-up", "Chest-to-bar pull-up", "Підтягування до грудей",
    "strength", "pull",
    ["latissimus_dorsi", "biceps"], ["trapezius", "rear_deltoid"],
    ["pull_up_bar"], "advanced", False, "compound", "bodyweight_reps",
    ["Pull until chest contacts the bar; maintain control."],
    ["Requires strong standard pull-ups first."],
    "Exhale on the pull.",
    ["Excessive kip if the goal is strength"],
    ["Treat as a skill/strength hybrid."],
    [],
    [("pull-up", "Chin-over-bar standard")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "lat-pulldown", "Lat pulldown", "Тяга верхнього блока",
    "strength", "pull",
    ["latissimus_dorsi"], ["biceps", "rhomboids", "rear_deltoid"],
    ["cable", "machine"], "beginner", False, "compound", "weight_reps",
    ["Pull the bar to upper chest by driving elbows down; control the return to a soft stretch."],
    ["Seat adjusted; thighs secured; torso tall."],
    "Exhale on the pull.",
    ["Leaning excessively", "Pulling behind the neck"],
    ["Prefer front pulldowns; avoid behind-neck variations."],
    ["Shoulder pain on pulldown"],
    [("seated-cable-row", "Horizontal pull")],
    [("pull-up", "Open-chain bodyweight")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

ex(
    "one-arm-dumbbell-row", "One-arm dumbbell row", "Тяга гантелі в нахилі однією рукою",
    "strength", "pull",
    ["latissimus_dorsi", "rhomboids"], ["biceps", "rear_deltoid", "erector_spinae"],
    ["dumbbell", "bench"], "beginner", True, "compound", "weight_reps",
    ["Hand and knee on bench; pull dumbbell to hip/pocket; lower with control."],
    ["Spine neutral; neck long."],
    "Exhale on the pull.",
    ["Rotating the torso to yank the weight", "Shrugging to the ear"],
    ["Think elbow to hip."],
    [],
    [("seated-cable-row", "Bilateral machine")],
    [("chest-supported-row", "Less lumbar demand"), ("barbell-row", "Bilateral barbell")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

ex(
    "barbell-row", "Barbell row", "Тяга штанги в нахилі",
    "strength", "pull",
    ["latissimus_dorsi", "rhomboids", "trapezius"], ["biceps", "erector_spinae", "rear_deltoid"],
    ["barbell"], "intermediate", False, "compound", "weight_reps",
    ["Hinge to torso ~30–45°, pull bar to lower chest/upper abdomen, lower under control."],
    ["Soft knees; braced spine; overhand or underhand grip."],
    "Exhale on the pull.",
    ["Using hip drive as a cheat if not intentional", "Rounding lumbar spine"],
    ["Keep the hinge solid; stop if back rounds."],
    ["Acute lumbar pain"],
    [("chest-supported-row", "Supported"), ("one-arm-dumbbell-row", "Unilateral")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "seated-cable-row", "Seated cable row", "Тяга горизонтального блока",
    "strength", "pull",
    ["latissimus_dorsi", "rhomboids"], ["biceps", "rear_deltoid"],
    ["cable", "machine"], "beginner", False, "compound", "weight_reps",
    ["Sit tall; pull handle to torso; squeeze shoulder blades; return without rounding."],
    ["Feet on platform; slight knee bend."],
    "Exhale on the pull.",
    ["Excessive layback", "Protracted shoulders throughout"],
    ["Use a full stretch without lumbar flexion."],
    [],
    [("one-arm-dumbbell-row", "Free weight")],
    [("barbell-row", "Hinged row")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "chest-supported-row", "Chest-supported row", "Тяга з упором грудьми",
    "strength", "pull",
    ["rhomboids", "latissimus_dorsi", "rear_deltoid"], ["biceps", "trapezius"],
    ["dumbbell", "bench"], "beginner", False, "compound", "weight_reps",
    ["Chest on incline bench; row dumbbells without yanking; squeeze at the top."],
    ["Bench ~30°; head neutral."],
    "Exhale on the pull.",
    ["Lifting chest off pad to cheat"],
    ["Great when you want less lower-back fatigue."],
    [],
    [("seated-cable-row", "Seated")],
    [("barbell-row", "Unsupported")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

ex(
    "face-pull", "Face pull", "Тяга до обличчя на блоці",
    "strength", "pull",
    ["rear_deltoid", "rhomboids", "rotator_cuff"], ["trapezius"],
    ["cable", "bands"], "beginner", False, "isolation", "weight_reps",
    ["Pull rope toward face with elbows high; externally rotate slightly at the end; return slow."],
    ["Cable at upper-chest/face height; light load."],
    "Exhale on the pull.",
    ["Turning it into a heavy row", "Shrugging only"],
    ["Prioritise feel in rear delts/cuff over load."],
    [],
    [],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "barbell-curl", "Barbell curl", "Згинання рук зі штангою",
    "strength", "pull",
    ["biceps"], ["forearms"],
    ["barbell", "ez_bar"], "beginner", False, "isolation", "weight_reps",
    ["Elbows near sides; curl the bar without swinging; lower fully."],
    ["Standing tall; optional EZ bar for wrists."],
    "Exhale on the curl.",
    ["Hip thrust cheat", "Elbows drifting far forward"],
    ["Control the eccentric."],
    ["Acute biceps tendinopathy"],
    [],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "dumbbell-curl", "Dumbbell curl", "Згинання рук з гантелями",
    "strength", "pull",
    ["biceps"], ["forearms"],
    ["dumbbell"], "beginner", True, "isolation", "weight_reps",
    ["Curl one or both dumbbells; optional supination through the ROM."],
    ["Standing or seated."],
    "Exhale on the curl.",
    ["Swinging torso"],
    ["Unilateral work can fix side-to-side gaps."],
    [],
    [],
    [("barbell-curl", "Bilateral bar")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "inverted-row", "Inverted row", "Горизонтальні підтягування",
    "strength", "pull",
    ["latissimus_dorsi", "rhomboids"], ["biceps", "rear_deltoid", "rectus_abdominis"],
    ["suspension", "pull_up_bar", "bodyweight"], "beginner", False, "compound", "bodyweight_reps",
    ["Body straight under a bar/rings; pull chest to bar; lower."],
    ["Bar height sets difficulty; heels down."],
    "Exhale on the pull.",
    ["Hips sagging", "Partial pulls"],
    ["Raise feet or lower bar to progress."],
    [],
    [("seated-cable-row", "Machine")],
    [("pull-up", "Vertical pull")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

# Lunge / single-leg
ex(
    "walking-lunge", "Walking lunge", "Випади в русі",
    "strength", "lunge",
    ["quadriceps", "gluteus_maximus"], ["hamstrings", "gluteus_medius", "calves"],
    ["bodyweight", "dumbbell"], "beginner", True, "compound", "reps",
    ["Step forward, descend until back knee nears floor, drive through front mid-foot into next step."],
    ["Tall torso; steps long enough for balance."],
    "Exhale to rise.",
    ["Front knee collapsing inward", "Short choppy steps with torso tipping"],
    ["Clear walkway; start bodyweight."],
    ["Acute knee pain"],
    [("reverse-lunge-dumbbell", "More stable for many"), ("split-squat-bodyweight", "Static")],
    [("bulgarian-split-squat", "Rear-foot elevated")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

ex(
    "reverse-lunge-dumbbell", "Reverse lunge (dumbbell)", "Зворотний випад з гантелями",
    "strength", "lunge",
    ["quadriceps", "gluteus_maximus"], ["hamstrings", "gluteus_medius"],
    ["dumbbell"], "beginner", True, "compound", "weight_reps",
    ["Step backward into a lunge; front shin stays fairly vertical; return to stand."],
    ["Dumbbells at sides; eyes forward."],
    "Exhale to stand.",
    ["Slamming the back knee", "Torso collapsing forward"],
    ["Often easier on the knees than forward lunges for some."],
    [],
    [("split-squat-bodyweight", "No step")],
    [("walking-lunge", "Traveling"), ("bulgarian-split-squat", "Harder")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "bulgarian-split-squat", "Bulgarian split squat", "Болгарський спліт-сквот",
    "strength", "lunge",
    ["quadriceps", "gluteus_maximus"], ["hamstrings", "gluteus_medius"],
    ["dumbbell", "bench"], "intermediate", True, "compound", "weight_reps",
    ["Rear foot elevated on a bench; descend on the front leg; stand without pushing mostly off the back foot."],
    ["Front foot far enough to keep heel down."],
    "Exhale to stand.",
    ["Front foot too close causing heel rise", "Holding breath excessively"],
    ["Start unloaded; pad the rear foot."],
    ["Rear-knee pain on the bench contact"],
    [("reverse-lunge-dumbbell", "Less stretch"), ("split-squat-bodyweight", "Both feet on floor")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "split-squat-bodyweight", "Split squat (bodyweight)", "Спліт-присід з вагою тіла",
    "strength", "lunge",
    ["quadriceps", "gluteus_maximus"], ["gluteus_medius"],
    ["bodyweight"], "beginner", True, "compound", "reps",
    ["Static split stance; lower vertically; rise."],
    ["Short or long stance alters emphasis; keep pelvis level."],
    "Exhale to rise.",
    ["Wobbling pelvis", "Front heel lift"],
    ["Hold a support for balance if needed."],
    [],
    [],
    [("reverse-lunge-dumbbell", "Add step + load"), ("bulgarian-split-squat", "Elevate rear foot")],
    [REF_ACSM_GARBER, REF_NSCA_ESSENTIALS],
)

ex(
    "lateral-lunge", "Lateral lunge", "Бічний випад",
    "strength", "lunge",
    ["quadriceps", "gluteus_maximus", "adductors"], ["gluteus_medius"],
    ["bodyweight", "dumbbell"], "beginner", True, "compound", "reps",
    ["Step to the side, sit into that hip with the other leg straight, then push back to centre."],
    ["Toes forward; chest tall."],
    "Exhale to return.",
    ["The straight leg's toe turning out wildly", "Rounding the back"],
    ["Useful for frontal-plane strength."],
    ["Acute groin strain"],
    [("goblet-squat", "Sagittal pattern")],
    [],
    [REF_NSCA_ESSENTIALS],
)

# Carry / core / rotation
ex(
    "farmers-carry", "Farmer's carry", "Прогулянка фермера",
    "strength", "carry",
    ["forearms", "trapezius", "full_body"], ["gluteus_medius", "rectus_abdominis"],
    ["dumbbell", "kettlebell", "trap_bar"], "beginner", False, "compound", "distance",
    ["Pick up heavy implements, stand tall, and walk with short controlled steps without leaning."],
    ["Clear path; loads equal unless suitcase variation."],
    "Steady breathing; avoid breath-holding the whole walk.",
    ["Rounded shoulders", "Shuffling with twisted torso"],
    ["Drop safely if grip fails; use chalk/straps sparingly for grip goals."],
    ["Acute low-back pain with loading"],
    [("suitcase-carry", "Unilateral"), ("plank", "Static brace")],
    [],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

ex(
    "suitcase-carry", "Suitcase carry", "Прогулянка з валізою (одна рука)",
    "strength", "carry",
    ["obliques", "gluteus_medius", "forearms"], ["trapezius"],
    ["dumbbell", "kettlebell"], "beginner", True, "compound", "distance",
    ["Carry a load in one hand while keeping shoulders and hips level; switch sides."],
    ["Stand tall before walking."],
    "Breathe normally while bracing lightly.",
    ["Leaning into the weight", "Hiking the loaded shoulder"],
    ["Great anti-lateral-flexion drill."],
    [],
    [("farmers-carry", "Bilateral")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "plank", "Plank", "Планка",
    "strength", "core_brace",
    ["transverse_abdominis", "rectus_abdominis"], ["front_deltoid", "gluteus_maximus"],
    ["bodyweight"], "beginner", False, "isometric", "hold",
    ["Forearms and toes on floor; body straight; ribs down; squeeze glutes; hold."],
    ["Elbows under shoulders; neutral neck."],
    "Steady breathing — do not hold breath the entire time.",
    ["Hips sagging or piking", "Shrugging shoulders to ears"],
    ["Stop if lumbar pain; regress to knees or shorter holds."],
    ["Acute lumbar pain with extension bias"],
    [("dead-bug", "Dynamic brace with less load")],
    [("side-plank", "Lateral"), ("ab-wheel-rollout", "Harder anti-extension")],
    [REF_ACSM_GARBER, REF_NSCA_ESSENTIALS],
)

ex(
    "side-plank", "Side plank", "Бічна планка",
    "strength", "core_brace",
    ["obliques", "gluteus_medius"], ["transverse_abdominis"],
    ["bodyweight"], "beginner", True, "isometric", "hold",
    ["On one forearm/side of foot, lift hips to a straight line; hold; switch."],
    ["Elbow under shoulder; stacked feet or staggered."],
    "Breathe steadily.",
    ["Hips rotating forward/back", "Collapsed shoulder"],
    ["Regress from knees if needed."],
    ["Shoulder pain on the support side"],
    [("plank", "Front plank")],
    [],
    [REF_ACSM_GARBER],
)

ex(
    "dead-bug", "Dead bug", "Мертвий жук",
    "strength", "core_brace",
    ["transverse_abdominis", "rectus_abdominis"], ["hip_flexors"],
    ["bodyweight"], "beginner", True, "isolation", "reps",
    ["Supine; opposite arm and leg extend while low back stays gently pressed toward floor; alternate."],
    ["Knees over hips to start; exhale on extension."],
    "Exhale as limbs extend; inhale to return.",
    ["Arching the low back", "Moving both limbs on the same side unintentionally"],
    ["Reduce ROM to keep spine quiet."],
    [],
    [],
    [("plank", "Isometric"), ("bird-dog", "Quadruped")],
    [REF_ACSM_GARBER, REF_NSCA_ESSENTIALS],
)

ex(
    "bird-dog", "Bird dog", "Птах-собака",
    "strength", "core_brace",
    ["erector_spinae", "gluteus_maximus", "transverse_abdominis"], ["rear_deltoid"],
    ["bodyweight"], "beginner", True, "isolation", "reps",
    ["Quadruped; extend opposite arm and leg without rotating hips; return and switch."],
    ["Hands under shoulders; knees under hips."],
    "Exhale on extension.",
    ["Lumbar sag", "Hiking the hip"],
    ["Move slowly; think long spine."],
    [],
    [],
    [("dead-bug", "Supine"), ("plank", "Hold")],
    [REF_ACSM_GARBER],
)

ex(
    "cable-woodchop", "Cable woodchop", "Дровокол на блоці",
    "strength", "rotation",
    ["obliques"], ["gluteus_maximus", "front_deltoid"],
    ["cable"], "beginner", True, "compound", "weight_reps",
    ["Pull the handle across the body in a diagonal while rotating through the hips/torso with controlled arms."],
    ["High-to-low or low-to-high; feet stable."],
    "Exhale on the chop.",
    ["Twisting only the lumbar spine", "Using arms alone"],
    ["Rotate through thoracic/hips; keep knees soft."],
    ["Acute discogenic pain with rotation"],
    [("pallof-press", "Anti-rotation")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "pallof-press", "Pallof press", "Жим Паллофа",
    "strength", "rotation",
    ["obliques", "transverse_abdominis"], ["front_deltoid"],
    ["cable", "bands"], "beginner", True, "isolation", "reps",
    ["Stand side-on to a cable/band; press the handle straight out, resisting rotation; hold briefly; return."],
    ["Handle at sternum; athletic stance."],
    "Exhale as you press out.",
    ["Letting the cable turn the hips", "Shrugging"],
    ["Anti-rotation > chasing heavy loads."],
    [],
    [],
    [("cable-woodchop", "Dynamic rotation")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "ab-wheel-rollout", "Ab-wheel rollout", "Прокатка з роликом",
    "strength", "core_brace",
    ["rectus_abdominis", "transverse_abdominis", "latissimus_dorsi"], ["front_deltoid"],
    ["bodyweight"], "advanced", False, "compound", "reps",
    ["From knees, roll forward while keeping ribs down; pull back without lumbar sag."],
    ["Wheel under shoulders to start; modest ROM first."],
    "Exhale to return; do not dump into a breath-hold panic.",
    ["Lumbar extension sag", "Going too far too soon"],
    ["Master dead bugs/planks first."],
    ["Acute lumbar pain"],
    [("plank", "Isometric anti-extension"), ("dead-bug", "Regression")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "hanging-knee-raise", "Hanging knee raise", "Підйом колін у висі",
    "strength", "core_brace",
    ["rectus_abdominis", "hip_flexors"], ["forearms"],
    ["pull_up_bar"], "intermediate", False, "isolation", "reps",
    ["Hang; tilt pelvis slightly; raise knees toward chest; lower without swinging wildly."],
    ["Active hang; avoid excessive swing."],
    "Exhale to raise.",
    ["Pure swinging momentum", "Shrugging into neck pain"],
    ["Use arm straps only if grip limits you and grip is not the goal."],
    ["Shoulder pain in hang"],
    [("dead-bug", "Floor regression")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "russian-twist", "Russian twist (bodyweight)", "Скручування російське (вага тіла)",
    "strength", "rotation",
    ["obliques", "rectus_abdominis"], [],
    ["bodyweight"], "beginner", False, "isolation", "reps",
    ["Seated lean-back with feet on floor; rotate torso side to side with control."],
    ["Keep chest up; optional light medicine ball later."],
    "Exhale each rotation.",
    ["Rounding and yanking the lumbar spine", "Moving only the arms"],
    ["Feet down is fine; avoid if rotation aggravates symptoms."],
    ["Discogenic pain with rotation"],
    [("pallof-press", "Anti-rotation alternative")],
    [("cable-woodchop", "Loaded diagonal")],
    [REF_NSCA_ESSENTIALS],
)

# More accessories / calves / machines
ex(
    "leg-curl-machine", "Lying/seated leg curl", "Згинання ніг у тренажері",
    "strength", "hinge",
    ["hamstrings"], ["calves"],
    ["machine"], "beginner", False, "isolation", "weight_reps",
    ["Curl the pad by flexing the knees; control the return."],
    ["Adjust machine to fit limb length."],
    "Exhale on the curl.",
    ["Lifting hips to cheat", "Partial ROM without intent"],
    ["Full controlled ROM preferred."],
    ["Acute hamstring strain"],
    [("romanian-deadlift-dumbbell", "Hip-dominant hamstrings")],
    [("nordic-curl-assisted", "Eccentric-biased")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "nordic-curl-assisted", "Nordic curl (assisted)", "Північне згинання (з допомогою)",
    "strength", "hinge",
    ["hamstrings"], ["gluteus_maximus"],
    ["bodyweight", "bands"], "advanced", False, "isolation", "reps",
    ["Kneel, ankles fixed; lower torso forward slowly using hamstrings; assist with hands/band to return."],
    ["Pad knees; partner or stable anchor for ankles."],
    "Steady breath; brace lightly.",
    ["Losing hip extension and folding at the hips", "Dropping uncontrolled"],
    ["High eccentric demand — progress cautiously."],
    ["Hamstring tendinopathy acute"],
    [("leg-curl-machine", "Machine isolation")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "leg-extension-machine", "Leg extension", "Розгинання ніг у тренажері",
    "strength", "squat",
    ["quadriceps"], [],
    ["machine"], "beginner", False, "isolation", "weight_reps",
    ["Extend the knees against the pad; lower under control."],
    ["Align knee joint with machine axis."],
    "Exhale to extend.",
    ["Using momentum", "Hyperextending painfully"],
    ["Useful accessory; not a full substitute for squats."],
    ["Painful PF joint loading — reduce ROM/load"],
    [("sit-to-stand", "Functional pattern")],
    [("back-squat", "Compound")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "calf-raise-standing", "Standing calf raise", "Підйом на носки стоячи",
    "strength", "other",
    ["calves"], [],
    ["machine", "bodyweight", "dumbbell"], "beginner", False, "isolation", "weight_reps",
    ["Rise onto the forefoot through a full ankle ROM; lower to a stretch."],
    ["Straight knees for gastroc emphasis; soft knees shift soleus."],
    "Exhale to rise.",
    ["Bouncing", "Partial ROM"],
    ["Control the eccentric for tendon health goals."],
    ["Acute Achilles pain — clinician guidance"],
    [],
    [],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

ex(
    "hip-abduction-machine", "Hip abduction machine", "Відведення стегна в тренажері",
    "strength", "other",
    ["gluteus_medius"], ["gluteus_maximus"],
    ["machine"], "beginner", False, "isolation", "weight_reps",
    ["Push the pads outward against resistance; return slow."],
    ["Upright torso; controlled ROM."],
    "Exhale to abduct.",
    ["Using momentum", "Excess lumbar movement"],
    ["Accessory for hip stability."],
    [],
    [("side-plank", "Anti-gravity lateral")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "hip-adduction-machine", "Hip adduction machine", "Приведення стегна в тренажері",
    "strength", "other",
    ["adductors"], [],
    ["machine"], "beginner", False, "isolation", "weight_reps",
    ["Squeeze pads inward; control the return."],
    ["Comfortable ROM only."],
    "Exhale to adduct.",
    ["Forcing extreme stretch"],
    ["Useful accessory; not mandatory for all."],
    ["Acute groin strain"],
    [],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "back-extension-roman", "Back extension (Roman chair)", "Гіперекстензія",
    "strength", "hinge",
    ["erector_spinae", "gluteus_maximus", "hamstrings"], [],
    ["machine", "bodyweight"], "beginner", False, "isolation", "reps",
    ["From hinged position, extend hips/trunk to a neutral finish without hyperextending; lower with control."],
    ["Pad at hips; arms across chest."],
    "Exhale to extend.",
    ["Overarching at the top", "Using arms to yank"],
    ["Think hip extension more than lumbar cranking."],
    ["Acute lumbar pain"],
    [("bird-dog", "Lower load"), ("glute-bridge", "Floor")],
    [("good-morning", "Loaded hinge")],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "shrug-dumbbell", "Dumbbell shrug", "Шраги з гантелями",
    "strength", "pull",
    ["trapezius"], ["forearms"],
    ["dumbbell"], "beginner", False, "isolation", "weight_reps",
    ["Stand tall; elevate shoulders toward ears; lower slow; minimal elbow bend."],
    ["Arms long; neck neutral."],
    "Exhale to shrug.",
    ["Rolling the shoulders aggressively", "Jutting the chin"],
    ["Simple vertical shrug is enough."],
    ["Neck pain"],
    [],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "wrist-curl", "Wrist curl", "Згинання зап'ясток",
    "strength", "pull",
    ["forearms"], [],
    ["dumbbell", "barbell"], "beginner", False, "isolation", "weight_reps",
    ["Forearms on thighs/bench; curl the load using the wrists; lower to stretch."],
    ["Light load; controlled tempo."],
    "Exhale to curl.",
    ["Using the arms to swing"],
    ["Stop with tendon pain."],
    ["Acute wrist tendinopathy"],
    [],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "band-pull-apart", "Band pull-apart", "Розведення гуми перед собою",
    "strength", "pull",
    ["rear_deltoid", "rhomboids"], ["trapezius"],
    ["bands"], "beginner", False, "isolation", "reps",
    ["Arms forward holding a band; pull apart until band touches chest; return slow."],
    ["Soft elbows; ribs down."],
    "Exhale on the pull-apart.",
    ["Shrugging", "Bending elbows into a row"],
    ["Excellent warm-up / posture accessory."],
    [],
    [],
    [("face-pull", "Cable/band face pull")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

ex(
    "goblet-lateral-squat", "Goblet lateral squat", "Бічний присід з гирею/гантеллю",
    "strength", "lunge",
    ["quadriceps", "adductors", "gluteus_maximus"], ["gluteus_medius"],
    ["kettlebell", "dumbbell"], "intermediate", True, "compound", "weight_reps",
    ["Hold a goblet load; shift into one hip like a lateral lunge while the other leg stays long; alternate."],
    ["Wide stance; heels down."],
    "Exhale to return to centre.",
    ["Collapsing chest", "Twisting the foot of the straight leg"],
    ["Combines squat & frontal plane mobility."],
    ["Groin irritation"],
    [("lateral-lunge", "Stepping version"), ("goblet-squat", "Sagittal")],
    [],
    [REF_NSCA_ESSENTIALS],
)

# Count strength so far - I'll add a few more to reach ~80
ex(
    "machine-chest-press", "Machine chest press", "Жим від грудей у тренажері",
    "strength", "push",
    ["pectoralis_major", "triceps", "front_deltoid"], [],
    ["machine"], "beginner", False, "compound", "weight_reps",
    ["Press handles to extension without slamming; return to a controlled stretch."],
    ["Seat so handles align with mid-chest."],
    "Exhale to press.",
    ["Shrugging", "Uneven lockout"],
    ["Good learning tool before free weights."],
    [],
    [("push-up", "Bodyweight"), ("dumbbell-bench-press", "Free weights")],
    [("barbell-bench-press", "Barbell")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

ex(
    "machine-shoulder-press", "Machine shoulder press", "Жим від плечей у тренажері",
    "strength", "push",
    ["front_deltoid", "triceps"], ["lateral_deltoid", "trapezius"],
    ["machine"], "beginner", False, "compound", "weight_reps",
    ["Press overhead on a machine path; lower with control."],
    ["Seat height so handles start near shoulders."],
    "Exhale to press.",
    ["Overarching lumbar spine"],
    ["Keep ribs down."],
    ["Shoulder pain overhead"],
    [("landmine-press", "Angled free"), ("overhead-press-dumbbell", "Free")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "pec-deck", "Pec deck", "Бабочка (пек-дек)",
    "strength", "push",
    ["pectoralis_major"], ["front_deltoid"],
    ["machine"], "beginner", False, "isolation", "weight_reps",
    ["Bring arms together on the arc; return slowly without losing shoulder position."],
    ["Seat adjusted; soft elbows."],
    "Exhale on the squeeze.",
    ["Excess stretch with pain"],
    ["Use a comfortable ROM."],
    ["Acute pec/shoulder strain"],
    [("cable-chest-fly", "Cable"), ("push-up", "Compound")],
    [],
    [REF_NSCA_ESSENTIALS],
)

ex(
    "smith-machine-squat", "Smith-machine squat", "Присід у машині Сміта",
    "strength", "squat",
    ["quadriceps", "gluteus_maximus"], ["hamstrings"],
    ["machine"], "beginner", False, "compound", "weight_reps",
    ["Bar on a fixed path; squat with feet placed so knees track well; use safeties."],
    ["Feet slightly forward of a free squat for many users."],
    "Inhale down, exhale up.",
    ["Feet too far under bar causing awkward path"],
    ["Fixed path ≠ free squat transfer; still useful."],
    [],
    [("goblet-squat", "Free pattern"), ("leg-press", "Machine legs")],
    [("back-squat", "Free barbell")],
    [REF_ESCAMILLA_SQUAT, REF_NSCA_ESSENTIALS],
)

ex(
    "step-up", "Step-up", "Захін на платформу",
    "strength", "lunge",
    ["quadriceps", "gluteus_maximus"], ["gluteus_medius", "calves"],
    ["box", "dumbbell", "bodyweight"], "beginner", True, "compound", "reps",
    ["Step onto a box with the whole foot; stand tall; step down with control."],
    ["Box height where knee comfort allows; no push-off cheating from the trail leg."],
    "Exhale to step up.",
    ["Pushing mostly with the bottom leg", "Knee cave"],
    ["Start low; hold rail if needed."],
    ["Acute knee pain"],
    [("sit-to-stand", "Regression"), ("split-squat-bodyweight", "Static")],
    [("bulgarian-split-squat", "More demand")],
    [REF_NSCA_ESSENTIALS, REF_ACSM_GARBER],
)

print(f"Strength count so far: {sum(1 for e in exercises if e['discipline']=='strength')}")

# ---------------------------------------------------------------------------
# YOGA (~15)
# ---------------------------------------------------------------------------
yoga = [
    ("downward-dog", "Downward-facing dog", "Собака мордою вниз", "mobility",
     ["hamstrings", "calves", "latissimus_dorsi"], ["shoulders".replace("shoulders", "front_deltoid")],
     ["Press hips up and back into an inverted V; pedaling heels optional; long spine."],
     ["Hands shoulder-width; feet hip-width; yoga mat."],
     "Steady nasal breathing.",
     ["Collapsed shoulders at ears", "Rounding lumbar aggressively"],
     ["Bend knees if hamstrings are tight."],
     ["Acute wrist pain — use fists/forearms variants"],
     [("child-pose", "Rest"), ("cat-cow", "Spinal warm-up")],
     [("three-legged-dog", "Lift one leg")]),
    ("child-pose", "Child's pose", "Поза дитини", "mobility",
     ["latissimus_dorsi", "erector_spinae"], ["gluteus_maximus"],
     ["Kneel, sit toward heels, fold torso forward, arms long or by sides; breathe."],
     ["Knees together or wide; support with a blanket if needed."],
     "Slow nasal breaths.",
     ["Forcing hips to heels with knee pain"],
     ["Resting pose — exit if dizzy."],
     ["Knee pain kneeling"],
     [],
     [("downward-dog", "More active stretch")]),
    ("cat-cow", "Cat-cow", "Кішка-корова", "mobility",
     ["erector_spinae"], ["rectus_abdominis"],
     ["Quadruped; alternate spinal flexion (cat) and extension (cow) with the breath."],
     ["Hands under shoulders; knees under hips."],
     "Inhale cow, exhale cat (common cue).",
     ["Dumping into end-range pain"],
     ["Move within comfort."],
     [],
     [],
     [("bird-dog", "Add limb reach")]),
    ("mountain-pose", "Mountain pose", "Тадасана (гора)", "balance",
     ["full_body"], [],
     ["Stand tall, feet together or hip-width, distribute weight evenly, arms by sides, long crown."],
     ["Yoga mat; soft gaze."],
     "Natural breathing.",
     ["Locked knees hyperextended painfully", "Ribs flaring"],
     ["Foundation pose for standing work."],
     [],
     [],
     [("tree-pose", "Balance progression")]),
    ("tree-pose", "Tree pose", "Поза дерева", "balance",
     ["gluteus_medius", "calves"], ["full_body"],
     ["Stand on one leg; place other foot on ankle/calf/inner thigh (not the knee); hands at heart or overhead."],
     ["Near a wall if needed."],
     "Steady breaths.",
     ["Pressing into the side of the knee joint"],
     ["Use wall support; gaze fixed."],
     ["Acute ankle sprain"],
     [("mountain-pose", "Two feet")],
     []),
    ("warrior-i", "Warrior I", "Воїн I", "lunge",
     ["quadriceps", "gluteus_maximus", "hip_flexors"], ["front_deltoid"],
     ["Lunge with back foot angled, hips squared forward, arms reach up."],
     ["Front knee tracks over mid-foot."],
     "Steady breathing.",
     ["Front knee cave", "Overarching lumbar"],
     ["Shorten stance if needed."],
     [],
     [("mountain-pose", "Reset"), ("low-lunge", "More hip flexor stretch")],
     [("warrior-ii", "Open hips variant")]),
    ("warrior-ii", "Warrior II", "Воїн II", "lunge",
     ["quadriceps", "gluteus_medius", "adductors"], ["lateral_deltoid"],
     ["Wide stance; front knee bent; arms reach front-back at shoulder height; gaze over front hand."],
     ["Back foot parallel-ish to short edge of mat."],
     "Steady breathing.",
     ["Front knee drifting in", "Collapsed chest"],
     ["Keep weight in both feet."],
     [],
     [("warrior-i", "Squared hips")],
     [("triangle-pose", "Straight front leg")]),
    ("triangle-pose", "Triangle pose", "Трикутник", "mobility",
     ["hamstrings", "adductors", "obliques"], ["calves"],
     ["From wide stance, hinge and reach front hand to shin/block/floor; top arm stacks; long spine."],
     ["Use a block under the bottom hand."],
     "Breathe into the side body.",
     ["Rounding to touch the floor at all costs"],
     ["Prioritise length over depth."],
     ["Discogenic pain with side bend — modify"],
     [("warrior-ii", "Bent-knee base")],
     []),
    ("bridge-pose-yoga", "Bridge pose", "Місток (йога)", "hinge",
     ["gluteus_maximus", "hamstrings"], ["erector_spinae"],
     ["Supine; feet flat; lift hips; optional clasp hands; lower vertebra by vertebra."],
     ["Feet under knees; yoga mat."],
     "Inhale to lift or breathe steadily in the hold.",
     ["Clenching lumbar instead of glutes"],
     ["Same family as glute bridge with yoga pacing."],
     [],
     [("glute-bridge", "Strength framing"), ("child-pose", "Counterpose")],
     [("wheel-pose", "Deeper backbend — advanced")]),
    ("wheel-pose", "Wheel pose", "Колесо (чакрасана)", "mobility",
     ["pectoralis_major", "hip_flexors", "erector_spinae"], ["triceps", "gluteus_maximus"],
     ["From bridge preparation, press into hands by ears and lift into a full backbend if appropriate."],
     ["Only with adequate shoulder/spine preparation."],
     "Do not breath-hold; exit carefully.",
     ["Forcing into lumbar pain", "Collapsed shoulders"],
     ["Advanced; skip if unsure — use bridge instead."],
     ["Uncontrolled hypertension", "Shoulder instability", "Pregnancy without specialist advice"],
     [("bridge-pose-yoga", "Regression")],
     []),
    ("seated-forward-fold", "Seated forward fold", "Нахил уперед сидячи", "mobility",
     ["hamstrings", "erector_spinae"], ["calves"],
     ["Sit tall; hinge from hips; reach toward feet; keep spine long; bend knees as needed."],
     ["Strap around feet optional."],
     "Exhale to fold gently; never bounce.",
     ["Rounding hard to grab feet"],
     ["Bent knees are a valid pose."],
     ["Acute disc herniation symptoms — clinical advice"],
     [("downward-dog", "Standing hinge stretch")],
     []),
    ("low-lunge", "Low lunge", "Низький випад", "lunge",
     ["hip_flexors", "quadriceps"], ["gluteus_maximus"],
     ["Kneeling lunge with hands on front thigh or mat; ease hips forward for hip-flexor stretch."],
     ["Pad the back knee."],
     "Steady breathing.",
     ["Dumping into lumbar extension"],
     ["Posterior pelvic tilt gently to target hip flexors."],
     ["Knee pain kneeling"],
     [("warrior-i", "Standing")],
     [("pigeon-pose", "Deeper hip opener")]),
    ("pigeon-pose", "Pigeon pose", "Голуб", "mobility",
     ["gluteus_maximus", "hip_flexors"], ["adductors"],
     ["Front leg flexed in front with shin angled as comfortable; back leg extended; fold forward if appropriate."],
     ["Use props under hip; never force the front knee."],
     "Slow breaths.",
     ["Torqueing the front knee", "Forcing the fold"],
     ["Figure-four on back is a safer alternative for many."],
     ["Knee ligament issues — prefer supine figure-four"],
     [("low-lunge", "Less external rotation demand")],
     []),
    ("worlds-greatest-stretch", "World's greatest stretch", "Комбінована мобільність у випаді", "mobility",
     ["hip_flexors", "hamstrings", "obliques", "thoracic".replace("thoracic", "erector_spinae")],
     ["adductors"],
     ["From a lunge, hand inside front foot; rotate and reach that arm up; optional hamstring straighten."],
     ["Dynamic warm-up pacing."],
     "Exhale into rotation.",
     ["Forcing end ranges cold"],
     ["Great pre-run / pre-lift opener."],
     [],
     [("low-lunge", "Static"), ("cat-cow", "Spinal prep")],
     []),
    ("three-legged-dog", "Three-legged dog", "Собака на трьох кінцівках", "balance",
     ["hamstrings", "gluteus_maximus", "shoulders".replace("shoulders", "front_deltoid")],
     ["calves"],
     ["From downward dog, lift one leg high without rotating the hips open unless intended; switch."],
     ["Strong hands; soft standing knee if needed."],
     "Steady breathing.",
     ["Collapsing the standing shoulder"],
     ["Keep pelvis relatively square for strength focus."],
     ["Wrist pain"],
     [("downward-dog", "Two feet")],
     []),
]

for eid, en, ukn, pattern, primary, secondary, tech, setup, breath, mist, saf, contra, reg, prog in yoga:
    # fix any bad muscle names
    secondary = [m for m in secondary if m in {
        "quadriceps","hamstrings","gluteus_maximus","gluteus_medius","calves","hip_flexors","adductors",
        "pectoralis_major","latissimus_dorsi","trapezius","rhomboids","rear_deltoid","front_deltoid",
        "lateral_deltoid","biceps","triceps","forearms","rectus_abdominis","obliques","transverse_abdominis",
        "erector_spinae","full_body","rotator_cuff","serratus_anterior","tibialis_anterior","neck"
    }]
    primary = [m for m in primary if m in secondary or True]
    primary = [m for m in primary if not m.endswith("replace") and "replace" not in m]
    ex(
        eid, en, ukn, "yoga", pattern, primary, secondary,
        ["yoga_mat", "bodyweight"], "beginner" if eid not in ("wheel-pose", "three-legged-dog") else "advanced" if eid == "wheel-pose" else "intermediate",
        eid in ("tree-pose", "three-legged-dog", "pigeon-pose", "low-lunge", "warrior-i"),
        "skill" if pattern in ("balance", "mobility") else "compound",
        "hold" if pattern in ("balance", "mobility") else "reps",
        tech, setup, breath, mist, saf, contra,
        reg, prog,
        [REF_ACSM_GARBER],
        tags=["yoga"],
    )

# Fix yoga levels manually for a few
for e in exercises:
    if e["id"] == "wheel-pose":
        e["level"] = "advanced"
        e["mechanics"] = "skill"
        e["trackingType"] = "hold"
    if e["id"] in ("tree-pose", "three-legged-dog"):
        e["level"] = "intermediate"
        e["trackingType"] = "hold"

# ---------------------------------------------------------------------------
# PILATES (~10)
# ---------------------------------------------------------------------------
pilates = [
    ("pilates-hundred", "The hundred", "Сотня (пілатес)",
     ["rectus_abdominis", "transverse_abdominis"], ["hip_flexors"],
     ["Curl head/shoulders up, legs in tabletop or extended, pump arms while breathing in 5 / out 5 for ~100 counts."],
     ["Imprinted or neutral pelvis per school; neck supported if needed."],
     "Inhale 5 pumps, exhale 5 pumps.",
     ["Neck strain from jutting chin", "Holding breath"],
     ["Keep head down if neck fatigues; bend knees."],
     ["Neck injury", "Late pregnancy without specialist advice"],
     [("dead-bug", "Core without neck load")],
     [("pilates-roll-up", "More flexion challenge")]),
    ("pilates-roll-up", "Roll-up", "Перекат уверх",
     ["rectus_abdominis"], ["hip_flexors"],
     ["From supine, articulate spine up to seated reach, then roll down with control."],
     ["Arms overhead to start; legs long or soft knees."],
     "Exhale to roll up; inhale at top; exhale to roll down.",
     ["Using momentum", "Yanking the neck"],
     ["Bend knees / use a strap if needed."],
     ["Acute discogenic pain"],
     [("pilates-hundred", "Less ROM")],
     []),
    ("pilates-single-leg-stretch", "Single-leg stretch", "Розтяг однієї ноги",
     ["rectus_abdominis", "transverse_abdominis"], ["hip_flexors"],
     ["Head curled up; alternate pulling one knee in while extending the other leg."],
     ["Pelvis stable; lower abdominals engaged."],
     "Coordinate breath with switches.",
     ["Rocking pelvis", "Neck tension"],
     ["Keep extended leg higher to make it easier."],
     [],
     [("dead-bug", "Head down")],
     [("pilates-double-leg-stretch", "Both legs")]),
    ("pilates-double-leg-stretch", "Double-leg stretch", "Розтяг двох ніг",
     ["rectus_abdominis", "transverse_abdominis"], [],
     ["Knees in, arms to ankles; extend arms and legs away; circle arms to gather again."],
     ["Maintain curl and lumbar control."],
     "Inhale extend, exhale gather (common cue).",
     ["Lumbar arching on the extend"],
     ["Reduce extension ROM to keep back quiet."],
     [],
     [("pilates-single-leg-stretch", "Easier")],
     []),
    ("pilates-shoulder-bridge", "Shoulder bridge", "Плечовий місток (пілатес)",
     ["gluteus_maximus", "hamstrings"], ["erector_spinae"],
     ["Articulate hips up into bridge; optional leg lifts; roll down."],
     ["Feet hip-width; arms long."],
     "Inhale to prepare; exhale to articulate up.",
     ["Dumping into lumbar"],
     ["Similar to glute bridge with articulation focus."],
     [],
     [("glute-bridge", "Strength cueing")],
     [("bridge-pose-yoga", "Yoga hold")]),
    ("pilates-side-kick", "Side-kick series", "Бічні піки",
     ["gluteus_medius", "obliques"], ["quadriceps"],
     ["Lie on side; prop head; kick top leg forward/back with control while torso stays still."],
     ["Body in one long line; hand for support."],
     "Exhale on effort phases.",
     ["Rolling the torso back", "Swinging wildly"],
     ["Small range first."],
     ["Hip labral pain — modify"],
     [("side-plank", "Isometric lateral")],
     []),
    ("pilates-swan", "Swan", "Лебідь",
     ["erector_spinae", "gluteus_maximus"], ["hamstrings"],
     ["Prone; lift chest using back extensors with legs long; lower with control."],
     ["Hands under shoulders; pubic bone heavy."],
     "Inhale to lift, exhale to lower (common).",
     ["Crunching lumbar painfully", "Hiking shoulders"],
     ["Keep lift modest."],
     ["Facetogenic extension pain"],
     [("bird-dog", "Less extension load"), ("back-extension-roman", "Strength variant")],
     []),
    ("pilates-leg-circles", "Leg circles", "Кола ногою",
     ["hip_flexors", "gluteus_medius", "transverse_abdominis"], [],
     ["Supine; one leg extended to ceiling; draw controlled circles without rocking the pelvis."],
     ["Other leg long or bent; hands by sides."],
     "Steady breathing.",
     ["Pelvis rocking", "Large uncontrolled circles"],
     ["Shrink the circle to keep pelvis quiet."],
     [],
     [("dead-bug", "Alternating limbs")],
     []),
    ("pilates-saw", "Saw", "Пила",
     ["obliques", "hamstrings"], ["erector_spinae"],
     ["Seated wide legs; rotate and reach little finger toward opposite foot in a pulsing reach; switch."],
     ["Tall seat bones; soft knees OK."],
     "Exhale to reach/rotate.",
     ["Collapsing the spine to reach"],
     ["Prioritise rotation + length."],
     ["Discogenic pain with flexion-rotation"],
     [("seated-forward-fold", "Sagittal fold"), ("pallof-press", "Anti-rotation strength")],
     []),
    ("pilates-plank-variations", "Pilates plank / leg-pull prep", "Планка в стилі пілатес",
     ["transverse_abdominis", "rectus_abdominis", "front_deltoid"], ["gluteus_maximus"],
     ["Hold a strong plank with Pilates breathing; optional small leg lifts if stable."],
     ["Hands under shoulders; heels back."],
     "Posterior lateral breathing cues common in Pilates.",
     ["Hips sagging", "Breath holding"],
     ["Same safety as strength plank."],
     [],
     [("plank", "Strength plank"), ("dead-bug", "Regression")],
     [("ab-wheel-rollout", "Harder anti-extension")]),
]

for row in pilates:
    eid, en, ukn, primary, secondary, tech, setup, breath, mist, saf, contra, reg, prog = row
    ex(
        eid, en, ukn, "pilates", "core_brace", primary, secondary,
        ["pilates_mat", "yoga_mat", "bodyweight"], "beginner" if "swan" not in eid else "intermediate",
        "single" in eid or "side" in eid or "leg-circles" in eid,
        "isolation", "reps" if "hundred" not in eid and "plank" not in eid else ("timed" if "hundred" in eid else "hold"),
        [tech], [setup], breath, [mist], [saf], contra, reg, prog,
        [REF_ACSM_GARBER], tags=["pilates"],
    )

# ---------------------------------------------------------------------------
# SWIMMING (~8)
# ---------------------------------------------------------------------------
swim = [
    ("freestyle-stroke", "Freestyle (front crawl)", "Кроль на грудях", "swim_stroke",
     ["latissimus_dorsi", "full_body", "delts".replace("delts", "rear_deltoid")],
     ["triceps", "quadriceps", "calves"],
     ["Alternate arm pulls with body roll; flutter kick; face in water with rhythmic breathing to the side."],
     ["Lane space; comfortable pace; goggles."],
     "Exhale underwater; inhale on the turn of the head.",
     ["Crossing midline wildly", "Holding breath too long", "Dropped elbow catch without intent"],
     ["Build volume gradually; stop with shoulder pain."],
     ["Uncontrolled seizure disorders in water unsupervised"],
     [("catch-up-drill", "Timing drill"), ("kickboard-kick", "Legs only")],
     [("paddle-freestyle", "Add resistance")]),
    ("backstroke", "Backstroke", "Кроль на спині", "swim_stroke",
     ["latissimus_dorsi", "rear_deltoid"], ["quadriceps", "trapezius"],
     ["Float on back; alternate continuous arm circles with steady flutter kick; head neutral."],
     ["Know lane lines; careful at walls."],
     "Steady rhythmic breathing (face clear of water).",
     ["Sitting hips low", "Crossing arms over midline excessively"],
     ["Watch for wall collisions."],
     [],
     [("kickboard-kick", "Kick focus")],
     []),
    ("breaststroke", "Breaststroke", "Брас", "swim_stroke",
     ["pectoralis_major", "adductors", "gluteus_maximus"], ["quadriceps", "triceps"],
     ["Pull-breathe-kick-glide timing; whip kick with knees tracking carefully; glide each cycle."],
     ["Legal whip kick practice; neck comfortable."],
     "Inhale during pull; exhale in glide/kick.",
     ["Incomplete glide", "Knee flare that stresses the joint"],
     ["Mind knee comfort on whip kick."],
     ["Medial knee pain with whip kick — modify"],
     [("pull-buoy-pull", "Upper body focus")],
     []),
    ("butterfly-stroke", "Butterfly", "Баттерфляй", "swim_stroke",
     ["latissimus_dorsi", "pectoralis_major", "full_body"], ["triceps", "calves"],
     ["Simultaneous arm pulls with dolphin kick waves; breathe forward sparingly."],
     ["High energy cost — short repeats first."],
     "Exhale in water; quick inhale forward.",
     ["Two separate kicks poorly timed", "Late breath causing sinking"],
     ["Advanced; prioritise drills before yardage."],
     ["Shoulder pain"],
     [("catch-up-drill", "Freestyle timing first"), ("kickboard-kick", "Dolphin on board")],
     []),
    ("catch-up-drill", "Catch-up drill", "Вправа catch-up", "swim_drill",
     ["latissimus_dorsi", "rear_deltoid"], ["triceps"],
     ["Freestyle where one hand waits extended until the other hand catches up; promotes length and timing."],
     ["Snorkel optional; focus on quiet entry."],
     "Match freestyle breathing pattern or use snorkel.",
     ["Pausing so long that hips sink"],
     ["Technique over speed."],
     [],
     [("freestyle-stroke", "Full stroke")],
     [("paddle-freestyle", "Load the pull")]),
    ("kickboard-kick", "Kickboard flutter/dolphin kick", "Робота ніг з дошкою", "swim_drill",
     ["quadriceps", "calves", "hip_flexors"], ["gluteus_maximus"],
     ["Hold a kickboard; propel with flutter or dolphin kicks; keep kick from the hips."],
     ["Board extended; relaxed ankles."],
     "Blow bubbles steadily.",
     ["Bending only at the knees", "Tense ankles"],
     ["Useful aerobic + technique tool."],
     [],
     [],
     [("freestyle-stroke", "Full stroke")]),
    ("pull-buoy-pull", "Pull buoy freestyle", "Кроль з колобашкою", "swim_drill",
     ["latissimus_dorsi", "pectoralis_major", "triceps"], [],
     ["Place pull buoy between thighs; swim freestyle emphasising the pull without kick."],
     ["Buoy secure; optional paddles later."],
     "Normal freestyle breathing.",
     ["Overreaching with poor catch", "Lumbar sway"],
     ["Good for upper-body aerobic work."],
     ["Shoulder irritation — reduce paddles/intensity"],
     [("freestyle-stroke", "Full stroke")],
     [("paddle-freestyle", "Add paddles")]),
    ("paddle-freestyle", "Paddle freestyle", "Кроль з лопатками", "swim_drill",
     ["latissimus_dorsi", "triceps"], ["rear_deltoid"],
     ["Freestyle with hand paddles to increase pull resistance; shorten session volume."],
     ["Paddles fitted; consider buoy."],
     "Do not breath-hold under higher load.",
     ["Shoulders hiking", "Forcing volume with paddles"],
     ["Paddles increase shoulder load — progress carefully."],
     ["Shoulder pain"],
     [("pull-buoy-pull", "No paddles"), ("catch-up-drill", "Technique")],
     []),
]

for row in swim:
    eid, en, ukn, pattern, primary, secondary, tech, setup, breath, mist, saf, contra, reg, prog = row
    primary = [p for p in primary if "replace" not in p]
    secondary = [p for p in secondary if "replace" not in p]
    equip = ["pool"]
    if "kickboard" in eid:
        equip.append("kickboard")
    if "pull-buoy" in eid:
        equip.append("pull_buoy")
    if "paddle" in eid:
        equip += ["paddle", "pull_buoy"]
    ex(
        eid, en, ukn, "swimming", pattern, primary, secondary, equip,
        "advanced" if eid == "butterfly-stroke" else "beginner" if "drill" in pattern or eid in ("kickboard-kick", "catch-up-drill") else "intermediate",
        False, "skill", "distance" if "stroke" in pattern else "distance",
        [tech], [setup], breath, [mist], [saf], contra, reg, prog,
        [REF_ACSM_GARBER, REF_SEILER], tags=["swim"],
    )

# ---------------------------------------------------------------------------
# RUNNING (~7)
# ---------------------------------------------------------------------------
run = [
    ("easy-run", "Easy run", "Легкий біг", "gait",
     ["quadriceps", "hamstrings", "calves", "gluteus_maximus"], ["tibialis_anterior", "hip_flexors"],
     ["Run at a conversational pace (roughly easy aerobic); relaxed shoulders; cadence comfortable."],
     ["Supportive shoes; safe route or treadmill."],
     "Rhythmic breathing; nasal+mouth as needed." ,
     ["Starting too fast", "Overstriding with hard heel braking"],
     ["Most weekly volume should feel easy in polarised models — descriptive, not dogma."],
     ["Chest pain / uncontrolled medical red flags — seek care"],
     [("brisk-walk", "If run not ready")],
     [("tempo-run", "Higher intensity"), ("interval-run", "Intervals")]),
    ("brisk-walk", "Brisk walk", "Швидка хода", "gait",
     ["gluteus_maximus", "quadriceps", "calves"], ["hip_flexors"],
     ["Walk with purposeful pace and arm swing; upright posture."],
     ["Flat route to start."],
     "Comfortable breathing.",
     ["Slouching", "Overstriding"],
     ["Excellent entry aerobic work and active recovery."],
     [],
     [],
     [("easy-run", "Progress to jog")]),
    ("tempo-run", "Tempo run", "Темповий біг", "gait",
     ["quadriceps", "hamstrings", "calves"], ["gluteus_maximus"],
     ["Sustained harder effort near threshold for a controlled block after warm-up; not an all-out race."],
     ["Warm up thoroughly; cool down after."],
     "Harder breathing but sustainable short phrases.",
     ["Turning tempo into a race", "Skipping warm-up"],
     ["Limit hard sessions; keep most volume easy (Seiler distribution evidence is descriptive)."],
     ["Illness / very low readiness"],
     [("easy-run", "Keep easy")],
     [("interval-run", "Intermittent hard")]),
    ("interval-run", "Interval run", "Інтервальний біг", "gait",
     ["quadriceps", "hamstrings", "calves", "full_body"], [],
     ["Alternate hard efforts with easy jog/walk recoveries for prescribed reps."],
     ["Track/treadmill helpful; clear warm-up."],
     "Recover breathing between reps.",
     ["Inadequate recovery jogs", "Poor pacing first reps"],
     ["Quality over junk volume; stop with sharp pain."],
     ["Cardiac symptoms — medical care"],
     [("easy-run", "Base"), ("tempo-run", "Sustained")],
     []),
    ("a-skip", "A-skip drill", "А-скіп", "run_drill",
     ["hip_flexors", "calves", "tibialis_anterior"], ["gluteus_maximus"],
     ["Rhythmic skip driving the knee up and striking under the hip with a quick toe-off."],
     ["Flat surface; short distance repeats."],
     "Relaxed face/breathing." ,
     ["Sitting back on the heels", "Tense shoulders"],
     ["Neuromuscular drill — not conditioning junk miles."],
     [],
     [("high-knees-drill", "Related knee drive")],
     [("easy-run", "Apply to run")]),
    ("high-knees-drill", "High-knees drill", "Високі підйоми колін", "run_drill",
     ["hip_flexors", "quadriceps", "calves"], [],
     ["March/run in place or travel with exaggerated knee drive and quick ground contacts."],
     ["Short bursts."],
     "Rhythmic breathing.",
     ["Leaning back", "Slamming feet"],
     ["Keep posture tall."],
     [],
     [("a-skip", "Skip pattern")],
     []),
    ("butt-kicks", "Butt kicks", "Захлести гомілки", "run_drill",
     ["hamstrings", "calves"], ["quadriceps"],
     ["Jog while kicking heels toward glutes with quick cadence; upright torso."],
     ["Short distance."],
     "Relaxed breathing.",
     ["Leaning forward excessively", "Slow soggy contacts"],
     ["Drill for recovery mechanics awareness."],
     [],
     [("high-knees-drill", "Anterior chain drill")],
     [("easy-run", "Integrate")]),
]

for row in run:
    eid, en, ukn, pattern, primary, secondary, tech, setup, breath, mist, saf, contra, reg, prog = row
    equip = ["road"] if eid in ("easy-run", "brisk-walk", "tempo-run") else ["track", "road"]
    if eid in ("a-skip", "high-knees-drill", "butt-kicks"):
        equip = ["track", "road", "none"]
    if eid == "interval-run":
        equip = ["track", "treadmill", "road"]
    refs = [REF_ACSM_GARBER, REF_SEILER] if pattern == "gait" else [REF_ACSM_GARBER]
    ex(
        eid, en, ukn, "running", pattern, primary, secondary, equip,
        "beginner" if eid in ("easy-run", "brisk-walk", "a-skip", "high-knees-drill", "butt-kicks") else "intermediate",
        False, "skill" if pattern == "run_drill" else "compound",
        "distance_time" if pattern == "gait" else "timed",
        [tech], [setup], breath, [mist], [saf], contra, reg, prog, refs, tags=["run"],
    )

# ---------------------------------------------------------------------------
# Write files
# ---------------------------------------------------------------------------

def write_all() -> None:
    DATA.mkdir(parents=True, exist_ok=True)
    LOCALES.mkdir(parents=True, exist_ok=True)
    TEMPLATES.mkdir(parents=True, exist_ok=True)

    # validate unique ids
    ids = [e["id"] for e in exercises]
    assert len(ids) == len(set(ids)), "duplicate ids"

    # strip empty aliases/tags optional? keep
    for e in exercises:
        path = DATA / f"{e['id']}.json"
        path.write_text(json.dumps(e, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    (LOCALES / "en-GB.json").write_text(
        json.dumps(en_gb, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    (LOCALES / "uk.json").write_text(
        json.dumps(uk, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )

    presets = {
        "home_gym": ["dumbbell", "kettlebell", "barbell", "bench", "bands", "pull_up_bar", "yoga_mat", "bodyweight", "none"],
        "commercial_gym": ["__all__"],
        "travel": ["bands", "bodyweight", "yoga_mat", "none"],
        "bodyweight": ["bodyweight", "none", "yoga_mat", "pull_up_bar"],
        "pool": ["pool", "kickboard", "pull_buoy", "fins", "paddle", "bodyweight", "none"],
    }
    PRESETS.write_text(json.dumps(presets, indent=2) + "\n", encoding="utf-8")

    # counts
    from collections import Counter
    c = Counter(e["discipline"] for e in exercises)
    print("Wrote", len(exercises), "exercises", dict(c))


if __name__ == "__main__":
    write_all()
