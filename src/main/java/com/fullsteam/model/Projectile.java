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
 */
@Getter
@Setter
public class Projectile extends AbstractOrdinance {
    private Vector2 position;
    private Vector2 velocity;
    private double maxRange;
    private double linearDamping;
    private double distanceTraveled = 0.0;

    public Projectile(int id, double x, double y, double vx, double vy,
                      double damage, double maxRange, int ownerTeam,
                      double linearDamping, Set<BulletEffect> bulletEffects,
                      Ordinance ordinance, double size) {
        super(id, createProjectileBody(size), id, ownerTeam, 
              new Vector2(x, y), damage, bulletEffects, ordinance, size);
        
        this.position = new Vector2(x, y);
        this.velocity = new Vector2(vx, vy);
        this.maxRange = maxRange;
        this.linearDamping = linearDamping;
        
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
     * Update projectile position and check if it should be deactivated
     */
    @Override
    public void update(double deltaTime) {
        if (!active) {
            return;
        }

        // Apply damping to velocity
        double dampingFactor = Math.pow(1.0 - linearDamping, deltaTime);
        velocity = velocity.product(dampingFactor);

        // Update position
        Vector2 movement = velocity.product(deltaTime);
        position = position.sum(movement);
        distanceTraveled += movement.getMagnitude();

        // Deactivate if traveled too far
        if (distanceTraveled >= maxRange) {
            active = false;
        }
    }

    /**
     * Get the angle of the projectile's velocity
     */
    public double getAngle() {
        return Math.atan2(velocity.y, velocity.x);
    }
    
    /**
     * Get the rotation angle for rendering
     */
    public double getRotation() {
        return getAngle();
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

