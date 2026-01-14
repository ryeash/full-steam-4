package com.fullsteam.model.customization;

import lombok.Getter;

/**
 * Categories for customizable entities (units and buildings)
 * Used for UI organization and filtering
 */
@Getter
public enum EntityCategory {
    // Unit categories
    INFANTRY("Infantry", "Ground-based infantry units"),
    VEHICLE("Vehicles", "Ground-based vehicle units"),
    FLYER("Air Units", "Flying units"),
    SUPPORT("Support", "Non-combat support units"),

    // Building categories
    PRODUCTION("Production", "Buildings that produce units"),
    DEFENSE("Defense", "Defensive structures"),
    ECONOMY("Economy", "Resource and credit generation"),
    TECH("Technology", "Research and tech buildings");

    private final String displayName;
    private final String description;

    EntityCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
