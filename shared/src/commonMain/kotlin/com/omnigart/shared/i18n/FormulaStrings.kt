package com.omnigart.shared.i18n

/** Calculation-settings copy (en-GB + uk). Merged into [StringCatalog] lookups. */
internal object FormulaStrings {
    val enGb: Map<String, String> =
        mapOf(
            "calc_settings_title" to "Calculations",
            "calc_settings_intro" to
                "Choose how OmniGart estimates BMR, heart-rate, body fat, and strength figures. " +
                "These are wellness estimates, not medical advice.",
            "calc_mode_simple" to "Simple",
            "calc_mode_advanced" to "Advanced",
            "calc_domain_bmr" to "BMR",
            "calc_domain_hr_max" to "HRmax",
            "calc_domain_body_fat" to "Body fat",
            "calc_domain_one_rm" to "1RM",
            "calc_domain_hr_zones" to "HR zones",
            "formula.bmr.mifflin_st_jeor.title" to "Mifflin–St Jeor",
            "formula.bmr.mifflin_st_jeor.explain" to
                "Widely used resting-energy estimate from height, weight, age, and sex.",
            "formula.bmr.harris_benedict.title" to "Harris–Benedict (revised)",
            "formula.bmr.harris_benedict.explain" to
                "Older resting-energy equation (Roza & Shizgal revision). Alternative to Mifflin–St Jeor.",
            "formula.hr_max.tanaka.title" to "Tanaka",
            "formula.hr_max.tanaka.explain" to "Age-predicted maximum heart rate: 208 − 0.7 × age.",
            "formula.hr_max.fox.title" to "Fox (220 − age)",
            "formula.hr_max.fox.explain" to "Traditional age-predicted maximum: 220 − age.",
            "formula.body_fat.cun_bae.title" to "CUN-BAE",
            "formula.body_fat.cun_bae.explain" to
                "Body-fat percentage estimate from BMI, age, and sex (CUN-BAE).",
            "formula.one_rm.epley.title" to "Epley",
            "formula.one_rm.epley.explain" to "Estimated 1RM from weight and reps (Epley).",
            "formula.one_rm.brzycki.title" to "Brzycki",
            "formula.one_rm.brzycki.explain" to "Estimated 1RM from weight and reps (Brzycki).",
            "formula.hr_zones.karvonen_5.title" to "Karvonen 5-zone",
            "formula.hr_zones.karvonen_5.explain" to
                "Five zones from heart-rate reserve (Karvonen).",
            "formula.hr_zones.karvonen_3.title" to "Karvonen 3-zone",
            "formula.hr_zones.karvonen_3.explain" to
                "Easy / Tempo / Hard zones from heart-rate reserve.",
            "formula.hr_zones.percent_hrmax_5.title" to "%HRmax 5-zone",
            "formula.hr_zones.percent_hrmax_5.explain" to
                "Five zones as percentages of maximum heart rate."
        )

    val uk: Map<String, String> =
        mapOf(
            "calc_settings_title" to "Розрахунки",
            "calc_settings_intro" to
                "Оберіть, як OmniGart оцінює BMR, пульс, жир у тілі та силові показники. " +
                "Це оцінки самопочуття, а не медична консультація.",
            "calc_mode_simple" to "Простий",
            "calc_mode_advanced" to "Розширений",
            "calc_domain_bmr" to "BMR",
            "calc_domain_hr_max" to "HRmax",
            "calc_domain_body_fat" to "Жир у тілі",
            "calc_domain_one_rm" to "1ПМ",
            "calc_domain_hr_zones" to "Зони пульсу",
            "formula.bmr.mifflin_st_jeor.title" to "Mifflin–St Jeor",
            "formula.bmr.mifflin_st_jeor.explain" to
                "Поширена оцінка енергії спокою за зростом, вагою, віком і статтю.",
            "formula.bmr.harris_benedict.title" to "Harris–Benedict (оновлена)",
            "formula.bmr.harris_benedict.explain" to
                "Старіша формула енергії спокою (редакція Roza & Shizgal). Альтернатива Mifflin–St Jeor.",
            "formula.hr_max.tanaka.title" to "Tanaka",
            "formula.hr_max.tanaka.explain" to "Прогноз максимального пульсу: 208 − 0,7 × вік.",
            "formula.hr_max.fox.title" to "Fox (220 − вік)",
            "formula.hr_max.fox.explain" to "Традиційний прогноз максимуму: 220 − вік.",
            "formula.body_fat.cun_bae.title" to "CUN-BAE",
            "formula.body_fat.cun_bae.explain" to
                "Оцінка відсотка жиру з ІМТ, віку та статі (CUN-BAE).",
            "formula.one_rm.epley.title" to "Epley",
            "formula.one_rm.epley.explain" to "Оцінка 1ПМ за вагою та повтореннями (Epley).",
            "formula.one_rm.brzycki.title" to "Brzycki",
            "formula.one_rm.brzycki.explain" to "Оцінка 1ПМ за вагою та повтореннями (Brzycki).",
            "formula.hr_zones.karvonen_5.title" to "Karvonen 5 зон",
            "formula.hr_zones.karvonen_5.explain" to
                "П’ять зон із резерву пульсу (Karvonen).",
            "formula.hr_zones.karvonen_3.title" to "Karvonen 3 зони",
            "formula.hr_zones.karvonen_3.explain" to
                "Зони Легко / Темп / Важко з резерву пульсу.",
            "formula.hr_zones.percent_hrmax_5.title" to "%HRmax 5 зон",
            "formula.hr_zones.percent_hrmax_5.explain" to
                "П’ять зон як відсотки максимального пульсу."
        )
}
