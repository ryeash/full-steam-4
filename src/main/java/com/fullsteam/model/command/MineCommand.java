package com.fullsteam.model.command;

import com.fullsteam.model.Building;
import com.fullsteam.model.Obstacle;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Command to mine (destroy) an obstacle
 * Includes pickaxe durability management and automatic HQ returns for repair
 */
@Getter
public class MineCommand extends UnitCommand {
    private final Obstacle obstacle;
    
    @Setter
    private boolean returningForRepair = false;
    
    @Setter
    private Building targetHeadquarters = null;
    
    public MineCommand(Unit unit, Obstacle obstacle, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.obstacle = obstacle;
    }
    
    @Override
    public boolean update(double deltaTime) {
        // Command fails if obstacle is destroyed
        if (obstacle == null || !obstacle.isActive()) {
            return false;
        }
        
        // Call the actual work methods
        if (returningForRepair) {
            boolean repairComplete = unit.returnForPickaxeRepair(targetHeadquarters, deltaTime);
            if (repairComplete) {
                // Repair complete, return to mining
                returningForRepair = false;
                targetHeadquarters = null;
            }
        } else {
            boolean needsRepair = unit.mineObstacle(obstacle, deltaTime);
            if (needsRepair) {
                // Pickaxe broke or low, switch to returning for repair
                returningForRepair = true;
                // RTSGameManager will set targetHeadquarters
            }
        }
        
        return true;
    }
    
    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        Vector2 currentPos = unit.getPosition();
        
        if (returningForRepair && targetHeadquarters != null) {
            // Moving to headquarters for repair
            Vector2 hqPos = targetHeadquarters.getPosition();
            double distance = currentPos.distance(hqPos);
            
            if (distance > 50.0) {
                // Compute path if needed (HQ doesn't move)
                if (path.isEmpty() || lastPathTarget == null || 
                    lastPathTarget.distance(hqPos) > 10.0) { // Check if target changed
                    computePathTo(hqPos);
                }
                
                // Follow path to HQ
                followPathTo(hqPos, nearbyUnits, 50.0);
            } else {
                unit.getBody().setLinearVelocity(0, 0);
            }
        } else if (obstacle != null && obstacle.isActive()) {
            // Moving to obstacle
            Vector2 obstaclePos = obstacle.getPosition();
            double distance = currentPos.distance(obstaclePos);
            double miningRange = obstacle.getSize() + 10.0;
            
            if (distance > miningRange) {
                // Compute path if needed (obstacle doesn't move)
                if (path.isEmpty() || lastPathTarget == null || 
                    lastPathTarget.distance(obstaclePos) > 10.0) { // Check if target changed
                    computePathTo(obstaclePos);
                }
                
                // Follow path to obstacle
                followPathTo(obstaclePos, nearbyUnits, miningRange);
            } else {
                unit.getBody().setLinearVelocity(0, 0);
            }
        }
    }
    
    @Override
    public Vector2 getTargetPosition() {
        if (returningForRepair && targetHeadquarters != null) {
            return targetHeadquarters.getPosition();
        }
        return obstacle != null ? obstacle.getPosition() : null;
    }
    
    @Override
    public boolean isMoving() {
        Vector2 targetPos = getTargetPosition();
        if (targetPos == null) return false;
        
        double distance = unit.getPosition().distance(targetPos);
        
        if (returningForRepair) {
            return distance > 50.0;
        } else {
            double miningRange = obstacle != null ? obstacle.getSize() + 10.0 : 0;
            return distance > miningRange;
        }
    }
    
    @Override
    public String getDescription() {
        if (returningForRepair) {
            return String.format("Returning to HQ %d for pickaxe repair", 
                    targetHeadquarters != null ? targetHeadquarters.getId() : -1);
        }
        return String.format("Mining obstacle %d", obstacle != null ? obstacle.getId() : -1);
    }
}

