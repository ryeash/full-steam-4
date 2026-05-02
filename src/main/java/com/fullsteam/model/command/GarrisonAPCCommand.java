package com.fullsteam.model.command;

import com.fullsteam.model.Unit;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;

/**
 * Command for a unit to enter a friendly transport (APC or Chinook) for garrison transport.
 */
@Getter
public class GarrisonAPCCommand extends UnitCommand {
    private final Unit apc;

    public GarrisonAPCCommand(Unit unit, Unit apc, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.apc = apc;
    }

    @Override
    public boolean update(double deltaTime) {
        // Command fails if carrier is destroyed or not active
        if (apc == null || !apc.isActive()) {
            return false;
        }

        // Check if unit is close enough to garrison
        Vector2 unitPos = unit.getPosition();
        Vector2 apcPos = apc.getPosition();
        double distance = unitPos.distance(apcPos);
        double garrisonRange = apc.getUnitType().getSize() + 10.0;
        if (distance <= garrisonRange) {
            return !apc.garrisonUnit(unit);
        }
        return true;
    }

    @Override
    public void updateMovement(double deltaTime) {
        if (apc == null || !apc.isActive()) {
            unit.getBody().setLinearVelocity(0, 0);
            return;
        }

        Vector2 currentPos = unit.getPosition();
        Vector2 apcPos = apc.getPosition();
        double distance = currentPos.distance(apcPos);
        double garrisonRange = apc.getUnitType().getSize() + 10.0;

        // Move to carrier if too far
        if (distance > garrisonRange) {
            if (path.isEmpty() || lastPathTarget == null || lastPathTarget.distance(apcPos) > 20.0) {
                computePathTo(apcPos);
            }
            followPathTo(apcPos, nearbyUnits(), garrisonRange);
        } else {
            // In range, stop moving
            unit.getBody().setLinearVelocity(0, 0);
        }
    }

    @Override
    public Vector2 getTargetPosition() {
        return apc != null ? apc.getPosition() : null;
    }

    @Override
    public boolean isMoving() {
        if (apc == null) {
            return false;
        }
        Vector2 currentPos = unit.getPosition();
        Vector2 apcPos = apc.getPosition();
        double distance = currentPos.distance(apcPos);
        double garrisonRange = apc.getUnitType().getSize() + 10.0;
        return distance > garrisonRange;
    }

    @Override
    public String getDescription() {
        return "Garrison in transport " + (apc != null ? apc.getId() : "null");
    }
}
