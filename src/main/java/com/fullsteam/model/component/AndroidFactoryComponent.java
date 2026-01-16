package com.fullsteam.model.component;

import com.fullsteam.games.IdGenerator;
import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.PlayerFaction;
import com.fullsteam.model.RTSCollisionProcessor;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.command.MoveCommand;
import com.fullsteam.model.factions.FactionDefinition;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Component that handles autonomous android production for Android Factory.
 * Automatically produces androids to maintain a maximum population.
 * When an android dies, a new one is automatically queued.
 * If the factory is destroyed, all androids are destroyed.
 * <p>
 * Used by: ANDROID_FACTORY
 */
@Slf4j
@Getter
public class AndroidFactoryComponent extends AbstractBuildingComponent {
    private static final int MAX_ANDROIDS = 6;
    private static final UnitType ANDROID_TYPE = UnitType.ANDROID;

    private final Set<Integer> controlledAndroidIds = new HashSet<>();
    private double productionProgress = 0;
    private boolean producingAndroid = false;
    @Setter
    private Vector2 rallyPoint;

    @Override
    public void update(boolean hasLowPower) {
        double deltaTime = gameEntities.getWorld().getTimeStep().getDeltaTime();
        if (building.isUnderConstruction() || hasLowPower) {
            return;
        }

        int currentCount = controlledAndroidIds.size();
        if (currentCount < MAX_ANDROIDS && !producingAndroid) {
            producingAndroid = true;
            productionProgress = 0;
            log.info("Android Factory {} started producing android ({}/{})",
                    building.getId(), currentCount, MAX_ANDROIDS);
        }

        if (producingAndroid) {
            PlayerFaction faction = gameEntities.getPlayerFactions().get(building.getOwnerId());
            double effectiveSpeed = 1.0;
            if (faction != null) {
                Map<BuildingType, FactionDefinition.BuildingStatModifier> buildingMods = faction.getFactionDefinition().getBuildingStatModifiers();
                if (buildingMods != null && buildingMods.containsKey(building.getBuildingType())) {
                    effectiveSpeed = buildingMods.get(building.getBuildingType()).getProductionSpeedMultiplier();
                }
            }

            productionProgress += deltaTime * effectiveSpeed;

            if (productionProgress >= ANDROID_TYPE.getBuildTimeSeconds()) {
                Vector2 spawnPos = findSpawnPosition(building);
                Unit android = new Unit(
                        IdGenerator.nextEntityId(),
                        ANDROID_TYPE,
                        spawnPos.x,
                        spawnPos.y,
                        building.getOwnerId(),
                        building.getTeamNumber(),
                        faction
                );
                android.initializeComponents(gameEntities);
                android.getComponent(AndroidComponent.class)
                        .ifPresent(ac -> ac.setAndroidFactoryId(building.getId()));

                registerAndroid(android.getId());
                producingAndroid = false;
                productionProgress = 0;

                gameEntities.getUnits().put(android.getId(), android);
                gameEntities.getWorld().addBody(android.getBody());

                if (building.getRallyPoint() != null) {
                    android.issueCommand(new MoveCommand(android, building.getRallyPoint(), false), gameEntities);
                }
            }
        }
    }

    /**
     * Find a valid spawn position near the factory.
     * Simple approach: try positions around the factory in a circle.
     */
    private Vector2 findSpawnPosition(Building factory) {
        Vector2 buildingPos = building.getPosition();

        double buildingRadius = building.getBody().getFixture(0).getShape().getRadius();
        double spawnRadius = buildingRadius + 5.0; // Small clearance from building edge

        double initialAngle;
        if (rallyPoint != null && !rallyPoint.equals(buildingPos)) {
            Vector2 toRallyPoint = rallyPoint.difference(buildingPos);
            initialAngle = Math.atan2(toRallyPoint.y, toRallyPoint.x);
        } else {
            initialAngle = 0;
        }

        Vector2 candidatePos = projectPosition(buildingPos, initialAngle, spawnRadius);
        if (isValidPosition(gameEntities, candidatePos)) {
            return candidatePos;
        }

        for (int i = 1; i < 12; i++) {
            double angleOffset = (Math.PI * 2 * i) / 12;
            if (i % 2 == 0) {
                angleOffset = -angleOffset;
            }
            double angle = initialAngle + angleOffset;
            candidatePos = projectPosition(buildingPos, angle, spawnRadius);
            if (isValidPosition(gameEntities, candidatePos)) {
                return candidatePos;
            }
        }

        log.warn("Could not find optimal spawn position near building {}, using fallback position", building.getId());
        return projectPosition(buildingPos, initialAngle, spawnRadius + 20.0);
    }

    @Override
    public void onDestroy() {
        log.info("Android Factory {} destroyed - {} androids will be destroyed", building.getId(), controlledAndroidIds.size());
        for (Integer controlledAndroidId : controlledAndroidIds) {
            Unit unit = gameEntities.getUnits().get(controlledAndroidId);
            if (unit != null) {
                unit.setActive(false);
            }
        }
    }

    /**
     * Register an android as controlled by this factory.
     *
     * @param androidId The ID of the android unit
     */
    public void registerAndroid(int androidId) {
        controlledAndroidIds.add(androidId);
    }

    /**
     * Unregister an android (when it dies).
     * This will trigger automatic replacement production.
     *
     * @param androidId The ID of the android unit
     */
    public void unregisterAndroid(int androidId) {
        if (controlledAndroidIds.remove(androidId)) {
            log.info("Android {} destroyed - Android Factory will produce replacement ({}/{} androids)",
                    androidId, controlledAndroidIds.size(), MAX_ANDROIDS);
        }
    }

    /**
     * Get production progress (0.0 to 1.0).
     */
    public double getProductionProgressPercent() {
        if (!producingAndroid) {
            return 0.0;
        }
        return Math.min(1.0, productionProgress / ANDROID_TYPE.getBuildTimeSeconds());
    }

    /**
     * Project a position at the given angle and distance from a center point.
     */
    private Vector2 projectPosition(Vector2 center, double angle, double distance) {
        double x = center.x + Math.cos(angle) * distance;
        double y = center.y + Math.sin(angle) * distance;
        return new Vector2(x, y);
    }

    /**
     * Check if a position is valid for spawning (not overlapping with other entities).
     */
    private boolean isValidPosition(GameEntities gameEntities, Vector2 position) {
        double unitSize = UnitType.ANDROID.getSize();
        return ((RTSCollisionProcessor) gameEntities.getWorld().getCollisionListeners().get(0))
                .isValidSpawnPosition(
                        position,
                        unitSize,
                        gameEntities.getGameConfig().getWorldWidth(),
                        gameEntities.getGameConfig().getWorldHeight());
    }
}


