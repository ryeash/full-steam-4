package com.fullsteam.model.component;

import com.fullsteam.games.IdGenerator;
import com.fullsteam.model.Building;
import com.fullsteam.model.PlayerFaction;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.command.MoveCommand;
import com.fullsteam.model.research.ResearchModifier;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.HashSet;
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
    private static final int MAX_ANDROIDS = 6; // Maximum number of androids per factory
    private static final UnitType ANDROID_TYPE = UnitType.ANDROID;

    private final Set<Integer> controlledAndroidIds = new HashSet<>();
    private double productionProgress = 0; // seconds
    private boolean producingAndroid = false;
    private ResearchModifier modifier = new ResearchModifier();
    @Setter
    private Vector2 rallyPoint;

    @Override
    public void update(boolean hasLowPower) {
        double deltaTime = gameEntities.getWorld().getTimeStep().getDeltaTime();
        // Don't produce while under construction or low power
        if (building.isUnderConstruction() || hasLowPower) {
            if (hasLowPower && producingAndroid) {
                log.debug("Android Factory {} production paused due to LOW POWER", building.getId());
            }
            return;
        }

        // Check if we need to produce more androids
        int currentCount = controlledAndroidIds.size();

        if (currentCount < MAX_ANDROIDS && !producingAndroid) {
            // Start production
            producingAndroid = true;
            productionProgress = 0;
            log.info("Android Factory {} started producing android ({}/{})",
                    building.getId(), currentCount, MAX_ANDROIDS);
        }

        // Update production progress
        if (producingAndroid) {
            productionProgress += deltaTime;

            // Check if production is complete
            if ((productionProgress * modifier.getProductionSpeedMultiplier()) >= ANDROID_TYPE.getBuildTimeSeconds()) {
                // Spawn the android
                Vector2 spawnPos = findSpawnPosition(building);

                Unit android = new Unit(
                        IdGenerator.nextEntityId(),
                        ANDROID_TYPE,
                        spawnPos.x,
                        spawnPos.y,
                        building.getOwnerId(),
                        building.getTeamNumber()
                );

                // Initialize components
                android.initializeComponents(gameEntities);

                // Link android to its factory via component
                android.getComponent(AndroidComponent.class)
                        .ifPresent(ac -> ac.setAndroidFactoryId(building.getId()));

                // Register android with factory
                registerAndroid(android.getId());

                // Reset production state
                producingAndroid = false;
                productionProgress = 0;

                // Apply research modifiers (reuse faction variable from above)
                PlayerFaction faction = gameEntities.getPlayerFactions().get(building.getOwnerId());
                if (faction != null && faction.getResearchManager() != null) {
                    android.applyResearchModifiers(faction.getResearchManager().getCumulativeModifier());
                }

                gameEntities.getUnits().put(android.getId(), android);
                gameEntities.getWorld().addBody(android.getBody());

                // Order android to rally point
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
        Vector2 factoryPos = factory.getPosition();
        double factorySize = factory.getBuildingType().getSize();
        double spawnDistance = factorySize + 50; // Spawn 50 units away from edge

        // Try 8 positions around the factory
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 * i) / 8;
            double x = factoryPos.x + Math.cos(angle) * spawnDistance;
            double y = factoryPos.y + Math.sin(angle) * spawnDistance;

            // For now, just return the first position
            // RTSGameManager will handle collision validation if needed
            return new Vector2(x, y);
        }

        // Fallback: spawn directly to the right
        return new Vector2(factoryPos.x + spawnDistance, factoryPos.y);
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

    @Override
    public void applyResearchModifiers(ResearchModifier modifier) {
        this.modifier = modifier;
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
        return Math.min(1.0, productionProgress / (ANDROID_TYPE.getBuildTimeSeconds() * modifier.getProductionSpeedMultiplier()));
    }
}


