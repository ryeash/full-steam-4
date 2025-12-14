package com.fullsteam.model.command;

import com.fullsteam.model.Building;
import com.fullsteam.model.Unit;
import com.fullsteam.model.command.UnitCommand;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Command to construct or help construct a building
 */
@Getter
public class ConstructCommand extends UnitCommand {
    private final Building building;
    
    public ConstructCommand(Unit unit, Building building, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.building = building;
    }
    
    @Override
    public boolean update(double deltaTime) {
        // Command fails if building is destroyed or construction is complete
        if (building == null || !building.isActive() || !building.isUnderConstruction()) {
            return false;
        }
        
        // Call the actual work method
        unit.constructBuilding(building, deltaTime);
        
        return true;
    }
    
    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        if (building == null || !building.isActive()) {
            unit.getBody().setLinearVelocity(0, 0);
            return;
        }
        
        Vector2 currentPos = unit.getPosition();
        Vector2 buildingPos = building.getPosition();
        double distance = currentPos.distance(buildingPos);
        double constructionRange = building.getBuildingType().getSize() + 20.0;
        
        // Move to building if too far
        if (distance > constructionRange) {
            // Compute path if needed (target has moved or no path exists)
            if (path.isEmpty() || lastPathTarget == null || 
                lastPathTarget.distance(buildingPos) > 30.0) {
                computePathTo(buildingPos);
            }
            
            // Follow path to building
            followPathTo(buildingPos, nearbyUnits, constructionRange);
        } else {
            // In range, stop moving
            unit.getBody().setLinearVelocity(0, 0);
        }
    }
    
    @Override
    public Vector2 getTargetPosition() {
        return building != null ? building.getPosition() : null;
    }
    
    @Override
    public boolean isMoving() {
        if (building == null) return false;
        
        double distance = unit.getPosition().distance(building.getPosition());
        double constructionRange = building.getBuildingType().getSize() + 20.0;
        return distance > constructionRange;
    }
    
    @Override
    public String getDescription() {
        return String.format("Constructing building %d (%s)", 
                building != null ? building.getId() : -1,
                building != null ? building.getBuildingType().name() : "null");
    }
}

