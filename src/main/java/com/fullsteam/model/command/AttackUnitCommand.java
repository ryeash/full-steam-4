package com.fullsteam.model.command;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Unit;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Command to attack a specific enemy unit
 */
@Getter
public class AttackUnitCommand extends UnitCommand {
    private final Unit target;

    public AttackUnitCommand(Unit unit, Unit target, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.target = target;
    }

    @Override
    public boolean update(double deltaTime) {
        // Command fails if target is dead
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
        double attackRange = unit.getUnitType().getAttackRange();

        // Move into range if too far
        if (distance > attackRange * 0.9) {
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

        // Delegate to unit's engage method with target parameter and gameEntities
        return unit.engageTarget(target, deltaTime, gameEntities);
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
        double attackRange = unit.getUnitType().getAttackRange();
        return distance > attackRange * 0.9;
    }

    @Override
    public String getDescription() {
        return String.format("Attack unit %d", target != null ? target.getId() : -1);
    }
}

