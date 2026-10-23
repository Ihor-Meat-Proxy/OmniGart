package com.omnigart.shared.calc.strategy

/** UI mode for Calculations settings. */
enum class FormulaMode {
    /** Curated defaults only; advanced alternatives hidden. */
    SIMPLE,

    /** Full strategy catalog with short explanations. */
    ADVANCED
}

enum class FormulaDomain {
    BMR,
    HR_MAX,
    BODY_FAT,
    ONE_RM,
    HR_ZONES
}

/**
 * Persisted formula keys for a profile.
 * Unknown / retired keys are migrated to [FormulaCatalog] defaults via [normalize].
 */
data class FormulaSelection(
    val mode: FormulaMode = FormulaMode.SIMPLE,
    val bmrKey: String = FormulaCatalog.DEFAULT_BMR,
    val hrMaxKey: String = FormulaCatalog.DEFAULT_HR_MAX,
    val bodyFatKey: String = FormulaCatalog.DEFAULT_BODY_FAT,
    val oneRmKey: String = FormulaCatalog.DEFAULT_ONE_RM,
    val hrZonesKey: String = FormulaCatalog.DEFAULT_HR_ZONES
) {
    fun keyFor(domain: FormulaDomain): String = when (domain) {
        FormulaDomain.BMR -> bmrKey
        FormulaDomain.HR_MAX -> hrMaxKey
        FormulaDomain.BODY_FAT -> bodyFatKey
        FormulaDomain.ONE_RM -> oneRmKey
        FormulaDomain.HR_ZONES -> hrZonesKey
    }

    fun withKey(domain: FormulaDomain, key: String): FormulaSelection = when (domain) {
        FormulaDomain.BMR -> copy(bmrKey = key)
        FormulaDomain.HR_MAX -> copy(hrMaxKey = key)
        FormulaDomain.BODY_FAT -> copy(bodyFatKey = key)
        FormulaDomain.ONE_RM -> copy(oneRmKey = key)
        FormulaDomain.HR_ZONES -> copy(hrZonesKey = key)
    }

    fun toPreferences(): FormulaPreferences = FormulaCatalog.preferencesFromKeys(
        bmrKey = bmrKey,
        hrMaxKey = hrMaxKey,
        bodyFatKey = bodyFatKey,
        oneRepMaxKey = oneRmKey,
        hrZonesKey = hrZonesKey
    )

    fun normalize(): FormulaSelection = FormulaSelection(
        mode = mode,
        bmrKey = FormulaCatalog.resolveBmrKey(bmrKey),
        hrMaxKey = FormulaCatalog.resolveHrMaxKey(hrMaxKey),
        bodyFatKey = FormulaCatalog.resolveBodyFatKey(bodyFatKey),
        oneRmKey = FormulaCatalog.resolveOneRmKey(oneRmKey),
        hrZonesKey = FormulaCatalog.resolveHrZonesKey(hrZonesKey)
    )
}

data class FormulaOptionMeta(
    val key: String,
    val titleKey: String,
    val explanationKey: String,
    /** Shown in SIMPLE mode (curated defaults). */
    val simpleDefault: Boolean
)

data class RecalcAuditEntry(
    val id: String,
    val profileId: String,
    val domain: FormulaDomain,
    val oldFormulaKey: String,
    val newFormulaKey: String,
    val rowsTouched: Int,
    val atIso: String
)
