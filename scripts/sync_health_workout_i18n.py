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
            "active and resting energy, body mass, and hydration — so sessions can appear in "
            "Apple Fitness with duration and calories. Data is not used for ads or sent to "
            "unrelated third parties. You can continue without granting access."
        ),
        "uk": (
            "OmniGart може читати й записувати зразки самопочуття — пульс, ВСР, сон, тренування, "
            "активну й базальну енергію, масу тіла та гідратацію — щоб сесії з’являлися в Apple Fitness "
            "із тривалістю та калоріями. Дані не використовуються для реклами й не передаються "
            "стороннім. Можна продовжити без доступу."
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
