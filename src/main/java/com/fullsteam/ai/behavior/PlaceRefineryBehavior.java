package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.model.BuildingType;

public final class PlaceRefineryBehavior extends AbstractPlaceBuildingBehavior {

    @Override
    protected BuildingType buildingType() {
        return BuildingType.REFINERY;
    }

    @Override
    protected int tickPeriod(SkirmishAiTickContext ctx) {
        return ctx.aiProfile().placeRefineryPeriod();
    }

    @Override
    protected int phaseSalt(SkirmishAiTickContext ctx) {
        return ctx.aiProfile().placeRefinerySalt();
    }

    @Override
    protected boolean concernsAllow(SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        return !ctx.hasCompletedRefinery()
                || concerns.economy() >= ctx.aiProfile().placeRefineryMinEconomyConcern();
    }

    @Override
    protected boolean extraPlacementGuards(SkirmishAiTickContext ctx) {
        return !ctx.hasCompletedRefinery() && ctx.refineriesUnderConstruction() == 0;
    }
}
