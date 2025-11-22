package com.fullsteam.model;

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
    private double distanceTraveled = 0.0;
    private Vector2 previousPosition; // For tracking distance traveled

    public Projectile(int id, double x, double y, double vx, double vy,
                      double damage, double maxRange, int ownerTeam,
                      double linearDamping, Set<BulletEffect> bulletEffects,
                      Ordinance ordinance, double size) {
        super(id, createProjectileBody(size), id, ownerTeam, 
              new Vector2(x, y), damage, bulletEffects, ordinance, size);
        
        this.maxRange = maxRange;
        this.previousPosition = new Vector2(x, y);
        
        body.translate(x, y);
        body.setLinearDamping(linearDamping);
        body.setLinearVelocity(vx, vy); // Set physics body velocity
    }

    private static Body createProjectileBody(double size) {
        Body body = new Body();
        body.addFixture(Geometry.createCircle(size));
        body.setMass(MassType.NORMAL);
        return body;
    }

    /**
     * Update projectile state and check if it should be deactivated
     */
    @Override
    public void update(double deltaTime) {
        if (!active) {
            return;
        }

        // Track distance traveled using physics body position (single source of truth)
        Vector2 currentPosition = body.getWorldCenter();
        distanceTraveled += currentPosition.distance(previousPosition);
        previousPosition = currentPosition.copy();

        // Deactivate if traveled too far
        if (distanceTraveled >= maxRange) {
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

