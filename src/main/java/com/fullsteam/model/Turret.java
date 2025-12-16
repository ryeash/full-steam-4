package com.fullsteam.model;

import com.fullsteam.model.weapon.Weapon;
import com.fullsteam.model.weapon.WeaponFactory;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Represents an individual turret on a multi-turret unit (e.g., deployed Crawler).
 * Each turret can independently target and fire at enemies.
 */
@Getter
@Setter
public class Turret {
    private static final Logger log = LoggerFactory.getLogger(Turret.class);

    private final int index; // Turret index (0-3 for Crawler)
    private final Vector2 offset; // Offset from unit center (relative position)
    private final Weapon weapon; // The weapon this turret fires

    private Unit targetUnit = null;
    private Building targetBuilding = null;
    private double rotation = 0.0; // Turret rotation (independent of unit rotation)
    private long lastTargetAcquisitionFrame = -999; // Track when we last looked for targets

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
    public void update(Unit parentUnit, GameEntities gameEntities, long frameCount) {
        Vector2 turretWorldPos = getWorldPosition(parentUnit);
        double turretRange = weapon.getRange();
        double attackRate = weapon.getAttackRate();

        // Clear invalid targets
        if (targetUnit != null && !targetUnit.isActive()) {
            clearTarget();
        }
        if (targetBuilding != null && !targetBuilding.isActive()) {
            clearTarget();
        }

        // Acquire new target if needed (stagger by turret index to avoid all turrets checking on same frame)
        if (!hasTarget() && (frameCount + index * 7L) % 30 == 0) {
            acquireTarget(parentUnit, turretWorldPos, gameEntities, turretRange, frameCount);
        }

        // Fire at target if in range and cooldown ready
        if (hasTarget() && weapon.canFire()) {
            List<AbstractOrdinance> ordinances = fireAtTarget(parentUnit, turretWorldPos, gameEntities);
            for (AbstractOrdinance ordinance : ordinances) {
                gameEntities.add(ordinance);
            }
        }
    }

    /**
     * Acquire a target for this turret
     */
    private void acquireTarget(Unit parentUnit, Vector2 turretWorldPos, GameEntities gameEntities, double range, long frameCount) {
        Unit nearestEnemyUnit = null;
        double nearestUnitDistance = Double.MAX_VALUE;

        // Find nearest enemy unit in range
        for (Unit enemyUnit : gameEntities.getUnits().values()) {
            if (!enemyUnit.isActive() || enemyUnit.getTeamNumber() == parentUnit.getTeamNumber()) {
                continue;
            }

            // Check if weapon can target this unit's elevation
            if (!Unit.canWeaponTargetUnit(weapon, enemyUnit)) {
                continue; // Weapon cannot hit this elevation level
            }

            double distance = turretWorldPos.distance(enemyUnit.getPosition());

            // Cloaked units can only be targeted within cloak detection range
            if (enemyUnit.isCloaked() && distance > Unit.getCloakDetectionRange()) {
                continue;
            }

            if (distance <= range && distance < nearestUnitDistance) {
                nearestEnemyUnit = enemyUnit;
                nearestUnitDistance = distance;
            }
        }

        // If found enemy unit, target it
        if (nearestEnemyUnit != null) {
            targetUnit = nearestEnemyUnit;
            targetBuilding = null;
            lastTargetAcquisitionFrame = frameCount;
            log.debug("Turret {} acquired unit target {} at distance {}",
                    index, nearestEnemyUnit.getId(), nearestUnitDistance);
            return;
        }

        // Otherwise, find nearest enemy building
        Building nearestEnemyBuilding = null;
        double nearestBuildingDistance = Double.MAX_VALUE;

        for (Building building : gameEntities.getBuildings().values()) {
            if (!building.isActive() || building.getTeamNumber() == parentUnit.getTeamNumber()) {
                continue;
            }

            // Check if weapon can target buildings (GROUND elevation)
            if (!Unit.canWeaponTargetBuildings(weapon)) {
                continue; // Weapon cannot hit ground targets (e.g., FLAK_TANK with LOW_AND_HIGH targeting)
            }

            double distance = turretWorldPos.distance(building.getPosition());
            if (distance <= range && distance < nearestBuildingDistance) {
                nearestEnemyBuilding = building;
                nearestBuildingDistance = distance;
            }
        }

        if (nearestEnemyBuilding != null) {
            targetBuilding = nearestEnemyBuilding;
            targetUnit = null;
            lastTargetAcquisitionFrame = frameCount;
            log.debug("Turret {} acquired building target {} at distance {}",
                    index, nearestEnemyBuilding.getId(), nearestBuildingDistance);
        } else {
            log.debug("Turret {} found no targets in range {}", index, range);
        }
    }

    /**
     * Fire at the current target
     * Returns the list of ordinances created (may be empty if unable to fire)
     */
    private List<AbstractOrdinance> fireAtTarget(Unit parentUnit, Vector2 turretWorldPos, GameEntities gameEntities) {
        Vector2 targetPos = null;

        // Use predictive aiming for moving units
        if (targetUnit != null) {
            targetPos = calculateInterceptPoint(turretWorldPos, targetUnit);
        } else if (targetBuilding != null) {
            targetPos = targetBuilding.getPosition();
        }

        if (targetPos == null) {
            return List.of();
        }

        double distance = turretWorldPos.distance(targetPos);
        double turretRange = weapon.getRange();

        if (distance > turretRange) {
            // Target out of range, clear it
            clearTarget();
            return List.of();
        }

        // Calculate turret rotation to face target
        rotation = Math.atan2(
                targetPos.y - turretWorldPos.y,
                targetPos.x - turretWorldPos.x
        );

        // Fire weapon from turret position (weapon handles cooldown tracking)
        List<AbstractOrdinance> ordinances = weapon.fire(
                turretWorldPos,
                targetPos,
                parentUnit.getId(),
                parentUnit.getTeamNumber(),
                parentUnit.getBody(),
                gameEntities
        );

        if (!ordinances.isEmpty()) {
            log.debug("Turret {} fired {} ordinance(s) at target (distance: {})",
                    index, ordinances.size(), distance);
        }

        return ordinances;
    }

    /**
     * Calculate intercept point for predictive aiming
     * Note: Projectile speed is now managed by the weapon, so we estimate based on typical values
     */
    private Vector2 calculateInterceptPoint(Vector2 turretPos, Unit target) {
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


    /**
     * Clear the current target
     */
    public void clearTarget() {
        this.targetUnit = null;
        this.targetBuilding = null;
    }

    /**
     * Check if turret has a valid target
     */
    public boolean hasTarget() {
        return (targetUnit != null && targetUnit.isActive())
                || (targetBuilding != null && targetBuilding.isActive());
    }
}

