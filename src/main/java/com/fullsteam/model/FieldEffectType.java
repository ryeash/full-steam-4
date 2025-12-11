package com.fullsteam.model;

import com.fullsteam.model.weapon.ElevationTargeting;
import lombok.Getter;

/**
 * Types of field effects (AOE damage zones) with elevation targeting.
 * 
 * <h3>Elevation System Integration:</h3>
 * Field effects now respect the elevation system, allowing for strategic depth:
 * <ul>
 *   <li><b>GROUND_ONLY</b> - Standard explosions, fire, etc. Only damage ground units and buildings</li>
 *   <li><b>GROUND_AND_LOW</b> - Effects like sandstorms that reach low-altitude aircraft</li>
 *   <li><b>ALL_ELEVATIONS</b> - Flak explosions that can hit high-altitude bombers</li>
 * </ul>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Standard tank shell explosion (ground only)
 * new FieldEffect(ownerId, FieldEffectType.EXPLOSION, position, radius, damage, duration, team);
 * 
 * // Flak cannon burst (hits aircraft at any altitude)
 * new FieldEffect(ownerId, FieldEffectType.FLAK_EXPLOSION, position, radius, damage, duration, team);
 * 
 * // The FieldEffect.canAffect() method automatically checks elevation:
 * if (effect.canAffect(unit)) {
 *     // Will only return true if effect's elevationTargeting can hit unit's elevation
 * }
 * </pre>
 * 
 * <h3>Future Extensions:</h3>
 * <ul>
 *   <li>PLASMA_BURST - Energy weapon AOE for beam weapons</li>
 *   <li>EMP_PULSE - Disables mechanical units, affects all elevations</li>
 *   <li>NAPALM - Fire effect that sticks to ground and low-altitude targets</li>
 * </ul>
 */
@Getter
public enum FieldEffectType {
    // Ground-based combat effects
    EXPLOSION(0.5, true, ElevationTargeting.GROUND_ONLY),      // Short duration, instant damage (ground only)
    FIRE(3.0, false, ElevationTargeting.GROUND_ONLY),          // Long duration, damage over time (ground only)
    ELECTRIC(1.0, false, ElevationTargeting.GROUND_ONLY),      // Medium duration, chain damage (ground only)
    FRAGMENTATION(0.3, true, ElevationTargeting.GROUND_ONLY),  // Very short, creates multiple projectiles (ground only)
    
    // Environmental effects
    SANDSTORM(Double.MAX_VALUE, false, ElevationTargeting.GROUND_AND_LOW), // Persistent, damages ground + low altitude
    
    // Anti-aircraft effects
    FLAK_EXPLOSION(0.4, true, ElevationTargeting.ALL_ELEVATIONS); // Anti-air burst, damages all elevations

    private final double defaultDuration;
    private final boolean instantaneous;
    private final ElevationTargeting elevationTargeting;

    FieldEffectType(double defaultDuration, boolean instantaneous, ElevationTargeting elevationTargeting) {
        this.defaultDuration = defaultDuration;
        this.instantaneous = instantaneous;
        this.elevationTargeting = elevationTargeting;
    }
    
    /**
     * Check if this field effect can damage a unit at the given elevation.
     */
    public boolean canAffectElevation(Elevation elevation) {
        return elevationTargeting.canTarget(elevation);
    }
}
