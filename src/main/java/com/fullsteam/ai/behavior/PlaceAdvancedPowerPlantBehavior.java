package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.model.BuildingType;

/**
 * Builds an {@link BuildingType#ADVANCED_POWER_PLANT} once the AI has invested in T2 (Research Lab)
 * and is starting to feel power pressure from its expanding base.
 *
 * <p>Placement is gated behind a completed Research Lab so the AI doesn't rush an expensive upgrade
 * before it has a viable economy. Only one advanced plant is built at a time.
 */
public final class PlaceAdvancedPowerPlantBehavior extends AbstractPlaceBuildingBehavior {

    @Override
    protected BuildingType buildingType() {
        return BuildingType.ADVANCED_POWER_PLANT;
    }

    @Override
    protected int tickPeriod(SkirmishAiTickContext ctx) {
        return ctx.aiProfile().placePowerPlantPeriod();
    }

    @Override
    protected int phaseSalt(SkirmishAiTickContext ctx) {
        // Offset from the standard power-plant check so they don't fire on the same frame
        return ctx.aiProfile().placePowerPlantSalt() + 5;
    }

    @Override
    protected boolean concernsAllow(SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        // Only upgrade once T2 is established — the Research Lab signals an active tech economy
        if (!ctx.hasCompletedResearchLab()) {
            return false;
        }
        // Build when power concern is present, power is actually low, or consumption is high
        return concerns.power() >= ctx.aiProfile().placePowerMinConcern()
                || ctx.lowPower()
                || ctx.powerConsumed() >= 150;
    }

    @Override
    protected boolean extraPlacementGuards(SkirmishAiTickContext ctx) {
        return ctx.advancedPowerPlantsUnderConstruction() == 0;
    }
}
