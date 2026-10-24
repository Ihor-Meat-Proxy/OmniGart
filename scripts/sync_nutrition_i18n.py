#!/usr/bin/env python3
"""Sync nutrition UI strings into shared JSON, Localizable.xcstrings, and Android values*."""

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


def L(
    en: str,
    uk: str,
    pl: str,
    be: str,
    kk: str,
    ka: str,
    es: str,
    fr: str,
    de: str,
    cs: str,
    zh: str,
) -> dict[str, str]:
    return {
        "en-GB": en,
        "uk": uk,
        "pl": pl,
        "be": be,
        "kk": kk,
        "ka": ka,
        "es": es,
        "fr": fr,
        "de": de,
        "cs": cs,
        "zh-Hans": zh,
    }


t(
    "nutrition_target_title",
    **L(
        "Suggested energy target",
        "Орієнтовна енергетична ціль",
        "Sugerowany cel energetyczny",
        "Арыентоўная энергетычная мэта",
        "Ұсынылған энергия мақсаты",
        "შემოთავაზებული ენერგიის მიზანი",
        "Objetivo energético sugerido",
        "Objectif énergétique suggéré",
        "Empfohlenes Energieziel",
        "Navrhovaný energetický cíl",
        "建议能量目标",
    ),
)
t(
    "nutrition_target_disclaimer",
    **L(
        "Estimated daily energy — a wellness coaching aid, not medical or dietary advice.",
        "Орієнтовна денна енергія — тренерська підказка, не медична чи дієтологічна порада.",
        "Szacowana energia dzienna — wskazówka wellness, nie porada medyczna ani dietetyczna.",
        "Арыентоўная дзённая энергія — трэнерская падказка, не медыцынская ці дыеталагічная парада.",
        "Болжалды күнделікті энергия — сауықтыру кеңесі, медициналық немесе диеталық кеңес емес.",
        "სავარაუდო დღიური ენერგია — კეთილდღეობის რჩევა, არა სამედიცინო ან დიეტური კონსულტაცია.",
        "Energía diaria estimada: ayuda de bienestar, no consejo médico ni dietético.",
        "Énergie quotidienne estimée — aide bien-être, pas un avis médical ou diététique.",
        "Geschätzte Tagesenergie — Wellness-Hilfe, keine medizinische oder ernährungsmedizinische Beratung.",
        "Odhadovaná denní energie — wellness pomůcka, ne lékařská ani dietní rada.",
        "估计每日能量——健康教练参考，不是医疗或饮食建议。",
    ),
)
t(
    "nutrition_confidence_title",
    **L(
        "Confidence",
        "Упевненість",
        "Pewność",
        "Упэўненасць",
        "Сенімділік",
        "სანდოობა",
        "Confianza",
        "Confiance",
        "Vertrauen",
        "Spolehlivost",
        "可信度",
    ),
)
t(
    "nutrition_confidence_rough",
    **L(
        "Rough",
        "Груба",
        "Przybliżona",
        "Грубая",
        "Шамамен",
        "მიახლოებითი",
        "Aproximada",
        "Approximative",
        "Grobe Schätzung",
        "Hrubý odhad",
        "粗略",
    ),
)
t(
    "nutrition_confidence_good",
    **L(
        "Good",
        "Добра",
        "Dobra",
        "Добрая",
        "Жақсы",
        "კარგი",
        "Buena",
        "Bonne",
        "Gut",
        "Dobrá",
        "良好",
    ),
)
t(
    "nutrition_confidence_personalised",
    **L(
        "Personalised",
        "Персоналізована",
        "Spersonalizowana",
        "Персаналізаваная",
        "Жекелендірілген",
        "პერსონალიზებული",
        "Personalizada",
        "Personnalisée",
        "Personalisiert",
        "Personalizovaná",
        "个性化",
    ),
)
t(
    "nutrition_reason_title",
    **L(
        "Why this changed",
        "Чому це змінилося",
        "Dlaczego to się zmieniło",
        "Чаму гэта змянілася",
        "Неге өзгерді",
        "რატომ შეიცვალა",
        "Por qué cambió",
        "Pourquoi cela a changé",
        "Warum sich das geändert hat",
        "Proč se to změnilo",
        "为何有此变化",
    ),
)
t(
    "nutrition_reason_no_weight",
    **L(
        "Add a weigh-in to see a suggested target.",
        "Додайте зважування, щоб побачити орієнтовну ціль.",
        "Dodaj ważenie, aby zobaczyć sugerowany cel.",
        "Дадайце ўзважванне, каб убачыць арыентоўную мэту.",
        "Ұсынылған мақсатты көру үшін салмақ жазыңыз.",
        "შემოთავაზებული მიზნის სანახავად დაამატეთ აწონვა.",
        "Añade un pesaje para ver un objetivo sugerido.",
        "Ajoutez une pesée pour voir un objectif suggéré.",
        "Fügen Sie eine Wägung hinzu, um ein empfohlenes Ziel zu sehen.",
        "Přidejte vážení, abyste viděli navrhovaný cíl.",
        "添加一次称重以查看建议目标。",
    ),
)
t(
    "nutrition_reason_rough_bmr",
    **L(
        "Height or age missing — showing a rough estimate.",
        "Немає зросту чи віку — показано грубу оцінку.",
        "Brak wzrostu lub wieku — pokazano przybliżony szacunek.",
        "Няма росту ці ўзросту — паказана грубая ацэнка.",
        "Бой немесе жас жоқ — шамамен бағалау көрсетілді.",
        "სიმაღლე ან ასაკი აკლია — ნაჩვენებია მიახლოებითი შეფასება.",
        "Falta altura o edad: se muestra una estimación aproximada.",
        "Taille ou âge manquant — estimation approximative.",
        "Größe oder Alter fehlt — grobe Schätzung.",
        "Chybí výška nebo věk — hrubý odhad.",
        "缺少身高或年龄——显示粗略估计。",
    ),
)
t(
    "nutrition_reason_baseline",
    **L(
        "Based on estimated resting energy and activity.",
        "На основі орієнтовної енергії спокою та активності.",
        "Na podstawie szacowanej energii spoczynkowej i aktywności.",
        "На аснове арыентоўнай энергіі спакою і актыўнасці.",
        "Демалыс энергиясы мен белсенділік бағасына негізделген.",
        "დასვენების ენერგიისა და აქტივობის შეფასებაზე დაყრდნობით.",
        "Basado en la energía en reposo y la actividad estimadas.",
        "Basé sur l’énergie de repos et l’activité estimées.",
        "Basierend auf geschätzter Ruheenergie und Aktivität.",
        "Na základě odhadované klidové energie a aktivity.",
        "基于估计的静息能量与活动量。",
    ),
)
t(
    "nutrition_reason_adaptive_ewma",
    **L(
        "Updated from your recent weigh-ins and food log.",
        "Оновлено за нещодавніми зважуваннями та журналом їжі.",
        "Zaktualizowano na podstawie ostatnich ważeń i dziennika posiłków.",
        "Абноўлена па нядаўніх узважваннях і журнале ежы.",
        "Соңғы салмақ пен тамақ журналы бойынша жаңартылды.",
        "განახლდა ბოლო აწონვებისა და საკვების ჟურნალის მიხედვით.",
        "Actualizado con tus pesajes y registro de comidas recientes.",
        "Mis à jour d’après vos pesées et journal alimentaire récents.",
        "Aktualisiert anhand Ihrer letzten Wägungen und Essensprotokolle.",
        "Aktualizováno podle nedávných vážení a jídelníčku.",
        "已根据近期称重与饮食记录更新。",
    ),
)
t(
    "nutrition_reason_energy_balance",
    **L(
        "Updated from your 21-day energy balance trend.",
        "Оновлено за 21-денним трендом енергетичного балансу.",
        "Zaktualizowano na podstawie 21-dniowego bilansu energii.",
        "Абноўлена па 21-дзённым трэндзе энергетычнага балансу.",
        "21 күндік энергия теңгерімі үрдісі бойынша жаңартылды.",
        "განახლდა 21-დღიანი ენერგეტიკული ბალანსის ტრენდით.",
        "Actualizado con tu tendencia de balance energético de 21 días.",
        "Mis à jour d’après votre tendance de bilan énergétique sur 21 jours.",
        "Aktualisiert anhand Ihres 21-Tage-Energiebilanztrends.",
        "Aktualizováno podle 21denního trendu energetické bilance.",
        "已根据您的 21 天能量平衡趋势更新。",
    ),
)
t(
    "nutrition_reason_paused_insufficient",
    **L(
        "Not enough recent logs — keeping your last estimate.",
        "Недостатньо нещодавніх записів — залишаємо попередню оцінку.",
        "Za mało ostatnich wpisów — zostawiamy poprzedni szacunek.",
        "Недастаткова нядаўніх запісаў — пакідаем папярэднюю ацэнку.",
        "Соңғы жазбалар жеткіліксіз — соңғы бағалау сақталады.",
        "ბოლო ჩანაწერები საკმარისი არ არის — რჩება წინა შეფასება.",
        "No hay suficientes registros recientes: se mantiene la última estimación.",
        "Pas assez de journaux récents — conservation de la dernière estimation.",
        "Zu wenige aktuelle Einträge — letzte Schätzung wird beibehalten.",
        "Nedostatek nedávných záznamů — ponecháváme poslední odhad.",
        "近期记录不足——保留上次估计。",
    ),
)
t(
    "nutrition_reason_logging_break",
    **L(
        "Logging break detected. Resume when you are ready — no pressure.",
        "Виявлено перерву в записах. Продовжуйте, коли будете готові — без тиску.",
        "Wykryto przerwę w logowaniu. Wróć, gdy będziesz gotowy — bez presji.",
        "Выяўлены перапынак у запісах. Працягвайце, калі будзеце гатовыя — без ціску.",
        "Жазба үзілісі байқалды. Дайын болғанда жалғастырыңыз — қысымсыз.",
        "ჩაწერის შესვენება აღმოჩნდა. გააგრძელეთ მზადყოფნისას — უზეწოლოდ.",
        "Se detectó una pausa en el registro. Continúa cuando quieras, sin presión.",
        "Pause de journalisation détectée. Reprenez quand vous voulez — sans pression.",
        "Protokollpause erkannt. Machen Sie weiter, wenn Sie bereit sind — ohne Druck.",
        "Zjištěna pauza v záznamech. Pokračujte, až budete připraveni — bez tlaku.",
        "检测到记录中断。准备好再继续即可——无需压力。",
    ),
)
t(
    "nutrition_reason_floor",
    **L(
        "Suggested energy was raised to stay at or above a safe floor.",
        "Орієнтовну енергію підвищено до безпечної мінімальної межі.",
        "Sugerowaną energię podniesiono do bezpiecznego minimum.",
        "Арыентоўную энергію павысілі да бяспечнай мінімальнай мяжы.",
        "Ұсынылған энергия қауіпсіз төмен шекке дейін көтерілді.",
        "შემოთავაზებული ენერგია აიწია უსაფრთხო მინიმუმამდე.",
        "Se subió la energía sugerida para no quedar bajo un mínimo seguro.",
        "L’énergie suggérée a été relevée pour rester au-dessus d’un plancher sûr.",
        "Empfohlene Energie wurde angehoben, um eine sichere Untergrenze einzuhalten.",
        "Navrhovaná energie byla zvýšena, aby zůstala nad bezpečnou spodní hranicí.",
        "已提高建议能量，使其不低于安全下限。",
    ),
)
t(
    "nutrition_reason_auto_raise",
    **L(
        "Recent loss looked faster than planned — target nudged up.",
        "Нещодавнє зниження виглядало швидшим за план — ціль трохи піднято.",
        "Ostatni spadek był szybszy niż plan — cel lekko podniesiono.",
        "Нядаўняе зніжэнне выглядала хутчэйшым за план — мэту трохі паднялі.",
        "Соңғы төмендеу жоспардан жылдам көрінді — мақсат сәл көтерілді.",
        "ბოლო კლება გეგმაზე სწრაფი იყო — მიზანი ოდნავ აიწია.",
        "La pérdida reciente pareció más rápida de lo previsto: se subió un poco el objetivo.",
        "La perte récente semblait plus rapide que prévu — objectif légèrement relevé.",
        "Neuerer Verlust wirkte schneller als geplant — Ziel leicht angehoben.",
        "Nedávný úbytek vypadal rychlejší než plán — cíl mírně zvýšen.",
        "近期减重快于计划——目标略微上调。",
    ),
)
t(
    "nutrition_reason_exercise_credit",
    **L(
        "Includes a partial credit for today’s movement.",
        "Включено частковий кредит за сьогоднішній рух.",
        "Uwzględniono częściowy kredyt za dzisiejszy ruch.",
        "Уключаны частковы крэдыт за сённяшні рух.",
        "Бүгінгі қозғалыс үшін ішінара кредит қосылды.",
        "დღევანდელი მოძრაობის ნაწილობრივი კრედიტი შედის.",
        "Incluye un crédito parcial por el movimiento de hoy.",
        "Inclut un crédit partiel pour le mouvement du jour.",
        "Enthält eine teilweise Anrechnung für die heutige Bewegung.",
        "Zahrnuje částečný kredit za dnešní pohyb.",
        "已计入今日活动的部分能量。",
    ),
)
t(
    "nutrition_floor_warning",
    **L(
        "This value is below the suggested minimum energy floor.",
        "Це значення нижче за рекомендовану мінімальну енергію.",
        "Ta wartość jest poniżej sugerowanego minimalnego poziomu energii.",
        "Гэта значэнне ніжэй за рэкамендаваную мінімальную энергію.",
        "Бұл мән ұсынылған ең төмен энергия шегінен төмен.",
        "ეს მნიშვნელობა შემოთავაზებულ მინიმალურ ენერგიაზე დაბალია.",
        "Este valor está por debajo del mínimo energético sugerido.",
        "Cette valeur est sous le plancher énergétique suggéré.",
        "Dieser Wert liegt unter der empfohlenen Mindestenergie.",
        "Tato hodnota je pod navrhovanou minimální energií.",
        "该值低于建议的最低能量下限。",
    ),
)
t(
    "nutrition_goal_settings_title",
    **L(
        "Energy goal and rate",
        "Енергетична ціль і темп",
        "Cel energetyczny i tempo",
        "Энергетычная мэта і тэмп",
        "Энергия мақсаты мен қарқыны",
        "ენერგიის მიზანი და ტემპი",
        "Objetivo energético y ritmo",
        "Objectif énergétique et rythme",
        "Energieziel und Tempo",
        "Energetický cíl a tempo",
        "能量目标与速率",
    ),
)
t(
    "nutrition_goal_first_set",
    **L(
        "If you have a health condition, are pregnant or under 18, check with a healthcare professional.",
        "Якщо маєте стан здоров’я, вагітні або молодші за 18, звертесь до фахівця.",
        "Jeśli masz problem zdrowotny, jesteś w ciąży lub masz mniej niż 18 lat, skonsultuj się ze specjalistą.",
        "Калі маеце стан здароўя, цяжарныя ці маладзейшыя за 18, звярніцеся да спецыяліста.",
        "Денсаулық жағдайы болса, жүкті болсаңыз немесе 18 жасқа толмасаңыз, маманға қаралыңыз.",
        "თუ გაქვთ ჯანმრთელობის მდგომარეობა, ორსულად ხართ ან 18 წლამდე ხართ, მიმართეთ სპეციალისტს.",
        "Si tienes una condición de salud, estás embarazada o eres menor de 18, consulta a un profesional.",
        "En cas de problème de santé, de grossesse ou si vous avez moins de 18 ans, consultez un professionnel.",
        "Bei einer Erkrankung, Schwangerschaft oder unter 18 Jahren bitte Fachperson fragen.",
        "Pokud máte zdravotní potíže, jste těhotná nebo mladší 18 let, poraďte se s odborníkem.",
        "如有健康状况、怀孕或未满 18 岁，请咨询专业人士。",
    ),
)
t(
    "nutrition_mode_guided",
    **L(
        "Guided",
        "Керована",
        "Prowadzony",
        "Кіраваны",
        "Басқарылатын",
        "მართული",
        "Guiado",
        "Guidé",
        "Geführt",
        "Řízený",
        "引导",
    ),
)
t(
    "nutrition_mode_flexible",
    **L(
        "Flexible weekly",
        "Гнучкий тиждень",
        "Elastyczny tydzień",
        "Гнуткі тыдзень",
        "Икемді апта",
        "მოქნილი კვირა",
        "Semanal flexible",
        "Hebdomadaire flexible",
        "Flexibel wöchentlich",
        "Flexibilní týden",
        "灵活周预算",
    ),
)
t(
    "nutrition_mode_manual",
    **L(
        "Manual",
        "Вручну",
        "Ręczny",
        "Уручную",
        "Қолмен",
        "ხელით",
        "Manual",
        "Manuel",
        "Manuell",
        "Ruční",
        "手动",
    ),
)
t(
    "nutrition_goal_lose",
    **L(
        "Lose",
        "Зниження",
        "Redukcja",
        "Зніжэнне",
        "Азайту",
        "კლება",
        "Perder",
        "Perte",
        "Abnehmen",
        "Hubnutí",
        "减重",
    ),
)
t(
    "nutrition_goal_maintain",
    **L(
        "Maintain",
        "Підтримання",
        "Utrzymanie",
        "Падтрыманне",
        "Ұстау",
        "შენარჩუნება",
        "Mantener",
        "Maintien",
        "Halten",
        "Udržení",
        "维持",
    ),
)
t(
    "nutrition_goal_gain",
    **L(
        "Gain",
        "Набір",
        "Przyrost",
        "Набор",
        "Қосу",
        "მატება",
        "Ganar",
        "Prise",
        "Zunehmen",
        "Přibírání",
        "增重",
    ),
)
t(
    "nutrition_measurements_title",
    **L(
        "Body measurements",
        "Обхвати тіла",
        "Pomiary ciała",
        "Абхваты цела",
        "Дене өлшемдері",
        "სხეულის გაზომვები",
        "Medidas corporales",
        "Mensurations",
        "Körpermaße",
        "Tělesné míry",
        "身体围度",
    ),
)
t(
    "nutrition_measurements_intro",
    **L(
        "Stored on this device. Waist-to-height is shown for awareness and is not used in the energy formula.",
        "Зберігається на цьому пристрої. Співвідношення талія/зріст показано для орієнтиру і не входить у формулу енергії.",
        "Przechowywane na tym urządzeniu. Stosunek talia/wzrost jest tylko informacyjny i nie wchodzi do wzoru energii.",
        "Захоўваецца на гэтай прыладзе. Суадносіны талія/рост паказаны для арыенціру і не ўваходзяць у формулу энергіі.",
        "Осы құрылғыда сақталады. Бел/бой қатынасы ақпарат үшін көрсетіледі және энергия формуласына кірмейді.",
        "ინახება ამ მოწყობილობაზე. წელი/სიმაღლე ნაჩვენებია ინფორმაციისთვის და ენერგიის ფორმულაში არ შედის.",
        "Se guarda en este dispositivo. La relación cintura/altura es informativa y no entra en la fórmula de energía.",
        "Conservé sur cet appareil. Le rapport taille/hauteur est indicatif et n’entre pas dans la formule d’énergie.",
        "Wird auf diesem Gerät gespeichert. Taille-zu-Größe dient der Orientierung und fließt nicht in die Energieformel ein.",
        "Uloženo v tomto zařízení. Poměr pas/výška je informativní a nevstupuje do vzorce energie.",
        "保存在本设备。腰高比仅供参考，不参与能量公式。",
    ),
)
t(
    "nutrition_site_waist",
    **L("Waist", "Талія", "Talia", "Талія", "Бел", "წელი", "Cintura", "Taille", "Taille", "Pas", "腰围"),
)
t(
    "nutrition_site_hips",
    **L("Hips", "Стегна", "Biodra", "Сцёгны", "Жамбас", "თეძოები", "Cadera", "Hanches", "Hüfte", "Boky", "臀围"),
)
t(
    "nutrition_site_chest",
    **L("Chest", "Груди", "Klatka", "Грудзі", "Кеуде", "გულმკერდი", "Pecho", "Poitrine", "Brust", "Hruď", "胸围"),
)
t(
    "nutrition_site_arm",
    **L("Arm", "Рука", "Ramię", "Рука", "Қол", "მკლავი", "Brazo", "Bras", "Arm", "Paže", "臂围"),
)
t(
    "nutrition_site_thigh",
    **L("Thigh", "Стегно", "Udo", "Сцягно", "Сан", "ბარძაყი", "Muslo", "Cuisse", "Oberschenkel", "Stehno", "大腿围"),
)
t(
    "nutrition_body_fat",
    **L(
        "Body fat %",
        "Жир %",
        "Tkanka tłuszczowa %",
        "Тлушч %",
        "Май %",
        "ცხიმი %",
        "Grasa corporal %",
        "Masse grasse %",
        "Körperfett %",
        "Tělesný tuk %",
        "体脂 %",
    ),
)
t(
    "nutrition_weight",
    **L("Weight", "Вага", "Waga", "Вага", "Салмақ", "წონა", "Peso", "Poids", "Gewicht", "Hmotnost", "体重"),
)
t(
    "nutrition_save_measurement",
    **L(
        "Save measurement",
        "Зберегти вимір",
        "Zapisz pomiar",
        "Захаваць вымярэнне",
        "Өлшемді сақтау",
        "გაზომვის შენახვა",
        "Guardar medida",
        "Enregistrer la mesure",
        "Messung speichern",
        "Uložit měření",
        "保存测量",
    ),
)
t(
    "nutrition_reminders_title",
    **L(
        "Nutrition reminders",
        "Нагадування про харчування",
        "Przypomnienia żywieniowe",
        "Нагадванні пра харчаванне",
        "Тамақтану еске салғыштары",
        "კვების შეხსენებები",
        "Recordatorios de nutrición",
        "Rappels nutrition",
        "Ernährungserinnerungen",
        "Výživová připomenutí",
        "营养提醒",
    ),
)
t(
    "nutrition_reminders_pause",
    **L(
        "Pause reminders",
        "Призупинити нагадування",
        "Wstrzymaj przypomnienia",
        "Прыпыніць нагадванні",
        "Еске салғыштарды тоқтату",
        "შეხსენებების პაუზა",
        "Pausar recordatorios",
        "Mettre les rappels en pause",
        "Erinnerungen pausieren",
        "Pozastavit připomenutí",
        "暂停提醒",
    ),
)
t(
    "nutrition_reminders_weigh_in",
    **L(
        "Weigh-in nudge after 3 days",
        "Нагадування зважитися після 3 днів",
        "Przypomnienie o ważeniu po 3 dniach",
        "Нагадванне ўзважыцца пасля 3 дзён",
        "3 күннен кейін салмақ еске салғыш",
        "აწონვის შეხსენება 3 დღის შემდეგ",
        "Aviso de pesaje tras 3 días",
        "Rappel de pesée après 3 jours",
        "Wäge-Hinweis nach 3 Tagen",
        "Připomenutí vážení po 3 dnech",
        "3 天后称重提醒",
    ),
)
t(
    "nutrition_reminders_measurements",
    **L(
        "Measurements every 2–4 weeks",
        "Обхвати кожні 2–4 тижні",
        "Pomiary co 2–4 tygodnie",
        "Абхваты кожныя 2–4 тыдні",
        "Өлшемдер әр 2–4 аптада",
        "გაზომვები ყოველ 2–4 კვირაში",
        "Medidas cada 2–4 semanas",
        "Mensurations toutes les 2–4 semaines",
        "Maße alle 2–4 Wochen",
        "Míry každých 2–4 týdny",
        "每 2–4 周测量围度",
    ),
)
t(
    "nutrition_checkin_title",
    **L(
        "Weekly check-in",
        "Тижнева перевірка",
        "Cotygodniowy przegląd",
        "Тыднёвая праверка",
        "Апталық тексеру",
        "ყოველკვირეული შემოწმება",
        "Revisión semanal",
        "Bilan hebdomadaire",
        "Wöchentlicher Check-in",
        "Týdenní kontrola",
        "每周回顾",
    ),
)
t(
    "nutrition_whtr_label",
    **L(
        "Waist-to-height",
        "Талія до зросту",
        "Talia do wzrostu",
        "Талія да росту",
        "Бел/бой",
        "წელი/სიმაღლე",
        "Cintura/altura",
        "Taille/hauteur",
        "Taille zu Größe",
        "Pas/výška",
        "腰高比",
    ),
)
t(
    "nutrition_low_intake_title",
    **L(
        "We’re here if you need support",
        "Ми поруч, якщо потрібна підтримка",
        "Jesteśmy tu, jeśli potrzebujesz wsparcia",
        "Мы побач, калі патрэбная падтрымка",
        "Қолдау керек болса, біз жақынбыз",
        "აქ ვართ, თუ მხარდაჭერა გჭირდებათ",
        "Estamos aquí si necesitas apoyo",
        "Nous sommes là si vous avez besoin de soutien",
        "Wir sind da, wenn Sie Unterstützung brauchen",
        "Jsme tu, pokud potřebujete podporu",
        "如需支持，我们在这里",
    ),
)
t(
    "nutrition_low_intake_body",
    **L(
        "Your recent logged energy looks very low. Consider Beat, NEDA, or a local helpline. You can hide calorie numbers in settings.",
        "Нещодавно записана енергія виглядає дуже низькою. Зверніться до Beat, NEDA або місцевої лінії допомоги. Калорії можна приховати в налаштуваннях.",
        "Ostatnio zapisana energia wygląda bardzo nisko. Rozważ Beat, NEDA lub lokalną infolinię. Kalorie możesz ukryć w ustawieniach.",
        "Нядаўна запісаная энергія выглядае вельмі нізкай. Звярніцеся да Beat, NEDA ці мясцовай лініі дапамогі. Калорыі можна схаваць у наладах.",
        "Соңғы жазылған энергия өте төмен көрінеді. Beat, NEDA немесе жергілікті желіні қарастырыңыз. Калорияларды баптаудан жасыруға болады.",
        "ბოლო ჩაწერილი ენერგია ძალიან დაბალი ჩანს. განიხილეთ Beat, NEDA ან ადგილობრივი ხაზი. კალორიების დამალვა შესაძლებელია პარამეტრებში.",
        "La energía registrada reciente parece muy baja. Considera Beat, NEDA o una línea local. Puedes ocultar las calorías en ajustes.",
        "L’énergie récemment enregistrée semble très basse. Envisagez Beat, NEDA ou une ligne locale. Vous pouvez masquer les calories dans les réglages.",
        "Ihre zuletzt protokollierte Energie wirkt sehr niedrig. Beat, NEDA oder eine lokale Hotline können helfen. Kalorien lassen sich in den Einstellungen ausblenden.",
        "Nedávno zaznamenaná energie vypadá velmi nízko. Zvažte Beat, NEDA nebo místní linku. Kalorie lze skrýt v nastavení.",
        "近期记录的能量看起来很低。可考虑 Beat、NEDA 或本地热线。可在设置中隐藏卡路里数字。",
    ),
)
t(
    "nutrition_open_measurements",
    **L(
        "Log measurements",
        "Записати обхвати",
        "Zapisz pomiary",
        "Запісаць абхваты",
        "Өлшемдерді жазу",
        "გაზომვების ჩაწერა",
        "Registrar medidas",
        "Enregistrer les mensurations",
        "Maße erfassen",
        "Zaznamenat míry",
        "记录围度",
    ),
)
t(
    "nutrition_kcal_unit",
    **L("kcal", "ккал", "kcal", "ккал", "ккал", "კკალ", "kcal", "kcal", "kcal", "kcal", "千卡"),
)
t(
    "nutrition_day_start",
    **L(
        "Nutrition day starts at",
        "День харчування починається о",
        "Dzień żywieniowy zaczyna się o",
        "Дзень харчавання пачынаецца а",
        "Тамақтану күні басталады",
        "კვების დღე იწყება",
        "El día nutricional empieza a las",
        "La journée nutrition commence à",
        "Ernährungstag beginnt um",
        "Nutriční den začíná v",
        "营养日起始于",
    ),
)
t(
    "nutrition_add_exercise",
    **L(
        "Add a partial credit for exercise energy",
        "Додати частковий кредит за енергію руху",
        "Dodaj częściowy kredyt za energię ruchu",
        "Дадаць частковы крэдыт за энергію руху",
        "Қозғалыс энергиясына ішінара кредит қосу",
        "მოძრაობის ენერგიის ნაწილობრივი კრედიტის დამატება",
        "Añadir un crédito parcial por la energía del ejercicio",
        "Ajouter un crédit partiel pour l’énergie d’exercice",
        "Teilweise Anrechnung der Bewegungsenergie",
        "Přidat částečný kredit za energii z pohybu",
        "为运动能量计入部分额度",
    ),
)
t(
    "nutrition_target_weight",
    **L(
        "Target weight (kg)",
        "Цільова вага (кг)",
        "Docelowa masa (kg)",
        "Мэтавая вага (кг)",
        "Мақсатты салмақ (кг)",
        "სამიზნე წონა (კგ)",
        "Peso objetivo (kg)",
        "Poids cible (kg)",
        "Zielgewicht (kg)",
        "Cílová hmotnost (kg)",
        "目标体重 (kg)",
    ),
)
t(
    "nutrition_water_goal",
    **L(
        "Water goal",
        "Ціль по воді",
        "Cel wodny",
        "Мэта па вадзе",
        "Су мақсаты",
        "წყლის მიზანი",
        "Objetivo de agua",
        "Objectif d’eau",
        "Wasserziel",
        "Cíl vody",
        "饮水目标",
    ),
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
    print(f"Synced {len(STRINGS)} nutrition keys × {len(LOCALES)} locales")


if __name__ == "__main__":
    main()
