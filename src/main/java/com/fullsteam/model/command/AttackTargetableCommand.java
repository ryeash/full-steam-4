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

        double weaponRange;
        if (unit.getWeapon() != null) {
            weaponRange = unit.getWeapon().getRange();
        } else {
            weaponRange = unit.getUnitType().getAttackRange();
        }

        double effectiveRange = weaponRange + target.getTargetSize();

        if (distance > effectiveRange * 0.9) {
            if (path.isEmpty() || lastPathTarget == null || lastPathTarget.distance(targetPos) > 50.0) {
                computePathTo(targetPos);
            }
            followPathTo(targetPos, nearbyUnits, effectiveRange * 0.9);
        } else {
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

            return unit.fireAt(aimPosition, target.getElevation(), gameEntities);
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


