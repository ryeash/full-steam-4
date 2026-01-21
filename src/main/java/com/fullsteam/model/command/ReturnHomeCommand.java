package com.fullsteam.model.command;

import com.fullsteam.model.Unit;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;

/**
 * Command for units in DEFENSIVE stance to return to their home position when idle.
 * This is an AI behavior that runs when the unit has no player orders and is far from home.
 */
@Getter
public class ReturnHomeCommand extends UnitCommand {
    private final Vector2 homePosition;
    private static final double ARRIVAL_THRESHOLD = 50.0; // Consider "arrived" when within 50 units

    public ReturnHomeCommand(Unit unit, Vector2 homePosition) {
        super(unit, false); // Never a player order
        this.homePosition = homePosition.copy();
    }

    @Override
    public boolean update(double deltaTime) {
        if (homePosition == null) {
            return false;
        }

        // Check if we've arrived at home
        Vector2 currentPos = unit.getPosition();
        double distanceFromHome = currentPos.distance(homePosition);

        if (distanceFromHome <= ARRIVAL_THRESHOLD) {
            // Arrived at home, stop
            unit.getBody().setLinearVelocity(0, 0);
            return false;
        }

        // Continue moving home
        return true;
    }

    @Override
    public void updateMovement(double deltaTime) {
        Vector2 currentPos = unit.getPosition();
        double distance = currentPos.distance(homePosition);

        if (distance <= ARRIVAL_THRESHOLD) {
            // Arrived, stop
            unit.getBody().setLinearVelocity(0, 0);
            return;
        }

        // Compute path if needed
        if (path.isEmpty() || lastPathTarget == null || lastPathTarget.distance(homePosition) > 10.0) {
            computePathTo(homePosition);
        }

        // Follow path to home
        followPathTo(homePosition, nearbyUnits(), ARRIVAL_THRESHOLD);
    }

    @Override
    public Vector2 getTargetPosition() {
        return homePosition;
    }

    @Override
    public boolean isMoving() {
        Vector2 currentPos = unit.getPosition();
        double distance = currentPos.distance(homePosition);
        return distance > ARRIVAL_THRESHOLD;
    }

    @Override
    public String getDescription() {
        return String.format("Returning to home position (%.1f, %.1f)", homePosition.x, homePosition.y);
    }
}
