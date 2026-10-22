package com.omnigart.planning

/**
 * Stub for a later release. Intentionally performs **no network I/O**.
 *
 * Privacy / routing requirements (see docs/PLANNING_ENGINE.md):
 * - Explicit opt-in + consent before any call
 * - Minimised/pseudonymised payload
 * - User-supplied or app-managed API key (never logged)
 * - `provider.data_collection = "deny"`, prefer `provider.zdr = true`
 * - EU prompt/completion residency via `https://eu.openrouter.ai` when required (Business/Enterprise)
 * - Validate AI JSON with [PlanSafetyValidator]; fall back to [RuleBasedPlanGenerator]
 */
class OpenRouterPlanGenerator(
    private val fallback: PlanGenerator = RuleBasedPlanGenerator(),
    private val safety: PlanSafetyValidator = PlanSafetyValidator(),
    private val networkEnabled: Boolean = false,
) : PlanGenerator {
    override val id: String = "openrouter-stub-v1"

    /**
     * @throws UnsupportedOperationException if [networkEnabled] is true but no client is wired
     * (this PR never wires a client). Otherwise returns the rule-based plan.
     */
    override fun generate(input: PlanGenerationInput): PlanRecommendation {
        if (networkEnabled) {
            throw UnsupportedOperationException(
                "OpenRouter client is not implemented in this release; keep networkEnabled=false.",
            )
        }
        // Demonstrate the validate/clamp path using the rule output as a stand-in proposal.
        val rules = fallback.generate(input)
        val proposal = AiPlanProposal(
            sessions = rules.sessions,
            claimedVolume = rules.volumeTargets,
        )
        val (_, clampNotes) = safety.validateAndClamp(proposal, input.profile)
        return rules.copy(
            generatorId = id,
            explanations = rules.explanations + clampNotes + PlanExplanation(
                code = "ai.disabled",
                message = "AI planning disabled; served deterministic rule-based plan.",
                evidence = EvidenceCatalog.ACSM_RT_2026,
            ),
        )
    }

    companion object {
        /** Prompt sketch for a future client — not sent anywhere in this PR. */
        const val SYSTEM_PROMPT_SKETCH: String =
            "You are OmniGart's planning assistant. Return one week of training as JSON matching " +
                "AiPlanProposal. Use only provided exercise ids. Respect injuries and equipment. " +
                "Annotate sessions with rule codes from the catalogue. Do not give medical diagnoses."

        val DEFAULT_PROVIDER_HINTS: Map<String, String> = mapOf(
            "data_collection" to "deny",
            "zdr" to "true",
            "eu_base_url" to "https://eu.openrouter.ai",
        )
    }
}
