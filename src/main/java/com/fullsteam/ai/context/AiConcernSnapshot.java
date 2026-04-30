package com.fullsteam.ai.context;

/**
 * Normalized pressures derived from {@link SkirmishAiTickContext} for behavior arbitration.
 */
public record AiConcernSnapshot(double defense, double economy, double power, double armyExpansion) {

    public static AiConcernSnapshot from(SkirmishAiTickContext ctx) {
        var p = ctx.aiProfile();
        double defense = ctx.enemiesNearBase() <= 0 ? 0.0
                : Math.min(1.0, ctx.enemiesNearBase() * p.defenseConcernPerEnemy());

        double economy = ctx.credits() < p.creditsLow() ? 0.85
                : ctx.credits() < p.creditsMid() ? 0.45 : 0.12;
        if (ctx.idleHarvesters() > 0 && ctx.credits() < p.creditsHarvesterBoostBelow()) {
            economy = Math.max(economy, p.economyHarvesterBoostMinConcern());
        }

        double power = ctx.lowPower() ? 1.0 : 0.0;
        if (!ctx.lowPower() && ctx.powerConsumed() > ctx.powerGenerated()) {
            power = Math.max(power, 0.45);
        }

        double armyExpansion = ctx.idleCombatUnits() < p.armyIdleLowCount() ? 0.55
                : ctx.idleCombatUnits() < p.armyIdleMidCount() ? 0.28 : 0.12;

        return new AiConcernSnapshot(defense, economy, power, armyExpansion);
    }
}
