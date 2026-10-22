#!/usr/bin/env python3
"""Generate ~15 evidence-based workout templates."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "content/exercises/templates"
LOC_EN = ROOT / "content/exercises/locales/en-GB.json"
LOC_UK = ROOT / "content/exercises/locales/uk.json"

REF_ACSM_RT = {
    "kind": "doi",
    "id": "10.1249/MSS.0000000000003897",
    "label": "ACSM 2026 RT Position Stand",
    "supports": ["programming", "strength", "hypertrophy"],
}
REF_ACSM_G = {
    "kind": "doi",
    "id": "10.1249/MSS.0b013e318213fefb",
    "label": "Garber et al. 2011 ACSM guidance",
    "supports": ["programming", "endurance", "mobility"],
}
REF_VOL = {
    "kind": "doi",
    "id": "10.1080/02640414.2016.1210197",
    "label": "Schoenfeld et al. 2016 volume dose–response",
    "supports": ["hypertrophy", "programming"],
}
REF_FREQ = {
    "kind": "doi",
    "id": "10.1007/s40279-016-0543-8",
    "label": "Schoenfeld et al. 2016 frequency meta-analysis",
    "supports": ["programming", "hypertrophy"],
}
REF_SEILER = {
    "kind": "doi",
    "id": "10.1123/ijspp.5.3.276",
    "label": "Seiler 2010 intensity distribution",
    "supports": ["endurance", "programming"],
}
REF_PELLAND = {
    "kind": "doi",
    "id": "10.1007/s40279-025-02344-w",
    "label": "Pelland et al. volume/frequency meta-regressions",
    "supports": ["programming", "hypertrophy", "strength"],
}

en = json.loads(LOC_EN.read_text())
uk = json.loads(LOC_UK.read_text())


def nk(tid: str, en_name: str, uk_name: str) -> dict:
    key = f"template.{tid.replace('-', '_')}.name"
    en[key] = en_name
    uk[key] = uk_name
    return {"enGB": key, "uk": key}


def snk(tid: str, sid: str, en_name: str, uk_name: str) -> dict:
    key = f"template.{tid.replace('-', '_')}.{sid.replace('-', '_')}.name"
    en[key] = en_name
    uk[key] = uk_name
    return {"enGB": key, "uk": key}


def item(eid, sets=3, reps="8-12", rir="2-3", rest=90, notes=None, **kw):
    d = {"exerciseId": eid, "sets": sets, "reps": reps, "rir": rir, "restSeconds": rest}
    if notes:
        d["notes"] = notes
    d.update(kw)
    return d


templates = []

templates.append({
    "id": "full-body-beginner-3x",
    "nameKeys": nk("full-body-beginner-3x", "Full-body beginner (3×/week)", "Повне тіло для початківців (3×/тиждень)"),
    "disciplines": ["strength"],
    "level": "beginner",
    "goal": "general_fitness",
    "daysPerWeek": 3,
    "sessionMinutes": 50,
    "equipmentPreset": "home_gym",
    "equipmentRequired": ["dumbbell", "bodyweight"],
    "rationale": "Three full-body sessions spread weekly hard-set volume so major muscles are trained ~2–3×/week, matching frequency evidence when volume is accumulated and ACSM guidance for multi-joint work.",
    "evidenceRefs": [REF_FREQ, REF_ACSM_RT, REF_VOL],
    "sessions": [
        {
            "id": "a",
            "nameKeys": snk("full-body-beginner-3x", "a", "Day A", "День A"),
            "blocks": [
                {"kind": "warmup", "items": [item("worlds-greatest-stretch", sets=1, reps="4/side", rir="5", rest=30)]},
                {"kind": "main", "items": [
                    item("goblet-squat"),
                    item("push-up", reps="as tolerated"),
                    item("romanian-deadlift-dumbbell"),
                    item("one-arm-dumbbell-row"),
                    item("plank", sets=3, reps="hold", durationSeconds=30, rest=45),
                ]},
            ],
        },
        {
            "id": "b",
            "nameKeys": snk("full-body-beginner-3x", "b", "Day B", "День B"),
            "blocks": [
                {"kind": "main", "items": [
                    item("reverse-lunge-dumbbell", reps="8/leg"),
                    item("dumbbell-bench-press"),
                    item("lat-pulldown"),
                    item("hip-thrust"),
                    item("dead-bug", reps="8/side"),
                ]},
            ],
        },
        {
            "id": "c",
            "nameKeys": snk("full-body-beginner-3x", "c", "Day C", "День C"),
            "blocks": [
                {"kind": "main", "items": [
                    item("trap-bar-deadlift", reps="5-8", rest=150),
                    item("overhead-press-dumbbell"),
                    item("inverted-row"),
                    item("farmers-carry", sets=3, reps="20-40m", distanceMeters=30, rest=60),
                    item("face-pull"),
                ]},
            ],
        },
    ],
})

templates.append({
    "id": "upper-lower-4x",
    "nameKeys": nk("upper-lower-4x", "Upper/lower intermediate (4×/week)", "Верх/низ середній рівень (4×/тиждень)"),
    "disciplines": ["strength"],
    "level": "intermediate",
    "goal": "hypertrophy",
    "daysPerWeek": 4,
    "sessionMinutes": 60,
    "equipmentPreset": "commercial_gym",
    "equipmentRequired": ["barbell", "dumbbell", "cable", "machine"],
    "rationale": "Four-day upper/lower keeps ~2× weekly frequency per muscle while allowing higher per-session volume within Schoenfeld/Pelland dose–response bands.",
    "evidenceRefs": [REF_VOL, REF_PELLAND, REF_FREQ],
    "sessions": [
        {"id": "u1", "nameKeys": snk("upper-lower-4x", "u1", "Upper 1", "Верх 1"), "blocks": [
            {"kind": "main", "items": [
                item("barbell-bench-press", reps="5-8", rest=150),
                item("chest-supported-row"),
                item("overhead-press-barbell", reps="5-8", rest=120),
                item("lat-pulldown"),
                item("lateral-raise"),
                item("tricep-pushdown"),
            ]},
        ]},
        {"id": "l1", "nameKeys": snk("upper-lower-4x", "l1", "Lower 1", "Низ 1"), "blocks": [
            {"kind": "main", "items": [
                item("back-squat", reps="5-8", rest=180),
                item("romanian-deadlift-barbell", reps="6-10", rest=150),
                item("walking-lunge", reps="8/leg"),
                item("leg-curl-machine"),
                item("calf-raise-standing"),
            ]},
        ]},
        {"id": "u2", "nameKeys": snk("upper-lower-4x", "u2", "Upper 2", "Верх 2"), "blocks": [
            {"kind": "main", "items": [
                item("pull-up", reps="as tolerated"),
                item("dumbbell-incline-press"),
                item("seated-cable-row"),
                item("cable-chest-fly"),
                item("face-pull"),
                item("barbell-curl"),
            ]},
        ]},
        {"id": "l2", "nameKeys": snk("upper-lower-4x", "l2", "Lower 2", "Низ 2"), "blocks": [
            {"kind": "main", "items": [
                item("trap-bar-deadlift", reps="5-8", rest=180),
                item("bulgarian-split-squat", reps="8/leg"),
                item("leg-press"),
                item("hip-thrust"),
                item("cable-woodchop", reps="10/side"),
            ]},
        ]},
    ],
})

templates.append({
    "id": "ppl-6x",
    "nameKeys": nk("ppl-6x", "Push/pull/legs (6×/week)", "Жим/тяга/ноги (6×/тиждень)"),
    "disciplines": ["strength"],
    "level": "intermediate",
    "goal": "hypertrophy",
    "daysPerWeek": 6,
    "sessionMinutes": 55,
    "equipmentPreset": "commercial_gym",
    "equipmentRequired": ["barbell", "dumbbell", "cable", "machine", "pull_up_bar"],
    "rationale": "PPL twice weekly delivers high frequency and volume for hypertrophy seekers who recover well; volume should still respect diminishing returns from Pelland et al.",
    "evidenceRefs": [REF_PELLAND, REF_VOL, REF_ACSM_RT],
    "sessions": [
        {"id": "push", "nameKeys": snk("ppl-6x", "push", "Push", "Жим"), "blocks": [{"kind": "main", "items": [
            item("barbell-bench-press", reps="6-10", rest=150), item("overhead-press-dumbbell"),
            item("dip-parallel-bar", reps="as tolerated"), item("lateral-raise"), item("tricep-pushdown"),
        ]}]},
        {"id": "pull", "nameKeys": snk("ppl-6x", "pull", "Pull", "Тяга"), "blocks": [{"kind": "main", "items": [
            item("pull-up"), item("barbell-row", reps="6-10", rest=120), item("seated-cable-row"),
            item("face-pull"), item("dumbbell-curl"),
        ]}]},
        {"id": "legs", "nameKeys": snk("ppl-6x", "legs", "Legs", "Ноги"), "blocks": [{"kind": "main", "items": [
            item("back-squat", reps="5-8", rest=180), item("romanian-deadlift-barbell"),
            item("leg-press"), item("walking-lunge", reps="8/leg"), item("calf-raise-standing"),
        ]}]},
    ],
})

templates.append({
    "id": "home-minimal-3x",
    "nameKeys": nk("home-minimal-3x", "Minimal-equipment home (3×/week)", "Мінімум обладнання вдома (3×/тиждень)"),
    "disciplines": ["strength"],
    "level": "beginner",
    "goal": "general_fitness",
    "daysPerWeek": 3,
    "sessionMinutes": 40,
    "equipmentPreset": "travel",
    "equipmentRequired": ["bands", "bodyweight"],
    "rationale": "Band + bodyweight full-body work preserves multi-joint stimulus when a gym is unavailable, aligned with ACSM guidance that some resistance is better than none.",
    "evidenceRefs": [REF_ACSM_G, REF_ACSM_RT],
    "sessions": [
        {"id": "h1", "nameKeys": snk("home-minimal-3x", "h1", "Home A", "Дім A"), "blocks": [{"kind": "main", "items": [
            item("goblet-squat", notes="Use backpack if no DB"), item("push-up"), item("inverted-row"),
            item("glute-bridge"), item("band-pull-apart"), item("plank", durationSeconds=30, reps="hold"),
        ]}]},
        {"id": "h2", "nameKeys": snk("home-minimal-3x", "h2", "Home B", "Дім B"), "blocks": [{"kind": "main", "items": [
            item("split-squat-bodyweight", reps="8/leg"), item("incline-push-up"), item("dead-bug", reps="8/side"),
            item("hip-hinge-dowel"), item("side-plank", reps="hold/side", durationSeconds=20),
        ]}]},
        {"id": "h3", "nameKeys": snk("home-minimal-3x", "h3", "Home C", "Дім C"), "blocks": [{"kind": "main", "items": [
            item("walking-lunge", reps="8/leg"), item("diamond-push-up"), item("bird-dog", reps="8/side"),
            item("russian-twist", reps="12/side"), item("calf-raise-standing"),
        ]}]},
    ],
})

templates.append({
    "id": "strength-focus-3x",
    "nameKeys": nk("strength-focus-3x", "Strength focus novice linear (3×/week)", "Силовий фокус лінійний (3×/тиждень)"),
    "disciplines": ["strength"],
    "level": "beginner",
    "goal": "strength",
    "daysPerWeek": 3,
    "sessionMinutes": 55,
    "equipmentPreset": "commercial_gym",
    "equipmentRequired": ["barbell", "bench"],
    "rationale": "Novice linear progression on squat/hinge/press/pull compounds with conservative accessories, consistent with ACSM RT overview emphasis on multi-joint loading.",
    "evidenceRefs": [REF_ACSM_RT, REF_PELLAND],
    "sessions": [
        {"id": "s1", "nameKeys": snk("strength-focus-3x", "s1", "Strength A", "Сила A"), "blocks": [{"kind": "main", "items": [
            item("back-squat", sets=3, reps="5", rir="2-3", rest=180),
            item("barbell-bench-press", sets=3, reps="5", rest=150),
            item("barbell-row", sets=3, reps="5-8", rest=120),
            item("plank", durationSeconds=30, reps="hold"),
        ]}]},
        {"id": "s2", "nameKeys": snk("strength-focus-3x", "s2", "Strength B", "Сила B"), "blocks": [{"kind": "main", "items": [
            item("conventional-deadlift", sets=3, reps="3-5", rir="2-3", rest=210),
            item("overhead-press-barbell", sets=3, reps="5", rest=150),
            item("pull-up", sets=3, reps="as tolerated"),
            item("farmers-carry", distanceMeters=40, reps="carry"),
        ]}]},
        {"id": "s3", "nameKeys": snk("strength-focus-3x", "s3", "Strength C", "Сила C"), "blocks": [{"kind": "main", "items": [
            item("front-squat", sets=3, reps="5", rest=180),
            item("close-grip-bench-press", sets=3, reps="5-8", rest=150),
            item("chest-supported-row"),
            item("hip-thrust", reps="6-10"),
        ]}]},
    ],
})

templates.append({
    "id": "hypertrophy-dumbbell-4x",
    "nameKeys": nk("hypertrophy-dumbbell-4x", "Dumbbell hypertrophy (4×/week)", "Гіпертрофія з гантелями (4×/тиждень)"),
    "disciplines": ["strength"],
    "level": "intermediate",
    "goal": "hypertrophy",
    "daysPerWeek": 4,
    "sessionMinutes": 50,
    "equipmentPreset": "home_gym",
    "equipmentRequired": ["dumbbell", "bench"],
    "rationale": "Upper/lower with dumbbells targets ~10–16 weekly hard sets per region using double progression in 8–12 rep ranges.",
    "evidenceRefs": [REF_VOL, REF_ACSM_RT],
    "sessions": [
        {"id": "du1", "nameKeys": snk("hypertrophy-dumbbell-4x", "du1", "DB Upper 1", "Гантелі верх 1"), "blocks": [{"kind": "main", "items": [
            item("dumbbell-bench-press"), item("one-arm-dumbbell-row"), item("overhead-press-dumbbell"),
            item("lateral-raise"), item("dumbbell-curl"),
        ]}]},
        {"id": "dl1", "nameKeys": snk("hypertrophy-dumbbell-4x", "dl1", "DB Lower 1", "Гантелі низ 1"), "blocks": [{"kind": "main", "items": [
            item("goblet-squat"), item("romanian-deadlift-dumbbell"), item("bulgarian-split-squat", reps="8/leg"),
            item("hip-thrust"), item("calf-raise-standing"),
        ]}]},
        {"id": "du2", "nameKeys": snk("hypertrophy-dumbbell-4x", "du2", "DB Upper 2", "Гантелі верх 2"), "blocks": [{"kind": "main", "items": [
            item("dumbbell-incline-press"), item("chest-supported-row"), item("floor-press-dumbbell"),
            item("face-pull", notes="Band OK"), item("overhead-press-dumbbell", sets=2),
        ]}]},
        {"id": "dl2", "nameKeys": snk("hypertrophy-dumbbell-4x", "dl2", "DB Lower 2", "Гантелі низ 2"), "blocks": [{"kind": "main", "items": [
            item("reverse-lunge-dumbbell", reps="8/leg"), item("single-leg-rdl", reps="8/leg"),
            item("lateral-lunge", reps="8/leg"), item("glute-bridge"), item("dead-bug", reps="8/side"),
        ]}]},
    ],
})

templates.append({
    "id": "bodyweight-travel-3x",
    "nameKeys": nk("bodyweight-travel-3x", "Travel bodyweight circuit (3×/week)", "Тревел коло з вагою тіла (3×/тиждень)"),
    "disciplines": ["strength"],
    "level": "beginner",
    "goal": "fat_loss_support",
    "daysPerWeek": 3,
    "sessionMinutes": 30,
    "equipmentPreset": "bodyweight",
    "equipmentRequired": ["bodyweight"],
    "rationale": "Short full-body circuits maintain stimulus while travelling; pair with walking for energy expenditure per ACSM activity guidance.",
    "evidenceRefs": [REF_ACSM_G],
    "sessions": [
        {"id": "t1", "nameKeys": snk("bodyweight-travel-3x", "t1", "Travel circuit", "Тревел коло"), "blocks": [{"kind": "main", "items": [
            item("squat".replace("squat", "sit-to-stand"), reps="12"), item("push-up"), item("split-squat-bodyweight", reps="8/leg"),
            item("inverted-row", notes="Use table if safe"), item("plank", durationSeconds=40, reps="hold"),
            item("bird-dog", reps="8/side"),
        ]}]},
    ],
})
# fix sit-to-stand
templates[-1]["sessions"][0]["blocks"][0]["items"][0]["exerciseId"] = "sit-to-stand"

templates.append({
    "id": "beginner-5k-8w",
    "nameKeys": nk("beginner-5k-8w", "5K beginner run block", "Блок підготовки до 5 км"),
    "disciplines": ["running"],
    "level": "beginner",
    "goal": "race_prep",
    "daysPerWeek": 3,
    "sessionMinutes": 40,
    "equipmentPreset": "custom",
    "equipmentRequired": ["road"],
    "rationale": "Mostly easy running with one quality stimulus reflects polarised descriptive distributions; weekly mileage increases softly with an honest caveat that fixed 10% rules are weakly evidenced.",
    "evidenceRefs": [REF_SEILER, REF_ACSM_G],
    "sessions": [
        {"id": "r1", "nameKeys": snk("beginner-5k-8w", "r1", "Easy + drills", "Легкий + дрили"), "blocks": [
            {"kind": "warmup", "items": [item("worlds-greatest-stretch", sets=1, reps="3/side", rest=15), item("a-skip", sets=2, reps="20m", rest=30)]},
            {"kind": "main", "items": [item("easy-run", sets=1, reps="20-30min", durationSeconds=1500, rir="n/a", rest=0)]},
        ]},
        {"id": "r2", "nameKeys": snk("beginner-5k-8w", "r2", "Intervals", "Інтервали"), "blocks": [
            {"kind": "main", "items": [
                item("easy-run", sets=1, reps="10minWU", durationSeconds=600, rest=0),
                item("interval-run", sets=6, reps="1min hard/2min easy", rest=0),
                item("brisk-walk", sets=1, reps="5min CD", durationSeconds=300),
            ]},
        ]},
        {"id": "r3", "nameKeys": snk("beginner-5k-8w", "r3", "Easy longish", "Легкий довший"), "blocks": [
            {"kind": "main", "items": [item("easy-run", sets=1, reps="30-40min", durationSeconds=2100, rest=0)]},
        ]},
    ],
})

templates.append({
    "id": "swim-technique-3x",
    "nameKeys": nk("swim-technique-3x", "Swim technique block (3×/week)", "Блок техніки плавання (3×/тиждень)"),
    "disciplines": ["swimming"],
    "level": "beginner",
    "goal": "technique",
    "daysPerWeek": 3,
    "sessionMinutes": 45,
    "equipmentPreset": "pool",
    "equipmentRequired": ["pool", "kickboard", "pull_buoy"],
    "rationale": "Prioritise drill density before yardage so aerobic volume does not entrench poor mechanics; easy aerobic dominance still applies when conditioning.",
    "evidenceRefs": [REF_ACSM_G, REF_SEILER],
    "sessions": [
        {"id": "sw1", "nameKeys": snk("swim-technique-3x", "sw1", "Catch focus", "Фокус на захват"), "blocks": [{"kind": "main", "items": [
            item("kickboard-kick", sets=4, reps="25-50m", distanceMeters=50, rest=30),
            item("catch-up-drill", sets=6, reps="25m", distanceMeters=25, rest=30),
            item("freestyle-stroke", sets=4, reps="50m easy", distanceMeters=50, rest=40),
        ]}]},
        {"id": "sw2", "nameKeys": snk("swim-technique-3x", "sw2", "Pull focus", "Фокус на тягу"), "blocks": [{"kind": "main", "items": [
            item("pull-buoy-pull", sets=6, reps="50m", distanceMeters=50, rest=30),
            item("freestyle-stroke", sets=4, reps="50m", distanceMeters=50, rest=40),
            item("backstroke", sets=2, reps="50m", distanceMeters=50, rest=40),
        ]}]},
        {"id": "sw3", "nameKeys": snk("swim-technique-3x", "sw3", "Mixed strokes", "Змішані стилі"), "blocks": [{"kind": "main", "items": [
            item("breaststroke", sets=4, reps="50m", distanceMeters=50, rest=45),
            item("freestyle-stroke", sets=4, reps="50m", distanceMeters=50, rest=40),
            item("kickboard-kick", sets=3, reps="50m", distanceMeters=50, rest=30),
        ]}]},
    ],
})

templates.append({
    "id": "yoga-mobility-flow",
    "nameKeys": nk("yoga-mobility-flow", "Yoga mobility flow", "Йога-флоу для мобільності"),
    "disciplines": ["yoga"],
    "level": "beginner",
    "goal": "mobility",
    "daysPerWeek": 3,
    "sessionMinutes": 30,
    "equipmentPreset": "bodyweight",
    "equipmentRequired": ["yoga_mat"],
    "rationale": "Flexibility and neuromotor practice on ≥2–3 days/week aligns with ACSM quantity/quality guidance; poses stay beginner-accessible.",
    "evidenceRefs": [REF_ACSM_G],
    "sessions": [
        {"id": "y1", "nameKeys": snk("yoga-mobility-flow", "y1", "Flow A", "Флоу A"), "blocks": [{"kind": "main", "items": [
            item("cat-cow", sets=1, reps="8", rir="5", rest=10),
            item("downward-dog", sets=3, reps="hold", durationSeconds=40, rest=20),
            item("low-lunge", sets=2, reps="hold/side", durationSeconds=40, rest=20),
            item("warrior-ii", sets=2, reps="hold/side", durationSeconds=30, rest=20),
            item("seated-forward-fold", sets=2, reps="hold", durationSeconds=45, rest=20),
            item("child-pose", sets=1, reps="hold", durationSeconds=60, rest=0),
        ]}]},
        {"id": "y2", "nameKeys": snk("yoga-mobility-flow", "y2", "Flow B", "Флоу B"), "blocks": [{"kind": "main", "items": [
            item("mountain-pose", sets=1, reps="hold", durationSeconds=30),
            item("tree-pose", sets=2, reps="hold/side", durationSeconds=30),
            item("triangle-pose", sets=2, reps="hold/side", durationSeconds=30),
            item("bridge-pose-yoga", sets=3, reps="hold", durationSeconds=30),
            item("pigeon-pose", sets=2, reps="hold/side", durationSeconds=40),
            item("child-pose", sets=1, reps="hold", durationSeconds=60),
        ]}]},
    ],
})

templates.append({
    "id": "pilates-core-3x",
    "nameKeys": nk("pilates-core-3x", "Pilates core foundations", "Основи кора в пілатесі"),
    "disciplines": ["pilates"],
    "level": "beginner",
    "goal": "mobility",
    "daysPerWeek": 3,
    "sessionMinutes": 35,
    "equipmentPreset": "bodyweight",
    "equipmentRequired": ["pilates_mat", "yoga_mat"],
    "rationale": "Controlled trunk endurance and motor control sessions complement ACSM neuromotor recommendations without high spinal load.",
    "evidenceRefs": [REF_ACSM_G],
    "sessions": [
        {"id": "p1", "nameKeys": snk("pilates-core-3x", "p1", "Core A", "Кор A"), "blocks": [{"kind": "main", "items": [
            item("pilates-hundred", sets=1, reps="100 counts", durationSeconds=90, rest=30),
            item("pilates-single-leg-stretch", sets=2, reps="8/side"),
            item("pilates-shoulder-bridge", sets=3, reps="8"),
            item("pilates-leg-circles", sets=2, reps="5/dir/leg"),
            item("pilates-plank-variations", sets=3, reps="hold", durationSeconds=30),
        ]}]},
        {"id": "p2", "nameKeys": snk("pilates-core-3x", "p2", "Core B", "Кор B"), "blocks": [{"kind": "main", "items": [
            item("pilates-roll-up", sets=3, reps="6"),
            item("pilates-double-leg-stretch", sets=2, reps="8"),
            item("pilates-side-kick", sets=2, reps="8/side"),
            item("pilates-swan", sets=3, reps="6"),
            item("pilates-saw", sets=2, reps="6/side"),
        ]}]},
    ],
})

templates.append({
    "id": "concurrent-lift-run-4x",
    "nameKeys": nk("concurrent-lift-run-4x", "Concurrent lift + easy run", "Поєднані силові + легкий біг"),
    "disciplines": ["strength", "running"],
    "level": "intermediate",
    "goal": "general_fitness",
    "daysPerWeek": 4,
    "sessionMinutes": 60,
    "equipmentPreset": "commercial_gym",
    "equipmentRequired": ["barbell", "dumbbell", "road"],
    "rationale": "Two lifting days plus two easy runs balance musculoskeletal and cardiorespiratory fitness per ACSM multi-component guidance; hard intervals limited.",
    "evidenceRefs": [REF_ACSM_G, REF_SEILER, REF_ACSM_RT],
    "sessions": [
        {"id": "cl1", "nameKeys": snk("concurrent-lift-run-4x", "cl1", "Lift A", "Сила A"), "blocks": [{"kind": "main", "items": [
            item("back-squat", reps="5-8", rest=150), item("barbell-bench-press", reps="5-8", rest=150),
            item("one-arm-dumbbell-row"), item("hip-thrust"),
        ]}]},
        {"id": "cr1", "nameKeys": snk("concurrent-lift-run-4x", "cr1", "Easy run", "Легкий біг"), "blocks": [{"kind": "main", "items": [
            item("easy-run", sets=1, reps="30-40min", durationSeconds=2100, rest=0),
        ]}]},
        {"id": "cl2", "nameKeys": snk("concurrent-lift-run-4x", "cl2", "Lift B", "Сила B"), "blocks": [{"kind": "main", "items": [
            item("romanian-deadlift-barbell", reps="6-10", rest=150), item("overhead-press-dumbbell"),
            item("pull-up"), item("walking-lunge", reps="8/leg"), item("pallof-press", reps="8/side"),
        ]}]},
        {"id": "cr2", "nameKeys": snk("concurrent-lift-run-4x", "cr2", "Run drills + easy", "Дрили + легкий"), "blocks": [
            {"kind": "warmup", "items": [item("high-knees-drill", sets=2, reps="20m"), item("butt-kicks", sets=2, reps="20m")]},
            {"kind": "main", "items": [item("easy-run", sets=1, reps="25min", durationSeconds=1500)]},
        ]},
    ],
})

templates.append({
    "id": "desk-worker-mobility-strength",
    "nameKeys": nk("desk-worker-mobility-strength", "Desk-worker strength + mobility", "Сила + мобільність для офісу"),
    "disciplines": ["strength", "yoga"],
    "level": "beginner",
    "goal": "general_fitness",
    "daysPerWeek": 3,
    "sessionMinutes": 45,
    "equipmentPreset": "home_gym",
    "equipmentRequired": ["dumbbell", "bands", "yoga_mat"],
    "rationale": "Combines posterior-chain and upper-back strength with thoracic/hip mobility to offset sedentary postures, within ACSM multi-component recommendations.",
    "evidenceRefs": [REF_ACSM_G, REF_ACSM_RT],
    "sessions": [
        {"id": "d1", "nameKeys": snk("desk-worker-mobility-strength", "d1", "Posterior + open", "Задня лінія"), "blocks": [
            {"kind": "warmup", "items": [item("cat-cow", sets=1, reps="8"), item("band-pull-apart", sets=2, reps="15")]},
            {"kind": "main", "items": [
                item("romanian-deadlift-dumbbell"), item("chest-supported-row"), item("face-pull"),
                item("hip-thrust"), item("dead-bug", reps="8/side"),
            ]},
            {"kind": "cooldown", "items": [item("downward-dog", durationSeconds=40, reps="hold"), item("child-pose", durationSeconds=60, reps="hold")]},
        ]},
        {"id": "d2", "nameKeys": snk("desk-worker-mobility-strength", "d2", "Legs + hips", "Ноги + стегна"), "blocks": [{"kind": "main", "items": [
            item("goblet-squat"), item("reverse-lunge-dumbbell", reps="8/leg"), item("glute-bridge"),
            item("worlds-greatest-stretch", sets=2, reps="4/side"), item("pigeon-pose", durationSeconds=40, reps="hold/side"),
        ]}]},
        {"id": "d3", "nameKeys": snk("desk-worker-mobility-strength", "d3", "Push + brace", "Жим + кор"), "blocks": [{"kind": "main", "items": [
            item("dumbbell-bench-press"), item("overhead-press-dumbbell"), item("pallof-press", reps="8/side"),
            item("side-plank", durationSeconds=25, reps="hold/side"), item("seated-forward-fold", durationSeconds=45, reps="hold"),
        ]}]},
    ],
})

templates.append({
    "id": "older-adult-functional-3x",
    "nameKeys": nk("older-adult-functional-3x", "Older-adult functional strength", "Функціональна сила для старших дорослих"),
    "disciplines": ["strength"],
    "level": "beginner",
    "goal": "general_fitness",
    "daysPerWeek": 3,
    "sessionMinutes": 40,
    "equipmentPreset": "home_gym",
    "equipmentRequired": ["dumbbell", "bodyweight"],
    "rationale": "Sit-to-stand, carries, and supported patterns emphasise function and balance with conservative RIR, consistent with ACSM guidance for older adults' multi-component training.",
    "evidenceRefs": [REF_ACSM_G, REF_ACSM_RT],
    "sessions": [
        {"id": "o1", "nameKeys": snk("older-adult-functional-3x", "o1", "Function A", "Функція A"), "blocks": [{"kind": "main", "items": [
            item("sit-to-stand", sets=3, reps="8-12", rir="3-4"),
            item("incline-push-up", rir="3-4"),
            item("one-arm-dumbbell-row", rir="3-4"),
            item("farmers-carry", distanceMeters=20, reps="carry", rir="3-4"),
            item("bird-dog", reps="6/side", rir="4"),
        ]}]},
        {"id": "o2", "nameKeys": snk("older-adult-functional-3x", "o2", "Function B", "Функція B"), "blocks": [{"kind": "main", "items": [
            item("goblet-squat", rir="3-4"), item("hip-hinge-dowel", sets=2, reps="8", rir="4"),
            item("overhead-press-dumbbell", rir="3-4"), item("step-up", reps="6/leg", rir="3-4"),
            item("dead-bug", reps="6/side", rir="4"),
        ]}]},
    ],
})

templates.append({
    "id": "glute-posterior-emphasis",
    "nameKeys": nk("glute-posterior-emphasis", "Posterior-chain emphasis (3×/week)", "Акцент на задню лінію (3×/тиждень)"),
    "disciplines": ["strength"],
    "level": "intermediate",
    "goal": "hypertrophy",
    "daysPerWeek": 3,
    "sessionMinutes": 50,
    "equipmentPreset": "commercial_gym",
    "equipmentRequired": ["barbell", "dumbbell", "bench"],
    "rationale": "Hinge and hip-extension emphasis raises glute/hamstring weekly sets within established hypertrophy volume bands; hip thrust included given EMG comparisons versus squats.",
    "evidenceRefs": [REF_VOL, {"kind": "doi", "id": "10.1123/jab.2014-0301", "label": "Contreras et al. 2015 hip thrust vs squat EMG", "supports": ["emg", "biomechanics"]}],
    "sessions": [
        {"id": "g1", "nameKeys": snk("glute-posterior-emphasis", "g1", "Hinge day", "День шарніру"), "blocks": [{"kind": "main", "items": [
            item("romanian-deadlift-barbell", reps="6-10", rest=150), item("hip-thrust", reps="8-12"),
            item("single-leg-rdl", reps="8/leg"), item("leg-curl-machine"), item("back-extension-roman"),
        ]}]},
        {"id": "g2", "nameKeys": snk("glute-posterior-emphasis", "g2", "Squat + thrust", "Присід + thrust"), "blocks": [{"kind": "main", "items": [
            item("back-squat", reps="5-8", rest=180), item("bulgarian-split-squat", reps="8/leg"),
            item("hip-thrust", sets=4), item("cable-woodchop", reps="10/side"),
        ]}]},
        {"id": "g3", "nameKeys": snk("glute-posterior-emphasis", "g3", "Mixed posterior", "Змішаний задній ланцюг"), "blocks": [{"kind": "main", "items": [
            item("trap-bar-deadlift", reps="5-8", rest=180), item("kettlebell-swing", sets=4, reps="10", rest=60),
            item("glute-bridge", sets=3, reps="12"), item("walking-lunge", reps="8/leg"),
        ]}]},
    ],
})

assert len(templates) >= 15, len(templates)

OUT.mkdir(parents=True, exist_ok=True)
for t in templates:
    (OUT / f"{t['id']}.json").write_text(json.dumps(t, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

LOC_EN.write_text(json.dumps(en, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
LOC_UK.write_text(json.dumps(uk, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
print("templates", len(templates))
