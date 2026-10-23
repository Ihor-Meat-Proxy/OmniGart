#!/usr/bin/env python3
"""Sync Health workout / energy UI strings (11 locales) into JSON, xcstrings, Android."""

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

STRINGS: dict[str, dict[str, str]] = {}


def t(key: str, **by_locale: str) -> None:
    assert "en-GB" in by_locale
    for loc in LOCALES:
        assert loc in by_locale, f"{key} missing {loc}"
    STRINGS[key] = {loc: by_locale[loc] for loc in LOCALES}


t(
    "perm_explainer_health_body",
    **{
        "en-GB": (
            "OmniGart can read and write wellness samples — heart rate, HRV, sleep, workouts, "
            "active and resting energy, heart-rate recovery, workout effort, optional mindful "
            "minutes, body mass, and hydration — so sessions can appear in Apple Fitness with "
            "duration and calories. Data is not used for ads or sent to unrelated third parties. "
            "You can continue without granting access."
        ),
        "uk": (
            "OmniGart може читати й записувати зразки самопочуття — пульс, ВСР, сон, тренування, "
            "активну й базальну енергію, відновлення пульсу, оцінку зусилля, за бажанням хвилини "
            "усвідомленості, масу тіла та гідратацію — щоб сесії з’являлися в Apple Fitness. "
            "Дані не для реклами й не передаються стороннім. Можна продовжити без доступу."
        ),
        "pl": (
            "OmniGart może czytać i zapisywać próbki wellness — tętno, HRV, sen, treningi, "
            "energię aktywną i spoczynkową, masę ciała i nawodnienie — aby sesje pojawiały się "
            "w Apple Fitness z czasem i kaloriami. Dane nie służą reklamom ani stronom trzecim. "
            "Możesz kontynuować bez dostępu."
        ),
        "be": (
            "OmniGart можа чытаць і запісваць узоры самаадчування — пульс, ВСР, сон, трэніроўкі, "
            "актыўную і базальную энергію, масу цела і гідратацыю — каб сесіі з’яўляліся ў Apple Fitness "
            "з працягласцю і калорыямі. Дадзеныя не для рэкламы і не перадаюцца трэцім бакам. "
            "Можна працягнуць без доступу."
        ),
        "kk": (
            "OmniGart сауықтыру үлгілерін оқи және жаза алады — жүрек соғысы, HRV, ұйқы, жаттығулар, "
            "белсенді және базалық энергия, дене салмағы және су ішу — сеанстар Apple Fitness-те "
            "ұзақтық пен калориямен көрінуі үшін. Деректер жарнамаға немесе үшінші тарапқа берілмейді. "
            "Рұқсатсыз жалғастыруға болады."
        ),
        "ka": (
            "OmniGart-ს შეუძლია წაიკითხოს და ჩაწეროს კეთილდღეობის ნიმუშები — გულისცემა, HRV, ძილი, "
            "ვარჯიშები, აქტიური და საბაზისო ენერგია, სხეულის მასა და ჰიდრატაცია — რომ სესიები "
            "გამოჩნდეს Apple Fitness-ში ხანგრძლივობით და კალორიებით. მონაცემები არ გამოიყენება "
            "რეკლამისთვის. შეგიძლიათ გააგრძელოთ წვდომის გარეშე."
        ),
        "es": (
            "OmniGart puede leer y escribir muestras de bienestar — frecuencia cardiaca, VFC, sueño, "
            "entrenamientos, energía activa y en reposo, masa corporal e hidratación — para que las "
            "sesiones aparezcan en Apple Fitness con duración y calorías. Los datos no se usan para "
            "publicidad. Puedes continuar sin conceder acceso."
        ),
        "fr": (
            "OmniGart peut lire et écrire des échantillons bien-être — fréquence cardiaque, VRC, sommeil, "
            "séances, énergie active et basale, masse corporelle et hydratation — pour afficher les "
            "séances dans Apple Fitness avec durée et calories. Les données ne servent pas à la pub. "
            "Vous pouvez continuer sans accorder l’accès."
        ),
        "de": (
            "OmniGart kann Wellness-Proben lesen und schreiben — Herzfrequenz, HRV, Schlaf, Workouts, "
            "aktive und Ruheenergie, Körpermasse und Hydration — damit Sitzungen in Apple Fitness mit "
            "Dauer und Kalorien erscheinen. Daten werden nicht für Werbung genutzt. Sie können ohne "
            "Zugriff fortfahren."
        ),
        "cs": (
            "OmniGart může číst a zapisovat wellness vzorky — tep, HRV, spánek, tréninky, aktivní a "
            "bazální energii, tělesnou hmotnost a hydrataci — aby se sezení zobrazila v Apple Fitness "
            "s délkou a kaloriemi. Data se nepoužívají k reklamě. Můžete pokračovat bez přístupu."
        ),
        "zh-Hans": (
            "OmniGart 可读写健康样本——心率、心率变异、睡眠、训练、活动与基础能量、体重和补水——"
            "以便会话以时长和热量出现在 Apple Fitness 中。数据不用于广告，也不发送给无关第三方。"
            "您也可不授予权限并继续。"
        ),
    },
)

t(
    "workout_summary_duration",
    **{
        "en-GB": "Duration",
        "uk": "Тривалість",
        "pl": "Czas trwania",
        "be": "Працягласць",
        "kk": "Ұзақтық",
        "ka": "ხანგრძლივობა",
        "es": "Duración",
        "fr": "Durée",
        "de": "Dauer",
        "cs": "Trvání",
        "zh-Hans": "时长",
    },
)
t(
    "workout_summary_active_kcal",
    **{
        "en-GB": "Active kcal",
        "uk": "Активні ккал",
        "pl": "Aktywne kcal",
        "be": "Актыўныя ккал",
        "kk": "Белсенді ккал",
        "ka": "აქტიური კკალ",
        "es": "Kcal activas",
        "fr": "Kcal actives",
        "de": "Aktive kcal",
        "cs": "Aktivní kcal",
        "zh-Hans": "活动千卡",
    },
)
t(
    "workout_summary_total_kcal",
    **{
        "en-GB": "Total kcal",
        "uk": "Усього ккал",
        "pl": "Suma kcal",
        "be": "Усяго ккал",
        "kk": "Барлық ккал",
        "ka": "სულ კკალ",
        "es": "Kcal totales",
        "fr": "Kcal totales",
        "de": "Gesamt-kcal",
        "cs": "Celkem kcal",
        "zh-Hans": "总千卡",
    },
)
t(
    "workout_summary_avg_hr",
    **{
        "en-GB": "Average heart rate",
        "uk": "Середній пульс",
        "pl": "Średnie tętno",
        "be": "Сярэдні пульс",
        "kk": "Орташа жүрек соғысы",
        "ka": "საშუალო გულისცემა",
        "es": "Frecuencia media",
        "fr": "Fréquence moyenne",
        "de": "Mittlere Herzfrequenz",
        "cs": "Průměrný tep",
        "zh-Hans": "平均心率",
    },
)
t(
    "workout_summary_max_hr",
    **{
        "en-GB": "Max heart rate",
        "uk": "Максимальний пульс",
        "pl": "Maks. tętno",
        "be": "Максімальны пульс",
        "kk": "Макс. жүрек соғысы",
        "ka": "მაქს. გულისცემა",
        "es": "Frecuencia máx.",
        "fr": "Fréquence max.",
        "de": "Max. Herzfrequenz",
        "cs": "Max. tep",
        "zh-Hans": "最大心率",
    },
)
t(
    "workout_summary_energy_estimate",
    **{
        "en-GB": "Energy is a wellness estimate (MET × body weight).",
        "uk": "Енергія — оцінка самопочуття (MET × маса тіла).",
        "pl": "Energia to szacunek wellness (MET × masa ciała).",
        "be": "Энергія — ацэнка самаадчування (MET × маса цела).",
        "kk": "Энергия — сауықтыру бағалауы (MET × дене салмағы).",
        "ka": "ენერგია კეთილდღეობის შეფასებაა (MET × სხეულის მასა).",
        "es": "La energía es una estimación de bienestar (MET × peso).",
        "fr": "L’énergie est une estimation bien-être (MET × masse).",
        "de": "Energie ist eine Wellness-Schätzung (MET × Körpermasse).",
        "cs": "Energie je wellness odhad (MET × hmotnost).",
        "zh-Hans": "能量为健康估算（MET × 体重）。",
    },
)
t(
    "workout_summary_effort",
    **{
        "en-GB": "Effort score",
        "uk": "Оцінка зусилля",
        "pl": "Wskaźnik wysiłku",
        "be": "Ацэнка намаганняў",
        "kk": "Күш-жігер бағасы",
        "ka": "ძალისხმევის ქულა",
        "es": "Esfuerzo",
        "fr": "Score d’effort",
        "de": "Anstrengungswert",
        "cs": "Skóre úsilí",
        "zh-Hans": "努力评分",
    },
)
t(
    "workout_summary_hr_recovery",
    **{
        "en-GB": "Heart-rate recovery",
        "uk": "Відновлення пульсу",
        "pl": "Regeneracja tętna",
        "be": "Аднаўленне пульсу",
        "kk": "Жүрек соғысының қалпына келуі",
        "ka": "გულისცემის აღდგენა",
        "es": "Recuperación cardiaca",
        "fr": "Récupération cardiaque",
        "de": "HF-Erholung",
        "cs": "Regenerace tepu",
        "zh-Hans": "心率恢复",
    },
)
t(
    "workout_start_on_watch",
    **{
        "en-GB": "Start on Apple Watch",
        "uk": "Почати на Apple Watch",
        "pl": "Start na Apple Watch",
        "be": "Пачаць на Apple Watch",
        "kk": "Apple Watch-та бастау",
        "ka": "დაწყება Apple Watch-ზე",
        "es": "Empezar en Apple Watch",
        "fr": "Démarrer sur Apple Watch",
        "de": "Auf Apple Watch starten",
        "cs": "Spustit na Apple Watch",
        "zh-Hans": "在 Apple Watch 上开始",
    },
)
t(
    "workout_watch_started",
    **{
        "en-GB": "Watch workout starting…",
        "uk": "Запуск тренування на годиннику…",
        "pl": "Uruchamianie treningu na zegarku…",
        "be": "Запуск трэніроўкі на гадзінніку…",
        "kk": "Сағатта жаттығу басталуда…",
        "ka": "საათზე ვარჯიში იწყება…",
        "es": "Iniciando entrenamiento en el reloj…",
        "fr": "Démarrage de la séance sur la montre…",
        "de": "Watch-Workout startet…",
        "cs": "Spouštění tréninku na hodinkách…",
        "zh-Hans": "正在手表上开始训练…",
    },
)
t(
    "workout_watch_unavailable",
    **{
        "en-GB": "Watch unavailable — use iPhone estimate below.",
        "uk": "Годинник недоступний — використайте оцінку на iPhone.",
        "pl": "Zegarek niedostępny — użyj szacunku na iPhonie.",
        "be": "Гадзіннік недаступны — выкарыстайце ацэнку на iPhone.",
        "kk": "Сағат қолжетімсіз — төмендегі iPhone бағалауын пайдаланыңыз.",
        "ka": "საათი მიუწვდომელია — გამოიყენეთ iPhone შეფასება.",
        "es": "Reloj no disponible — usa la estimación del iPhone.",
        "fr": "Montre indisponible — utilisez l’estimation iPhone.",
        "de": "Watch nicht verfügbar — iPhone-Schätzung nutzen.",
        "cs": "Hodinky nedostupné — použijte odhad na iPhonu.",
        "zh-Hans": "手表不可用 — 请使用下方 iPhone 估算。",
    },
)
t(
    "workout_start_iphone_estimate",
    **{
        "en-GB": "Log on iPhone (estimate)",
        "uk": "Запис на iPhone (оцінка)",
        "pl": "Zapis na iPhonie (szacunek)",
        "be": "Запіс на iPhone (ацэнка)",
        "kk": "iPhone-да жазу (бағалау)",
        "ka": "ჩაწერა iPhone-ზე (შეფასება)",
        "es": "Registrar en iPhone (estimación)",
        "fr": "Journal iPhone (estimation)",
        "de": "Auf iPhone protokollieren (Schätzung)",
        "cs": "Záznam na iPhonu (odhad)",
        "zh-Hans": "在 iPhone 上记录（估算）",
    },
)
t(
    "workout_yoga_title",
    **{
        "en-GB": "Yoga",
        "uk": "Йога",
        "pl": "Joga",
        "be": "Йога",
        "kk": "Йога",
        "ka": "იოგა",
        "es": "Yoga",
        "fr": "Yoga",
        "de": "Yoga",
        "cs": "Jóga",
        "zh-Hans": "瑜伽",
    },
)
t(
    "workout_pilates_title",
    **{
        "en-GB": "Pilates",
        "uk": "Пілатес",
        "pl": "Pilates",
        "be": "Пілатэс",
        "kk": "Пилатес",
        "ka": "პილატესი",
        "es": "Pilates",
        "fr": "Pilates",
        "de": "Pilates",
        "cs": "Pilates",
        "zh-Hans": "普拉提",
    },
)
t(
    "workout_mindful_body_intro",
    **{
        "en-GB": (
            "Prefer a live watch session for heart rate and energy. On iPhone alone, OmniGart "
            "saves a MET wellness estimate labelled as such."
        ),
        "uk": (
            "Краще жива сесія на годиннику для пульсу й енергії. Лише на iPhone OmniGart "
            "зберігає оцінку MET із відповідною позначкою."
        ),
        "pl": (
            "Preferuj żywą sesję na zegarku dla tętna i energii. Na samym iPhonie OmniGart "
            "zapisuje szacunek MET z etykietą."
        ),
        "be": (
            "Лепш жывая сесія на гадзінніку для пульсу й энергіі. Толькі на iPhone OmniGart "
            "захоўвае ацэнку MET з пазнакай."
        ),
        "kk": (
            "Жүрек соғысы мен энергия үшін сағаттағы тірі сеансты таңдаңыз. Тек iPhone-да "
            "OmniGart MET бағалауын белгілеп сақтайды."
        ),
        "ka": (
            "გულისცემისა და ენერგიისთვის უპირატესობა მიანიჭეთ საათის ცოცხალ სესიას. მხოლოდ "
            "iPhone-ზე OmniGart ინახავს MET შეფასებას შესაბამისი ნიშნით."
        ),
        "es": (
            "Prefiere una sesión en vivo en el reloj para frecuencia y energía. Solo en iPhone, "
            "OmniGart guarda una estimación MET etiquetada."
        ),
        "fr": (
            "Préférez une séance live sur la montre pour le pouls et l’énergie. Sur iPhone seul, "
            "OmniGart enregistre une estimation MET étiquetée."
        ),
        "de": (
            "Bevorzugen Sie eine Live-Session auf der Watch für Puls und Energie. Nur auf dem "
            "iPhone speichert OmniGart eine gekennzeichnete MET-Schätzung."
        ),
        "cs": (
            "Preferujte živou relaci na hodinkách pro tep a energii. Jen na iPhonu OmniGart "
            "uloží označený odhad MET."
        ),
        "zh-Hans": (
            "建议用手表实时会话采集心率与能量。仅在 iPhone 上时，OmniGart 会保存带标注的 MET 健康估算。"
        ),
    },
)
t(
    "workout_mindful_minutes_toggle",
    **{
        "en-GB": "Save mindful minutes for yoga",
        "uk": "Зберігати хвилини усвідомленості для йоги",
        "pl": "Zapisuj minuty uważności dla jogi",
        "be": "Захоўваць хвіліны ўсвядомленасці для йогі",
        "kk": "Йога үшін mindful минуттарды сақтау",
        "ka": "იოგისთვის mindful წუთების შენახვა",
        "es": "Guardar minutos mindful en yoga",
        "fr": "Enregistrer les minutes mindful pour le yoga",
        "de": "Achtsamkeitsminuten für Yoga speichern",
        "cs": "Ukládat mindful minuty pro jógu",
        "zh-Hans": "为瑜伽保存正念分钟",
    },
)
t(
    "workout_mindful_minutes_body",
    **{
        "en-GB": "Optional. Off by default. Writes a mindful session only when you enable this.",
        "uk": "За бажанням. Типово вимкнено. Записує mindful-сесію лише коли ви це вмикаєте.",
        "pl": "Opcjonalne. Domyślnie wyłączone. Zapisuje sesję uważności tylko po włączeniu.",
        "be": "Па жаданні. Па змаўчанні выключана. Запісвае mindful-сесію толькі калі ўключана.",
        "kk": "Қосымша. Әдепкі өшірулі. Тек қосқанда mindful сеансын жазады.",
        "ka": "სურვილისამებრ. ნაგულისხმევად გამორთულია. იწერს მხოლოდ ჩართვისას.",
        "es": "Opcional. Desactivado por defecto. Solo escribe si lo activas.",
        "fr": "Optionnel. Désactivé par défaut. N’écrit que si vous l’activez.",
        "de": "Optional. Standardmäßig aus. Schreibt nur, wenn Sie es aktivieren.",
        "cs": "Volitelné. Ve výchozím stavu vypnuto. Zapisuje jen po zapnutí.",
        "zh-Hans": "可选。默认关闭。仅在启用时写入正念会话。",
    },
)
t(
    "workout_mindful_settings_title",
    **{
        "en-GB": "Yoga mindful minutes",
        "uk": "Усвідомленість у йозі",
        "pl": "Uważność w jodze",
        "be": "Усвядомленасць у йозе",
        "kk": "Йога mindful минуттары",
        "ka": "იოგის mindful წუთები",
        "es": "Minutos mindful de yoga",
        "fr": "Minutes mindful du yoga",
        "de": "Yoga-Achtsamkeitsminuten",
        "cs": "Mindful minuty jógy",
        "zh-Hans": "瑜伽正念分钟",
    },
)

# Watch Zone 2 energy / summary labels (String Catalog / Text keys).
t(
    "watch.zone2.active_kcal",
    **{
        "en-GB": "Active kcal",
        "uk": "Активні ккал",
        "pl": "Aktywne kcal",
        "be": "Актыўныя ккал",
        "kk": "Белсенді ккал",
        "ka": "აქტიური კკალ",
        "es": "Kcal activas",
        "fr": "Kcal actives",
        "de": "Aktive kcal",
        "cs": "Aktivní kcal",
        "zh-Hans": "活动千卡",
    },
)
t(
    "watch.zone2.summary.duration",
    **{
        "en-GB": "Duration",
        "uk": "Тривалість",
        "pl": "Czas",
        "be": "Працягласць",
        "kk": "Ұзақтық",
        "ka": "ხანგრძლივობა",
        "es": "Duración",
        "fr": "Durée",
        "de": "Dauer",
        "cs": "Trvání",
        "zh-Hans": "时长",
    },
)
t(
    "watch.zone2.summary.active_kcal",
    **{
        "en-GB": "Active kcal",
        "uk": "Активні ккал",
        "pl": "Aktywne kcal",
        "be": "Актыўныя ккал",
        "kk": "Белсенді ккал",
        "ka": "აქტიური კკალ",
        "es": "Kcal activas",
        "fr": "Kcal actives",
        "de": "Aktive kcal",
        "cs": "Aktivní kcal",
        "zh-Hans": "活动千卡",
    },
)
t(
    "watch.zone2.summary.total_kcal",
    **{
        "en-GB": "Total kcal",
        "uk": "Усього ккал",
        "pl": "Suma kcal",
        "be": "Усяго ккал",
        "kk": "Барлық ккал",
        "ka": "სულ კკალ",
        "es": "Kcal totales",
        "fr": "Kcal totales",
        "de": "Gesamt-kcal",
        "cs": "Celkem kcal",
        "zh-Hans": "总千卡",
    },
)
t(
    "watch.zone2.summary.avg_hr",
    **{
        "en-GB": "Avg HR",
        "uk": "Сер. пульс",
        "pl": "Śr. tętno",
        "be": "Сяр. пульс",
        "kk": "Орт. ЖС",
        "ka": "საშ. გც",
        "es": "FC media",
        "fr": "FC moy.",
        "de": "Ø HF",
        "cs": "Prům. tep",
        "zh-Hans": "平均心率",
    },
)
t(
    "watch.zone2.summary.max_hr",
    **{
        "en-GB": "Max HR",
        "uk": "Макс. пульс",
        "pl": "Maks. tętno",
        "be": "Макс. пульс",
        "kk": "Макс. ЖС",
        "ka": "მაქს. გც",
        "es": "FC máx.",
        "fr": "FC max.",
        "de": "Max. HF",
        "cs": "Max. tep",
        "zh-Hans": "最大心率",
    },
)
t(
    "watch.workout.strength_traditional",
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
    "watch.workout.strength_functional",
    **{
        "en-GB": "Functional strength",
        "uk": "Функціональна сила",
        "pl": "Siła funkcjonalna",
        "be": "Функцыянальная сіла",
        "kk": "Функционалдық күш",
        "ka": "ფუნქციური ძალა",
        "es": "Fuerza funcional",
        "fr": "Force fonctionnelle",
        "de": "Funktionelle Kraft",
        "cs": "Funkční síla",
        "zh-Hans": "功能性力量",
    },
)
t(
    "watch.workout.yoga",
    **{
        "en-GB": "Yoga",
        "uk": "Йога",
        "pl": "Joga",
        "be": "Йога",
        "kk": "Йога",
        "ka": "იოგა",
        "es": "Yoga",
        "fr": "Yoga",
        "de": "Yoga",
        "cs": "Jóga",
        "zh-Hans": "瑜伽",
    },
)
t(
    "watch.workout.pilates",
    **{
        "en-GB": "Pilates",
        "uk": "Пілатес",
        "pl": "Pilates",
        "be": "Пілатэс",
        "kk": "Пилатес",
        "ka": "პილატესი",
        "es": "Pilates",
        "fr": "Pilates",
        "de": "Pilates",
        "cs": "Pilates",
        "zh-Hans": "普拉提",
    },
)
t(
    "watch.workout.pause",
    **{
        "en-GB": "Pause",
        "uk": "Пауза",
        "pl": "Pauza",
        "be": "Паўза",
        "kk": "Кідірту",
        "ka": "პაუზა",
        "es": "Pausa",
        "fr": "Pause",
        "de": "Pause",
        "cs": "Pauza",
        "zh-Hans": "暂停",
    },
)
t(
    "watch.workout.resume",
    **{
        "en-GB": "Resume",
        "uk": "Продовжити",
        "pl": "Wznów",
        "be": "Працягнуць",
        "kk": "Жалғастыру",
        "ka": "გაგრძელება",
        "es": "Reanudar",
        "fr": "Reprendre",
        "de": "Fortsetzen",
        "cs": "Pokračovat",
        "zh-Hans": "继续",
    },
)
t(
    "watch.workout.segment",
    **{
        "en-GB": "Segment",
        "uk": "Сегмент",
        "pl": "Segment",
        "be": "Сегмент",
        "kk": "Сегмент",
        "ka": "სეგმენტი",
        "es": "Segmento",
        "fr": "Segment",
        "de": "Segment",
        "cs": "Segment",
        "zh-Hans": "分段",
    },
)
t(
    "watch.workout.segments",
    **{
        "en-GB": "Segments",
        "uk": "Сегменти",
        "pl": "Segmenty",
        "be": "Сегменты",
        "kk": "Сегменттер",
        "ka": "სეგმენტები",
        "es": "Segmentos",
        "fr": "Segments",
        "de": "Segmente",
        "cs": "Segmenty",
        "zh-Hans": "分段数",
    },
)
t(
    "watch.workout.stop",
    **{
        "en-GB": "End",
        "uk": "Завершити",
        "pl": "Zakończ",
        "be": "Завяршыць",
        "kk": "Аяқтау",
        "ka": "დასრულება",
        "es": "Terminar",
        "fr": "Terminer",
        "de": "Beenden",
        "cs": "Ukončit",
        "zh-Hans": "结束",
    },
)
t(
    "watch.workout.duration",
    **{
        "en-GB": "Duration",
        "uk": "Тривалість",
        "pl": "Czas",
        "be": "Працягласць",
        "kk": "Ұзақтық",
        "ka": "ხანგრძლივობა",
        "es": "Duración",
        "fr": "Durée",
        "de": "Dauer",
        "cs": "Trvání",
        "zh-Hans": "时长",
    },
)
t(
    "watch.workout.summary.wellness",
    **{
        "en-GB": "Wellness summary — coaching reflection, not medical advice.",
        "uk": "Підсумок самопочуття — тренерська рефлексія, не медична консультація.",
        "pl": "Podsumowanie wellness — refleksja coachingowa, nie porada medyczna.",
        "be": "Вынік самаадчування — трэнерская рэфлексія, не медыцынская кансультацыя.",
        "kk": "Сауықтыру қорытындысы — жаттықтырушылық ой, медициналық кеңес емес.",
        "ka": "კეთილდღეობის შეჯამება — სამწვრთნელო რეფლექსია, არა სამედიცინო რჩევა.",
        "es": "Resumen de bienestar — reflexión de coaching, no consejo médico.",
        "fr": "Résumé bien-être — réflexion coaching, pas un avis médical.",
        "de": "Wellness-Zusammenfassung — Coaching-Reflexion, keine medizinische Beratung.",
        "cs": "Wellness souhrn — trenérská reflexe, ne lékařská rada.",
        "zh-Hans": "健康摘要——教练反思，不是医疗建议。",
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
            ap = ROOT / "androidApp/src/main/res" / folder / "strings.xml"
            text = ap.read_text(encoding="utf-8")
            for key, locales in STRINGS.items():
                if key.startswith("watch."):
                    continue  # watch-only catalog keys
                value = escape_android(
                    locales["en-GB"] if folder == "values" else locales[locale]
                )
                pattern = re.compile(
                    rf'<string name="{re.escape(key)}">.*?</string>', re.DOTALL
                )
                replacement = f'<string name="{key}">{value}</string>'
                if pattern.search(text):
                    text = pattern.sub(replacement, text)
                else:
                    text = text.replace("</resources>", f"    {replacement}\n</resources>")
            ap.write_text(text, encoding="utf-8")

    xc_path = ROOT / "iosApp/OmniGart/Localizable.xcstrings"
    catalog = json.loads(xc_path.read_text(encoding="utf-8"))
    strings = catalog.setdefault("strings", {})
    for key, locales in STRINGS.items():
        entry = strings.setdefault(key, {"extractionState": "manual", "localizations": {}})
        locs = entry.setdefault("localizations", {})
        for locale, value in locales.items():
            locs[locale] = {"stringUnit": {"state": "translated", "value": value}}
    xc_path.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Synced {len(STRINGS)} health-workout keys × {len(LOCALES)} locales")


if __name__ == "__main__":
    main()
