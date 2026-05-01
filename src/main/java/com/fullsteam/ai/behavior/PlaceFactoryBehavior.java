package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.model.BuildingType;

/**
 * Builds a {@link BuildingType#FACTORY} after the research lab completes so vehicle production can begin.
 */
public final class PlaceFactoryBehavior extends AbstractPlaceBuildingBehavior {

    @Override
    protected BuildingType buildingType() {
        return BuildingType.FACTORY;
    }

    @Override
    protected int tickPeriod(SkirmishAiTickContext ctx) {
        return ctx.aiProfile().placeRefineryPeriod();
    }

    @Override
    protected int phaseSalt(SkirmishAiTickContext ctx) {
        return ctx.aiProfile().placeRefinerySalt() + 19;
    }

    @Override
    protected boolean concernsAllow(SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        return !ctx.hasCompletedFactory()
                || concerns.economy() >= ctx.aiProfile().placeRefineryMinEconomyConcern();
    }

    @Override
    protected boolean extraPlacementGuards(SkirmishAiTickContext ctx) {
        return ctx.hasCompletedResearchLab()
                && !ctx.hasCompletedFactory()
                && ctx.factoriesUnderConstruction() == 0;
    }
}
