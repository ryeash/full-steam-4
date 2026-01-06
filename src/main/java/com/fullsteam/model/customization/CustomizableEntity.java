package com.fullsteam.model.customization;

import java.util.List;

/**
 * Interface for entities that can be selected during faction customization
 * (units, buildings, perks)
 */
public interface CustomizableEntity {
    /**
     * Unique identifier for this entity
     */
    String getId();
    
    /**
     * Human-readable display name
     */
    String getDisplayName();
    
    /**
     * Description of what this entity does
     */
    String getDescription();
    
    /**
     * Point cost for faction customization
     */
    int getPointCost();
    
    /**
     * Category for UI organization
     */
    EntityCategory getCategory();
    
    /**
     * Tags for filtering and search (e.g., "ANTI_VEHICLE", "HEAVY", "SUPPORT")
     */
    List<String> getTags();
    
    /**
     * Path to icon image (optional)
     */
    default String getIconPath() {
        return null;
    }
}
