package com.fullsteam.model;

import com.fullsteam.model.weapon.Weapon;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;

/**
 * Interface for entities that can be targeted and damaged by weapons.
 * This includes units, buildings, and wall segments.
 * <p>
 * Provides a common interface for:
 * - Target acquisition (finding nearest enemy)
 * - Damage application
 * - Elevation checking (for air/ground targeting)
 * - Position and team information
 */
public interface Targetable {

    /**
     * Get the unique ID of this targetable entity
     */
    int getId();

    /**
     * Get the physics body of the targetable
     */
    Body getBody();

    /**
     * Get the team number (for friendly fire checks)
     */
    int getTeamNumber();

    /**
     * Get the current position in world space
     */
    Vector2 getPosition();

    /**
     * Get the elevation level (GROUND, LOW, HIGH)
     * Used by weapons to determine if they can hit this target
     */
    Elevation getElevation();

    /**
     * Check if this entity is currently active (alive/functioning)
     */
    boolean isActive();

    /**
     * Apply damage to this entity
     *
     * @param damage Amount of damage to apply
     */
    void takeDamage(double damage);

    /**
     * Get current health
     */
    double getHealth();

    /**
     * Get maximum health
     */
    double getMaxHealth();

    /**
     * Get the size/radius for hit detection and targeting range calculations
     * For units: collision radius
     * For buildings: building size (half-width)
     */
    double getTargetSize();

    /**
     * Get the type of targetable for display/logging purposes
     */
    default String getTargetType() {
        return this.getClass().getSimpleName();
    }

    /**
     * Check if this target is cloaked/invisible (for detection range checks)
     * Default: false (most entities are visible)
     */
    default boolean isCloaked() {
        return false;
    }

    /**
     * Get health as a percentage (0.0 to 1.0)
     * Useful for AI target prioritization
     */
    default double getHealthPercent() {
        return getHealth() / getMaxHealth();
    }

    /**
     * Check if this target is an enemy of the given team
     */
    default boolean isEnemyOf(int teamNumber) {
        return this.getTeamNumber() != teamNumber;
    }

    /**
     * Determine if this targetable is valid for the given {@link Weapon} at the given position.
     *
     * @param forWeapon      the weapon to evaluate
     * @param weaponTeam     the team that the weapon belongs to
     * @param weaponPosition the position of the weapon
     * @return true if the given weapon can target and hit this targetable
     */
    default boolean isValidTargetFor(Weapon forWeapon, int weaponTeam, Vector2 weaponPosition) {
        double distance = getPosition().distance(weaponPosition);
        return isActive()
                && isEnemyOf(weaponTeam)
                && forWeapon.getElevationTargeting().canTarget(getElevation())
                && distance < forWeapon.getRange()
                && (!isCloaked() || (isCloaked() && distance < Unit.getCloakDetectionRange()));
    }
}
