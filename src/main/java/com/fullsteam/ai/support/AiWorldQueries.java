package com.fullsteam.ai.support;

import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Obstacle;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class AiWorldQueries {

    private AiWorldQueries() {
    }

    public static Vector2 enemyEconomyCentroid(GameEntities entities, int myTeam) {
        List<Vector2> positions = new ArrayList<>();
        for (Building b : entities.getBuildings().values()) {
            if (!b.isActive() || b.getTeamNumber() == myTeam) {
                continue;
            }
            if (b.getBuildingType() == BuildingType.HEADQUARTERS) {
                positions.add(b.getPosition().copy());
            }
        }
        if (!positions.isEmpty()) {
            Vector2 sum = new Vector2(0, 0);
            for (Vector2 p : positions) {
                sum.add(p);
            }
            return sum.divide((double) positions.size());
        }
        for (Building b : entities.getBuildings().values()) {
            if (b.isActive() && b.getTeamNumber() != myTeam) {
                return b.getPosition().copy();
            }
        }
        return null;
    }

    /**
     * Returns the position of the nearest active enemy HQ to {@code from}, or any enemy HQ
     * if {@code from} is null. The AI is permitted to know enemy HQ locations omnisciently.
     */
    public static Vector2 nearestEnemyHQ(GameEntities entities, int myTeam, Vector2 from) {
        Optional<Building> hq = entities.getBuildings().values().stream()
                .filter(b -> b.isActive()
                        && b.getTeamNumber() != myTeam
                        && b.getBuildingType() == BuildingType.HEADQUARTERS)
                .min(from != null
                        ? Comparator.comparingDouble(b -> from.distanceSquared(b.getPosition()))
                        : Comparator.comparingInt(Building::getId));
        return hq.map(b -> b.getPosition().copy()).orElse(null);
    }

    public static Obstacle nearestHarvestable(GameEntities entities, Vector2 from) {
        if (from == null) {
            return null;
        }
        return entities.getObstacles().values().stream()
                .filter(Obstacle::isHarvestable)
                .filter(Obstacle::isActive)
                .min(Comparator.comparingDouble(o -> from.distanceSquared(o.getPosition())))
                .orElse(null);
    }

    /**
     * Returns the world-space position of the best tactical attack target for an assault,
     * ordered by priority: enemy HQ > any enemy building > {@code null}.
     *
     * <p>Used by {@link com.fullsteam.ai.tactics.AiTacticsDirector} to set assault destinations.
     *
     * @param from optional reference point for nearest-first ordering; may be {@code null}
     */
    public static Vector2 nearestAssaultTarget(GameEntities entities, int myTeam, Vector2 from) {
        // Priority 1: enemy HQ
        Vector2 hq = nearestEnemyHQ(entities, myTeam, from);
        if (hq != null) {
            return hq;
        }
        // Priority 2: any other enemy building (refineries, factories, etc.)
        Comparator<Building> byDist = from != null
                ? Comparator.comparingDouble(b -> from.distanceSquared(b.getPosition()))
                : Comparator.comparingInt(Building::getId);
        return entities.getBuildings().values().stream()
                .filter(b -> b.isActive() && b.getTeamNumber() != myTeam)
                .min(byDist)
                .map(b -> b.getPosition().copy())
                .orElse(null);
    }

    /**
     * Randomised assault target for new-wave selection.
     *
     * <p>70% of the time returns the nearest enemy HQ (the dominant win condition).
     * 30% of the time targets a random enemy production or tech building instead —
     * creating economic harassment that makes the AI feel less scripted across matches.
     * Falls through to {@link #nearestAssaultTarget} if no raid-worthy target exists.
     *
     * @param from optional reference point; may be {@code null}
     */
    public static Vector2 randomisedAssaultTarget(GameEntities entities, int myTeam, Vector2 from) {
        if (ThreadLocalRandom.current().nextDouble() < 0.30) {
            List<Building> raidTargets = new ArrayList<>();
            for (Building b : entities.getBuildings().values()) {
                if (!b.isActive() || b.getTeamNumber() == myTeam) {
                    continue;
                }
                BuildingType bt = b.getBuildingType();
                if (bt == BuildingType.REFINERY
                        || bt == BuildingType.FACTORY
                        || bt == BuildingType.BARRACKS
                        || bt == BuildingType.RESEARCH_LAB
                        || bt == BuildingType.TECH_CENTER) {
                    raidTargets.add(b);
                }
            }
            if (!raidTargets.isEmpty()) {
                Building pick = raidTargets.get(ThreadLocalRandom.current().nextInt(raidTargets.size()));
                return pick.getPosition().copy();
            }
        }
        return nearestAssaultTarget(entities, myTeam, from);
    }
}
