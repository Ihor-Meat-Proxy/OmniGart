#!/usr/bin/env python3
"""Generate bundled yoga pose/flow and pilates exercise/session libraries."""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ["garber-acsm-2011", "acsm-rt-2026"]

# id, sanskrit, english_en, english_uk, family, level, primary, secondary, equipment, unilateral, hold
YOGA_POSES: list[tuple] = [
    ("mountain-pose", "Tadasana", "Mountain pose", "Поза гори", "standing", "beginner", ["quadriceps", "gluteus_maximus"], ["trapezius"], ["yoga_mat", "bodyweight"], False, 30),
    ("chair-pose", "Utkatasana", "Chair pose", "Поза стільця", "standing", "beginner", ["quadriceps", "gluteus_maximus"], ["erector_spinae"], ["yoga_mat", "bodyweight"], False, 30),
    ("forward-fold", "Uttanasana", "Standing forward fold", "Стоячий нахил уперед", "forward_fold", "beginner", ["hamstrings", "erector_spinae"], ["calves"], ["yoga_mat", "yoga_block", "bodyweight"], False, 40),
    ("half-forward-fold", "Ardha Uttanasana", "Half forward fold", "Напівнахил уперед", "forward_fold", "beginner", ["hamstrings", "erector_spinae"], ["rhomboids"], ["yoga_mat", "bodyweight"], False, 20),
    ("downward-dog", "Adho Mukha Svanasana", "Downward-facing dog", "Собака мордою вниз", "standing", "beginner", ["hamstrings", "shoulders_proxy"], ["calves", "latissimus_dorsi"], ["yoga_mat", "bodyweight"], False, 40),
    ("upward-dog", "Urdhva Mukha Svanasana", "Upward-facing dog", "Собака мордою вгору", "backbend", "intermediate", ["erector_spinae", "gluteus_maximus"], ["triceps", "hip_flexors"], ["yoga_mat", "bodyweight"], False, 20),
    ("cobra-pose", "Bhujangasana", "Cobra pose", "Поза кобри", "backbend", "beginner", ["erector_spinae"], ["gluteus_maximus", "pectoralis_major"], ["yoga_mat", "bodyweight"], False, 25),
    ("plank-pose", "Phalakasana", "Plank pose", "Планка", "prone", "beginner", ["rectus_abdominis", "transverse_abdominis"], ["shoulders_proxy", "quadriceps"], ["yoga_mat", "bodyweight"], False, 30),
    ("chaturanga", "Chaturanga Dandasana", "Four-limbed staff", "Чатуранґа", "prone", "intermediate", ["triceps", "pectoralis_major"], ["rectus_abdominis", "serratus_anterior"], ["yoga_mat", "bodyweight"], False, 10),
    ("low-lunge", "Anjaneyasana", "Low lunge", "Низький випад", "hip_opener", "beginner", ["hip_flexors", "quadriceps"], ["gluteus_maximus"], ["yoga_mat", "yoga_block", "bodyweight"], True, 35),
    ("high-lunge", "Ashta Chandrasana", "High lunge", "Високий випад", "standing", "beginner", ["quadriceps", "gluteus_maximus"], ["hip_flexors"], ["yoga_mat", "bodyweight"], True, 30),
    ("warrior-i", "Virabhadrasana I", "Warrior I", "Воїн I", "standing", "beginner", ["quadriceps", "gluteus_maximus", "hip_flexors"], ["front_deltoid"], ["yoga_mat", "bodyweight"], True, 35),
    ("warrior-ii", "Virabhadrasana II", "Warrior II", "Воїн II", "standing", "beginner", ["quadriceps", "gluteus_medius"], ["adductors", "lateral_deltoid"], ["yoga_mat", "bodyweight"], True, 40),
    ("warrior-iii", "Virabhadrasana III", "Warrior III", "Воїн III", "balance", "intermediate", ["gluteus_maximus", "hamstrings"], ["erector_spinae", "transverse_abdominis"], ["yoga_mat", "bodyweight"], True, 25),
    ("triangle-pose", "Trikonasana", "Triangle pose", "Поза трикутника", "standing", "beginner", ["hamstrings", "adductors"], ["obliques", "lateral_deltoid"], ["yoga_mat", "yoga_block", "bodyweight"], True, 35),
    ("extended-side-angle", "Utthita Parsvakonasana", "Extended side angle", "Витягнутий бічний кут", "standing", "intermediate", ["quadriceps", "adductors"], ["obliques", "latissimus_dorsi"], ["yoga_mat", "yoga_block", "bodyweight"], True, 35),
    ("reverse-warrior", "Viparita Virabhadrasana", "Reverse warrior", "Зворотний воїн", "standing", "beginner", ["quadriceps", "obliques"], ["latissimus_dorsi"], ["yoga_mat", "bodyweight"], True, 25),
    ("tree-pose", "Vrksasana", "Tree pose", "Поза дерева", "balance", "beginner", ["gluteus_medius", "quadriceps"], ["transverse_abdominis"], ["yoga_mat", "bodyweight"], True, 30),
    ("eagle-pose", "Garudasana", "Eagle pose", "Поза орла", "balance", "intermediate", ["gluteus_medius", "adductors"], ["trapezius", "rear_deltoid"], ["yoga_mat", "bodyweight"], True, 25),
    ("standing-split", "Urdhva Prasarita Eka Padasana", "Standing split", "Стоячий шпагат", "balance", "advanced", ["hamstrings", "gluteus_maximus"], ["calves"], ["yoga_mat", "yoga_block", "bodyweight"], True, 20),
    ("goddess-pose", "Utkata Konasana", "Goddess pose", "Поза богині", "standing", "beginner", ["quadriceps", "adductors", "gluteus_maximus"], ["calves"], ["yoga_mat", "bodyweight"], False, 35),
    ("pyramid-pose", "Parsvottanasana", "Pyramid pose", "Поза піраміди", "forward_fold", "intermediate", ["hamstrings", "calves"], ["erector_spinae"], ["yoga_mat", "yoga_block", "bodyweight"], True, 30),
    ("wide-legged-forward-fold", "Prasarita Padottanasana", "Wide-legged forward fold", "Широкий нахил уперед", "forward_fold", "beginner", ["hamstrings", "adductors"], ["erector_spinae"], ["yoga_mat", "yoga_block", "bodyweight"], False, 40),
    ("garland-pose", "Malasana", "Garland squat", "Поза гірлянди", "hip_opener", "beginner", ["adductors", "gluteus_maximus"], ["quadriceps", "calves"], ["yoga_mat", "yoga_block", "bodyweight"], False, 35),
    ("cat-cow", "Marjaryasana–Bitilasana", "Cat–cow", "Кішка–корова", "spinal_mobility", "beginner", ["erector_spinae", "rectus_abdominis"], ["trapezius"], ["yoga_mat", "bodyweight"], False, 60),
    ("thread-needle", "Sucirandhrasana (variant)", "Thread the needle", "Нитка в голку", "twist", "beginner", ["rear_deltoid", "rhomboids"], ["obliques"], ["yoga_mat", "bodyweight"], True, 30),
    ("child-pose", "Balasana", "Child's pose", "Поза дитини", "restorative", "beginner", ["latissimus_dorsi", "gluteus_maximus"], ["hip_flexors"], ["yoga_mat", "bolster", "bodyweight"], False, 60),
    ("puppy-pose", "Uttana Shishosana", "Puppy pose", "Поза цуценяти", "forward_fold", "beginner", ["latissimus_dorsi", "trapezius"], ["pectoralis_major"], ["yoga_mat", "yoga_block", "bodyweight"], False, 40),
    ("sphinx-pose", "Salamba Bhujangasana", "Sphinx pose", "Поза сфінкса", "backbend", "beginner", ["erector_spinae"], ["gluteus_maximus"], ["yoga_mat", "bodyweight"], False, 40),
    ("locust-pose", "Salabhasana", "Locust pose", "Поза сарани", "prone", "beginner", ["erector_spinae", "gluteus_maximus"], ["hamstrings"], ["yoga_mat", "bodyweight"], False, 20),
    ("bow-pose", "Dhanurasana", "Bow pose", "Поза лука", "backbend", "intermediate", ["erector_spinae", "gluteus_maximus"], ["quadriceps", "front_deltoid"], ["yoga_mat", "yoga_strap", "bodyweight"], False, 20),
    ("bridge-pose", "Setu Bandha Sarvangasana", "Bridge pose", "Поза моста", "backbend", "beginner", ["gluteus_maximus", "hamstrings"], ["erector_spinae"], ["yoga_mat", "yoga_block", "bodyweight"], False, 35),
    ("wheel-pose", "Urdhva Dhanurasana", "Wheel pose", "Поза колеса", "backbend", "advanced", ["gluteus_maximus", "erector_spinae"], ["triceps", "quadriceps"], ["yoga_mat", "bodyweight"], False, 15),
    ("camel-pose", "Ustrasana", "Camel pose", "Поза верблюда", "backbend", "intermediate", ["hip_flexors", "erector_spinae"], ["quadriceps", "gluteus_maximus"], ["yoga_mat", "yoga_block", "bodyweight"], False, 20),
    ("pigeon-pose", "Eka Pada Rajakapotasana (prep)", "Pigeon pose (prep)", "Поза голуба (підготовка)", "hip_opener", "intermediate", ["gluteus_maximus", "hip_flexors"], ["gluteus_medius"], ["yoga_mat", "yoga_block", "bodyweight"], True, 45),
    ("lizard-pose", "Utthan Pristhasana", "Lizard pose", "Поза ящірки", "hip_opener", "intermediate", ["hip_flexors", "adductors"], ["hamstrings"], ["yoga_mat", "yoga_block", "bodyweight"], True, 40),
    ("frog-pose", "Mandukasana", "Frog pose", "Поза жаби", "hip_opener", "intermediate", ["adductors"], ["hip_flexors"], ["yoga_mat", "blanket", "bodyweight"], False, 45),
    ("butterfly-pose", "Baddha Konasana", "Bound angle (butterfly)", "Зв’язаний кут", "hip_opener", "beginner", ["adductors"], ["hip_flexors"], ["yoga_mat", "yoga_block", "bodyweight"], False, 45),
    ("seated-forward-fold", "Paschimottanasana", "Seated forward fold", "Сидячий нахил уперед", "forward_fold", "beginner", ["hamstrings", "erector_spinae"], ["calves"], ["yoga_mat", "yoga_strap", "yoga_block", "bodyweight"], False, 45),
    ("head-to-knee", "Janu Sirsasana", "Head-to-knee pose", "Голова до коліна", "forward_fold", "beginner", ["hamstrings"], ["erector_spinae", "adductors"], ["yoga_mat", "yoga_strap", "bodyweight"], True, 40),
    ("revolved-head-to-knee", "Parivrtta Janu Sirsasana", "Revolved head-to-knee", "Скручений нахил до коліна", "twist", "intermediate", ["obliques", "hamstrings"], ["latissimus_dorsi"], ["yoga_mat", "yoga_strap", "bodyweight"], True, 30),
    ("seated-twist", "Ardha Matsyendrasana", "Half lord of the fishes", "Напівпоза Матсієндри", "twist", "beginner", ["obliques", "erector_spinae"], ["gluteus_medius"], ["yoga_mat", "bodyweight"], True, 35),
    ("supine-twist", "Supta Matsyendrasana", "Supine twist", "Скручування лежачи", "twist", "beginner", ["obliques"], ["gluteus_medius", "erector_spinae"], ["yoga_mat", "bodyweight"], True, 45),
    ("boat-pose", "Navasana", "Boat pose", "Поза човна", "seated", "intermediate", ["rectus_abdominis", "hip_flexors"], ["transverse_abdominis"], ["yoga_mat", "bodyweight"], False, 20),
    ("staff-pose", "Dandasana", "Staff pose", "Поза посоха", "seated", "beginner", ["quadriceps", "erector_spinae"], ["transverse_abdominis"], ["yoga_mat", "bodyweight"], False, 25),
    ("tabletop", "Bharmanasana", "Tabletop", "Стіл на чотирьох", "transition", "beginner", ["full_body"], ["transverse_abdominis"], ["yoga_mat", "bodyweight"], False, 20),
    ("bird-dog-yoga", "Dandayamana Bharmanasana", "Bird dog (yoga)", "Птах-собака", "balance", "beginner", ["erector_spinae", "gluteus_maximus"], ["transverse_abdominis", "rear_deltoid"], ["yoga_mat", "bodyweight"], True, 20),
    ("side-plank-yoga", "Vasisthasana", "Side plank", "Бічна планка", "balance", "intermediate", ["obliques", "serratus_anterior"], ["gluteus_medius"], ["yoga_mat", "bodyweight"], True, 20),
    ("crow-pose", "Bakasana", "Crow pose", "Поза ворони", "balance", "advanced", ["triceps", "transverse_abdominis"], ["hip_flexors", "front_deltoid"], ["yoga_mat", "yoga_block", "bodyweight"], False, 15),
    ("dolphin-pose", "Ardha Pincha Mayurasana", "Dolphin pose", "Поза дельфіна", "inversion", "intermediate", ["shoulders_proxy", "core_proxy"], ["hamstrings", "calves"], ["yoga_mat", "bodyweight"], False, 30),
    ("legs-up-wall", "Viparita Karani", "Legs up the wall", "Ноги вгору на стіну", "restorative", "beginner", ["hamstrings"], ["calves"], ["yoga_mat", "blanket", "bodyweight"], False, 180),
    ("happy-baby", "Ananda Balasana", "Happy baby", "Щаслива дитина", "hip_opener", "beginner", ["adductors", "hamstrings"], ["gluteus_maximus"], ["yoga_mat", "yoga_strap", "bodyweight"], False, 45),
    ("knee-to-chest", "Apanasana", "Knees to chest", "Коліна до грудей", "restorative", "beginner", ["gluteus_maximus", "erector_spinae"], ["hip_flexors"], ["yoga_mat", "bodyweight"], False, 40),
    ("corpse-pose", "Savasana", "Corpse pose", "Поза трупа (розслаблення)", "restorative", "beginner", ["full_body"], [], ["yoga_mat", "blanket", "bodyweight"], False, 180),
    ("reclining-bound-angle", "Supta Baddha Konasana", "Reclining bound angle", "Зв’язаний кут лежачи", "restorative", "beginner", ["adductors", "hip_flexors"], [], ["yoga_mat", "bolster", "yoga_block", "bodyweight"], False, 120),
    ("cow-face-pose", "Gomukhasana", "Cow face pose", "Поза коров’ячої морди", "seated", "intermediate", ["gluteus_maximus", "external_rotators_proxy"], ["triceps", "pectoralis_major"], ["yoga_mat", "yoga_strap", "bodyweight"], True, 40),
    ("hero-pose", "Virasana", "Hero pose", "Поза героя", "seated", "beginner", ["quadriceps"], ["tibialis_anterior"], ["yoga_mat", "yoga_block", "bodyweight"], False, 60),
    ("easy-pose", "Sukhasana", "Easy seated pose", "Зручна сидяча поза", "seated", "beginner", ["hip_flexors", "erector_spinae"], ["adductors"], ["yoga_mat", "blanket", "bodyweight"], False, 60),
    ("half-splits", "Ardha Hanumanasana", "Half splits", "Напівшпагат", "forward_fold", "beginner", ["hamstrings"], ["calves"], ["yoga_mat", "yoga_block", "bodyweight"], True, 35),
    ("three-legged-dog", "Tri Pada Adho Mukha Svanasana", "Three-legged dog", "Триногий собака", "standing", "intermediate", ["hamstrings", "gluteus_maximus"], ["shoulders_proxy"], ["yoga_mat", "bodyweight"], True, 20),
    ("crescent-lunge-twist", "Parivrtta Anjaneyasana", "Revolved lunge", "Скручений випад", "twist", "intermediate", ["obliques", "hip_flexors"], ["quadriceps", "gluteus_medius"], ["yoga_mat", "bodyweight"], True, 25),
]

# Fix proxy muscles to real taxonomy ids
MUSCLE_FIX = {
    "shoulders_proxy": "front_deltoid",
    "core_proxy": "transverse_abdominis",
    "external_rotators_proxy": "gluteus_medius",
    "spinal_mobility": "erector_spinae",  # family misuse guard
}


def fix_muscles(ms: list[str]) -> list[str]:
    out = []
    for m in ms:
        m2 = MUSCLE_FIX.get(m, m)
        if m2 == "spinal_mobility":
            m2 = "erector_spinae"
        if m2 not in out:
            out.append(m2)
    return out


# Fix family for cat-cow - I used spinal_mobility which isn't in enum
FAMILY_FIX = {"spinal_mobility": "transition"}


def uk_sanskrit(s: str) -> str:
    return s  # Sanskrit transliteration shared across locales


def pose_technique(english: str, family: str) -> list[str]:
    return [
        f"Enter {english} with steady footing and an even pelvis; lengthen the spine without forcing end range.",
        "Distribute load through the feet or support points; soften the jaw and keep the neck long.",
    ]


def pose_cues(family: str) -> list[str]:
    return [
        "Breathe steadily; ease out of the shape if sharp pain appears.",
        "Use blocks or a strap to keep joints stacked and comfortable.",
    ]


def safety(family: str) -> list[str]:
    return [
        "Move within a comfortable range; props are encouraged rather than forcing depth.",
        "Stop and rest if you feel dizziness, numbness, or joint locking.",
    ]


def contraindications(family: str, level: str) -> list[str]:
    notes = [
        "General safety: seek clinician guidance for recent injury, uncontrolled hypertension, or acute illness before practising.",
    ]
    if family in ("inversion", "backbend") or level == "advanced":
        notes.append(
            "General safety: deeper backbends and inversions may be unsuitable during pregnancy or with unmanaged glaucoma, cervical issues, or uncontrolled blood pressure—choose a regression."
        )
    if family == "twist":
        notes.append(
            "General safety: keep twists gentle after abdominal surgery or during later pregnancy unless cleared by a qualified professional."
        )
    return notes


def write_json(path: Path, obj: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def generate_yoga(en: dict[str, str], uk: dict[str, str]) -> list[str]:
    pose_ids = []
    for row in YOGA_POSES:
        (pid, sanskrit, en_name, uk_name, family, level, primary, secondary, equipment, unilateral, hold) = row
        family = FAMILY_FIX.get(family, family)
        primary = fix_muscles(primary)
        secondary = fix_muscles(secondary)
        # remove accidental family string in muscles
        primary = [m for m in primary if m in (
            "quadriceps","hamstrings","gluteus_maximus","gluteus_medius","calves","hip_flexors","adductors",
            "pectoralis_major","latissimus_dorsi","trapezius","rhomboids","rear_deltoid","front_deltoid",
            "lateral_deltoid","biceps","triceps","forearms","rectus_abdominis","obliques","transverse_abdominis",
            "erector_spinae","full_body","rotator_cuff","serratus_anterior","tibialis_anterior","neck",
        )]
        secondary = [m for m in secondary if m in (
            "quadriceps","hamstrings","gluteus_maximus","gluteus_medius","calves","hip_flexors","adductors",
            "pectoralis_major","latissimus_dorsi","trapezius","rhomboids","rear_deltoid","front_deltoid",
            "lateral_deltoid","biceps","triceps","forearms","rectus_abdominis","obliques","transverse_abdominis",
            "erector_spinae","full_body","rotator_cuff","serratus_anterior","tibialis_anterior","neck",
        )]
        if not primary:
            primary = ["full_body"]
        slug = pid.replace("-", "_")
        en_key = f"yoga.pose.{slug}.english"
        sa_key = f"yoga.pose.{slug}.sanskrit"
        en[en_key] = en_name
        uk[en_key] = uk_name
        en[sa_key] = sanskrit
        uk[sa_key] = uk_sanskrit(sanskrit)
        obj = {
            "id": pid,
            "englishNameKeys": {"enGB": en_key, "uk": en_key},
            "sanskritNameKeys": {"enGB": sa_key, "uk": sa_key},
            "family": family,
            "level": level,
            "primaryMuscles": primary,
            "secondaryMuscles": secondary,
            "equipment": equipment,
            "technique": pose_technique(en_name, family),
            "cues": pose_cues(family),
            "breathing": "Inhale to lengthen; exhale to settle into the shape without collapsing.",
            "safetyNotes": safety(family),
            "contraindicationNotes": contraindications(family, level),
            "evidenceEntryIds": list(EVIDENCE),
            "holdSecondsDefault": hold,
            "unilateral": unilateral,
            "bundled": True,
            "aliases": [],
            "tags": ["yoga", family, level],
        }
        write_json(ROOT / "content/yoga/data/poses" / f"{pid}.json", obj)
        pose_ids.append(pid)
    return pose_ids


FLOWS = [
    ("morning-sun-salutations", "Morning sun salutations", "Ранкові вітання сонцю", "beginner", 20, "morning_energy",
     ["mountain-pose", "forward-fold", "half-forward-fold", "plank-pose", "cobra-pose", "downward-dog", "low-lunge", "warrior-i", "child-pose"]),
    ("gentle-mobility-flow", "Gentle mobility flow", "М’який потік рухливості", "beginner", 25, "mobility",
     ["easy-pose", "cat-cow", "thread-needle", "downward-dog", "low-lunge", "triangle-pose", "seated-forward-fold", "supine-twist", "corpse-pose"]),
    ("hip-opener-sequence", "Hip opener sequence", "Послідовність для стегон", "intermediate", 30, "hip_openers",
     ["mountain-pose", "goddess-pose", "garland-pose", "lizard-pose", "pigeon-pose", "butterfly-pose", "frog-pose", "happy-baby", "reclining-bound-angle"]),
    ("balance-focus-flow", "Balance focus flow", "Потік на баланс", "intermediate", 25, "strength_balance",
     ["mountain-pose", "tree-pose", "eagle-pose", "warrior-iii", "standing-split", "side-plank-yoga", "crow-pose", "child-pose"]),
    ("evening-wind-down", "Evening wind-down", "Вечірнє заспокоєння", "beginner", 20, "evening_wind_down",
     ["easy-pose", "cat-cow", "child-pose", "seated-forward-fold", "seated-twist", "legs-up-wall", "knee-to-chest", "corpse-pose"]),
    ("spinal-mobility-flow", "Spinal mobility flow", "Потік для хребта", "beginner", 20, "spinal_mobility",
     ["tabletop", "cat-cow", "thread-needle", "sphinx-pose", "locust-pose", "bridge-pose", "supine-twist", "corpse-pose"]),
    ("strength-standing-flow", "Standing strength flow", "Силовий стоячий потік", "intermediate", 35, "strength_balance",
     ["mountain-pose", "chair-pose", "warrior-i", "warrior-ii", "extended-side-angle", "reverse-warrior", "pyramid-pose", "warrior-iii", "forward-fold"]),
    ("restorative-restore", "Restorative restore", "Відновлювальна практика", "beginner", 30, "restorative",
     ["supported-child" , "legs-up-wall", "reclining-bound-angle", "supine-twist", "happy-baby", "knee-to-chest", "corpse-pose", "easy-pose"]),
    ("full-body-vinyasa", "Full-body vinyasa", "Повний віньяса", "intermediate", 40, "full_body",
     ["mountain-pose", "forward-fold", "plank-pose", "chaturanga", "upward-dog", "downward-dog", "warrior-i", "warrior-ii", "triangle-pose", "boat-pose", "bridge-pose", "corpse-pose"]),
    ("beginner-foundations", "Beginner foundations", "Основи для початківців", "beginner", 25, "mobility",
     ["mountain-pose", "chair-pose", "warrior-ii", "triangle-pose", "downward-dog", "cobra-pose", "child-pose", "bridge-pose", "corpse-pose"]),
    ("hamstring-care", "Hamstring care flow", "Потік для задньої поверхні стегна", "beginner", 25, "mobility",
     ["mountain-pose", "forward-fold", "pyramid-pose", "half-splits", "head-to-knee", "seated-forward-fold", "wide-legged-forward-fold", "legs-up-wall"]),
    ("twists-and-breath", "Twists and breath", "Скручування і дихання", "intermediate", 25, "spinal_mobility",
     ["easy-pose", "cat-cow", "crescent-lunge-twist", "revolved-head-to-knee", "seated-twist", "supine-twist", "child-pose", "corpse-pose"]),
]


def generate_flows(pose_ids: set[str], en: dict[str, str], uk: dict[str, str]) -> None:
    # Fix restorative flow fake id
    for i, flow in enumerate(FLOWS):
        fid, en_name, uk_name, level, mins, focus, poses = flow
        poses = ["child-pose" if p == "supported-child" else p for p in poses]
        FLOWS[i] = (fid, en_name, uk_name, level, mins, focus, poses)

    for fid, en_name, uk_name, level, mins, focus, poses in FLOWS:
        missing = [p for p in poses if p not in pose_ids]
        if missing:
            raise SystemExit(f"Flow {fid} missing poses: {missing}")
        slug = fid.replace("-", "_")
        name_key = f"yoga.flow.{slug}.name"
        desc_key = f"yoga.flow.{slug}.description"
        en[name_key] = en_name
        uk[name_key] = uk_name
        en[desc_key] = f"A {mins}-minute {level} yoga sequence focused on {focus.replace('_', ' ')}."
        uk[desc_key] = f"Послідовність йоги на {mins} хв ({level}), акцент: {focus.replace('_', ' ')}."
        sequence = []
        for p in poses:
            hold = 30
            for row in YOGA_POSES:
                if row[0] == p:
                    hold = row[10]
                    break
            sequence.append({"poseId": p, "holdSeconds": min(hold, 90), "side": "both" if any(r[0] == p and r[9] for r in YOGA_POSES) else "none"})
        obj = {
            "id": fid,
            "nameKeys": {"enGB": name_key, "uk": name_key},
            "descriptionKeys": {"enGB": desc_key, "uk": desc_key},
            "level": level,
            "durationMinutes": mins,
            "focus": focus,
            "poseIds": poses,
            "sequence": sequence,
            "evidenceEntryIds": list(EVIDENCE),
            "bundled": True,
            "equipmentSuggested": ["yoga_mat", "yoga_block", "yoga_strap", "bodyweight"],
        }
        write_json(ROOT / "content/yoga/data/flows" / f"{fid}.json", obj)


PILATES = [
    # id, en, uk, apparatus, level, primary, secondary, equipment, tracking, reps/seconds, unilateral
    ("pilates-hundred", "The hundred", "Сотня", "mat", "beginner", ["rectus_abdominis", "transverse_abdominis"], ["hip_flexors"], ["pilates_mat", "yoga_mat", "bodyweight"], "breaths", 100, False),
    ("pilates-roll-up", "Roll-up", "Скручування вгору", "mat", "intermediate", ["rectus_abdominis"], ["hip_flexors"], ["pilates_mat", "bodyweight"], "reps", 8, False),
    ("pilates-roll-over", "Roll-over", "Перекат назад", "mat", "intermediate", ["rectus_abdominis", "erector_spinae"], ["hamstrings"], ["pilates_mat", "bodyweight"], "reps", 6, False),
    ("pilates-single-leg-circle", "Single-leg circles", "Кола однією ногою", "mat", "beginner", ["hip_flexors", "adductors"], ["transverse_abdominis"], ["pilates_mat", "bodyweight"], "reps", 8, True),
    ("pilates-rolling-like-ball", "Rolling like a ball", "Перекат клубочком", "mat", "beginner", ["rectus_abdominis"], ["hip_flexors"], ["pilates_mat", "bodyweight"], "reps", 8, False),
    ("pilates-single-leg-stretch", "Single-leg stretch", "Розтяг однієї ноги", "mat", "beginner", ["rectus_abdominis", "transverse_abdominis"], ["hip_flexors"], ["pilates_mat", "bodyweight"], "reps", 10, True),
    ("pilates-double-leg-stretch", "Double-leg stretch", "Розтяг двох ніг", "mat", "intermediate", ["rectus_abdominis", "transverse_abdominis"], ["hip_flexors"], ["pilates_mat", "bodyweight"], "reps", 8, False),
    ("pilates-scissors", "Scissors", "Ножиці", "mat", "intermediate", ["rectus_abdominis", "hip_flexors"], ["hamstrings"], ["pilates_mat", "bodyweight"], "reps", 10, True),
    ("pilates-lower-lift", "Lower lift", "Опускання ніг", "mat", "intermediate", ["rectus_abdominis", "hip_flexors"], ["transverse_abdominis"], ["pilates_mat", "bodyweight"], "reps", 8, False),
    ("pilates-criss-cross", "Criss-cross", "Навхрест", "mat", "intermediate", ["obliques", "rectus_abdominis"], ["hip_flexors"], ["pilates_mat", "bodyweight"], "reps", 10, True),
    ("pilates-spine-stretch-forward", "Spine stretch forward", "Розтяг хребта вперед", "mat", "beginner", ["erector_spinae", "hamstrings"], ["transverse_abdominis"], ["pilates_mat", "bodyweight"], "reps", 6, False),
    ("pilates-open-leg-rocker", "Open-leg rocker", "Гойдалка з відкритими ногами", "mat", "advanced", ["rectus_abdominis", "hip_flexors"], ["hamstrings"], ["pilates_mat", "bodyweight"], "reps", 6, False),
    ("pilates-corkscrew", "Corkscrew", "Штопор", "mat", "advanced", ["obliques", "rectus_abdominis"], ["hip_flexors"], ["pilates_mat", "bodyweight"], "reps", 6, False),
    ("pilates-saw", "Saw", "Пилка", "mat", "beginner", ["obliques", "hamstrings"], ["erector_spinae"], ["pilates_mat", "bodyweight"], "reps", 6, True),
    ("pilates-swan", "Swan", "Лебідь", "mat", "beginner", ["erector_spinae"], ["gluteus_maximus", "triceps"], ["pilates_mat", "bodyweight"], "reps", 6, False),
    ("pilates-swimming", "Swimming", "Плавання", "mat", "beginner", ["erector_spinae", "gluteus_maximus"], ["rear_deltoid", "hamstrings"], ["pilates_mat", "bodyweight"], "timed", 30, False),
    ("pilates-shoulder-bridge", "Shoulder bridge", "Плечовий міст", "mat", "beginner", ["gluteus_maximus", "hamstrings"], ["erector_spinae"], ["pilates_mat", "bodyweight"], "reps", 8, False),
    ("pilates-teaser", "Teaser", "Тізер", "mat", "advanced", ["rectus_abdominis", "hip_flexors"], ["transverse_abdominis"], ["pilates_mat", "bodyweight"], "reps", 5, False),
    ("pilates-side-kick-front-back", "Side kick — front/back", "Бічний удар — вперед/назад", "mat", "beginner", ["gluteus_medius", "gluteus_maximus"], ["obliques"], ["pilates_mat", "bodyweight"], "reps", 10, True),
    ("pilates-side-kick-circles", "Side kick — circles", "Бічний удар — кола", "mat", "intermediate", ["gluteus_medius"], ["obliques"], ["pilates_mat", "bodyweight"], "reps", 8, True),
    ("pilates-side-plank", "Side plank (pilates)", "Бічна планка (пілатес)", "mat", "intermediate", ["obliques", "serratus_anterior"], ["gluteus_medius"], ["pilates_mat", "bodyweight"], "hold", 20, True),
    ("pilates-leg-pull-front", "Leg pull front", "Підйом ноги в планці", "mat", "advanced", ["rectus_abdominis", "gluteus_maximus"], ["shoulders"], ["pilates_mat", "bodyweight"], "reps", 6, True),
    ("pilates-leg-pull-back", "Leg pull back", "Підйом ноги в зворотному упорі", "mat", "advanced", ["gluteus_maximus", "triceps"], ["hamstrings"], ["pilates_mat", "bodyweight"], "reps", 6, True),
    ("pilates-push-up-series", "Pilates push-up series", "Серія віджимань пілатесу", "mat", "intermediate", ["pectoralis_major", "triceps"], ["rectus_abdominis", "serratus_anterior"], ["pilates_mat", "bodyweight"], "reps", 6, False),
    ("pilates-mermaid", "Mermaid stretch", "Русалка", "mat", "beginner", ["obliques", "latissimus_dorsi"], ["adductors"], ["pilates_mat", "bodyweight"], "reps", 4, True),
    ("pilates-spine-twist", "Spine twist", "Скручування хребта", "mat", "beginner", ["obliques"], ["erector_spinae"], ["pilates_mat", "bodyweight"], "reps", 8, False),
    ("pilates-neck-pull", "Neck pull", "Тяга шиї", "mat", "advanced", ["rectus_abdominis", "erector_spinae"], ["hip_flexors"], ["pilates_mat", "bodyweight"], "reps", 5, False),
    ("pilates-jackknife", "Jackknife", "Ніж", "mat", "advanced", ["rectus_abdominis", "erector_spinae"], ["hamstrings"], ["pilates_mat", "bodyweight"], "reps", 5, False),
    ("pilates-hip-circles", "Hip circles", "Кола стегнами", "mat", "intermediate", ["hip_flexors", "obliques"], ["transverse_abdominis"], ["pilates_mat", "bodyweight"], "reps", 6, False),
    ("pilates-magic-circle-squeeze", "Magic circle squeeze", "Стискання кільця", "mat", "beginner", ["adductors", "transverse_abdominis"], ["gluteus_medius"], ["pilates_mat", "magic_circle", "bodyweight"], "reps", 12, False),
    # Reformer
    ("reformer-footwork", "Reformer footwork", "Робота стопами на реформері", "reformer", "beginner", ["quadriceps", "gluteus_maximus"], ["calves", "hamstrings"], ["pilates_reformer"], "reps", 10, False),
    ("reformer-leg-circles", "Reformer leg circles", "Кола ногами на реформері", "reformer", "beginner", ["hip_flexors", "adductors"], ["transverse_abdominis"], ["pilates_reformer"], "reps", 8, True),
    ("reformer-hundred", "Reformer hundred", "Сотня на реформері", "reformer", "intermediate", ["rectus_abdominis", "transverse_abdominis"], ["hip_flexors"], ["pilates_reformer"], "breaths", 100, False),
    ("reformer-coordination", "Coordination", "Координація", "reformer", "intermediate", ["rectus_abdominis", "triceps"], ["hip_flexors"], ["pilates_reformer"], "reps", 6, False),
    ("reformer-row-arms", "Rowing — arms", "Гребля — руки", "reformer", "beginner", ["rhomboids", "latissimus_dorsi"], ["biceps", "rear_deltoid"], ["pilates_reformer"], "reps", 8, False),
    ("reformer-long-stretch", "Long stretch", "Довгий розтяг", "reformer", "intermediate", ["rectus_abdominis", "serratus_anterior"], ["shoulders", "gluteus_maximus"], ["pilates_reformer"], "reps", 6, False),
    ("reformer-down-stretch", "Down stretch", "Нижній розтяг", "reformer", "intermediate", ["hip_flexors", "erector_spinae"], ["shoulders"], ["pilates_reformer"], "reps", 6, False),
    ("reformer-elephant", "Elephant", "Слон", "reformer", "beginner", ["hamstrings", "calves"], ["transverse_abdominis"], ["pilates_reformer"], "reps", 8, False),
    ("reformer-stomach-massage", "Stomach massage series", "Серія massage живота", "reformer", "intermediate", ["quadriceps", "transverse_abdominis"], ["hip_flexors"], ["pilates_reformer"], "reps", 8, False),
    ("reformer-short-box-round", "Short box — round back", "Коротка коробка — кругла спина", "reformer", "beginner", ["rectus_abdominis"], ["hip_flexors"], ["pilates_reformer", "pilates_box"], "reps", 6, False),
    ("reformer-short-box-flat", "Short box — flat back", "Коротка коробка — рівна спина", "reformer", "intermediate", ["rectus_abdominis", "hip_flexors"], ["erector_spinae"], ["pilates_reformer", "pilates_box"], "reps", 6, False),
    ("reformer-short-box-side", "Short box — side", "Коротка коробка — бік", "reformer", "intermediate", ["obliques"], ["quadriceps"], ["pilates_reformer", "pilates_box"], "reps", 6, True),
    ("reformer-knee-stretches", "Knee stretches", "Розтяги з колін", "reformer", "intermediate", ["hip_flexors", "rectus_abdominis"], ["gluteus_maximus"], ["pilates_reformer"], "reps", 8, False),
    ("reformer-running", "Running on the reformer", "Біг на реформері", "reformer", "beginner", ["calves", "quadriceps"], ["gluteus_maximus"], ["pilates_reformer"], "reps", 20, False),
    ("reformer-pelvic-press", "Pelvic press", "Тиснення тазом", "reformer", "beginner", ["gluteus_maximus", "hamstrings"], ["erector_spinae"], ["pilates_reformer"], "reps", 8, False),
    ("reformer-single-leg-stretch", "Reformer single-leg stretch", "Розтяг однієї ноги на реформері", "reformer", "beginner", ["rectus_abdominis", "hip_flexors"], ["transverse_abdominis"], ["pilates_reformer"], "reps", 10, True),
    ("reformer-arm-circles", "Arm circles on straps", "Кола руками на ременях", "reformer", "beginner", ["front_deltoid", "pectoralis_major"], ["serratus_anterior"], ["pilates_reformer", "pilates_strap"], "reps", 8, False),
    ("reformer-chest-expansion", "Chest expansion", "Розкриття грудей", "reformer", "beginner", ["rear_deltoid", "rhomboids"], ["triceps"], ["pilates_reformer", "pilates_strap"], "reps", 8, False),
    ("reformer-side-splits", "Side splits", "Бічні шпагати на каретці", "reformer", "intermediate", ["adductors", "gluteus_medius"], ["quadriceps"], ["pilates_reformer"], "reps", 6, False),
    ("reformer-front-splits", "Front splits", "Передні шпагати на каретці", "reformer", "advanced", ["hip_flexors", "hamstrings"], ["gluteus_maximus"], ["pilates_reformer"], "reps", 6, True),
]


def fix_pil_muscles(ms: list[str]) -> list[str]:
    out = []
    for m in ms:
        if m == "shoulders":
            m = "front_deltoid"
        if m not in out:
            out.append(m)
    return out


def generate_pilates(en: dict[str, str], uk: dict[str, str]) -> list[str]:
    ids = []
    for row in PILATES:
        (pid, en_name, uk_name, apparatus, level, primary, secondary, equipment, tracking, n, unilateral) = row
        primary = fix_pil_muscles(primary)
        secondary = fix_pil_muscles(secondary)
        slug = pid.replace("-", "_")
        key = f"pilates.exercise.{slug}.name"
        en[key] = en_name
        uk[key] = uk_name
        obj = {
            "id": pid,
            "nameKeys": {"enGB": key, "uk": key},
            "apparatus": apparatus,
            "level": level,
            "primaryMuscles": primary,
            "secondaryMuscles": secondary,
            "equipment": equipment,
            "technique": [
                f"Perform {en_name} with a braced centre, neutral or imprinted pelvis as coached, and smooth carriage or limb control.",
                "Prioritise articulation and breath over range; reduce springs or amplitude if form breaks.",
            ],
            "cues": [
                "Keep ribs heavy and shoulders away from the ears.",
                "Exhale on effort; inhale to prepare or lengthen.",
            ],
            "breathing": "Coordinate breath with each repetition; never hold the breath through effort.",
            "safetyNotes": [
                "Use a head support or keep the head down if the neck fatigues.",
                "Reduce load (springs, lever, or range) rather than forcing lumbar flexion or extension.",
            ],
            "contraindicationNotes": [
                "General safety: seek clinician guidance for acute spinal pain, recent surgery, or unmanaged medical conditions before loaded pilates work.",
                "General safety: during pregnancy or postpartum, favour clinician-cleared regressions and avoid breath-holding or extreme abdominal pressure.",
            ],
            "evidenceEntryIds": list(EVIDENCE),
            "trackingType": tracking,
            "unilateral": unilateral,
            "bundled": True,
            "aliases": [],
            "tags": ["pilates", apparatus, level],
        }
        if tracking in ("reps", "breaths"):
            obj["defaultReps"] = int(n)
        else:
            obj["defaultSeconds"] = int(n)
        write_json(ROOT / "content/pilates/data/exercises" / f"{pid}.json", obj)
        ids.append(pid)
    return ids


SESSIONS = [
    ("mat-core-foundations", "Mat core foundations", "Основи кора на маті", "beginner", 25, "mat", "core",
     ["pilates-hundred", "pilates-single-leg-stretch", "pilates-double-leg-stretch", "pilates-spine-stretch-forward", "pilates-saw", "pilates-shoulder-bridge", "pilates-swan"]),
    ("mat-full-body-flow", "Mat full-body flow", "Повне тіло на маті", "intermediate", 35, "mat", "full_body",
     ["pilates-hundred", "pilates-roll-up", "pilates-single-leg-circle", "pilates-criss-cross", "pilates-swan", "pilates-swimming", "pilates-side-kick-front-back", "pilates-push-up-series"]),
    ("mat-posture-reset", "Mat posture reset", "Осанка на маті", "beginner", 20, "mat", "posture",
     ["pilates-spine-stretch-forward", "pilates-spine-twist", "pilates-swan", "pilates-swimming", "pilates-shoulder-bridge", "pilates-mermaid"]),
    ("mat-hips-glutes", "Mat hips and glutes", "Стегна і сідниці на маті", "intermediate", 30, "mat", "hips_glutes",
     ["pilates-shoulder-bridge", "pilates-side-kick-front-back", "pilates-side-kick-circles", "pilates-side-plank", "pilates-single-leg-circle", "pilates-magic-circle-squeeze"]),
    ("mat-athletic-core", "Athletic mat core", "Атлетичний кор на маті", "advanced", 30, "mat", "athletic",
     ["pilates-hundred", "pilates-teaser", "pilates-corkscrew", "pilates-jackknife", "pilates-leg-pull-front", "pilates-leg-pull-back", "pilates-open-leg-rocker"]),
    ("reformer-foundations", "Reformer foundations", "Основи на реформері", "beginner", 30, "reformer", "full_body",
     ["reformer-footwork", "reformer-leg-circles", "reformer-hundred", "reformer-row-arms", "reformer-elephant", "reformer-running", "reformer-pelvic-press"]),
    ("reformer-core-power", "Reformer core power", "Сила кора на реформері", "intermediate", 35, "reformer", "core",
     ["reformer-hundred", "reformer-coordination", "reformer-long-stretch", "reformer-knee-stretches", "reformer-short-box-round", "reformer-short-box-flat", "reformer-single-leg-stretch"]),
    ("reformer-posture-pull", "Reformer posture and pull", "Осанка і тяга на реформері", "beginner", 25, "reformer", "posture",
     ["reformer-row-arms", "reformer-chest-expansion", "reformer-arm-circles", "reformer-short-box-flat", "reformer-elephant", "reformer-pelvic-press"]),
    ("reformer-athletic-split", "Reformer athletic splits", "Атлетичні шпагати на реформері", "advanced", 35, "reformer", "athletic",
     ["reformer-footwork", "reformer-front-splits", "reformer-side-splits", "reformer-long-stretch", "reformer-down-stretch", "reformer-knee-stretches", "reformer-stomach-massage"]),
    ("mixed-mat-reformer-bridge", "Mat-to-reformer bridge", "Місток мат–реформер", "intermediate", 40, "mixed", "full_body",
     ["pilates-hundred", "pilates-roll-up", "pilates-swan", "reformer-footwork", "reformer-hundred", "reformer-long-stretch", "reformer-row-arms", "reformer-pelvic-press"]),
]


def generate_sessions(ex_ids: set[str], en: dict[str, str], uk: dict[str, str]) -> None:
    for sid, en_name, uk_name, level, mins, apparatus, focus, exercises in SESSIONS:
        missing = [e for e in exercises if e not in ex_ids]
        if missing:
            raise SystemExit(f"Session {sid} missing: {missing}")
        slug = sid.replace("-", "_")
        name_key = f"pilates.session.{slug}.name"
        desc_key = f"pilates.session.{slug}.description"
        block_key = f"pilates.session.{slug}.block_main"
        en[name_key] = en_name
        uk[name_key] = uk_name
        en[desc_key] = f"A {mins}-minute {level} pilates session ({apparatus}) focused on {focus.replace('_', ' ')}."
        uk[desc_key] = f"Сесія пілатесу на {mins} хв ({level}, {apparatus}), акцент: {focus.replace('_', ' ')}."
        en[block_key] = "Main series"
        uk[block_key] = "Основна серія"
        items = []
        for eid in exercises:
            item = {"exerciseId": eid, "sets": 1}
            # look up defaults
            for row in PILATES:
                if row[0] == eid:
                    if row[8] in ("reps", "breaths"):
                        item["reps"] = int(row[9]) if row[8] != "breaths" else 10
                    else:
                        item["seconds"] = int(row[9])
                    break
            items.append(item)
        obj = {
            "id": sid,
            "nameKeys": {"enGB": name_key, "uk": name_key},
            "descriptionKeys": {"enGB": desc_key, "uk": desc_key},
            "level": level,
            "durationMinutes": mins,
            "apparatus": apparatus,
            "focus": focus,
            "exerciseIds": exercises,
            "blocks": [{"titleKey": block_key, "items": items}],
            "evidenceEntryIds": list(EVIDENCE),
            "bundled": True,
        }
        write_json(ROOT / "content/pilates/data/sessions" / f"{sid}.json", obj)


def main() -> None:
    en: dict[str, str] = {}
    uk: dict[str, str] = {}
    pose_ids = generate_yoga(en, uk)
    print(f"Yoga poses: {len(pose_ids)}")
    generate_flows(set(pose_ids), en, uk)
    print(f"Yoga flows: {len(FLOWS)}")
    write_json(ROOT / "content/yoga/locales/en-GB.json", dict(sorted(en.items())))
    write_json(ROOT / "content/yoga/locales/uk.json", dict(sorted(uk.items())))

    en_p: dict[str, str] = {}
    uk_p: dict[str, str] = {}
    ex_ids = generate_pilates(en_p, uk_p)
    print(f"Pilates exercises: {len(ex_ids)}")
    generate_sessions(set(ex_ids), en_p, uk_p)
    print(f"Pilates sessions: {len(SESSIONS)}")
    write_json(ROOT / "content/pilates/locales/en-GB.json", dict(sorted(en_p.items())))
    write_json(ROOT / "content/pilates/locales/uk.json", dict(sorted(uk_p.items())))


if __name__ == "__main__":
    main()
