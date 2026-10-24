#!/usr/bin/env python3
"""Sync workout PDF export strings (11 locales) + emit PdfStrings.kt."""

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


# --- titles / actions ---
t(
    "pdf_export_button",
    **{
        "en-GB": "Export PDF",
        "uk": "Експортувати PDF",
        "pl": "Eksportuj PDF",
        "be": "Экспартаваць PDF",
        "kk": "PDF экспорттау",
        "ka": "PDF-ის ექსპორტი",
        "es": "Exportar PDF",
        "fr": "Exporter en PDF",
        "de": "PDF exportieren",
        "cs": "Exportovat PDF",
        "zh-Hans": "导出 PDF",
    },
)
t(
    "pdf_export_range_button",
    **{
        "en-GB": "Export date-range PDF",
        "uk": "Експортувати PDF за період",
        "pl": "Eksportuj PDF z zakresu dat",
        "be": "Экспартаваць PDF за перыяд",
        "kk": "Күн аралығы PDF экспорттау",
        "ka": "თარიღების დიაპაზონის PDF ექსპორტი",
        "es": "Exportar PDF por fechas",
        "fr": "Exporter un PDF sur une période",
        "de": "PDF für Zeitraum exportieren",
        "cs": "Exportovat PDF za období",
        "zh-Hans": "导出日期范围 PDF",
    },
)
t(
    "pdf_report_single_title",
    **{
        "en-GB": "Workout summary",
        "uk": "Підсумок тренування",
        "pl": "Podsumowanie treningu",
        "be": "Вынік трэніроўкі",
        "kk": "Жаттығу қорытындысы",
        "ka": "ვარჯიშის შეჯამება",
        "es": "Resumen del entrenamiento",
        "fr": "Résumé de séance",
        "de": "Workout-Zusammenfassung",
        "cs": "Shrnutí tréninku",
        "zh-Hans": "训练摘要",
    },
)
t(
    "pdf_report_range_title",
    **{
        "en-GB": "Workout report",
        "uk": "Звіт про тренування",
        "pl": "Raport treningowy",
        "be": "Справаздача аб трэніроўках",
        "kk": "Жаттығу есебі",
        "ka": "ვარჯიშის ანგარიში",
        "es": "Informe de entrenamientos",
        "fr": "Rapport d’entraînement",
        "de": "Trainingsbericht",
        "cs": "Tréninková zpráva",
        "zh-Hans": "训练报告",
    },
)
t(
    "pdf_export_ready",
    **{
        "en-GB": "PDF ready to share.",
        "uk": "PDF готовий до поширення.",
        "pl": "PDF gotowy do udostępnienia.",
        "be": "PDF гатовы да перадачы.",
        "kk": "PDF бөлісуге дайын.",
        "ka": "PDF მზად არის გასაზიარებლად.",
        "es": "PDF listo para compartir.",
        "fr": "PDF prêt à partager.",
        "de": "PDF bereit zum Teilen.",
        "cs": "PDF je připraven ke sdílení.",
        "zh-Hans": "PDF 已就绪，可分享。",
    },
)
t(
    "pdf_export_failed",
    **{
        "en-GB": "Could not create the PDF.",
        "uk": "Не вдалося створити PDF.",
        "pl": "Nie udało się utworzyć PDF.",
        "be": "Не атрымалася стварыць PDF.",
        "kk": "PDF жасау мүмкін болмады.",
        "ka": "PDF-ის შექმნა ვერ მოხერხდა.",
        "es": "No se pudo crear el PDF.",
        "fr": "Impossible de créer le PDF.",
        "de": "PDF konnte nicht erstellt werden.",
        "cs": "PDF se nepodařilo vytvořit.",
        "zh-Hans": "无法创建 PDF。",
    },
)

# --- fields ---
FIELDS = {
    "pdf_field_date": ("Date", "Дата", "Data", "Дата", "Күні", "თარიღი", "Fecha", "Date", "Datum", "Datum", "日期"),
    "pdf_field_activity": ("Activity", "Активність", "Aktywność", "Актыўнасць", "Белсенділік", "აქტივობა", "Actividad", "Activité", "Aktivität", "Aktivita", "活动"),
    "pdf_field_duration": ("Duration", "Тривалість", "Czas trwania", "Працягласць", "Ұзақтық", "ხანგრძლივობა", "Duración", "Durée", "Dauer", "Trvání", "时长"),
    "pdf_field_active_kcal": ("Active energy", "Активна енергія", "Energia aktywna", "Актыўная энергія", "Белсенді энергия", "აქტიური ენერგია", "Energía activa", "Énergie active", "Aktive Energie", "Aktivní energie", "活动能量"),
    "pdf_field_total_kcal": ("Total energy", "Загальна енергія", "Energia całkowita", "Агульная энергія", "Жалпы энергия", "ჯამური ენერგია", "Energía total", "Énergie totale", "Gesamtenergie", "Celková energie", "总能量"),
    "pdf_field_avg_hr": ("Average heart rate", "Середній пульс", "Średnie tętno", "Сярэдні пульс", "Орташа жүрек соғысы", "საშუალო პულსი", "Frecuencia cardiaca media", "Fréquence cardiaque moyenne", "Durchschnittliche Herzfrequenz", "Průměrná tepová frekvence", "平均心率"),
    "pdf_field_max_hr": ("Max heart rate", "Максимальний пульс", "Maksymalne tętno", "Максімальны пульс", "Ең жоғары жүрек соғысы", "მაქსიმალური პულსი", "Frecuencia cardiaca máxima", "Fréquence cardiaque max.", "Maximale Herzfrequenz", "Maximální tepová frekvence", "最大心率"),
    "pdf_field_hr_chart": ("Heart rate", "Пульс", "Tętno", "Пульс", "Жүрек соғысы", "პულსი", "Frecuencia cardiaca", "Fréquence cardiaque", "Herzfrequenz", "Tepová frekvence", "心率"),
    "pdf_field_time_in_zone": ("Time in zone", "Час у зоні", "Czas w strefie", "Час у зоне", "Аймақтағы уақыт", "დრო ზონაში", "Tiempo en zona", "Temps en zone", "Zeit in Zone", "Čas v zóně", "区间时间"),
    "pdf_field_distance": ("Distance", "Дистанція", "Dystans", "Дыстанцыя", "Қашықтық", "მანძილი", "Distancia", "Distance", "Distanz", "Vzdálenost", "距离"),
    "pdf_field_pace": ("Pace", "Темп", "Tempo", "Тэмп", "Қарқын", "ტემპი", "Ritmo", "Allure", "Tempo", "Tempo", "配速"),
    "pdf_field_cadence": ("Cadence", "Каденс", "Kadencja", "Кадэнс", "Каденс", "კადენსი", "Cadencia", "Cadence", "Kadenz", "Kadence", "步频"),
    "pdf_field_total_weight": ("Total weight lifted", "Загальна піднята вага", "Łączny ciężar", "Агульная паднятая вага", "Жалпы көтерілген салмақ", "ჯამური აწეული წონა", "Peso total levantado", "Charge totale soulevée", "Gesamtgewicht gehoben", "Celková zvednutá váha", "总举起重量"),
    "pdf_field_sets": ("Sets", "Підходи", "Serie", "Падыходы", "Жақындаулар", "მიდგომები", "Series", "Séries", "Sätze", "Série", "组数"),
    "pdf_field_segments": ("Segments", "Сегменти", "Segmenty", "Сегменты", "Сегменттер", "სეგმენტები", "Segmentos", "Segments", "Segmente", "Segmenty", "分段"),
    "pdf_field_muscle_map": ("Muscle map", "Карта м’язів", "Mapa mięśni", "Карта мышцаў", "Бұлшықет картасы", "კუნთების რუკა", "Mapa muscular", "Carte musculaire", "Muskelkarte", "Svalová mapa", "肌肉图"),
    "pdf_zone_prefix": ("Z", "Z", "Z", "Z", "Z", "Z", "Z", "Z", "Z", "Z", "Z"),
    "pdf_minutes_suffix": ("min", "хв", "min", "хв", "мин", "წთ", "min", "min", "Min", "min", "分钟"),
    "pdf_unit_kg": ("kg", "кг", "kg", "кг", "кг", "კგ", "kg", "kg", "kg", "kg", "公斤"),
}

for key, values in FIELDS.items():
    t(key, **dict(zip(LOCALES, values)))

ACTIVITIES = {
    "pdf_activity_strength": ("Strength", "Сила", "Siła", "Сіла", "Күш", "ძალა", "Fuerza", "Force", "Kraft", "Síla", "力量"),
    "pdf_activity_running": ("Running", "Біг", "Bieg", "Бег", "Жүгіру", "სირბილი", "Carrera", "Course", "Laufen", "Běh", "跑步"),
    "pdf_activity_cycling": ("Cycling", "Велосипед", "Rower", "Веласіпед", "Велосипед", "ველოსიპედი", "Ciclismo", "Cyclisme", "Radfahren", "Cyklistika", "骑行"),
    "pdf_activity_swimming": ("Swimming", "Плавання", "Pływanie", "Плаванне", "Жүзу", "ცურვა", "Natación", "Natation", "Schwimmen", "Plavání", "游泳"),
    "pdf_activity_walking": ("Walking", "Ходьба", "Chód", "Хадзьба", "Жүру", "სიარული", "Caminata", "Marche", "Gehen", "Chůze", "步行"),
    "pdf_activity_hiit": ("HIIT", "HIIT", "HIIT", "HIIT", "HIIT", "HIIT", "HIIT", "HIIT", "HIIT", "HIIT", "高强度间歇"),
    "pdf_activity_yoga": ("Yoga", "Йога", "Joga", "Ёга", "Йога", "იოგა", "Yoga", "Yoga", "Yoga", "Jóga", "瑜伽"),
    "pdf_activity_pilates": ("Pilates", "Пілатес", "Pilates", "Пілатэс", "Пилатес", "პილატესი", "Pilates", "Pilates", "Pilates", "Pilates", "普拉提"),
    "pdf_activity_other": ("Other", "Інше", "Inne", "Іншае", "Басқа", "სხვა", "Otro", "Autre", "Sonstiges", "Jiné", "其他"),
}
for key, values in ACTIVITIES.items():
    t(key, **dict(zip(LOCALES, values)))


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
            locs[loc] = {"stringUnit": {"state": "translated", "value": value}}
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def kotlin_escape(s: str) -> str:
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")


def emit_kotlin() -> None:
    out = ROOT / "shared/src/commonMain/kotlin/com/omnigart/shared/health/store/pdf/PdfStrings.kt"
    lines = [
        "package com.omnigart.shared.health.store.pdf",
        "",
        "/** Generated by scripts/sync_health_pdf_i18n.py — do not edit by hand. */",
        "object PdfStrings {",
        "    private val tables: Map<String, Map<String, String>> = mapOf(",
    ]
    for loc in LOCALES:
        lines.append(f'        "{loc}" to mapOf(')
        for key, by_loc in STRINGS.items():
            lines.append(f'            "{key}" to "{kotlin_escape(by_loc[loc])}",')
        # wellness disclaimer from en-GB/uk catalogs is long — pull from strings.json
        lines.append("        ),")
    lines.append("    )")
    lines.append("")
    lines.append("    fun t(key: String, locale: String): String {")
    lines.append('        val normalized = locale.replace(\'_\', \'-\')')
    lines.append("        val table = tables[normalized] ?: tables[normalized.substringBefore('-')] ?: tables.getValue(\"en-GB\")")
    lines.append('        return table[key] ?: tables.getValue("en-GB")[key] ?: key')
    lines.append("    }")
    lines.append("}")
    lines.append("")
    out.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    sync_json()
    sync_android()
    sync_xcstrings()
    emit_kotlin()
    print(f"Synced {len(STRINGS)} PDF keys × {len(LOCALES)} locales")


if __name__ == "__main__":
    main()
