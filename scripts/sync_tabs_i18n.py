#!/usr/bin/env python3
"""Sync phone-tab UI strings into shared JSON, Localizable.xcstrings, and Android values*."""

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

# Compact translations: en-GB first; other locales follow glossary-aligned wellness wording.
STRINGS: dict[str, dict[str, str]] = {}


def t(key: str, **by_locale: str) -> None:
    assert "en-GB" in by_locale
    for loc in LOCALES:
        assert loc in by_locale, f"{key} missing {loc}"
    STRINGS[key] = {loc: by_locale[loc] for loc in LOCALES}


t(
    "food_intro",
    **{
        "en-GB": "Log meals offline. Daily totals are wellness coaching aids, not medical advice.",
        "uk": "Записуйте їжу офлайн. Денні підсумки — тренерські оцінки, не медична консультація.",
        "pl": "Zapisuj posiłki offline. Dzienne sumy to wskazówki wellness, nie porada medyczna.",
        "be": "Запісвайце ежу афлайн. Дзённыя вынікі — трэнерскія ацэнкі, не медыцынская кансультацыя.",
        "kk": "Тамақты офлайн жазыңыз. Күнделікті қорытындылар — сауықтыру кеңестері, медициналық кеңес емес.",
        "ka": "ჩაიწერეთ საკვები ოფლაინ. დღიური ჯამები კეთილდღეობის რჩევაა, არა სამედიცინო კონსულტაცია.",
        "es": "Registra comidas sin conexión. Los totales diarios son ayudas de bienestar, no consejo médico.",
        "fr": "Enregistrez les repas hors ligne. Les totaux du jour sont des aides bien-être, pas un avis médical.",
        "de": "Mahlzeiten offline erfassen. Tageswerte sind Wellness-Hilfen, keine medizinische Beratung.",
        "cs": "Zaznamenávejte jídlo offline. Denní součty jsou wellness pomůcky, ne lékařská rada.",
        "zh-Hans": "离线记录餐食。每日合计为健康教练参考，不是医疗建议。",
    },
)
t(
    "food_totals_title",
    **{
        "en-GB": "Today’s totals",
        "uk": "Сьогоднішні підсумки",
        "pl": "Dzisiejsze sumy",
        "be": "Сённяшнія вынікі",
        "kk": "Бүгінгі қорытынды",
        "ka": "დღევანდელი ჯამები",
        "es": "Totales de hoy",
        "fr": "Totaux du jour",
        "de": "Heutige Summen",
        "cs": "Dnešní součty",
        "zh-Hans": "今日合计",
    },
)
t(
    "food_log_title",
    **{
        "en-GB": "Add food",
        "uk": "Додати їжу",
        "pl": "Dodaj posiłek",
        "be": "Дадаць ежу",
        "kk": "Тамақ қосу",
        "ka": "საკვების დამატება",
        "es": "Añadir comida",
        "fr": "Ajouter un aliment",
        "de": "Essen hinzufügen",
        "cs": "Přidat jídlo",
        "zh-Hans": "添加食物",
    },
)
t(
    "food_today_title",
    **{
        "en-GB": "Logged today",
        "uk": "Записано сьогодні",
        "pl": "Zapisane dziś",
        "be": "Запісана сёння",
        "kk": "Бүгін жазылған",
        "ka": "დღეს ჩაწერილი",
        "es": "Registrado hoy",
        "fr": "Enregistré aujourd’hui",
        "de": "Heute erfasst",
        "cs": "Zaznamenáno dnes",
        "zh-Hans": "今日已记录",
    },
)
t(
    "food_name_placeholder",
    **{
        "en-GB": "Food name",
        "uk": "Назва їжі",
        "pl": "Nazwa potrawy",
        "be": "Назва ежы",
        "kk": "Тамақ атауы",
        "ka": "საკვების სახელი",
        "es": "Nombre del alimento",
        "fr": "Nom de l’aliment",
        "de": "Lebensmittelname",
        "cs": "Název jídla",
        "zh-Hans": "食物名称",
    },
)
t(
    "food_energy_kcal",
    **{
        "en-GB": "Energy (kcal)",
        "uk": "Енергія (ккал)",
        "pl": "Energia (kcal)",
        "be": "Энергія (ккал)",
        "kk": "Энергия (ккал)",
        "ka": "ენერგია (კკალ)",
        "es": "Energía (kcal)",
        "fr": "Énergie (kcal)",
        "de": "Energie (kcal)",
        "cs": "Energie (kcal)",
        "zh-Hans": "能量（千卡）",
    },
)
t(
    "food_protein_g",
    **{
        "en-GB": "Protein (g)",
        "uk": "Білок (г)",
        "pl": "Białko (g)",
        "be": "Бялок (г)",
        "kk": "Ақуыз (г)",
        "ka": "ცილა (გ)",
        "es": "Proteína (g)",
        "fr": "Protéines (g)",
        "de": "Eiweiß (g)",
        "cs": "Bílkoviny (g)",
        "zh-Hans": "蛋白质（克）",
    },
)
t(
    "food_carbs_g",
    **{
        "en-GB": "Carbs (g)",
        "uk": "Вуглеводи (г)",
        "pl": "Węglowodany (g)",
        "be": "Вугляводы (г)",
        "kk": "Көмірсулар (г)",
        "ka": "ნახშირწყლები (გ)",
        "es": "Hidratos (g)",
        "fr": "Glucides (g)",
        "de": "Kohlenhydrate (g)",
        "cs": "Sacharidy (g)",
        "zh-Hans": "碳水（克）",
    },
)
t(
    "food_fat_g",
    **{
        "en-GB": "Fat (g)",
        "uk": "Жири (г)",
        "pl": "Tłuszcz (g)",
        "be": "Тлушчы (г)",
        "kk": "Май (г)",
        "ka": "ცხიმი (გ)",
        "es": "Grasa (g)",
        "fr": "Lipides (g)",
        "de": "Fett (g)",
        "cs": "Tuky (g)",
        "zh-Hans": "脂肪（克）",
    },
)
t(
    "food_add",
    **{
        "en-GB": "Add entry",
        "uk": "Додати запис",
        "pl": "Dodaj wpis",
        "be": "Дадаць запіс",
        "kk": "Жазба қосу",
        "ka": "ჩანაწერის დამატება",
        "es": "Añadir entrada",
        "fr": "Ajouter l’entrée",
        "de": "Eintrag hinzufügen",
        "cs": "Přidat záznam",
        "zh-Hans": "添加记录",
    },
)
t(
    "food_empty",
    **{
        "en-GB": "No food logged yet today.",
        "uk": "Сьогодні ще немає записів про їжу.",
        "pl": "Dziś nie zapisano jeszcze posiłków.",
        "be": "Сёння яшчэ няма запісаў пра ежу.",
        "kk": "Бүгін әлі тамақ жазылмаған.",
        "ka": "დღეს საკვები ჯერ არ არის ჩაწერილი.",
        "es": "Aún no hay comida registrada hoy.",
        "fr": "Aucun aliment enregistré aujourd’hui.",
        "de": "Heute noch keine Mahlzeiten erfasst.",
        "cs": "Dnes zatím není zaznamenáno žádné jídlo.",
        "zh-Hans": "今天尚未记录食物。",
    },
)
t(
    "workouts_library_title",
    **{
        "en-GB": "Exercise library",
        "uk": "Бібліотека вправ",
        "pl": "Biblioteka ćwiczeń",
        "be": "Бібліятэка практыкаванняў",
        "kk": "Жаттығулар кітапханасы",
        "ka": "ვარჯიშების ბიბლიოთეკა",
        "es": "Biblioteca de ejercicios",
        "fr": "Bibliothèque d’exercices",
        "de": "Übungsbibliothek",
        "cs": "Knihovna cviků",
        "zh-Hans": "动作库",
    },
)
t(
    "workouts_log_title",
    **{
        "en-GB": "Strength log",
        "uk": "Силовий журнал",
        "pl": "Dziennik siłowy",
        "be": "Сілавы журнал",
        "kk": "Күш журналы",
        "ka": "ძალის ჟურნალი",
        "es": "Registro de fuerza",
        "fr": "Journal de force",
        "de": "Kraftprotokoll",
        "cs": "Silový deník",
        "zh-Hans": "力量记录",
    },
)
t(
    "workouts_zone2_title",
    **{
        "en-GB": "Zone 2 run",
        "uk": "Біг у зоні 2",
        "pl": "Bieg w strefie 2",
        "be": "Бег у зоне 2",
        "kk": "2-аймақ жүгіру",
        "ka": "ზონა 2 სირბილი",
        "es": "Carrera en zona 2",
        "fr": "Course zone 2",
        "de": "Zone-2-Lauf",
        "cs": "Běh v zóně 2",
        "zh-Hans": "二区跑步",
    },
)
t(
    "workouts_zone2_body",
    **{
        "en-GB": "Start an easy Zone 2 continuous run for wellness aerobic work. Heart-rate bands are coaching estimates.",
        "uk": "Почніть легкий безперервний біг у зоні 2 для аеробної роботи. Зони пульсу — тренерські оцінки.",
        "pl": "Rozpocznij łatwy ciągły bieg w strefie 2 dla pracy tlenowej. Strefy tętna to szacunki coachingowe.",
        "be": "Пачніце лёгкі бесперапынны бег у зоне 2 для аэробнай працы. Зоны пульсу — трэнерскія ацэнкі.",
        "kk": "Аэробтық жұмыс үшін жеңіл 2-аймақ жүгіруін бастаңыз. Жүрек соғу аймақтары — жаттықтырушы бағалаулары.",
        "ka": "დაიწყეთ მსუბუქი ზონა 2 უწყვეტი სირბილი აერობული სამუშაოსთვის. გულისცემის ზონები მწვრთნელის შეფასებებია.",
        "es": "Inicia una carrera continua fácil en zona 2 para trabajo aeróbico. Las bandas de FC son estimaciones de coaching.",
        "fr": "Lancez une course continue facile en zone 2 pour le travail aérobie. Les zones FC sont des estimations d’entraînement.",
        "de": "Starten Sie einen leichten Zone-2-Dauerlauf für aerobes Wellness-Training. HF-Zonen sind Coaching-Schätzungen.",
        "cs": "Spusťte snadný souvislý běh v zóně 2 pro aerobní práci. Tepové zóny jsou trenérské odhady.",
        "zh-Hans": "开始轻松的二区持续跑，作为有氧健康训练。心率区间为教练估算。",
    },
)
t(
    "workouts_zone2_start",
    **{
        "en-GB": "Start Zone 2",
        "uk": "Старт зони 2",
        "pl": "Start strefy 2",
        "be": "Старт зоны 2",
        "kk": "2-аймақты бастау",
        "ka": "ზონა 2-ის დაწყება",
        "es": "Iniciar zona 2",
        "fr": "Démarrer zone 2",
        "de": "Zone 2 starten",
        "cs": "Spustit zónu 2",
        "zh-Hans": "开始二区",
    },
)
t(
    "workouts_zone2_scheduled",
    **{
        "en-GB": "Zone 2 plan sent to Workout.",
        "uk": "План зони 2 надіслано в Workout.",
        "pl": "Plan strefy 2 wysłany do Workout.",
        "be": "План зоны 2 адпраўлены ў Workout.",
        "kk": "2-аймақ жоспары Workout-қа жіберілді.",
        "ka": "ზონა 2 გეგმა გაიგზავნა Workout-ში.",
        "es": "Plan de zona 2 enviado a Workout.",
        "fr": "Plan zone 2 envoyé à Workout.",
        "de": "Zone-2-Plan an Workout gesendet.",
        "cs": "Plán zóny 2 odeslán do Workout.",
        "zh-Hans": "二区计划已发送到 Workout。",
    },
)
t(
    "workouts_zone2_unavailable",
    **{
        "en-GB": "Could not schedule Zone 2 right now.",
        "uk": "Зараз не вдалося запланувати зону 2.",
        "pl": "Nie udało się zaplanować strefy 2.",
        "be": "Цяпер не атрымалася запланаваць зону 2.",
        "kk": "Қазір 2-аймақты жоспарлау мүмкін болмады.",
        "ka": "ზონა 2-ის დაგეგმვა ახლა ვერ მოხერხდა.",
        "es": "No se pudo programar la zona 2 ahora.",
        "fr": "Impossible de planifier la zone 2 pour le moment.",
        "de": "Zone 2 konnte gerade nicht geplant werden.",
        "cs": "Zónu 2 teď nelze naplánovat.",
        "zh-Hans": "暂时无法安排二区训练。",
    },
)
t(
    "workouts_zone2_flag_off",
    **{
        "en-GB": "WorkoutKit scheduling is off in this build. You can still train with the in-app watch Zone 2 session when available.",
        "uk": "Планування WorkoutKit у цій збірці вимкнено. Можна тренуватися з сесією зони 2 на годиннику, коли вона доступна.",
        "pl": "Planowanie WorkoutKit jest wyłączone w tej kompilacji. Nadal możesz trenować sesją strefy 2 na zegarku, gdy będzie dostępna.",
        "be": "Планаванне WorkoutKit у гэтай зборцы выключана. Можна трэніравацца з сесіяй зоны 2 на гадзінніку, калі яна даступная.",
        "kk": "Бұл құрастыруда WorkoutKit жоспарлау өшірулі. Қолжетімді болса, сағаттағы 2-аймақ сессиясымен жаттыға аласыз.",
        "ka": "ამ აგებაში WorkoutKit დაგეგმვა გამორთულია. ხელმისაწვდომობისას შეგიძლიათ საათის ზონა 2 სესიით ივარჯიშოთ.",
        "es": "La programación de WorkoutKit está desactivada en esta versión. Aún puedes entrenar con la sesión Zone 2 del reloj cuando esté disponible.",
        "fr": "La planification WorkoutKit est désactivée dans cette version. Vous pouvez toujours vous entraîner avec la session Zone 2 de la montre lorsqu’elle est disponible.",
        "de": "WorkoutKit-Planung ist in diesem Build aus. Sie können weiterhin mit der Zone-2-Session auf der Watch trainieren, wenn verfügbar.",
        "cs": "Plánování WorkoutKit je v tomto sestavení vypnuté. Stále můžete trénovat sezónou zóny 2 na hodinkách, až bude dostupná.",
        "zh-Hans": "此构建中未启用 WorkoutKit 排程。手表上的二区会话可用时仍可训练。",
    },
)
t(
    "recovery_intro",
    **{
        "en-GB": "Wellness recovery estimates from your profile and recent inputs. Not medical advice.",
        "uk": "Оцінки відновлення самопочуття з вашого профілю та недавніх даних. Не медична консультація.",
        "pl": "Szacunki regeneracji wellness z profilu i ostatnich danych. To nie porada medyczna.",
        "be": "Ацэнкі аднаўлення самаадчування з профілю і нядаўніх дадзеных. Не медыцынская кансультацыя.",
        "kk": "Профиль мен соңғы деректерден сауықтыру қалпына келу бағалаулары. Медициналық кеңес емес.",
        "ka": "კეთილდღეობის აღდგენის შეფასებები პროფილიდან და ბოლო მონაცემებიდან. სამედიცინო რჩევა არ არის.",
        "es": "Estimaciones de recuperación de bienestar desde tu perfil y entradas recientes. No es consejo médico.",
        "fr": "Estimations de récupération bien-être à partir de votre profil et des entrées récentes. Pas un avis médical.",
        "de": "Wellness-Erholungsschätzungen aus Profil und jüngsten Eingaben. Keine medizinische Beratung.",
        "cs": "Wellness odhady regenerace z profilu a nedávných vstupů. Nejde o lékařskou radu.",
        "zh-Hans": "根据个人资料与近期输入得出的恢复健康估算。不是医疗建议。",
    },
)
t(
    "recovery_estimates_title",
    **{
        "en-GB": "Today’s estimates",
        "uk": "Сьогоднішні оцінки",
        "pl": "Dzisiejsze szacunki",
        "be": "Сённяшнія ацэнкі",
        "kk": "Бүгінгі бағалаулар",
        "ka": "დღევანდელი შეფასებები",
        "es": "Estimaciones de hoy",
        "fr": "Estimations du jour",
        "de": "Heutige Schätzungen",
        "cs": "Dnešní odhady",
        "zh-Hans": "今日估算",
    },
)
t(
    "recovery_empty",
    **{
        "en-GB": "Complete your profile to see wellness estimates.",
        "uk": "Заповніть профіль, щоб бачити оцінки самопочуття.",
        "pl": "Uzupełnij profil, aby zobaczyć szacunki wellness.",
        "be": "Запоўніце профіль, каб бачыць ацэнкі самаадчування.",
        "kk": "Сауықтыру бағалауларын көру үшін профильді толтырыңыз.",
        "ka": "კეთილდღეობის შეფასებების სანახავად შეავსეთ პროფილი.",
        "es": "Completa tu perfil para ver estimaciones de bienestar.",
        "fr": "Complétez votre profil pour voir les estimations de bien-être.",
        "de": "Vervollständigen Sie Ihr Profil, um Wellness-Schätzungen zu sehen.",
        "cs": "Vyplňte profil, abyste viděli wellness odhady.",
        "zh-Hans": "完善个人资料以查看健康估算。",
    },
)
t(
    "planner_intro",
    **{
        "en-GB": "Plan workouts on-device and optionally schedule a Zone 2 run into Workout.",
        "uk": "Плануйте тренування на пристрої та за бажанням додайте біг у зоні 2 до Workout.",
        "pl": "Planuj treningi na urządzeniu i opcjonalnie zaplanuj bieg w strefie 2 w Workout.",
        "be": "Плануйце трэніроўкі на прыладзе і пры жаданні дадайце бег у зоне 2 у Workout.",
        "kk": "Құрылғыда жаттығуларды жоспарлаңыз және қаласаңыз 2-аймақ жүгіруін Workout-қа қосыңыз.",
        "ka": "დაგეგმეთ ვარჯიშები მოწყობილობაზე და სურვილისამებრ დაამატეთ ზონა 2 სირბილი Workout-ში.",
        "es": "Planifica entrenamientos en el dispositivo y, si quieres, programa una carrera zona 2 en Workout.",
        "fr": "Planifiez des séances sur l’appareil et, en option, une course zone 2 dans Workout.",
        "de": "Planen Sie Workouts auf dem Gerät und optional einen Zone-2-Lauf in Workout.",
        "cs": "Plánujte tréninky na zařízení a volitelně naplánujte běh v zóně 2 do Workout.",
        "zh-Hans": "在设备上规划训练，并可选择将二区跑安排到 Workout。",
    },
)
t(
    "planner_planned_title",
    **{
        "en-GB": "Planned workouts",
        "uk": "Заплановані тренування",
        "pl": "Zaplanowane treningi",
        "be": "Запланаваныя трэніроўкі",
        "kk": "Жоспарланған жаттығулар",
        "ka": "დაგეგმილი ვარჯიშები",
        "es": "Entrenamientos planificados",
        "fr": "Séances planifiées",
        "de": "Geplante Workouts",
        "cs": "Naplánované tréninky",
        "zh-Hans": "已计划训练",
    },
)
t(
    "planner_empty",
    **{
        "en-GB": "No planned workouts yet.",
        "uk": "Ще немає запланованих тренувань.",
        "pl": "Brak zaplanowanych treningów.",
        "be": "Пакуль няма запланаваных трэніровак.",
        "kk": "Әлі жоспарланған жаттығулар жоқ.",
        "ka": "დაგეგმილი ვარჯიშები ჯერ არ არის.",
        "es": "Aún no hay entrenamientos planificados.",
        "fr": "Aucune séance planifiée pour l’instant.",
        "de": "Noch keine geplanten Workouts.",
        "cs": "Zatím žádné naplánované tréninky.",
        "zh-Hans": "尚无已计划的训练。",
    },
)
t(
    "planner_seed_demo",
    **{
        "en-GB": "Add Zone 2 plan",
        "uk": "Додати план зони 2",
        "pl": "Dodaj plan strefy 2",
        "be": "Дадаць план зоны 2",
        "kk": "2-аймақ жоспарын қосу",
        "ka": "ზონა 2 გეგმის დამატება",
        "es": "Añadir plan zona 2",
        "fr": "Ajouter un plan zone 2",
        "de": "Zone-2-Plan hinzufügen",
        "cs": "Přidat plán zóny 2",
        "zh-Hans": "添加二区计划",
    },
)
t(
    "planner_demo_zone2_name",
    **{
        "en-GB": "Zone 2 easy run",
        "uk": "Легкий біг у зоні 2",
        "pl": "Łatwy bieg w strefie 2",
        "be": "Лёгкі бег у зоне 2",
        "kk": "2-аймақ жеңіл жүгіру",
        "ka": "ზონა 2 მსუბუქი სირბილი",
        "es": "Carrera fácil zona 2",
        "fr": "Course facile zone 2",
        "de": "Zone-2-Easy-Run",
        "cs": "Snadný běh v zóně 2",
        "zh-Hans": "二区轻松跑",
    },
)
t(
    "planner_demo_zone2_notes",
    **{
        "en-GB": "Continuous easy aerobic work for wellness.",
        "uk": "Безперервна легка аеробна робота для самопочуття.",
        "pl": "Ciągła łatwa praca tlenowa dla wellness.",
        "be": "Бесперапынная лёгкая аэробная праца для самаадчування.",
        "kk": "Сауықтыру үшін үздіксіз жеңіл аэробтық жұмыс.",
        "ka": "უწყვეტი მსუბუქი აერობული სამუშაო კეთილდღეობისთვის.",
        "es": "Trabajo aeróbico fácil continuo para bienestar.",
        "fr": "Travail aérobie facile continu pour le bien-être.",
        "de": "Kontinuierliche leichte aerobe Arbeit für Wellness.",
        "cs": "Souvislá snadná aerobní práce pro wellness.",
        "zh-Hans": "持续轻松有氧，用于健康管理。",
    },
)
t(
    "planner_schedule_title",
    **{
        "en-GB": "Schedule to Workout",
        "uk": "Запланувати в Workout",
        "pl": "Zaplanuj w Workout",
        "be": "Запланаваць у Workout",
        "kk": "Workout-қа жоспарлау",
        "ka": "Workout-ში დაგეგმვა",
        "es": "Programar en Workout",
        "fr": "Planifier dans Workout",
        "de": "In Workout planen",
        "cs": "Naplánovat do Workout",
        "zh-Hans": "安排到 Workout",
    },
)
t(
    "planner_schedule_body",
    **{
        "en-GB": "Send a Zone 2 continuous run into the system Workout app when scheduling is enabled.",
        "uk": "Надішліть безперервний біг у зоні 2 до системної програми Workout, коли планування увімкнено.",
        "pl": "Wyślij ciągły bieg w strefie 2 do systemowej aplikacji Workout, gdy planowanie jest włączone.",
        "be": "Адпраўце бесперапынны бег у зоне 2 у сістэмную праграму Workout, калі планаванне ўключана.",
        "kk": "Жоспарлау қосулы болса, 2-аймақ жүгіруін жүйелік Workout қолданбасына жіберіңіз.",
        "ka": "დაგეგმვის ჩართვისას გაგზავნეთ ზონა 2 უწყვეტი სირბილი სისტემურ Workout აპში.",
        "es": "Envía una carrera continua zona 2 a la app Workout del sistema cuando la programación esté activa.",
        "fr": "Envoyez une course continue zone 2 vers l’app Workout système lorsque la planification est activée.",
        "de": "Senden Sie einen Zone-2-Dauerlauf an die System-Workout-App, wenn Planung aktiv ist.",
        "cs": "Odešlete souvislý běh v zóně 2 do systémové aplikace Workout, když je plánování zapnuté.",
        "zh-Hans": "在启用排程时，将二区持续跑发送到系统 Workout 应用。",
    },
)
t(
    "planner_schedule_when",
    **{
        "en-GB": "Start time",
        "uk": "Час старту",
        "pl": "Czas startu",
        "be": "Час старту",
        "kk": "Басталу уақыты",
        "ka": "დაწყების დრო",
        "es": "Hora de inicio",
        "fr": "Heure de début",
        "de": "Startzeit",
        "cs": "Čas startu",
        "zh-Hans": "开始时间",
    },
)
t(
    "planner_schedule_zone2",
    **{
        "en-GB": "Schedule Zone 2",
        "uk": "Запланувати зону 2",
        "pl": "Zaplanuj strefę 2",
        "be": "Запланаваць зону 2",
        "kk": "2-аймақты жоспарлау",
        "ka": "ზონა 2-ის დაგეგმვა",
        "es": "Programar zona 2",
        "fr": "Planifier zone 2",
        "de": "Zone 2 planen",
        "cs": "Naplánovat zónu 2",
        "zh-Hans": "安排二区",
    },
)
t(
    "planner_schedule_ok",
    **{
        "en-GB": "Scheduled in Workout.",
        "uk": "Заплановано в Workout.",
        "pl": "Zaplanowano w Workout.",
        "be": "Запланавана ў Workout.",
        "kk": "Workout-та жоспарланды.",
        "ka": "დაგეგმილია Workout-ში.",
        "es": "Programado en Workout.",
        "fr": "Planifié dans Workout.",
        "de": "In Workout geplant.",
        "cs": "Naplánováno ve Workout.",
        "zh-Hans": "已在 Workout 中安排。",
    },
)
t(
    "planner_schedule_unavailable",
    **{
        "en-GB": "Scheduling unavailable right now.",
        "uk": "Планування зараз недоступне.",
        "pl": "Planowanie jest teraz niedostępne.",
        "be": "Планаванне зараз недаступнае.",
        "kk": "Жоспарлау қазір қолжетімсіз.",
        "ka": "დაგეგმვა ახლა მიუწვდომელია.",
        "es": "La programación no está disponible ahora.",
        "fr": "La planification est indisponible pour le moment.",
        "de": "Planung derzeit nicht verfügbar.",
        "cs": "Plánování teď není dostupné.",
        "zh-Hans": "暂时无法排程。",
    },
)
t(
    "profile_intro",
    **{
        "en-GB": "Edit the details from onboarding. Changes stay on this device.",
        "uk": "Редагуйте дані з онбордингу. Зміни залишаються на цьому пристрої.",
        "pl": "Edytuj dane z onboardingu. Zmiany zostają na tym urządzeniu.",
        "be": "Рэдагуйце дадзеныя з анбордынгу. Змены застаюцца на гэтай прыладзе.",
        "kk": "Онбординг деректерін өңдеңіз. Өзгерістер осы құрылғыда қалады.",
        "ka": "შეცვალეთ ონბორდინგის მონაცემები. ცვლილებები რჩება ამ მოწყობილობაზე.",
        "es": "Edita los datos del onboarding. Los cambios permanecen en este dispositivo.",
        "fr": "Modifiez les données d’intégration. Les changements restent sur cet appareil.",
        "de": "Bearbeiten Sie die Onboarding-Angaben. Änderungen bleiben auf diesem Gerät.",
        "cs": "Upravte údaje z onboardingu. Změny zůstanou na tomto zařízení.",
        "zh-Hans": "编辑新手引导中的信息。更改仅保存在本设备。",
    },
)
t(
    "profile_edit_title",
    **{
        "en-GB": "Your profile",
        "uk": "Ваш профіль",
        "pl": "Twój profil",
        "be": "Ваш профіль",
        "kk": "Сіздің профиліңіз",
        "ka": "თქვენი პროფილი",
        "es": "Tu perfil",
        "fr": "Votre profil",
        "de": "Ihr Profil",
        "cs": "Váš profil",
        "zh-Hans": "您的个人资料",
    },
)
t(
    "profile_save",
    **{
        "en-GB": "Save profile",
        "uk": "Зберегти профіль",
        "pl": "Zapisz profil",
        "be": "Захаваць профіль",
        "kk": "Профильді сақтау",
        "ka": "პროფილის შენახვა",
        "es": "Guardar perfil",
        "fr": "Enregistrer le profil",
        "de": "Profil speichern",
        "cs": "Uložit profil",
        "zh-Hans": "保存个人资料",
    },
)
t(
    "profile_saved",
    **{
        "en-GB": "Profile saved.",
        "uk": "Профіль збережено.",
        "pl": "Profil zapisany.",
        "be": "Профіль захаваны.",
        "kk": "Профиль сақталды.",
        "ka": "პროფილი შენახულია.",
        "es": "Perfil guardado.",
        "fr": "Profil enregistré.",
        "de": "Profil gespeichert.",
        "cs": "Profil uložen.",
        "zh-Hans": "个人资料已保存。",
    },
)


t(
    "workouts_program_title",
    **{
        "en-GB": "Strength programme",
        "uk": "Силова програма",
        "pl": "Program siłowy",
        "be": "Сілавая праграма",
        "kk": "Күш бағдарламасы",
        "ka": "ძალის პროგრამა",
        "es": "Programa de fuerza",
        "fr": "Programme de force",
        "de": "Kraftprogramm",
        "cs": "Silový program",
        "zh-Hans": "力量计划",
    },
)

t(
    "workouts_program_intro",
    **{
        "en-GB": "Build a full-body or split plan from your equipment. Coaching aid only — not medical advice.",
        "uk": "Складіть план на все тіло або спліт з наявного обладнання. Лише тренерська допомога — не медична порада.",
        "pl": "Ułóż plan całego ciała lub split z dostępnego sprzętu. To wskazówka treningowa, nie porada medyczna.",
        "be": "Складзіце план на ўсё цела або спліт з наяўнага абсталявання. Толькі трэнерская дапамога — не медыцынская парада.",
        "kk": "Қолдағы жабдықтан толық дене немесе сплит жоспарын жасаңыз. Тек жаттықтырушылық көмек — медициналық кеңес емес.",
        "ka": "ააწყვეთ მთელი სხეულის ან სპლიტის გეგმა თქვენი აღჭურვილობით. მხოლოდ მწვრთნელის დახმარებაა — არა სამედიცინო რჩევა.",
        "es": "Crea un plan de cuerpo completo o dividido con tu material. Solo ayuda de entrenamiento, no consejo médico.",
        "fr": "Créez un plan full-body ou split selon votre matériel. Aide d’entraînement uniquement — pas un avis médical.",
        "de": "Erstellen Sie einen Ganzkörper- oder Split-Plan aus Ihrem Gerät. Nur Trainingshilfe — keine medizinische Beratung.",
        "cs": "Sestavte celotělový nebo split plán podle vybavení. Jen tréninková pomůcka — ne lékařská rada.",
        "zh-Hans": "根据现有器械生成全身或分化计划。仅供训练参考，不是医疗建议。",
    },
)

t(
    "workouts_program_split",
    **{
        "en-GB": "Programme type",
        "uk": "Тип програми",
        "pl": "Typ programu",
        "be": "Тып праграмы",
        "kk": "Бағдарлама түрі",
        "ka": "პროგრამის ტიპი",
        "es": "Tipo de programa",
        "fr": "Type de programme",
        "de": "Programmtyp",
        "cs": "Typ programu",
        "zh-Hans": "计划类型",
    },
)

t(
    "workouts_program_days",
    **{
        "en-GB": "Days per week",
        "uk": "Днів на тиждень",
        "pl": "Dni w tygodniu",
        "be": "Дзён на тыдзень",
        "kk": "Аптасына күн",
        "ka": "დღე კვირაში",
        "es": "Días por semana",
        "fr": "Jours par semaine",
        "de": "Tage pro Woche",
        "cs": "Dní týdně",
        "zh-Hans": "每周天数",
    },
)

t(
    "workouts_equipment_title",
    **{
        "en-GB": "Equipment",
        "uk": "Обладнання",
        "pl": "Sprzęt",
        "be": "Абсталяванне",
        "kk": "Жабдық",
        "ka": "აღჭურვილობა",
        "es": "Material",
        "fr": "Matériel",
        "de": "Geräte",
        "cs": "Vybavení",
        "zh-Hans": "器械",
    },
)

t(
    "workouts_equipment_edit",
    **{
        "en-GB": "Edit equipment",
        "uk": "Редагувати обладнання",
        "pl": "Edytuj sprzęt",
        "be": "Рэдагаваць абсталяванне",
        "kk": "Жабдықты өңдеу",
        "ka": "აღჭურვილობის რედაქტირება",
        "es": "Editar material",
        "fr": "Modifier le matériel",
        "de": "Geräte bearbeiten",
        "cs": "Upravit vybavení",
        "zh-Hans": "编辑器械",
    },
)

t(
    "workouts_equipment_presets",
    **{
        "en-GB": "Presets",
        "uk": "Пресети",
        "pl": "Presety",
        "be": "Пресеты",
        "kk": "Алдын ала орнатулар",
        "ka": "პრესეტები",
        "es": "Preajustes",
        "fr": "Préréglages",
        "de": "Voreinstellungen",
        "cs": "Předvolby",
        "zh-Hans": "预设",
    },
)

t(
    "equipment_preset_home",
    **{
        "en-GB": "Home",
        "uk": "Дім",
        "pl": "Dom",
        "be": "Дом",
        "kk": "Үй",
        "ka": "სახლი",
        "es": "Casa",
        "fr": "Maison",
        "de": "Zuhause",
        "cs": "Domov",
        "zh-Hans": "居家",
    },
)

t(
    "equipment_preset_gym",
    **{
        "en-GB": "Gym",
        "uk": "Зал",
        "pl": "Siłownia",
        "be": "Зала",
        "kk": "Зал",
        "ka": "დარბაზი",
        "es": "Gimnasio",
        "fr": "Salle",
        "de": "Fitnessstudio",
        "cs": "Posilovna",
        "zh-Hans": "健身房",
    },
)

t(
    "equipment_preset_travel",
    **{
        "en-GB": "Travel",
        "uk": "Подорож",
        "pl": "Podróż",
        "be": "Падарожжа",
        "kk": "Саяхат",
        "ka": "მოგზაურობა",
        "es": "Viaje",
        "fr": "Voyage",
        "de": "Reise",
        "cs": "Cestování",
        "zh-Hans": "旅行",
    },
)

t(
    "equipment_preset_save_home",
    **{
        "en-GB": "Save as Home",
        "uk": "Зберегти як Дім",
        "pl": "Zapisz jako Dom",
        "be": "Захаваць як Дом",
        "kk": "Үй ретінде сақтау",
        "ka": "სახლად შენახვა",
        "es": "Guardar como Casa",
        "fr": "Enregistrer comme Maison",
        "de": "Als Zuhause speichern",
        "cs": "Uložit jako Domov",
        "zh-Hans": "保存为居家",
    },
)

t(
    "workouts_week_title",
    **{
        "en-GB": "This week",
        "uk": "Цей тиждень",
        "pl": "Ten tydzień",
        "be": "Гэты тыдзень",
        "kk": "Осы апта",
        "ka": "ამ კვირაში",
        "es": "Esta semana",
        "fr": "Cette semaine",
        "de": "Diese Woche",
        "cs": "Tento týden",
        "zh-Hans": "本周",
    },
)

t(
    "workouts_week_day",
    **{
        "en-GB": "Session",
        "uk": "Сесія",
        "pl": "Sesja",
        "be": "Сесія",
        "kk": "Сессия",
        "ka": "სესია",
        "es": "Sesión",
        "fr": "Séance",
        "de": "Einheit",
        "cs": "Jednotka",
        "zh-Hans": "课次",
    },
)

t(
    "workouts_muscles_title",
    **{
        "en-GB": "Muscles worked",
        "uk": "Задіяні м’язи",
        "pl": "Zaangażowane mięśnie",
        "be": "Задзейнічаныя мышцы",
        "kk": "Жұмыс істейтін бұлшықеттер",
        "ka": "მუშა კუნთები",
        "es": "Músculos trabajados",
        "fr": "Muscles sollicités",
        "de": "Beanspruchte Muskeln",
        "cs": "Zapojené svaly",
        "zh-Hans": "训练肌群",
    },
)

t(
    "workouts_adjust_equipment",
    **{
        "en-GB": "Adjust equipment",
        "uk": "Підлаштувати обладнання",
        "pl": "Dostosuj sprzęt",
        "be": "Падладзіць абсталяванне",
        "kk": "Жабдықты бейімдеу",
        "ka": "აღჭურვილობის მორგება",
        "es": "Ajustar material",
        "fr": "Ajuster le matériel",
        "de": "Geräte anpassen",
        "cs": "Upravit vybavení",
        "zh-Hans": "调整器械",
    },
)

t(
    "workouts_substituted",
    **{
        "en-GB": "Substituted",
        "uk": "Замінено",
        "pl": "Zastąpiono",
        "be": "Заменена",
        "kk": "Ауыстырылды",
        "ka": "შეცვლილია",
        "es": "Sustituido",
        "fr": "Remplacé",
        "de": "Ersetzt",
        "cs": "Nahrazeno",
        "zh-Hans": "已替换",
    },
)

t(
    "watch_strength_start",
    **{
        "en-GB": "Strength session",
        "uk": "Силова сесія",
        "pl": "Sesja siłowa",
        "be": "Сілавая сесія",
        "kk": "Күш сессиясы",
        "ka": "ძალის სესია",
        "es": "Sesión de fuerza",
        "fr": "Séance de force",
        "de": "Krafteinheit",
        "cs": "Silová jednotka",
        "zh-Hans": "力量课次",
    },
)

t(
    "watch_equipment_sync_hint",
    **{
        "en-GB": "Toggles sync from iPhone. Changes apply to this session only.",
        "uk": "Перемикачі синхронізуються з iPhone. Зміни лише для цієї сесії.",
        "pl": "Przełączniki synchronizują się z iPhone’a. Zmiany tylko dla tej sesji.",
        "be": "Пераключальнікі сінхранізуюцца з iPhone. Змены толькі для гэтай сесіі.",
        "kk": "Ауыстырғыштар iPhone-нан синхрондалады. Өзгерістер тек осы сессияға.",
        "ka": "გადამრთველები სინქრონდება iPhone-დან. ცვლილებები მხოლოდ ამ სესიისთვის.",
        "es": "Los interruptores se sincronizan desde el iPhone. Solo para esta sesión.",
        "fr": "Les bascules se synchronisent depuis l’iPhone. Uniquement pour cette séance.",
        "de": "Schalter synchronisieren vom iPhone. Nur für diese Einheit.",
        "cs": "Přepínače se synchronizují z iPhonu. Jen pro tuto jednotku.",
        "zh-Hans": "开关与 iPhone 同步。仅作用于本次课次。",
    },
)

t(
    "program_split_full_body",
    **{
        "en-GB": "Full body",
        "uk": "Все тіло",
        "pl": "Całe ciało",
        "be": "Усё цела",
        "kk": "Толық дене",
        "ka": "მთელი სხეული",
        "es": "Cuerpo completo",
        "fr": "Full body",
        "de": "Ganzkörper",
        "cs": "Celé tělo",
        "zh-Hans": "全身",
    },
)

t(
    "program_split_upper_lower",
    **{
        "en-GB": "Upper / lower",
        "uk": "Верх / низ",
        "pl": "Góra / dół",
        "be": "Верх / ніз",
        "kk": "Жоғарғы / төменгі",
        "ka": "ზედა / ქვედა",
        "es": "Superior / inferior",
        "fr": "Haut / bas",
        "de": "Ober- / Unterkörper",
        "cs": "Horní / dolní",
        "zh-Hans": "上下肢分化",
    },
)

t(
    "program_split_ppl",
    **{
        "en-GB": "Push / pull / legs",
        "uk": "Жим / тяга / ноги",
        "pl": "Push / pull / nogi",
        "be": "Жым / цяга / ногі",
        "kk": "Жим / тарту / аяқ",
        "ka": "ბიძგი / თხრა / ფეხები",
        "es": "Empuje / tirón / piernas",
        "fr": "Push / pull / jambes",
        "de": "Push / Pull / Beine",
        "cs": "Tlaky / tahy / nohy",
        "zh-Hans": "推/拉/腿",
    },
)

t(
    "program_split_bro",
    **{
        "en-GB": "Bro split",
        "uk": "Бро-спліт",
        "pl": "Bro split",
        "be": "Бро-спліт",
        "kk": "Бро-сплит",
        "ka": "ბრო-სპლიტი",
        "es": "Bro split",
        "fr": "Bro split",
        "de": "Bro-Split",
        "cs": "Bro split",
        "zh-Hans": "肌肉分化",
    },
)

t(
    "program_session_full_a",
    **{
        "en-GB": "Full body A",
        "uk": "Все тіло A",
        "pl": "Całe ciało A",
        "be": "Усё цела A",
        "kk": "Толық дене A",
        "ka": "მთელი სხეული A",
        "es": "Cuerpo completo A",
        "fr": "Full body A",
        "de": "Ganzkörper A",
        "cs": "Celé tělo A",
        "zh-Hans": "全身 A",
    },
)

t(
    "program_session_full_b",
    **{
        "en-GB": "Full body B",
        "uk": "Все тіло B",
        "pl": "Całe ciało B",
        "be": "Усё цела B",
        "kk": "Толық дене B",
        "ka": "მთელი სხეული B",
        "es": "Cuerpo completo B",
        "fr": "Full body B",
        "de": "Ganzkörper B",
        "cs": "Celé tělo B",
        "zh-Hans": "全身 B",
    },
)

t(
    "program_session_full_c",
    **{
        "en-GB": "Full body C",
        "uk": "Все тіло C",
        "pl": "Całe ciało C",
        "be": "Усё цела C",
        "kk": "Толық дене C",
        "ka": "მთელი სხეული C",
        "es": "Cuerpo completo C",
        "fr": "Full body C",
        "de": "Ganzkörper C",
        "cs": "Celé tělo C",
        "zh-Hans": "全身 C",
    },
)

t(
    "program_session_upper_a",
    **{
        "en-GB": "Upper A",
        "uk": "Верх A",
        "pl": "Góra A",
        "be": "Верх A",
        "kk": "Жоғарғы A",
        "ka": "ზედა A",
        "es": "Superior A",
        "fr": "Haut A",
        "de": "Oberkörper A",
        "cs": "Horní A",
        "zh-Hans": "上肢 A",
    },
)

t(
    "program_session_upper_b",
    **{
        "en-GB": "Upper B",
        "uk": "Верх B",
        "pl": "Góra B",
        "be": "Верх B",
        "kk": "Жоғарғы B",
        "ka": "ზედა B",
        "es": "Superior B",
        "fr": "Haut B",
        "de": "Oberkörper B",
        "cs": "Horní B",
        "zh-Hans": "上肢 B",
    },
)

t(
    "program_session_lower_a",
    **{
        "en-GB": "Lower A",
        "uk": "Низ A",
        "pl": "Dół A",
        "be": "Ніз A",
        "kk": "Төменгі A",
        "ka": "ქვედა A",
        "es": "Inferior A",
        "fr": "Bas A",
        "de": "Unterkörper A",
        "cs": "Dolní A",
        "zh-Hans": "下肢 A",
    },
)

t(
    "program_session_lower_b",
    **{
        "en-GB": "Lower B",
        "uk": "Низ B",
        "pl": "Dół B",
        "be": "Ніз B",
        "kk": "Төменгі B",
        "ka": "ქვედა B",
        "es": "Inferior B",
        "fr": "Bas B",
        "de": "Unterkörper B",
        "cs": "Dolní B",
        "zh-Hans": "下肢 B",
    },
)

t(
    "program_session_push",
    **{
        "en-GB": "Push",
        "uk": "Жим",
        "pl": "Push",
        "be": "Жым",
        "kk": "Жим",
        "ka": "ბიძგი",
        "es": "Empuje",
        "fr": "Push",
        "de": "Push",
        "cs": "Tlaky",
        "zh-Hans": "推",
    },
)

t(
    "program_session_pull",
    **{
        "en-GB": "Pull",
        "uk": "Тяга",
        "pl": "Pull",
        "be": "Цяга",
        "kk": "Тарту",
        "ka": "თხრა",
        "es": "Tirón",
        "fr": "Pull",
        "de": "Pull",
        "cs": "Tahy",
        "zh-Hans": "拉",
    },
)

t(
    "program_session_legs",
    **{
        "en-GB": "Legs",
        "uk": "Ноги",
        "pl": "Nogi",
        "be": "Ногі",
        "kk": "Аяқ",
        "ka": "ფეხები",
        "es": "Piernas",
        "fr": "Jambes",
        "de": "Beine",
        "cs": "Nohy",
        "zh-Hans": "腿",
    },
)

t(
    "program_session_chest",
    **{
        "en-GB": "Chest",
        "uk": "Груди",
        "pl": "Klatka",
        "be": "Грудзі",
        "kk": "Кеуде",
        "ka": "მკერდი",
        "es": "Pecho",
        "fr": "Poitrine",
        "de": "Brust",
        "cs": "Hruď",
        "zh-Hans": "胸",
    },
)

t(
    "program_session_back",
    **{
        "en-GB": "Back",
        "uk": "Спина",
        "pl": "Plecy",
        "be": "Спіна",
        "kk": "Арқа",
        "ka": "ზურგი",
        "es": "Espalda",
        "fr": "Dos",
        "de": "Rücken",
        "cs": "Záda",
        "zh-Hans": "背",
    },
)

t(
    "program_session_shoulders",
    **{
        "en-GB": "Shoulders",
        "uk": "Плечі",
        "pl": "Barki",
        "be": "Плечы",
        "kk": "Иық",
        "ka": "მხრები",
        "es": "Hombros",
        "fr": "Épaules",
        "de": "Schultern",
        "cs": "Ramena",
        "zh-Hans": "肩",
    },
)

t(
    "program_session_arms",
    **{
        "en-GB": "Arms",
        "uk": "Руки",
        "pl": "Ręce",
        "be": "Рукі",
        "kk": "Қол",
        "ka": "ხელები",
        "es": "Brazos",
        "fr": "Bras",
        "de": "Arme",
        "cs": "Paže",
        "zh-Hans": "手臂",
    },
)

t(
    "equipment_item_pull_up_bar",
    **{
        "en-GB": "Pull-up bar",
        "uk": "Турнік",
        "pl": "Drążek",
        "be": "Турнік",
        "kk": "Турник",
        "ka": "ტურნიკი",
        "es": "Barra de dominadas",
        "fr": "Barre de traction",
        "de": "Klimmzugstange",
        "cs": "Hrazda",
        "zh-Hans": "引体向上杆",
    },
)

t(
    "equipment_item_dip_bars",
    **{
        "en-GB": "Dip bars",
        "uk": "Бруси",
        "pl": "Poręcze",
        "be": "Брусы",
        "kk": "Брусья",
        "ka": "ბარზეები",
        "es": "Barras paralelas",
        "fr": "Barres parallèles",
        "de": "Barren",
        "cs": "Bradla",
        "zh-Hans": "双杠",
    },
)

t(
    "equipment_item_bands",
    **{
        "en-GB": "Resistance bands",
        "uk": "Гумові петлі",
        "pl": "Gumy",
        "be": "Гумовыя стужкі",
        "kk": "Резеңке таспалар",
        "ka": "რეზინის ლენტები",
        "es": "Bandas elásticas",
        "fr": "Élastiques",
        "de": "Widerstandsbänder",
        "cs": "Odporové gumy",
        "zh-Hans": "弹力带",
    },
)

t(
    "equipment_item_dumbbell",
    **{
        "en-GB": "Dumbbells",
        "uk": "Гантелі",
        "pl": "Hantle",
        "be": "Гантэлі",
        "kk": "Гантельдер",
        "ka": "ჰანტელები",
        "es": "Mancuernas",
        "fr": "Haltères",
        "de": "Kurzhanteln",
        "cs": "Jednoručky",
        "zh-Hans": "哑铃",
    },
)

t(
    "equipment_item_kettlebell",
    **{
        "en-GB": "Kettlebells",
        "uk": "Гирі",
        "pl": "Kettlebell",
        "be": "Гірлі",
        "kk": "Гирялар",
        "ka": "გირიები",
        "es": "Pesas rusas",
        "fr": "Kettlebells",
        "de": "Kettlebells",
        "cs": "Kettlebelly",
        "zh-Hans": "壶铃",
    },
)

t(
    "equipment_item_barbell",
    **{
        "en-GB": "Barbell",
        "uk": "Штанга",
        "pl": "Sztanga",
        "be": "Штанга",
        "kk": "Штанга",
        "ka": "შტანგა",
        "es": "Barra",
        "fr": "Barre",
        "de": "Langhantel",
        "cs": "Osa",
        "zh-Hans": "杠铃",
    },
)

t(
    "equipment_item_plates",
    **{
        "en-GB": "Weight plates",
        "uk": "Диски",
        "pl": "Talerze",
        "be": "Дыскі",
        "kk": "Дискілер",
        "ka": "დისკები",
        "es": "Discos",
        "fr": "Disques",
        "de": "Hantelscheiben",
        "cs": "Kotouče",
        "zh-Hans": "杠铃片",
    },
)

t(
    "equipment_item_bench_flat",
    **{
        "en-GB": "Flat bench",
        "uk": "Горизонтальна лава",
        "pl": "Ławka płaska",
        "be": "Гарызантальная лава",
        "kk": "Жазық орындық",
        "ka": "ბრტყელი სკამი",
        "es": "Banco plano",
        "fr": "Banc plat",
        "de": "Flachbank",
        "cs": "Rovná lavice",
        "zh-Hans": "平板凳",
    },
)

t(
    "equipment_item_bench_adjustable",
    **{
        "en-GB": "Adjustable bench",
        "uk": "Регульована лава",
        "pl": "Ławka regulowana",
        "be": "Рэгуляваная лава",
        "kk": "Реттелетін орындық",
        "ka": "რეგულირებადი სკამი",
        "es": "Banco ajustable",
        "fr": "Banc réglable",
        "de": "Verstellbare Bank",
        "cs": "Nastavitelná lavice",
        "zh-Hans": "可调节凳",
    },
)

t(
    "equipment_item_squat_rack",
    **{
        "en-GB": "Squat rack",
        "uk": "Стійка для присідань",
        "pl": "Stojak",
        "be": "Стойка",
        "kk": "Скватка",
        "ka": "სკვატის თარო",
        "es": "Rack de sentadillas",
        "fr": "Cage à squat",
        "de": "Kniebeugenständer",
        "cs": "Dřepový stojan",
        "zh-Hans": "深蹲架",
    },
)

t(
    "equipment_item_cable",
    **{
        "en-GB": "Cable machine",
        "uk": "Блоковий тренажер",
        "pl": "Wyciąg",
        "be": "Блочны трэнажор",
        "kk": "Блок тренажері",
        "ka": "ბლოკური ტრენაჟორი",
        "es": "Polea",
        "fr": "Poulie",
        "de": "Kabelzug",
        "cs": "Kladka",
        "zh-Hans": "绳索器械",
    },
)

t(
    "equipment_item_smith_machine",
    **{
        "en-GB": "Smith machine",
        "uk": "Машина Сміта",
        "pl": "Maszyna Smitha",
        "be": "Машына Сміта",
        "kk": "Смит машинасы",
        "ka": "სმიტის მანქანა",
        "es": "Máquina Smith",
        "fr": "Machine Smith",
        "de": "Smith-Maschine",
        "cs": "Smithův stroj",
        "zh-Hans": "史密斯机",
    },
)

t(
    "equipment_item_leg_press",
    **{
        "en-GB": "Leg press",
        "uk": "Жим ногами",
        "pl": "Wyciskanie nogami",
        "be": "Ціск нагамі",
        "kk": "Аяқпен жим",
        "ka": "ფეხების პრესი",
        "es": "Prensa de piernas",
        "fr": "Presse à cuisses",
        "de": "Beinpresse",
        "cs": "Leg press",
        "zh-Hans": "腿举",
    },
)

t(
    "equipment_item_lat_pulldown",
    **{
        "en-GB": "Lat pulldown",
        "uk": "Верхня тяга",
        "pl": "Wyciąg górny",
        "be": "Верхняя цяга",
        "kk": "Жоғары тарту",
        "ka": "ზედა თხრილი",
        "es": "Jalón al pecho",
        "fr": "Tirage vertical",
        "de": "Latzug",
        "cs": "Stahování kladky",
        "zh-Hans": "高位下拉",
    },
)

t(
    "equipment_item_assisted_pullup_dip_machine",
    **{
        "en-GB": "Assisted pull-up / dip machine",
        "uk": "Гравітрон",
        "pl": "Maszyna z pomocą",
        "be": "Трэнажор з процівагай",
        "kk": "Қарсы салмақ тренажері",
        "ka": "დამხმარე მანქანა",
        "es": "Máquina asistida",
        "fr": "Machine assistée",
        "de": "Assistenzmaschine",
        "cs": "Stroj s výpomocí",
        "zh-Hans": "辅助引体/双杠器械",
    },
)

t(
    "equipment_item_machine",
    **{
        "en-GB": "Resistance machines",
        "uk": "Тренажери",
        "pl": "Maszyny",
        "be": "Трэнажоры",
        "kk": "Тренажерлер",
        "ka": "ტრენაჟორები",
        "es": "Máquinas",
        "fr": "Machines",
        "de": "Geräte",
        "cs": "Stroje",
        "zh-Hans": "力量器械",
    },
)

t(
    "equipment_item_trap_bar",
    **{
        "en-GB": "Trap bar",
        "uk": "Трап-гриф",
        "pl": "Trap bar",
        "be": "Трап-грыф",
        "kk": "Трап-гриф",
        "ka": "ტრაპ-გრიფი",
        "es": "Barra trap",
        "fr": "Barre trap",
        "de": "Trap Bar",
        "cs": "Trap bar",
        "zh-Hans": "六角杠",
    },
)

t(
    "equipment_item_ez_bar",
    **{
        "en-GB": "EZ bar",
        "uk": "EZ-гриф",
        "pl": "Gryf EZ",
        "be": "EZ-грыф",
        "kk": "EZ-гриф",
        "ka": "EZ-გრიფი",
        "es": "Barra EZ",
        "fr": "Barre EZ",
        "de": "SZ-Stange",
        "cs": "EZ osa",
        "zh-Hans": "曲杆",
    },
)

t(
    "equipment_item_suspension",
    **{
        "en-GB": "Suspension trainer",
        "uk": "Підвісні петлі",
        "pl": "Taśmy podwieszane",
        "be": "Падвесныя стужкі",
        "kk": "Аспалы таспалар",
        "ka": "ჩამოკიდებული ლენტები",
        "es": "Suspensión",
        "fr": "Suspension",
        "de": "Schlingentrainer",
        "cs": "Závěsný trenér",
        "zh-Hans": "悬挂训练器",
    },
)

t(
    "equipment_item_box",
    **{
        "en-GB": "Plyometric box",
        "uk": "Тумба",
        "pl": "Box",
        "be": "Тумба",
        "kk": "Тумба",
        "ka": "ტუმბა",
        "es": "Cajón",
        "fr": "Box",
        "de": "Sprungbox",
        "cs": "Box",
        "zh-Hans": "跳箱",
    },
)

t(
    "equipment_item_landmine",
    **{
        "en-GB": "Landmine",
        "uk": "Лендмайн",
        "pl": "Landmine",
        "be": "Лендмайн",
        "kk": "Лендмайн",
        "ka": "ლენდმაინი",
        "es": "Landmine",
        "fr": "Landmine",
        "de": "Landmine",
        "cs": "Landmine",
        "zh-Hans": "地雷架",
    },
)

t(
    "equipment_item_yoga_mat",
    **{
        "en-GB": "Yoga mat",
        "uk": "Килімок для йоги",
        "pl": "Mata do jogi",
        "be": "Дыванок для ёгі",
        "kk": "Йога төсеніші",
        "ka": "იოგას საგები",
        "es": "Esterilla de yoga",
        "fr": "Tapis de yoga",
        "de": "Yogamatte",
        "cs": "Podložka na jógu",
        "zh-Hans": "瑜伽垫",
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
    print(f"Synced {len(STRINGS)} tab keys × {len(LOCALES)} locales")


if __name__ == "__main__":
    main()
