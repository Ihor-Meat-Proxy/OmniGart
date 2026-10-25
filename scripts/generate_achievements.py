#!/usr/bin/env python3
"""Generate OmniGart achievement catalog + en-GB/uk locale strings."""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "content" / "achievements"


def metal(m: str) -> dict:
    return {"kind": "metal", "metal": m}


def level(n: int) -> dict:
    return {"kind": "level", "level": n}


def counter(metric: str, value: float, *, filters: dict | None = None, window: dict | None = None) -> dict:
    rule: dict = {
        "kind": "counter",
        "metric": metric,
        "comparator": "gte",
        "value": value,
    }
    if filters:
        rule["filters"] = filters
    if window:
        rule["window"] = window
    return rule


def streak(
    metric: str,
    value: int,
    *,
    filters: dict | None = None,
    allow_planned_rest: bool = True,
    allow_streak_freeze: bool = True,
    max_freeze_per_30d: int = 2,
) -> dict:
    rule: dict = {
        "kind": "streak",
        "metric": metric,
        "comparator": "gte",
        "value": value,
        "streakPolicy": {
            "allowPlannedRest": allow_planned_rest,
            "allowStreakFreeze": allow_streak_freeze,
            "maxFreezeDaysPer30Days": max_freeze_per_30d,
        },
    }
    if filters:
        rule["filters"] = filters
    return rule


def threshold(metric: str, value: float, *, filters: dict | None = None) -> dict:
    rule: dict = {
        "kind": "threshold",
        "metric": metric,
        "comparator": "gte",
        "value": value,
    }
    if filters:
        rule["filters"] = filters
    return rule


def personal_record(metric: str, *, filters: dict | None = None) -> dict:
    rule: dict = {
        "kind": "personal_record",
        "metric": metric,
        "comparator": "gt",
        "value": 0,
    }
    if filters:
        rule["filters"] = filters
    return rule


def challenge_rule(challenge_type: str, value: int = 1) -> dict:
    return {
        "kind": "challenge",
        "metric": "challenge_completed",
        "comparator": "gte",
        "value": value,
        "filters": {"challengeType": challenge_type},
    }


def ach(
    id_: str,
    category: str,
    tier: dict,
    icon: str,
    rule: dict,
    *,
    family: str | None = None,
    hidden: bool = False,
    repeatable: bool = False,
    sort: int = 0,
) -> dict:
    slug = id_.replace(".", "_").replace("-", "_")
    item = {
        "id": id_,
        "category": category,
        "tier": tier,
        "familyId": family or id_.rsplit(".", 1)[0] if id_.count(".") >= 2 else id_,
        "hidden": hidden,
        "repeatable": repeatable,
        "iconId": icon,
        "titleKey": f"achievement.{slug}.title",
        "descriptionKey": f"achievement.{slug}.description",
        "rule": rule,
        "sortOrder": sort,
    }
    return item


# (id, category, tier, icon, rule, title_en, desc_en, title_uk, desc_uk, kwargs)
ENTRIES: list[tuple] = []


def add(
    id_: str,
    category: str,
    tier: dict,
    icon: str,
    rule: dict,
    title_en: str,
    desc_en: str,
    title_uk: str,
    desc_uk: str,
    **kwargs,
) -> None:
    ENTRIES.append((id_, category, tier, icon, rule, title_en, desc_en, title_uk, desc_uk, kwargs))


# —— Strength (9) ——
add(
    "strength.first_session",
    "strength",
    metal("bronze"),
    "strength",
    counter("workout_completed", 1, filters={"discipline": "strength"}),
    "First lift logged",
    "Complete your first strength session.",
    "Перше силове тренування",
    "Завершіть перше силове тренування.",
    family="strength.sessions",
    sort=10,
)
add(
    "strength.sessions.5",
    "strength",
    metal("bronze"),
    "strength",
    counter("workout_completed", 5, filters={"discipline": "strength"}),
    "Five under the bar",
    "Complete 5 strength sessions.",
    "П’ять силових",
    "Завершіть 5 силових тренувань.",
    family="strength.sessions",
    sort=20,
)
add(
    "strength.sessions.15",
    "strength",
    metal("silver"),
    "strength",
    counter("workout_completed", 15, filters={"discipline": "strength"}),
    "Building the habit",
    "Complete 15 strength sessions.",
    "Звичка зростає",
    "Завершіть 15 силових тренувань.",
    family="strength.sessions",
    sort=30,
)
add(
    "strength.sessions.40",
    "strength",
    metal("gold"),
    "strength",
    counter("workout_completed", 40, filters={"discipline": "strength"}),
    "Steady strength",
    "Complete 40 strength sessions — consistency over heroics.",
    "Стабільна сила",
    "Завершіть 40 силових тренувань — стабільність важливіша за героїзм.",
    family="strength.sessions",
    sort=40,
)
add(
    "strength.sessions.100",
    "strength",
    metal("platinum"),
    "trophy",
    counter("workout_completed", 100, filters={"discipline": "strength"}),
    "Century of sessions",
    "Complete 100 strength sessions.",
    "Сотня тренувань",
    "Завершіть 100 силових тренувань.",
    family="strength.sessions",
    sort=50,
)
add(
    "strength.first_pr",
    "strength",
    metal("silver"),
    "trophy",
    personal_record("load_kg", filters={"discipline": "strength"}),
    "Personal best",
    "Log your first strength personal record.",
    "Особистий рекорд",
    "Зафіксуйте перший силовий особистий рекорд.",
    family="strength.pr",
    sort=60,
)
add(
    "strength.prs.5",
    "strength",
    metal("gold"),
    "trophy",
    counter("personal_record", 5, filters={"discipline": "strength"}),
    "Five personal bests",
    "Log 5 strength personal records over time.",
    "П’ять рекордів",
    "Зафіксуйте 5 силових особистих рекордів.",
    family="strength.pr",
    sort=70,
)
add(
    "strength.week_plan_adherence",
    "strength",
    metal("silver"),
    "planner",
    counter("planned_session_completed", 3, filters={"discipline": "strength"}, window={"unit": "week", "count": 1}),
    "Week as planned",
    "Complete at least 3 planned strength sessions in a calendar week.",
    "Тиждень за планом",
    "Завершіть щонайменше 3 заплановані силові сесії за календарний тиждень.",
    family="strength.adherence",
    sort=80,
)
add(
    "strength.streak.28",
    "strength",
    metal("gold"),
    "streak",
    streak("training_or_rest_day", 28, filters={"discipline": "strength"}),
    "Four-week rhythm",
    "Keep a 28-day strength plan streak — planned rest days count.",
    "Чотиритижневий ритм",
    "Тримайте 28-денну серію силового плану — запланований відпочинок рахується.",
    family="strength.streak",
    sort=90,
)

# —— Running (8) ——
add(
    "running.first_session",
    "running",
    metal("bronze"),
    "running",
    counter("workout_completed", 1, filters={"discipline": "running"}),
    "First kilometres",
    "Log your first run.",
    "Перші кілометри",
    "Запишіть перший біг.",
    family="running.sessions",
    sort=10,
)
add(
    "running.sessions.5",
    "running",
    metal("bronze"),
    "running",
    counter("workout_completed", 5, filters={"discipline": "running"}),
    "Finding your stride",
    "Complete 5 runs.",
    "Знайти крок",
    "Завершіть 5 пробіжок.",
    family="running.sessions",
    sort=20,
)
add(
    "running.sessions.20",
    "running",
    metal("silver"),
    "running",
    counter("workout_completed", 20, filters={"discipline": "running"}),
    "Regular runner",
    "Complete 20 runs.",
    "Регулярний бігун",
    "Завершіть 20 пробіжок.",
    family="running.sessions",
    sort=30,
)
add(
    "running.sessions.50",
    "running",
    metal("gold"),
    "running",
    counter("workout_completed", 50, filters={"discipline": "running"}),
    "Fifty runs",
    "Complete 50 runs.",
    "П’ятдесят пробіжок",
    "Завершіть 50 пробіжок.",
    family="running.sessions",
    sort=40,
)
add(
    "running.single.5k",
    "running",
    metal("bronze"),
    "running",
    threshold("distance_m", 5000, filters={"discipline": "running"}),
    "Five-K outing",
    "Complete a single run of at least 5 km.",
    "П’ять кілометрів",
    "Завершіть одну пробіжку щонайменше на 5 км.",
    family="running.distance_single",
    sort=50,
)
add(
    "running.single.10k",
    "running",
    metal("silver"),
    "running",
    threshold("distance_m", 10000, filters={"discipline": "running"}),
    "Ten-K outing",
    "Complete a single run of at least 10 km.",
    "Десять кілометрів",
    "Завершіть одну пробіжку щонайменше на 10 км.",
    family="running.distance_single",
    sort=60,
)
add(
    "running.month.50km",
    "running",
    metal("gold"),
    "running",
    counter("distance_m", 50000, filters={"discipline": "running"}, window={"unit": "month", "count": 1}),
    "50 km month",
    "Accumulate 50 km of running in a calendar month.",
    "50 км за місяць",
    "Наберіть 50 км бігу за календарний місяць.",
    family="running.distance_month",
    sort=70,
)
add(
    "running.month.100km",
    "running",
    metal("platinum"),
    "trophy",
    counter("distance_m", 100000, filters={"discipline": "running"}, window={"unit": "month", "count": 1}),
    "100 km month",
    "Accumulate 100 km of running in a calendar month — pace yourself.",
    "100 км за місяць",
    "Наберіть 100 км бігу за календарний місяць — без поспіху.",
    family="running.distance_month",
    sort=80,
)

# —— Swimming (7) ——
add(
    "swimming.first_session",
    "swimming",
    metal("bronze"),
    "swimming",
    counter("workout_completed", 1, filters={"discipline": "swimming"}),
    "First lengths",
    "Log your first swim.",
    "Перші доріжки",
    "Запишіть перше плавання.",
    family="swimming.sessions",
    sort=10,
)
add(
    "swimming.sessions.5",
    "swimming",
    metal("bronze"),
    "swimming",
    counter("workout_completed", 5, filters={"discipline": "swimming"}),
    "Pool regular",
    "Complete 5 swims.",
    "Постійний у басейні",
    "Завершіть 5 плавальних сесій.",
    family="swimming.sessions",
    sort=20,
)
add(
    "swimming.sessions.20",
    "swimming",
    metal("silver"),
    "swimming",
    counter("workout_completed", 20, filters={"discipline": "swimming"}),
    "Twenty swims",
    "Complete 20 swims.",
    "Двадцять запливів",
    "Завершіть 20 плавальних сесій.",
    family="swimming.sessions",
    sort=30,
)
add(
    "swimming.sessions.40",
    "swimming",
    metal("gold"),
    "swimming",
    counter("workout_completed", 40, filters={"discipline": "swimming"}),
    "Lap legend (quietly)",
    "Complete 40 swims.",
    "Сорок запливів",
    "Завершіть 40 плавальних сесій.",
    family="swimming.sessions",
    sort=40,
)
add(
    "swimming.single.1000m",
    "swimming",
    metal("bronze"),
    "swimming",
    threshold("distance_m", 1000, filters={"discipline": "swimming"}),
    "Kilometre in the water",
    "Swim at least 1 000 m in one session.",
    "Кілометр у воді",
    "Пропливіть щонайменше 1 000 м за одну сесію.",
    family="swimming.distance_single",
    sort=50,
)
add(
    "swimming.single.2000m",
    "swimming",
    metal("silver"),
    "swimming",
    threshold("distance_m", 2000, filters={"discipline": "swimming"}),
    "Two-K swim",
    "Swim at least 2 000 m in one session.",
    "Два кілометри",
    "Пропливіть щонайменше 2 000 м за одну сесію.",
    family="swimming.distance_single",
    sort=60,
)
add(
    "swimming.month.20km",
    "swimming",
    metal("gold"),
    "swimming",
    counter("distance_m", 20000, filters={"discipline": "swimming"}, window={"unit": "month", "count": 1}),
    "20 km swim month",
    "Accumulate 20 km of swimming in a calendar month.",
    "20 км за місяць",
    "Наберіть 20 км плавання за календарний місяць.",
    family="swimming.distance_month",
    sort=70,
)

# —— Yoga (7) ——
add(
    "yoga.first_session",
    "yoga",
    metal("bronze"),
    "yoga",
    counter("workout_completed", 1, filters={"discipline": "yoga"}),
    "First mat session",
    "Complete your first yoga session.",
    "Перша сесія на килимку",
    "Завершіть першу йога-сесію.",
    family="yoga.sessions",
    sort=10,
)
add(
    "yoga.sessions.5",
    "yoga",
    metal("bronze"),
    "yoga",
    counter("workout_completed", 5, filters={"discipline": "yoga"}),
    "Five on the mat",
    "Complete 5 yoga sessions.",
    "П’ять на килимку",
    "Завершіть 5 йога-сесій.",
    family="yoga.sessions",
    sort=20,
)
add(
    "yoga.sessions.15",
    "yoga",
    metal("silver"),
    "yoga",
    counter("workout_completed", 15, filters={"discipline": "yoga"}),
    "Mobility habit",
    "Complete 15 yoga sessions.",
    "Звичка рухливості",
    "Завершіть 15 йога-сесій.",
    family="yoga.sessions",
    sort=30,
)
add(
    "yoga.sessions.40",
    "yoga",
    metal("gold"),
    "yoga",
    counter("workout_completed", 40, filters={"discipline": "yoga"}),
    "Steady practice",
    "Complete 40 yoga sessions.",
    "Стабільна практика",
    "Завершіть 40 йога-сесій.",
    family="yoga.sessions",
    sort=40,
)
add(
    "yoga.minutes.30",
    "yoga",
    metal("bronze"),
    "yoga",
    threshold("duration_min", 30, filters={"discipline": "yoga"}),
    "Half-hour practice",
    "Complete a yoga session of at least 30 minutes.",
    "Півгодини практики",
    "Завершіть йога-сесію щонайменше на 30 хвилин.",
    family="yoga.duration",
    sort=50,
)
add(
    "yoga.streak.14",
    "yoga",
    metal("silver"),
    "streak",
    streak("training_or_rest_day", 14, filters={"discipline": "yoga"}),
    "Two-week yoga rhythm",
    "Keep a 14-day yoga plan streak — rest days count when planned.",
    "Двотижневий ритм йоги",
    "Тримайте 14-денну серію йога-плану — відпочинок рахується, якщо запланований.",
    family="yoga.streak",
    sort=60,
)
add(
    "yoga.challenge.30d_mobility",
    "yoga",
    metal("gold"),
    "achievement",
    challenge_rule("mobility_30_day"),
    "30-day mobility",
    "Finish a 30-day mobility challenge.",
    "30 днів рухливості",
    "Завершіть 30-денний виклик рухливості.",
    family="yoga.challenge",
    sort=70,
)

# —— Pilates (7) ——
add(
    "pilates.first_session",
    "pilates",
    metal("bronze"),
    "pilates",
    counter("workout_completed", 1, filters={"discipline": "pilates"}),
    "First pilates session",
    "Complete your first pilates session.",
    "Перша сесія пілатесу",
    "Завершіть першу сесію пілатесу.",
    family="pilates.sessions",
    sort=10,
)
add(
    "pilates.sessions.5",
    "pilates",
    metal("bronze"),
    "pilates",
    counter("workout_completed", 5, filters={"discipline": "pilates"}),
    "Core beginnings",
    "Complete 5 pilates sessions.",
    "Початок кору",
    "Завершіть 5 сесій пілатесу.",
    family="pilates.sessions",
    sort=20,
)
add(
    "pilates.sessions.15",
    "pilates",
    metal("silver"),
    "pilates",
    counter("workout_completed", 15, filters={"discipline": "pilates"}),
    "Fifteen focused",
    "Complete 15 pilates sessions.",
    "П’ятнадцять у фокусі",
    "Завершіть 15 сесій пілатесу.",
    family="pilates.sessions",
    sort=30,
)
add(
    "pilates.sessions.40",
    "pilates",
    metal("gold"),
    "pilates",
    counter("workout_completed", 40, filters={"discipline": "pilates"}),
    "Pilates staple",
    "Complete 40 pilates sessions.",
    "Пілатес як звичка",
    "Завершіть 40 сесій пілатесу.",
    family="pilates.sessions",
    sort=40,
)
add(
    "pilates.minutes.25",
    "pilates",
    metal("bronze"),
    "pilates",
    threshold("duration_min", 25, filters={"discipline": "pilates"}),
    "Focused twenty-five",
    "Complete a pilates session of at least 25 minutes.",
    "Двадцять п’ять уважних",
    "Завершіть сесію пілатесу щонайменше на 25 хвилин.",
    family="pilates.duration",
    sort=50,
)
add(
    "pilates.streak.14",
    "pilates",
    metal("silver"),
    "streak",
    streak("training_or_rest_day", 14, filters={"discipline": "pilates"}),
    "Two-week pilates rhythm",
    "Keep a 14-day pilates plan streak — planned rest counts.",
    "Двотижневий ритм пілатесу",
    "Тримайте 14-денну серію пілатес-плану — запланований відпочинок рахується.",
    family="pilates.streak",
    sort=60,
)
add(
    "pilates.week_plan",
    "pilates",
    metal("silver"),
    "planner",
    counter("planned_session_completed", 2, filters={"discipline": "pilates"}, window={"unit": "week", "count": 1}),
    "Pilates week kept",
    "Complete at least 2 planned pilates sessions in a calendar week.",
    "Тиждень пілатесу",
    "Завершіть щонайменше 2 заплановані сесії пілатесу за тиждень.",
    family="pilates.adherence",
    sort=70,
)

# —— Nutrition (8) — logging only, never restriction ——
add(
    "nutrition.first_log",
    "nutrition",
    metal("bronze"),
    "food-log",
    counter("meal_logged", 1),
    "First meal logged",
    "Log your first meal or snack.",
    "Перший запис їжі",
    "Запишіть перший прийом їжі або перекус.",
    family="nutrition.logs",
    sort=10,
)
add(
    "nutrition.logs.10",
    "nutrition",
    metal("bronze"),
    "food",
    counter("meal_logged", 10),
    "Ten meals noted",
    "Log 10 meals or snacks.",
    "Десять записів",
    "Запишіть 10 прийомів їжі або перекусів.",
    family="nutrition.logs",
    sort=20,
)
add(
    "nutrition.logs.50",
    "nutrition",
    metal("silver"),
    "food",
    counter("meal_logged", 50),
    "Fifty logs",
    "Log 50 meals or snacks — awareness, not restriction.",
    "П’ятдесят записів",
    "Запишіть 50 прийомів їжі — усвідомленість, не обмеження.",
    family="nutrition.logs",
    sort=30,
)
add(
    "nutrition.logs.200",
    "nutrition",
    metal("gold"),
    "food",
    counter("meal_logged", 200),
    "Logging mainstay",
    "Log 200 meals or snacks.",
    "Звичка записувати",
    "Запишіть 200 прийомів їжі або перекусів.",
    family="nutrition.logs",
    sort=40,
)
add(
    "nutrition.days.7",
    "nutrition",
    metal("bronze"),
    "streak",
    streak("nutrition_day", 7),
    "Week of food awareness",
    "Log food on 7 consecutive days (planned rest from logging may use a freeze).",
    "Тиждень усвідомленості",
    "Записуйте їжу 7 днів поспіль (можна використати «заморозку» серії).",
    family="nutrition.streak",
    sort=50,
)
add(
    "nutrition.days.21",
    "nutrition",
    metal("silver"),
    "streak",
    streak("nutrition_day", 21),
    "Three-week food log streak",
    "Log food on 21 consecutive days — freezes and rest days are supported.",
    "Три тижні записів їжі",
    "Записуйте їжу 21 день поспіль — заморозки та відпочинок підтримуються.",
    family="nutrition.streak",
    sort=60,
)
add(
    "nutrition.days.66",
    "nutrition",
    metal("gold"),
    "streak",
    streak("nutrition_day", 66),
    "Habit window",
    "Log food across a 66-day streak window — inspired by habit-formation research averages.",
    "Вікно звички",
    "Записуйте їжу протягом 66-денної серії — орієнтир із досліджень формування звичок.",
    family="nutrition.streak",
    sort=70,
)
add(
    "nutrition.protein_days.14",
    "nutrition",
    metal("silver"),
    "food",
    streak("protein_target_day", 14),
    "Protein rhythm",
    "Meet your personal protein target on 14 streak days — targets are yours, not extreme cuts.",
    "Ритм білка",
    "Досягайте особистої цілі з білка 14 днів поспіль — цілі ваші, без крайнощів.",
    family="nutrition.protein",
    sort=80,
)

# —— Water (7) — meeting goals, not excess ——
add(
    "water.first_log",
    "water",
    metal("bronze"),
    "water",
    counter("water_logged", 1),
    "First sip logged",
    "Log water for the first time.",
    "Перший ковток",
    "Запишіть воду вперше.",
    family="water.logs",
    sort=10,
)
add(
    "water.goal_days.3",
    "water",
    metal("bronze"),
    "water",
    streak("water_goal_day", 3),
    "Three hydrated days",
    "Meet your personal hydration goal three days in a row.",
    "Три дні гідратації",
    "Досягайте особистої цілі гідратації три дні поспіль.",
    family="water.goal_streak",
    sort=20,
)
add(
    "water.goal_days.7",
    "water",
    metal("silver"),
    "water",
    streak("water_goal_day", 7),
    "Hydration week",
    "Meet your hydration goal seven days in a row.",
    "Тиждень гідратації",
    "Досягайте цілі гідратації сім днів поспіль.",
    family="water.goal_streak",
    sort=30,
)
add(
    "water.goal_days.30",
    "water",
    metal("gold"),
    "water",
    streak("water_goal_day", 30),
    "Hydration month rhythm",
    "Meet your hydration goal across a 30-day streak (freezes allowed).",
    "Місячний ритм води",
    "Досягайте цілі гідратації протягом 30-денної серії (заморозки дозволені).",
    family="water.goal_streak",
    sort=40,
)
add(
    "water.goal_days.100",
    "water",
    metal("platinum"),
    "trophy",
    counter("water_goal_day", 100),
    "Hundred hydrated days",
    "Meet your hydration goal on 100 separate days (not necessarily consecutive).",
    "Сто днів гідратації",
    "Досягайте цілі гідратації у 100 окремих днях (не обов’язково підряд).",
    family="water.goal_count",
    sort=50,
)
add(
    "water.challenge.21d",
    "water",
    metal("gold"),
    "achievement",
    challenge_rule("hydration_21_day"),
    "21-day hydration challenge",
    "Complete a 21-day hydration challenge at your own goal.",
    "21-денний виклик гідратації",
    "Завершіть 21-денний виклик гідратації за власною ціллю.",
    family="water.challenge",
    sort=60,
)
add(
    "water.logs.50",
    "water",
    metal("silver"),
    "water",
    counter("water_logged", 50),
    "Fifty water logs",
    "Log water 50 times.",
    "П’ятдесят записів води",
    "Запишіть воду 50 разів.",
    family="water.logs",
    sort=70,
)

# —— Recovery / sleep (9) ——
add(
    "recovery.sleep_first",
    "recovery",
    metal("bronze"),
    "sleep",
    counter("sleep_imported", 1),
    "First sleep import",
    "Import or log a night of sleep.",
    "Перший імпорт сну",
    "Імпортуйте або запишіть ніч сну.",
    family="recovery.sleep_logs",
    sort=10,
)
add(
    "recovery.sleep_nights.7",
    "recovery",
    metal("bronze"),
    "sleep",
    counter("sleep_imported", 7),
    "Week of sleep data",
    "Log or import 7 nights of sleep.",
    "Тиждень даних сну",
    "Запишіть або імпортуйте 7 ночей сну.",
    family="recovery.sleep_logs",
    sort=20,
)
add(
    "recovery.sleep_nights.30",
    "recovery",
    metal("silver"),
    "sleep",
    counter("sleep_imported", 30),
    "Month of sleep awareness",
    "Log or import 30 nights of sleep.",
    "Місяць усвідомлення сну",
    "Запишіть або імпортуйте 30 ночей сну.",
    family="recovery.sleep_logs",
    sort=30,
)
add(
    "recovery.sleep_streak.14",
    "recovery",
    metal("silver"),
    "streak",
    streak("sleep_logged_day", 14),
    "Fortnight of sleep logging",
    "Log sleep fourteen days in a row.",
    "Два тижні записів сну",
    "Записуйте сон чотирнадцять днів поспіль.",
    family="recovery.sleep_streak",
    sort=40,
)
add(
    "recovery.rest_when_low.3",
    "recovery",
    metal("gold"),
    "readiness",
    counter("rest_taken_on_low_readiness", 3),
    "Wise rest",
    "Take a planned rest day on low readiness three times — recovery is progress.",
    "Розумний відпочинок",
    "Візьміть запланований відпочинок при низькій готовності тричі — відновлення це прогрес.",
    family="recovery.wise_rest",
    sort=50,
)
add(
    "recovery.rest_when_low.10",
    "recovery",
    metal("platinum"),
    "recovery",
    counter("rest_taken_on_low_readiness", 10),
    "Recovery advocate",
    "Honour low-readiness rest ten times.",
    "Захисник відновлення",
    "Поважайте відпочинок при низькій готовності десять разів.",
    family="recovery.wise_rest",
    sort=60,
)
add(
    "recovery.hrv_logged.10",
    "recovery",
    metal("bronze"),
    "hrv",
    counter("hrv_logged", 10),
    "HRV awareness",
    "Log heart-rate variability on 10 days.",
    "Усвідомлення ВСР",
    "Запишіть варіабельність серцевого ритму у 10 днях.",
    family="recovery.hrv",
    sort=70,
)
add(
    "recovery.challenge.14d_sleep",
    "recovery",
    metal("gold"),
    "achievement",
    challenge_rule("sleep_logging_14_day"),
    "14-day sleep challenge",
    "Complete a 14-day sleep-logging challenge.",
    "14-денний виклик сну",
    "Завершіть 14-денний виклик запису сну.",
    family="recovery.challenge",
    sort=80,
)
add(
    "recovery.stress_checkins.20",
    "recovery",
    metal("silver"),
    "stress",
    counter("stress_checkin", 20),
    "Stress check-ins",
    "Complete 20 stress or mood check-ins.",
    "Перевірки стресу",
    "Зробіть 20 перевірок стресу або настрою.",
    family="recovery.stress",
    sort=90,
)

# —— Body measurements (6) — logging consistency only ——
add(
    "measurements.first",
    "body_measurements",
    metal("bronze"),
    "measurements",
    counter("measurement_logged", 1),
    "First measurement",
    "Log a body measurement (any site).",
    "Перший вимір",
    "Запишіть вимір тіла (будь-який).",
    family="measurements.logs",
    sort=10,
)
add(
    "measurements.logs.5",
    "body_measurements",
    metal("bronze"),
    "measurements",
    counter("measurement_logged", 5),
    "Five check-ins",
    "Log body measurements five times — tracking trends, not chasing crash loss.",
    "П’ять перевірок",
    "Запишіть виміри п’ять разів — тренди, не стрімке схуднення.",
    family="measurements.logs",
    sort=20,
)
add(
    "measurements.logs.20",
    "body_measurements",
    metal("silver"),
    "measurements",
    counter("measurement_logged", 20),
    "Twenty measurement logs",
    "Log body measurements twenty times.",
    "Двадцять записів вимірів",
    "Запишіть виміри тіла двадцять разів.",
    family="measurements.logs",
    sort=30,
)
add(
    "measurements.weekly.8",
    "body_measurements",
    metal("gold"),
    "measurements",
    counter("measurement_week_logged", 8),
    "Eight weeks of logging",
    "Log measurements in eight distinct calendar weeks.",
    "Вісім тижнів записів",
    "Записуйте виміри у восьми різних календарних тижнях.",
    family="measurements.weeks",
    sort=40,
)
add(
    "measurements.streak.4_weeks",
    "body_measurements",
    metal("silver"),
    "streak",
    streak("measurement_week_day", 28),
    "Monthly measuring rhythm",
    "Keep a four-week measurement logging rhythm (one log per week keeps the streak).",
    "Місячний ритм вимірів",
    "Тримайте чотиритижневий ритм вимірів (один запис на тиждень підтримує серію).",
    family="measurements.streak",
    sort=50,
)
add(
    "measurements.sites.3",
    "body_measurements",
    metal("bronze"),
    "measurements",
    counter("distinct_measurement_sites", 3),
    "Three sites tracked",
    "Log at least three different measurement sites over time.",
    "Три зони вимірів",
    "Запишіть щонайменше три різні зони вимірів з часом.",
    family="measurements.sites",
    sort=60,
)

# —— Consistency (12) ——
add(
    "consistency.first_active_day",
    "consistency",
    metal("bronze"),
    "streak",
    counter("active_day", 1),
    "Day one",
    "Complete your first active day in OmniGart.",
    "День перший",
    "Завершіть перший активний день в OmniGart.",
    family="consistency.active",
    sort=10,
)
add(
    "consistency.active_days.7",
    "consistency",
    metal("bronze"),
    "streak",
    counter("active_day", 7),
    "Seven active days",
    "Accumulate 7 active days.",
    "Сім активних днів",
    "Наберіть 7 активних днів.",
    family="consistency.active",
    sort=20,
)
add(
    "consistency.active_days.30",
    "consistency",
    metal("silver"),
    "streak",
    counter("active_day", 30),
    "Thirty active days",
    "Accumulate 30 active days.",
    "Тридцять активних днів",
    "Наберіть 30 активних днів.",
    family="consistency.active",
    sort=30,
)
add(
    "consistency.active_days.100",
    "consistency",
    metal("gold"),
    "trophy",
    counter("active_day", 100),
    "Hundred active days",
    "Accumulate 100 active days.",
    "Сто активних днів",
    "Наберіть 100 активних днів.",
    family="consistency.active",
    sort=40,
)
add(
    "consistency.streak.7",
    "consistency",
    metal("bronze"),
    "streak",
    streak("active_or_rest_day", 7),
    "Seven-day streak",
    "Keep a 7-day streak — planned rest and streak freezes count.",
    "Серія з семи днів",
    "Тримайте 7-денну серію — запланований відпочинок і заморозки рахуються.",
    family="consistency.streak",
    sort=50,
)
add(
    "consistency.streak.21",
    "consistency",
    metal("silver"),
    "streak",
    streak("active_or_rest_day", 21),
    "Twenty-one day streak",
    "Keep a 21-day streak with rest days and freezes supported.",
    "Серія з 21 дня",
    "Тримайте 21-денну серію з підтримкою відпочинку та заморозок.",
    family="consistency.streak",
    sort=60,
)
add(
    "consistency.streak.66",
    "consistency",
    metal("gold"),
    "streak",
    streak("active_or_rest_day", 66),
    "Sixty-six day streak",
    "Reach a 66-day streak window — aligned with average habit automation times in Lally et al. 2010.",
    "Серія з 66 днів",
    "Досягніть 66-денної серії — орієнтир середнього часу автоматизації звички (Lally et al., 2010).",
    family="consistency.streak",
    sort=70,
)
add(
    "consistency.streak.100",
    "consistency",
    metal("platinum"),
    "trophy",
    streak("active_or_rest_day", 100),
    "Hundred-day rhythm",
    "Keep a 100-day streak of plan adherence.",
    "Ритм зі ста днів",
    "Тримайте 100-денну серію дотримання плану.",
    family="consistency.streak",
    sort=80,
)
add(
    "consistency.multicategory.3",
    "consistency",
    metal("silver"),
    "achievement",
    counter("distinct_disciplines_week", 3, window={"unit": "week", "count": 1}),
    "Cross-training week",
    "Train in at least three disciplines in one calendar week.",
    "Тиждень крос-тренувань",
    "Тренуйтеся щонайменше у трьох дисциплінах за один тиждень.",
    family="consistency.variety",
    sort=90,
)
add(
    "consistency.weekly_goal.4",
    "consistency",
    level(1),
    "planner",
    counter("weekly_goal_met", 4),
    "Four weeks on target",
    "Meet your weekly activity goal four times.",
    "Чотири тижні в цілі",
    "Досягайте тижневої цілі активності чотири рази.",
    family="consistency.weekly_goal",
    sort=100,
)
add(
    "consistency.weekly_goal.12",
    "consistency",
    level(2),
    "planner",
    counter("weekly_goal_met", 12),
    "Twelve weeks on target",
    "Meet your weekly activity goal twelve times.",
    "Дванадцять тижнів у цілі",
    "Досягайте тижневої цілі активності дванадцять разів.",
    family="consistency.weekly_goal",
    sort=110,
)
add(
    "consistency.challenge.any",
    "consistency",
    metal("gold"),
    "achievement",
    challenge_rule("any", value=1),
    "Challenge finisher",
    "Complete any personal time-boxed challenge.",
    "Завершувач виклику",
    "Завершіть будь-який особистий обмежений у часі виклик.",
    family="consistency.challenge",
    sort=120,
    hidden=True,
)

# —— Extra to reach ~80 if needed: levelled water/nutrition already counted ——
# Count check in main()

CHALLENGES = [
    {
        "id": "challenge.mobility_30_day",
        "challengeType": "mobility_30_day",
        "durationDays": 30,
        "iconId": "yoga",
        "titleKey": "challenge.mobility_30_day.title",
        "descriptionKey": "challenge.mobility_30_day.description",
        "goal": {
            "metric": "workout_completed",
            "comparator": "gte",
            "value": 20,
            "filters": {"discipline": "yoga"},
            "window": {"unit": "challenge", "count": 1},
        },
        "localOnly": True,
        "social": False,
    },
    {
        "id": "challenge.run_100km_month",
        "challengeType": "run_100km_month",
        "durationDays": 31,
        "iconId": "running",
        "titleKey": "challenge.run_100km_month.title",
        "descriptionKey": "challenge.run_100km_month.description",
        "goal": {
            "metric": "distance_m",
            "comparator": "gte",
            "value": 100000,
            "filters": {"discipline": "running"},
            "window": {"unit": "challenge", "count": 1},
        },
        "localOnly": True,
        "social": False,
    },
    {
        "id": "challenge.hydration_21_day",
        "challengeType": "hydration_21_day",
        "durationDays": 21,
        "iconId": "water",
        "titleKey": "challenge.hydration_21_day.title",
        "descriptionKey": "challenge.hydration_21_day.description",
        "goal": {
            "metric": "water_goal_day",
            "comparator": "gte",
            "value": 18,
            "window": {"unit": "challenge", "count": 1},
        },
        "localOnly": True,
        "social": False,
    },
    {
        "id": "challenge.sleep_logging_14_day",
        "challengeType": "sleep_logging_14_day",
        "durationDays": 14,
        "iconId": "sleep",
        "titleKey": "challenge.sleep_logging_14_day.title",
        "descriptionKey": "challenge.sleep_logging_14_day.description",
        "goal": {
            "metric": "sleep_logged_day",
            "comparator": "gte",
            "value": 12,
            "window": {"unit": "challenge", "count": 1},
        },
        "localOnly": True,
        "social": False,
    },
    {
        "id": "challenge.strength_adherence_28_day",
        "challengeType": "strength_adherence_28_day",
        "durationDays": 28,
        "iconId": "strength",
        "titleKey": "challenge.strength_adherence_28_day.title",
        "descriptionKey": "challenge.strength_adherence_28_day.description",
        "goal": {
            "metric": "training_or_rest_day",
            "comparator": "gte",
            "value": 24,
            "filters": {"discipline": "strength"},
            "window": {"unit": "challenge", "count": 1},
        },
        "localOnly": True,
        "social": False,
    },
]

CHALLENGE_STRINGS = {
    "en-GB": {
        "challenge.mobility_30_day.title": "30-day mobility",
        "challenge.mobility_30_day.description": "Complete 20 yoga or mobility sessions within 30 days. Rest days are encouraged.",
        "challenge.run_100km_month.title": "100 km month",
        "challenge.run_100km_month.description": "Accumulate 100 km of running within about a month. Build gradually.",
        "challenge.hydration_21_day.title": "21-day hydration",
        "challenge.hydration_21_day.description": "Meet your personal water goal on at least 18 of 21 days.",
        "challenge.sleep_logging_14_day.title": "14-day sleep log",
        "challenge.sleep_logging_14_day.description": "Log or import sleep on at least 12 of 14 nights.",
        "challenge.strength_adherence_28_day.title": "28-day strength rhythm",
        "challenge.strength_adherence_28_day.description": "Keep strength-plan adherence on at least 24 of 28 days, including planned rest.",
    },
    "uk": {
        "challenge.mobility_30_day.title": "30 днів рухливості",
        "challenge.mobility_30_day.description": "Завершіть 20 сесій йоги або рухливості за 30 днів. Відпочинок вітається.",
        "challenge.run_100km_month.title": "100 км за місяць",
        "challenge.run_100km_month.description": "Наберіть 100 км бігу приблизно за місяць. Збільшуйте навантаження поступово.",
        "challenge.hydration_21_day.title": "21 день гідратації",
        "challenge.hydration_21_day.description": "Досягайте особистої цілі води щонайменше 18 із 21 дня.",
        "challenge.sleep_logging_14_day.title": "14 днів запису сну",
        "challenge.sleep_logging_14_day.description": "Записуйте або імпортуйте сон щонайменше 12 із 14 ночей.",
        "challenge.strength_adherence_28_day.title": "28 днів силового ритму",
        "challenge.strength_adherence_28_day.description": "Дотримуйтесь силового плану щонайменше 24 із 28 днів, включно із запланованим відпочинком.",
    },
}


def main() -> None:
    achievements = []
    en: dict[str, str] = {}
    uk: dict[str, str] = {}
    for i, (id_, category, tier, icon, rule, te, de, tu, du, kwargs) in enumerate(ENTRIES):
        item = ach(id_, category, tier, icon, rule, **kwargs)
        achievements.append(item)
        en[item["titleKey"]] = te
        en[item["descriptionKey"]] = de
        uk[item["titleKey"]] = tu
        uk[item["descriptionKey"]] = du

    # category labels
    cats = {
        "strength": ("Strength", "Сила"),
        "running": ("Running", "Біг"),
        "swimming": ("Swimming", "Плавання"),
        "yoga": ("Yoga", "Йога"),
        "pilates": ("Pilates", "Пілатес"),
        "nutrition": ("Nutrition", "Харчування"),
        "water": ("Water", "Вода"),
        "recovery": ("Recovery & sleep", "Відновлення та сон"),
        "body_measurements": ("Body measurements", "Виміри тіла"),
        "consistency": ("Consistency", "Стабільність"),
    }
    for key, (e, u) in cats.items():
        en[f"achievement.category.{key}"] = e
        uk[f"achievement.category.{key}"] = u

    en["achievement.tier.bronze"] = "Bronze"
    en["achievement.tier.silver"] = "Silver"
    en["achievement.tier.gold"] = "Gold"
    en["achievement.tier.platinum"] = "Platinum"
    uk["achievement.tier.bronze"] = "Бронза"
    uk["achievement.tier.silver"] = "Срібло"
    uk["achievement.tier.gold"] = "Золото"
    uk["achievement.tier.platinum"] = "Платина"
    en["achievement.progress.next_tier"] = "Progress to next tier"
    uk["achievement.progress.next_tier"] = "Прогрес до наступного рівня"
    en["achievement.backfill.summary"] = "You unlocked {count} achievements from imported history."
    uk["achievement.backfill.summary"] = "Ви відкрили {count} досягнень з імпортованої історії."
    en["achievement.secret.unlocked"] = "Secret achievement unlocked"
    uk["achievement.secret.unlocked"] = "Секретне досягнення відкрито"

    en.update(CHALLENGE_STRINGS["en-GB"])
    uk.update(CHALLENGE_STRINGS["uk"])

    catalog = {
        "version": 1,
        "localePolicy": [
            "en-GB",
            "uk",
            "pl",
            "be",
            "kk",
            "ka",
            "es",
            "fr",
            "de",
            "cs",
            "zh-Hans",
        ],
        "healthyByDesign": {
            "rewardConsistency": True,
            "forbidCalorieRestrictionRewards": True,
            "forbidDehydrationOrExcessWaterRewards": True,
            "forbidTrainingThroughPoorRecoveryRewards": True,
            "forbidRapidWeightLossRewards": True,
            "weightAchievementsTrackLoggingOnly": True,
            "streaksAllowPlannedRestAndFreeze": True,
        },
        "achievements": achievements,
    }

    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "data").mkdir(exist_ok=True)
    (OUT / "locales").mkdir(exist_ok=True)

    (OUT / "data" / "achievements.json").write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (OUT / "data" / "challenge-templates.json").write_text(
        json.dumps({"version": 1, "templates": CHALLENGES}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    (OUT / "locales" / "en-GB.json").write_text(
        json.dumps(dict(sorted(en.items())), ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (OUT / "locales" / "uk.json").write_text(
        json.dumps(dict(sorted(uk.items())), ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    from collections import Counter

    counts = Counter(a["category"] for a in achievements)
    print(f"Total achievements: {len(achievements)}")
    for k, v in sorted(counts.items()):
        print(f"  {k}: {v}")
    print(f"Challenge templates: {len(CHALLENGES)}")
    print(f"Locale keys en-GB: {len(en)}, uk: {len(uk)}")


if __name__ == "__main__":
    main()
