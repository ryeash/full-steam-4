package com.fullsteam.model;

import lombok.Getter;

/**
 * Represents the elevation/altitude level of units in the game.
 * This determines which weapons can target them and their strategic positioning.
 *
 * <p>Strategic Implications:
 * <ul>
 *   <li>GROUND units are standard forces, targetable by all weapons</li>
 *   <li>LOW altitude units (VTOLs) are immune to basic infantry fire</li>
 *   <li>HIGH altitude units (fixed-wing) require specialized anti-air weapons</li>
 * </ul>
 */
@Getter
public enum Elevation {
    /**
     * Ground level - standard units and buildings.
     * Targetable by all weapons.
     * Examples: Infantry, tanks, buildings, walls
     */
    GROUND(0, "Ground", "🎯"),

    /**
     * Low altitude - VTOL aircraft, close air support.
     * Cannot be hit by basic infantry weapons.
     * Targetable by rockets and anti-air weapons.
     * Examples: Scout Drone, Gunship (future)
     */
    LOW(1, "Low Altitude", "↑"),

    /**
     * High altitude - fixed-wing aircraft, strategic bombers.
     * Only targetable by specialized anti-air weapons.
     * Examples: Bomber, Interceptor (future)
     */
    HIGH(2, "High Altitude", "↑↑");

    private final int level;
    private final String displayName;
    private final String icon;

    Elevation(int level, String displayName, String icon) {
        this.level = level;
        this.displayName = displayName;
        this.icon = icon;
    }

    /**
     * Check if this elevation is higher than another.
     *
     * @param other The elevation to compare to
     * @return true if this elevation is higher
     */
    public boolean isHigherThan(Elevation other) {
        return this.level > other.level;
    }

    /**
     * Check if this elevation is at ground level.
     *
     * @return true if GROUND elevation
     */
    public boolean isGround() {
        return this == GROUND;
    }

    /**
     * Check if this elevation is airborne (LOW or HIGH).
     *
     * @return true if not GROUND
     */
    public boolean isAirborne() {
        return this != GROUND;
    }
}






