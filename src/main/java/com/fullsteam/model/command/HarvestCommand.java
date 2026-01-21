package com.fullsteam.model.command;

import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.Obstacle;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

/**
 * Command to harvest resources from a harvestable obstacle and return them to a refinery
 */
@Getter
@Slf4j
public class HarvestCommand extends UnitCommand {
    private final Obstacle obstacle;

    @Setter
    private boolean returningResources = false;

    @Setter
    private Building targetRefinery = null;

    // Track when we last re-evaluated refinery to avoid doing it every frame
    private long lastRefineryCheckTime = 0;
    private static final long REFINERY_CHECK_INTERVAL_MS = 1000; // Check every 1 second

    public HarvestCommand(Unit unit, Obstacle obstacle, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.obstacle = obstacle;
    }

    @Override
    public boolean update(double deltaTime) {
        // Command fails if obstacle is depleted or not harvestable
        if (obstacle == null || !obstacle.isActive() || !obstacle.isHarvestable()) {
            return false;
        }

        // Help workers find/update their target refinery when returning resources
        if (returningResources) {
            updateTargetRefinery();

            boolean depositedResources = unit.returnResourcesToRefinery(targetRefinery, deltaTime);
            if (depositedResources) {
                // Resources deposited, return to harvesting
                returningResources = false;
                targetRefinery = null;
            }
        } else {
            boolean shouldReturn = unit.harvestResourcesFromObstacle(obstacle, deltaTime);
            if (shouldReturn) {
                // Full or obstacle depleted, switch to returning
                returningResources = true;
                // Find nearest refinery
                updateTargetRefinery();
            }
        }

        return true;
    }

    /**
     * Find and update the target refinery for this harvest command.
     * Re-evaluates periodically to find closer refineries or handle destroyed refineries.
     */
    private void updateTargetRefinery() {
        if (gameEntities == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Building currentRefinery = targetRefinery;

        // Re-evaluate refinery if:
        // 1. No refinery assigned yet
        // 2. Current refinery is no longer valid (destroyed or under construction)
        // 3. Periodically check for a closer one (every 1 second)
        boolean needsReevaluation = currentRefinery == null ||
                !currentRefinery.isActive() ||
                currentRefinery.isUnderConstruction() ||
                (now - lastRefineryCheckTime >= REFINERY_CHECK_INTERVAL_MS);

        if (needsReevaluation) {
            Building refinery = findNearestRefinery();
            if (refinery != currentRefinery) {
                targetRefinery = refinery;
                log.debug("Worker {} updated refinery to {} (type: {})",
                        unit.getId(),
                        refinery != null ? refinery.getId() : "null",
                        refinery != null ? refinery.getBuildingType() : "none");
            }
            lastRefineryCheckTime = now;
        }
    }

    /**
     * Find the nearest refinery or headquarters for this worker to deposit resources.
     *
     * @return Nearest valid refinery/headquarters, or null if none found
     */
    private Building findNearestRefinery() {
        Building nearestDropoff = null;
        double nearestDistance = Double.MAX_VALUE;

        // Search for both refineries AND headquarters, pick the nearest one
        for (Building building : gameEntities.getBuildings().values()) {
            if (building.isActive() &&
                    building.getOwnerId() == unit.getOwnerId() &&
                    !building.isUnderConstruction() &&
                    (building.getBuildingType() == BuildingType.REFINERY ||
                            building.getBuildingType() == BuildingType.HEADQUARTERS)) {

                double distance = unit.getPosition().distance(building.getPosition());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestDropoff = building;
                }
            }
        }

        return nearestDropoff;
    }

    @Override
    public void updateMovement(double deltaTime) {
        Vector2 currentPos = unit.getPosition();
        if (returningResources && targetRefinery != null) {
            Vector2 refineryPos = targetRefinery.getPosition();
            double distance = currentPos.distance(refineryPos);
            if (distance > 50.0) {
                if (path.isEmpty() || lastPathTarget == null || lastPathTarget.distance(refineryPos) > 10.0) {
                    computePathTo(refineryPos);
                }
                followPathTo(refineryPos, nearbyUnits(), 50.0);
            } else {
                unit.getBody().setLinearVelocity(0, 0);
            }
        } else if (obstacle != null && obstacle.isActive()) {
            Vector2 obstaclePos = obstacle.getPosition();
            double distance = currentPos.distance(obstaclePos);
            double effectiveHarvestRange = obstacle.getHarvestRange() + obstacle.getSize();
            if (distance > effectiveHarvestRange) {
                if (path.isEmpty() || lastPathTarget == null || lastPathTarget.distance(obstaclePos) > 10.0) {
                    computePathTo(obstaclePos);
                }
                followPathTo(obstaclePos, nearbyUnits(), effectiveHarvestRange);
            } else {
                unit.getBody().setLinearVelocity(0, 0);
            }
        }
    }

    @Override
    public Vector2 getTargetPosition() {
        if (returningResources && targetRefinery != null) {
            return targetRefinery.getPosition();
        }
        return obstacle != null ? obstacle.getPosition() : null;
    }

    @Override
    public boolean isMoving() {
        Vector2 targetPos = getTargetPosition();
        if (targetPos == null) {
            return false;
        }
        double distance = unit.getPosition().distance(targetPos);
        return distance > 50.0;
    }

    @Override
    public String getDescription() {
        if (returningResources) {
            return String.format("Returning resources to refinery %d", targetRefinery != null ? targetRefinery.getId() : -1);
        }
        return String.format("Harvesting obstacle %d (%s)",
                obstacle != null ? obstacle.getId() : -1,
                obstacle != null && obstacle.getResourceType() != null ? obstacle.getResourceType().name() : "null");
    }
}

