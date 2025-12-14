package com.fullsteam.model.command;

import com.fullsteam.model.Building;
import com.fullsteam.model.Unit;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Command for a unit to enter a bunker (garrison)
 */
@Getter
public class GarrisonBunkerCommand extends UnitCommand {
    private final Building bunker;

    public GarrisonBunkerCommand(Unit unit, Building bunker, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.bunker = bunker;
    }

    @Override
    public boolean update(double deltaTime) {
        // Command fails if bunker is destroyed or not active
        if (bunker == null || !bunker.isActive()) {
            return false;
        }

        // Check if unit is close enough to garrison
        Vector2 unitPos = unit.getPosition();
        Vector2 bunkerPos = bunker.getPosition();
        double distance = unitPos.distance(bunkerPos);
        double garrisonRange = bunker.getBuildingType().getSize() + 10.0;

        if (distance <= garrisonRange) {
            // Try to garrison the unit
            boolean success = bunker.garrisonUnit(unit);
            return !success; // Command completes when garrison succeeds
        }

        return true; // Continue moving toward bunker
    }

    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        if (bunker == null || !bunker.isActive()) {
            unit.getBody().setLinearVelocity(0, 0);
            return;
        }

        Vector2 currentPos = unit.getPosition();
        Vector2 bunkerPos = bunker.getPosition();
        double distance = currentPos.distance(bunkerPos);
        double garrisonRange = bunker.getBuildingType().getSize() + 10.0;

        // Move to bunker if too far
        if (distance > garrisonRange) {
            // Compute path if needed (bunker doesn't move)
            if (path.isEmpty() || lastPathTarget == null) {
                computePathTo(bunkerPos);
            }
            
            // Follow path to bunker
            followPathTo(bunkerPos, nearbyUnits, garrisonRange);
        } else {
            // In range, stop moving
            unit.getBody().setLinearVelocity(0, 0);
        }
    }

    @Override
    public Vector2 getTargetPosition() {
        return bunker != null ? bunker.getPosition() : null;
    }

    @Override
    public boolean isMoving() {
        if (bunker == null) {
            return false;
        }
        Vector2 currentPos = unit.getPosition();
        Vector2 bunkerPos = bunker.getPosition();
        double distance = currentPos.distance(bunkerPos);
        double garrisonRange = bunker.getBuildingType().getSize() + 10.0;
        return distance > garrisonRange;
    }

    @Override
    public String getDescription() {
        return "Garrison in bunker " + (bunker != null ? bunker.getId() : "null");
    }
}
