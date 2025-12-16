package com.fullsteam.model.command;

import com.fullsteam.model.Building;
import com.fullsteam.model.Obstacle;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Command to harvest resources from a harvestable obstacle and return them to a refinery
 */
@Getter
public class HarvestCommand extends UnitCommand {
    private final Obstacle obstacle;

    @Setter
    private boolean returningResources = false;

    @Setter
    private Building targetRefinery = null;

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

        // Call the actual work methods
        if (returningResources) {
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
                // RTSGameManager will set targetRefinery
            }
        }

        return true;
    }

    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        Vector2 currentPos = unit.getPosition();

        if (returningResources && targetRefinery != null) {
            // Moving to refinery
            Vector2 refineryPos = targetRefinery.getPosition();
            double distance = currentPos.distance(refineryPos);

            if (distance > 50.0) {
                // Compute path if needed (refinery doesn't move)
                if (path.isEmpty() || lastPathTarget == null ||
                        lastPathTarget.distance(refineryPos) > 10.0) { // Check if target changed
                    computePathTo(refineryPos);
                }

                // Follow path to refinery
                followPathTo(refineryPos, nearbyUnits, 50.0);
            } else {
                unit.getBody().setLinearVelocity(0, 0);
            }
        } else if (obstacle != null && obstacle.isActive()) {
            // Moving to obstacle
            Vector2 obstaclePos = obstacle.getPosition();
            double distance = currentPos.distance(obstaclePos);

            if (distance > 50.0) {
                // Compute path if needed (obstacle doesn't move)
                if (path.isEmpty() || lastPathTarget == null ||
                        lastPathTarget.distance(obstaclePos) > 10.0) { // Check if target changed
                    computePathTo(obstaclePos);
                }

                // Follow path to obstacle
                followPathTo(obstaclePos, nearbyUnits, 50.0);
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
        if (targetPos == null) return false;

        double distance = unit.getPosition().distance(targetPos);
        return distance > 50.0;
    }

    @Override
    public String getDescription() {
        if (returningResources) {
            return String.format("Returning resources to refinery %d",
                    targetRefinery != null ? targetRefinery.getId() : -1);
        }
        return String.format("Harvesting obstacle %d (%s)",
                obstacle != null ? obstacle.getId() : -1,
                obstacle != null && obstacle.getResourceType() != null ? obstacle.getResourceType().name() : "null");
    }
}

