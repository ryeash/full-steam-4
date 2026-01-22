package com.fullsteam.model.weapon;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.BulletEffect;
import com.fullsteam.model.Elevation;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Ordinance;
import com.fullsteam.model.Projectile;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;

import java.util.List;
import java.util.Set;

/**
 * Weapon that fires grenades in an arcing trajectory.
 * Grenades have slower projectile speed and arc over obstacles,
 * dealing AOE damage on impact.
 */
@Getter
@Setter
public class GrenadeWeapon extends Weapon {
    private double projectileSpeed;
    private double linearDamping;
    private double projectileSize;
    private Set<BulletEffect> bulletEffects;

    /**
     * Create a grenade weapon.
     * Grenades always have EXPLOSIVE effect for AOE damage.
     */
    public GrenadeWeapon(double damage,
                         double range,
                         double attackRate,
                         double projectileSpeed,
                         double linearDamping,
                         double projectileSize,
                         ElevationTargeting elevationTargeting) {
        super(damage, range, attackRate, elevationTargeting);
        this.projectileSpeed = projectileSpeed;
        this.linearDamping = linearDamping;
        this.projectileSize = projectileSize;
        // Grenades always have EXPLOSIVE effect
        this.bulletEffects = Set.of(BulletEffect.EXPLOSIVE);
    }

    @Override
    protected List<AbstractOrdinance> createOrdinances(Vector2 position,
                                                       Vector2 targetPosition,
                                                       Elevation targetElevation,
                                                       int ownerId,
                                                       int ownerTeam,
                                                       Body ignoredBody,
                                                       GameEntities gameEntities) {
        // Calculate direction to target
        Vector2 direction = targetPosition.copy()
                .subtract(position)
                .getNormalized();

        // Grenades travel slower than bullets but arc over obstacles
        Vector2 velocity = direction.multiply(projectileSpeed);

        // Create grenade projectile
        Projectile grenade = new Projectile(
                position,
                velocity,
                damage,
                range,
                ownerId,
                ownerTeam,
                linearDamping,
                bulletEffects,
                Ordinance.GRENADE, // Always use grenade ordinance type
                projectileSize,
                elevationTargeting,
                targetElevation,
                null,  // Grenades don't use seeking (ballistic arc)
                0.0,
                0.0
        );

        return List.of(grenade);
    }
}
