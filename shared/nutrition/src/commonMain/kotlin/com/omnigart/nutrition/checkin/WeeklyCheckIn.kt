package com.omnigart.nutrition.checkin

/**
 * Weekly check-in on a user-chosen weekday with skippable sections.
 * Hosts show a local notification; sections can be dismissed individually.
 */
object WeeklyCheckIn {

    enum class Section {
        INCOMPLETE_DAYS,
        WEIGH_IN_PROMPT,
        FASTING_VS_UNLOGGED,
        PROPOSED_TARGET_CHANGE,
    }

    data class ProposedTargetChange(
        val previousKcal: Double,
        val proposedKcal: Double,
        val reasonKey: String,
    )

    data class CheckInState(
        val weekday: Int,
        val sections: List<Section>,
        val incompleteDayKeys: List<String>,
        val proposedChange: ProposedTargetChange?,
        val skipped: Set<Section> = emptySet(),
    )

    data class CheckInDecision(
        val acceptedTargetKcal: Double?,
        val remainingIncomplete: List<String>,
        val markedFasting: List<String>,
        val dismissed: Set<Section>,
    )

    fun build(
        weekday: Int,
        incompleteDayKeys: List<String>,
        needsWeighIn: Boolean,
        ambiguousUnloggedDayKeys: List<String>,
        proposedChange: ProposedTargetChange?,
    ): CheckInState {
        val sections = buildList {
            if (incompleteDayKeys.isNotEmpty()) add(Section.INCOMPLETE_DAYS)
            if (needsWeighIn) add(Section.WEIGH_IN_PROMPT)
            if (ambiguousUnloggedDayKeys.isNotEmpty()) add(Section.FASTING_VS_UNLOGGED)
            if (proposedChange != null &&
                kotlin.math.abs(proposedChange.proposedKcal - proposedChange.previousKcal) >= 1.0
            ) {
                add(Section.PROPOSED_TARGET_CHANGE)
            }
        }
        return CheckInState(
            weekday = weekday.coerceIn(0, 6),
            sections = sections,
            incompleteDayKeys = incompleteDayKeys,
            proposedChange = proposedChange,
        )
    }

    fun isCheckInDay(todayWeekday: Int, configuredWeekday: Int): Boolean =
        todayWeekday.coerceIn(0, 6) == configuredWeekday.coerceIn(0, 6)

    fun applyUserChoices(
        state: CheckInState,
        acceptProposedTarget: Boolean?,
        markFastingDayKeys: List<String>,
        skipSections: Set<Section>,
    ): CheckInDecision {
        val accepted =
            when {
                Section.PROPOSED_TARGET_CHANGE in skipSections -> null
                acceptProposedTarget == true -> state.proposedChange?.proposedKcal
                else -> null
            }
        val remaining =
            if (Section.INCOMPLETE_DAYS in skipSections) {
                emptyList()
            } else {
                state.incompleteDayKeys
            }
        return CheckInDecision(
            acceptedTargetKcal = accepted,
            remainingIncomplete = remaining,
            markedFasting = markFastingDayKeys,
            dismissed = skipSections,
        )
    }
}
