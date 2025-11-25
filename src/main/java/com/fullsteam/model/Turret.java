package com.fullsteam.model;

import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an individual turret on a multi-turret unit (e.g., deployed Crawler).
 * Each turret can independently target and fire at enemies.
 */
@Getter
@Setter
public class Turret {
    private static final Logger log = LoggerFactory.getLogger(Turret.class);
    private static final double projectileSpeed = 450.0; // Heavy turret projectile speed

    private final int index; // Turret index (0-3 for Crawler)
    private final Vector2 offset; // Offset from unit center (relative position)

    private Unit targetUnit = null;
    private Building targetBuilding = null;
    private double rotation = 0.0; // Turret rotation (independent of unit rotation)
    private long lastFireTime = 0;
    private long lastTargetAcquisitionFrame = -999; // Track when we last looked for targets

    public Turret(int index, Vector2 offset) {
        this.index = index;
        this.offset = offset;
    }

    /**
     * Get the world position of this turret based on parent unit position
     */
    public Vector2 getWorldPosition(Unit parentUnit) {
        return parentUnit.getPosition().add(offset);
    }

    /**
     * Update this turret's targeting and firing logic
     * Returns a projectile if the turret fired, null otherwise
     */
    public Projectile update(Unit parentUnit, GameEntities gameEntities, long frameCount) {
        Vector2 turretWorldPos = getWorldPosition(parentUnit);
        double turretRange = parentUnit.getAttackRange();
        double attackRate = parentUnit.getAttackRate();

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
        if (hasTarget() && canFire(attackRate)) {
            return fireAtTarget(parentUnit, turretWorldPos, rotation, turretRange);
        }

        return null;
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

            double distance = turretWorldPos.distance(enemyUnit.getPosition());
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
     * Returns the projectile created, or null if unable to fire
     */
    private Projectile fireAtTarget(Unit parentUnit, Vector2 turretWorldPos, double unitRotation, double turretRange) {
        Vector2 targetPos = null;

        // Use predictive aiming for moving units
        if (targetUnit != null) {
            targetPos = calculateInterceptPoint(turretWorldPos, targetUnit);
        } else if (targetBuilding != null) {
            targetPos = targetBuilding.getPosition();
        }

        if (targetPos == null) {
            return null;
        }

        double distance = turretWorldPos.distance(targetPos);

        if (distance > turretRange) {
            // Target out of range, clear it
            clearTarget();
            return null;
        }

        // Calculate turret rotation to face target
        double angleToTarget = Math.atan2(
                targetPos.y - turretWorldPos.y,
                targetPos.x - turretWorldPos.x
        );
        rotation = angleToTarget;

        // Create projectile from turret position
        Projectile projectile = createProjectile(parentUnit, turretWorldPos, angleToTarget);
        this.lastFireTime = System.currentTimeMillis();

        log.debug("Turret {} fired at target (distance: {})", index, distance);

        return projectile;
    }

    /**
     * Calculate intercept point for predictive aiming
     */
    private Vector2 calculateInterceptPoint(Vector2 turretPos, Unit target) {
        Vector2 targetPos = target.getPosition();
        Vector2 targetVelocity = target.getBody().getLinearVelocity();

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
     * Create a projectile fired from this turret
     */
    private Projectile createProjectile(Unit parentUnit, Vector2 turretPos, double angle) {
        // Calculate velocity from angle and speed
        double vx = Math.cos(angle) * projectileSpeed;
        double vy = Math.sin(angle) * projectileSpeed;

        // Use heavy ordinance for Crawler turrets
        Ordinance ordinance = Ordinance.SHELL;

        // Create bullet effects set
        Set<BulletEffect> bulletEffects = new HashSet<>();
        // Turret projectiles are explosive
        bulletEffects.add(BulletEffect.EXPLOSIVE);

        return new Projectile(
                turretPos.x,
                turretPos.y,
                vx,
                vy,
                parentUnit.getDamage(),
                parentUnit.getAttackRange(), // Max range
                parentUnit.getId(),
                parentUnit.getTeamNumber(),
                0.2, // Linear damping
                bulletEffects,
                ordinance,
                5.0 // Large turret shells (same as Crawler projectile size)
        );
    }

    /**
     * Check if this turret can fire (based on attack rate)
     */
    public boolean canFire(double attackRate) {
        long now = System.currentTimeMillis();
        double attackCooldown = 1000.0 / attackRate; // Convert attacks/sec to milliseconds
        return (now - lastFireTime) >= attackCooldown;
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

