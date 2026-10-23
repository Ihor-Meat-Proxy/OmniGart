#!/usr/bin/env python3
"""Generate achievement catalogue JSON + en-GB/uk locale strings.

Healthy-behaviour constraints are enforced here so the catalogue never rewards
extreme dieting, dehydration, very low calories, or weight-loss speed.
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "data"
LOCALES = ROOT / "locales"

Tier = list  # convenience


def tiers_bsp(bronze: float, silver: float, gold: float, platinum: float) -> list[dict]:
    return [
        {"id": "bronze", "threshold": bronze},
        {"id": "silver", "threshold": silver},
        {"id": "gold", "threshold": gold},
        {"id": "platinum", "threshold": platinum},
    ]


def tiers_levels(*values: float) -> list[dict]:
    return [{"id": f"level_{i}", "threshold": v} for i, v in enumerate(values, start=1)]


def unlocked(threshold: float = 1) -> list[dict]:
    return [{"id": "unlocked", "threshold": threshold}]


def ach(
    id_: str,
    category: str,
    type_: str,
    tiers: list[dict],
    rule: dict,
    icon: str,
    title_en: str,
    title_uk: str,
    desc_en: str,
    desc_uk: str,
    *,
    hidden: bool = False,
    repeatable: bool = False,
    healthy_notes: str | None = None,
) -> dict:
    key_base = id_.replace(".", "_")
    # string keys use dotted id after achievement.
    title_key = f"achievement.{id_}.title"
    desc_key = f"achievement.{id_}.description"
    item = {
        "id": id_,
        "category": category,
        "type": type_,
        "tiers": tiers,
        "rule": rule,
        "iconId": icon,
        "hidden": hidden,
        "repeatable": repeatable,
        "titleKey": title_key,
        "descriptionKey": desc_key,
        "_strings": {
            "en": {title_key: title_en, desc_key: desc_en},
            "uk": {title_key: title_uk, desc_key: desc_uk},
        },
    }
    if healthy_notes:
        item["healthyNotes"] = healthy_notes
    return item


def rule(
    metric: str,
    aggregation: str,
    window: dict,
    *,
    threshold: float | None = None,
    filters: dict | None = None,
    compare: str = "gte",
) -> dict:
    r: dict = {
        "metric": metric,
        "aggregation": aggregation,
        "window": window,
        "compare": compare,
    }
    if threshold is not None:
        r["threshold"] = threshold
    if filters:
        r["filters"] = filters
    return r


def build() -> list[dict]:
    a: list[dict] = []

    # ── GENERAL / CONSISTENCY (15) ──────────────────────────────────────────
    a.append(
        ach(
            "general.first_workout",
            "general",
            "discovery",
            unlocked(),
            rule("workouts_completed", "count", {"kind": "lifetime"}, threshold=1),
            "workouts",
            "First workout",
            "Перше тренування",
            "Complete any workout to get started.",
            "Завершіть будь-яке тренування, щоб розпочати.",
        )
    )
    a.append(
        ach(
            "general.disciplines_explorer",
            "general",
            "discovery",
            tiers_bsp(2, 3, 4, 5),
            rule("disciplines_tried", "distinct", {"kind": "lifetime"}),
            "discovery",
            "Discipline explorer",
            "Дослідник дисциплін",
            "Try different training disciplines over time.",
            "Спробуйте різні дисципліни тренувань з часом.",
        )
    )
    a.append(
        ach(
            "general.weekly_consistency",
            "general",
            "streak",
            tiers_bsp(2, 4, 8, 12),
            rule(
                "workouts_completed",
                "streak",
                {
                    "kind": "streak_days",
                    "qualify": "activity_or_planned_rest",
                    "restDayTolerancePerWeek": 1,
                    "freezeTokensAllowed": True,
                    "maxFreezePerStreak": 2,
                },
                filters={"discipline": ["strength", "running", "swimming", "yoga", "pilates"]},
            ),
            "streak",
            "Steady weeks",
            "Стабільні тижні",
            "Keep a weekly rhythm with planned rest days counting toward your streak.",
            "Тримайте тижневий ритм: заплановані дні відпочинку зараховуються до серії.",
            healthy_notes="Rest-tolerant streak; freeze tokens allowed; does not require daily hard sessions.",
        )
    )
    a.append(
        ach(
            "general.logging_habit",
            "general",
            "streak",
            tiers_bsp(7, 30, 90, 180),
            rule(
                "food_days_logged",
                "streak",
                {
                    "kind": "streak_days",
                    "qualify": "log",
                    "restDayTolerancePerWeek": 1,
                    "freezeTokensAllowed": True,
                    "maxFreezePerStreak": 3,
                },
            ),
            "food-log",
            "Logging habit",
            "Звичка записувати",
            "Log something (food, water, workout, or sleep) on consecutive days.",
            "Записуйте щось (їжу, воду, тренування чи сон) кілька днів поспіль.",
            healthy_notes="Rewards logging consistency, not calorie targets.",
        )
    )
    a.append(
        ach(
            "general.workout_milestones",
            "general",
            "milestone",
            tiers_bsp(10, 50, 100, 250),
            rule("workouts_completed", "count", {"kind": "lifetime"}),
            "trophy",
            "Workout milestones",
            "Віхи тренувань",
            "Accumulate completed workouts across all disciplines.",
            "Накопичуйте завершені тренування в усіх дисциплінах.",
        )
    )
    a.append(
        ach(
            "general.honour_rest",
            "general",
            "milestone",
            tiers_bsp(4, 12, 26, 52),
            rule("planned_rest_honoured", "count", {"kind": "lifetime"}),
            "recovery",
            "Rest respected",
            "Відпочинок у пошані",
            "Honour planned rest days when readiness suggests recovery.",
            "Дотримуйтесь запланованих днів відпочинку, коли готовність підказує відновлення.",
            healthy_notes="Explicitly rewards resting when recovery signals call for it.",
        )
    )
    a.append(
        ach(
            "general.freeze_wise",
            "general",
            "discovery",
            unlocked(),
            rule("freeze_tokens_used", "count", {"kind": "lifetime"}, threshold=1),
            "freeze-token",
            "Wise freeze",
            "Розумна пауза",
            "Use a freeze token once instead of forcing a session while unwell.",
            "Один раз використайте жетон паузи замість примусового тренування під час нездужання.",
            healthy_notes="Normalises protecting streaks without overtraining.",
        )
    )
    a.append(
        ach(
            "general.morning_or_evening_rhythm",
            "general",
            "streak",
            tiers_bsp(5, 14, 30, 60),
            rule(
                "workouts_completed",
                "streak",
                {
                    "kind": "streak_days",
                    "qualify": "activity_or_planned_rest",
                    "restDayTolerancePerWeek": 2,
                    "freezeTokensAllowed": True,
                    "maxFreezePerStreak": 2,
                },
            ),
            "streak",
            "Rhythm keeper",
            "Хранитель ритму",
            "Build a flexible activity rhythm that includes rest.",
            "Побудуйте гнучкий ритм активності з урахуванням відпочинку.",
        )
    )
    a.append(
        ach(
            "general.multi_discipline_week",
            "general",
            "single_session",
            tiers_bsp(2, 3, 4, 5),
            rule(
                "disciplines_tried",
                "distinct",
                {"kind": "calendar", "unit": "week", "count": 1},
            ),
            "discovery",
            "Varied week",
            "Різноманітний тиждень",
            "Train across multiple disciplines in one calendar week.",
            "Тренуйтесь у кількох дисциплінах протягом одного календарного тижня.",
        )
    )
    a.append(
        ach(
            "general.session_minutes",
            "general",
            "milestone",
            tiers_bsp(300, 1000, 3000, 10000),
            rule("session_duration_min", "sum", {"kind": "lifetime"}),
            "timer",
            "Time on the mat and floor",
            "Час на килимку й у залі",
            "Accumulate minutes of completed training sessions.",
            "Накопичуйте хвилини завершених тренувальних сесій.",
        )
    )
    a.append(
        ach(
            "general.early_consistency",
            "general",
            "challenge",
            unlocked(3),
            rule(
                "workouts_completed",
                "count",
                {"kind": "challenge_period", "period": "current_month"},
                threshold=3,
            ),
            "challenge",
            "Month opener",
            "Старт місяця",
            "Complete three workouts in the current month.",
            "Завершіть три тренування в поточному місяці.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "general.balanced_month",
            "general",
            "challenge",
            unlocked(8),
            rule(
                "workouts_completed",
                "count",
                {"kind": "challenge_period", "period": "current_month"},
                threshold=8,
                filters={"discipline": ["strength", "running", "swimming", "yoga", "pilates"]},
            ),
            "challenge",
            "Balanced month",
            "Збалансований місяць",
            "Complete eight sessions this month without skipping planned rest.",
            "Завершіть вісім сесій цього місяця, не пропускаючи запланований відпочинок.",
            repeatable=True,
            healthy_notes="Volume target sits below daily grind; rest is expected.",
        )
    )
    a.append(
        ach(
            "general.secret_comeback",
            "general",
            "discovery",
            unlocked(),
            rule("workouts_completed", "count", {"kind": "rolling_days", "days": 14}, threshold=3),
            "achievement",
            "Quiet comeback",
            "Тихе повернення",
            "Return with three sessions within two weeks after a break.",
            "Поверніться з трьома сесіями протягом двох тижнів після перерви.",
            hidden=True,
        )
    )
    a.append(
        ach(
            "general.year_of_movement",
            "general",
            "milestone",
            tiers_levels(50, 100, 150, 200, 300),
            rule("workouts_completed", "count", {"kind": "calendar", "unit": "month", "count": 12}),
            "trophy",
            "Year of movement",
            "Рік руху",
            "Accumulate workouts across a rolling year of training.",
            "Накопичуйте тренування протягом року.",
        )
    )
    a.append(
        ach(
            "general.share_ready",
            "general",
            "discovery",
            unlocked(),
            rule("workouts_completed", "count", {"kind": "lifetime"}, threshold=5),
            "share",
            "Ready to share",
            "Готові поділитися",
            "Reach five workouts — enough history to share progress if you choose.",
            "Досягніть п'яти тренувань — достатньо історії, щоб поділитися прогресом за бажанням.",
        )
    )
    a.append(
        ach(
            "general.evidence_curious",
            "general",
            "discovery",
            unlocked(),
            rule("workouts_completed", "count", {"kind": "lifetime"}, threshold=1),
            "evidence",
            "Evidence curious",
            "Цікавість до доказів",
            "Start training — achievements here favour habits backed by behaviour-change evidence.",
            "Почніть тренуватись — досягнення тут підтримують звички, підкріплені доказами зміни поведінки.",
            hidden=True,
        )
    )

    # ── STRENGTH (20) ───────────────────────────────────────────────────────
    a.append(
        ach(
            "strength.first_session",
            "strength",
            "discovery",
            unlocked(),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                threshold=1,
                filters={"discipline": ["strength"]},
            ),
            "strength",
            "First strength session",
            "Перша силова сесія",
            "Complete your first strength workout.",
            "Завершіть перше силове тренування.",
        )
    )
    a.append(
        ach(
            "strength.sessions",
            "strength",
            "milestone",
            tiers_bsp(5, 25, 75, 200),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                filters={"discipline": ["strength"]},
            ),
            "strength",
            "Strength sessions",
            "Силові сесії",
            "Complete strength workouts over time.",
            "Завершуйте силові тренування з часом.",
        )
    )
    a.append(
        ach(
            "strength.volume_tonnes",
            "strength",
            "milestone",
            tiers_bsp(1000, 10000, 50000, 100000),
            rule("volume_kg", "sum", {"kind": "lifetime"}, filters={"discipline": ["strength"]}),
            "weight",
            "Tonnes moved",
            "Тонни піднято",
            "Accumulate lifetime lifting volume (tier thresholds: 1 / 10 / 50 / 100 tonnes).",
            "Накопичуйте lifetime-обсяг підйомів (пороги рівнів: 1 / 10 / 50 / 100 тонн).",
            healthy_notes="Volume milestones in kg (display as tonnes); not body-weight change.",
        )
    )
    a.append(
        ach(
            "strength.sets_logged",
            "strength",
            "milestone",
            tiers_bsp(50, 250, 1000, 5000),
            rule("sets_logged", "count", {"kind": "lifetime"}, filters={"discipline": ["strength"]}),
            "add-set",
            "Sets in the book",
            "Підходи в журналі",
            "Log working sets across strength sessions.",
            "Записуйте робочі підходи на силових сесіях.",
        )
    )
    a.append(
        ach(
            "strength.weekly_practice",
            "strength",
            "streak",
            tiers_bsp(2, 6, 12, 24),
            rule(
                "workouts_completed",
                "streak",
                {
                    "kind": "streak_days",
                    "qualify": "activity_or_planned_rest",
                    "restDayTolerancePerWeek": 2,
                    "freezeTokensAllowed": True,
                    "maxFreezePerStreak": 2,
                },
                filters={"discipline": ["strength"]},
            ),
            "streak",
            "Strength rhythm",
            "Силовий ритм",
            "Keep a strength practice with planned rest days included.",
            "Тримайте силову практику із запланованими днями відпочинку.",
            healthy_notes="Streak counts planned rest; tolerance for life events via freeze tokens.",
        )
    )
    for lift, label_en, label_uk in [
        ("squat", "Squat PR", "ПР у присіді"),
        ("hinge", "Hinge PR", "ПР у тязі"),
        ("press", "Press PR", "ПР у жимі"),
        ("pull", "Pull PR", "ПР у тязі на себе"),
    ]:
        a.append(
            ach(
                f"strength.pr_{lift}",
                "strength",
                "personal_record",
                tiers_bsp(1, 3, 10, 25),
                rule(
                    "personal_record_events",
                    "count",
                    {"kind": "lifetime"},
                    filters={"discipline": ["strength"]},
                ),
                "personal-record",
                label_en,
                label_uk,
                f"Set personal records in the {lift} pattern family.",
                f"Встановлюйте особисті рекорди в сімействі рухів «{label_uk.split()[-1]}».",
            )
        )
    a.append(
        ach(
            "strength.long_session",
            "strength",
            "single_session",
            tiers_bsp(45, 60, 75, 90),
            rule(
                "session_duration_min",
                "max",
                {"kind": "single_event"},
                filters={"discipline": ["strength"]},
            ),
            "timer",
            "Solid session",
            "Солідна сесія",
            "Complete a focused strength session of meaningful duration.",
            "Завершіть зосереджену силову сесію достатньої тривалості.",
            healthy_notes="Caps emphasise quality sessions, not marathon overreach.",
        )
    )
    a.append(
        ach(
            "strength.session_volume",
            "strength",
            "single_session",
            tiers_bsp(2000, 5000, 10000, 20000),
            rule(
                "volume_kg",
                "sum",
                {"kind": "single_event"},
                filters={"discipline": ["strength"]},
            ),
            "weight",
            "Session volume",
            "Обсяг сесії",
            "Move substantial volume in a single strength session.",
            "Підніміть значний обсяг за одну силову сесію.",
        )
    )
    a.append(
        ach(
            "strength.month_challenge",
            "strength",
            "challenge",
            unlocked(8),
            rule(
                "workouts_completed",
                "count",
                {"kind": "challenge_period", "period": "current_month"},
                threshold=8,
                filters={"discipline": ["strength"]},
            ),
            "challenge",
            "Strength month",
            "Силовий місяць",
            "Complete eight strength sessions this month.",
            "Завершіть вісім силових сесій цього місяця.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "strength.compound_focus",
            "strength",
            "milestone",
            tiers_bsp(10, 40, 100, 250),
            rule(
                "sets_logged",
                "count",
                {"kind": "lifetime"},
                filters={"discipline": ["strength"]},
            ),
            "muscle-legs",
            "Compound craft",
            "Майстерність базових",
            "Accumulate sets on compound strength patterns.",
            "Накопичуйте підходи в базових силових патернах.",
        )
    )
    a.append(
        ach(
            "strength.secret_pr_night",
            "strength",
            "personal_record",
            unlocked(),
            rule(
                "personal_record_events",
                "exists",
                {"kind": "single_event"},
                threshold=1,
                filters={"discipline": ["strength"]},
            ),
            "personal-record",
            "Quiet PR",
            "Тихий рекорд",
            "Set a personal record without fanfare.",
            "Встановіть особистий рекорд без зайвого шуму.",
            hidden=True,
        )
    )
    a.append(
        ach(
            "strength.deload_honour",
            "strength",
            "discovery",
            unlocked(),
            rule("planned_rest_honoured", "count", {"kind": "rolling_days", "days": 14}, threshold=2),
            "recovery",
            "Deload honour",
            "Повага до розвантаження",
            "Take planned easier or rest days during a deload window.",
            "Візьміть заплановані легші чи відпочинкові дні під час розвантаження.",
            healthy_notes="Rewards smart unloading, not grinding through fatigue.",
        )
    )
    a.append(
        ach(
            "strength.year_volume",
            "strength",
            "milestone",
            tiers_levels(5000, 20000, 50000, 80000, 120000),
            rule(
                "volume_kg",
                "sum",
                {"kind": "calendar", "unit": "month", "count": 12},
                filters={"discipline": ["strength"]},
            ),
            "weight",
            "Annual volume",
            "Річний обсяг",
            "Accumulate volume across a training year (thresholds in kg).",
            "Накопичуйте обсяг протягом тренувального року (пороги в кг).",
        )
    )
    a.append(
        ach(
            "strength.three_week_block",
            "strength",
            "challenge",
            unlocked(6),
            rule(
                "workouts_completed",
                "count",
                {"kind": "rolling_days", "days": 21},
                threshold=6,
                filters={"discipline": ["strength"]},
            ),
            "challenge",
            "Three-week block",
            "Тритижневий блок",
            "Complete six strength sessions in three weeks.",
            "Завершіть шість силових сесій за три тижні.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "strength.unilateral_sets",
            "strength",
            "milestone",
            tiers_bsp(20, 80, 200, 500),
            rule("sets_logged", "count", {"kind": "lifetime"}, filters={"discipline": ["strength"]}),
            "muscle-legs",
            "Single-leg craft",
            "Майстерність на одну ногу",
            "Log many unilateral strength sets over time.",
            "Записуйте багато односторонніх силових підходів з часом.",
        )
    )
    a.append(
        ach(
            "strength.upper_lower_week",
            "strength",
            "challenge",
            unlocked(4),
            rule(
                "workouts_completed",
                "count",
                {"kind": "calendar", "unit": "week", "count": 1},
                threshold=4,
                filters={"discipline": ["strength"]},
            ),
            "challenge",
            "Four-session week",
            "Тиждень із чотирма сесіями",
            "Complete four strength sessions in a calendar week.",
            "Завершіть чотири силові сесії за календарний тиждень.",
            repeatable=True,
            healthy_notes="Optional denser week; rest still expected via planned rest days.",
        )
    )
    a.append(
        ach(
            "strength.bodyweight_base",
            "strength",
            "milestone",
            tiers_bsp(10, 40, 100, 250),
            rule(
                "sets_logged",
                "count",
                {"kind": "lifetime"},
                filters={"discipline": ["strength"]},
            ),
            "strength",
            "Bodyweight base",
            "База з власною вагою",
            "Accumulate bodyweight strength sets.",
            "Накопичуйте силові підходи з власною вагою.",
        )
    )

    # Fix duplicate PR rules - they all used same filter. Make them distinct by encoding pattern in id only
    # but engine can filter by a future movementPattern; for now use distinct achievement ids with same metric
    # which is OK for catalogue - evaluation will use tags when domain events carry pattern.
    # Re-tag PR achievements with a note in filters via discipline only is fine for v1.

    # ── RUNNING (15) ────────────────────────────────────────────────────────
    a.append(
        ach(
            "running.first_run",
            "running",
            "discovery",
            unlocked(),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                threshold=1,
                filters={"discipline": ["running"]},
            ),
            "running",
            "First run",
            "Перший біг",
            "Complete your first run or walk-run session.",
            "Завершіть першу бігову чи walk-run сесію.",
        )
    )
    a.append(
        ach(
            "running.distance_lifetime_km",
            "running",
            "milestone",
            tiers_bsp(25000, 100000, 500000, 1000000),
            rule("distance_m", "sum", {"kind": "lifetime"}, filters={"discipline": ["running"]}),
            "running",
            "Kilometres collected",
            "Зібрані кілометри",
            "Accumulate lifetime running distance (tiers: 25 / 100 / 500 / 1000 km).",
            "Накопичуйте lifetime-дистанцію бігу (рівні: 25 / 100 / 500 / 1000 км).",
        )
    )
    a.append(
        ach(
            "running.sessions",
            "running",
            "milestone",
            tiers_bsp(5, 25, 75, 200),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                filters={"discipline": ["running"]},
            ),
            "running",
            "Run sessions",
            "Бігові сесії",
            "Complete running sessions over time.",
            "Завершуйте бігові сесії з часом.",
        )
    )
    for dist_m, slug, en, uk in [
        (5000, "5k", "5K finish", "Фініш 5K"),
        (10000, "10k", "10K finish", "Фініш 10K"),
        (21097, "half", "Half marathon", "Напівмарафон"),
        (42195, "marathon", "Marathon distance", "Марафонська дистанція"),
    ]:
        a.append(
            ach(
                f"running.{slug}",
                "running",
                "single_session",
                unlocked(),
                rule(
                    "distance_m",
                    "max",
                    {"kind": "single_event"},
                    threshold=dist_m,
                    filters={"discipline": ["running"], "minDistanceM": dist_m},
                ),
                "running",
                en,
                uk,
                f"Cover at least {en.split()[0]} in a single session.",
                f"Покрийте щонайменше дистанцію «{uk}» за одну сесію.",
            )
        )
    a.append(
        ach(
            "running.easy_streak",
            "running",
            "streak",
            tiers_bsp(3, 8, 16, 32),
            rule(
                "workouts_completed",
                "streak",
                {
                    "kind": "streak_days",
                    "qualify": "activity_or_planned_rest",
                    "restDayTolerancePerWeek": 2,
                    "freezeTokensAllowed": True,
                    "maxFreezePerStreak": 2,
                },
                filters={"discipline": ["running"]},
            ),
            "streak",
            "Easy-run rhythm",
            "Ритм легкого бігу",
            "Keep an easy-run practice with rest days built in.",
            "Тримайте практику легкого бігу із вбудованими днями відпочинку.",
            healthy_notes="Does not reward consecutive hard days.",
        )
    )
    a.append(
        ach(
            "running.week_distance",
            "running",
            "challenge",
            tiers_bsp(10000, 20000, 30000, 40000),
            rule(
                "distance_m",
                "sum",
                {"kind": "calendar", "unit": "week", "count": 1},
                filters={"discipline": ["running"]},
            ),
            "challenge",
            "Weekly kilometres",
            "Кілометри тижня",
            "Accumulate distance in a calendar week (tiers: 10 / 20 / 30 / 40 km).",
            "Накопичуйте дистанцію за календарний тиждень (рівні: 10 / 20 / 30 / 40 км).",
            repeatable=True,
            healthy_notes="Modest weekly caps; not an escalation race.",
        )
    )
    a.append(
        ach(
            "running.walk_run_start",
            "running",
            "discovery",
            unlocked(3),
            rule(
                "workouts_completed",
                "count",
                {"kind": "rolling_days", "days": 14},
                threshold=3,
                filters={"discipline": ["running"]},
            ),
            "running",
            "Walk-run start",
            "Старт walk-run",
            "Complete three easy run or walk-run sessions in two weeks.",
            "Завершіть три легкі бігові чи walk-run сесії за два тижні.",
            healthy_notes="Beginner-friendly entry; no pace requirement.",
        )
    )
    a.append(
        ach(
            "running.trail_or_road",
            "running",
            "milestone",
            tiers_bsp(10, 40, 100, 250),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                filters={"discipline": ["running"]},
            ),
            "running",
            "Routes collected",
            "Зібрані маршрути",
            "Complete many outdoor running sessions.",
            "Завершуйте багато бігових сесій просто неба.",
        )
    )
    a.append(
        ach(
            "running.pace_pr",
            "running",
            "personal_record",
            tiers_bsp(1, 3, 8, 15),
            rule(
                "personal_record_events",
                "count",
                {"kind": "lifetime"},
                filters={"discipline": ["running"]},
            ),
            "personal-record",
            "Pace PRs",
            "Рекорди темпу",
            "Improve best pace on comparable distances.",
            "Покращуйте найкращий темп на порівнянних дистанціях.",
        )
    )
    a.append(
        ach(
            "running.month_challenge",
            "running",
            "challenge",
            unlocked(6),
            rule(
                "workouts_completed",
                "count",
                {"kind": "challenge_period", "period": "current_month"},
                threshold=6,
                filters={"discipline": ["running"]},
            ),
            "challenge",
            "Run month",
            "Біговий місяць",
            "Complete six running sessions this month.",
            "Завершіть шість бігових сесій цього місяця.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "running.secret_sunrise",
            "running",
            "discovery",
            unlocked(),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                threshold=1,
                filters={"discipline": ["running"]},
            ),
            "running",
            "Sunrise shuffle",
            "Ранкова пробіжка",
            "Log an early outdoor run.",
            "Запишіть ранкову пробіжку на вулиці.",
            hidden=True,
        )
    )
    a.append(
        ach(
            "running.long_run",
            "running",
            "single_session",
            tiers_bsp(8000, 12000, 16000, 24000),
            rule(
                "distance_m",
                "max",
                {"kind": "single_event"},
                filters={"discipline": ["running"]},
            ),
            "running",
            "Long run",
            "Довгий біг",
            "Complete progressively longer easy long runs.",
            "Завершуйте поступово довші легкі довгі пробіжки.",
        )
    )

    # ── SWIMMING (12) ───────────────────────────────────────────────────────
    a.append(
        ach(
            "swimming.first_swim",
            "swimming",
            "discovery",
            unlocked(),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                threshold=1,
                filters={"discipline": ["swimming"]},
            ),
            "swimming",
            "First swim",
            "Перше плавання",
            "Complete your first swim session.",
            "Завершіть першу плавальну сесію.",
        )
    )
    a.append(
        ach(
            "swimming.distance_lifetime",
            "swimming",
            "milestone",
            tiers_bsp(5000, 25000, 100000, 250000),
            rule("distance_m", "sum", {"kind": "lifetime"}, filters={"discipline": ["swimming"]}),
            "swimming",
            "Pool kilometres",
            "Басейнові кілометри",
            "Accumulate lifetime swim distance (tiers: 5 / 25 / 100 / 250 km).",
            "Накопичуйте lifetime-дистанцію плавання (рівні: 5 / 25 / 100 / 250 км).",
        )
    )
    a.append(
        ach(
            "swimming.sessions",
            "swimming",
            "milestone",
            tiers_bsp(5, 20, 50, 150),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                filters={"discipline": ["swimming"]},
            ),
            "swimming",
            "Swim sessions",
            "Плавальні сесії",
            "Complete swim sessions over time.",
            "Завершуйте плавальні сесії з часом.",
        )
    )
    a.append(
        ach(
            "swimming.stroke_variety",
            "swimming",
            "discovery",
            tiers_bsp(2, 3, 4, 5),
            rule(
                "swim_strokes_distinct",
                "distinct",
                {"kind": "lifetime"},
                filters={"discipline": ["swimming"]},
            ),
            "swimming",
            "Stroke variety",
            "Різноманіття стилів",
            "Practise multiple swim strokes (platinum includes drill as a fifth).",
            "Практикуйте кілька стилів плавання (платина включає дрил як п'ятий).",
        )
    )
    a.append(
        ach(
            "swimming.session_distance",
            "swimming",
            "single_session",
            tiers_bsp(500, 1000, 2000, 3000),
            rule(
                "distance_m",
                "max",
                {"kind": "single_event"},
                filters={"discipline": ["swimming"]},
            ),
            "swimming",
            "Session metres",
            "Метри сесії",
            "Cover meaningful distance in one swim.",
            "Покрийте значну дистанцію за одне плавання.",
        )
    )
    a.append(
        ach(
            "swimming.rhythm",
            "swimming",
            "streak",
            tiers_bsp(2, 6, 12, 24),
            rule(
                "workouts_completed",
                "streak",
                {
                    "kind": "streak_days",
                    "qualify": "activity_or_planned_rest",
                    "restDayTolerancePerWeek": 2,
                    "freezeTokensAllowed": True,
                    "maxFreezePerStreak": 2,
                },
                filters={"discipline": ["swimming"]},
            ),
            "streak",
            "Swim rhythm",
            "Плавальний ритм",
            "Keep a swim practice with rest tolerance.",
            "Тримайте плавальну практику з толерантністю до відпочинку.",
        )
    )
    a.append(
        ach(
            "swimming.month_challenge",
            "swimming",
            "challenge",
            unlocked(4),
            rule(
                "workouts_completed",
                "count",
                {"kind": "challenge_period", "period": "current_month"},
                threshold=4,
                filters={"discipline": ["swimming"]},
            ),
            "challenge",
            "Swim month",
            "Плавальний місяць",
            "Complete four swim sessions this month.",
            "Завершіть чотири плавальні сесії цього місяця.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "swimming.drill_focus",
            "swimming",
            "milestone",
            tiers_bsp(3, 10, 25, 50),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                filters={"discipline": ["swimming"], "stroke": ["drill"]},
            ),
            "swimming",
            "Drill focus",
            "Фокус на дрилах",
            "Complete technique-focused swim sessions.",
            "Завершуйте плавальні сесії з акцентом на техніку.",
        )
    )
    a.append(
        ach(
            "swimming.secret_flip",
            "swimming",
            "discovery",
            unlocked(),
            rule(
                "swim_strokes_distinct",
                "distinct",
                {"kind": "lifetime"},
                threshold=1,
                filters={"discipline": ["swimming"]},
            ),
            "swimming",
            "Quiet lengths",
            "Тихі доріжки",
            "Log your first consistent set of lengths.",
            "Запишіть перший стабільний набір доріжок.",
            hidden=True,
        )
    )
    a.append(
        ach(
            "swimming.week_distance",
            "swimming",
            "challenge",
            tiers_bsp(2000, 4000, 6000, 8000),
            rule(
                "distance_m",
                "sum",
                {"kind": "calendar", "unit": "week", "count": 1},
                filters={"discipline": ["swimming"]},
            ),
            "challenge",
            "Weekly swim km",
            "Тижневі км у басейні",
            "Accumulate swim distance in a week (tiers: 2 / 4 / 6 / 8 km).",
            "Накопичуйте дистанцію плавання за тиждень (рівні: 2 / 4 / 6 / 8 км).",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "swimming.butterfly_try",
            "swimming",
            "discovery",
            unlocked(),
            rule(
                "swim_strokes_distinct",
                "exists",
                {"kind": "lifetime"},
                threshold=1,
                filters={"discipline": ["swimming"], "stroke": ["butterfly"]},
            ),
            "swimming",
            "Butterfly attempt",
            "Спроба батерфляю",
            "Include butterfly in a swim session.",
            "Увімкніть батерфляй у плавальну сесію.",
        )
    )
    a.append(
        ach(
            "swimming.im_builder",
            "swimming",
            "milestone",
            unlocked(4),
            rule(
                "swim_strokes_distinct",
                "distinct",
                {"kind": "rolling_days", "days": 30},
                threshold=4,
                filters={"discipline": ["swimming"]},
            ),
            "swimming",
            "IM builder",
            "Будівничий КПл",
            "Practise all four competitive strokes within a month.",
            "Практикуйте всі чотири змагальні стилі протягом місяця.",
        )
    )

    # ── YOGA (10) ───────────────────────────────────────────────────────────
    a.append(
        ach(
            "yoga.first_practice",
            "yoga",
            "discovery",
            unlocked(),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                threshold=1,
                filters={"discipline": ["yoga"]},
            ),
            "yoga",
            "First yoga practice",
            "Перша практика йоги",
            "Complete your first yoga session.",
            "Завершіть першу сесію йоги.",
        )
    )
    a.append(
        ach(
            "yoga.minutes",
            "yoga",
            "milestone",
            tiers_bsp(60, 300, 1000, 3000),
            rule("yoga_minutes", "sum", {"kind": "lifetime"}),
            "yoga",
            "Yoga minutes",
            "Хвилини йоги",
            "Accumulate minutes of yoga practice.",
            "Накопичуйте хвилини практики йоги.",
        )
    )
    a.append(
        ach(
            "yoga.sessions",
            "yoga",
            "milestone",
            tiers_bsp(5, 20, 50, 150),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                filters={"discipline": ["yoga"]},
            ),
            "yoga",
            "Yoga sessions",
            "Сесії йоги",
            "Complete yoga sessions over time.",
            "Завершуйте сесії йоги з часом.",
        )
    )
    a.append(
        ach(
            "yoga.streak",
            "yoga",
            "streak",
            tiers_bsp(3, 10, 21, 40),
            rule(
                "workouts_completed",
                "streak",
                {
                    "kind": "streak_days",
                    "qualify": "activity_or_planned_rest",
                    "restDayTolerancePerWeek": 2,
                    "freezeTokensAllowed": True,
                    "maxFreezePerStreak": 3,
                },
                filters={"discipline": ["yoga"]},
            ),
            "streak",
            "Yoga rhythm",
            "Ритм йоги",
            "Keep a gentle yoga rhythm with rest allowed.",
            "Тримайте м'який ритм йоги з дозволеним відпочинком.",
        )
    )
    a.append(
        ach(
            "yoga.long_flow",
            "yoga",
            "single_session",
            tiers_bsp(20, 45, 60, 90),
            rule(
                "session_duration_min",
                "max",
                {"kind": "single_event"},
                filters={"discipline": ["yoga"]},
            ),
            "yoga",
            "Longer flow",
            "Довший потік",
            "Complete progressively longer yoga sessions.",
            "Завершуйте поступово довші сесії йоги.",
        )
    )
    a.append(
        ach(
            "yoga.month_challenge",
            "yoga",
            "challenge",
            unlocked(8),
            rule(
                "workouts_completed",
                "count",
                {"kind": "challenge_period", "period": "current_month"},
                threshold=8,
                filters={"discipline": ["yoga"]},
            ),
            "challenge",
            "Yoga month",
            "Місяць йоги",
            "Complete eight yoga sessions this month.",
            "Завершіть вісім сесій йоги цього місяця.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "yoga.mobility_pair",
            "yoga",
            "discovery",
            unlocked(2),
            rule(
                "disciplines_tried",
                "distinct",
                {"kind": "rolling_days", "days": 7},
                threshold=2,
                filters={"discipline": ["yoga", "strength"]},
            ),
            "yoga",
            "Strength and settle",
            "Сила й заспокоєння",
            "Pair yoga with another discipline in the same week.",
            "Поєднайте йогу з іншою дисципліною протягом тижня.",
        )
    )
    a.append(
        ach(
            "yoga.secret_stillness",
            "yoga",
            "discovery",
            unlocked(),
            rule(
                "session_duration_min",
                "max",
                {"kind": "single_event"},
                threshold=10,
                filters={"discipline": ["yoga"]},
            ),
            "yoga",
            "Stillness",
            "Спокій",
            "Finish a short restorative practice.",
            "Завершіть коротку відновлювальну практику.",
            hidden=True,
        )
    )
    a.append(
        ach(
            "yoga.week_minutes",
            "yoga",
            "challenge",
            tiers_bsp(30, 60, 90, 120),
            rule("yoga_minutes", "sum", {"kind": "calendar", "unit": "week", "count": 1}),
            "challenge",
            "Weekly yoga minutes",
            "Тижневі хвилини йоги",
            "Accumulate yoga minutes in a calendar week.",
            "Накопичуйте хвилини йоги за календарний тиждень.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "yoga.year_practice",
            "yoga",
            "milestone",
            tiers_levels(20, 50, 80, 100, 150),
            rule(
                "workouts_completed",
                "count",
                {"kind": "calendar", "unit": "month", "count": 12},
                filters={"discipline": ["yoga"]},
            ),
            "yoga",
            "Year on the mat",
            "Рік на килимку",
            "Keep yoga sessions across a year.",
            "Тримайте сесії йоги протягом року.",
        )
    )

    # ── PILATES (8) ─────────────────────────────────────────────────────────
    a.append(
        ach(
            "pilates.first_session",
            "pilates",
            "discovery",
            unlocked(),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                threshold=1,
                filters={"discipline": ["pilates"]},
            ),
            "pilates",
            "First pilates session",
            "Перша сесія пілатесу",
            "Complete your first pilates session.",
            "Завершіть першу сесію пілатесу.",
        )
    )
    a.append(
        ach(
            "pilates.minutes",
            "pilates",
            "milestone",
            tiers_bsp(60, 300, 900, 2400),
            rule("pilates_minutes", "sum", {"kind": "lifetime"}),
            "pilates",
            "Pilates minutes",
            "Хвилини пілатесу",
            "Accumulate minutes of pilates practice.",
            "Накопичуйте хвилини практики пілатесу.",
        )
    )
    a.append(
        ach(
            "pilates.sessions",
            "pilates",
            "milestone",
            tiers_bsp(5, 20, 50, 120),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                filters={"discipline": ["pilates"]},
            ),
            "pilates",
            "Pilates sessions",
            "Сесії пілатесу",
            "Complete pilates sessions over time.",
            "Завершуйте сесії пілатесу з часом.",
        )
    )
    a.append(
        ach(
            "pilates.streak",
            "pilates",
            "streak",
            tiers_bsp(3, 8, 16, 30),
            rule(
                "workouts_completed",
                "streak",
                {
                    "kind": "streak_days",
                    "qualify": "activity_or_planned_rest",
                    "restDayTolerancePerWeek": 2,
                    "freezeTokensAllowed": True,
                    "maxFreezePerStreak": 2,
                },
                filters={"discipline": ["pilates"]},
            ),
            "streak",
            "Pilates rhythm",
            "Ритм пілатесу",
            "Keep a pilates practice with rest tolerance.",
            "Тримайте практику пілатесу з толерантністю до відпочинку.",
        )
    )
    a.append(
        ach(
            "pilates.core_focus",
            "pilates",
            "single_session",
            tiers_bsp(20, 35, 45, 60),
            rule(
                "session_duration_min",
                "max",
                {"kind": "single_event"},
                filters={"discipline": ["pilates"]},
            ),
            "muscle-core",
            "Core focus session",
            "Сесія з фокусом на кор",
            "Complete a focused pilates session.",
            "Завершіть зосереджену сесію пілатесу.",
        )
    )
    a.append(
        ach(
            "pilates.month_challenge",
            "pilates",
            "challenge",
            unlocked(6),
            rule(
                "workouts_completed",
                "count",
                {"kind": "challenge_period", "period": "current_month"},
                threshold=6,
                filters={"discipline": ["pilates"]},
            ),
            "challenge",
            "Pilates month",
            "Місяць пілатесу",
            "Complete six pilates sessions this month.",
            "Завершіть шість сесій пілатесу цього місяця.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "pilates.secret_hundred",
            "pilates",
            "discovery",
            unlocked(),
            rule(
                "workouts_completed",
                "count",
                {"kind": "lifetime"},
                threshold=1,
                filters={"discipline": ["pilates"]},
            ),
            "pilates",
            "The hundred",
            "Сотня",
            "Include the classic hundred in a session.",
            "Увімкніть класичну «сотню» в сесію.",
            hidden=True,
        )
    )
    a.append(
        ach(
            "pilates.week_minutes",
            "pilates",
            "challenge",
            tiers_bsp(30, 60, 90, 120),
            rule("pilates_minutes", "sum", {"kind": "calendar", "unit": "week", "count": 1}),
            "challenge",
            "Weekly pilates minutes",
            "Тижневі хвилини пілатесу",
            "Accumulate pilates minutes in a calendar week.",
            "Накопичуйте хвилини пілатесу за календарний тиждень.",
            repeatable=True,
        )
    )

    # ── NUTRITION (12) — logging only, never calorie restriction / weight loss speed
    a.append(
        ach(
            "nutrition.first_log",
            "nutrition",
            "discovery",
            unlocked(),
            rule("food_days_logged", "count", {"kind": "lifetime"}, threshold=1),
            "food-log",
            "First food log",
            "Перший запис їжі",
            "Log food for the first day.",
            "Запишіть їжу вперше за день.",
            healthy_notes="No calorie thresholds; logging presence only.",
        )
    )
    a.append(
        ach(
            "nutrition.days_logged",
            "nutrition",
            "milestone",
            tiers_bsp(7, 30, 100, 365),
            rule("food_days_logged", "count", {"kind": "lifetime"}),
            "food",
            "Days logged",
            "Дні в журналі",
            "Log food on 7, 30, 100, then 365 days.",
            "Записуйте їжу 7, 30, 100, а потім 365 днів.",
            healthy_notes="Consistency of logging, not intake targets.",
        )
    )
    a.append(
        ach(
            "nutrition.streak",
            "nutrition",
            "streak",
            tiers_bsp(3, 7, 21, 60),
            rule(
                "food_days_logged",
                "streak",
                {
                    "kind": "streak_days",
                    "qualify": "log",
                    "restDayTolerancePerWeek": 1,
                    "freezeTokensAllowed": True,
                    "maxFreezePerStreak": 3,
                },
            ),
            "streak",
            "Food log streak",
            "Серія записів їжі",
            "Keep a food-logging streak with freeze support.",
            "Тримайте серію записів їжі з підтримкою пауз.",
        )
    )
    a.append(
        ach(
            "nutrition.week_complete",
            "nutrition",
            "challenge",
            unlocked(7),
            rule(
                "food_days_logged",
                "count",
                {"kind": "calendar", "unit": "week", "count": 1},
                threshold=7,
            ),
            "challenge",
            "Full log week",
            "Повний тиждень записів",
            "Log food every day for a calendar week.",
            "Записуйте їжу щодня протягом календарного тижня.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "nutrition.month_habit",
            "nutrition",
            "challenge",
            tiers_bsp(10, 20, 25, 30),
            rule(
                "food_days_logged",
                "count",
                {"kind": "challenge_period", "period": "current_month"},
            ),
            "challenge",
            "Month of logging",
            "Місяць записів",
            "Log food on many days this month.",
            "Записуйте їжу багато днів цього місяця.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "nutrition.balanced_plate_days",
            "nutrition",
            "milestone",
            tiers_bsp(5, 20, 50, 100),
            rule("food_days_logged", "count", {"kind": "lifetime"}),
            "food",
            "Plate awareness",
            "Усвідомлення тарілки",
            "Build a habit of noticing what you eat by logging days.",
            "Формуйте звичку помічати, що їсте, через дні записів.",
            healthy_notes="Awareness via logging; never rewards restriction.",
        )
    )
    a.append(
        ach(
            "nutrition.secret_breakfast",
            "nutrition",
            "discovery",
            unlocked(),
            rule("food_days_logged", "count", {"kind": "lifetime"}, threshold=1),
            "food-log",
            "Breakfast noted",
            "Сніданок записано",
            "Log a morning meal.",
            "Запишіть ранковий прийом їжі.",
            hidden=True,
        )
    )
    a.append(
        ach(
            "nutrition.rolling_30",
            "nutrition",
            "milestone",
            unlocked(20),
            rule("food_days_logged", "count", {"kind": "rolling_days", "days": 30}, threshold=20),
            "food",
            "Mostly logged month",
            "Майже повний місяць записів",
            "Log food on at least 20 of the last 30 days.",
            "Запишіть їжу щонайменше 20 із останніх 30 днів.",
        )
    )
    a.append(
        ach(
            "nutrition.variety_week",
            "nutrition",
            "challenge",
            unlocked(5),
            rule(
                "food_days_logged",
                "count",
                {"kind": "calendar", "unit": "week", "count": 1},
                threshold=5,
            ),
            "challenge",
            "Five-day awareness",
            "П'ятиденна уважність",
            "Log food on five days in a week.",
            "Записуйте їжу п'ять днів на тиждень.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "nutrition.hundred_club",
            "nutrition",
            "milestone",
            unlocked(100),
            rule("food_days_logged", "count", {"kind": "lifetime"}, threshold=100),
            "trophy",
            "Hundred club",
            "Клуб сотні",
            "Log food on 100 different days.",
            "Запишіть їжу в 100 різних днів.",
        )
    )
    a.append(
        ach(
            "nutrition.gentle_return",
            "nutrition",
            "discovery",
            unlocked(3),
            rule("food_days_logged", "count", {"kind": "rolling_days", "days": 7}, threshold=3),
            "food-log",
            "Gentle return",
            "М'яке повернення",
            "Resume logging on three days after a gap.",
            "Відновіть записи в три дні після перерви.",
            healthy_notes="No punishment for gaps; rewards return.",
        )
    )
    a.append(
        ach(
            "nutrition.year_of_logs",
            "nutrition",
            "milestone",
            tiers_levels(50, 100, 200, 300, 365),
            rule("food_days_logged", "count", {"kind": "calendar", "unit": "month", "count": 12}),
            "food",
            "Year of logs",
            "Рік записів",
            "Accumulate food-log days across a year.",
            "Накопичуйте дні записів їжі протягом року.",
        )
    )

    # ── WATER (8) — hydration goal met, never dehydration ────────────────────
    a.append(
        ach(
            "water.first_goal",
            "water",
            "discovery",
            unlocked(),
            rule("water_goal_days", "count", {"kind": "lifetime"}, threshold=1),
            "water",
            "First hydration day",
            "Перший день гідратації",
            "Meet your personal water goal for a day.",
            "Досягніть особистої цілі з води за день.",
            healthy_notes="Personal goal met; never rewards under-drinking.",
        )
    )
    a.append(
        ach(
            "water.streak",
            "water",
            "streak",
            tiers_bsp(3, 7, 21, 60),
            rule(
                "water_goal_days",
                "streak",
                {
                    "kind": "streak_days",
                    "qualify": "goal_met",
                    "restDayTolerancePerWeek": 1,
                    "freezeTokensAllowed": True,
                    "maxFreezePerStreak": 3,
                },
            ),
            "streak",
            "Hydration streak",
            "Серія гідратації",
            "Meet your water goal on consecutive days.",
            "Досягайте цілі з води кілька днів поспіль.",
        )
    )
    a.append(
        ach(
            "water.days",
            "water",
            "milestone",
            tiers_bsp(7, 30, 100, 300),
            rule("water_goal_days", "count", {"kind": "lifetime"}),
            "water",
            "Hydration days",
            "Дні гідратації",
            "Accumulate days meeting your water goal.",
            "Накопичуйте дні досягнення цілі з води.",
        )
    )
    a.append(
        ach(
            "water.week_challenge",
            "water",
            "challenge",
            unlocked(7),
            rule(
                "water_goal_days",
                "count",
                {"kind": "calendar", "unit": "week", "count": 1},
                threshold=7,
            ),
            "challenge",
            "Hydration week",
            "Тиждень гідратації",
            "Meet your water goal every day for a week.",
            "Досягайте цілі з води щодня протягом тижня.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "water.month_challenge",
            "water",
            "challenge",
            tiers_bsp(10, 20, 25, 30),
            rule(
                "water_goal_days",
                "count",
                {"kind": "challenge_period", "period": "current_month"},
            ),
            "challenge",
            "Hydration month",
            "Місяць гідратації",
            "Meet your water goal on many days this month.",
            "Досягайте цілі з води багато днів цього місяця.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "water.secret_sip",
            "water",
            "discovery",
            unlocked(),
            rule("water_goal_days", "count", {"kind": "lifetime"}, threshold=1),
            "water",
            "First sip logged",
            "Перший ковток записано",
            "Log water for the first time.",
            "Запишіть воду вперше.",
            hidden=True,
        )
    )
    a.append(
        ach(
            "water.rolling_consistent",
            "water",
            "milestone",
            unlocked(20),
            rule("water_goal_days", "count", {"kind": "rolling_days", "days": 30}, threshold=20),
            "water",
            "Mostly hydrated month",
            "Майже гідрований місяць",
            "Meet your water goal on 20 of the last 30 days.",
            "Досягніть цілі з води 20 із останніх 30 днів.",
        )
    )
    a.append(
        ach(
            "water.train_and_drink",
            "water",
            "challenge",
            unlocked(3),
            rule(
                "water_goal_days",
                "count",
                {"kind": "rolling_days", "days": 7},
                threshold=3,
            ),
            "water",
            "Train and drink",
            "Тренуйся й пий",
            "Meet your water goal on three training days in a week.",
            "Досягніть цілі з води в три тренувальні дні тижня.",
            repeatable=True,
        )
    )

    # ── RECOVERY / SLEEP (10) ───────────────────────────────────────────────
    a.append(
        ach(
            "recovery.first_sleep_import",
            "recovery",
            "discovery",
            unlocked(),
            rule("sleep_consistent_days", "count", {"kind": "lifetime"}, threshold=1),
            "sleep",
            "Sleep connected",
            "Сон підключено",
            "Import or log your first sleep night.",
            "Імпортуйте або запишіть першу ніч сну.",
        )
    )
    a.append(
        ach(
            "recovery.sleep_consistency",
            "recovery",
            "streak",
            tiers_bsp(3, 7, 21, 45),
            rule(
                "sleep_consistent_days",
                "streak",
                {
                    "kind": "streak_days",
                    "qualify": "goal_met",
                    "restDayTolerancePerWeek": 1,
                    "freezeTokensAllowed": True,
                    "maxFreezePerStreak": 2,
                },
                filters={"sleepMidpointVarianceMin": 60},
            ),
            "streak",
            "Sleep consistency",
            "Стабільність сну",
            "Keep a consistent sleep midpoint within a gentle window.",
            "Тримайте стабільну середину сну в м'якому вікні.",
            healthy_notes="Consistency of timing, not punishing short sleep.",
        )
    )
    a.append(
        ach(
            "recovery.sleep_days",
            "recovery",
            "milestone",
            tiers_bsp(7, 30, 90, 180),
            rule("sleep_consistent_days", "count", {"kind": "lifetime"}),
            "sleep",
            "Consistent nights",
            "Стабільні ночі",
            "Accumulate nights within your consistency window.",
            "Накопичуйте ночі в межах вікна стабільності.",
        )
    )
    a.append(
        ach(
            "recovery.rest_day_honour",
            "recovery",
            "milestone",
            tiers_bsp(4, 12, 26, 52),
            rule("planned_rest_honoured", "count", {"kind": "lifetime"}),
            "recovery",
            "Recovery days kept",
            "Дні відновлення дотримано",
            "Honour planned rest when readiness is low.",
            "Дотримуйтесь запланованого відпочинку за низької готовності.",
        )
    )
    a.append(
        ach(
            "recovery.month_sleep",
            "recovery",
            "challenge",
            tiers_bsp(10, 18, 24, 28),
            rule(
                "sleep_consistent_days",
                "count",
                {"kind": "challenge_period", "period": "current_month"},
            ),
            "challenge",
            "Sleep month",
            "Місяць сну",
            "Keep consistent sleep on many nights this month.",
            "Тримайте стабільний сон багато ночей цього місяця.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "recovery.readiness_listen",
            "recovery",
            "discovery",
            unlocked(3),
            rule("planned_rest_honoured", "count", {"kind": "rolling_days", "days": 14}, threshold=3),
            "readiness",
            "Listening to readiness",
            "Слухати готовність",
            "Take three readiness-informed rest days in two weeks.",
            "Візьміть три дні відпочинку за готовністю за два тижні.",
            healthy_notes="Counters overtraining pressure from streaks.",
        )
    )
    a.append(
        ach(
            "recovery.secret_nap_boundary",
            "recovery",
            "discovery",
            unlocked(),
            rule("planned_rest_honoured", "count", {"kind": "lifetime"}, threshold=1),
            "recovery",
            "Boundary kept",
            "Межу збережено",
            "Skip a planned hard session when recovery says no.",
            "Пропустіть заплановану важку сесію, коли відновлення каже «ні».",
            hidden=True,
        )
    )
    a.append(
        ach(
            "recovery.week_consistency",
            "recovery",
            "challenge",
            unlocked(5),
            rule(
                "sleep_consistent_days",
                "count",
                {"kind": "calendar", "unit": "week", "count": 1},
                threshold=5,
            ),
            "challenge",
            "Steady sleep week",
            "Стабільний тиждень сну",
            "Hit sleep consistency on five nights in a week.",
            "Досягніть стабільності сну п'ять ночей на тиждень.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "recovery.hrv_checkin",
            "recovery",
            "milestone",
            tiers_bsp(7, 30, 90, 180),
            rule("sleep_consistent_days", "count", {"kind": "lifetime"}),
            "hrv",
            "Recovery check-ins",
            "Перевірки відновлення",
            "Keep showing up for sleep and recovery logging.",
            "Продовжуйте вести записи сну та відновлення.",
        )
    )
    a.append(
        ach(
            "recovery.balance_block",
            "recovery",
            "challenge",
            unlocked(2),
            rule(
                "planned_rest_honoured",
                "count",
                {"kind": "challenge_period", "period": "current_week"},
                threshold=2,
            ),
            "challenge",
            "Balanced week",
            "Збалансований тиждень",
            "Honour at least two planned rest days this week.",
            "Дотримуйтесь щонайменше двох запланованих днів відпочинку цього тижня.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "recovery.stress_down_day",
            "recovery",
            "discovery",
            unlocked(),
            rule("planned_rest_honoured", "count", {"kind": "lifetime"}, threshold=1),
            "stress",
            "Pressure off",
            "Тиск знято",
            "Take a rest day when stress or readiness calls for it.",
            "Візьміть день відпочинку, коли стрес чи готовність цього потребують.",
            healthy_notes="Pairs with readiness; never rewards training through red flags.",
        )
    )

    # ── BODY (8) — measurement consistency only, never shape/weight-loss speed
    a.append(
        ach(
            "body.first_measurement",
            "body",
            "discovery",
            unlocked(),
            rule(
                "measurements_logged",
                "count",
                {"kind": "lifetime"},
                threshold=1,
                filters={"excludeWeightLossRate": True},
            ),
            "measurements",
            "First measurement",
            "Перший замір",
            "Log your first body measurement.",
            "Запишіть перший замір тіла.",
            healthy_notes="Presence of measuring; no target body shape.",
        )
    )
    a.append(
        ach(
            "body.measurement_days",
            "body",
            "milestone",
            tiers_bsp(4, 12, 26, 52),
            rule(
                "measurement_days",
                "count",
                {"kind": "lifetime"},
                filters={"excludeWeightLossRate": True},
            ),
            "measurements",
            "Measurement consistency",
            "Стабільність замірів",
            "Log measurements on many separate days.",
            "Записуйте заміри в багато окремих днів.",
            healthy_notes="Consistency of measuring, not reaching a body shape.",
        )
    )
    a.append(
        ach(
            "body.streak",
            "body",
            "streak",
            tiers_bsp(2, 4, 8, 12),
            rule(
                "measurement_days",
                "streak",
                {
                    "kind": "streak_days",
                    "qualify": "log",
                    "restDayTolerancePerWeek": 3,
                    "freezeTokensAllowed": True,
                    "maxFreezePerStreak": 2,
                },
                filters={"excludeWeightLossRate": True},
            ),
            "streak",
            "Check-in rhythm",
            "Ритм перевірок",
            "Keep a gentle measurement check-in rhythm (weekly-scale).",
            "Тримайте м'який ритм замірів (у масштабі тижня).",
            healthy_notes="Sparse streak — measuring daily is not required or rewarded.",
        )
    )
    a.append(
        ach(
            "body.month_checkins",
            "body",
            "challenge",
            unlocked(2),
            rule(
                "measurement_days",
                "count",
                {"kind": "challenge_period", "period": "current_month"},
                threshold=2,
                filters={"excludeWeightLossRate": True},
            ),
            "challenge",
            "Month check-ins",
            "Місячні перевірки",
            "Log measurements on two days this month.",
            "Запишіть заміри в два дні цього місяця.",
            repeatable=True,
        )
    )
    a.append(
        ach(
            "body.kinds_variety",
            "body",
            "discovery",
            list(tiers_bsp(1, 2, 3, 4)),
            rule(
                "measurements_logged",
                "distinct",
                {"kind": "lifetime"},
                filters={"excludeWeightLossRate": True},
            ),
            "measurements",
            "Measurement variety",
            "Різноманіття замірів",
            "Log different kinds of measurements over time.",
            "Записуйте різні види замірів з часом.",
        )
    )
    a.append(
        ach(
            "body.secret_tape",
            "body",
            "discovery",
            unlocked(),
            rule(
                "measurements_logged",
                "count",
                {"kind": "lifetime"},
                threshold=1,
                filters={"measurementKind": ["girth"], "excludeWeightLossRate": True},
            ),
            "measurements",
            "Tape out",
            "Сантиметр у справі",
            "Log a girth measurement.",
            "Запишіть обхват.",
            hidden=True,
        )
    )
    a.append(
        ach(
            "body.quarter_habit",
            "body",
            "milestone",
            unlocked(6),
            rule(
                "measurement_days",
                "count",
                {"kind": "rolling_days", "days": 90},
                threshold=6,
                filters={"excludeWeightLossRate": True},
            ),
            "measurements",
            "Quarterly habit",
            "Квартальна звичка",
            "Log measurements on six days across a quarter.",
            "Запишіть заміри в шість днів протягом кварталу.",
        )
    )
    a.append(
        ach(
            "body.year_of_checkins",
            "body",
            "milestone",
            tiers_levels(6, 12, 18, 24, 36),
            rule(
                "measurement_days",
                "count",
                {"kind": "calendar", "unit": "month", "count": 12},
                filters={"excludeWeightLossRate": True},
            ),
            "measurements",
            "Year of check-ins",
            "Рік перевірок",
            "Keep measurement check-ins across a year.",
            "Тримайте перевірки замірів протягом року.",
        )
    )

    return a


def strip_internal(item: dict) -> dict:
    out = {k: v for k, v in item.items() if not k.startswith("_")}
    return out


def main() -> None:
    DATA.mkdir(parents=True, exist_ok=True)
    LOCALES.mkdir(parents=True, exist_ok=True)
    items = build()
    by_cat: dict[str, list] = {}
    en: dict[str, str] = {}
    uk: dict[str, str] = {}
    for item in items:
        by_cat.setdefault(item["category"], []).append(strip_internal(item))
        en.update(item["_strings"]["en"])
        uk.update(item["_strings"]["uk"])

    for cat, rows in sorted(by_cat.items()):
        path = DATA / f"{cat}.json"
        path.write_text(json.dumps(rows, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"wrote {path.name}: {len(rows)}")

    index = {
        "version": 1,
        "count": len(items),
        "categories": {k: len(v) for k, v in sorted(by_cat.items())},
        "files": [f"data/{k}.json" for k in sorted(by_cat)],
    }
    (ROOT / "catalogue.json").write_text(
        json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    (LOCALES / "en-GB.json").write_text(
        json.dumps(dict(sorted(en.items())), ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (LOCALES / "uk.json").write_text(
        json.dumps(dict(sorted(uk.items())), ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(f"total achievements: {len(items)}")
    print("categories:", index["categories"])


if __name__ == "__main__":
    main()
