package com.fullsteam.model.component;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Building;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Unit;
import com.fullsteam.model.weapon.Weapon;
import com.fullsteam.model.weapon.WeaponFactory;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

/**
 * Component that handles defensive turret behavior for buildings.
 * Defensive buildings can target and attack enemy units within range.
 * Target acquisition is handled externally (by RTSGameManager),
 * this component handles rotation, firing, and cooldown management.
 * <p>
 * Used by: TURRET
 */
@Getter
@Setter
public class DefenseComponent implements IBuildingComponent {
    private static final double DEFAULT_TURRET_RANGE = 300.0;

    private Unit targetUnit = null;

    private final Weapon weapon;
    private final double range; // Kept for backward compatibility

    /**
     * Create a defense component with default turret stats.
     */
    public DefenseComponent() {
        this.weapon = WeaponFactory.getTurretWeapon();
        this.range = weapon.getRange();
    }

    /**
     * Create a defense component with custom turret stats.
     *
     * @param damage     Damage per shot
     * @param attackRate Attacks per second
     * @param range      Maximum attack range
     */
    public DefenseComponent(double damage, double attackRate, double range) {
        this.weapon = WeaponFactory.getTurretWeapon();
        // Override with custom stats
        weapon.setDamage(damage);
        weapon.setAttackRate(attackRate);
        weapon.setRange(range);
        this.range = range;
    }

    @Override
    public void update(GameEntities gameEntities, Building building, boolean hasLowPower) {
        if (!building.isActive()) {
            return;
        }
        acquireTurretTarget(gameEntities, building);

        // Target acquisition is handled by RTSGameManager
        if (targetUnit != null && targetUnit.isActive()) {
            Vector2 turretPos = building.getPosition();
            Vector2 targetPos = targetUnit.getPosition();
            double distance = turretPos.distance(targetPos);

            // Check if target is in range
            if (distance <= weapon.getRange()) {
                // Face target
                Vector2 direction = targetPos.copy().subtract(turretPos);
                building.setRotation(Math.atan2(direction.y, direction.x));

                // Attack if cooldown is ready (weapon handles cooldown tracking)
                if (weapon.canFire()) {
                    fireAtTarget(gameEntities, building, turretPos, targetPos);
                }
            } else {
                // Target out of range
                targetUnit = null;
            }
        }
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
            // Add projectile to game world (turrets fire projectiles, not beams)
            if (ordinance instanceof com.fullsteam.model.Projectile projectile) {
                gameEntities.getProjectiles().put(projectile.getId(), projectile);
                gameEntities.getWorld().addBody(projectile.getBody());
            }
        }
    }

    private void acquireTurretTarget(GameEntities gameEntities, Building turret) {
        if (targetUnit != null
                && targetUnit.isActive()
                && !targetUnit.isCloaked()
                && targetUnit.getPosition().distance(turret.getPosition()) < DEFAULT_TURRET_RANGE) {
            // we have an active target, in range, and not cloaked
            return;
        }
        Vector2 turretPos = turret.getPosition();

        // Find nearest enemy unit
        Unit nearestEnemy = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Unit unit : gameEntities.getUnits().values()) {
            if (unit.isActive() && unit.getTeamNumber() != turret.getTeamNumber()) {
                double distance = turretPos.distance(unit.getPosition());
                if (distance <= range && distance < nearestDistance) {
                    nearestEnemy = unit;
                    nearestDistance = distance;
                }
            }
        }
        turret.setTargetUnit(nearestEnemy);
    }
}




