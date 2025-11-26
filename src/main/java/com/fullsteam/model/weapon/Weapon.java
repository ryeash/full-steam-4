package com.fullsteam.model.weapon;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.GameEntities;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;

/**
 * Abstract base class for all weapon types in the game.
 * Encapsulates weapon stats, firing behavior, and cooldown tracking.
 * 
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
    
    // Fire rate tracking
    protected long lastFireTime = 0; // Timestamp of last fire (milliseconds)
    
    public Weapon(double damage, double range, double attackRate) {
        this.damage = damage;
        this.range = range;
        this.attackRate = attackRate;
    }
    
    /**
     * Fire this weapon from a position towards a target.
     * This method handles cooldown tracking automatically.
     * 
     * @param position The firing position
     * @param targetPosition The target position to aim at
     * @param ownerId The ID of the entity firing (for damage attribution)
     * @param ownerTeam The team number of the firing entity
     * @param ignoredBody The physics body to ignore in collision/raycasting (typically the firer)
     * @param gameEntities Access to all game entities and the physics world
     * @return The created ordinance (Projectile, Beam, etc.) or null if unable to fire
     */
    public AbstractOrdinance fire(
        Vector2 position,
        Vector2 targetPosition,
        int ownerId,
        int ownerTeam,
        Body ignoredBody,
        GameEntities gameEntities
    ) {
        // Check if weapon is ready to fire
        if (!canFire()) {
            return null;
        }
        
        // Fire the weapon (implemented by subclass)
        AbstractOrdinance ordinance = createOrdinance(position, targetPosition, ownerId, ownerTeam, ignoredBody, gameEntities);
        
        // Record the fire time if successful
        if (ordinance != null) {
            recordFire();
        }
        
        return ordinance;
    }
    
    /**
     * Create the ordinance for this weapon type.
     * Subclasses implement this to create their specific ordinance (Projectile, Beam, etc.).
     * 
     * @param position The firing position
     * @param targetPosition The target position to aim at
     * @param ownerId The ID of the entity firing (for damage attribution)
     * @param ownerTeam The team number of the firing entity
     * @param ignoredBody The physics body to ignore in collision/raycasting (typically the firer)
     * @param gameEntities Access to all game entities and the physics world
     * @return The created ordinance (Projectile, Beam, etc.) or null if unable to create
     */
    protected abstract AbstractOrdinance createOrdinance(
        Vector2 position,
        Vector2 targetPosition,
        int ownerId,
        int ownerTeam,
        Body ignoredBody,
        GameEntities gameEntities
    );
    
    /**
     * Get the attack cooldown in milliseconds based on attack rate.
     * @return Milliseconds between attacks
     */
    public double getAttackCooldownMs() {
        return 1000.0 / attackRate;
    }
    
    /**
     * Check if this weapon can fire (cooldown has elapsed).
     * @return true if weapon is ready to fire
     */
    public boolean canFire() {
        long now = System.currentTimeMillis();
        return (now - lastFireTime) >= getAttackCooldownMs();
    }
    
    /**
     * Check if this weapon can fire with a custom current time.
     * Useful for testing or when you already have the current time.
     * 
     * @param currentTimeMs Current time in milliseconds
     * @return true if weapon is ready to fire
     */
    public boolean canFire(long currentTimeMs) {
        return (currentTimeMs - lastFireTime) >= getAttackCooldownMs();
    }
    
    /**
     * Mark that this weapon has fired (updates lastFireTime).
     * Should be called after successfully firing.
     */
    public void recordFire() {
        this.lastFireTime = System.currentTimeMillis();
    }
    
    /**
     * Mark that this weapon has fired with a custom timestamp.
     * Useful for testing or when you already have the current time.
     * 
     * @param currentTimeMs Current time in milliseconds
     */
    public void recordFire(long currentTimeMs) {
        this.lastFireTime = currentTimeMs;
    }
    
    /**
     * Get time remaining until weapon can fire again (in milliseconds).
     * @return Milliseconds until ready, or 0 if ready now
     */
    public double getCooldownRemaining() {
        long now = System.currentTimeMillis();
        double elapsed = now - lastFireTime;
        double cooldown = getAttackCooldownMs();
        return Math.max(0, cooldown - elapsed);
    }
    
    /**
     * Reset the fire cooldown (makes weapon immediately ready to fire).
     * Useful for special abilities or testing.
     */
    public void resetCooldown() {
        this.lastFireTime = 0;
    }
    
    /**
     * Apply a damage multiplier (for research upgrades, buffs, etc.)
     */
    public void applyDamageMultiplier(double multiplier) {
        this.damage *= multiplier;
    }
    
    /**
     * Apply a range multiplier (for research upgrades, buffs, etc.)
     */
    public void applyRangeMultiplier(double multiplier) {
        this.range *= multiplier;
    }
    
    /**
     * Apply an attack rate multiplier (for research upgrades, buffs, etc.)
     */
    public void applyAttackRateMultiplier(double multiplier) {
        this.attackRate *= multiplier;
    }
    
    /**
     * Create a copy of this weapon (useful for per-unit weapon instances that can be upgraded)
     */
    public abstract Weapon copy();
}

