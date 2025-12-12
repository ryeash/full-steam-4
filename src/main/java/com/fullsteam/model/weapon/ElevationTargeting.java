package com.fullsteam.model.weapon;

import com.fullsteam.model.Elevation;
import lombok.Getter;

/**
 * Defines which elevation levels a weapon can target.
 * This creates strategic counters - not all weapons can hit all targets.
 *
 * <p>Design Philosophy:
 * <ul>
 *   <li>Most ground weapons cannot hit high-altitude aircraft</li>
 *   <li>Rockets can hit low-altitude targets (helicopters/VTOLs)</li>
 *   <li>Specialized AA weapons can hit all elevations</li>
 *   <li>Research can upgrade weapon targeting capabilities</li>
 * </ul>
 */
@Getter
public enum ElevationTargeting {
    /**
     * Can only target ground units.
     * Examples: Rifles, cannons, basic projectiles
     */
    GROUND_ONLY("Ground Only", "Can only target ground units"),

    /**
     * Can target ground and low-altitude units.
     * Examples: Rockets, machine guns
     */
    GROUND_AND_LOW("Ground + Low Alt", "Can target ground and low-altitude units"),

    LOW_AND_HIGH("Low Alt + High Alt", "Can target any flying units"),

    /**
     * Can target all elevation levels.
     * Examples: AA missiles, flak cannons, heat-seeking rockets (researched)
     */
    ALL_ELEVATIONS("All Elevations", "Can target units at any altitude");

    private final String displayName;
    private final String description;

    ElevationTargeting(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Check if this weapon can target a unit at the given elevation.
     *
     * @param targetElevation The elevation of the target
     * @return true if this weapon can hit targets at that elevation
     */
    public boolean canTarget(Elevation targetElevation) {
        return switch (this) {
            case GROUND_ONLY -> targetElevation == Elevation.GROUND;
            case GROUND_AND_LOW -> targetElevation == Elevation.GROUND || targetElevation == Elevation.LOW;
            case LOW_AND_HIGH -> targetElevation == Elevation.HIGH || targetElevation == Elevation.LOW;
            case ALL_ELEVATIONS -> true;
        };
    }
}

