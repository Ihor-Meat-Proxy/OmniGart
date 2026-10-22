package com.omnigart.evidence

/**
 * Plain-language GRADE-like labels for the ⓘ evidence sheet.
 */
object GradeLabels {
    fun label(level: EvidenceGradeLevel, locale: EvidenceLocale = EvidenceLocale.EN_GB): String =
        when (locale) {
            EvidenceLocale.EN_GB -> when (level) {
                EvidenceGradeLevel.HIGH -> "High confidence"
                EvidenceGradeLevel.MODERATE -> "Moderate confidence"
                EvidenceGradeLevel.LOW -> "Low confidence"
                EvidenceGradeLevel.VERY_LOW -> "Very low confidence"
            }
            EvidenceLocale.UK -> when (level) {
                EvidenceGradeLevel.HIGH -> "Висока впевненість"
                EvidenceGradeLevel.MODERATE -> "Помірна впевненість"
                EvidenceGradeLevel.LOW -> "Низька впевненість"
                EvidenceGradeLevel.VERY_LOW -> "Дуже низька впевненість"
            }
        }

    fun shortBlurb(level: EvidenceGradeLevel, locale: EvidenceLocale = EvidenceLocale.EN_GB): String =
        when (locale) {
            EvidenceLocale.EN_GB -> when (level) {
                EvidenceGradeLevel.HIGH ->
                    "Further research is unlikely to change our confidence in this finding."
                EvidenceGradeLevel.MODERATE ->
                    "Further research may change our confidence or refine the estimate."
                EvidenceGradeLevel.LOW ->
                    "Further research is likely to change our confidence in this finding."
                EvidenceGradeLevel.VERY_LOW ->
                    "Any estimate of effect is very uncertain."
            }
            EvidenceLocale.UK -> when (level) {
                EvidenceGradeLevel.HIGH ->
                    "Подальші дослідження навряд чи змінять нашу впевненість у цьому висновку."
                EvidenceGradeLevel.MODERATE ->
                    "Подальші дослідження можуть змінити впевненість або уточнити оцінку."
                EvidenceGradeLevel.LOW ->
                    "Подальші дослідження, ймовірно, змінять нашу впевненість у цьому висновку."
                EvidenceGradeLevel.VERY_LOW ->
                    "Будь-яка оцінка ефекту є дуже непевною."
            }
        }
}

enum class EvidenceLocale {
    EN_GB,
    UK,
}
