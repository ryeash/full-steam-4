package com.fullsteam.model.customization;

import lombok.Getter;

/**
 * Categories for customizable entities (units and buildings)
 * Used for UI organization and filtering
 */
@Getter
public enum BuildingCategory {
    PRODUCTION("Production", "Buildings that produce units"),
    DEFENSE("Defense", "Defensive structures"),
    ECONOMY("Economy", "Resource and credit generation"),
    TECH("Technology", "Research and tech buildings");

    private final String displayName;
    private final String description;

    BuildingCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
