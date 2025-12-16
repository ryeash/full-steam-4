package com.fullsteam.model.research;

import lombok.Getter;

/**
 * Categories for organizing research types.
 * Used for UI organization and filtering.
 */
@Getter
public enum ResearchCategory {
    COMBAT("Combat", "Damage, range, and attack rate upgrades", 0xFF4444),
    DEFENSE("Defense", "Health and armor upgrades", 0x4444FF),
    MOBILITY("Mobility", "Movement speed upgrades", 0x44FF44),
    ECONOMY("Economy", "Resource gathering and production upgrades", 0xFFAA00),
    SPECIAL("Special", "Unique abilities and vision upgrades", 0xFF44FF);

    private final String displayName;
    private final String description;
    private final int color;

    ResearchCategory(String displayName, String description, int color) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
    }
}









