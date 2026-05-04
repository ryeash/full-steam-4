package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.ai.support.AiWorldQueries;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.InputAction;
import com.fullsteam.model.RTSPlayerInput;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.command.IdleCommand;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Accumulates idle combat units and launches a coordinated strike at the nearest
 * enemy Headquarters once the force meets the minimum threshold from
 * {@link com.fullsteam.ai.SkirmishAiProfile#assaultMinForce()}.
 *
 * <p>The AI is permitted to know enemy HQ positions directly (omniscient target
 * selection) so the attack does not stall waiting for scouting.
 *
 * <p>The assault is suppressed while the base is under serious threat so that
 * {@link DefendBaseBehavior} can handle the immediate emergency first.
 */
public final class LaunchAssaultBehavior implements SkirmishAiBehavior {

    @Override
    public boolean isReadyThisFrame(SkirmishAiTickContext ctx) {
        return AiTickPhasing.every(
                ctx.frameCount(), ctx.playerId(),
                ctx.aiProfile().assaultPeriod(), ctx.aiProfile().assaultSalt());
    }

    @Override
    public boolean isApplicable(GameEntities entities, SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        // Defer while defending — DefendBaseBehavior takes priority
        if (concerns.defense() >= ctx.aiProfile().defendMinConcern()) {
            return false;
        }
        return AiWorldQueries.nearestEnemyHQ(entities, ctx.faction().getTeamNumber(), ctx.baseAnchor()) != null;
    }

    @Override
    public Optional<RTSPlayerInput> propose(GameEntities entities, SkirmishAiTickContext ctx) {
        Vector2 target = AiWorldQueries.nearestEnemyHQ(
                entities, ctx.faction().getTeamNumber(), ctx.baseAnchor());
        if (target == null) {
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

        if (attackers.size() < ctx.aiProfile().assaultMinForce()) {
            return Optional.empty();
        }

        RTSPlayerInput in = new RTSPlayerInput();
        in.setAction(InputAction.ATTACK_MOVE);
        in.setUnitIds(attackers);
        in.setTargetPosition(target);
        return Optional.of(in);
    }
}
