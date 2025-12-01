package com.fullsteam.model.component;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Building;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Unit;
import com.fullsteam.model.research.ResearchModifier;
import com.fullsteam.model.weapon.Weapon;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

@Getter
@Setter
public class DefenseComponent extends AbstractBuildingComponent {
    private Weapon weapon;
    private Unit targetUnit = null;

    public DefenseComponent(Weapon weapon) {
        this.weapon = weapon;
    }

    @Override
    public void update(boolean hasLowPower) {
        if (!building.isActive() || hasLowPower) {
            return;
        }
        acquireTurretTarget(gameEntities, building);

        if (targetUnit != null && targetUnit.isActive()) {
            Vector2 turretPos = building.getPosition();
            Vector2 targetPos = targetUnit.getPosition();
            double distance = turretPos.distance(targetPos);

            // Check if target is in range
            if (distance <= weapon.getRange()) {
                Vector2 direction = targetPos.copy().subtract(turretPos);
                building.setRotation(Math.atan2(direction.y, direction.x));
                fireAtTarget(gameEntities, building, turretPos, targetPos);
            } else {
                // Target out of range
                targetUnit = null;
            }
        }
    }

    @Override
    public void applyResearchModifiers(ResearchModifier modifier) {
        this.weapon = weapon.copyWithModifiers(modifier);
    }

    /**
     * Fire ordinance at the current target using the weapon system.
     *
     * @param building  The building firing
     * @param turretPos Position of the turret
     * @param targetPos Position of the target
     */
    private void fireAtTarget(GameEntities gameEntities, Building building, Vector2 turretPos, Vector2 targetPos) {
        if (targetUnit == null || !targetUnit.isActive()) {
            return;
        }

        // Fire weapon
        AbstractOrdinance ordinance = weapon.fire(
                turretPos,
                targetPos,
                building.getId(),
                building.getTeamNumber(),
                building.getBody(),
                gameEntities
        );

        if (ordinance != null) {
            // Add ordinance to game world (defensive buildings can fire projectiles or beams)
            gameEntities.add(ordinance);
        }
    }

    private void acquireTurretTarget(GameEntities gameEntities, Building turret) {
        if (targetUnit != null
                && targetUnit.isActive()
                && !targetUnit.isCloaked()
                && targetUnit.getPosition().distance(turret.getPosition()) < weapon.getRange()) {
            // we have an active target, in range, and not cloaked
            return;
        }
        Vector2 turretPos = turret.getPosition();

        // Find nearest enemy unit
        Unit nearestEnemy = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Unit unit : gameEntities.getUnits().values()) {
            double distance = turretPos.distance(unit.getPosition());
            boolean targetable = unit.isActive()
                    && unit.getTeamNumber() != turret.getTeamNumber()
                    && (!unit.isCloaked() || (unit.isCloaked() && distance < Unit.getCloakDetectionRange()));
            if (targetable && distance <= weapon.getRange() && distance < nearestDistance) {
                nearestEnemy = unit;
                nearestDistance = distance;
            }
        }
        targetUnit = nearestEnemy;
    }
}




