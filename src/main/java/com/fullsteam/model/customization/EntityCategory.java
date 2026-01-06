package com.fullsteam.model.customization;

/**
 * Categories for customizable entities (units and buildings)
 * Used for UI organization and filtering
 */
public enum EntityCategory {
    // Unit categories
    INFANTRY("Infantry", "Ground-based infantry units"),
    VEHICLE("Vehicles", "Ground-based vehicle units"),
    FLYER("Air Units", "Flying units"),
    SUPPORT("Support", "Non-combat support units"),
    HERO("Hero Units", "Powerful unique hero units"),
    
    // Building categories
    PRODUCTION("Production", "Buildings that produce units"),
    DEFENSE("Defense", "Defensive structures"),
    ECONOMY("Economy", "Resource and credit generation"),
    TECH("Technology", "Research and tech buildings"),
    MONUMENT("Monuments", "Powerful unique faction monuments");
    
    private final String displayName;
    private final String description;
    
    EntityCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this is a unit category
     */
    public boolean isUnitCategory() {
        return this == INFANTRY || this == VEHICLE || this == FLYER || 
               this == SUPPORT || this == HERO;
    }
    
    /**
     * Check if this is a building category
     */
    public boolean isBuildingCategory() {
        return this == PRODUCTION || this == DEFENSE || this == ECONOMY || 
               this == TECH || this == MONUMENT;
    }
}
