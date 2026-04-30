package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.model.BuildingType;

public final class PlacePowerPlantBehavior extends AbstractPlaceBuildingBehavior {

    @Override
    protected BuildingType buildingType() {
        return BuildingType.POWER_PLANT;
    }

    @Override
    protected int tickPeriod(SkirmishAiTickContext ctx) {
        return ctx.aiProfile().placePowerPlantPeriod();
    }

    @Override
    protected int phaseSalt(SkirmishAiTickContext ctx) {
        return ctx.aiProfile().placePowerPlantSalt();
    }

    @Override
    protected boolean concernsAllow(SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        return concerns.power() >= ctx.aiProfile().placePowerMinConcern() || ctx.lowPower();
    }

    @Override
    protected boolean extraPlacementGuards(SkirmishAiTickContext ctx) {
        return ctx.powerPlantsUnderConstruction() == 0;
    }
}
