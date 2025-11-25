package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.BulletEffect;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Ordinance;
import com.fullsteam.model.Projectile;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

import java.util.HashSet;
import java.util.Set;

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
    private static final double DEFAULT_TURRET_DAMAGE = 25.0;
    private static final double DEFAULT_TURRET_ATTACK_RATE = 2.0; // attacks per second
    private static final double DEFAULT_TURRET_RANGE = 300.0;
    private static final double DEFAULT_PROJECTILE_SPEED = 600.0;

    private Unit targetUnit = null;
    private long lastAttackTime = 0;

    private final double damage;
    private final double attackRate;
    private final double range;
    private final double projectileSpeed;

    /**
     * Create a defense component with default turret stats.
     */
    public DefenseComponent() {
        this(DEFAULT_TURRET_DAMAGE, DEFAULT_TURRET_ATTACK_RATE, DEFAULT_TURRET_RANGE, DEFAULT_PROJECTILE_SPEED);
    }

    /**
     * Create a defense component with custom turret stats.
     *
     * @param damage     Damage per shot
     * @param attackRate Attacks per second
     * @param range      Maximum attack range
     */
    public DefenseComponent(double damage, double attackRate, double range) {
        this(damage, attackRate, range, DEFAULT_PROJECTILE_SPEED);
    }

    /**
     * Create a defense component with fully custom stats.
     *
     * @param damage          Damage per shot
     * @param attackRate      Attacks per second
     * @param range           Maximum attack range
     * @param projectileSpeed Speed of fired projectiles
     */
    public DefenseComponent(double damage, double attackRate, double range, double projectileSpeed) {
        this.damage = damage;
        this.attackRate = attackRate;
        this.range = range;
        this.projectileSpeed = projectileSpeed;
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
            if (distance <= range) {
                // Face target
                Vector2 direction = targetPos.copy().subtract(turretPos);
                building.setRotation(Math.atan2(direction.y, direction.x));

                // Attack if cooldown is ready
                long now = System.currentTimeMillis();
                double attackInterval = 1000.0 / attackRate;
                if (now - lastAttackTime >= attackInterval) {
                    lastAttackTime = now;
                    fireAtTarget(gameEntities, building, turretPos, targetPos);
                }
            } else {
                // Target out of range
                targetUnit = null;
            }
        }
    }

    /**
     * Fire a projectile at the current target.
     *
     * @param building  The building firing
     * @param turretPos Position of the turret
     * @param targetPos Position of the target
     */
    private void fireAtTarget(GameEntities gameEntities, Building building, Vector2 turretPos, Vector2 targetPos) {
        if (targetUnit == null || !targetUnit.isActive()) {
            return;
        }

        Vector2 direction = targetPos.copy().subtract(turretPos);
        direction.normalize();

        // Turrets fire bullets
        Vector2 velocity = direction.multiply(projectileSpeed);

        Set<BulletEffect> effects = new HashSet<>();

        Projectile projectile = new Projectile(
                turretPos.x,
                turretPos.y,
                velocity.x,
                velocity.y,
                damage,
                range,
                building.getId(),
                building.getTeamNumber(),
                0.2, // linear damping
                effects,
                Ordinance.BULLET,
                3.5 // Building turrets fire medium-sized projectiles
        );
        gameEntities.getProjectiles().put(projectile.getId(), projectile);
        gameEntities.getWorld().addBody(projectile.getBody());
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




