package com.fullsteam.model.weapon;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Elevation;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.research.ResearchModifier;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Abstract base class for all weapon types in the game.
 * Encapsulates weapon stats, firing behavior, and cooldown tracking.
 * <p>
 * This design pattern provides:
 * - Centralized weapon configuration
 * - Reusable weapon definitions
 * - Easy weapon balancing and tuning
 * - Support for weapon upgrades via research
 * - Cleaner entity code (Units/Buildings just hold a Weapon reference)
 * - Built-in fire rate limiting and cooldown management
 */
@Getter
@Setter
public abstract class Weapon {
    protected double damage;
    protected double range;
    protected double attackRate; // Attacks per second
    protected ElevationTargeting elevationTargeting; // Which elevations this weapon can target

    // Fire rate tracking
    protected long lastFireTime = 0; // Timestamp of last fire (milliseconds)

    public Weapon(double damage, double range, double attackRate, ElevationTargeting elevationTargeting) {
        this.damage = damage;
        this.range = range;
        this.attackRate = attackRate;
        this.elevationTargeting = elevationTargeting;
    }

    /**
     * Fire this weapon from a position towards a target with research modifiers applied.
     * This method handles cooldown tracking automatically.
     *
     * @param position       The firing position
     * @param targetPosition The target position to aim at
     * @param ownerId        The ID of the entity firing (for damage attribution)
     * @param ownerTeam      The team number of the firing entity
     * @param ignoredBody    The physics body to ignore in collision/raycasting (typically the firer)
     * @param gameEntities   Access to all game entities and the physics world
     * @param modifier       Research modifiers to apply to damage/range/rate
     * @return List of created ordinance (may be empty if unable to fire)
     */
    public List<AbstractOrdinance> fire(Vector2 position,
                                        Vector2 targetPosition,
                                        Elevation targetElevation,
                                        int ownerId,
                                        int ownerTeam,
                                        Body ignoredBody,
                                        GameEntities gameEntities,
                                        ResearchModifier modifier) {
        // Check if weapon is ready to fire (with research-modified attack rate)
        if (!canFire(modifier)) {
            return List.of();
        }

        // Fire the weapon (implemented by subclass)
        List<AbstractOrdinance> ordinances = createOrdinances(
            position, targetPosition, targetElevation, 
            ownerId, ownerTeam, ignoredBody, gameEntities, modifier
        );

        // Record the fire time if successful
        if (!ordinances.isEmpty()) {
            recordFire();
        }

        return ordinances;
    }

    /**
     * Create the ordinances for this weapon type with research modifiers applied.
     * Subclasses implement this to create their specific ordinance(s) (Projectile, Beam, etc.).
     * Most weapons return a single-element list, but some (like multi-barrel weapons) return multiple.
     *
     * @param position       The firing position
     * @param targetPosition The target position to aim at
     * @param ownerId        The ID of the entity firing (for damage attribution)
     * @param ownerTeam      The team number of the firing entity
     * @param ignoredBody    The physics body to ignore in collision/raycasting (typically the firer)
     * @param gameEntities   Access to all game entities and the physics world
     * @param modifier       Research modifiers to apply to damage/range/rate
     * @return List of created ordinances (may be empty if unable to create)
     */
    protected abstract List<AbstractOrdinance> createOrdinances(
            Vector2 position,
            Vector2 targetPosition,
            Elevation targetElevation,
            int ownerId,
            int ownerTeam,
            Body ignoredBody,
            GameEntities gameEntities,
            ResearchModifier modifier
    );

    /**
     * Get the attack cooldown in milliseconds based on attack rate with research modifiers.
     *
     * @param modifier Research modifier for attack rate
     * @return Milliseconds between attacks
     */
    public double getAttackCooldownMs(ResearchModifier modifier) {
        double effectiveAttackRate = attackRate * modifier.getAttackRateMultiplier();
        return 1000.0 / effectiveAttackRate;
    }

    /**
     * Check if this weapon can fire (cooldown has elapsed) with research modifiers.
     *
     * @param modifier Research modifier for attack rate
     * @return true if weapon is ready to fire
     */
    public boolean canFire(ResearchModifier modifier) {
        long now = System.currentTimeMillis();
        return (now - lastFireTime) >= getAttackCooldownMs(modifier);
    }

    /**
     * Get effective range with research modifiers applied.
     *
     * @param modifier Research modifier for range
     * @return Effective weapon range
     */
    public double getEffectiveRange(ResearchModifier modifier) {
        return range * modifier.getAttackRangeMultiplier();
    }

    /**
     * Mark that this weapon has fired (updates lastFireTime).
     * Should be called after successfully firing.
     */
    public void recordFire() {
        this.lastFireTime = System.currentTimeMillis();
    }

    /**
     * Create a copy of this weapon (useful for sharing weapon definitions)
     */
    public abstract Weapon copy();
}

