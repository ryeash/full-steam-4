package com.fullsteam.model.weapon;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.BulletEffect;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Ordinance;
import com.fullsteam.model.Projectile;
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
                elevationTargeting
        );
        
        return List.of(projectile);
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

