package com.fullsteam.model.factions;

import lombok.Getter;

/**
 * Represents the different playable factions in the RTS game.
 * Each faction has unique units, buildings, tech trees, and bonuses.
 */
@Getter
public enum Faction {
    /**
     * TERRAN - Balanced faction with access to all standard units and buildings.
     * Theme: Versatile military force with balanced stats.
     */
    TERRAN(
            "Terran Coalition",
            "Balanced military force with access to all standard units and buildings. " +
                    "Versatile and adaptable to any situation.",
            0x4169E1  // Royal blue
    ),

    /**
     * NOMADS - Mobile warfare faction focused on speed and light units.
     * Theme: Fast, mobile units with weaker defenses.
     */
    NOMADS(
            "Nomad Clans",
            "Fast and mobile warfare specialists. Vehicles are cheaper and faster, " +
                    "but buildings are weaker. Excels at hit-and-run tactics.",
            0xFF8C00  // Dark orange
    ),

    /**
     * SYNTHESIS - Advanced technology faction with powerful but expensive units.
     * Theme: High-tech units with shields and energy weapons.
     */
    SYNTHESIS(
            "Synthesis Collective",
            "Advanced technology faction with powerful units and superior power efficiency. " +
                    "Units are expensive but come with shield technology.",
            0x00CED1  // Dark turquoise
    ),

    /**
     * TECH_ALLIANCE - High-tech faction specializing in beam weapons and energy technology.
     * Theme: Beam weapons, energy shields, and advanced technology.
     */
    TECH_ALLIANCE(
            "Tech Alliance",
            "Cutting-edge technology faction specializing in beam weapons and energy systems. " +
                    "All units use instant-hit beam weapons with high precision and no projectile travel time.",
            0x00FF00  // Bright green (energy theme)
    ),

    /**
     * STORM_WINGS - Air superiority faction specializing in aerial dominance.
     * Theme: Limited ground forces, powerful and cost-effective air units.
     */
    STORM_WINGS(
            "Storm Wings",
            "Elite air force faction with aerial superiority. " +
                    "Air units are cheaper and more powerful, but ground forces are limited. " +
                    "Relies on air dominance to control the battlefield.",
            0x4682B4  // Steel blue (sky theme)
    );

    private final String displayName;
    private final String description;
    private final int themeColor;

    Faction(String displayName, String description, int themeColor) {
        this.displayName = displayName;
        this.description = description;
        this.themeColor = themeColor;
    }

    /**
     * Get the default faction for new players
     */
    public static Faction getDefault() {
        return TERRAN;
    }
}
