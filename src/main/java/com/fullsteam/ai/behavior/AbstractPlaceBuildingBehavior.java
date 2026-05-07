package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.ai.support.AiUnitSupport;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.RTSPlayerInput;
import com.fullsteam.model.ResourceType;
import org.dyn4j.geometry.Vector2;

import java.util.List;
import java.util.Optional;

/**
 * Issues {@link RTSPlayerInput} with {@link RTSPlayerInput#setBuildOrder} / {@link RTSPlayerInput#setBuildLocation}
 * after choosing an idle builder and a collision-valid site — same path humans use.
 */
public abstract class AbstractPlaceBuildingBehavior implements SkirmishAiBehavior {

    protected abstract BuildingType buildingType();

    protected abstract int tickPeriod(SkirmishAiTickContext ctx);

    protected abstract int phaseSalt(SkirmishAiTickContext ctx);

    protected abstract boolean concernsAllow(SkirmishAiTickContext ctx, AiConcernSnapshot concerns);

    protected boolean extraPlacementGuards(SkirmishAiTickContext ctx) {
        return true;
    }

    @Override
    public boolean isReadyThisFrame(SkirmishAiTickContext ctx) {
        return AiTickPhasing.every(ctx.frameCount(), ctx.playerId(), tickPeriod(ctx), phaseSalt(ctx));
    }

    @Override
    public boolean isApplicable(GameEntities entities, SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        if (ctx.baseAnchor() == null || !concernsAllow(ctx, concerns) || !extraPlacementGuards(ctx)) {
            return false;
        }
        BuildingType bt = buildingType();
        var faction = ctx.faction();
        if (!faction.canBuildBuilding(bt) || !faction.hasResources(ResourceType.CREDITS, faction.getBuildingCost(bt))) {
            return false;
        }
        if (!entities.getMissingTechForConstruction(ctx.playerId(), bt).isEmpty()) {
            return false;
        }
        if (bt.isUniquePerPlayer() && entities.playerHasActiveBuilding(ctx.playerId(), bt)) {
            return false;
        }
        return AiUnitSupport.pickBuildWorker(entities, ctx.playerId(), ctx.baseAnchor()).isPresent();
    }

    @Override
    public Optional<RTSPlayerInput> propose(GameEntities entities, SkirmishAiTickContext ctx) {
        var worker = AiUnitSupport.pickBuildWorker(entities, ctx.playerId(), ctx.baseAnchor());
        if (worker.isEmpty()) {
            return Optional.empty();
        }
        Optional<Vector2> loc = entities.suggestBuildLocationNear(buildingType(), ctx.baseAnchor());
        if (loc.isEmpty()) {
            return Optional.empty();
        }
        RTSPlayerInput in = new RTSPlayerInput();
        in.setAction(com.fullsteam.model.InputAction.BUILD);
        in.setUnitIds(List.of(worker.get().getId()));
        in.setBuildingType(buildingType());
        in.setTargetPosition(loc.get());
        return Optional.of(in);
    }
}
