#!/usr/bin/env python3
"""Sync Insights/Health UI strings into shared JSON, Localizable.xcstrings, and Android values*."""
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
    "tab_insights",
    **{
        "en-GB": "Insights",
        "uk": "Аналітика",
        "pl": "Analizy",
        "be": "Аналітыка",
        "kk": "Талдау",
        "ka": "ანალიტიკა",
        "es": "Análisis",
        "fr": "Analyses",
        "de": "Einblicke",
        "cs": "Přehledy",
        "zh-Hans": "洞察",
    },
)

t(
    "tab_health",
    **{
        "en-GB": "Health",
        "uk": "Здоров’я",
        "pl": "Zdrowie",
        "be": "Здароўе",
        "kk": "Денсаулық",
        "ka": "ჯანმრთელობა",
        "es": "Salud",
        "fr": "Santé",
        "de": "Gesundheit",
        "cs": "Zdraví",
        "zh-Hans": "健康",
    },
)

t(
    "insights_intro",
    **{
        "en-GB": "Training trends from your on-device workouts. Wellness estimates only — not medical advice.",
        "uk": "Тенденції тренувань із локальних занять. Лише оцінки добробуту — не медична порада.",
        "pl": "Trendy treningowe z lokalnych treningów. Tylko szacunki wellness — nie porada medyczna.",
        "be": "Тэндэнцыі трэніровак з лакальных заняткаў. Толькі ацэнкі дабрабыту — не медыцынская парада.",
        "kk": "Құрылғыдағы жаттығулардан трендтер. Тек сауықтыру бағалаулары — медициналық кеңес емес.",
        "ka": "ტენდენციები თქვენი ადგილობრივი ვარჯიშებიდან. მხოლოდ კეთილდღეობის შეფასებები — არა სამედიცინო რჩევა.",
        "es": "Tendencias de entrenamiento desde tus entrenos en el dispositivo. Solo estimaciones de bienestar — no consejo médico.",
        "fr": "Tendances d’entraînement depuis vos séances sur l’appareil. Estimations bien-être uniquement — pas un avis médical.",
        "de": "Trainingstrends aus Ihren Geräte-Workouts. Nur Wellness-Schätzungen — keine medizinische Beratung.",
        "cs": "Tréninkové trendy z lokálních tréninků. Pouze wellness odhady — ne lékařská rada.",
        "zh-Hans": "来自本机训练的趋势。仅为健康估算，不是医疗建议。",
    },
)

t(
    "insights_empty_title",
    **{
        "en-GB": "No workouts yet",
        "uk": "Поки немає тренувань",
        "pl": "Brak treningów",
        "be": "Пакуль няма трэніровак",
        "kk": "Әзірге жаттығу жоқ",
        "ka": "ჯერ ვარჯიშები არაა",
        "es": "Aún no hay entrenos",
        "fr": "Pas encore de séances",
        "de": "Noch keine Workouts",
        "cs": "Zatím žádné tréninky",
        "zh-Hans": "暂无训练",
    },
)

t(
    "insights_empty_body",
    **{
        "en-GB": "Log or import workouts to see fitness, load and trends here.",
        "uk": "Запишіть або імпортуйте тренування, щоб бачити форму, навантаження і тренди.",
        "pl": "Zapisz lub zaimportuj treningi, aby zobaczyć formę, obciążenie i trendy.",
        "be": "Запішыце або імпартуйце трэніроўкі, каб бачыць форму, нагрузку і тэндэнцыі.",
        "kk": "Форма, жүктеме және трендтерді көру үшін жаттығуларды жазыңыз немесе импорттаңыз.",
        "ka": "ჩაიწერეთ ან შემოიტანეთ ვარჯიშები ფორმის, დატვირთვისა და ტენდენციების სანახავად.",
        "es": "Registra o importa entrenos para ver forma, carga y tendencias.",
        "fr": "Enregistrez ou importez des séances pour voir forme, charge et tendances.",
        "de": "Workouts erfassen oder importieren, um Fitness, Load und Trends zu sehen.",
        "cs": "Zaznamenejte nebo importujte tréninky pro formu, zátěž a trendy.",
        "zh-Hans": "记录或导入训练后即可查看体能、负荷与趋势。",
    },
)

t(
    "insights_fitness_title",
    **{
        "en-GB": "Fitness",
        "uk": "Форма",
        "pl": "Forma",
        "be": "Форма",
        "kk": "Форма",
        "ka": "ფორმა",
        "es": "Forma",
        "fr": "Forme",
        "de": "Fitness",
        "cs": "Forma",
        "zh-Hans": "体能",
    },
)

t(
    "insights_ctl",
    **{
        "en-GB": "Fitness (CTL)",
        "uk": "Форма (CTL)",
        "pl": "Forma (CTL)",
        "be": "Форма (CTL)",
        "kk": "Форма (CTL)",
        "ka": "ფორმა (CTL)",
        "es": "Forma (CTL)",
        "fr": "Forme (CTL)",
        "de": "Fitness (CTL)",
        "cs": "Forma (CTL)",
        "zh-Hans": "体能 (CTL)",
    },
)

t(
    "insights_atl",
    **{
        "en-GB": "Fatigue (ATL)",
        "uk": "Втома (ATL)",
        "pl": "Zmęczenie (ATL)",
        "be": "Стомленасць (ATL)",
        "kk": "Шаршау (ATL)",
        "ka": "დაღლილობა (ATL)",
        "es": "Fatiga (ATL)",
        "fr": "Fatigue (ATL)",
        "de": "Ermüdung (ATL)",
        "cs": "Únava (ATL)",
        "zh-Hans": "疲劳 (ATL)",
    },
)

t(
    "insights_tsb",
    **{
        "en-GB": "Form (TSB)",
        "uk": "Свіжість (TSB)",
        "pl": "Świeżość (TSB)",
        "be": "Свежасць (TSB)",
        "kk": "Сергектік (TSB)",
        "ka": "სიახლე (TSB)",
        "es": "Frescura (TSB)",
        "fr": "Fraîcheur (TSB)",
        "de": "Form (TSB)",
        "cs": "Čerstvost (TSB)",
        "zh-Hans": "状态 (TSB)",
    },
)

t(
    "insights_fitness_caption",
    **{
        "en-GB": "42-day fitness and 7-day fatigue with a confidence band and rest projection.",
        "uk": "42-денна форма і 7-денна втома з діапазоном довіри та проєкцією відпочинку.",
        "pl": "42-dniowa forma i 7-dniowe zmęczenie z pasmem pewności i projekcją odpoczynku.",
        "be": "42-дзённая форма і 7-дзённая стомленасць з дыяпазонам даверу і праекцыяй адпачынку.",
        "kk": "42 күндік форма және 7 күндік шаршау, сенім жолағы және демалыс болжамымен.",
        "ka": "42-დღიანი ფორმა და 7-დღიანი დაღლილობა ნდობის ზოლითა და დასვენების პროექციით.",
        "es": "Forma a 42 días y fatiga a 7 días con banda de confianza y proyección de descanso.",
        "fr": "Forme sur 42 jours et fatigue sur 7 jours avec bande de confiance et projection de repos.",
        "de": "42-Tage-Fitness und 7-Tage-Ermüdung mit Vertrauensband und Ruhe-Projektion.",
        "cs": "42denní forma a 7denní únava s pásmem jistoty a projekcí odpočinku.",
        "zh-Hans": "42 天体能与 7 天疲劳，含置信带与休息推演。",
    },
)

t(
    "insights_tlr_title",
    **{
        "en-GB": "Training load ratio",
        "uk": "Співвідношення навантаження",
        "pl": "Wskaźnik obciążenia",
        "be": "Суадносіны нагрузкі",
        "kk": "Жүктеме қатынасы",
        "ka": "დატვირთვის თანაფარდობა",
        "es": "Ratio de carga",
        "fr": "Ratio de charge",
        "de": "Trainingslast-Verhältnis",
        "cs": "Poměr zátěže",
        "zh-Hans": "训练负荷比",
    },
)

t(
    "insights_tlr_sweet",
    **{
        "en-GB": "Near the soft sweet spot (about 0.8–1.3). A wellness guide only.",
        "uk": "Біля м’якої «золотої» зони (близько 0,8–1,3). Лише орієнтир добробуту.",
        "pl": "Blisko miękkiego sweet spotu (ok. 0,8–1,3). Tylko wskazówka wellness.",
        "be": "Каля мяккай «залатой» зоны (каля 0,8–1,3). Толькі арыенцір дабрабыту.",
        "kk": "Жұмсақ ыңғайлы аймаққа жақын (шамамен 0,8–1,3). Тек сауықтыру бағдары.",
        "ka": "რბილ სასურველ ზონასთან ახლოს (დაახლ. 0.8–1.3). მხოლოდ კეთილდღეობის ორიენტირი.",
        "es": "Cerca de la zona suave (aprox. 0,8–1,3). Solo guía de bienestar.",
        "fr": "Près de la zone douce (environ 0,8–1,3). Guide bien-être uniquement.",
        "de": "Nahe am weichen Sweet Spot (etwa 0,8–1,3). Nur Wellness-Orientierung.",
        "cs": "Blízko měkkému sweet spotu (asi 0,8–1,3). Jen wellness vodítko.",
        "zh-Hans": "接近柔和适宜区间（约 0.8–1.3）。仅为健康参考。",
    },
)

t(
    "insights_tlr_low",
    **{
        "en-GB": "Below the soft band — recent load is lighter than your fitness base.",
        "uk": "Нижче м’якої зони — недавнє навантаження легше за базу форми.",
        "pl": "Poniżej miękkiego zakresu — niedawne obciążenie jest lżejsze niż baza formy.",
        "be": "Ніжэй мяккай зоны — нядаўняя нагрузка лягчэйшая за базу формы.",
        "kk": "Жұмсақ жолақтан төмен — соңғы жүктеме форма негізіндегінен жеңіл.",
        "ka": "რბილ ზოლზე ქვემოთ — ბოლოდროინდელი დატვირთვა ფორმის ბაზაზე მსუბუქია.",
        "es": "Por debajo de la banda suave — la carga reciente es más ligera que tu base.",
        "fr": "Sous la bande douce — la charge récente est plus légère que votre base.",
        "de": "Unter dem weichen Band — die aktuelle Last ist leichter als Ihre Basis.",
        "cs": "Pod měkkým pásmem — nedávná zátěž je lehčí než základ formy.",
        "zh-Hans": "低于柔和区间——近期负荷轻于体能基础。",
    },
)

t(
    "insights_tlr_elevated",
    **{
        "en-GB": "Above the soft band — recent load is higher than your fitness base.",
        "uk": "Вище м’якої зони — недавнє навантаження вище за базу форми.",
        "pl": "Powyżej miękkiego zakresu — niedawne obciążenie jest wyższe niż baza formy.",
        "be": "Вышэй мяккай зоны — нядаўняя нагрузка вышэйшая за базу формы.",
        "kk": "Жұмсақ жолақтан жоғары — соңғы жүктеме форма негізіндегінен жоғары.",
        "ka": "რბილ ზოლზე ზემოთ — ბოლოდროინდელი დატვირთვა ფორმის ბაზაზე მაღალია.",
        "es": "Por encima de la banda suave — la carga reciente supera tu base.",
        "fr": "Au-dessus de la bande douce — la charge récente dépasse votre base.",
        "de": "Über dem weichen Band — die aktuelle Last liegt über Ihrer Basis.",
        "cs": "Nad měkkým pásmem — nedávná zátěž je vyšší než základ formy.",
        "zh-Hans": "高于柔和区间——近期负荷高于体能基础。",
    },
)

t(
    "insights_tlr_need_fitness",
    **{
        "en-GB": "Build a fitness base with a few weeks of training to see this ratio.",
        "uk": "Наберіть базу форми за кілька тижнів тренувань, щоб побачити це співвідношення.",
        "pl": "Zbuduj bazę formy przez kilka tygodni treningu, aby zobaczyć ten wskaźnik.",
        "be": "Набярыце базу формы за некалькі тыдняў трэніровак, каб убачыць гэтыя суадносіны.",
        "kk": "Осы қатынасты көру үшін бірнеше апта жаттығып форма негізін жинаңыз.",
        "ka": "ამ თანაფარდობის სანახავად რამდენიმე კვირის ვარჯიშით ააშენეთ ფორმის ბაზა.",
        "es": "Crea una base de forma con unas semanas de entrenamiento para ver este ratio.",
        "fr": "Construisez une base de forme sur quelques semaines pour voir ce ratio.",
        "de": "Bauen Sie mit einigen Trainingswochen eine Fitnessbasis auf, um dieses Verhältnis zu sehen.",
        "cs": "Vybudujte základ formy pár týdny tréninku, abyste viděli tento poměr.",
        "zh-Hans": "先训练数周建立体能基础后再查看此比值。",
    },
)

t(
    "insights_focus_title",
    **{
        "en-GB": "Training load focus",
        "uk": "Фокус навантаження",
        "pl": "Fokus obciążenia",
        "be": "Фокус нагрузкі",
        "kk": "Жүктеме фокусы",
        "ka": "დატვირთვის ფოკუსი",
        "es": "Foco de carga",
        "fr": "Focus de charge",
        "de": "Last-Fokus",
        "cs": "Fokus zátěže",
        "zh-Hans": "负荷焦点",
    },
)

t(
    "insights_focus_anaerobic",
    **{
        "en-GB": "Anaerobic",
        "uk": "Анаеробне",
        "pl": "Anaerobowe",
        "be": "Анаэробнае",
        "kk": "Анаэробты",
        "ka": "ანაერობული",
        "es": "Anaeróbico",
        "fr": "Anaérobie",
        "de": "Anaerob",
        "cs": "Anaerobní",
        "zh-Hans": "无氧",
    },
)

t(
    "insights_focus_high_aerobic",
    **{
        "en-GB": "High aerobic",
        "uk": "Високе аеробне",
        "pl": "Wysoko aerobowe",
        "be": "Высокае аэробнае",
        "kk": "Жоғары аэробты",
        "ka": "მაღალი აერობული",
        "es": "Aeróbico alto",
        "fr": "Aérobie élevé",
        "de": "Hoch aerob",
        "cs": "Vysoce aerobní",
        "zh-Hans": "高有氧",
    },
)

t(
    "insights_focus_low_aerobic",
    **{
        "en-GB": "Low aerobic",
        "uk": "Низьке аеробне",
        "pl": "Nisko aerobowe",
        "be": "Нізкае аэробнае",
        "kk": "Төмен аэробты",
        "ka": "დაბალი აერობული",
        "es": "Aeróbico bajo",
        "fr": "Aérobie bas",
        "de": "Niedrig aerob",
        "cs": "Nízce aerobní",
        "zh-Hans": "低有氧",
    },
)

t(
    "insights_focus_caption",
    **{
        "en-GB": "Last 4 weeks, from heart-rate zones when available.",
        "uk": "Останні 4 тижні, з HR-зон якщо є.",
        "pl": "Ostatnie 4 tygodnie, ze stref HR gdy dostępne.",
        "be": "Апошнія 4 тыдні, з HR-зон калі ёсць.",
        "kk": "Соңғы 4 апта, HR аймақтары болса солардан.",
        "ka": "ბოლო 4 კვირა, HR ზონებიდან თუ ხელმისაწვდომია.",
        "es": "Últimas 4 semanas, desde zonas de FC si hay datos.",
        "fr": "4 dernières semaines, d’après les zones FC si disponibles.",
        "de": "Letzte 4 Wochen, aus HF-Zonen falls verfügbar.",
        "cs": "Poslední 4 týdny, ze zón TF pokud jsou.",
        "zh-Hans": "近 4 周；有心率时按心率区间划分。",
    },
)

t(
    "insights_goals_title",
    **{
        "en-GB": "Goals",
        "uk": "Цілі",
        "pl": "Cele",
        "be": "Мэты",
        "kk": "Мақсаттар",
        "ka": "მიზნები",
        "es": "Objetivos",
        "fr": "Objectifs",
        "de": "Ziele",
        "cs": "Cíle",
        "zh-Hans": "目标",
    },
)

t(
    "insights_goals_empty",
    **{
        "en-GB": "No goals yet.",
        "uk": "Поки немає цілей.",
        "pl": "Brak celów.",
        "be": "Пакуль няма мэтаў.",
        "kk": "Әзірге мақсат жоқ.",
        "ka": "ჯერ მიზნები არაა.",
        "es": "Aún no hay objetivos.",
        "fr": "Pas encore d’objectifs.",
        "de": "Noch keine Ziele.",
        "cs": "Zatím žádné cíle.",
        "zh-Hans": "暂无目标。",
    },
)

t(
    "insights_add_goal",
    **{
        "en-GB": "Add Goal",
        "uk": "Додати ціль",
        "pl": "Dodaj cel",
        "be": "Дадаць мэту",
        "kk": "Мақсат қосу",
        "ka": "მიზნის დამატება",
        "es": "Añadir objetivo",
        "fr": "Ajouter un objectif",
        "de": "Ziel hinzufügen",
        "cs": "Přidat cíl",
        "zh-Hans": "添加目标",
    },
)

t(
    "insights_goal_title_field",
    **{
        "en-GB": "Goal title",
        "uk": "Назва цілі",
        "pl": "Nazwa celu",
        "be": "Назва мэты",
        "kk": "Мақсат атауы",
        "ka": "მიზნის სახელი",
        "es": "Título del objetivo",
        "fr": "Titre de l’objectif",
        "de": "Zielname",
        "cs": "Název cíle",
        "zh-Hans": "目标名称",
    },
)

t(
    "insights_save_goal",
    **{
        "en-GB": "Save",
        "uk": "Зберегти",
        "pl": "Zapisz",
        "be": "Захаваць",
        "kk": "Сақтау",
        "ka": "შენახვა",
        "es": "Guardar",
        "fr": "Enregistrer",
        "de": "Speichern",
        "cs": "Uložit",
        "zh-Hans": "保存",
    },
)

t(
    "insights_cancel",
    **{
        "en-GB": "Cancel",
        "uk": "Скасувати",
        "pl": "Anuluj",
        "be": "Скасаваць",
        "kk": "Болдырмау",
        "ka": "გაუქმება",
        "es": "Cancelar",
        "fr": "Annuler",
        "de": "Abbrechen",
        "cs": "Zrušit",
        "zh-Hans": "取消",
    },
)

t(
    "insights_this_week",
    **{
        "en-GB": "This Week",
        "uk": "Цей тиждень",
        "pl": "Ten tydzień",
        "be": "Гэты тыдзень",
        "kk": "Осы апта",
        "ka": "ამ კვირას",
        "es": "Esta semana",
        "fr": "Cette semaine",
        "de": "Diese Woche",
        "cs": "Tento týden",
        "zh-Hans": "本周",
    },
)

t(
    "insights_this_month",
    **{
        "en-GB": "This Month",
        "uk": "Цей місяць",
        "pl": "Ten miesiąc",
        "be": "Гэты месяц",
        "kk": "Осы ай",
        "ka": "ამ თვეს",
        "es": "Este mes",
        "fr": "Ce mois",
        "de": "Dieser Monat",
        "cs": "Tento měsíc",
        "zh-Hans": "本月",
    },
)

t(
    "insights_metric_load",
    **{
        "en-GB": "Training load",
        "uk": "Тренувальне навантаження",
        "pl": "Obciążenie treningowe",
        "be": "Трэніровачная нагрузка",
        "kk": "Жаттығу жүктемесі",
        "ka": "სავარჯიშო დატვირთვა",
        "es": "Carga de entrenamiento",
        "fr": "Charge d’entraînement",
        "de": "Trainingslast",
        "cs": "Tréninková zátěž",
        "zh-Hans": "训练负荷",
    },
)

t(
    "insights_metric_time",
    **{
        "en-GB": "Workout time",
        "uk": "Час тренувань",
        "pl": "Czas treningu",
        "be": "Час трэніровак",
        "kk": "Жаттығу уақыты",
        "ka": "ვარჯიშის დრო",
        "es": "Tiempo de entreno",
        "fr": "Temps d’entraînement",
        "de": "Trainingszeit",
        "cs": "Čas tréninku",
        "zh-Hans": "训练时长",
    },
)

t(
    "insights_metric_kcal",
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
    "insights_metric_count",
    **{
        "en-GB": "Workouts",
        "uk": "Тренування",
        "pl": "Treningi",
        "be": "Трэніроўкі",
        "kk": "Жаттығулар",
        "ka": "ვარჯიშები",
        "es": "Entrenos",
        "fr": "Séances",
        "de": "Workouts",
        "cs": "Tréninky",
        "zh-Hans": "训练次数",
    },
)

t(
    "insights_vs_previous",
    **{
        "en-GB": "Orange: this period · Grey: previous period",
        "uk": "Помаранчевий: цей період · Сірий: попередній",
        "pl": "Pomarańcz: ten okres · Szary: poprzedni",
        "be": "Аранжавы: гэты перыяд · Шэры: папярэдні",
        "kk": "Қызғылт сары: осы кезең · Сұр: алдыңғы",
        "ka": "ნარინჯისფერი: ეს პერიოდი · ნაცრისფერი: წინა",
        "es": "Naranja: este periodo · Gris: anterior",
        "fr": "Orange : cette période · Gris : précédente",
        "de": "Orange: dieser Zeitraum · Grau: vorheriger",
        "cs": "Oranžová: toto období · Šedá: předchozí",
        "zh-Hans": "橙色：本期 · 灰色：上期",
    },
)

t(
    "insights_past_12_months",
    **{
        "en-GB": "Past 12 months",
        "uk": "Останні 12 місяців",
        "pl": "Ostatnie 12 miesięcy",
        "be": "Апошнія 12 месяцаў",
        "kk": "Соңғы 12 ай",
        "ka": "ბოლო 12 თვე",
        "es": "Últimos 12 meses",
        "fr": "12 derniers mois",
        "de": "Letzte 12 Monate",
        "cs": "Posledních 12 měsíců",
        "zh-Hans": "过去 12 个月",
    },
)

t(
    "insights_no_sport_distance",
    **{
        "en-GB": "No distance sports logged in this window.",
        "uk": "У цьому вікні немає дистанційних видів.",
        "pl": "Brak sportów dystansowych w tym oknie.",
        "be": "У гэтым акне няма дыстанцыйных відаў.",
        "kk": "Осы терезеде қашықтық спорты жоқ.",
        "ka": "ამ ფანჯარაში დისტანციური სპორტი არაა.",
        "es": "No hay deportes de distancia en esta ventana.",
        "fr": "Pas de sports de distance dans cette fenêtre.",
        "de": "Keine Distanzsportarten in diesem Fenster.",
        "cs": "V tomto okně nejsou vzdálenostní sporty.",
        "zh-Hans": "此时间段无距离类运动。",
    },
)

t(
    "insights_ytd_load",
    **{
        "en-GB": "Year to date · cumulative load",
        "uk": "З початку року · накопичене навантаження",
        "pl": "Od początku roku · skumulowane obciążenie",
        "be": "З пачатку года · назапашаная нагрузка",
        "kk": "Жыл басынан · жинақталған жүктеме",
        "ka": "წლის დასაწყისიდან · დაგროვილი დატვირთვა",
        "es": "En lo que va de año · carga acumulada",
        "fr": "Depuis le début de l’année · charge cumulée",
        "de": "Seit Jahresbeginn · kumulierte Last",
        "cs": "Od začátku roku · kumulativní zátěž",
        "zh-Hans": "年初至今 · 累计负荷",
    },
)

t(
    "insights_heatmap_workouts",
    **{
        "en-GB": "Workout calendar",
        "uk": "Календар тренувань",
        "pl": "Kalendarz treningów",
        "be": "Каляндар трэніровак",
        "kk": "Жаттығу күнтізбесі",
        "ka": "ვარჯიშების კალენდარი",
        "es": "Calendario de entrenos",
        "fr": "Calendrier des séances",
        "de": "Workout-Kalender",
        "cs": "Kalendář tréninků",
        "zh-Hans": "训练日历",
    },
)

t(
    "insights_heatmap_exercise",
    **{
        "en-GB": "Exercise minutes calendar",
        "uk": "Календар хвилин руху",
        "pl": "Kalendarz minut ruchu",
        "be": "Каляндар хвілін руху",
        "kk": "Қозғалыс минуттарының күнтізбесі",
        "ka": "მოძრაობის წუთების კალენდარი",
        "es": "Calendario de minutos de ejercicio",
        "fr": "Calendrier des minutes d’exercice",
        "de": "Kalender der Bewegungsminuten",
        "cs": "Kalendář minut pohybu",
        "zh-Hans": "运动分钟日历",
    },
)

t(
    "insights_all_time",
    **{
        "en-GB": "All time",
        "uk": "За весь час",
        "pl": "Cały czas",
        "be": "За ўвесь час",
        "kk": "Барлық уақыт",
        "ka": "ყველა დრო",
        "es": "Todo el tiempo",
        "fr": "Tout le temps",
        "de": "Gesamt",
        "cs": "Za celou dobu",
        "zh-Hans": "全部时间",
    },
)

t(
    "insights_workouts",
    **{
        "en-GB": "Workouts",
        "uk": "Тренування",
        "pl": "Treningi",
        "be": "Трэніроўкі",
        "kk": "Жаттығулар",
        "ka": "ვარჯიშები",
        "es": "Entrenos",
        "fr": "Séances",
        "de": "Workouts",
        "cs": "Tréninky",
        "zh-Hans": "训练",
    },
)

t(
    "insights_active_kcal",
    **{
        "en-GB": "Active energy",
        "uk": "Активна енергія",
        "pl": "Energia aktywna",
        "be": "Актыўная энергія",
        "kk": "Белсенді энергия",
        "ka": "აქტიური ენერგია",
        "es": "Energía activa",
        "fr": "Énergie active",
        "de": "Aktive Energie",
        "cs": "Aktivní energie",
        "zh-Hans": "活动能量",
    },
)

t(
    "insights_hours",
    **{
        "en-GB": "Hours",
        "uk": "Години",
        "pl": "Godziny",
        "be": "Гадзіны",
        "kk": "Сағат",
        "ka": "საათები",
        "es": "Horas",
        "fr": "Heures",
        "de": "Stunden",
        "cs": "Hodiny",
        "zh-Hans": "小时",
    },
)

t(
    "insights_wellness_note",
    **{
        "en-GB": "Wellness estimates only — not a medical diagnosis or treatment.",
        "uk": "Лише оцінки добробуту — не медичний діагноз і не лікування.",
        "pl": "Tylko szacunki wellness — nie diagnoza ani leczenie.",
        "be": "Толькі ацэнкі дабрабыту — не медыцынскі дыягназ і не лячэнне.",
        "kk": "Тек сауықтыру бағалаулары — медициналық диагноз немесе ем емес.",
        "ka": "მხოლოდ კეთილდღეობის შეფასებები — არა დიაგნოზი ან მკურნალობა.",
        "es": "Solo estimaciones de bienestar — no diagnóstico ni tratamiento.",
        "fr": "Estimations bien-être uniquement — pas un diagnostic ni un traitement.",
        "de": "Nur Wellness-Schätzungen — keine Diagnose oder Behandlung.",
        "cs": "Pouze wellness odhady — ne diagnóza ani léčba.",
        "zh-Hans": "仅为健康估算，不是诊断或治疗。",
    },
)

t(
    "insights_chart_a11y",
    **{
        "en-GB": "Trend chart",
        "uk": "Графік тренду",
        "pl": "Wykres trendu",
        "be": "Графік трэнду",
        "kk": "Тренд диаграммасы",
        "ka": "ტენდენციის გრაფიკი",
        "es": "Gráfico de tendencia",
        "fr": "Graphique de tendance",
        "de": "Trenddiagramm",
        "cs": "Graf trendu",
        "zh-Hans": "趋势图",
    },
)

t(
    "insights_heatmap_a11y",
    **{
        "en-GB": "Activity heatmap",
        "uk": "Теплова карта активності",
        "pl": "Mapa ciepła aktywności",
        "be": "Цеплавая карта актыўнасці",
        "kk": "Белсенділік жылу картасы",
        "ka": "აქტივობის სითბური რუკა",
        "es": "Mapa de calor de actividad",
        "fr": "Carte de chaleur d’activité",
        "de": "Aktivitäts-Heatmap",
        "cs": "Teplotní mapa aktivity",
        "zh-Hans": "活动热力图",
    },
)

t(
    "health_metrics_intro",
    **{
        "en-GB": "Health metrics from Apple Health or Health Connect. Read-only. Wellness display only.",
        "uk": "Показники здоров’я з Apple Health або Health Connect. Лише читання. Лише відображення добробуту.",
        "pl": "Wskaźniki zdrowia z Apple Health lub Health Connect. Tylko odczyt. Tylko wellness.",
        "be": "Паказчыкі здароўя з Apple Health або Health Connect. Толькі чытанне. Толькі дабрабыт.",
        "kk": "Apple Health немесе Health Connect көрсеткіштері. Тек оқу. Тек сауықтыру көрсетілімі.",
        "ka": "ჯანმრთელობის მეტრიკები Apple Health ან Health Connect-დან. მხოლოდ კითხვა. მხოლოდ კეთილდღეობა.",
        "es": "Métricas de salud de Apple Health o Health Connect. Solo lectura. Solo bienestar.",
        "fr": "Mesures de santé depuis Apple Health ou Health Connect. Lecture seule. Affichage bien-être uniquement.",
        "de": "Gesundheitswerte aus Apple Health oder Health Connect. Nur Lesen. Nur Wellness-Anzeige.",
        "cs": "Zdravotní metriky z Apple Health nebo Health Connect. Jen čtení. Jen wellness zobrazení.",
        "zh-Hans": "来自 Apple Health 或 Health Connect 的健康指标。只读。仅为健康展示。",
    },
)

t(
    "health_section_today",
    **{
        "en-GB": "Today",
        "uk": "Сьогодні",
        "pl": "Dziś",
        "be": "Сёння",
        "kk": "Бүгін",
        "ka": "დღეს",
        "es": "Hoy",
        "fr": "Aujourd’hui",
        "de": "Heute",
        "cs": "Dnes",
        "zh-Hans": "今日",
    },
)

t(
    "health_section_heart",
    **{
        "en-GB": "Heart",
        "uk": "Серце",
        "pl": "Serce",
        "be": "Сэрца",
        "kk": "Жүрек",
        "ka": "გული",
        "es": "Corazón",
        "fr": "Cœur",
        "de": "Herz",
        "cs": "Srdce",
        "zh-Hans": "心脏",
    },
)

t(
    "health_section_vitals",
    **{
        "en-GB": "Vitals",
        "uk": "Життєві показники",
        "pl": "Parametry życiowe",
        "be": "Жыццёвыя паказчыкі",
        "kk": "Өмірлік көрсеткіштер",
        "ka": "ვიტალური",
        "es": "Constantes",
        "fr": "Signes vitaux",
        "de": "Vitalwerte",
        "cs": "Vitální hodnoty",
        "zh-Hans": "生命体征",
    },
)

t(
    "health_section_nutrition",
    **{
        "en-GB": "Nutrition",
        "uk": "Харчування",
        "pl": "Odżywianie",
        "be": "Харчаванне",
        "kk": "Тамақтану",
        "ka": "კვება",
        "es": "Nutrición",
        "fr": "Nutrition",
        "de": "Ernährung",
        "cs": "Výživa",
        "zh-Hans": "营养",
    },
)

t(
    "health_section_sleep",
    **{
        "en-GB": "Sleep",
        "uk": "Сон",
        "pl": "Sen",
        "be": "Сон",
        "kk": "Ұйқы",
        "ka": "ძილი",
        "es": "Sueño",
        "fr": "Sommeil",
        "de": "Schlaf",
        "cs": "Spánek",
        "zh-Hans": "睡眠",
    },
)

t(
    "health_section_mindful",
    **{
        "en-GB": "Mindful",
        "uk": "Уважність",
        "pl": "Uważność",
        "be": "Уважнасць",
        "kk": "Зейін",
        "ka": "ყურადღებიანობა",
        "es": "Atención plena",
        "fr": "Pleine conscience",
        "de": "Achtsamkeit",
        "cs": "Všímavost",
        "zh-Hans": "正念",
    },
)

t(
    "health_section_past7",
    **{
        "en-GB": "Past 7 days",
        "uk": "Останні 7 днів",
        "pl": "Ostatnie 7 dni",
        "be": "Апошнія 7 дзён",
        "kk": "Соңғы 7 күн",
        "ka": "ბოლო 7 დღე",
        "es": "Últimos 7 días",
        "fr": "7 derniers jours",
        "de": "Letzte 7 Tage",
        "cs": "Posledních 7 dní",
        "zh-Hans": "过去 7 天",
    },
)

t(
    "health_section_past30",
    **{
        "en-GB": "Past 30 days",
        "uk": "Останні 30 днів",
        "pl": "Ostatnie 30 dni",
        "be": "Апошнія 30 дзён",
        "kk": "Соңғы 30 күн",
        "ka": "ბოლო 30 დღე",
        "es": "Últimos 30 días",
        "fr": "30 derniers jours",
        "de": "Letzte 30 Tage",
        "cs": "Posledních 30 dní",
        "zh-Hans": "过去 30 天",
    },
)

t(
    "health_section_all_time",
    **{
        "en-GB": "All time",
        "uk": "За весь час",
        "pl": "Cały czas",
        "be": "За ўвесь час",
        "kk": "Барлық уақыт",
        "ka": "ყველა დრო",
        "es": "Todo el tiempo",
        "fr": "Tout le temps",
        "de": "Gesamt",
        "cs": "Za celou dobu",
        "zh-Hans": "全部时间",
    },
)

t(
    "health_active_energy",
    **{
        "en-GB": "Active energy",
        "uk": "Активна енергія",
        "pl": "Energia aktywna",
        "be": "Актыўная энергія",
        "kk": "Белсенді энергия",
        "ka": "აქტიური ენერგია",
        "es": "Energía activa",
        "fr": "Énergie active",
        "de": "Aktive Energie",
        "cs": "Aktivní energie",
        "zh-Hans": "活动能量",
    },
)

t(
    "health_resting_energy",
    **{
        "en-GB": "Resting energy",
        "uk": "Енергія спокою",
        "pl": "Energia spoczynkowa",
        "be": "Энергія спакою",
        "kk": "Демалыс энергиясы",
        "ka": "მოსვენების ენერგია",
        "es": "Energía en reposo",
        "fr": "Énergie au repos",
        "de": "Ruheenergie",
        "cs": "Energie v klidu",
        "zh-Hans": "静息能量",
    },
)

t(
    "health_exercise_minutes",
    **{
        "en-GB": "Exercise minutes",
        "uk": "Хвилини руху",
        "pl": "Minuty ruchu",
        "be": "Хвіліны руху",
        "kk": "Қозғалыс минуттары",
        "ka": "მოძრაობის წუთები",
        "es": "Minutos de ejercicio",
        "fr": "Minutes d’exercice",
        "de": "Bewegungsminuten",
        "cs": "Minuty pohybu",
        "zh-Hans": "运动分钟",
    },
)

t(
    "health_steps",
    **{
        "en-GB": "Steps",
        "uk": "Кроки",
        "pl": "Kroki",
        "be": "Крокі",
        "kk": "Қадамдар",
        "ka": "ნაბიჯები",
        "es": "Pasos",
        "fr": "Pas",
        "de": "Schritte",
        "cs": "Kroky",
        "zh-Hans": "步数",
    },
)

t(
    "health_resting_hr",
    **{
        "en-GB": "Resting heart rate",
        "uk": "ЧСС спокою",
        "pl": "Tętno spoczynkowe",
        "be": "ЧСС спакою",
        "kk": "Тыныш жүрек соғысы",
        "ka": "მოსვენების გულისცემა",
        "es": "FC en reposo",
        "fr": "FC au repos",
        "de": "Ruheherzfrequenz",
        "cs": "Klídová TF",
        "zh-Hans": "静息心率",
    },
)

t(
    "health_hrv",
    **{
        "en-GB": "HRV",
        "uk": "ВСР",
        "pl": "HRV",
        "be": "ВСР",
        "kk": "HRV",
        "ka": "HRV",
        "es": "VFC",
        "fr": "VFC",
        "de": "HRV",
        "cs": "HRV",
        "zh-Hans": "心率变异",
    },
)

t(
    "health_hrv_stress",
    **{
        "en-GB": "HRV stress index",
        "uk": "Індекс напруження ВСР",
        "pl": "Indeks stresu HRV",
        "be": "Індэкс напружання ВСР",
        "kk": "HRV стресс индексі",
        "ka": "HRV სტრესის ინდექსი",
        "es": "Índice de estrés VFC",
        "fr": "Indice de stress VFC",
        "de": "HRV-Stressindex",
        "cs": "Index stresu HRV",
        "zh-Hans": "HRV 压力指数",
    },
)

t(
    "health_hrv_stress_estimate",
    **{
        "en-GB": "Wellness estimate (Baevsky-style)",
        "uk": "Оцінка добробуту (у стилі Баєвського)",
        "pl": "Szacunek wellness (styl Bajewskiego)",
        "be": "Ацэнка дабрабыту (стыль Баеўскага)",
        "kk": "Сауықтыру бағалауы (Баевский стилі)",
        "ka": "კეთილდღეობის შეფასება (ბაევსკის სტილი)",
        "es": "Estimación de bienestar (estilo Baevsky)",
        "fr": "Estimation bien-être (style Baevsky)",
        "de": "Wellness-Schätzung (Baevsky-Stil)",
        "cs": "Wellness odhad (styl Baevského)",
        "zh-Hans": "健康估算（Baevsky 风格）",
    },
)

t(
    "health_recovery_hrv",
    **{
        "en-GB": "Recovery HRV",
        "uk": "Відновлювальна ВСР",
        "pl": "HRV regeneracji",
        "be": "Аднаўленчая ВСР",
        "kk": "Қалпына келу HRV",
        "ka": "აღდგენის HRV",
        "es": "VFC de recuperación",
        "fr": "VFC de récupération",
        "de": "Erholungs-HRV",
        "cs": "Zotavovací HRV",
        "zh-Hans": "恢复 HRV",
    },
)

t(
    "health_blood_pressure",
    **{
        "en-GB": "Blood pressure",
        "uk": "Тиск",
        "pl": "Ciśnienie",
        "be": "Ціск",
        "kk": "Қан қысымы",
        "ka": "წნევა",
        "es": "Presión arterial",
        "fr": "Tension artérielle",
        "de": "Blutdruck",
        "cs": "Krevní tlak",
        "zh-Hans": "血压",
    },
)

t(
    "health_spo2",
    **{
        "en-GB": "Oxygen saturation",
        "uk": "Насичення киснем",
        "pl": "Saturacja",
        "be": "Насычэнне кіслародам",
        "kk": "Оттегімен қанығу",
        "ka": "ჟანგბადით გაჯერება",
        "es": "Saturación de oxígeno",
        "fr": "Saturation en oxygène",
        "de": "Sauerstoffsättigung",
        "cs": "Saturace kyslíkem",
        "zh-Hans": "血氧饱和度",
    },
)

t(
    "health_respiratory_rate",
    **{
        "en-GB": "Respiratory rate",
        "uk": "Частота дихання",
        "pl": "Częstość oddechów",
        "be": "Частата дыхання",
        "kk": "Тыныс алу жиілігі",
        "ka": "სუნთქვის სიხშირე",
        "es": "Frecuencia respiratoria",
        "fr": "Fréquence respiratoire",
        "de": "Atemfrequenz",
        "cs": "Dechová frekvence",
        "zh-Hans": "呼吸频率",
    },
)

t(
    "health_wrist_temperature",
    **{
        "en-GB": "Wrist temperature",
        "uk": "Температура зап’ястя",
        "pl": "Temperatura nadgarstka",
        "be": "Тэмпература запясця",
        "kk": "Білек температурасы",
        "ka": "მაჯის ტემპერატურა",
        "es": "Temperatura de muñeca",
        "fr": "Température au poignet",
        "de": "Handgelenkstemperatur",
        "cs": "Teplota zápěstí",
        "zh-Hans": "腕温",
    },
)

t(
    "health_dietary_energy",
    **{
        "en-GB": "Dietary energy",
        "uk": "Харчова енергія",
        "pl": "Energia z pożywienia",
        "be": "Харчовая энергія",
        "kk": "Тағам энергиясы",
        "ka": "კვების ენერგია",
        "es": "Energía dietética",
        "fr": "Énergie alimentaire",
        "de": "Nahrungsenergie",
        "cs": "Potravní energie",
        "zh-Hans": "膳食能量",
    },
)

t(
    "health_dietary_macros",
    **{
        "en-GB": "Macros",
        "uk": "Макронутрієнти",
        "pl": "Makroskładniki",
        "be": "Макранутрыенты",
        "kk": "Макронутриенттер",
        "ka": "მაკრონუტრიენტები",
        "es": "Macros",
        "fr": "Macros",
        "de": "Makronährstoffe",
        "cs": "Makra",
        "zh-Hans": "宏量营养素",
    },
)

t(
    "health_sleep",
    **{
        "en-GB": "Sleep",
        "uk": "Сон",
        "pl": "Sen",
        "be": "Сон",
        "kk": "Ұйқы",
        "ka": "ძილი",
        "es": "Sueño",
        "fr": "Sommeil",
        "de": "Schlaf",
        "cs": "Spánek",
        "zh-Hans": "睡眠",
    },
)

t(
    "health_asleep",
    **{
        "en-GB": "Asleep",
        "uk": "Сон",
        "pl": "Sen",
        "be": "Сон",
        "kk": "Ұйқыда",
        "ka": "ძილში",
        "es": "Dormido",
        "fr": "Endormi",
        "de": "Schlafend",
        "cs": "Ve spánku",
        "zh-Hans": "入睡",
    },
)

t(
    "health_sleep_start",
    **{
        "en-GB": "Start",
        "uk": "Початок",
        "pl": "Początek",
        "be": "Пачатак",
        "kk": "Басталуы",
        "ka": "დასაწყისი",
        "es": "Inicio",
        "fr": "Début",
        "de": "Beginn",
        "cs": "Začátek",
        "zh-Hans": "开始",
    },
)

t(
    "health_sleep_end",
    **{
        "en-GB": "End",
        "uk": "Кінець",
        "pl": "Koniec",
        "be": "Канец",
        "kk": "Аяқталуы",
        "ka": "დასასრული",
        "es": "Fin",
        "fr": "Fin",
        "de": "Ende",
        "cs": "Konec",
        "zh-Hans": "结束",
    },
)

t(
    "health_mindful_minutes",
    **{
        "en-GB": "Mindful minutes",
        "uk": "Хвилини уважності",
        "pl": "Minuty uważności",
        "be": "Хвіліны ўважнасці",
        "kk": "Зейін минуттары",
        "ka": "ყურადღებიანობის წუთები",
        "es": "Minutos de atención plena",
        "fr": "Minutes de pleine conscience",
        "de": "Achtsamkeitsminuten",
        "cs": "Minuty všímavosti",
        "zh-Hans": "正念分钟",
    },
)

t(
    "health_permission_needed",
    **{
        "en-GB": "Health access is needed to show this metric.",
        "uk": "Щоб показати цей показник, потрібен доступ до здоров’я.",
        "pl": "Aby pokazać ten wskaźnik, potrzebny jest dostęp do zdrowia.",
        "be": "Каб паказаць гэты паказчык, патрэбны доступ да здароўя.",
        "kk": "Бұл көрсеткішті көрсету үшін денсаулыққа қолжетімділік керек.",
        "ka": "ამ მეტრიკის საჩვენებლად საჭიროა ჯანმრთელობის წვდომა.",
        "es": "Se necesita acceso a salud para mostrar esta métrica.",
        "fr": "L’accès Santé est requis pour afficher cette mesure.",
        "de": "Für diese Kennzahl ist Gesundheitszugriff nötig.",
        "cs": "Pro zobrazení této metriky je potřeba přístup ke zdraví.",
        "zh-Hans": "需要健康数据访问权限才能显示此指标。",
    },
)

t(
    "health_no_data",
    **{
        "en-GB": "No data for this period.",
        "uk": "Немає даних за цей період.",
        "pl": "Brak danych w tym okresie.",
        "be": "Няма дадзеных за гэты перыяд.",
        "kk": "Осы кезеңде дерек жоқ.",
        "ka": "ამ პერიოდისთვის მონაცემები არაა.",
        "es": "No hay datos para este periodo.",
        "fr": "Pas de données pour cette période.",
        "de": "Keine Daten für diesen Zeitraum.",
        "cs": "Pro toto období nejsou data.",
        "zh-Hans": "此时间段无数据。",
    },
)

t(
    "health_request_access",
    **{
        "en-GB": "Request access",
        "uk": "Запитати доступ",
        "pl": "Poproś o dostęp",
        "be": "Запытаць доступ",
        "kk": "Қолжетімділік сұрау",
        "ka": "წვდომის მოთხოვნა",
        "es": "Solicitar acceso",
        "fr": "Demander l’accès",
        "de": "Zugriff anfordern",
        "cs": "Požádat o přístup",
        "zh-Hans": "请求访问",
    },
)

t(
    "health_wellness_note",
    **{
        "en-GB": "Wellness display only — not a medical diagnosis or treatment. OmniGart never writes or deletes these health records from this tab.",
        "uk": "Лише відображення добробуту — не діагноз і не лікування. OmniGart ніколи не записує й не видаляє ці записи здоров’я з цієї вкладки.",
        "pl": "Tylko wyświetlanie wellness — nie diagnoza ani leczenie. OmniGart nigdy nie zapisuje ani nie usuwa tych rekordów zdrowia z tej karty.",
        "be": "Толькі адлюстраванне дабрабыту — не дыягназ і не лячэнне. OmniGart ніколі не запісвае і не выдаляе гэтыя запісы здароўя з гэтай укладкі.",
        "kk": "Тек сауықтыру көрсетілімі — диагноз немесе ем емес. OmniGart осы қойыншықтан бұл денсаулық жазбаларын ешқашан жазбайды және жоймайды.",
        "ka": "მხოლოდ კეთილდღეობის ჩვენება — არა დიაგნოზი ან მკურნალობა. OmniGart ამ ჩანართიდან ამ ჯანმრთელობის ჩანაწერებს არასდროს წერს ან შლის.",
        "es": "Solo visualización de bienestar — no diagnóstico ni tratamiento. OmniGart nunca escribe ni elimina estos registros de salud desde esta pestaña.",
        "fr": "Affichage bien-être uniquement — pas un diagnostic ni un traitement. OmniGart n’écrit ni ne supprime jamais ces enregistrements depuis cet onglet.",
        "de": "Nur Wellness-Anzeige — keine Diagnose oder Behandlung. OmniGart schreibt oder löscht diese Gesundheitsdaten von diesem Tab nie.",
        "cs": "Jen wellness zobrazení — ne diagnóza ani léčba. OmniGart z této karty tyto zdravotní záznamy nikdy nezapisuje ani nemaže.",
        "zh-Hans": "仅为健康展示，不是诊断或治疗。OmniGart 不会从此页写入或删除这些健康记录。",
    },
)

t(
    "health_unit_kcal",
    **{
        "en-GB": "kcal",
        "uk": "ккал",
        "pl": "kcal",
        "be": "ккал",
        "kk": "ккал",
        "ka": "კკალ",
        "es": "kcal",
        "fr": "kcal",
        "de": "kcal",
        "cs": "kcal",
        "zh-Hans": "千卡",
    },
)

t(
    "health_unit_min",
    **{
        "en-GB": "min",
        "uk": "хв",
        "pl": "min",
        "be": "хв",
        "kk": "мин",
        "ka": "წთ",
        "es": "min",
        "fr": "min",
        "de": "Min",
        "cs": "min",
        "zh-Hans": "分钟",
    },
)

t(
    "health_unit_steps",
    **{
        "en-GB": "steps",
        "uk": "кроки",
        "pl": "kroki",
        "be": "крокі",
        "kk": "қадам",
        "ka": "ნაბიჯი",
        "es": "pasos",
        "fr": "pas",
        "de": "Schritte",
        "cs": "kroky",
        "zh-Hans": "步",
    },
)

t(
    "health_unit_bpm",
    **{
        "en-GB": "bpm",
        "uk": "уд/хв",
        "pl": "bpm",
        "be": "уд/хв",
        "kk": "соғу/мин",
        "ka": "დარტყმა/წთ",
        "es": "lpm",
        "fr": "bpm",
        "de": "bpm",
        "cs": "tep/min",
        "zh-Hans": "次/分",
    },
)

t(
    "health_unit_ms",
    **{
        "en-GB": "ms",
        "uk": "мс",
        "pl": "ms",
        "be": "мс",
        "kk": "мс",
        "ka": "მწმ",
        "es": "ms",
        "fr": "ms",
        "de": "ms",
        "cs": "ms",
        "zh-Hans": "毫秒",
    },
)

t(
    "health_unit_percent",
    **{
        "en-GB": "%",
        "uk": "%",
        "pl": "%",
        "be": "%",
        "kk": "%",
        "ka": "%",
        "es": "%",
        "fr": "%",
        "de": "%",
        "cs": "%",
        "zh-Hans": "%",
    },
)

t(
    "health_unit_brpm",
    **{
        "en-GB": "brpm",
        "uk": "дих/хв",
        "pl": "oddech/min",
        "be": "дых/хв",
        "kk": "тыныс/мин",
        "ka": "ამოსუნთქვა/წთ",
        "es": "rpm",
        "fr": "rpm",
        "de": "Atem/min",
        "cs": "dech/min",
        "zh-Hans": "次/分",
    },
)

t(
    "health_unit_celsius",
    **{
        "en-GB": "°C",
        "uk": "°C",
        "pl": "°C",
        "be": "°C",
        "kk": "°C",
        "ka": "°C",
        "es": "°C",
        "fr": "°C",
        "de": "°C",
        "cs": "°C",
        "zh-Hans": "°C",
    },
)

t(
    "health_unit_mmhg",
    **{
        "en-GB": "mmHg",
        "uk": "мм рт. ст.",
        "pl": "mmHg",
        "be": "мм рт. сл.",
        "kk": "мм сын. бағ.",
        "ka": "მმHg",
        "es": "mmHg",
        "fr": "mmHg",
        "de": "mmHg",
        "cs": "mmHg",
        "zh-Hans": "毫米汞柱",
    },
)

t(
    "health_unit_g",
    **{
        "en-GB": "g",
        "uk": "г",
        "pl": "g",
        "be": "г",
        "kk": "г",
        "ka": "გ",
        "es": "g",
        "fr": "g",
        "de": "g",
        "cs": "g",
        "zh-Hans": "克",
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
                value = escape_android(
                    locales["en-GB"] if folder == "values" else locales[locale]
                )
                pattern = re.compile(rf'<string name="{re.escape(key)}">.*?</string>', re.DOTALL)
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
    print(f"Synced {len(STRINGS)} insights/health keys × {len(LOCALES)} locales")


if __name__ == "__main__":
    main()

