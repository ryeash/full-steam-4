package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.RTSPlayerInput;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.command.IdleCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Focus-fire nearest threat near base using {@link RTSPlayerInput#setAttackUnitOrder}.
 */
public final class DefendBaseBehavior implements SkirmishAiBehavior {

    @Override
    public boolean isReadyThisFrame(SkirmishAiTickContext ctx) {
        return AiTickPhasing.every(ctx.frameCount(), ctx.playerId(), ctx.aiProfile().defendPeriod(), ctx.aiProfile().defendPhaseSalt());
    }

    @Override
    public boolean isApplicable(GameEntities entities, SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        return concerns.defense() >= ctx.aiProfile().defendMinConcern() && ctx.nearestEnemyNearBase().isPresent();
    }

    @Override
    public Optional<RTSPlayerInput> propose(GameEntities entities, SkirmishAiTickContext ctx) {
        Unit target = ctx.nearestEnemyNearBase().orElse(null);
        if (target == null || !target.isActive()) {
            return Optional.empty();
        }
        List<Integer> attackers = new ArrayList<>();
        for (Unit u : entities.getUnits().values()) {
            if (!u.belongsTo(ctx.playerId()) || !u.isActive() || u.isGarrisoned()) {
                continue;
            }
            if (!u.getUnitType().canAttack()
                    || u.getUnitType() == UnitType.WORKER
                    || u.getUnitType().isSortieBased()) {
                continue;
            }
            if (!(u.getCurrentCommand() instanceof IdleCommand)) {
                continue;
            }
            attackers.add(u.getId());
        }
        if (attackers.isEmpty()) {
            return Optional.empty();
        }
        RTSPlayerInput in = new RTSPlayerInput();
        in.setSelectUnits(attackers);
        in.setAttackUnitOrder(target.getId());
        return Optional.of(in);
    }
}
