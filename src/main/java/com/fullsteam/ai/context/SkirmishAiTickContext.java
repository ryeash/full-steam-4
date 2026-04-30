package com.fullsteam.ai.context;

import com.fullsteam.ai.SkirmishAiProfile;
import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Player;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.command.IdleCommand;
import org.dyn4j.geometry.Vector2;

import java.util.Optional;
import java.util.Set;

/**
 * Read-only snapshot of one AI player's situation for a single {@link com.fullsteam.ai.SkirmishAiDirector} tick.
 */
public record SkirmishAiTickContext(
        int playerId,
        int frameCount,
        Player faction,
        Vector2 baseAnchor,
        Set<BuildingType> completedBuildingTypes,
        int credits,
        boolean lowPower,
        int powerGenerated,
        int powerConsumed,
        int enemiesNearBase,
        Optional<Unit> nearestEnemyNearBase,
        int idleHarvesters,
        int idleCombatUnits,
        int refineriesUnderConstruction,
        int powerPlantsUnderConstruction,
        int researchLabsUnderConstruction,
        int factoriesUnderConstruction,
        SkirmishAiProfile aiProfile
) {
    public static SkirmishAiTickContext build(GameEntities entities, int playerId, int frameCount) {
        Player faction = entities.getPlayerFactions().get(playerId);
        if (faction == null) {
            throw new IllegalArgumentException("Unknown player " + playerId);
        }
        int team = faction.getTeamNumber();
        SkirmishAiProfile profile = SkirmishAiProfile.forDifficulty(faction.getSkirmishAiDifficulty());
        Vector2 anchor = resolveBaseAnchor(entities, playerId);

        int enemies = 0;
        Unit nearest = null;
        double nearestDist = Double.MAX_VALUE;
        double defenseR = profile.defenseRadius();
        if (anchor != null) {
            for (Unit u : entities.getUnits().values()) {
                if (!u.isActive() || u.getTeamNumber() == team) {
                    continue;
                }
                double d = u.getPosition().distanceSquared(anchor);
                if (d <= defenseR * defenseR) {
                    enemies++;
                    if (d < nearestDist) {
                        nearestDist = d;
                        nearest = u;
                    }
                }
            }
        }

        int harvesters = 0;
        int combatIdle = 0;
        int refineriesUc = 0;
        int plantsUc = 0;
        int researchLabsUc = 0;
        int factoriesUc = 0;

        for (Building b : entities.getBuildings().values()) {
            if (!b.belongsTo(playerId)) {
                continue;
            }
            if (b.getBuildingType() == BuildingType.REFINERY && b.isUnderConstruction()) {
                refineriesUc++;
            }
            if (b.getBuildingType() == BuildingType.POWER_PLANT && b.isUnderConstruction()) {
                plantsUc++;
            }
            if (b.getBuildingType() == BuildingType.RESEARCH_LAB && b.isUnderConstruction()) {
                researchLabsUc++;
            }
            if (b.getBuildingType() == BuildingType.FACTORY && b.isUnderConstruction()) {
                factoriesUc++;
            }
        }

        for (Unit u : entities.getUnits().values()) {
            if (!u.belongsTo(playerId) || !u.isActive() || u.isGarrisoned()) {
                continue;
            }
            if (u.getUnitType().canHarvest()) {
                if (isIdleWorker(u)) {
                    harvesters++;
                }
                continue;
            }
            if (!u.getUnitType().canAttack()
                    || u.getUnitType() == UnitType.WORKER
                    || u.getUnitType().isSortieBased()) {
                continue;
            }
            if (u.getCurrentCommand() instanceof IdleCommand) {
                combatIdle++;
            }
        }

        return new SkirmishAiTickContext(
                playerId,
                frameCount,
                faction,
                anchor != null ? anchor.copy() : null,
                entities.getConstructedBuildingTypes(playerId),
                faction.getResourceAmount(com.fullsteam.model.ResourceType.CREDITS),
                faction.isHasLowPower(),
                faction.getPowerGenerated(),
                faction.getPowerConsumed(),
                enemies,
                Optional.ofNullable(nearest),
                harvesters,
                combatIdle,
                refineriesUc,
                plantsUc,
                researchLabsUc,
                factoriesUc,
                profile
        );
    }

    private static Vector2 resolveBaseAnchor(GameEntities entities, int playerId) {
        Building hq = entities.getBuildings().values().stream()
                .filter(b -> b.belongsTo(playerId)
                        && b.getBuildingType() == BuildingType.HEADQUARTERS
                        && b.isActive())
                .findFirst()
                .orElse(null);
        if (hq != null) {
            return hq.getPosition().copy();
        }
        return entities.getBuildings().values().stream()
                .filter(b -> b.belongsTo(playerId) && b.isActive())
                .map(Building::getPosition)
                .map(Vector2::copy)
                .findFirst()
                .orElse(null);
    }

    private static boolean isIdleWorker(Unit u) {
        return u.getCurrentCommand() == null || u.getCurrentCommand() instanceof IdleCommand;
    }

    public boolean hasCompletedRefinery() {
        return completedBuildingTypes.contains(BuildingType.REFINERY);
    }

    public boolean hasCompletedResearchLab() {
        return completedBuildingTypes.contains(BuildingType.RESEARCH_LAB);
    }

    public boolean hasCompletedFactory() {
        return completedBuildingTypes.contains(BuildingType.FACTORY);
    }
}
