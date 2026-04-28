package com.fullsteam.model;

import com.fullsteam.games.IdGenerator;
import com.fullsteam.model.weapon.ElevationTargeting;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

import java.util.Optional;
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

        // drop dead time as a fallback for distance traveled limit failures
        setExpires(System.currentTimeMillis() + 5000);
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

        if (bulletEffects.contains(BulletEffect.SEEKING) && targetEntityId != null) {
            updateSeekingBehavior(gameEntities);
        }

        // Deactivate if traveled too far
        if (origin.distance(getPosition()) >= maxRange) {
            active = false;
        }
    }

    /**
     * Update seeking missile behavior using {@link Body#applyForce(Vector2)} so dyn4j integrates
     * acceleration with the world's timestep (see {@code RTSGameManager} after entity updates).
     * <p>
     * Lateral acceleration magnitude {@code |sin θ| · speed · turnRate} matches a turn rate
     * bounded by {@code turnRate} rad/s at constant speed for small heading error θ; forward
     * thrust applies {@code accelerationRate} along the current velocity.
     */
    private void updateSeekingBehavior(GameEntities gameEntities) {
        GameEntity target = findTarget(gameEntities);

        if (target == null || !target.isActive()) {
            targetEntityId = null;
            return;
        }

        Vector2 currentPos = getPosition();
        Vector2 targetPos = target.getPosition();
        Vector2 desiredDirection = targetPos.copy().subtract(currentPos).getNormalized();

        Vector2 velocity = body.getLinearVelocity();
        double speed = velocity.getMagnitude();
        double mass = body.getMass().getMass();

        if (speed < 10.0) {
            body.applyForce(desiredDirection.multiply(mass * accelerationRate));
            return;
        }

        Vector2 forward = velocity.copy().getNormalized();
        double sinAngle = forward.cross(desiredDirection);
        double cosAngle = forward.dot(desiredDirection);

        double lateralAccelMag = Math.abs(sinAngle) * speed * turnRate;
        Vector2 lateralDir = new Vector2(-forward.y, forward.x);
        if (Math.abs(sinAngle) < 1e-6 && cosAngle < -0.99) {
            lateralAccelMag = speed * turnRate;
        } else if (sinAngle < 0) {
            lateralDir.negate();
        }
        if (lateralAccelMag > 1e-8) {
            lateralDir.normalize();
            body.applyForce(lateralDir.multiply(mass * lateralAccelMag));
        }

        body.applyForce(forward.multiply(mass * accelerationRate));
    }

    /**
     * Find the target entity being tracked
     */
    private GameEntity findTarget(GameEntities gameEntities) {
        // Check units first
        return Optional.<GameEntity>ofNullable(gameEntities.getUnits().get(targetEntityId))
                .orElse(gameEntities.getBuildings().get(targetEntityId));
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

