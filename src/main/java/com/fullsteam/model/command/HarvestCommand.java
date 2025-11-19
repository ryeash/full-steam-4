package com.fullsteam.model.command;

import com.fullsteam.model.Building;
import com.fullsteam.model.ResourceDeposit;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Command to harvest resources from a deposit and return them to a refinery
 */
@Getter
public class HarvestCommand extends UnitCommand {
    private final ResourceDeposit deposit;
    
    @Setter
    private boolean returningResources = false;
    
    @Setter
    private Building targetRefinery = null;
    
    public HarvestCommand(Unit unit, ResourceDeposit deposit, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.deposit = deposit;
    }
    
    @Override
    public boolean update(double deltaTime) {
        // Command fails if deposit is depleted
        if (deposit == null || !deposit.isActive()) {
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
            boolean shouldReturn = unit.harvestResources(deposit, deltaTime);
            if (shouldReturn) {
                // Full or deposit depleted, switch to returning
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
                unit.applySteeringForces(refineryPos, nearbyUnits, deltaTime);
            } else {
                unit.getBody().setLinearVelocity(0, 0);
            }
        } else if (deposit != null && deposit.isActive()) {
            // Moving to deposit
            Vector2 depositPos = deposit.getPosition();
            double distance = currentPos.distance(depositPos);
            
            if (distance > 50.0) {
                unit.applySteeringForces(depositPos, nearbyUnits, deltaTime);
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
        return deposit != null ? deposit.getPosition() : null;
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
        return String.format("Harvesting deposit %d (%s)", 
                deposit != null ? deposit.getId() : -1,
                deposit != null ? deposit.getResourceType().name() : "null");
    }
}

