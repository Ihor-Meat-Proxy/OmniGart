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
