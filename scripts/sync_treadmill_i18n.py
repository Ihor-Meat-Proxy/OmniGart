#!/usr/bin/env python3
"""Sync Treadmill run UI strings (11 locales) into JSON, xcstrings, Android, Wear."""

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
    "treadmill_run_title",
    **{
        "en-GB": "Treadmill run",
        "uk": "Біг на доріжці",
        "pl": "Bieg na bieżni",
        "be": "Бег на дарожцы",
        "kk": "Жүгіру жолы",
        "ka": "სარბენი ბილიკი",
        "es": "Carrera en cinta",
        "fr": "Course tapis",
        "de": "Laufband-Lauf",
        "cs": "Běh na pásu",
        "zh-Hans": "跑步机跑步",
    },
)
t(
    "treadmill_run_body",
    **{
        "en-GB": "Connect a compatible treadmill for live speed, incline, and distance. Without a link, the watch estimates distance — you can calibrate after.",
        "uk": "Підключіть сумісну доріжку для живої швидкості, нахилу та дистанції. Без зв’язку годинник оцінює дистанцію — можна відкалібрувати після.",
        "pl": "Połącz zgodną bieżnię, by widzieć prędkość, nachylenie i dystans. Bez połączenia zegarek szacuje dystans — możesz skalibrować później.",
        "be": "Падключыце сумяшчальную дарожку для хуткасці, нахілу і дыстанцыі. Без сувязі гадзіннік ацэньвае дыстанцыю — можна адкалібраваць пасля.",
        "kk": "Жылдамдық, еңіс және қашықтық үшін үйлесімді жүгіру жолын қосыңыз. Байланыс болмаса сағат қашықтықты бағалайды — кейін калибрлеуге болады.",
        "ka": "დააკავშირეთ თავსებადი სარბენი ბილიკი სიჩქარის, დახრისა და დისტანციისთვის. კავშირის გარეშე საათი აფასებს დისტანციას — შემდეგ შეგიძლიათ დააკალიბროთ.",
        "es": "Conecta una cinta compatible para ver velocidad, inclinación y distancia. Sin enlace, el reloj estima la distancia; puedes calibrar después.",
        "fr": "Connectez un tapis compatible pour la vitesse, la pente et la distance. Sans lien, la montre estime la distance — calibrage possible après.",
        "de": "Verbinde ein kompatibles Laufband für Tempo, Steigung und Distanz. Ohne Verbindung schätzt die Watch die Distanz — danach kalibrierbar.",
        "cs": "Připojte kompatibilní pás pro rychlost, sklon a vzdálenost. Bez spojení hodinky odhadnou vzdálenost — poté můžete kalibrovat.",
        "zh-Hans": "连接兼容跑步机以查看速度、坡度和距离。未连接时由手表估算距离，结束后可校准。",
    },
)
t(
    "treadmill_run_open",
    **{
        "en-GB": "Open Treadmill run",
        "uk": "Відкрити біг на доріжці",
        "pl": "Otwórz bieg na bieżni",
        "be": "Адкрыць бег на дарожцы",
        "kk": "Жүгіру жолын ашу",
        "ka": "სარბენი ბილიკის გახსნა",
        "es": "Abrir carrera en cinta",
        "fr": "Ouvrir course tapis",
        "de": "Laufband-Lauf öffnen",
        "cs": "Otevřít běh na pásu",
        "zh-Hans": "打开跑步机跑步",
    },
)
t(
    "treadmill_connection_title",
    **{
        "en-GB": "Treadmill",
        "uk": "Доріжка",
        "pl": "Bieżnia",
        "be": "Дарожка",
        "kk": "Жүгіру жолы",
        "ka": "სარბენი ბილიკი",
        "es": "Cinta",
        "fr": "Tapis",
        "de": "Laufband",
        "cs": "Pás",
        "zh-Hans": "跑步机",
    },
)
t(
    "treadmill_scan",
    **{
        "en-GB": "Scan for treadmills",
        "uk": "Шукати доріжки",
        "pl": "Szukaj bieżni",
        "be": "Шукаць дарожкі",
        "kk": "Жүгіру жолдарын іздеу",
        "ka": "სარბენი ბილიკების ძიება",
        "es": "Buscar cintas",
        "fr": "Rechercher des tapis",
        "de": "Laufbänder suchen",
        "cs": "Hledat pásy",
        "zh-Hans": "扫描跑步机",
    },
)
t(
    "treadmill_reconnect",
    **{
        "en-GB": "Reconnect saved treadmill",
        "uk": "Перез’єднати збережену доріжку",
        "pl": "Połącz ponownie zapisaną bieżnię",
        "be": "Перазлучыць захаваную дарожку",
        "kk": "Сақталған жүгіру жолын қайта қосу",
        "ka": "შენახული სარბენი ბილიკის ხელახალი კავშირი",
        "es": "Reconectar cinta guardada",
        "fr": "Reconnecter le tapis enregistré",
        "de": "Gespeichertes Laufband erneut verbinden",
        "cs": "Znovu připojit uložený pás",
        "zh-Hans": "重新连接已保存的跑步机",
    },
)
t(
    "treadmill_allow_control",
    **{
        "en-GB": "Allow remote speed and incline (opt-in)",
        "uk": "Дозволити дистанційну швидкість і нахил (за згодою)",
        "pl": "Zezwól na zdalne tempo i nachylenie (opcjonalnie)",
        "be": "Дазволіць аддаленую хуткасць і нахіл (па жаданні)",
        "kk": "Қашықтан жылдамдық пен еңіске рұқсат (қалау бойынша)",
        "ka": "დისტანციური სიჩქარისა და დახრის დაშვება (არჩევით)",
        "es": "Permitir velocidad e inclinación remotas (opcional)",
        "fr": "Autoriser vitesse et pente à distance (optionnel)",
        "de": "Fernsteuerung von Tempo und Steigung erlauben (opt-in)",
        "cs": "Povolit vzdálenou rychlost a sklon (volitelně)",
        "zh-Hans": "允许远程调节速度和坡度（需开启）",
    },
)
t(
    "treadmill_live_title",
    **{
        "en-GB": "Live",
        "uk": "Наживо",
        "pl": "Na żywo",
        "be": "Ужывую",
        "kk": "Тікелей",
        "ka": "ცოცხალი",
        "es": "En vivo",
        "fr": "En direct",
        "de": "Live",
        "cs": "Živě",
        "zh-Hans": "实时",
    },
)
t(
    "treadmill_speed",
    **{
        "en-GB": "Speed",
        "uk": "Швидкість",
        "pl": "Prędkość",
        "be": "Хуткасць",
        "kk": "Жылдамдық",
        "ka": "სიჩქარე",
        "es": "Velocidad",
        "fr": "Vitesse",
        "de": "Tempo",
        "cs": "Rychlost",
        "zh-Hans": "速度",
    },
)
t(
    "treadmill_pace",
    **{
        "en-GB": "Pace",
        "uk": "Темп",
        "pl": "Tempo",
        "be": "Тэмп",
        "kk": "Темп",
        "ka": "ტემპი",
        "es": "Ritmo",
        "fr": "Allure",
        "de": "Pace",
        "cs": "Tempo",
        "zh-Hans": "配速",
    },
)
t(
    "treadmill_incline",
    **{
        "en-GB": "Incline",
        "uk": "Нахил",
        "pl": "Nachylenie",
        "be": "Нахіл",
        "kk": "Еңіс",
        "ka": "დახრა",
        "es": "Inclinación",
        "fr": "Pente",
        "de": "Steigung",
        "cs": "Sklon",
        "zh-Hans": "坡度",
    },
)
t(
    "treadmill_distance",
    **{
        "en-GB": "Distance",
        "uk": "Дистанція",
        "pl": "Dystans",
        "be": "Дыстанцыя",
        "kk": "Қашықтық",
        "ka": "დისტანცია",
        "es": "Distancia",
        "fr": "Distance",
        "de": "Distanz",
        "cs": "Vzdálenost",
        "zh-Hans": "距离",
    },
)
t(
    "treadmill_hr",
    **{
        "en-GB": "Heart rate",
        "uk": "Пульс",
        "pl": "Tętno",
        "be": "Пульс",
        "kk": "Жүрек соғысы",
        "ka": "გულისცემა",
        "es": "Frecuencia cardiaca",
        "fr": "Fréquence cardiaque",
        "de": "Herzfrequenz",
        "cs": "Tep",
        "zh-Hans": "心率",
    },
)
t(
    "treadmill_zone",
    **{
        "en-GB": "HR zone",
        "uk": "Зона пульсу",
        "pl": "Strefa tętna",
        "be": "Зона пульсу",
        "kk": "ЖС аймағы",
        "ka": "გულისცემის ზონა",
        "es": "Zona FC",
        "fr": "Zone FC",
        "de": "HF-Zone",
        "cs": "Tepová zóna",
        "zh-Hans": "心率区间",
    },
)
t(
    "treadmill_calories",
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
    "treadmill_control_title",
    **{
        "en-GB": "Remote control",
        "uk": "Дистанційне керування",
        "pl": "Sterowanie zdalne",
        "be": "Аддаленае кіраванне",
        "kk": "Қашықтан басқару",
        "ka": "დისტანციური მართვა",
        "es": "Control remoto",
        "fr": "Commande à distance",
        "de": "Fernsteuerung",
        "cs": "Dálkové ovládání",
        "zh-Hans": "远程控制",
    },
)
t(
    "treadmill_speed_up",
    **{
        "en-GB": "Speed +",
        "uk": "Швидкість +",
        "pl": "Prędkość +",
        "be": "Хуткасць +",
        "kk": "Жылдамдық +",
        "ka": "სიჩქარე +",
        "es": "Velocidad +",
        "fr": "Vitesse +",
        "de": "Tempo +",
        "cs": "Rychlost +",
        "zh-Hans": "加速",
    },
)
t(
    "treadmill_speed_down",
    **{
        "en-GB": "Speed −",
        "uk": "Швидкість −",
        "pl": "Prędkość −",
        "be": "Хуткасць −",
        "kk": "Жылдамдық −",
        "ka": "სიჩქარე −",
        "es": "Velocidad −",
        "fr": "Vitesse −",
        "de": "Tempo −",
        "cs": "Rychlost −",
        "zh-Hans": "减速",
    },
)
t(
    "treadmill_incline_up",
    **{
        "en-GB": "Incline +",
        "uk": "Нахил +",
        "pl": "Nachylenie +",
        "be": "Нахіл +",
        "kk": "Еңіс +",
        "ka": "დახრა +",
        "es": "Inclinación +",
        "fr": "Pente +",
        "de": "Steigung +",
        "cs": "Sklon +",
        "zh-Hans": "坡度 +",
    },
)
t(
    "treadmill_incline_down",
    **{
        "en-GB": "Incline −",
        "uk": "Нахил −",
        "pl": "Nachylenie −",
        "be": "Нахіл −",
        "kk": "Еңіс −",
        "ka": "დახრა −",
        "es": "Inclinación −",
        "fr": "Pente −",
        "de": "Steigung −",
        "cs": "Sklon −",
        "zh-Hans": "坡度 −",
    },
)
t(
    "treadmill_start",
    **{
        "en-GB": "Start treadmill run",
        "uk": "Почати біг на доріжці",
        "pl": "Start biegu na bieżni",
        "be": "Пачаць бег на дарожцы",
        "kk": "Жүгіру жолын бастау",
        "ka": "სარბენი ბილიკის დაწყება",
        "es": "Iniciar carrera en cinta",
        "fr": "Démarrer course tapis",
        "de": "Laufband-Lauf starten",
        "cs": "Spustit běh na pásu",
        "zh-Hans": "开始跑步机跑步",
    },
)
t(
    "treadmill_finish",
    **{
        "en-GB": "Finish and save",
        "uk": "Завершити й зберегти",
        "pl": "Zakończ i zapisz",
        "be": "Завяршыць і захаваць",
        "kk": "Аяқтау және сақтау",
        "ka": "დასრულება და შენახვა",
        "es": "Terminar y guardar",
        "fr": "Terminer et enregistrer",
        "de": "Beenden und speichern",
        "cs": "Dokončit a uložit",
        "zh-Hans": "结束并保存",
    },
)
t(
    "treadmill_pause",
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
    "treadmill_resume",
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
    "treadmill_saved",
    **{
        "en-GB": "Treadmill run saved locally and to Apple Health / Health Connect when allowed.",
        "uk": "Біг на доріжці збережено локально та в Apple Health / Health Connect за дозволом.",
        "pl": "Bieg na bieżni zapisany lokalnie oraz w Apple Health / Health Connect, gdy wolno.",
        "be": "Бег на дарожцы захаваны лакальна і ў Apple Health / Health Connect пры дазволе.",
        "kk": "Жүгіру жолы жергілікті және рұқсат болса Apple Health / Health Connect-ке сақталды.",
        "ka": "სარბენი ბილიკი შენახულია ადგილობრივად და Apple Health / Health Connect-ში ნებართვისას.",
        "es": "Carrera en cinta guardada en el dispositivo y en Apple Health / Health Connect si se permite.",
        "fr": "Course tapis enregistrée localement et dans Apple Health / Health Connect si autorisé.",
        "de": "Laufband-Lauf lokal und in Apple Health / Health Connect gespeichert (falls erlaubt).",
        "cs": "Běh na pásu uložen lokálně a do Apple Health / Health Connect, pokud je povoleno.",
        "zh-Hans": "跑步机跑步已保存在本地；若已授权也会写入 Apple Health / Health Connect。",
    },
)
t(
    "treadmill_calibration_title",
    **{
        "en-GB": "Distance calibration",
        "uk": "Калібрування дистанції",
        "pl": "Kalibracja dystansu",
        "be": "Каліброўка дыстанцыі",
        "kk": "Қашықтықты калибрлеу",
        "ka": "დისტანციის კალიბრაცია",
        "es": "Calibración de distancia",
        "fr": "Calibration de distance",
        "de": "Distanzkalibrierung",
        "cs": "Kalibrace vzdálenosti",
        "zh-Hans": "距离校准",
    },
)
t(
    "treadmill_calibration_body",
    **{
        "en-GB": "No treadmill link — enter the console distance (metres) to calibrate the watch estimate.",
        "uk": "Немає зв’язку з доріжкою — введіть дистанцію з консолі (метри), щоб відкалібрувати оцінку годинника.",
        "pl": "Brak połączenia z bieżnią — wpisz dystans z konsoli (metry), by skalibrować szacunek zegarka.",
        "be": "Няма сувязі з дарожкай — увядзіце дыстанцыю з кансолі (метры), каб адкалібраваць ацэнку гадзінніка.",
        "kk": "Жүгіру жолымен байланыс жоқ — сағат бағасын калибрлеу үшін консоль қашықтығын (метр) енгізіңіз.",
        "ka": "სარბენ ბილიკთან კავშირი არ არის — საათის შეფასების კალიბრაციისთვის შეიყვანეთ კონსოლის დისტანცია (მეტრი).",
        "es": "Sin enlace a la cinta — introduce la distancia de la consola (metros) para calibrar la estimación del reloj.",
        "fr": "Pas de lien tapis — saisissez la distance console (mètres) pour calibrer l’estimation de la montre.",
        "de": "Kein Laufband-Link — Konsolen-Distanz (Meter) eingeben, um die Watch-Schätzung zu kalibrieren.",
        "cs": "Bez spojení s pásem — zadejte vzdálenost z konzole (metry) pro kalibraci odhadu hodinek.",
        "zh-Hans": "未连接跑步机——输入控制台距离（米）以校准手表估算。",
    },
)
t(
    "treadmill_calibration_metres",
    **{
        "en-GB": "Console distance (m)",
        "uk": "Дистанція з консолі (м)",
        "pl": "Dystans z konsoli (m)",
        "be": "Дыстанцыя з кансолі (м)",
        "kk": "Консоль қашықтығы (м)",
        "ka": "კონსოლის დისტანცია (მ)",
        "es": "Distancia de consola (m)",
        "fr": "Distance console (m)",
        "de": "Konsolen-Distanz (m)",
        "cs": "Vzdálenost z konzole (m)",
        "zh-Hans": "控制台距离（米）",
    },
)
t(
    "treadmill_diagnostics_title",
    **{
        "en-GB": "Treadmill diagnostics",
        "uk": "Діагностика доріжки",
        "pl": "Diagnostyka bieżni",
        "be": "Дыягностыка дарожкі",
        "kk": "Жүгіру жолының диагностикасы",
        "ka": "სარბენი ბილიკის დიაგნოსტიკა",
        "es": "Diagnóstico de cinta",
        "fr": "Diagnostics tapis",
        "de": "Laufband-Protokoll",
        "cs": "Diagnostika pásu",
        "zh-Hans": "跑步机诊断",
    },
)
t(
    "treadmill_diagnostics_export",
    **{
        "en-GB": "Export log",
        "uk": "Експортувати журнал",
        "pl": "Eksportuj dziennik",
        "be": "Экспартаваць журнал",
        "kk": "Журналды экспорттау",
        "ka": "ჟურნალის ექსპორტი",
        "es": "Exportar registro",
        "fr": "Exporter le journal",
        "de": "Protokoll exportieren",
        "cs": "Exportovat protokol",
        "zh-Hans": "导出日志",
    },
)
t(
    "treadmill_diagnostics_close",
    **{
        "en-GB": "Close",
        "uk": "Закрити",
        "pl": "Zamknij",
        "be": "Закрыць",
        "kk": "Жабу",
        "ka": "დახურვა",
        "es": "Cerrar",
        "fr": "Fermer",
        "de": "Schließen",
        "cs": "Zavřít",
        "zh-Hans": "关闭",
    },
)
t(
    "treadmill_no_connection",
    **{
        "en-GB": "No treadmill linked",
        "uk": "Доріжку не підключено",
        "pl": "Brak połączonej bieżni",
        "be": "Дарожка не падключана",
        "kk": "Жүгіру жолы қосылмаған",
        "ka": "სარბენი ბილიკი არ არის დაკავშირებული",
        "es": "Sin cinta vinculada",
        "fr": "Aucun tapis lié",
        "de": "Kein Laufband verbunden",
        "cs": "Žádný pás není připojen",
        "zh-Hans": "未连接跑步机",
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


def upsert_android(path: Path, locale_key: str, folder_is_default: bool) -> None:
    if not path.exists():
        return
    text = path.read_text(encoding="utf-8")
    for key, locales in STRINGS.items():
        value = escape_android(locales["en-GB"] if folder_is_default else locales[locale_key])
        pattern = re.compile(rf'<string name="{re.escape(key)}">.*?</string>', re.DOTALL)
        replacement = f'<string name="{key}">{value}</string>'
        if pattern.search(text):
            text = pattern.sub(replacement, text)
        else:
            text = text.replace("</resources>", f"    {replacement}\n</resources>")
    path.write_text(text, encoding="utf-8")


t(
    "treadmill_walk_title",
    **{
        "en-GB": "Treadmill walk",
        "uk": "Ходьба на доріжці",
        "pl": "Chód na bieżni",
        "be": "Хадзьба на дарожцы",
        "kk": "Жүгіру жолында жүру",
        "ka": "სიარული სარბენ ბილიკზე",
        "es": "Caminata en cinta",
        "fr": "Marche tapis",
        "de": "Laufband-Gehen",
        "cs": "Chůze na pásu",
        "zh-Hans": "跑步机步行",
    },
)

t(
    "treadmill_walk_body",
    **{
        "en-GB": "Indoor walk with or without a Bluetooth treadmill. Pace and distance come from the machine when linked, otherwise from watch or phone motion — calibrate after if needed.",
        "uk": "Кімнатна ходьба з Bluetooth-доріжкою або без неї. Темп і дистанція з машини, інакше з годинника чи телефону — можна відкалібрувати.",
        "pl": "Chód indoor z bieżnią Bluetooth lub bez. Tempo i dystans z urządzenia albo z zegarka/telefonu — możesz skalibrować później.",
        "be": "Хадзьба ў памяшканні з Bluetooth-дарожкай або без. Тэмп і дыстанцыя з машыны, інакш з гадзінніка ці тэлефона — можна адкалібраваць.",
        "kk": "Bluetooth жүгіру жолымен немесе онсыз ішкі жүру. Қадам мен қашықтық құрылғыдан, әйтпесе сағат/телефоннан — кейін калибрлеуге болады.",
        "ka": "შიდა სიარული Bluetooth სარბენი ბილიკით ან მის გარეშე. ტემპი და დისტანცია აპარატიდან, სხვა შემთხვევაში საათიდან ან ტელეფონიდან — შემდეგ შეგიძლიათ დააკალიბროთ.",
        "es": "Caminata indoor con o sin cinta Bluetooth. Ritmo y distancia de la máquina o del reloj/teléfono; puedes calibrar después.",
        "fr": "Marche indoor avec ou sans tapis Bluetooth. Allure et distance viennent de la machine, sinon de la montre ou du téléphone — calibrage possible.",
        "de": "Indoor-Gehen mit oder ohne Bluetooth-Laufband. Tempo und Distanz vom Gerät, sonst von Watch oder Phone — danach kalibrierbar.",
        "cs": "Chůze indoor s Bluetooth pásem nebo bez. Tempo a vzdálenost ze stroje, jinak z hodinek nebo telefonu — lze kalibrovat.",
        "zh-Hans": "可在有或无蓝牙跑步机时进行室内步行。配速与距离来自器械，否则来自手表或手机运动估算，结束后可校准。",
    },
)

t(
    "treadmill_program_section",
    **{
        "en-GB": "Programs",
        "uk": "Програми",
        "pl": "Programy",
        "be": "Праграмы",
        "kk": "Бағдарламалар",
        "ka": "პროგრამები",
        "es": "Programas",
        "fr": "Programmes",
        "de": "Programme",
        "cs": "Programy",
        "zh-Hans": "课程",
    },
)

t(
    "treadmill_mode_title",
    **{
        "en-GB": "Mode",
        "uk": "Режим",
        "pl": "Tryb",
        "be": "Рэжым",
        "kk": "Режим",
        "ka": "რეჟიმი",
        "es": "Modo",
        "fr": "Mode",
        "de": "Modus",
        "cs": "Režim",
        "zh-Hans": "模式",
    },
)

t(
    "treadmill_program_easy_run_title",
    **{
        "en-GB": "Easy run",
        "uk": "Легкий біг",
        "pl": "Łatwy bieg",
        "be": "Лёгкі бег",
        "kk": "Жеңіл жүгіру",
        "ka": "მარტივი სირბილი",
        "es": "Carrera suave",
        "fr": "Course facile",
        "de": "Lockerer Lauf",
        "cs": "Lehký běh",
        "zh-Hans": "轻松跑",
    },
)

t(
    "treadmill_program_easy_run_body",
    **{
        "en-GB": "Steady easy pace.",
        "uk": "Рівний легкий темп.",
        "pl": "Równe łatwe tempo.",
        "be": "Роўны лёгкі тэмп.",
        "kk": "Тегіс жеңіл қарқын.",
        "ka": "თანაბარი მარტივი ტემპი.",
        "es": "Ritmo fácil constante.",
        "fr": "Allure facile régulière.",
        "de": "Gleichmäßiges lockeres Tempo.",
        "cs": "Rovnoměrné lehké tempo.",
        "zh-Hans": "稳定轻松配速。",
    },
)

t(
    "treadmill_program_intervals_title",
    **{
        "en-GB": "Intervals",
        "uk": "Інтервали",
        "pl": "Interwały",
        "be": "Інтэрвалы",
        "kk": "Интервалдар",
        "ka": "ინტერვალები",
        "es": "Intervalos",
        "fr": "Intervalles",
        "de": "Intervalle",
        "cs": "Intervaly",
        "zh-Hans": "间歇",
    },
)

t(
    "treadmill_program_intervals_body",
    **{
        "en-GB": "Hard repeats with recoveries.",
        "uk": "Важкі повтори з відновленням.",
        "pl": "Ciężkie powtórzenia z odpoczynkiem.",
        "be": "Цяжкія паўторы з аднаўленнем.",
        "kk": "Қалпына келумен ауыр қайталаулар.",
        "ka": "მძიმე გამეორებები აღდგენით.",
        "es": "Repeticiones duras con recuperación.",
        "fr": "Répétitions dures avec récupération.",
        "de": "Harte Wiederholungen mit Erholung.",
        "cs": "Tvrdé opakování s odpočinkem.",
        "zh-Hans": "高强度重复与恢复。",
    },
)

t(
    "treadmill_program_hill_title",
    **{
        "en-GB": "Hill / incline",
        "uk": "Пагорби / нахил",
        "pl": "Wzgórza / nachylenie",
        "be": "Пагоркі / нахіл",
        "kk": "Төбе / еңіс",
        "ka": "ბორცვი / დახრა",
        "es": "Colinas / inclinación",
        "fr": "Côtes / pente",
        "de": "Hügel / Steigung",
        "cs": "Kopce / sklon",
        "zh-Hans": "爬坡",
    },
)

t(
    "treadmill_program_hill_body",
    **{
        "en-GB": "Incline-focused hills.",
        "uk": "Фокус на нахилі.",
        "pl": "Skupienie na nachyleniu.",
        "be": "Фокус на нахіле.",
        "kk": "Еңіске бағытталған.",
        "ka": "დახრაზე ფოკუსი.",
        "es": "Enfoque en inclinación.",
        "fr": "Accent sur la pente.",
        "de": "Fokus auf Steigung.",
        "cs": "Zaměření na sklon.",
        "zh-Hans": "以坡度为主。",
    },
)

t(
    "treadmill_program_tempo_title",
    **{
        "en-GB": "Tempo",
        "uk": "Темпо",
        "pl": "Tempo",
        "be": "Тэмпа",
        "kk": "Темпо",
        "ka": "ტემპო",
        "es": "Tempo",
        "fr": "Tempo",
        "de": "Tempo",
        "cs": "Tempo",
        "zh-Hans": "节奏跑",
    },
)

t(
    "treadmill_program_tempo_body",
    **{
        "en-GB": "Sustained comfortably hard pace.",
        "uk": "Стійкий комфортно-важкий темп.",
        "pl": "Utrzymane komfortowo ciężkie tempo.",
        "be": "Устойлівы камфортна-цяжкі тэмп.",
        "kk": "Тұрақты ыңғайлы-ауыр қарқын.",
        "ka": "მდგრადი კომფორტულად რთული ტემპი.",
        "es": "Ritmo sostenido cómodamente duro.",
        "fr": "Allure soutenue confortablement dure.",
        "de": "Dauerhaft angenehm hartes Tempo.",
        "cs": "Udržované příjemně těžké tempo.",
        "zh-Hans": "持续稍吃力的配速。",
    },
)

t(
    "treadmill_program_fartlek_title",
    **{
        "en-GB": "Fartlek",
        "uk": "Фартлек",
        "pl": "Fartlek",
        "be": "Фартлек",
        "kk": "Фартлек",
        "ka": "ფარტლეკი",
        "es": "Fartlek",
        "fr": "Fartlek",
        "de": "Fartlek",
        "cs": "Fartlek",
        "zh-Hans": "法特莱克",
    },
)

t(
    "treadmill_program_fartlek_body",
    **{
        "en-GB": "Playful speed changes.",
        "uk": "Грайливі зміни швидкості.",
        "pl": "Zabawne zmiany tempa.",
        "be": "Гульнявыя змены хуткасці.",
        "kk": "Ойыншыл жылдамдық өзгерістері.",
        "ka": "სათამაშო სიჩქარის ცვლილებები.",
        "es": "Cambios de ritmo jugados.",
        "fr": "Changements d’allure ludiques.",
        "de": "Spielerische Tempowechsel.",
        "cs": "Hravé změny tempa.",
        "zh-Hans": "趣味变速。",
    },
)

t(
    "treadmill_program_progression_title",
    **{
        "en-GB": "Progression",
        "uk": "Прогресія",
        "pl": "Progresja",
        "be": "Прагрэсія",
        "kk": "Прогрессия",
        "ka": "პროგრესია",
        "es": "Progresión",
        "fr": "Progression",
        "de": "Steigerung",
        "cs": "Progrese",
        "zh-Hans": "渐进",
    },
)

t(
    "treadmill_program_progression_body",
    **{
        "en-GB": "Build pace across the session.",
        "uk": "Нарощуйте темп протягом сесії.",
        "pl": "Buduj tempo w trakcie sesji.",
        "be": "Нарошчвайце тэмп цягам сесіі.",
        "kk": "Сессия бойы қарқынды арттырыңыз.",
        "ka": "ტემპის აწევა სესიის განმავლობაში.",
        "es": "Sube el ritmo a lo largo.",
        "fr": "Augmentez l’allure au fil de la séance.",
        "de": "Tempo über die Einheit steigern.",
        "cs": "Zvyšujte tempo v průběhu.",
        "zh-Hans": "全程逐步提速。",
    },
)

t(
    "treadmill_program_walk_run_title",
    **{
        "en-GB": "Walk–run (beginner)",
        "uk": "Ходьба–біг (початківці)",
        "pl": "Chód–bieg (początkujący)",
        "be": "Хадзьба–бег (пачаткоўцы)",
        "kk": "Жүру–жүгіру (бастаушы)",
        "ka": "სიარული–სირბილი (დამწყები)",
        "es": "Caminar–correr (principiante)",
        "fr": "Marche–course (débutant)",
        "de": "Gehen–Laufen (Einsteiger)",
        "cs": "Chůze–běh (začátečník)",
        "zh-Hans": "走跑结合（初学）",
    },
)

t(
    "treadmill_program_walk_run_body",
    **{
        "en-GB": "Short jogs with walk recoveries.",
        "uk": "Короткі пробіжки з ходьбою.",
        "pl": "Krótkie truchty z chodem.",
        "be": "Кароткія прабежкі з хадзьбой.",
        "kk": "Қысқа жүгірістер жүрумен.",
        "ka": "მოკლე სირბილები სიარულით.",
        "es": "Troteos cortos con caminatas.",
        "fr": "Courtes foulées avec marches.",
        "de": "Kurze Trababschnitte mit Gehen.",
        "cs": "Krátké klusy s chůzí.",
        "zh-Hans": "短慢跑与步行恢复。",
    },
)

t(
    "treadmill_program_recovery_walk_title",
    **{
        "en-GB": "Recovery walk",
        "uk": "Відновлювальна ходьба",
        "pl": "Chód regeneracyjny",
        "be": "Аднаўленчая хадзьба",
        "kk": "Қалпына келтіру жүрісі",
        "ka": "აღდგენითი სიარული",
        "es": "Caminata de recuperación",
        "fr": "Marche de récupération",
        "de": "Regenerationsgehen",
        "cs": "Regenerační chůze",
        "zh-Hans": "恢复步行",
    },
)

t(
    "treadmill_program_recovery_walk_body",
    **{
        "en-GB": "Easy indoor walk.",
        "uk": "Легка кімнатна ходьба.",
        "pl": "Łatwy chód indoor.",
        "be": "Лёгкая хадзьба ў памяшканні.",
        "kk": "Жеңіл ішкі жүру.",
        "ka": "მარტივი შიდა სიარული.",
        "es": "Caminata indoor fácil.",
        "fr": "Marche indoor facile.",
        "de": "Leichtes Indoor-Gehen.",
        "cs": "Lehká indoor chůze.",
        "zh-Hans": "轻松室内步行。",
    },
)

t(
    "treadmill_hr_alert_high",
    **{
        "en-GB": "Heart rate is very high — consider slowing down or stopping",
        "uk": "Пульс дуже високий — варто сповільнитися або зупинитися",
        "pl": "Tętno jest bardzo wysokie — rozważ zwolnienie lub zatrzymanie",
        "be": "Пульс вельмі высокі — варта прыцішыцца ці спыніцца",
        "kk": "Жүрек соғысы өте жоғары — баяулатыңыз немесе тоқтаңыз",
        "ka": "გულისცემა ძალიან მაღალია — შეანელეთ ან გაჩერდით",
        "es": "La frecuencia es muy alta — plantéate ralentizar o parar",
        "fr": "Fréquence très élevée — envisagez de ralentir ou d’arrêter",
        "de": "Herzfrequenz sehr hoch — Tempo drosseln oder stoppen",
        "cs": "Tep je velmi vysoký — zvažte zpomalení nebo zastavení",
        "zh-Hans": "心率很高 — 请考虑放慢或停止",
    },
)

t(
    "treadmill_hr_alert_low",
    **{
        "en-GB": "Heart rate looks unusually low for this effort",
        "uk": "Пульс незвично низький для цього навантаження",
        "pl": "Tętno wygląda nietypowo nisko jak na ten wysiłek",
        "be": "Пульс незвычайна нізкі для гэтай нагрузкі",
        "kk": "Бұл жүктеме үшін жүрек соғысы әдеттен тыс төмен",
        "ka": "გულისცემა უჩვეულოდ დაბალია ამ დატვირთვისთვის",
        "es": "La FC parece baja para este esfuerzo",
        "fr": "FC inhabituellement basse pour cet effort",
        "de": "HF ungewöhnlich niedrig für diese Belastung",
        "cs": "Tep je pro tuto zátěž nezvykle nízký",
        "zh-Hans": "以当前强度看心率偏低",
    },
)

t(
    "treadmill_hr_alert_dropout",
    **{
        "en-GB": "Heart-rate sensor signal lost",
        "uk": "Сигнал пульсометра втрачено",
        "pl": "Utracono sygnał czujnika tętna",
        "be": "Сігнал пульсометра страчаны",
        "kk": "Жүрек соғысы сенсорының сигналы жоғалды",
        "ka": "გულისცემის სენსორის სიგნალი დაიკარგა",
        "es": "Se perdió la señal del sensor de FC",
        "fr": "Signal du capteur FC perdu",
        "de": "HF-Sensor-Signal verloren",
        "cs": "Signál tepového senzoru ztracen",
        "zh-Hans": "心率传感器信号丢失",
    },
)

t(
    "treadmill_hr_alert_above_zone",
    **{
        "en-GB": "Above the target heart-rate zone",
        "uk": "Вище цільової пульсової зони",
        "pl": "Powyżej docelowej strefy tętna",
        "be": "Вышэй мэтавай пульсавой зоны",
        "kk": "Мақсатты жүрек соғысы аймағынан жоғары",
        "ka": "სამიზნე გულისცემის ზონაზე მაღლა",
        "es": "Por encima de la zona objetivo",
        "fr": "Au-dessus de la zone cible",
        "de": "Über der Ziel-HF-Zone",
        "cs": "Nad cílovou tepovou zónou",
        "zh-Hans": "高于目标心率区间",
    },
)

t(
    "treadmill_hr_alert_below_zone",
    **{
        "en-GB": "Below the target heart-rate zone",
        "uk": "Нижче цільової пульсової зони",
        "pl": "Poniżej docelowej strefy tętna",
        "be": "Ніжэй мэтавай пульсавой зоны",
        "kk": "Мақсатты жүрек соғысы аймағынан төмен",
        "ka": "სამიზნე გულისცემის ზონაზე დაბლა",
        "es": "Por debajo de la zona objetivo",
        "fr": "En dessous de la zone cible",
        "de": "Unter der Ziel-HF-Zone",
        "cs": "Pod cílovou tepovou zónou",
        "zh-Hans": "低于目标心率区间",
    },
)

t(
    "treadmill_program_auto_step",
    **{
        "en-GB": "Setting speed and incline on the treadmill.",
        "uk": "Setting speed and incline on the treadmill.",
        "pl": "Setting speed and incline on the treadmill.",
        "be": "Setting speed and incline on the treadmill.",
        "kk": "Setting speed and incline on the treadmill.",
        "ka": "Setting speed and incline on the treadmill.",
        "es": "Setting speed and incline on the treadmill.",
        "fr": "Setting speed and incline on the treadmill.",
        "de": "Setting speed and incline on the treadmill.",
        "cs": "Setting speed and incline on the treadmill.",
        "zh-Hans": "Setting speed and incline on the treadmill.",
    },
)

t(
    "treadmill_program_prompt_step",
    **{
        "en-GB": "Adjust speed / incline to match the step.",
        "uk": "Adjust speed / incline to match the step.",
        "pl": "Adjust speed / incline to match the step.",
        "be": "Adjust speed / incline to match the step.",
        "kk": "Adjust speed / incline to match the step.",
        "ka": "Adjust speed / incline to match the step.",
        "es": "Adjust speed / incline to match the step.",
        "fr": "Adjust speed / incline to match the step.",
        "de": "Adjust speed / incline to match the step.",
        "cs": "Adjust speed / incline to match the step.",
        "zh-Hans": "Adjust speed / incline to match the step.",
    },
)

t(
    "treadmill_program_complete",
    **{
        "en-GB": "Program complete.",
        "uk": "Program complete.",
        "pl": "Program complete.",
        "be": "Program complete.",
        "kk": "Program complete.",
        "ka": "Program complete.",
        "es": "Program complete.",
        "fr": "Program complete.",
        "de": "Program complete.",
        "cs": "Program complete.",
        "zh-Hans": "Program complete.",
    },
)

t(
    "treadmill_step_warmup",
    **{
        "en-GB": "Warm-up",
        "uk": "Warm-up",
        "pl": "Warm-up",
        "be": "Warm-up",
        "kk": "Warm-up",
        "ka": "Warm-up",
        "es": "Warm-up",
        "fr": "Warm-up",
        "de": "Warm-up",
        "cs": "Warm-up",
        "zh-Hans": "Warm-up",
    },
)

t(
    "treadmill_step_easy",
    **{
        "en-GB": "Easy",
        "uk": "Easy",
        "pl": "Easy",
        "be": "Easy",
        "kk": "Easy",
        "ka": "Easy",
        "es": "Easy",
        "fr": "Easy",
        "de": "Easy",
        "cs": "Easy",
        "zh-Hans": "Easy",
    },
)

t(
    "treadmill_step_cooldown",
    **{
        "en-GB": "Cool-down",
        "uk": "Cool-down",
        "pl": "Cool-down",
        "be": "Cool-down",
        "kk": "Cool-down",
        "ka": "Cool-down",
        "es": "Cool-down",
        "fr": "Cool-down",
        "de": "Cool-down",
        "cs": "Cool-down",
        "zh-Hans": "Cool-down",
    },
)

t(
    "treadmill_step_hard",
    **{
        "en-GB": "Hard",
        "uk": "Hard",
        "pl": "Hard",
        "be": "Hard",
        "kk": "Hard",
        "ka": "Hard",
        "es": "Hard",
        "fr": "Hard",
        "de": "Hard",
        "cs": "Hard",
        "zh-Hans": "Hard",
    },
)

t(
    "treadmill_step_recover",
    **{
        "en-GB": "Recover",
        "uk": "Recover",
        "pl": "Recover",
        "be": "Recover",
        "kk": "Recover",
        "ka": "Recover",
        "es": "Recover",
        "fr": "Recover",
        "de": "Recover",
        "cs": "Recover",
        "zh-Hans": "Recover",
    },
)

t(
    "treadmill_step_hill",
    **{
        "en-GB": "Hill",
        "uk": "Hill",
        "pl": "Hill",
        "be": "Hill",
        "kk": "Hill",
        "ka": "Hill",
        "es": "Hill",
        "fr": "Hill",
        "de": "Hill",
        "cs": "Hill",
        "zh-Hans": "Hill",
    },
)

t(
    "treadmill_step_tempo",
    **{
        "en-GB": "Tempo",
        "uk": "Tempo",
        "pl": "Tempo",
        "be": "Tempo",
        "kk": "Tempo",
        "ka": "Tempo",
        "es": "Tempo",
        "fr": "Tempo",
        "de": "Tempo",
        "cs": "Tempo",
        "zh-Hans": "Tempo",
    },
)

t(
    "treadmill_step_surge",
    **{
        "en-GB": "Surge",
        "uk": "Surge",
        "pl": "Surge",
        "be": "Surge",
        "kk": "Surge",
        "ka": "Surge",
        "es": "Surge",
        "fr": "Surge",
        "de": "Surge",
        "cs": "Surge",
        "zh-Hans": "Surge",
    },
)

t(
    "treadmill_step_build",
    **{
        "en-GB": "Build",
        "uk": "Build",
        "pl": "Build",
        "be": "Build",
        "kk": "Build",
        "ka": "Build",
        "es": "Build",
        "fr": "Build",
        "de": "Build",
        "cs": "Build",
        "zh-Hans": "Build",
    },
)

t(
    "treadmill_step_warmup_walk",
    **{
        "en-GB": "Warm-up walk",
        "uk": "Warm-up walk",
        "pl": "Warm-up walk",
        "be": "Warm-up walk",
        "kk": "Warm-up walk",
        "ka": "Warm-up walk",
        "es": "Warm-up walk",
        "fr": "Warm-up walk",
        "de": "Warm-up walk",
        "cs": "Warm-up walk",
        "zh-Hans": "Warm-up walk",
    },
)

t(
    "treadmill_step_jog",
    **{
        "en-GB": "Jog",
        "uk": "Jog",
        "pl": "Jog",
        "be": "Jog",
        "kk": "Jog",
        "ka": "Jog",
        "es": "Jog",
        "fr": "Jog",
        "de": "Jog",
        "cs": "Jog",
        "zh-Hans": "Jog",
    },
)

t(
    "treadmill_step_walk",
    **{
        "en-GB": "Walk",
        "uk": "Walk",
        "pl": "Walk",
        "be": "Walk",
        "kk": "Walk",
        "ka": "Walk",
        "es": "Walk",
        "fr": "Walk",
        "de": "Walk",
        "cs": "Walk",
        "zh-Hans": "Walk",
    },
)

t(
    "treadmill_step_cooldown_walk",
    **{
        "en-GB": "Cool-down walk",
        "uk": "Cool-down walk",
        "pl": "Cool-down walk",
        "be": "Cool-down walk",
        "kk": "Cool-down walk",
        "ka": "Cool-down walk",
        "es": "Cool-down walk",
        "fr": "Cool-down walk",
        "de": "Cool-down walk",
        "cs": "Cool-down walk",
        "zh-Hans": "Cool-down walk",
    },
)

t(
    "treadmill_step_easy_walk",
    **{
        "en-GB": "Easy walk",
        "uk": "Easy walk",
        "pl": "Easy walk",
        "be": "Easy walk",
        "kk": "Easy walk",
        "ka": "Easy walk",
        "es": "Easy walk",
        "fr": "Easy walk",
        "de": "Easy walk",
        "cs": "Easy walk",
        "zh-Hans": "Easy walk",
    },
)

t(
    "treadmill_hr_settings_title",
    **{
        "en-GB": "Heart rate",
        "uk": "Heart rate",
        "pl": "Heart rate",
        "be": "Heart rate",
        "kk": "Heart rate",
        "ka": "Heart rate",
        "es": "Heart rate",
        "fr": "Heart rate",
        "de": "Heart rate",
        "cs": "Heart rate",
        "zh-Hans": "Heart rate",
    },
)

t(
    "treadmill_hr_alerts_enabled",
    **{
        "en-GB": "Heart-rate alerts",
        "uk": "Heart-rate alerts",
        "pl": "Heart-rate alerts",
        "be": "Heart-rate alerts",
        "kk": "Heart-rate alerts",
        "ka": "Heart-rate alerts",
        "es": "Heart-rate alerts",
        "fr": "Heart-rate alerts",
        "de": "Heart-rate alerts",
        "cs": "Heart-rate alerts",
        "zh-Hans": "Heart-rate alerts",
    },
)

t(
    "treadmill_hr_max",
    **{
        "en-GB": "Max heart rate (bpm)",
        "uk": "Max heart rate (bpm)",
        "pl": "Max heart rate (bpm)",
        "be": "Max heart rate (bpm)",
        "kk": "Max heart rate (bpm)",
        "ka": "Max heart rate (bpm)",
        "es": "Max heart rate (bpm)",
        "fr": "Max heart rate (bpm)",
        "de": "Max heart rate (bpm)",
        "cs": "Max heart rate (bpm)",
        "zh-Hans": "Max heart rate (bpm)",
    },
)

t(
    "treadmill_hr_karvonen",
    **{
        "en-GB": "Use Karvonen zones",
        "uk": "Use Karvonen zones",
        "pl": "Use Karvonen zones",
        "be": "Use Karvonen zones",
        "kk": "Use Karvonen zones",
        "ka": "Use Karvonen zones",
        "es": "Use Karvonen zones",
        "fr": "Use Karvonen zones",
        "de": "Use Karvonen zones",
        "cs": "Use Karvonen zones",
        "zh-Hans": "Use Karvonen zones",
    },
)

t(
    "treadmill_hr_rest",
    **{
        "en-GB": "Resting heart rate",
        "uk": "Resting heart rate",
        "pl": "Resting heart rate",
        "be": "Resting heart rate",
        "kk": "Resting heart rate",
        "ka": "Resting heart rate",
        "es": "Resting heart rate",
        "fr": "Resting heart rate",
        "de": "Resting heart rate",
        "cs": "Resting heart rate",
        "zh-Hans": "Resting heart rate",
    },
)

t(
    "treadmill_hr_strap_scan",
    **{
        "en-GB": "Scan chest strap",
        "uk": "Scan chest strap",
        "pl": "Scan chest strap",
        "be": "Scan chest strap",
        "kk": "Scan chest strap",
        "ka": "Scan chest strap",
        "es": "Scan chest strap",
        "fr": "Scan chest strap",
        "de": "Scan chest strap",
        "cs": "Scan chest strap",
        "zh-Hans": "Scan chest strap",
    },
)

t(
    "treadmill_hr_strap_connected",
    **{
        "en-GB": "Chest strap connected",
        "uk": "Chest strap connected",
        "pl": "Chest strap connected",
        "be": "Chest strap connected",
        "kk": "Chest strap connected",
        "ka": "Chest strap connected",
        "es": "Chest strap connected",
        "fr": "Chest strap connected",
        "de": "Chest strap connected",
        "cs": "Chest strap connected",
        "zh-Hans": "Chest strap connected",
    },
)

t(
    "treadmill_export_workoutkit",
    **{
        "en-GB": "Export program to Workout",
        "uk": "Export program to Workout",
        "pl": "Export program to Workout",
        "be": "Export program to Workout",
        "kk": "Export program to Workout",
        "ka": "Export program to Workout",
        "es": "Export program to Workout",
        "fr": "Export program to Workout",
        "de": "Export program to Workout",
        "cs": "Export program to Workout",
        "zh-Hans": "Export program to Workout",
    },
)

t(
    "treadmill_export_ok",
    **{
        "en-GB": "Program scheduled in Workout",
        "uk": "Program scheduled in Workout",
        "pl": "Program scheduled in Workout",
        "be": "Program scheduled in Workout",
        "kk": "Program scheduled in Workout",
        "ka": "Program scheduled in Workout",
        "es": "Program scheduled in Workout",
        "fr": "Program scheduled in Workout",
        "de": "Program scheduled in Workout",
        "cs": "Program scheduled in Workout",
        "zh-Hans": "Program scheduled in Workout",
    },
)

t(
    "treadmill_export_failed",
    **{
        "en-GB": "Could not schedule in Workout",
        "uk": "Could not schedule in Workout",
        "pl": "Could not schedule in Workout",
        "be": "Could not schedule in Workout",
        "kk": "Could not schedule in Workout",
        "ka": "Could not schedule in Workout",
        "es": "Could not schedule in Workout",
        "fr": "Could not schedule in Workout",
        "de": "Could not schedule in Workout",
        "cs": "Could not schedule in Workout",
        "zh-Hans": "Could not schedule in Workout",
    },
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
            upsert_android(
                ROOT / "androidApp/src/main/res" / folder / "strings.xml",
                locale,
                folder_is_default=(folder == "values"),
            )
            upsert_android(
                ROOT / "wearApp/src/main/res" / folder / "strings.xml",
                locale,
                folder_is_default=(folder == "values"),
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

    # StringCatalog.kt fallbacks (en-GB) — append constants if missing.
    catalog_kt = ROOT / "shared/src/commonMain/kotlin/com/omnigart/shared/i18n/StringCatalog.kt"
    kt = catalog_kt.read_text(encoding="utf-8")
    for key in STRINGS:
        const = key.upper()
        if f'const val {const}' not in kt and f'"{key}"' not in kt:
            # Prefer JSON resources; catalog constants are optional for new keys.
            pass
    print(f"Synced {len(STRINGS)} treadmill keys × {len(LOCALES)} locales")


if __name__ == "__main__":
    main()
