package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.ai.support.AiUnitSupport;
import com.fullsteam.model.Building;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.RTSPlayerInput;
import com.fullsteam.model.Unit;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Sends idle workers to unfinished foundations via {@link RTSPlayerInput#setConstructOrder}.
 */
public final class ResumeConstructionBehavior implements SkirmishAiBehavior {

    @Override
    public boolean isReadyThisFrame(SkirmishAiTickContext ctx) {
        return AiTickPhasing.every(ctx.frameCount(), ctx.playerId(), ctx.aiProfile().resumeConstructionPeriod(), ctx.aiProfile().resumeConstructionSalt());
    }

    @Override
    public boolean isApplicable(GameEntities entities, SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        return entities.getBuildings().values().stream()
                .anyMatch(b -> b.belongsTo(ctx.playerId()) && b.isUnderConstruction());
    }

    @Override
    public Optional<RTSPlayerInput> propose(GameEntities entities, SkirmishAiTickContext ctx) {
        if (ctx.baseAnchor() == null) {
            return Optional.empty();
        }
        Building site = entities.getBuildings().values().stream()
                .filter(b -> b.belongsTo(ctx.playerId()) && b.isUnderConstruction())
                .min(Comparator.comparingDouble(b -> b.getPosition().distanceSquared(ctx.baseAnchor())))
                .orElse(null);
        if (site == null) {
            return Optional.empty();
        }
        Optional<Unit> worker = AiUnitSupport.pickIdleConstructWorker(entities, ctx.playerId(), site.getPosition());
        if (worker.isEmpty()) {
            return Optional.empty();
        }
        RTSPlayerInput in = new RTSPlayerInput();
        in.setAction(com.fullsteam.model.InputAction.CONSTRUCT);
        in.setUnitIds(List.of(worker.get().getId()));
        in.setTargetEntityId(site.getId());
        return Optional.of(in);
    }
}
