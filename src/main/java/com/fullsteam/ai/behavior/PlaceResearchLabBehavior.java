package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.model.BuildingType;

/**
 * Builds a {@link BuildingType#RESEARCH_LAB} after core economy (refinery) is up — first step of the T2 tech path.
 */
public final class PlaceResearchLabBehavior extends AbstractPlaceBuildingBehavior {

    @Override
    protected BuildingType buildingType() {
        return BuildingType.RESEARCH_LAB;
    }

    @Override
    protected int tickPeriod(SkirmishAiTickContext ctx) {
        return ctx.aiProfile().placeRefineryPeriod();
    }

    @Override
    protected int phaseSalt(SkirmishAiTickContext ctx) {
        return ctx.aiProfile().placeRefinerySalt() + 2;
    }

    @Override
    protected boolean concernsAllow(SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        return concerns.economy() >= ctx.aiProfile().placeRefineryMinEconomyConcern();
    }

    @Override
    protected boolean extraPlacementGuards(SkirmishAiTickContext ctx) {
        return ctx.hasCompletedRefinery()
                && !ctx.hasCompletedResearchLab()
                && ctx.researchLabsUnderConstruction() == 0;
    }
}
