package com.fullsteam.model;

import com.fullsteam.model.weapon.Weapon;
import com.fullsteam.model.weapon.WeaponFactory;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Represents an individual turret on a multi-turret unit (e.g., deployed Crawler).
 * Each turret can independently target and fire at enemies.
 */
@Getter
@Setter
public class Turret {
    private final int index;
    private final Vector2 offset;
    private final Weapon weapon;

    private Targetable target = null;
    private double rotation = 0.0;

    public Turret(int index, Vector2 offset, double damage, double range, double attackRate) {
        this.index = index;
        this.offset = offset;
        this.weapon = WeaponFactory.getCrawlerTurretWeapon(damage, range, attackRate);
    }

    /**
     * Get the world position of this turret based on parent unit position
     */
    public Vector2 getWorldPosition(Unit parentUnit) {
        return parentUnit.getPosition().add(offset);
    }

    /**
     * Update this turret's targeting and firing logic
     */
    public void update(Unit parentUnit, GameEntities gameEntities) {
        Vector2 worldPosition = getWorldPosition(parentUnit);
        if (target == null || !target.isValidTargetFor(weapon, parentUnit.getTeamNumber(), worldPosition)) {
            target = gameEntities.findNearestEnemyTargetable(worldPosition, parentUnit.getTeamNumber(), weapon);
        }

        // Fire at target if in range and cooldown ready
        if (target != null) {
            List<AbstractOrdinance> ordinances = fireAtTarget(parentUnit, worldPosition, gameEntities);
            for (AbstractOrdinance ordinance : ordinances) {
                gameEntities.add(ordinance);
            }
        }
    }

    /**
     * Fire at the current target
     * Returns the list of ordinances created (may be empty if unable to fire)
     */
    private List<AbstractOrdinance> fireAtTarget(Unit parentUnit, Vector2 turretWorldPos, GameEntities gameEntities) {
        Vector2 targetPos = calculateInterceptPoint(target.getPosition(), target);
        rotation = Math.atan2(
                targetPos.y - turretWorldPos.y,
                targetPos.x - turretWorldPos.x
        );
        // Get research modifiers from parent unit's faction
        return weapon.fire(
                turretWorldPos,
                targetPos,
                target.getElevation(),
                parentUnit.getId(),
                parentUnit.getTeamNumber(),
                parentUnit.getBody(),
                gameEntities,
                parentUnit.getFaction().getResearchManager().getCumulativeModifier()
        );
    }

    /**
     * Calculate intercept point for predictive aiming
     * Note: Projectile speed is now managed by the weapon, so we estimate based on typical values
     */
    private Vector2 calculateInterceptPoint(Vector2 turretPos, Targetable target) {
        Vector2 targetPos = target.getPosition();
        Vector2 targetVelocity = target.getBody().getLinearVelocity();

        // Estimate projectile speed (turrets typically fire at 450 units/sec)
        double projectileSpeed = 450.0;

        // If target is stationary or projectile is very fast, no need to lead
        double targetSpeed = targetVelocity.getMagnitude();
        if (targetSpeed < 1.0) {
            return targetPos.copy();
        }

        // Calculate time to intercept using quadratic formula
        Vector2 toTarget = targetPos.copy().subtract(turretPos);
        double a = targetVelocity.dot(targetVelocity) - (projectileSpeed * projectileSpeed);
        double b = 2 * toTarget.dot(targetVelocity);
        double c = toTarget.dot(toTarget);

        double discriminant = b * b - 4 * a * c;

        // If no solution, aim at current position
        if (discriminant < 0 || Math.abs(a) < 0.001) {
            return targetPos.copy();
        }

        // Use the smaller positive root (earliest intercept time)
        double t1 = (-b + Math.sqrt(discriminant)) / (2 * a);
        double t2 = (-b - Math.sqrt(discriminant)) / (2 * a);

        double interceptTime;
        if (t1 > 0 && t2 > 0) {
            interceptTime = Math.min(t1, t2);
        } else if (t1 > 0) {
            interceptTime = t1;
        } else if (t2 > 0) {
            interceptTime = t2;
        } else {
            // No positive solution, aim at current position
            return targetPos.copy();
        }

        // Clamp intercept time to reasonable values (max 3 seconds ahead)
        interceptTime = Math.min(interceptTime, 3.0);

        // Calculate intercept position
        return targetPos.copy().add(targetVelocity.copy().multiply(interceptTime));
    }
}

