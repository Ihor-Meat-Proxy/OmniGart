package com.omnigart.planning

/**
 * Planning entry point. Implementations must be deterministic given the same input
 * (AI stub may later wrap a network client; this PR ships rules + a no-network stub).
 */
interface PlanGenerator {
    val id: String
    fun generate(input: PlanGenerationInput): PlanRecommendation
}
