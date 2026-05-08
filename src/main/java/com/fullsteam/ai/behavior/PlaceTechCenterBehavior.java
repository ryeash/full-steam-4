package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.model.BuildingType;

/**
 * Builds a {@link BuildingType#TECH_CENTER} once the Research Lab is complete — the final step of the
 * T3 tech path.  The AI defers placement until it has at least 50 surplus power (the Tech Center's drain)
 * so it does not immediately stall production when the building completes.  If power is insufficient,
 * {@link PlacePowerPlantBehavior} (higher priority in the building channel) will fire first and provide
 * the required headroom.
 */
public final class PlaceTechCenterBehavior extends AbstractPlaceBuildingBehavior {

    /** Power drained by a completed Tech Center — must be available as surplus before placement. */
    private static final int TECH_CENTER_POWER_DRAIN = 50;

    @Override
    protected BuildingType buildingType() {
        return BuildingType.TECH_CENTER;
    }

    @Override
    protected int tickPeriod(SkirmishAiTickContext ctx) {
        return ctx.aiProfile().placeRefineryPeriod();
    }

    @Override
    protected int phaseSalt(SkirmishAiTickContext ctx) {
        // Unique prime offset so this behavior ticks on a different frame than ResearchLab (+2) / Factory (+19)
        return ctx.aiProfile().placeRefinerySalt() + 31;
    }

    @Override
    protected boolean concernsAllow(SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        return !ctx.hasCompletedTechCenter()
                || concerns.economy() >= ctx.aiProfile().placeRefineryMinEconomyConcern();
    }

    @Override
    protected boolean extraPlacementGuards(SkirmishAiTickContext ctx) {
        if (ctx.hasCompletedTechCenter() || ctx.techCentersUnderConstruction() > 0) {
            return false;
        }
        if (!ctx.hasCompletedResearchLab()) {
            return false;
        }
        // Only proceed when there is enough surplus power to absorb the Tech Center's drain without
        // immediately causing a low-power condition.  PlacePowerPlantBehavior (higher priority) will
        // build another plant first if the AI is already running close to its limit.
        int surplus = ctx.powerGenerated() - ctx.powerConsumed();
        return surplus >= TECH_CENTER_POWER_DRAIN;
    }
}
