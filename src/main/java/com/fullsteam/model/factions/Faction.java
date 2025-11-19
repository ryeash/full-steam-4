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

