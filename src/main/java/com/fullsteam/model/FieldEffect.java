package com.fullsteam.model;

import com.fullsteam.games.IdGenerator;
import lombok.Getter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a temporary field effect in the game world (explosions, fire, electric fields, etc.)
 * Now uses physics bodies with sensor fixtures for optimized collision detection.
 */
@Getter
public class FieldEffect extends GameEntity {
    private final int ownerId;
    private final FieldEffectType type;
    private double radius; // Non-final to support growing effects
    private final double initialRadius;
    private final double maxRadius;
    private final double damage;
    private final int ownerTeam;
    private final long armingTime;
    private final Set<Integer> affectedEntities; // Track which entities have been affected

    public FieldEffect(int ownerId, FieldEffectType type, Vector2 position, double radius, double damage, double duration, int ownerTeam) {
        this(IdGenerator.nextEntityId(), ownerId, type, position, radius, radius, damage, duration, 0, ownerTeam);
    }

    public FieldEffect(int id, int ownerId, FieldEffectType type, Vector2 position, double radius, double maxRadius, double damage, double duration, long armingTime, int ownerTeam) {
        super(id, createFieldEffectBody(position, radius), Double.POSITIVE_INFINITY); // Field effects are indestructible
        this.ownerId = ownerId;
        this.type = type;
        this.initialRadius = radius;
        this.radius = radius;
        this.maxRadius = maxRadius;
        this.damage = damage;
        this.expires = (long) (System.currentTimeMillis() + (duration * 1000)); // duration in seconds
        this.armingTime = armingTime;
        this.ownerTeam = ownerTeam;
        this.affectedEntities = new HashSet<>();
        this.active = true;
        getBody().setUserData(this);
    }

    private static Body createFieldEffectBody(Vector2 position, double radius) {
        Body body = new Body();
        Circle circle = new Circle(radius);
        BodyFixture fixture = body.addFixture(circle);
        fixture.setSensor(true);
        body.setMass(MassType.INFINITE); // Make it static
        body.getTransform().setTranslation(position.x, position.y);
        return body;
    }

    public void update(double deltaTime) {
        if (!isActive()) {
            return;
        }

        if (radius < maxRadius) {
            double oldRadius = radius;
            long elapsed = System.currentTimeMillis() - created;
            long duration = expires - created;
            double progress = elapsed / (double) duration;

            // Grow over first 50% of lifetime, then stabilize
            if (progress < 0.5) {
                radius = initialRadius + (maxRadius - initialRadius) * (progress / 0.5);
            } else {
                radius = maxRadius;
            }

            // Update physics body if radius changed (more frequent updates for smoother growth)
            if (Math.abs(radius - oldRadius) > 0.1) {
                updateBodyRadius(radius);
            }
        }

        if (System.currentTimeMillis() > expires) {
            active = false;
        }
    }

    /**
     * Update the physics body's sensor radius (for growing effects)
     */
    private void updateBodyRadius(double newRadius) {
        Body body = getBody();

        // Remove old fixture
        if (body.getFixtureCount() > 0) {
            body.removeFixture(0);
        }

        // Add new fixture with updated radius
        Circle circle = new Circle(newRadius);
        BodyFixture fixture = body.addFixture(circle);
        fixture.setSensor(true);
    }

    public boolean isInRange(Vector2 targetPosition) {
        return getPosition().distance(targetPosition) <= radius;
    }

    public boolean canAffect(GameEntity entity) {
        if (!entity.isActive()) {
            return false;
        }

        // Team-based damage rules for units
        if (entity instanceof Unit unit) {
            // Can't damage self (though this should be rare for field effects)
            if (unit.getId() == ownerId) {
                return false;
            }

            // Check elevation targeting - can this field effect damage units at this elevation?
            if (!type.canAffectElevation(unit.getUnitType().getElevation())) {
                return false;
            }

            // FFA mode (team 0): Can damage all other units (already checked not self above)
            // Team mode (team > 0): Can only damage units on different teams
            if (ownerTeam == 0 || ownerTeam != unit.getTeamNumber()) {
                return canDamageEntity(entity.getId());
            }

            return false;
        }

        // Team-based damage rules for buildings (buildings are always at ground level)
        if (entity instanceof Building building) {
            // Can't damage buildings owned by the same player
            if (building.getOwnerId() == ownerId) {
                return false;
            }

            // Check if field effect can damage ground-level targets
            if (!type.canAffectElevation(Elevation.GROUND)) {
                return false;
            }

            // FFA mode (team 0): Can damage all other buildings (already checked not same owner above)
            // Team mode (team > 0): Can only damage buildings on different teams
            if (ownerTeam == 0 || building.getTeamNumber() != ownerTeam) {
                return canDamageEntity(entity.getId());
            }

            return false;
        }

        // Other entities (shouldn't happen, but allow damage by default)
        return canDamageEntity(entity.getId());
    }

    /**
     * Check if this entity can be damaged by this field effect
     * Instantaneous effects (EXPLOSION) damage once per entity
     * DOT effects (ELECTRIC, FIRE, POISON) damage every physics frame while entity is in range
     */
    private boolean canDamageEntity(int entityId) {
        // Instantaneous effects only damage once per entity
        if (type.isInstantaneous()) {
            return !affectedEntities.contains(entityId);
        }
        return true;
    }

    public void markAsAffected(GameEntity entity) {
        int entityId = entity.getId();
        // For instantaneous effects, mark entity as affected to prevent re-damage
        if (type.isInstantaneous()) {
            affectedEntities.add(entityId);
        }
    }

    public double getIntensityAtPosition(Vector2 targetPosition) {
        if (!isInRange(targetPosition)) {
            return 0.0;
        }
        return switch (type) {
            // explosions degrade with distance from center
            case EXPLOSION -> {
                double distance = getPosition().distance(targetPosition);
                double intensity = 0.5 + (0.5 * (distance / radius));
                yield Math.max(0.0, intensity);
            }
            // Earthquakes have uniform intensity
            default -> 1.0;
        };
    }

    public double getDamageAtPosition(Vector2 targetPosition) {
        return damage * getIntensityAtPosition(targetPosition);
    }

    public long getDuration() {
        return Math.max(expires - created, 0);
    }

    public long getTimeRemaining() {
        return Math.max(expires - System.currentTimeMillis(), 0);
    }

    public double getProgress() {
        long duration = expires - created;
        long timeRemaining = expires - System.currentTimeMillis();
        return (duration > 0 && timeRemaining > 0)
                ? (double) (duration - timeRemaining) / duration
                : 1.0;
    }

//    /**
//     * Trigger the mine and create an explosion field effect (for proximity mines)
//     */
//    public FieldEffect trigger() {
//        if (type != FieldEffectType.PROXIMITY_MINE || hasTriggered) {
//            return null;
//        }
//
//        hasTriggered = true;
//        active = false;
//
//        // Create explosion field effect
//        return new FieldEffect(
//                getId() + 10000, // Offset ID to avoid conflicts
//                ownerId,
//                FieldEffectType.EXPLOSION,
//                getPosition(),
//                explosionRadius,
//                explosionDamage,
//                FieldEffectType.EXPLOSION.getDefaultDuration(),
//                ownerTeam
//        );
//    }

    /**
     * Check if the mine is armed (for proximity mines)
     */
    public boolean isArmed() {
        return System.currentTimeMillis() > armingTime;
    }
}
