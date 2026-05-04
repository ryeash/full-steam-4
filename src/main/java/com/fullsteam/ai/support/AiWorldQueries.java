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
}
