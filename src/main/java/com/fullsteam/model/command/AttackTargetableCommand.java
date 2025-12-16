package com.fullsteam.model.command;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Targetable;
import com.fullsteam.model.Unit;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Unified command to attack any targetable entity (unit, building, wall segment).
 * Replaces AttackUnitCommand, AttackBuildingCommand, and AttackWallSegmentCommand.
 * <p>
 * This command handles:
 * - Pathfinding to the target
 * - Range checking (accounts for target size)
 * - Predictive aiming for moving targets
 * - Combat execution
 */
@Getter
public class AttackTargetableCommand extends UnitCommand {
    private final Targetable target;

    public AttackTargetableCommand(Unit unit, Targetable target, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.target = target;
    }

    @Override
    public boolean update(double deltaTime) {
        // Command fails if target is destroyed
        return target != null && target.isActive();
    }

    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        if (target == null || !target.isActive()) {
            unit.getBody().setLinearVelocity(0, 0);
            return;
        }

        Vector2 currentPos = unit.getPosition();
        Vector2 targetPos = target.getPosition();
        double distance = currentPos.distance(targetPos);

        // Get weapon range - handle units with component-managed weapons (e.g., Gunship)
        double weaponRange;
        if (unit.getWeapon() != null) {
            weaponRange = unit.getWeapon().getRange();
        } else {
            // Fallback to UnitType range for units with component-managed weapons
            weaponRange = unit.getUnitType().getAttackRange();
        }

        // Account for target size (larger targets can be hit from slightly further away)
        double effectiveRange = weaponRange + target.getTargetSize();

        // Move into range if too far
        if (distance > effectiveRange * 0.9) {
            // Recompute path periodically as target moves
            // Static targets (buildings, walls) won't need frequent recomputation
            if (path.isEmpty() || lastPathTarget == null ||
                    lastPathTarget.distance(targetPos) > 50.0) { // Target moved significantly
                computePathTo(targetPos);
            }

            // Follow path to target
            followPathTo(targetPos, nearbyUnits, effectiveRange * 0.9);
        } else {
            // In range, stop moving
            unit.getBody().setLinearVelocity(0, 0);
        }
    }

    @Override
    public List<AbstractOrdinance> updateCombat(double deltaTime) {
        if (target == null || !target.isActive()) {
            return List.of();
        }

        Vector2 currentPos = unit.getPosition();
        Vector2 targetPos = target.getPosition();
        double distance = currentPos.distance(targetPos);

        // Get weapon range
        double weaponRange;
        if (unit.getWeapon() != null) {
            weaponRange = unit.getWeapon().getRange();
        } else {
            weaponRange = unit.getUnitType().getAttackRange();
        }
        double effectiveRange = weaponRange + target.getTargetSize();

        // Check if in range
        if (distance <= effectiveRange * 0.9) {
            // Stop moving when in range
            unit.getBody().setLinearVelocity(0, 0);

            // Face target
            Vector2 direction = targetPos.copy().subtract(currentPos);
            unit.setRotation(Math.atan2(direction.y, direction.x));

            // Attack with predictive aiming for moving targets (units)
            Vector2 aimPosition;
            if (target instanceof Unit targetUnit) {
                // Use predictive aiming for moving targets
                aimPosition = unit.calculateInterceptPoint(targetUnit);
            } else {
                // Stationary targets (buildings, walls) - aim directly
                aimPosition = targetPos;
            }

            return unit.fireAt(aimPosition, gameEntities);
        }

        return List.of();
    }

    @Override
    public Vector2 getTargetPosition() {
        return target != null ? target.getPosition() : null;
    }

    @Override
    public boolean isMoving() {
        if (target == null) {
            return false;
        }

        double distance = unit.getPosition().distance(target.getPosition());
        double weaponRange = unit.getWeapon() != null ?
                unit.getWeapon().getRange() :
                unit.getUnitType().getAttackRange();
        double effectiveRange = weaponRange + target.getTargetSize();
        return distance > effectiveRange * 0.9;
    }

    @Override
    public String getDescription() {
        if (target == null) {
            return "Attack (no target)";
        }
        return String.format("Attack %s %d", target.getTargetType(), target.getId());
    }
}


