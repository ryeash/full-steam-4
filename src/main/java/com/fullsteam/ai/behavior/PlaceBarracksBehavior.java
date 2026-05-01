package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.model.BuildingType;

/**
 * Builds the first {@link BuildingType#BARRACKS} — starting bases only include HQ + workers.
 */
public final class PlaceBarracksBehavior extends AbstractPlaceBuildingBehavior {

    @Override
    protected BuildingType buildingType() {
        return BuildingType.BARRACKS;
    }

    @Override
    protected int tickPeriod(SkirmishAiTickContext ctx) {
        return ctx.aiProfile().placeRefineryPeriod();
    }

    @Override
    protected int phaseSalt(SkirmishAiTickContext ctx) {
        return ctx.aiProfile().placeRefinerySalt() + 11;
    }

    @Override
    protected boolean concernsAllow(SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        return !ctx.hasCompletedBarracks()
                || concerns.economy() >= ctx.aiProfile().placeRefineryMinEconomyConcern();
    }

    @Override
    protected boolean extraPlacementGuards(SkirmishAiTickContext ctx) {
        return !ctx.hasCompletedBarracks() && ctx.barracksUnderConstruction() == 0;
    }
}
