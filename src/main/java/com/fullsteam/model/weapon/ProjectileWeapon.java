package com.fullsteam.model.weapon;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Building;
import com.fullsteam.model.BulletEffect;
import com.fullsteam.model.Elevation;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.GameEntity;
import com.fullsteam.model.Ordinance;
import com.fullsteam.model.Projectile;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;

import java.util.List;
import java.util.Set;

/**
 * Weapon that fires ballistic projectiles.
 * Projectiles travel over time, are affected by physics (damping), and have a max range.
 */
@Getter
@Setter
public class ProjectileWeapon extends Weapon {
    private double projectileSpeed;
    private double linearDamping; // Physics damping (air resistance)
    private double projectileSize; // Visual/collision size
    private Ordinance ordinanceType; // Visual type (BULLET, ROCKET, SHELL, etc.)
    private Set<BulletEffect> bulletEffects; // Special effects (EXPLOSIVE, PIERCING, etc.)

    /**
     * Create a projectile weapon with full configuration.
     */
    public ProjectileWeapon(double damage,
                            double range,
                            double attackRate,
                            double projectileSpeed,
                            double linearDamping,
                            double projectileSize,
                            Ordinance ordinanceType,
                            Set<BulletEffect> bulletEffects,
                            ElevationTargeting elevationTargeting) {
        super(damage, range, attackRate, elevationTargeting);
        this.projectileSpeed = projectileSpeed;
        this.linearDamping = linearDamping;
        this.projectileSize = projectileSize;
        this.ordinanceType = ordinanceType;
        this.bulletEffects = bulletEffects != null ? Set.copyOf(bulletEffects) : Set.of();
    }

    @Override
    protected List<AbstractOrdinance> createOrdinances(Vector2 position,
                                                       Vector2 targetPosition,
                                                       Elevation targetElevation,
                                                       int ownerId,
                                                       int ownerTeam,
                                                       Body ignoredBody,
                                                       GameEntities gameEntities) {
        Vector2 velocity = targetPosition.copy()
                .subtract(position)
                .getNormalized()
                .multiply(projectileSpeed);

        // Determine seeking missile parameters
        Integer targetEntityId = null;
        double turnRate = 0.0;
        double accelerationRate = 0.0;
        
        if (bulletEffects.contains(BulletEffect.SEEKING)) {
            // Find closest enemy entity near target position for tracking
            targetEntityId = findTargetEntityNear(targetPosition, ownerId, ownerTeam, gameEntities, ignoredBody);
            
            // Set seeking parameters based on ordinance type
            if (ordinanceType == Ordinance.ROCKET) {
                // SAM Launcher or Interceptor missiles
                turnRate = 3.5;        // 3.5 radians/sec (~200°/sec)
                accelerationRate = 80.0; // Accelerate while tracking
            }
        }

        // Create and return projectile in a list (single projectile for standard weapons)
        Projectile projectile = new Projectile(
                position,
                velocity,
                damage,
                range,
                ownerId,
                ownerTeam,
                linearDamping,
                bulletEffects,
                ordinanceType,
                projectileSize,
                elevationTargeting,
                targetElevation,
                targetEntityId,
                turnRate,
                accelerationRate
        );

        return List.of(projectile);
    }
    
    /**
     * Find the closest enemy entity near the target position for seeking missiles.
     * Prioritizes units over buildings for anti-air missiles.
     */
    private Integer findTargetEntityNear(Vector2 targetPosition, int ownerId, int ownerTeam,
                                         GameEntities gameEntities, Body ignoredBody) {
        double searchRadius = 60.0; // Lock onto targets within 60 pixels of aim point
        GameEntity closestTarget = null;
        double closestDistance = Double.MAX_VALUE;
        
        // Check units first (priority for AA missiles)
        for (Unit unit : gameEntities.getUnits().values()) {
            if (unit.getTeamNumber() != ownerTeam && unit.isActive() && !unit.isGarrisoned()) {
                // Skip if this is the ignored body (shooter)
                if (ignoredBody != null && unit.getBody() == ignoredBody) {
                    continue;
                }
                
                double dist = unit.getPosition().distance(targetPosition);
                if (dist < searchRadius && dist < closestDistance) {
                    closestDistance = dist;
                    closestTarget = unit;
                }
            }
        }
        
        // Check buildings (lower priority for AA missiles, but still valid targets)
        if (closestTarget == null) {
            for (Building building : gameEntities.getBuildings().values()) {
                if (building.getTeamNumber() != ownerTeam && building.isActive() && !building.isUnderConstruction()) {
                    double dist = building.getPosition().distance(targetPosition);
                    if (dist < searchRadius && dist < closestDistance) {
                        closestDistance = dist;
                        closestTarget = building;
                    }
                }
            }
        }
        
        return closestTarget != null ? closestTarget.getId() : null;
    }

    @Override
    public Weapon copy() {
        return new ProjectileWeapon(
                damage,
                range,
                attackRate,
                projectileSpeed,
                linearDamping,
                projectileSize,
                ordinanceType,
                Set.copyOf(bulletEffects),
                elevationTargeting
        );
    }
}

