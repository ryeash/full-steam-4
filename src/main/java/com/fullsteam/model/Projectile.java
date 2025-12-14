package com.fullsteam.model;

import com.fullsteam.games.IdGenerator;
import com.fullsteam.model.weapon.ElevationTargeting;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

import java.util.Set;

/**
 * Represents a ballistic projectile fired by units or buildings in the RTS game.
 * Projectiles travel over time and are affected by physics.
 * Position and velocity are managed by the physics body (single source of truth).
 */
@Getter
@Setter
public class Projectile extends AbstractOrdinance {
    private double maxRange;

    public Projectile(Vector2 origin, Vector2 velocity,
                      double damage, double maxRange, int ownerId, int ownerTeam,
                      double linearDamping, Set<BulletEffect> bulletEffects,
                      Ordinance ordinance, double size, ElevationTargeting elevationTargeting,
                      Elevation currentElevation) {
        super(IdGenerator.nextEntityId(), createProjectileBody(size), ownerId, ownerTeam,
                origin, damage, bulletEffects, ordinance, size, elevationTargeting, currentElevation);

        this.maxRange = maxRange;
        body.translate(origin);
        body.setLinearDamping(linearDamping);
        body.setLinearVelocity(velocity); // Set physics body velocity
    }

    private static Body createProjectileBody(double size) {
        Body body = new Body();
        body.addFixture(Geometry.createCircle(size));
        body.setMass(MassType.NORMAL);
        body.setBullet(true);
        return body;
    }

    /**
     * Update projectile state and check if it should be deactivated
     */
    @Override
    public void update(GameEntities gameEntities) {
        if (!active) {
            return;
        }
        // Deactivate if traveled too far
        if (origin.distance(getPosition()) >= maxRange) {
            active = false;
        }
    }

    /**
     * Get the rotation angle for rendering (based on physics body velocity)
     */
    public double getRotation() {
        Vector2 velocity = body.getLinearVelocity();
        return Math.atan2(velocity.y, velocity.x);
    }

    /**
     * Compatibility method - delegates to parent's getOrdinanceType()
     */
    public Ordinance getOrdinance() {
        return getOrdinanceType();
    }

    /**
     * Compatibility method - delegates to parent's getAffectedEntities()
     */
    public Set<Integer> getAffectedPlayers() {
        return getAffectedEntities();
    }
}

