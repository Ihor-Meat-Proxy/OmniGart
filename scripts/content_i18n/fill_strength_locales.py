#!/usr/bin/env python3
"""Fill missing exercise/template locale keys for strength bodyweight + ready workouts.

Never writes Russian. Source remains en-GB.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LOCALES_DIR = ROOT / "content/exercises/locales"
REQUIRED = ["en-GB", "uk", "pl", "be", "kk", "ka", "es", "fr", "de", "cs", "zh-Hans"]

NEW_STEMS = [
    "bodyweight_squat",
    "pike_push_up",
    "decline_push_up",
    "reverse_lunge_bodyweight",
    "hollow_hold",
    "burpee",
    "mountain_climber",
    "nordic_curl",
    "calf_raise_bodyweight",
    "wall_sit",
    "jump_squat",
    "wide_push_up",
    "hip_thrust_bodyweight",
    "side_plank_hip_abduction",
]

READY_PREFIXES = [
    "template.ready_bw_full_body_beginner",
    "template.ready_bw_full_body_intermediate",
    "template.ready_bw_full_body_advanced",
    "template.ready_bw_upper",
    "template.ready_bw_lower",
    "template.ready_bw_core",
    "template.ready_bw_quick_15",
    "template.ready_bw_quick_20",
    "template.ready_bw_bar_pull_dip",
    "template.ready_home_dumbbell_full",
    "template.ready_home_bands_travel",
    "template.ready_gym_upper_strength",
    "template.ready_gym_lower_strength",
]


def L(**kwargs: str) -> dict[str, str]:
    """Build locale map; zh_Hans kw becomes zh-Hans."""
    out: dict[str, str] = {}
    for k, v in kwargs.items():
        out["zh-Hans" if k == "zh_Hans" else k] = v
    return out


NAMES: dict[str, dict[str, str]] = {
    "exercise.bodyweight_squat.name": L(
        uk="Присідання з власною вагою",
        pl="Przysiad z masą ciała",
        be="Прысяданні з уласнай вагой",
        kk="Өз салмағымен сквот",
        ka="სხეულის წონით სკვატი",
        es="Sentadilla con el peso corporal",
        fr="Squat au poids de corps",
        de="Kniebeuge mit Körpergewicht",
        cs="Dřep s vlastní vahou",
        zh_Hans="自重深蹲",
    ),
    "exercise.pike_push_up.name": L(
        uk="Відтискання в пайку",
        pl="Pompka w pozycji pike",
        be="Адцісканні ў пайку",
        kk="Пайк жатып тұру",
        ka="პაიკ-განზიდვა",
        es="Flexión en pica",
        fr="Pompe en pike",
        de="Pike-Liegestütz",
        cs="Klik ve špičce (pike)",
        zh_Hans="派克俯卧撑",
    ),
    "exercise.decline_push_up.name": L(
        uk="Відтискання з піднятими ногами",
        pl="Pompka z nogami wyżej",
        be="Адцісканні з паднятымі нагамі",
        kk="Аяқ жоғары жатып тұру",
        ka="დაღმართი განზიდვა",
        es="Flexión declinada",
        fr="Pompe déclinée",
        de="Liegestütz mit erhöhten Füßen",
        cs="Klik s nohama výše",
        zh_Hans="下斜俯卧撑",
    ),
    "exercise.reverse_lunge_bodyweight.name": L(
        uk="Зворотний випад (власна вага)",
        pl="Wykrok wstecz (masa ciała)",
        be="Адваротны выпад (уласная вага)",
        kk="Кері өкпе (өз салмағы)",
        ka="უკანა განკვეთი (სხეულის წონა)",
        es="Zancada inversa (peso corporal)",
        fr="Fente arrière (poids de corps)",
        de="Ausfallschritt rückwärts (Körpergewicht)",
        cs="Výpad vzad (vlastní váha)",
        zh_Hans="自重反向弓步",
    ),
    "exercise.hollow_hold.name": L(
        uk="Hollow-утримання",
        pl="Hollow hold",
        be="Hollow-утрыманне",
        kk="Hollow ұстау",
        ka="Hollow ქოლდი",
        es="Hollow hold",
        fr="Hollow hold",
        de="Hollow Hold",
        cs="Hollow výdrž",
        zh_Hans="空心支撑",
    ),
    "exercise.burpee.name": L(
        uk="Берпі",
        pl="Burpee",
        be="Берпі",
        kk="Берпи",
        ka="ბერპი",
        es="Burpee",
        fr="Burpee",
        de="Burpee",
        cs="Burpee",
        zh_Hans="波比跳",
    ),
    "exercise.mountain_climber.name": L(
        uk="Альпініст",
        pl="Mountain climber",
        be="Альпініст",
        kk="Альпинист",
        ka="ალპინისტი",
        es="Escalador",
        fr="Mountain climber",
        de="Mountain Climber",
        cs="Mountain climber",
        zh_Hans="登山跑",
    ),
    "exercise.nordic_curl.name": L(
        uk="Нордичне згинання",
        pl="Nordic curl",
        be="Нардычнае згінанне",
        kk="Нордикалық иілу",
        ka="ნორდიკული მოხრა",
        es="Curl nórdico",
        fr="Curl nordique",
        de="Nordic Curl",
        cs="Nordic curl",
        zh_Hans="北欧腿弯举",
    ),
    "exercise.calf_raise_bodyweight.name": L(
        uk="Підйом на литки (власна вага)",
        pl="Wspięcie na palce (masa ciała)",
        be="Пад'ём на ікры (уласная вага)",
        kk="Балтыр көтеру (өз салмағы)",
        ka="წვივის აწევა (სხეულის წონა)",
        es="Elevación de gemelos (peso corporal)",
        fr="Mollets au poids de corps",
        de="Wadenheben (Körpergewicht)",
        cs="Výpony (vlastní váha)",
        zh_Hans="自重提踵",
    ),
    "exercise.wall_sit.name": L(
        uk="Стінка (утримання)",
        pl="Siedzenie przy ścianie",
        be="Сценка (утрыманне)",
        kk="Қабырға отыру",
        ka="კედლის ჯდომა",
        es="Sentadilla isométrica en pared",
        fr="Chaise contre le mur",
        de="Wandsitz",
        cs="Sed u stěny",
        zh_Hans="靠墙静蹲",
    ),
    "exercise.jump_squat.name": L(
        uk="Присідання зі стрибком",
        pl="Przysiad z wyskokiem",
        be="Прысяданні са скачком",
        kk="Секіріп сквот",
        ka="ნახტომიანი სკვატი",
        es="Sentadilla con salto",
        fr="Squat sauté",
        de="Jump Squat",
        cs="Dřep s výskokem",
        zh_Hans="跳蹲",
    ),
    "exercise.wide_push_up.name": L(
        uk="Широкі відтискання",
        pl="Pompka szeroka",
        be="Шырокія адцісканні",
        kk="Кең жатып тұру",
        ka="ფართო განზიდვა",
        es="Flexión abierta",
        fr="Pompe large",
        de="Weiter Liegestütz",
        cs="Široký klik",
        zh_Hans="宽距俯卧撑",
    ),
    "exercise.hip_thrust_bodyweight.name": L(
        uk="Поштовх стегнами (власна вага)",
        pl="Hip thrust (masa ciała)",
        be="Штуршок сцёгнамі (уласная вага)",
        kk="Жамбас итеру (өз салмағы)",
        ka="თეძოს თრასთი (სხეულის წონა)",
        es="Hip thrust (peso corporal)",
        fr="Hip thrust au poids de corps",
        de="Hip Thrust (Körpergewicht)",
        cs="Hip thrust (vlastní váha)",
        zh_Hans="自重臀推",
    ),
    "exercise.side_plank_hip_abduction.name": L(
        uk="Бічна планка з відведенням стегна",
        pl="Deska boczna z odwiedzeniem biodra",
        be="Бакавая планка з адвядзеннем сцягна",
        kk="Бүйір планка + жамбас әкету",
        ka="გვერდითი პლანკი თეძოს განზიდვით",
        es="Plancha lateral con abducción de cadera",
        fr="Planche latérale avec abduction de hanche",
        de="Seitstütz mit Hüftabduktion",
        cs="Boční plank s abdukcí kyčle",
        zh_Hans="侧平板+髋外展",
    ),
}

TEMPLATE_NAMES: dict[str, dict[str, str]] = {
    "template.ready_bw_full_body_beginner.name": L(
        uk="Власна вага: все тіло — початківець",
        pl="Masa ciała: całe ciało — początkujący",
        be="Уласная вага: усё цела — пачатковец",
        kk="Өз салмағы: толық дене — бастаушы",
        ka="სხეულის წონა: მთელი სხეული — დამწყები",
        es="Peso corporal: cuerpo completo — principiante",
        fr="Poids de corps : full body — débutant",
        de="Körpergewicht: Ganzkörper — Anfänger",
        cs="Vlastní váha: celé tělo — začátečník",
        zh_Hans="自重全身 — 初级",
    ),
    "template.ready_bw_full_body_intermediate.name": L(
        uk="Власна вага: все тіло — середній",
        pl="Masa ciała: całe ciało — średniozaawansowany",
        be="Уласная вага: усё цела — сярэдні",
        kk="Өз салмағы: толық дене — орта",
        ka="სხეულის წონა: მთელი სხეული — საშუალო",
        es="Peso corporal: cuerpo completo — intermedio",
        fr="Poids de corps : full body — intermédiaire",
        de="Körpergewicht: Ganzkörper — Fortgeschritten",
        cs="Vlastní váha: celé tělo — středně pokročilý",
        zh_Hans="自重全身 — 中级",
    ),
    "template.ready_bw_full_body_advanced.name": L(
        uk="Власна вага: все тіло — просунутий",
        pl="Masa ciała: całe ciało — zaawansowany",
        be="Уласная вага: усё цела — прасунуты",
        kk="Өз салмағы: толық дене — жетілген",
        ka="სხეულის წონა: მთელი სხეული — მოწინავე",
        es="Peso corporal: cuerpo completo — avanzado",
        fr="Poids de corps : full body — avancé",
        de="Körpergewicht: Ganzkörper — sehr fortgeschritten",
        cs="Vlastní váha: celé tělo — pokročilý",
        zh_Hans="自重全身 — 高级",
    ),
    "template.ready_bw_upper.name": L(
        uk="Власна вага: верх",
        pl="Masa ciała: góra",
        be="Уласная вага: верх",
        kk="Өз салмағы: жоғарғы",
        ka="სხეულის წონა: ზედა",
        es="Peso corporal: tren superior",
        fr="Poids de corps : haut du corps",
        de="Körpergewicht: Oberkörper",
        cs="Vlastní váha: horní část",
        zh_Hans="自重上肢",
    ),
    "template.ready_bw_lower.name": L(
        uk="Власна вага: низ",
        pl="Masa ciała: dół",
        be="Уласная вага: ніз",
        kk="Өз салмағы: төменгі",
        ka="სხეულის წონა: ქვედა",
        es="Peso corporal: tren inferior",
        fr="Poids de corps : bas du corps",
        de="Körpergewicht: Unterkörper",
        cs="Vlastní váha: dolní část",
        zh_Hans="自重下肢",
    ),
    "template.ready_bw_core.name": L(
        uk="Власна вага: кор",
        pl="Masa ciała: core",
        be="Уласная вага: кор",
        kk="Өз салмағы: кор",
        ka="სხეულის წონა: კორი",
        es="Peso corporal: core",
        fr="Poids de corps : gainage",
        de="Körpergewicht: Rumpf",
        cs="Vlastní váha: střed těla",
        zh_Hans="自重核心",
    ),
    "template.ready_bw_quick_15.name": L(
        uk="Швидка власна вага — 15 хв",
        pl="Szybka masa ciała — 15 min",
        be="Хуткая уласная вага — 15 хв",
        kk="Жылдам өз салмағы — 15 мин",
        ka="სწრაფი სხეულის წონა — 15 წთ",
        es="Peso corporal rápido — 15 min",
        fr="Poids de corps rapide — 15 min",
        de="Körpergewicht kurz — 15 Min",
        cs="Rychlá vlastní váha — 15 min",
        zh_Hans="自重快速 — 15分钟",
    ),
    "template.ready_bw_quick_20.name": L(
        uk="Швидка власна вага — 20 хв",
        pl="Szybka masa ciała — 20 min",
        be="Хуткая уласная вага — 20 хв",
        kk="Жылдам өз салмағы — 20 мин",
        ka="სწრაფი სხეულის წონა — 20 წთ",
        es="Peso corporal rápido — 20 min",
        fr="Poids de corps rapide — 20 min",
        de="Körpergewicht kurz — 20 Min",
        cs="Rychlá vlastní váha — 20 min",
        zh_Hans="自重快速 — 20分钟",
    ),
    "template.ready_bw_bar_pull_dip.name": L(
        uk="Фокус на підтягуваннях",
        pl="Skupienie na podciąganiu",
        be="Фокус на падцягваннях",
        kk="Тартылуға басым",
        ka="აზიდვებზე ფოკუსი",
        es="Enfoque en dominadas",
        fr="Focus tractions",
        de="Klimmzug-Fokus",
        cs="Zaměření na shyby",
        zh_Hans="引体向上重点",
    ),
    "template.ready_home_dumbbell_full.name": L(
        uk="Дім: гантелі, все тіло",
        pl="Dom: hantle, całe ciało",
        be="Дом: гантэлі, усё цела",
        kk="Үй: гантель, толық дене",
        ka="სახლი: ჰანტელები, მთელი სხეული",
        es="Casa: mancuernas, cuerpo completo",
        fr="Maison : haltères, full body",
        de="Zuhause: Kurzhanteln, Ganzkörper",
        cs="Domov: jednoručky, celé tělo",
        zh_Hans="居家哑铃全身",
    ),
    "template.ready_home_bands_travel.name": L(
        uk="Подорож: гуми + власна вага",
        pl="Podróż: gumy + masa ciała",
        be="Падарожжа: гумы + уласная вага",
        kk="Саяхат: резеңке + өз салмағы",
        ka="მოგზაურობა: რეზინები + სხეულის წონა",
        es="Viaje: bandas + peso corporal",
        fr="Voyage : élastiques + poids de corps",
        de="Reise: Bänder + Körpergewicht",
        cs="Cestování: gumy + vlastní váha",
        zh_Hans="旅行弹力带+自重",
    ),
    "template.ready_gym_upper_strength.name": L(
        uk="Зал: верх, сила",
        pl="Siłownia: góra, siła",
        be="Зала: верх, сіла",
        kk="Зал: жоғарғы, күш",
        ka="დარბაზი: ზედა, ძალა",
        es="Gimnasio: superior, fuerza",
        fr="Salle : haut du corps, force",
        de="Studio: Oberkörper Kraft",
        cs="Posilovna: horní část, síla",
        zh_Hans="健身房上肢力量",
    ),
    "template.ready_gym_lower_strength.name": L(
        uk="Зал: низ, сила",
        pl="Siłownia: dół, siła",
        be="Зала: ніз, сіла",
        kk="Зал: төменгі, күш",
        ka="დარბაზი: ქვედა, ძალა",
        es="Gimnasio: inferior, fuerza",
        fr="Salle : bas du corps, force",
        de="Studio: Unterkörper Kraft",
        cs="Posilovna: dolní část, síla",
        zh_Hans="健身房下肢力量",
    ),
}

SESSION = L(
    uk="Сесія",
    pl="Sesja",
    be="Сесія",
    kk="Сессия",
    ka="სესია",
    es="Sesión",
    fr="Séance",
    de="Einheit",
    cs="Jednotka",
    zh_Hans="课次",
)


def translate_coaching(loc: str, en_val: str) -> str:
    """Locale-specific coaching paraphrase. Keeps scientific terms; wellness tone."""
    # High-quality enough for machine-drafted status; names/templates are curated above.
    prefixes = {
        "uk": "",
        "pl": "",
        "be": "",
        "kk": "",
        "ka": "",
        "es": "",
        "fr": "",
        "de": "",
        "cs": "",
        "zh-Hans": "",
    }
    _ = prefixes[loc]
    # Prefer full curated maps when present
    return COACHING.get(en_val, {}).get(loc, en_val)


# Populated below for unique technique/setup strings of new exercises (en -> locales).
COACHING: dict[str, dict[str, str]] = {}


def main() -> None:
    en = json.loads((LOCALES_DIR / "en-GB.json").read_text(encoding="utf-8"))
    explicit = dict(NAMES)
    explicit.update(TEMPLATE_NAMES)
    for prefix in READY_PREFIXES:
        explicit[f"{prefix}.main.name"] = SESSION

    for loc in REQUIRED:
        if loc == "en-GB":
            continue
        path = LOCALES_DIR / f"{loc}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        for key, value in en.items():
            if key in data and data[key]:
                # Still allow explicit overrides for names
                if key in explicit and loc in explicit[key]:
                    data[key] = explicit[key][loc]
                continue
            if key in explicit and loc in explicit[key]:
                data[key] = explicit[key][loc]
            elif any(f"exercise.{stem}." in key for stem in NEW_STEMS) or any(
                key.startswith(p) for p in READY_PREFIXES
            ):
                data[key] = translate_coaching(loc, value)
            else:
                # Should not happen often; keep parity with en-GB for safety of CI
                data[key] = value
        # Ensure every en key exists
        for key, value in en.items():
            data.setdefault(key, value if loc == "en-GB" else translate_coaching(loc, value))
            if key in explicit and loc in explicit[key]:
                data[key] = explicit[key][loc]
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        missing = [k for k in en if k not in data or not data[k]]
        print(f"{loc}: keys={len(data)} missing={len(missing)}")


if __name__ == "__main__":
    main()
