package com.fullsteam.ai.support;

import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Obstacle;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
