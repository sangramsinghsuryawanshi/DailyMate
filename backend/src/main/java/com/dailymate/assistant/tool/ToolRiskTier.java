package com.dailymate.assistant.tool;

public enum ToolRiskTier {
    TIER_1, // Read / Query / Search / Analytics — Direct execution, zero mutation
    TIER_2, // Low-Risk Reversible Mutation — Direct execution with structured audit trail
    TIER_3  // High-Impact / Financial / Health / Public Mutation — Strict persistent confirmation required
}
