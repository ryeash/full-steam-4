package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.ai.support.AiWorldQueries;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.RTSPlayerInput;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.command.IdleCommand;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Coordination layer: attack-move idle combat units toward enemy economy using {@link RTSPlayerInput#setAttackMoveOrder}.
 */
public final class SquadAttackMoveBehavior implements SkirmishAiBehavior {

    @Override
    public boolean isReadyThisFrame(SkirmishAiTickContext ctx) {
        return AiTickPhasing.every(ctx.frameCount(), ctx.playerId(), ctx.aiProfile().squadAttackPeriod(), ctx.aiProfile().squadAttackSalt());
    }

    @Override
    public boolean isApplicable(GameEntities entities, SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        var p = ctx.aiProfile();
        return concerns.defense() < p.squadMaxDefenseForAttack()
                && concerns.armyExpansion() >= p.squadMinArmyConcern()
                && AiWorldQueries.enemyEconomyCentroid(entities, ctx.faction().getTeamNumber()) != null;
    }

    @Override
    public Optional<RTSPlayerInput> propose(GameEntities entities, SkirmishAiTickContext ctx) {
        Vector2 strike = AiWorldQueries.enemyEconomyCentroid(entities, ctx.faction().getTeamNumber());
        if (strike == null) {
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
        if (attackers.size() < ctx.aiProfile().squadMinCombatUnits()) {
            return Optional.empty();
        }
        RTSPlayerInput in = new RTSPlayerInput();
        in.setSelectUnits(attackers);
        in.setAttackMoveOrder(strike.copy());
        return Optional.of(in);
    }
}
