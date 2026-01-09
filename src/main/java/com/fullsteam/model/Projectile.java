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
    
    // Seeking missile support
    private Integer targetEntityId;  // ID of target being tracked (null if not seeking)
    private double turnRate;         // Radians per second (how fast missile can turn)
    private double accelerationRate; // Speed increase per second
    private int seekingUpdateCounter = 0; // Update seeking every N frames for performance

    public Projectile(Vector2 origin,
                      Vector2 velocity,
                      double damage,
                      double maxRange,
                      int ownerId,
                      int ownerTeam,
                      double linearDamping,
                      Set<BulletEffect> bulletEffects,
                      Ordinance ordinance,
                      double size,
                      ElevationTargeting elevationTargeting,
                      Elevation currentElevation,
                      Integer targetEntityId,
                      double turnRate,
                      double accelerationRate) {
        super(IdGenerator.nextEntityId(), createProjectileBody(size), ownerId, ownerTeam,
                origin, damage, bulletEffects, ordinance, size, elevationTargeting, currentElevation);

        this.maxRange = maxRange;
        this.targetEntityId = targetEntityId;
        this.turnRate = turnRate;
        this.accelerationRate = accelerationRate;
        
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
        
        // Handle seeking behavior (update every 3 frames for performance)
        if (bulletEffects.contains(BulletEffect.SEEKING) && targetEntityId != null) {
            seekingUpdateCounter++;
            if (seekingUpdateCounter >= 3) {
                updateSeekingBehavior(gameEntities);
                seekingUpdateCounter = 0;
            }
        }
        
        // Deactivate if traveled too far
        if (origin.distance(getPosition()) >= maxRange) {
            active = false;
        }
    }
    
    /**
     * Update seeking missile behavior - adjust velocity to track target
     */
    private void updateSeekingBehavior(GameEntities gameEntities) {
        // 1. Find target entity
        GameEntity target = findTarget(gameEntities);
        
        // 2. If target is dead/invalid, continue on current trajectory
        if (target == null || !target.isActive()) {
            targetEntityId = null;  // Stop seeking
            return;
        }
        
        // 3. Calculate direction to target
        Vector2 currentPos = getPosition();
        Vector2 targetPos = target.getPosition();
        Vector2 desiredDirection = targetPos.copy().subtract(currentPos).getNormalized();
        
        // 4. Get current velocity direction
        Vector2 currentVelocity = body.getLinearVelocity();
        double currentSpeed = currentVelocity.getMagnitude();
        
        // Don't seek if velocity is too low (avoid division by zero)
        if (currentSpeed < 10.0) {
            return;
        }
        
        Vector2 currentDirection = currentVelocity.copy().getNormalized();
        
        // 5. Calculate turn angle (limited by turnRate)
        double desiredAngle = Math.atan2(desiredDirection.y, desiredDirection.x);
        double currentAngle = Math.atan2(currentDirection.y, currentDirection.x);
        double angleDiff = normalizeAngle(desiredAngle - currentAngle);
        
        // 6. Apply turn rate limit (missiles can't turn instantly)
        // Assuming 60 FPS, each update is ~0.0167 seconds, but we update every 3 frames = 0.05 seconds
        double deltaTime = 0.05; // 3 frames at 60 FPS
        double maxTurn = turnRate * deltaTime;
        double actualTurn = Math.max(-maxTurn, Math.min(maxTurn, angleDiff));
        double newAngle = currentAngle + actualTurn;
        
        // 7. Apply acceleration (missiles speed up as they track)
        double newSpeed = currentSpeed + (accelerationRate * deltaTime);
        
        // 8. Set new velocity
        Vector2 newDirection = new Vector2(Math.cos(newAngle), Math.sin(newAngle));
        Vector2 newVelocity = newDirection.multiply(newSpeed);
        body.setLinearVelocity(newVelocity);
    }
    
    /**
     * Find the target entity being tracked
     */
    private GameEntity findTarget(GameEntities gameEntities) {
        // Check units first
        Unit unit = gameEntities.getUnits().get(targetEntityId);
        if (unit != null) {
            return unit;
        }
        
        // Check buildings
        Building building = gameEntities.getBuildings().get(targetEntityId);
        if (building != null) {
            return building;
        }
        
        return null;
    }
    
    /**
     * Normalize angle to [-PI, PI] range
     */
    private double normalizeAngle(double angle) {
        while (angle > Math.PI) {
            angle -= 2 * Math.PI;
        }
        while (angle < -Math.PI) {
            angle += 2 * Math.PI;
        }
        return angle;
    }

    /**
     * Get the rotation angle for rendering (based on physics body velocity)
     */
    public double getRotation() {
        Vector2 velocity = body.getLinearVelocity();
        return Math.atan2(velocity.y, velocity.x);
    }

    /**
     * Compatibility method - delegates to parent's getAffectedEntities()
     */
    public Set<Integer> getAffectedPlayers() {
        return getAffectedEntities();
    }
}

