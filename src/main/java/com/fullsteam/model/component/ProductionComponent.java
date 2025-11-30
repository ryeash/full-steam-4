package com.fullsteam.model.component;

import com.fullsteam.games.IdGenerator;
import com.fullsteam.model.Building;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.PlayerFaction;
import com.fullsteam.model.RTSCollisionProcessor;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.command.MoveCommand;
import com.fullsteam.model.research.ResearchModifier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Component that handles unit production for buildings.
 * Buildings with this component can queue and produce units over time.
 * Production is paused when the building has low power.
 * <p>
 * Used by: HEADQUARTERS, BARRACKS, FACTORY, WEAPONS_DEPOT, ADVANCED_FACTORY
 */
@Slf4j
@Getter
public class ProductionComponent extends AbstractBuildingComponent {
    private final Queue<ProductionOrder> productionQueue = new LinkedList<>();
    private ResearchModifier researchModifier = new ResearchModifier();
    private ProductionOrder currentProduction = null;
    private double productionProgress = 0; // seconds
    private Vector2 rallyPoint;

    /**
     * Create a production component with a default rally point.
     *
     * @param initialRallyPoint The initial rally point for produced units
     */
    public ProductionComponent(Vector2 initialRallyPoint) {
        this.rallyPoint = initialRallyPoint != null ? initialRallyPoint.copy() : null;
    }

    @Override
    public void update(boolean hasLowPower) {
        double deltaTime = gameEntities.getWorld().getTimeStep().getDeltaTime();
        // Start next production if none active
        if (currentProduction == null && !productionQueue.isEmpty()) {
            currentProduction = productionQueue.poll();
            productionProgress = 0;
            log.info("Building {} started producing {}", building.getId(), currentProduction.unitType);
        }

        // Update current production (only if not low power)
        if (currentProduction != null && !hasLowPower) {
            productionProgress += deltaTime;

            // Check if production is complete
            if ((productionProgress * researchModifier.getProductionSpeedMultiplier()) >= currentProduction.unitType.getBuildTimeSeconds()) {
                UnitType unitType = currentProduction.unitType;
                currentProduction = null;
                productionProgress = 0;

                // Create unit
                Unit unit = new Unit(
                        IdGenerator.nextEntityId(),
                        unitType,
                        0, 0,
                        building.getOwnerId(),
                        building.getTeamNumber()
                );

                // Find spawn position near building
                Vector2 spawnPos = findSpawnPosition(gameEntities, building, unit);
                unit.setPosition(spawnPos);

                // Apply research modifiers from player's research
                PlayerFaction faction = gameEntities.getPlayerFactions().get(building.getOwnerId());
                if (faction != null && faction.getResearchManager() != null) {
                    unit.applyResearchModifiers(faction.getResearchManager().getCumulativeModifier());
                }

                gameEntities.getUnits().put(unit.getId(), unit);
                gameEntities.getWorld().addBody(unit.getBody());

                // Order unit to rally point
                if (building.getRallyPoint() != null) {
                    unit.issueCommand(new MoveCommand(unit, building.getRallyPoint(), false));
                }
            }
        } else if (hasLowPower && currentProduction != null) {
            // Production is paused due to low power
            log.debug("Building {} production paused due to LOW POWER", building.getId());
        }
    }

    @Override
    public void applyResearchModifiers(ResearchModifier modifier) {
        this.researchModifier = modifier;
    }

    /**
     * Queue a unit for production.
     * Note: Validation should be done by the caller (RTSGameManager) using faction tech tree
     *
     * @param unitType The type of unit to produce
     * @return true if successfully queued
     */
    public boolean queueUnitProduction(UnitType unitType) {
        productionQueue.add(new ProductionOrder(unitType));
        return true;
    }

    /**
     * Cancel current production.
     *
     * @return The unit type that was cancelled, or null if no production active
     */
    public UnitType cancelCurrentProduction() {
        if (currentProduction != null) {
            UnitType cancelled = currentProduction.unitType;
            currentProduction = null;
            productionProgress = 0;
            return cancelled;
        }
        return null;
    }

    /**
     * Get production progress as a percentage (0.0 to 1.0).
     *
     * @return Production progress, or 0 if no production active
     */
    public double getProductionPercent() {
        if (currentProduction == null) {
            return 0;
        }
        return productionProgress / currentProduction.unitType.getBuildTimeSeconds();
    }

    /**
     * Get the number of units in the production queue (excluding current production).
     *
     * @return Number of queued units
     */
    public int getProductionQueueSize() {
        return productionQueue.size();
    }

    /**
     * Set the rally point for produced units.
     *
     * @param point The new rally point
     */
    public void setRallyPoint(Vector2 point) {
        this.rallyPoint = point != null ? point.copy() : null;
    }

    /**
     * Represents a unit production order in the queue.
     */
    @Getter
    public static class ProductionOrder {
        private final UnitType unitType;
        private final long queueTime;

        public ProductionOrder(UnitType unitType) {
            this.unitType = unitType;
            this.queueTime = System.currentTimeMillis();
        }
    }

    /**
     * Find a valid spawn position near a building.
     * Projects the spawn point at the proper radius in the direction of the waypoint (rally point).
     * If that position is blocked, rotates around the building to find a clear spot.
     */
    private Vector2 findSpawnPosition(GameEntities gameEntities, Building building, Unit unit) {
        Vector2 buildingPos = building.getPosition();
        Vector2 rallyPoint = building.getRallyPoint();

        // Calculate spawn radius (building radius + small clearance)
        double buildingRadius = building.getBody().getFixture(0).getShape().getRadius();
        double spawnRadius = buildingRadius + 5.0; // Small clearance from building edge

        // Determine initial spawn angle based on rally point direction
        double initialAngle;
        if (rallyPoint != null && !rallyPoint.equals(buildingPos)) {
            // Project spawn point in direction of rally point
            Vector2 toRallyPoint = rallyPoint.difference(buildingPos);
            initialAngle = Math.atan2(toRallyPoint.y, toRallyPoint.x);
        } else {
            // No rally point or rally point is at building, use default direction (right)
            initialAngle = 0;
        }

        // Try the initial projected position first
        Vector2 candidatePos = projectPosition(buildingPos, initialAngle, spawnRadius);
        if (isValidPosition(gameEntities, candidatePos, unit)) {
            return candidatePos;
        }

        // If initial position is blocked, try rotating around the building
        // Try 11 additional positions (12 total, evenly spaced around the circle)
        for (int i = 1; i < 12; i++) {
            // Alternate between clockwise and counter-clockwise rotations
            // This finds the nearest clear spot to the desired direction
            double angleOffset = (Math.PI * 2 * i) / 12;
            if (i % 2 == 0) {
                angleOffset = -angleOffset; // Counter-clockwise
            }

            double angle = initialAngle + angleOffset;
            candidatePos = projectPosition(buildingPos, angle, spawnRadius);

            if (isValidPosition(gameEntities, candidatePos, unit)) {
                return candidatePos;
            }
        }

        // If no valid position found, use fallback (spawn at initial angle but further out)
        log.warn("Could not find optimal spawn position near building {}, using fallback position",
                building.getId());
        return projectPosition(buildingPos, initialAngle, spawnRadius + 20.0);
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
    private boolean isValidPosition(GameEntities gameEntities, Vector2 position, Unit unit) {
        // Use a standard unit size for validation (most units are similar size)
        double unitSize = unit.getBody().getFixture(0).getShape().getRadius();

        return ((RTSCollisionProcessor) gameEntities.getWorld().getCollisionListeners().get(0))
                .isValidSpawnPosition(
                        position,
                        unitSize,
                        gameEntities.getResourceDeposits(),
                        gameEntities.getGameConfig().getWorldWidth(),
                        gameEntities.getGameConfig().getWorldHeight());
    }
}




