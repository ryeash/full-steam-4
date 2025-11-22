package com.fullsteam.model.command;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Unit;
import com.fullsteam.model.WallSegment;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Command to attack a wall segment
 */
@Getter
public class AttackWallSegmentCommand extends UnitCommand {
    private final WallSegment target;

    public AttackWallSegmentCommand(Unit unit, WallSegment target, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.target = target;
    }

    @Override
    public boolean update(double deltaTime) {
        // Command fails if target is destroyed
        if (target == null || !target.isActive()) {
            return false;
        }
        return true;
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
        double attackRange = unit.getUnitType().getAttackRange();
        double effectiveRange = attackRange + 20.0; // Wall segments have ~8 thickness

        // Move into range if too far
        if (distance > effectiveRange * 0.9) {
            unit.applySteeringForces(targetPos, nearbyUnits, deltaTime);
        } else {
            // In range, stop moving
            unit.getBody().setLinearVelocity(0, 0);
        }
    }

    @Override
    public AbstractOrdinance updateCombat(double deltaTime) {
        if (target == null || !target.isActive()) {
            return null;
        }

        Vector2 currentPos = unit.getPosition();
        Vector2 targetPos = target.getPosition();
        double distance = currentPos.distance(targetPos);
        double effectiveRange = unit.getAttackRange() + 20.0; // Wall segments have ~8 thickness

        // Movement is handled by AttackWallSegmentCommand.updateMovement()
        // This method just does the actual combat

        // Check if in range
        if (distance <= effectiveRange * 0.9) {
            // Stop moving when in range
            unit.getBody().setLinearVelocity(0, 0);

            // Face target
            Vector2 direction = targetPos.copy().subtract(currentPos);
            unit.setRotation(Math.atan2(direction.y, direction.x));

            // Attack if cooldown is ready
            long now = System.currentTimeMillis();
            double attackInterval = 1000.0 / unit.getAttackRate();
            if (now - unit.getLastAttackTime() >= attackInterval) {
                unit.setLastAttackTime(now);
                // Fire projectile or beam at wall segment (world from gameEntities)
                return unit.fireAt(targetPos, gameEntities != null ? gameEntities.getWorld() : null);
            }
        }

        return null;
    }

    @Override
    public Vector2 getTargetPosition() {
        return target != null ? target.getPosition() : null;
    }

    @Override
    public boolean isMoving() {
        if (target == null) return false;

        double distance = unit.getPosition().distance(target.getPosition());
        double attackRange = unit.getUnitType().getAttackRange();
        double effectiveRange = attackRange + 20.0;
        return distance > effectiveRange * 0.9;
    }

    @Override
    public String getDescription() {
        return String.format("Attack wall segment %d", target != null ? target.getId() : -1);
    }
}

