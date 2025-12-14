package com.fullsteam.model.weapon;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.BulletEffect;
import com.fullsteam.model.Elevation;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Ordinance;
import com.fullsteam.model.Projectile;
import com.fullsteam.model.Unit;
import com.fullsteam.model.research.ResearchModifier;
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
                                                       int ownerId,
                                                       int ownerTeam,
                                                       Body ignoredBody,
                                                       GameEntities gameEntities) {
        // Calculate direction to target
        Vector2 direction = targetPosition.copy().subtract(position);
        direction.normalize();

        // Calculate velocity
        Vector2 velocity = direction.multiply(projectileSpeed);

        // Determine the elevation for this projectile based on what we're targeting
        // Check what's at the target position to determine proper elevation
        Elevation ordinanceElevation = determineOrdinanceElevation(targetPosition, gameEntities);

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
                ordinanceElevation
        );
        
        return List.of(projectile);
    }
    
    /**
     * Determine what elevation the ordinance should fly at based on the target position.
     * This allows projectiles fired at aircraft to fly at aircraft elevation and not collide with ground obstacles.
     */
    private Elevation determineOrdinanceElevation(Vector2 targetPosition, GameEntities gameEntities) {
        // Check if we're targeting an airborne unit
        double searchRadius = 50.0; // Search for units near the target position
        
        for (Unit unit : gameEntities.getUnits().values()) {
            if (!unit.isActive()) continue;
            
            double distance = unit.getPosition().distance(targetPosition);
            if (distance < searchRadius) {
                // Found a unit near target - use its elevation
                Elevation targetElevation = unit.getUnitType().getElevation();
                if (targetElevation.isAirborne() && elevationTargeting.canTarget(targetElevation)) {
                    // Targeting an airborne unit - projectile flies at that elevation
                    return targetElevation;
                }
            }
        }
        
        // Default to GROUND elevation (for hitting ground units, buildings, obstacles)
        return Elevation.GROUND;
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

    @Override
    public Weapon copyWithModifiers(ResearchModifier modifier) {
        return new ProjectileWeapon(
                damage * modifier.getBeamDamageMultiplier(),
                range * modifier.getAttackRangeMultiplier(),
                attackRate * modifier.getAttackRateMultiplier(),
                projectileSpeed,
                linearDamping,
                projectileSize,
                ordinanceType,
                Set.copyOf(bulletEffects),
                elevationTargeting
        );
    }
}

