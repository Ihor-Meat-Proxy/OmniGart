#!/usr/bin/env python3
"""Sync food-logging UI strings into shared JSON, Localizable.xcstrings, and Android values*."""

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
    "food_search_title",
    **{
        "en-GB": "Search foods",
        "uk": "Пошук їжі",
        "pl": "Szukaj żywności",
        "be": "Пошук ежы",
        "kk": "Тамақ іздеу",
        "ka": "საკვების ძებნა",
        "es": "Buscar alimentos",
        "fr": "Rechercher des aliments",
        "de": "Lebensmittel suchen",
        "cs": "Hledat potraviny",
        "zh-Hans": "搜索食物",
    },
)
t(
    "food_search_placeholder",
    **{
        "en-GB": "Name or brand",
        "uk": "Назва або бренд",
        "pl": "Nazwa lub marka",
        "be": "Назва або бренд",
        "kk": "Атауы немесе бренд",
        "ka": "სახელი ან ბრენდი",
        "es": "Nombre o marca",
        "fr": "Nom ou marque",
        "de": "Name oder Marke",
        "cs": "Název nebo značka",
        "zh-Hans": "名称或品牌",
    },
)
t(
    "food_meal_group",
    **{
        "en-GB": "Meal",
        "uk": "Прийом їжі",
        "pl": "Posiłek",
        "be": "Прыём ежы",
        "kk": "Тамақ",
        "ka": "კვება",
        "es": "Comida",
        "fr": "Repas",
        "de": "Mahlzeit",
        "cs": "Jídlo",
        "zh-Hans": "餐次",
    },
)
t(
    "food_meal_breakfast",
    **{
        "en-GB": "Breakfast",
        "uk": "Сніданок",
        "pl": "Śniadanie",
        "be": "Сніданак",
        "kk": "Таңғы ас",
        "ka": "საუზმე",
        "es": "Desayuno",
        "fr": "Petit-déjeuner",
        "de": "Frühstück",
        "cs": "Snídaně",
        "zh-Hans": "早餐",
    },
)
t(
    "food_meal_lunch",
    **{
        "en-GB": "Lunch",
        "uk": "Обід",
        "pl": "Obiad",
        "be": "Абед",
        "kk": "Түскі ас",
        "ka": "სადილი",
        "es": "Almuerzo",
        "fr": "Déjeuner",
        "de": "Mittagessen",
        "cs": "Oběd",
        "zh-Hans": "午餐",
    },
)
t(
    "food_meal_dinner",
    **{
        "en-GB": "Dinner",
        "uk": "Вечеря",
        "pl": "Kolacja",
        "be": "Вячэра",
        "kk": "Кешкі ас",
        "ka": "ვახშამი",
        "es": "Cena",
        "fr": "Dîner",
        "de": "Abendessen",
        "cs": "Večeře",
        "zh-Hans": "晚餐",
    },
)
t(
    "food_meal_snacks",
    **{
        "en-GB": "Snacks",
        "uk": "Перекуси",
        "pl": "Przekąski",
        "be": "Перакусы",
        "kk": "Тіскебасар",
        "ka": "სნეკები",
        "es": "Tentempiés",
        "fr": "Collations",
        "de": "Snacks",
        "cs": "Svačiny",
        "zh-Hans": "零食",
    },
)
t(
    "food_gaps_title",
    **{
        "en-GB": "Fill my gaps",
        "uk": "Закрити прогалини",
        "pl": "Uzupełnij braki",
        "be": "Закрыць прабелы",
        "kk": "Бос орындарды толтыру",
        "ka": "შეავსე ხარვეზები",
        "es": "Cubrir carencias",
        "fr": "Combler les manques",
        "de": "Lücken füllen",
        "cs": "Doplnit mezery",
        "zh-Hans": "补足不足",
    },
)
t(
    "food_gaps_none",
    **{
        "en-GB": "No large nutrient gaps versus reference intakes today.",
        "uk": "Сьогодні немає великих прогалин відносно референсного споживання.",
        "pl": "Brak dużych niedoborów względem spożycia referencyjnego dziś.",
        "be": "Сёння няма вялікіх прабелаў адносна рэферэнснага ўжывання.",
        "kk": "Бүгін анықтамалық тұтынуға қатысты үлкен олқылық жоқ.",
        "ka": "დღეს საცნობარო მიღებასთან დიდი ხარვეზი არ ჩანს.",
        "es": "Hoy no hay grandes carencias frente a las ingestas de referencia.",
        "fr": "Pas de grands écarts face aux apports de référence aujourd’hui.",
        "de": "Heute keine großen Lücken gegenüber den Referenzzufuhren.",
        "cs": "Dnes nejsou velké mezery vůči referenčním příjmům.",
        "zh-Hans": "今日相对参考摄入量无明显缺口。",
    },
)
t(
    "food_gaps_hint",
    **{
        "en-GB": "Suggestions use reference intakes and your log. Wellness coaching only — not medical advice.",
        "uk": "Пропозиції базуються на референсному споживанні та вашому журналі. Лише wellness — не медична порада.",
        "pl": "Sugestie opierają się na spożyciu referencyjnym i Twoim dzienniku. Tylko wellness — nie porada medyczna.",
        "be": "Прапановы заснаваны на рэферэнсным ужыванні і вашым журнале. Толькі wellness — не медыцынская парада.",
        "kk": "Ұсыныстар анықтамалық тұтыну мен журналыңызға негізделген. Тек сауықтыру — медициналық кеңес емес.",
        "ka": "რჩევები ეყრდნობა საცნობარო მიღებას და თქვენს ჟურნალს. მხოლოდ კეთილდღეობა — არა სამედიცინო რჩევა.",
        "es": "Las sugerencias usan ingestas de referencia y tu registro. Solo bienestar — no consejo médico.",
        "fr": "Les suggestions s’appuient sur les apports de référence et votre journal. Bien-être seulement — pas un avis médical.",
        "de": "Vorschläge nutzen Referenzzufuhren und Ihr Protokoll. Nur Wellness — keine medizinische Beratung.",
        "cs": "Návrhy vycházejí z referenčních příjmů a vašeho deníku. Jen wellness — ne lékařská rada.",
        "zh-Hans": "建议基于参考摄入量与您的记录。仅供健康参考，不是医疗建议。",
    },
)
t(
    "food_review_title",
    **{
        "en-GB": "Morning food review",
        "uk": "Ранковий огляд їжі",
        "pl": "Poranny przegląd posiłków",
        "be": "Ранішні агляд ежы",
        "kk": "Таңғы тамақ шолуы",
        "ka": "დილის საკვების მიმოხილვა",
        "es": "Resumen matutino de comidas",
        "fr": "Revue alimentaire du matin",
        "de": "Morgen-Essensrückblick",
        "cs": "Ranní přehled jídla",
        "zh-Hans": "晨间饮食回顾",
    },
)
t(
    "food_review_run",
    **{
        "en-GB": "Review yesterday",
        "uk": "Оглянути вчора",
        "pl": "Przejrzyj wczoraj",
        "be": "Прагледзець учора",
        "kk": "Кешені қарау",
        "ka": "გუშინდელის მიმოხილვა",
        "es": "Revisar ayer",
        "fr": "Revoir hier",
        "de": "Gestern prüfen",
        "cs": "Zkontrolovat včerejšek",
        "zh-Hans": "回顾昨日",
    },
)
t(
    "food_review_footer",
    **{
        "en-GB": "This is a wellness summary, not medical advice.",
        "uk": "Це wellness-підсумок, не медична порада.",
        "pl": "To podsumowanie wellness, nie porada medyczna.",
        "be": "Гэта wellness-вынік, не медыцынская парада.",
        "kk": "Бұл сауықтыру қорытындысы, медициналық кеңес емес.",
        "ka": "ეს კეთილდღეობის შეჯამებაა, არა სამედიცინო რჩევა.",
        "es": "Esto es un resumen de bienestar, no consejo médico.",
        "fr": "Ceci est un résumé bien-être, pas un avis médical.",
        "de": "Dies ist eine Wellness-Zusammenfassung, keine medizinische Beratung.",
        "cs": "Toto je wellness shrnutí, ne lékařská rada.",
        "zh-Hans": "这是健康摘要，不是医疗建议。",
    },
)
t(
    "food_review_ai_toggle",
    **{
        "en-GB": "On-device morning review",
        "uk": "Ранковий огляд на пристрої",
        "pl": "Poranny przegląd na urządzeniu",
        "be": "Ранішні агляд на прыладзе",
        "kk": "Құрылғыдағы таңғы шолу",
        "ka": "დილის მიმოხილვა მოწყობილობაზე",
        "es": "Resumen matutino en el dispositivo",
        "fr": "Revue du matin sur l’appareil",
        "de": "Morgenrückblick auf dem Gerät",
        "cs": "Ranní přehled v zařízení",
        "zh-Hans": "设备端晨间回顾",
    },
)
t(
    "food_review_ai_ready",
    **{
        "en-GB": "On-device review is available on this device.",
        "uk": "Огляд на пристрої доступний на цьому пристрої.",
        "pl": "Przegląd na urządzeniu jest dostępny.",
        "be": "Агляд на прыладзе даступны.",
        "kk": "Құрылғыдағы шолу қолжетімді.",
        "ka": "მოწყობილობაზე მიმოხილვა ხელმისაწვდომია.",
        "es": "El resumen en el dispositivo está disponible.",
        "fr": "La revue sur l’appareil est disponible.",
        "de": "Gerätseitiger Rückblick ist verfügbar.",
        "cs": "Přehled v zařízení je k dispozici.",
        "zh-Hans": "此设备支持设备端回顾。",
    },
)
t(
    "food_ai_unavailable",
    **{
        "en-GB": "On-device review is not available. A simple totals summary is used instead.",
        "uk": "Огляд на пристрої недоступний. Натомість використовується простий підсумок.",
        "pl": "Przegląd na urządzeniu niedostępny. Używane jest proste podsumowanie.",
        "be": "Агляд на прыладзе недаступны. Выкарыстоўваецца просты вынік.",
        "kk": "Құрылғыдағы шолу қолжетімсіз. Қарапайым қорытынды қолданылады.",
        "ka": "მოწყობილობაზე მიმოხილვა მიუწვდომელია. გამოიყენება მარტივი შეჯამება.",
        "es": "El resumen en el dispositivo no está disponible. Se usa un resumen sencillo.",
        "fr": "Revue sur l’appareil indisponible. Un résumé simple est utilisé.",
        "de": "Gerätseitiger Rückblick nicht verfügbar. Es wird eine einfache Zusammenfassung genutzt.",
        "cs": "Přehled v zařízení není k dispozici. Použije se jednoduché shrnutí.",
        "zh-Hans": "设备端回顾不可用，将改用简单合计摘要。",
    },
)
t(
    "food_ai_model_not_ready",
    **{
        "en-GB": "The on-device model is not ready yet.",
        "uk": "Модель на пристрої ще не готова.",
        "pl": "Model na urządzeniu nie jest jeszcze gotowy.",
        "be": "Мадэль на прыладзе яшчэ не гатовая.",
        "kk": "Құрылғыдағы модель әлі дайын емес.",
        "ka": "მოწყობილობის მოდელი ჯერ მზად არ არის.",
        "es": "El modelo en el dispositivo aún no está listo.",
        "fr": "Le modèle sur l’appareil n’est pas encore prêt.",
        "de": "Das gerätseitige Modell ist noch nicht bereit.",
        "cs": "Model v zařízení ještě není připraven.",
        "zh-Hans": "设备端模型尚未就绪。",
    },
)
t(
    "food_review_failed",
    **{
        "en-GB": "Could not build a morning review.",
        "uk": "Не вдалося створити ранковий огляд.",
        "pl": "Nie udało się utworzyć porannego przeglądu.",
        "be": "Не ўдалося стварыць ранішні агляд.",
        "kk": "Таңғы шолуды жасау мүмкін болмады.",
        "ka": "დილის მიმოხილვის შექმნა ვერ მოხერხდა.",
        "es": "No se pudo crear el resumen matutino.",
        "fr": "Impossible de créer la revue du matin.",
        "de": "Morgenrückblick konnte nicht erstellt werden.",
        "cs": "Nepodařilo se vytvořit ranní přehled.",
        "zh-Hans": "无法生成晨间回顾。",
    },
)
t(
    "food_attribution_short",
    **{
        "en-GB": "Food data: USDA FoodData Central (public domain) and Open Food Facts (ODbL).",
        "uk": "Дані про їжу: USDA FoodData Central (суспільне надбання) та Open Food Facts (ODbL).",
        "pl": "Dane żywności: USDA FoodData Central (domena publiczna) i Open Food Facts (ODbL).",
        "be": "Дадзеныя пра ежу: USDA FoodData Central (грамадскі набытак) і Open Food Facts (ODbL).",
        "kk": "Тамақ деректері: USDA FoodData Central (қоғамдық игілік) және Open Food Facts (ODbL).",
        "ka": "საკვების მონაცემები: USDA FoodData Central (საჯარო დომენი) და Open Food Facts (ODbL).",
        "es": "Datos de alimentos: USDA FoodData Central (dominio público) y Open Food Facts (ODbL).",
        "fr": "Données alimentaires : USDA FoodData Central (domaine public) et Open Food Facts (ODbL).",
        "de": "Lebensmitteldaten: USDA FoodData Central (Gemeinfreiheit) und Open Food Facts (ODbL).",
        "cs": "Data o potravinách: USDA FoodData Central (volné dílo) a Open Food Facts (ODbL).",
        "zh-Hans": "食物数据：USDA FoodData Central（公有领域）与 Open Food Facts（ODbL）。",
    },
)
t(
    "food_totals_line",
    **{
        "en-GB": "%1$d kcal · P %2$d · C %3$d · F %4$d",
        "uk": "%1$d ккал · Б %2$d · В %3$d · Ж %4$d",
        "pl": "%1$d kcal · B %2$d · W %3$d · T %4$d",
        "be": "%1$d ккал · Б %2$d · В %3$d · Т %4$d",
        "kk": "%1$d ккал · А %2$d · К %3$d · М %4$d",
        "ka": "%1$d კკალ · ც %2$d · ნ %3$d · ცხ %4$d",
        "es": "%1$d kcal · P %2$d · C %3$d · G %4$d",
        "fr": "%1$d kcal · P %2$d · G %3$d · L %4$d",
        "de": "%1$d kcal · E %2$d · KH %3$d · F %4$d",
        "cs": "%1$d kcal · B %2$d · S %3$d · T %4$d",
        "zh-Hans": "%1$d 千卡 · 蛋白 %2$d · 碳水 %3$d · 脂肪 %4$d",
    },
)
t(
    "food_offline_only",
    **{
        "en-GB": "Offline-only food search",
        "uk": "Лише офлайн-пошук їжі",
        "pl": "Tylko wyszukiwanie offline",
        "be": "Толькі афлайн-пошук ежы",
        "kk": "Тек офлайн тамақ іздеу",
        "ka": "მხოლოდ ოფლაინ ძებნა",
        "es": "Búsqueda de alimentos solo sin conexión",
        "fr": "Recherche alimentaire hors ligne uniquement",
        "de": "Nur Offline-Lebensmittelsuche",
        "cs": "Pouze offline vyhledávání potravin",
        "zh-Hans": "仅离线搜索食物",
    },
)
t(
    "food_online_privacy_note",
    **{
        "en-GB": "Online food search sends your query to Open Food Facts.",
        "uk": "Онлайн-пошук їжі надсилає ваш запит до Open Food Facts.",
        "pl": "Wyszukiwanie online wysyła zapytanie do Open Food Facts.",
        "be": "Анлайн-пошук ежы дасылае ваш запыт у Open Food Facts.",
        "kk": "Онлайн тамақ іздеу сұрауыңызды Open Food Facts-қа жібереді.",
        "ka": "ონლაინ ძებნა თქვენს მოთხოვნას Open Food Facts-ს უგზავნის.",
        "es": "La búsqueda en línea envía tu consulta a Open Food Facts.",
        "fr": "La recherche en ligne envoie votre requête à Open Food Facts.",
        "de": "Online-Suche sendet Ihre Anfrage an Open Food Facts.",
        "cs": "Online hledání odesílá váš dotaz do Open Food Facts.",
        "zh-Hans": "在线食物搜索会将您的查询发送至 Open Food Facts。",
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
    print(f"Synced {len(STRINGS)} food-logging keys × {len(LOCALES)} locales")


if __name__ == "__main__":
    main()
