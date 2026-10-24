#!/usr/bin/env python3
"""Sync in-workout logging UI strings (11 locales) into JSON, xcstrings, Android, Wear."""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCALES = [
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
]
ANDROID = {
    "en-GB": "values-en-rGB",
    "uk": "values-uk",
    "pl": "values-pl",
    "be": "values-be",
    "kk": "values-kk",
    "ka": "values-ka",
    "es": "values-es",
    "fr": "values-fr",
    "de": "values-de",
    "cs": "values-cs",
    "zh-Hans": "values-zh-rCN",
}
WEAR_ANDROID = ANDROID  # same folder names under wearApp

STRINGS: dict[str, dict[str, str]] = {}


def t(key: str, **by_locale: str) -> None:
    assert "en-GB" in by_locale
    for loc in LOCALES:
        assert loc in by_locale, f"{key} missing {loc}"
    STRINGS[key] = {loc: by_locale[loc] for loc in LOCALES}


t(
    "workout_finish",
    **{
        "en-GB": "Finish",
        "uk": "Завершити",
        "pl": "Zakończ",
        "be": "Завяршыць",
        "kk": "Аяқтау",
        "ka": "დასრულება",
        "es": "Finalizar",
        "fr": "Terminer",
        "de": "Beenden",
        "cs": "Dokončit",
        "zh-Hans": "完成",
    },
)
t(
    "workout_rest_timer",
    **{
        "en-GB": "Rest",
        "uk": "Відпочинок",
        "pl": "Odpoczynek",
        "be": "Адпачынак",
        "kk": "Демалыс",
        "ka": "დასვენება",
        "es": "Descanso",
        "fr": "Repos",
        "de": "Pause",
        "cs": "Odpočinek",
        "zh-Hans": "休息",
    },
)
t(
    "workout_note",
    **{
        "en-GB": "Note",
        "uk": "Нотатка",
        "pl": "Notatka",
        "be": "Нататка",
        "kk": "Жазба",
        "ka": "შენიშვნა",
        "es": "Nota",
        "fr": "Note",
        "de": "Notiz",
        "cs": "Poznámka",
        "zh-Hans": "备注",
    },
)
t(
    "workout_progression",
    **{
        "en-GB": "Target",
        "uk": "Ціль",
        "pl": "Cel",
        "be": "Мэта",
        "kk": "Мақсат",
        "ka": "მიზანი",
        "es": "Objetivo",
        "fr": "Objectif",
        "de": "Ziel",
        "cs": "Cíl",
        "zh-Hans": "目标",
    },
)
t(
    "workout_col_set",
    **{
        "en-GB": "SET",
        "uk": "ПІДХІД",
        "pl": "SERIA",
        "be": "ПАДЫХОД",
        "kk": "ЖИЫН",
        "ka": "სეტი",
        "es": "SERIE",
        "fr": "SÉRIE",
        "de": "SATZ",
        "cs": "SÉRIE",
        "zh-Hans": "组",
    },
)
t(
    "workout_col_previous",
    **{
        "en-GB": "PREVIOUS",
        "uk": "МИНУЛИЙ",
        "pl": "POPRZEDNI",
        "be": "ПАПЯРЭДНІ",
        "kk": "АЛДЫҢҒЫ",
        "ka": "წინა",
        "es": "ANTERIOR",
        "fr": "PRÉCÉDENT",
        "de": "VORHER",
        "cs": "PŘEDCHOZÍ",
        "zh-Hans": "上次",
    },
)
t(
    "workout_col_kg",
    **{
        "en-GB": "KG",
        "uk": "КГ",
        "pl": "KG",
        "be": "КГ",
        "kk": "КГ",
        "ka": "კგ",
        "es": "KG",
        "fr": "KG",
        "de": "KG",
        "cs": "KG",
        "zh-Hans": "公斤",
    },
)
t(
    "workout_col_lb",
    **{
        "en-GB": "LB",
        "uk": "ФУНТ",
        "pl": "LB",
        "be": "ФУНТ",
        "kk": "ФУНТ",
        "ka": "ფუნტი",
        "es": "LB",
        "fr": "LB",
        "de": "LB",
        "cs": "LB",
        "zh-Hans": "磅",
    },
)
t(
    "workout_col_reps",
    **{
        "en-GB": "REPS",
        "uk": "ПОВТ.",
        "pl": "POWTT.",
        "be": "ПАЎТ.",
        "kk": "ҚАЙТ.",
        "ka": "განმ.",
        "es": "REPS",
        "fr": "RÉPS",
        "de": "WDH.",
        "cs": "OPAK.",
        "zh-Hans": "次数",
    },
)
t(
    "workout_col_done",
    **{
        "en-GB": "✓",
        "uk": "✓",
        "pl": "✓",
        "be": "✓",
        "kk": "✓",
        "ka": "✓",
        "es": "✓",
        "fr": "✓",
        "de": "✓",
        "cs": "✓",
        "zh-Hans": "✓",
    },
)
t(
    "workout_replace_exercise",
    **{
        "en-GB": "Replace exercise",
        "uk": "Замінити вправу",
        "pl": "Zamień ćwiczenie",
        "be": "Замяніць практыкаванне",
        "kk": "Жаттығуды ауыстыру",
        "ka": "ვარჯიშის შეცვლა",
        "es": "Sustituir ejercicio",
        "fr": "Remplacer l’exercice",
        "de": "Übung ersetzen",
        "cs": "Nahradit cvik",
        "zh-Hans": "替换动作",
    },
)
t(
    "workout_remove_exercise",
    **{
        "en-GB": "Remove exercise",
        "uk": "Прибрати вправу",
        "pl": "Usuń ćwiczenie",
        "be": "Прыбраць практыкаванне",
        "kk": "Жаттығуды алып тастау",
        "ka": "ვარჯიშის ამოშლა",
        "es": "Quitar ejercicio",
        "fr": "Retirer l’exercice",
        "de": "Übung entfernen",
        "cs": "Odstranit cvik",
        "zh-Hans": "移除动作",
    },
)
t(
    "workout_mark_warmup",
    **{
        "en-GB": "Mark as warm-up",
        "uk": "Позначити як розминку",
        "pl": "Oznacz jako rozgrzewkę",
        "be": "Пазначыць як размінку",
        "kk": "Қыздыруу деп белгілеу",
        "ka": "გახურებად მონიშვნა",
        "es": "Marcar como calentamiento",
        "fr": "Marquer comme échauffement",
        "de": "Als Aufwärmen markieren",
        "cs": "Označit jako rozcvičku",
        "zh-Hans": "标为热身",
    },
)
t(
    "workout_mark_drop",
    **{
        "en-GB": "Mark as drop set",
        "uk": "Позначити як дроп-сет",
        "pl": "Oznacz jako drop set",
        "be": "Пазначыць як дроп-сэт",
        "kk": "Дроп-сет деп белгілеу",
        "ka": "დროპ-სეტად მონიშვნა",
        "es": "Marcar como drop set",
        "fr": "Marquer comme drop set",
        "de": "Als Drop-Satz markieren",
        "cs": "Označit jako drop set",
        "zh-Hans": "标为递减组",
    },
)
t(
    "workout_mark_failure",
    **{
        "en-GB": "Mark as failure",
        "uk": "Позначити як до відмови",
        "pl": "Oznacz jako do upadku",
        "be": "Пазначыць як да адмовы",
        "kk": "Сәтсіздікке дейін деп белгілеу",
        "ka": "უარამდე მონიშვნა",
        "es": "Marcar como al fallo",
        "fr": "Marquer comme à l’échec",
        "de": "Als Muskelversagen markieren",
        "cs": "Označit jako do selhání",
        "zh-Hans": "标为力竭",
    },
)
t(
    "workout_record_rpe",
    **{
        "en-GB": "Record RPE",
        "uk": "Записати RPE",
        "pl": "Zapisz RPE",
        "be": "Запісаць RPE",
        "kk": "RPE жазу",
        "ka": "RPE ჩაწერა",
        "es": "Registrar RPE",
        "fr": "Enregistrer le RPE",
        "de": "RPE erfassen",
        "cs": "Zaznamenat RPE",
        "zh-Hans": "记录 RPE",
    },
)
t(
    "workout_set_menu",
    **{
        "en-GB": "Set options",
        "uk": "Параметри підходу",
        "pl": "Opcje serii",
        "be": "Параметры падыходу",
        "kk": "Жиын параметрлері",
        "ka": "სეტის პარამეტრები",
        "es": "Opciones de serie",
        "fr": "Options de série",
        "de": "Satzoptionen",
        "cs": "Možnosti série",
        "zh-Hans": "组选项",
    },
)
t(
    "workout_weight_lb",
    **{
        "en-GB": "Weight (lb)",
        "uk": "Вага (фунт)",
        "pl": "Ciężar (lb)",
        "be": "Вага (фунт)",
        "kk": "Салмақ (фунт)",
        "ka": "წონა (ფუნტი)",
        "es": "Peso (lb)",
        "fr": "Charge (lb)",
        "de": "Gewicht (lb)",
        "cs": "Váha (lb)",
        "zh-Hans": "重量（磅）",
    },
)
t(
    "workout_body_map",
    **{
        "en-GB": "Muscles",
        "uk": "М’язи",
        "pl": "Mięśnie",
        "be": "Мышцы",
        "kk": "Бұлшықеттер",
        "ka": "კუნთები",
        "es": "Músculos",
        "fr": "Muscles",
        "de": "Muskeln",
        "cs": "Svaly",
        "zh-Hans": "肌肉",
    },
)
t(
    "workout_export_workoutkit",
    **{
        "en-GB": "Add to Apple Workout",
        "uk": "Додати до Apple Workout",
        "pl": "Dodaj do Apple Workout",
        "be": "Дадаць у Apple Workout",
        "kk": "Apple Workout-қа қосу",
        "ka": "Apple Workout-ში დამატება",
        "es": "Añadir a Apple Workout",
        "fr": "Ajouter à Apple Workout",
        "de": "Zu Apple Workout hinzufügen",
        "cs": "Přidat do Apple Workout",
        "zh-Hans": "添加到 Apple 体能训练",
    },
)
t(
    "workout_workoutkit_limit",
    **{
        "en-GB": (
            "Apple’s Workout app cannot accept weight or rep entry from other apps. "
            "OmniGart exports each set as a work step and each rest as recovery so you can "
            "follow the routine there; log load × reps in OmniGart on iPhone or Apple Watch."
        ),
        "uk": (
            "Додаток Workout від Apple не приймає вагу чи повторення з інших програм. "
            "OmniGart експортує кожен підхід як роботу, а відпочинок як відновлення; "
            "вагу × повторення записуйте в OmniGart на iPhone або Apple Watch."
        ),
        "pl": (
            "Aplikacja Workout Apple nie przyjmuje ciężaru ani powtórzeń z innych aplikacji. "
            "OmniGart eksportuje każdą serię jako pracę, a odpoczynek jako regenerację; "
            "obciążenie × powtórzenia zapisuj w OmniGart na iPhonie lub Apple Watch."
        ),
        "be": (
            "Праграма Workout ад Apple не прымае вагу ці паўторы з іншых праграм. "
            "OmniGart экспартуе кожны падыход як працу, а адпачынак як аднаўленне; "
            "вагу × паўторы запісвайце ў OmniGart на iPhone або Apple Watch."
        ),
        "kk": (
            "Apple Workout басқа қолданбалардан салмақ немесе қайталауды қабылдамайды. "
            "OmniGart әр жиынды жұмыс, демалысты қалпына келтіру қадамы ретінде экспорттайды; "
            "салмақ × қайталауды OmniGart-та iPhone немесе Apple Watch-та жазыңыз."
        ),
        "ka": (
            "Apple-ის Workout აპი სხვა აპებიდან წონას ან გამეორებებს არ იღებს. "
            "OmniGart თითოეულ სეტს სამუშაო, დასვენებას კი აღდგენის ნაბიჯად აქცევს; "
            "დატვირთვა × გამეორებები ჩაწერეთ OmniGart-ში iPhone-ზე ან Apple Watch-ზე."
        ),
        "es": (
            "La app Workout de Apple no acepta peso ni repeticiones de otras apps. "
            "OmniGart exporta cada serie como trabajo y cada descanso como recuperación; "
            "registra carga × reps en OmniGart en el iPhone o el Apple Watch."
        ),
        "fr": (
            "L’app Workout d’Apple n’accepte pas le poids ni les répétitions d’autres apps. "
            "OmniGart exporte chaque série en travail et chaque repos en récupération ; "
            "enregistrez charge × reps dans OmniGart sur iPhone ou Apple Watch."
        ),
        "de": (
            "Apples Workout-App übernimmt keine Gewichte oder Wiederholungen von anderen Apps. "
            "OmniGart exportiert jeden Satz als Arbeit und jede Pause als Erholung; "
            "Last × Wdh. in OmniGart auf iPhone oder Apple Watch erfassen."
        ),
        "cs": (
            "Apple Workout nepřijímá váhu ani opakování z jiných aplikací. "
            "OmniGart exportuje každou sérii jako práci a odpočinek jako regeneraci; "
            "zátěž × opakování zapisujte v OmniGart na iPhonu nebo Apple Watch."
        ),
        "zh-Hans": (
            "Apple 体能训练无法从其他应用接收重量或次数。"
            "OmniGart 会将每组导出为工作步骤、休息导出为恢复步骤以便跟随；"
            "请在 iPhone 或 Apple Watch 的 OmniGart 中记录重量×次数。"
        ),
    },
)
t(
    "workout_summary_energy_estimate",
    **{
        "en-GB": "Energy is a wellness estimate (heart rate or work-based), not a medical measurement.",
        "uk": "Енергія — оцінка самопочуття (пульс або робота), не медичний вимір.",
        "pl": "Energia to szacunek wellness (tętno lub praca), nie pomiar medyczny.",
        "be": "Энергія — ацэнка самаадчування (пульс або праца), не медыцынскае вымярэнне.",
        "kk": "Энергия — сауықтыру бағалауы (жүрек соғысы немесе жұмыс), медициналық өлшем емес.",
        "ka": "ენერგია კეთილდღეობის შეფასებაა (პულსი ან სამუშაო), არა სამედიცინო გაზომვა.",
        "es": "La energía es una estimación de bienestar (pulsaciones o trabajo), no una medida médica.",
        "fr": "L’énergie est une estimation bien-être (fréquence ou travail), pas une mesure médicale.",
        "de": "Energie ist eine Wellness-Schätzung (Herzfrequenz oder Arbeit), keine medizinische Messung.",
        "cs": "Energie je wellness odhad (tep nebo práce), ne lékařské měření.",
        "zh-Hans": "能量为健康估算（心率或做功），并非医学测量。",
    },
)
t(
    "workout_sets_count",
    **{
        "en-GB": "Sets",
        "uk": "Підходи",
        "pl": "Serie",
        "be": "Падыходы",
        "kk": "Жиындар",
        "ka": "სეტები",
        "es": "Series",
        "fr": "Séries",
        "de": "Sätze",
        "cs": "Série",
        "zh-Hans": "组数",
    },
)
t(
    "workout_volume_short",
    **{
        "en-GB": "Volume",
        "uk": "Об’єм",
        "pl": "Objętość",
        "be": "Аб’ём",
        "kk": "Көлем",
        "ka": "მოცულობა",
        "es": "Volumen",
        "fr": "Volume",
        "de": "Volumen",
        "cs": "Objem",
        "zh-Hans": "容量",
    },
)
t(
    "wear_strength_title",
    **{
        "en-GB": "Strength",
        "uk": "Сила",
        "pl": "Siła",
        "be": "Сіла",
        "kk": "Күш",
        "ka": "ძალა",
        "es": "Fuerza",
        "fr": "Force",
        "de": "Kraft",
        "cs": "Síla",
        "zh-Hans": "力量",
    },
)
t(
    "wear_strength_done",
    **{
        "en-GB": "Done",
        "uk": "Готово",
        "pl": "Gotowe",
        "be": "Гатова",
        "kk": "Дайын",
        "ka": "მზადაა",
        "es": "Listo",
        "fr": "Fait",
        "de": "Fertig",
        "cs": "Hotovo",
        "zh-Hans": "完成",
    },
)
t(
    "wear_strength_start",
    **{
        "en-GB": "Strength",
        "uk": "Сила",
        "pl": "Siła",
        "be": "Сіла",
        "kk": "Күш",
        "ka": "ძალა",
        "es": "Fuerza",
        "fr": "Force",
        "de": "Kraft",
        "cs": "Síla",
        "zh-Hans": "力量",
    },
)
t(
    "wear_strength_stop",
    **{
        "en-GB": "End",
        "uk": "Кінець",
        "pl": "Koniec",
        "be": "Канец",
        "kk": "Аяқтау",
        "ka": "დასრულება",
        "es": "Fin",
        "fr": "Fin",
        "de": "Ende",
        "cs": "Konec",
        "zh-Hans": "结束",
    },
)
t(
    "wear_strength_rest",
    **{
        "en-GB": "Rest %1$ds",
        "uk": "Відпочинок %1$d с",
        "pl": "Odpoczynek %1$d s",
        "be": "Адпачынак %1$d с",
        "kk": "Демалыс %1$d с",
        "ka": "დასვენება %1$d წმ",
        "es": "Descanso %1$d s",
        "fr": "Repos %1$d s",
        "de": "Pause %1$d s",
        "cs": "Odpočinek %1$d s",
        "zh-Hans": "休息 %1$d 秒",
    },
)
t(
    "wear_strength_weight",
    **{
        "en-GB": "kg",
        "uk": "кг",
        "pl": "kg",
        "be": "кг",
        "kk": "кг",
        "ka": "კგ",
        "es": "kg",
        "fr": "kg",
        "de": "kg",
        "cs": "kg",
        "zh-Hans": "公斤",
    },
)
t(
    "wear_strength_reps",
    **{
        "en-GB": "Reps",
        "uk": "Повтори",
        "pl": "Powtórzenia",
        "be": "Паўторы",
        "kk": "Қайталау",
        "ka": "გამეორება",
        "es": "Reps",
        "fr": "Répétitions",
        "de": "Wdh.",
        "cs": "Opakování",
        "zh-Hans": "次数",
    },
)
t(
    "wear_strength_set",
    **{
        "en-GB": "Set %1$d",
        "uk": "Підхід %1$d",
        "pl": "Seria %1$d",
        "be": "Падыход %1$d",
        "kk": "Жақындау %1$d",
        "ka": "მიდგომა %1$d",
        "es": "Serie %1$d",
        "fr": "Série %1$d",
        "de": "Satz %1$d",
        "cs": "Série %1$d",
        "zh-Hans": "第 %1$d 组",
    },
)
t(
    "wear_strength_hr",
    **{
        "en-GB": "HR %1$d",
        "uk": "ЧСС %1$d",
        "pl": "HR %1$d",
        "be": "ЧСС %1$d",
        "kk": "ЖСЖ %1$d",
        "ka": "გსს %1$d",
        "es": "FC %1$d",
        "fr": "FC %1$d",
        "de": "HF %1$d",
        "cs": "TF %1$d",
        "zh-Hans": "心率 %1$d",
    },
)
t(
    "wear_strength_kcal",
    **{
        "en-GB": "%1$s kcal",
        "uk": "%1$s ккал",
        "pl": "%1$s kcal",
        "be": "%1$s ккал",
        "kk": "%1$s ккал",
        "ka": "%1$s კკალ",
        "es": "%1$s kcal",
        "fr": "%1$s kcal",
        "de": "%1$s kcal",
        "cs": "%1$s kcal",
        "zh-Hans": "%1$s 千卡",
    },
)
t(
    "watch.strength.done",
    **{
        "en-GB": "Done",
        "uk": "Готово",
        "pl": "Gotowe",
        "be": "Гатова",
        "kk": "Дайын",
        "ka": "მზადაა",
        "es": "Listo",
        "fr": "Fait",
        "de": "Fertig",
        "cs": "Hotovo",
        "zh-Hans": "完成",
    },
)
t(
    "watch.strength.rest",
    **{
        "en-GB": "Rest",
        "uk": "Відпочинок",
        "pl": "Odpoczynek",
        "be": "Адпачынак",
        "kk": "Демалыс",
        "ka": "დასვენება",
        "es": "Descanso",
        "fr": "Repos",
        "de": "Pause",
        "cs": "Odpočinek",
        "zh-Hans": "休息",
    },
)
t(
    "watch.strength.weight",
    **{
        "en-GB": "kg",
        "uk": "кг",
        "pl": "kg",
        "be": "кг",
        "kk": "кг",
        "ka": "კგ",
        "es": "kg",
        "fr": "kg",
        "de": "kg",
        "cs": "kg",
        "zh-Hans": "公斤",
    },
)
t(
    "watch.strength.reps",
    **{
        "en-GB": "Reps",
        "uk": "Повтори",
        "pl": "Powtórzenia",
        "be": "Паўторы",
        "kk": "Қайталау",
        "ka": "გამეორება",
        "es": "Reps",
        "fr": "Répétitions",
        "de": "Wdh.",
        "cs": "Opakování",
        "zh-Hans": "次数",
    },
)
t(
    "watch.strength.set",
    **{
        "en-GB": "Set",
        "uk": "Підхід",
        "pl": "Seria",
        "be": "Падыход",
        "kk": "Жақындау",
        "ka": "მიდგომა",
        "es": "Serie",
        "fr": "Série",
        "de": "Satz",
        "cs": "Série",
        "zh-Hans": "组",
    },
)
t(
    "watch.strength.target",
    **{
        "en-GB": "Target",
        "uk": "Ціль",
        "pl": "Cel",
        "be": "Мэта",
        "kk": "Мақсат",
        "ka": "მიზანი",
        "es": "Objetivo",
        "fr": "Objectif",
        "de": "Ziel",
        "cs": "Cíl",
        "zh-Hans": "目标",
    },
)
t(
    "watch.strength.sets",
    **{
        "en-GB": "Sets",
        "uk": "Підходи",
        "pl": "Serie",
        "be": "Падыходы",
        "kk": "Жақындаулар",
        "ka": "მიდგომები",
        "es": "Series",
        "fr": "Séries",
        "de": "Sätze",
        "cs": "Série",
        "zh-Hans": "组数",
    },
)


def escape_android(value: str) -> str:
    return (
        value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', '\\"')
        .replace("'", "\\'")
        .replace("\n", "\\n")
    )


def patch_android_strings(path: Path, locale: str, folder_is_default: bool) -> None:
    if not path.exists():
        return
    text = path.read_text(encoding="utf-8")
    for key, locales in STRINGS.items():
        if key.startswith("watch."):
            continue
        value = escape_android(locales["en-GB"] if folder_is_default else locales[locale])
        pattern = re.compile(rf'<string name="{re.escape(key)}">.*?</string>', re.DOTALL)
        replacement = f'<string name="{key}">{value}</string>'
        if pattern.search(text):
            text = pattern.sub(replacement, text)
        else:
            text = text.replace("</resources>", f"    {replacement}\n</resources>")
    path.write_text(text, encoding="utf-8")


def main() -> None:
    for locale in LOCALES:
        path = ROOT / "shared/src/commonMain/resources/i18n" / locale / "strings.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        for key, locales in STRINGS.items():
            data[key] = locales[locale]
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

        folders = [ANDROID[locale]]
        if locale == "en-GB":
            folders.append("values")
        for folder in folders:
            default = folder == "values"
            patch_android_strings(
                ROOT / "androidApp/src/main/res" / folder / "strings.xml", locale, default
            )
            patch_android_strings(
                ROOT / "wearApp/src/main/res" / folder / "strings.xml", locale, default
            )

    xc_path = ROOT / "iosApp/OmniGart/Localizable.xcstrings"
    catalog = json.loads(xc_path.read_text(encoding="utf-8"))
    strings = catalog.setdefault("strings", {})
    for key, locales in STRINGS.items():
        entry = strings.setdefault(key, {"extractionState": "manual", "localizations": {}})
        locs = entry.setdefault("localizations", {})
        for locale, value in locales.items():
            locs[locale] = {"stringUnit": {"state": "translated", "value": value}}
    xc_path.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    watch_xc = ROOT / "watchApp/OmniGartWatch/Localizable.xcstrings"
    if watch_xc.exists():
        wcat = json.loads(watch_xc.read_text(encoding="utf-8"))
        wstrings = wcat.setdefault("strings", {})
        for key, locales in STRINGS.items():
            if not key.startswith("watch.") and not key.startswith("wear_"):
                continue
            entry = wstrings.setdefault(key, {"extractionState": "manual", "localizations": {}})
            locs = entry.setdefault("localizations", {})
            for locale, value in locales.items():
                locs[locale] = {"stringUnit": {"state": "translated", "value": value}}
        watch_xc.write_text(json.dumps(wcat, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(f"Synced {len(STRINGS)} in-workout keys × {len(LOCALES)} locales")


if __name__ == "__main__":
    main()
