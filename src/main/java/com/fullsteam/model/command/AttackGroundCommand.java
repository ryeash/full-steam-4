package com.fullsteam.model.command;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Unit;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Command to attack a specific ground location (force attack - CMD/CTRL + right click)
 * Useful for artillery and area denial
 */
@Getter
public class AttackGroundCommand extends UnitCommand {
    private final Vector2 groundTarget;

    public AttackGroundCommand(Unit unit, Vector2 groundTarget, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.groundTarget = groundTarget.copy();
    }

    @Override
    public boolean update(double deltaTime) {
        // Command stays active until cancelled
        return true;
    }

    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        Vector2 currentPos = unit.getPosition();
        double distance = currentPos.distance(groundTarget);
        double attackRange = unit.getUnitType().getAttackRange();

        // Move into range if too far
        if (distance > attackRange * 0.9) {
            // Compute path if needed (ground target is static)
            if (path.isEmpty() || lastPathTarget == null) {
                computePathTo(groundTarget);
            }
            
            // Follow path to ground target
            followPathTo(groundTarget, nearbyUnits, attackRange * 0.9);
        } else {
            // In range, stop moving
            unit.getBody().setLinearVelocity(0, 0);
        }
    }

    @Override
    public List<AbstractOrdinance> updateCombat(double deltaTime) {
        if (groundTarget == null) {
            return List.of();
        }

        Vector2 currentPos = unit.getPosition();
        double distance = currentPos.distance(groundTarget);

        // Check if in range
        if (distance <= unit.getWeapon().getRange() * 0.9) {
            // Stop moving when in range
            unit.getBody().setLinearVelocity(0, 0);

            // Face target
            Vector2 direction = groundTarget.copy().subtract(currentPos);
            unit.setRotation(Math.atan2(direction.y, direction.x));
            return unit.fireAt(groundTarget, gameEntities);
        }
        return List.of();
    }

    @Override
    public Vector2 getTargetPosition() {
        return groundTarget;
    }

    @Override
    public boolean isMoving() {
        double distance = unit.getPosition().distance(groundTarget);
        double attackRange = unit.getUnitType().getAttackRange();
        return distance > attackRange * 0.9;
    }

    @Override
    public String getDescription() {
        return String.format("Attack ground at (%.1f, %.1f)", groundTarget.x, groundTarget.y);
    }
}

