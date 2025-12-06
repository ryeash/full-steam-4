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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Weapon that fires multiple projectiles simultaneously in a spread pattern.
 * Useful for units like COLOSSUS (3 parallel shots) or shotgun-style weapons.
 */
@Getter
@Setter
public class MultiProjectileWeapon extends Weapon {
    private double projectileSpeed;
    private double linearDamping;
    private double projectileSize;
    private Ordinance ordinanceType;
    private Set<BulletEffect> bulletEffects;
    private int projectileCount; // Number of projectiles to fire
    private double spreadDistance; // Distance between parallel projectiles (0 for point source)
    private double spreadAngle; // Angle spread in radians (0 for parallel)

    /**
     * Create a multi-projectile weapon with parallel projectiles (like COLOSSUS).
     * Projectiles fire in a parallel pattern perpendicular to the firing direction.
     */
    public MultiProjectileWeapon(double damage,
                                 double range,
                                 double attackRate,
                                 double projectileSpeed,
                                 double linearDamping,
                                 double projectileSize,
                                 Ordinance ordinanceType,
                                 Set<BulletEffect> bulletEffects,
                                 int projectileCount,
                                 double spreadDistance) {
        super(damage, range, attackRate);
        this.projectileSpeed = projectileSpeed;
        this.linearDamping = linearDamping;
        this.projectileSize = projectileSize;
        this.ordinanceType = ordinanceType;
        this.bulletEffects = bulletEffects != null ? Set.copyOf(bulletEffects) : Set.of();
        this.projectileCount = Math.max(1, projectileCount);
        this.spreadDistance = spreadDistance;
        this.spreadAngle = 0.0; // Parallel by default
    }

    /**
     * Create a multi-projectile weapon with angular spread (like shotgun).
     * Projectiles fire in a cone pattern from the source.
     */
    public MultiProjectileWeapon(double damage,
                                 double range,
                                 double attackRate,
                                 double projectileSpeed,
                                 double linearDamping,
                                 double projectileSize,
                                 Ordinance ordinanceType,
                                 Set<BulletEffect> bulletEffects,
                                 int projectileCount,
                                 double spreadAngle,
                                 boolean isAngularSpread) {
        super(damage, range, attackRate);
        this.projectileSpeed = projectileSpeed;
        this.linearDamping = linearDamping;
        this.projectileSize = projectileSize;
        this.ordinanceType = ordinanceType;
        this.bulletEffects = bulletEffects != null ? Set.copyOf(bulletEffects) : Set.of();
        this.projectileCount = Math.max(1, projectileCount);
        this.spreadDistance = 0.0;
        this.spreadAngle = spreadAngle; // Cone spread
    }

    @Override
    protected List<AbstractOrdinance> createOrdinances(Vector2 position,
                                                       Vector2 targetPosition,
                                                       int ownerId,
                                                       int ownerTeam,
                                                       Body ignoredBody,
                                                       GameEntities gameEntities) {
        List<AbstractOrdinance> ordinances = new ArrayList<>();

        // Calculate direction to target
        Vector2 direction = targetPosition.copy().subtract(position);
        direction.normalize();

        // If parallel spread (like COLOSSUS triple barrels)
        if (spreadDistance > 0) {
            // Calculate perpendicular vector for spreading
            Vector2 perpendicular = new Vector2(-direction.y, direction.x);
            
            // Calculate starting offset for centered spread
            double totalWidth = (projectileCount - 1) * spreadDistance;
            double startOffset = -totalWidth / 2.0;

            for (int i = 0; i < projectileCount; i++) {
                // Calculate offset position for this projectile
                double offset = startOffset + (i * spreadDistance);
                Vector2 spawnPos = position.copy().add(perpendicular.copy().multiply(offset));

                // All projectiles travel in the same direction (parallel)
                Vector2 velocity = direction.copy().multiply(projectileSpeed);

                Projectile projectile = new Projectile(
                        spawnPos,
                        velocity,
                        damage / projectileCount, // Split damage among projectiles
                        range,
                        ownerId,
                        ownerTeam,
                        linearDamping,
                        bulletEffects,
                        ordinanceType,
                        projectileSize
                );
                ordinances.add(projectile);
            }
        }
        // If angular spread (like shotgun)
        else if (spreadAngle > 0) {
            // Calculate angle step between projectiles
            double totalAngle = spreadAngle;
            double angleStep = totalAngle / Math.max(1, projectileCount - 1);
            double startAngle = -totalAngle / 2.0;

            for (int i = 0; i < projectileCount; i++) {
                // Calculate angle for this projectile
                double angle = startAngle + (i * angleStep);
                
                // Rotate the direction vector by the angle
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                Vector2 rotatedDir = new Vector2(
                        direction.x * cos - direction.y * sin,
                        direction.x * sin + direction.y * cos
                );
                rotatedDir.normalize();

                // Calculate velocity for this projectile
                Vector2 velocity = rotatedDir.multiply(projectileSpeed);

                Projectile projectile = new Projectile(
                        position.copy(),
                        velocity,
                        damage / projectileCount, // Split damage among projectiles
                        range,
                        ownerId,
                        ownerTeam,
                        linearDamping,
                        bulletEffects,
                        ordinanceType,
                        projectileSize
                );
                ordinances.add(projectile);
            }
        }
        // Fallback: single projectile
        else {
            Vector2 velocity = direction.multiply(projectileSpeed);
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
                    projectileSize
            );
            ordinances.add(projectile);
        }

        return ordinances;
    }

    @Override
    public Weapon copy() {
        MultiProjectileWeapon copy = new MultiProjectileWeapon(
                damage,
                range,
                attackRate,
                projectileSpeed,
                linearDamping,
                projectileSize,
                ordinanceType,
                Set.copyOf(bulletEffects),
                projectileCount,
                spreadDistance
        );
        copy.spreadAngle = this.spreadAngle;
        return copy;
    }

    @Override
    public Weapon copyWithModifiers(ResearchModifier modifier) {
        MultiProjectileWeapon copy = new MultiProjectileWeapon(
                damage * modifier.getBeamDamageMultiplier(),
                range * modifier.getAttackRangeMultiplier(),
                attackRate * modifier.getAttackRateMultiplier(),
                projectileSpeed,
                linearDamping,
                projectileSize,
                ordinanceType,
                Set.copyOf(bulletEffects),
                projectileCount,
                spreadDistance
        );
        copy.spreadAngle = this.spreadAngle;
        return copy;
    }
}




