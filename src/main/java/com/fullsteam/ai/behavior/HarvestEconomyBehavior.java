package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.ai.support.AiWorldQueries;
import com.fullsteam.model.Obstacle;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.RTSPlayerInput;
import com.fullsteam.model.Unit;
import com.fullsteam.model.command.IdleCommand;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class HarvestEconomyBehavior implements SkirmishAiBehavior {

    @Override
    public boolean isReadyThisFrame(SkirmishAiTickContext ctx) {
        return AiTickPhasing.every(ctx.frameCount(), ctx.playerId(), ctx.aiProfile().harvestPeriod(), ctx.aiProfile().harvestSalt());
    }

    @Override
    public boolean isApplicable(GameEntities entities, SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        // Always assign harvest when workers are idle. Economy concern is ~0.12 when credits are above
        // creditsMid, which is below harvestMinEconomyConcern and used to block this behavior entirely.
        return ctx.idleHarvesters() > 0;
    }

    @Override
    public Optional<RTSPlayerInput> propose(GameEntities entities, SkirmishAiTickContext ctx) {
        List<Unit> idleHarvesters = new ArrayList<>();
        for (Unit u : entities.getUnits().values()) {
            if (!u.belongsTo(ctx.playerId()) || !u.isActive() || u.isGarrisoned() || !u.getUnitType().canHarvest()) {
                continue;
            }
            if (!(u.getCurrentCommand() == null || u.getCurrentCommand() instanceof IdleCommand)) {
                continue;
            }
            idleHarvesters.add(u);
        }
        if (idleHarvesters.isEmpty()) {
            return Optional.empty();
        }
        Vector2 anchor = ctx.baseAnchor();
        int configured = ctx.aiProfile().reservedBuildWorkers();
        if (anchor != null && configured > 0 && idleHarvesters.size() >= 2) {
            int holdNearBase = Math.min(configured, idleHarvesters.size() - 1);
            idleHarvesters.sort(Comparator.comparingDouble(u -> u.getPosition().distanceSquared(anchor)));
            idleHarvesters = new ArrayList<>(idleHarvesters.subList(holdNearBase, idleHarvesters.size()));
        }
        if (idleHarvesters.isEmpty()) {
            return Optional.empty();
        }
        Vector2 pivot = centroid(idleHarvesters);
        Obstacle best = AiWorldQueries.nearestHarvestable(entities, pivot);
        if (best == null) {
            return Optional.empty();
        }
        RTSPlayerInput in = new RTSPlayerInput();
        in.setSelectUnits(idleHarvesters.stream().map(Unit::getId).toList());
        in.setHarvestOrder(best.getId());
        return Optional.of(in);
    }

    private static Vector2 centroid(List<Unit> units) {
        Vector2 sum = new Vector2(0, 0);
        for (Unit u : units) {
            sum.add(u.getPosition().copy());
        }
        return sum.divide((double) units.size());
    }
}
