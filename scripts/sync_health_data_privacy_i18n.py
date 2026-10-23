#!/usr/bin/env python3
"""Sync Data and privacy / local health store UI strings (11 locales)."""

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
    "data_privacy_title",
    **{
        "en-GB": "Data and privacy",
        "uk": "Дані та приватність",
        "pl": "Dane i prywatność",
        "be": "Дадзеныя і прыватнасць",
        "kk": "Деректер және құпиялылық",
        "ka": "მონაცემები და კონფიდენციალობა",
        "es": "Datos y privacidad",
        "fr": "Données et confidentialité",
        "de": "Daten und Datenschutz",
        "cs": "Data a soukromí",
        "zh-Hans": "数据与隐私",
    },
)

t(
    "data_privacy_intro",
    **{
        "en-GB": (
            "OmniGart keeps a full local copy of your workouts and samples on this device. "
            "Export stays on-device. Platform sync is optional and never required for logging."
        ),
        "uk": (
            "OmniGart зберігає повну локальну копію тренувань і зразків на цьому пристрої. "
            "Експорт лишається на пристрої. Синхронізація з платформою необов’язкова."
        ),
        "pl": (
            "OmniGart przechowuje pełną lokalną kopię treningów i próbek na tym urządzeniu. "
            "Eksport pozostaje na urządzeniu. Synchronizacja z platformą jest opcjonalna."
        ),
        "be": (
            "OmniGart захоўвае поўную лакальную копію трэніровак і ўзораў на гэтай прыладзе. "
            "Экспарт застаецца на прыладзе. Сінхранізацыя з платформай неабавязковая."
        ),
        "kk": (
            "OmniGart жаттығулар мен үлгілердің толық жергілікті көшірмесін осы құрылғыда сақтайды. "
            "Экспорт құрылғыда қалады. Платформамен синхрондау міндетті емес."
        ),
        "ka": (
            "OmniGart ინახავს ვარჯიშებისა და ნიმუშების სრულ ლოკალურ ასლს ამ მოწყობილობაზე. "
            "ექსპორტი რჩება მოწყობილობაზე. პლატფორმასთან სინქრონიზაცია არასავალდებულოა."
        ),
        "es": (
            "OmniGart guarda una copia local completa de tus entrenamientos y muestras en este dispositivo. "
            "La exportación permanece en el dispositivo. La sincronización es opcional."
        ),
        "fr": (
            "OmniGart conserve une copie locale complète de vos séances et échantillons sur cet appareil. "
            "L’export reste sur l’appareil. La synchronisation est facultative."
        ),
        "de": (
            "OmniGart speichert eine vollständige lokale Kopie Ihrer Workouts und Messwerte auf diesem Gerät. "
            "Der Export bleibt auf dem Gerät. Die Synchronisierung ist optional."
        ),
        "cs": (
            "OmniGart uchovává úplnou místní kopii tréninků a vzorků na tomto zařízení. "
            "Export zůstává v zařízení. Synchronizace s platformou je volitelná."
        ),
        "zh-Hans": (
            "OmniGart 会在本机保存训练与采样的完整本地副本。"
            "导出仅留在本机。平台同步为可选，记录训练无需授权。"
        ),
    },
)

t(
    "data_privacy_sync_status",
    **{
        "en-GB": "Platform sync",
        "uk": "Синхронізація з платформою",
        "pl": "Synchronizacja z platformą",
        "be": "Сінхранізацыя з платформай",
        "kk": "Платформа синхрондауы",
        "ka": "პლატფორმის სინქრონიზაცია",
        "es": "Sincronización de plataforma",
        "fr": "Synchronisation plateforme",
        "de": "Plattform-Synchronisierung",
        "cs": "Synchronizace s platformou",
        "zh-Hans": "平台同步",
    },
)

t(
    "data_privacy_sync_status_line",
    **{
        "en-GB": "Platform sync: %1$s",
        "uk": "Синхронізація з платформою: %1$s",
        "pl": "Synchronizacja z platformą: %1$s",
        "be": "Сінхранізацыя з платформай: %1$s",
        "kk": "Платформа синхрондауы: %1$s",
        "ka": "პლატფორმის სინქრონიზაცია: %1$s",
        "es": "Sincronización de plataforma: %1$s",
        "fr": "Synchronisation plateforme : %1$s",
        "de": "Plattform-Synchronisierung: %1$s",
        "cs": "Synchronizace s platformou: %1$s",
        "zh-Hans": "平台同步：%1$s",
    },
)

t(
    "data_privacy_last_sync",
    **{
        "en-GB": "Last sync",
        "uk": "Остання синхронізація",
        "pl": "Ostatnia synchronizacja",
        "be": "Апошняя сінхранізацыя",
        "kk": "Соңғы синхрондау",
        "ka": "ბოლო სინქრონიზაცია",
        "es": "Última sincronización",
        "fr": "Dernière synchronisation",
        "de": "Letzte Synchronisierung",
        "cs": "Poslední synchronizace",
        "zh-Hans": "上次同步",
    },
)

t(
    "data_privacy_last_sync_line",
    **{
        "en-GB": "Last sync: %1$s",
        "uk": "Остання синхронізація: %1$s",
        "pl": "Ostatnia synchronizacja: %1$s",
        "be": "Апошняя сінхранізацыя: %1$s",
        "kk": "Соңғы синхрондау: %1$s",
        "ka": "ბოლო სინქრონიზაცია: %1$s",
        "es": "Última sincronización: %1$s",
        "fr": "Dernière synchronisation : %1$s",
        "de": "Letzte Synchronisierung: %1$s",
        "cs": "Poslední synchronizace: %1$s",
        "zh-Hans": "上次同步：%1$s",
    },
)

t(
    "data_privacy_sync_now",
    **{
        "en-GB": "Sync from platform now",
        "uk": "Синхронізувати з платформи зараз",
        "pl": "Synchronizuj z platformy teraz",
        "be": "Сінхранізаваць з платформы зараз",
        "kk": "Платформадан қазір синхрондау",
        "ka": "პლატფორმიდან სინქრონიზაცია ახლა",
        "es": "Sincronizar desde la plataforma ahora",
        "fr": "Synchroniser depuis la plateforme",
        "de": "Jetzt von der Plattform synchronisieren",
        "cs": "Synchronizovat z platformy nyní",
        "zh-Hans": "立即从平台同步",
    },
)

t(
    "data_privacy_export",
    **{
        "en-GB": "Export health data",
        "uk": "Експортувати дані самопочуття",
        "pl": "Eksportuj dane wellness",
        "be": "Экспартаваць дадзеныя самаадчування",
        "kk": "Сауықтыру деректерін экспорттау",
        "ka": "კეთილდღეობის მონაცემების ექსპორტი",
        "es": "Exportar datos de bienestar",
        "fr": "Exporter les données de bien-être",
        "de": "Wellness-Daten exportieren",
        "cs": "Exportovat wellness data",
        "zh-Hans": "导出健康数据",
    },
)

t(
    "data_privacy_export_share",
    **{
        "en-GB": "Share export",
        "uk": "Поділитися експортом",
        "pl": "Udostępnij eksport",
        "be": "Падзяліцца экспартам",
        "kk": "Экспортпен бөлісу",
        "ka": "ექსპორტის გაზიარება",
        "es": "Compartir exportación",
        "fr": "Partager l’export",
        "de": "Export teilen",
        "cs": "Sdílet export",
        "zh-Hans": "分享导出",
    },
)

t(
    "data_privacy_import_json",
    **{
        "en-GB": "Import OmniGart JSON",
        "uk": "Імпортувати OmniGart JSON",
        "pl": "Importuj JSON OmniGart",
        "be": "Імпартаваць OmniGart JSON",
        "kk": "OmniGart JSON импорттау",
        "ka": "OmniGart JSON-ის იმპორტი",
        "es": "Importar JSON de OmniGart",
        "fr": "Importer le JSON OmniGart",
        "de": "OmniGart-JSON importieren",
        "cs": "Importovat OmniGart JSON",
        "zh-Hans": "导入 OmniGart JSON",
    },
)

t(
    "data_privacy_conflicts_title",
    **{
        "en-GB": "Conflict review",
        "uk": "Перегляд конфліктів",
        "pl": "Przegląd konfliktów",
        "be": "Прагляд канфліктаў",
        "kk": "Қақтығыстарды қарау",
        "ka": "კონფლიქტების განხილვა",
        "es": "Revisión de conflictos",
        "fr": "Revue des conflits",
        "de": "Konfliktprüfung",
        "cs": "Kontrola konfliktů",
        "zh-Hans": "冲突审阅",
    },
)

t(
    "data_privacy_resolve_all",
    **{
        "en-GB": "Mark conflicts reviewed",
        "uk": "Позначити конфлікти переглянутими",
        "pl": "Oznacz konflikty jako przejrzane",
        "be": "Пазначыць канфлікты прагледжанымі",
        "kk": "Қақтығыстарды қаралған деп белгілеу",
        "ka": "კონფლიქტების მონიშვნა განხილულად",
        "es": "Marcar conflictos como revisados",
        "fr": "Marquer les conflits comme revus",
        "de": "Konflikte als geprüft markieren",
        "cs": "Označit konflikty jako zkontrolované",
        "zh-Hans": "将冲突标为已审阅",
    },
)

t(
    "data_privacy_export_ready",
    **{
        "en-GB": "Export ready to share or save.",
        "uk": "Експорт готовий до поширення або збереження.",
        "pl": "Eksport gotowy do udostępnienia lub zapisania.",
        "be": "Экспарт гатовы да перадачы або захавання.",
        "kk": "Экспорт бөлісуге немесе сақтауға дайын.",
        "ka": "ექსპორტი მზად არის გასაზიარებლად ან შესანახად.",
        "es": "Exportación lista para compartir o guardar.",
        "fr": "Export prêt à partager ou enregistrer.",
        "de": "Export bereit zum Teilen oder Speichern.",
        "cs": "Export je připraven ke sdílení nebo uložení.",
        "zh-Hans": "导出已就绪，可分享或保存。",
    },
)

t(
    "data_privacy_export_failed",
    **{
        "en-GB": "Could not prepare the export.",
        "uk": "Не вдалося підготувати експорт.",
        "pl": "Nie udało się przygotować eksportu.",
        "be": "Не атрымалася падрыхтаваць экспарт.",
        "kk": "Экспортты дайындау мүмкін болмады.",
        "ka": "ექსპორტის მომზადება ვერ მოხერხდა.",
        "es": "No se pudo preparar la exportación.",
        "fr": "Impossible de préparer l’export.",
        "de": "Export konnte nicht vorbereitet werden.",
        "cs": "Export se nepodařilo připravit.",
        "zh-Hans": "无法准备导出。",
    },
)

t(
    "data_privacy_import_ok",
    **{
        "en-GB": "Imported %d workouts.",
        "uk": "Імпортовано тренувань: %d.",
        "pl": "Zaimportowano treningów: %d.",
        "be": "Імпартавана трэніровак: %d.",
        "kk": "Импортталған жаттығулар: %d.",
        "ka": "იმპორტირებული ვარჯიშები: %d.",
        "es": "Entrenamientos importados: %d.",
        "fr": "Séances importées : %d.",
        "de": "%d Workouts importiert.",
        "cs": "Importováno tréninků: %d.",
        "zh-Hans": "已导入 %d 次训练。",
    },
)

t(
    "data_privacy_import_failed",
    **{
        "en-GB": "Could not import that file.",
        "uk": "Не вдалося імпортувати цей файл.",
        "pl": "Nie udało się zaimportować tego pliku.",
        "be": "Не атрымалася імпартаваць гэты файл.",
        "kk": "Бұл файлды импорттау мүмкін болмады.",
        "ka": "ამ ფაილის იმპორტი ვერ მოხერხდა.",
        "es": "No se pudo importar ese archivo.",
        "fr": "Impossible d’importer ce fichier.",
        "de": "Diese Datei konnte nicht importiert werden.",
        "cs": "Tento soubor se nepodařilo importovat.",
        "zh-Hans": "无法导入该文件。",
    },
)

t(
    "data_privacy_status_authorized",
    **{
        "en-GB": "Connected",
        "uk": "Підключено",
        "pl": "Połączono",
        "be": "Падключана",
        "kk": "Қосылған",
        "ka": "დაკავშირებულია",
        "es": "Conectado",
        "fr": "Connecté",
        "de": "Verbunden",
        "cs": "Připojeno",
        "zh-Hans": "已连接",
    },
)

t(
    "data_privacy_status_denied",
    **{
        "en-GB": "Permission denied — local store still works",
        "uk": "Дозвіл відхилено — локальне сховище працює",
        "pl": "Odmowa dostępu — lokalny magazyn działa",
        "be": "Дазвол адхілены — лакальнае сховішча працуе",
        "kk": "Рұқсат қабылданбады — жергілікті қойма жұмыс істейді",
        "ka": "ნებართვა უარყოფილია — ლოკალური საცავი მუშაობს",
        "es": "Permiso denegado — el almacén local sigue activo",
        "fr": "Permission refusée — le stockage local fonctionne",
        "de": "Berechtigung verweigert — lokaler Speicher funktioniert",
        "cs": "Oprávnění odepřeno — místní úložiště funguje",
        "zh-Hans": "权限已拒绝 — 本地存储仍可用",
    },
)

t(
    "data_privacy_status_partial",
    **{
        "en-GB": "Partial access",
        "uk": "Частковий доступ",
        "pl": "Częściowy dostęp",
        "be": "Частковы доступ",
        "kk": "Ішінара қолжетімділік",
        "ka": "ნაწილობრივი წვდომა",
        "es": "Acceso parcial",
        "fr": "Accès partiel",
        "de": "Teilzugriff",
        "cs": "Částečný přístup",
        "zh-Hans": "部分访问",
    },
)

t(
    "data_privacy_status_unknown",
    **{
        "en-GB": "Not determined",
        "uk": "Не визначено",
        "pl": "Nie określono",
        "be": "Не вызначана",
        "kk": "Анықталмаған",
        "ka": "განუსაზღვრელი",
        "es": "Sin determinar",
        "fr": "Non déterminé",
        "de": "Nicht festgelegt",
        "cs": "Neurčeno",
        "zh-Hans": "未确定",
    },
)

t(
    "data_privacy_permission_note",
    **{
        "en-GB": (
            "OmniGart can read and write wellness workouts and samples when you allow it. "
            "You can keep logging without granting access. OmniGart never deletes platform "
            "data it did not create, and never writes back data from other apps."
        ),
        "uk": (
            "OmniGart може читати й записувати тренування та зразки самопочуття, якщо ви дозволите. "
            "Можна вести журнал без доступу. OmniGart ніколи не видаляє дані платформи, які не створював, "
            "і не записує назад дані інших додатків."
        ),
        "pl": (
            "OmniGart może czytać i zapisywać treningi oraz próbki wellness, gdy na to pozwolisz. "
            "Możesz logować bez dostępu. OmniGart nie usuwa danych platformy, których nie utworzył, "
            "i nie zapisuje danych z innych aplikacji."
        ),
        "be": (
            "OmniGart можа чытаць і запісваць трэніроўкі і ўзоры самаадчування, калі вы дазволіце. "
            "Можна весьці журнал без доступу. OmniGart ніколі не выдаляе дадзеныя платформы, якія не ствараў, "
            "і не запісвае дадзеныя іншых праграм."
        ),
        "kk": (
            "Рұқсат берсеңіз, OmniGart сауықтыру жаттығуларын және үлгілерін оқи және жаза алады. "
            "Рұқсатсыз журнал жүргізуге болады. OmniGart өзі жасамаган платформа деректерін жоймайды "
            "және басқа қолданба деректерін кері жазбайды."
        ),
        "ka": (
            "OmniGart-ს შეუძლია წაიკითხოს და ჩაიწეროს კეთილდღეობის ვარჯიშები და ნიმუშები, თუ დართავთ. "
            "შეგიძლიათ ჩანაწერი ნებართვის გარეშე. OmniGart არასოდეს შლის პლატფორმის მონაცემებს, "
            "რომლებიც არ შეუქმნია, და არ წერს უკან სხვა აპების მონაცემებს."
        ),
        "es": (
            "OmniGart puede leer y escribir entrenamientos y muestras de bienestar si lo permites. "
            "Puedes seguir registrando sin conceder acceso. OmniGart nunca elimina datos de la plataforma "
            "que no creó, ni reescribe datos de otras apps."
        ),
        "fr": (
            "OmniGart peut lire et écrire des séances et échantillons de bien-être si vous l’autorisez. "
            "Vous pouvez journaliser sans accès. OmniGart ne supprime jamais les données plateforme "
            "qu’il n’a pas créées, et n’écrit pas les données d’autres apps."
        ),
        "de": (
            "OmniGart kann Wellness-Workouts und Messwerte lesen und schreiben, wenn Sie es erlauben. "
            "Protokollieren geht auch ohne Zugriff. OmniGart löscht keine Plattformdaten, die es nicht "
            "erstellt hat, und schreibt keine Daten anderer Apps zurück."
        ),
        "cs": (
            "OmniGart může číst a zapisovat wellness tréninky a vzorky, pokud to povolíte. "
            "Záznamy fungují i bez oprávnění. OmniGart nikdy nemaže data platformy, která nevytvořil, "
            "a nezapisuje zpět data jiných aplikací."
        ),
        "zh-Hans": (
            "在您允许时，OmniGart 可读写健康训练与采样。"
            "未授权时仍可正常记录。OmniGart 从不删除非其创建的平台数据，"
            "也从不回写其他应用的数据。"
        ),
    },
)


def sync_json() -> None:
    for loc in LOCALES:
        path = ROOT / "shared/src/commonMain/resources/i18n" / loc / "strings.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        for key, by_loc in STRINGS.items():
            data[key] = by_loc[loc]
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def android_escape(s: str) -> str:
    return (
        s.replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\n", "\\n")
    )


def sync_android() -> None:
    for loc, folder in ANDROID.items():
        path = ROOT / "androidApp/src/main/res" / folder / "strings.xml"
        text = path.read_text(encoding="utf-8")
        for key, by_loc in STRINGS.items():
            value = android_escape(by_loc[loc])
            pattern = re.compile(rf'<string name="{re.escape(key)}">.*?</string>', re.DOTALL)
            replacement = f'<string name="{key}">{value}</string>'
            if pattern.search(text):
                text = pattern.sub(replacement, text)
            else:
                text = text.replace("</resources>", f"    {replacement}\n</resources>")
        path.write_text(text, encoding="utf-8")
        if loc == "en-GB":
            default = ROOT / "androidApp/src/main/res/values/strings.xml"
            dtext = default.read_text(encoding="utf-8")
            for key, by_loc in STRINGS.items():
                value = android_escape(by_loc[loc])
                pattern = re.compile(rf'<string name="{re.escape(key)}">.*?</string>', re.DOTALL)
                replacement = f'<string name="{key}">{value}</string>'
                if pattern.search(dtext):
                    dtext = pattern.sub(replacement, dtext)
                else:
                    dtext = dtext.replace("</resources>", f"    {replacement}\n</resources>")
            default.write_text(dtext, encoding="utf-8")


def sync_xcstrings() -> None:
    path = ROOT / "iosApp/OmniGart/Localizable.xcstrings"
    data = json.loads(path.read_text(encoding="utf-8"))
    strings = data.setdefault("strings", {})
    for key, by_loc in STRINGS.items():
        entry = strings.setdefault(key, {"localizations": {}})
        locs = entry.setdefault("localizations", {})
        for loc, value in by_loc.items():
            locs[loc] = {
                "stringUnit": {
                    "state": "translated",
                    "value": value,
                }
            }
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    sync_json()
    sync_android()
    sync_xcstrings()
    print(f"Synced {len(STRINGS)} keys × {len(LOCALES)} locales")


if __name__ == "__main__":
    main()
