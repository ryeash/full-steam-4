package com.fullsteam.model.customization;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitCategory;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Template for a building that can be selected during faction customization.
 * Contains metadata and stats for display in the UI.
 */
@Getter
@Builder
public class BuildingTemplate implements CustomizableEntity {
    private final BuildingType buildingType;
    private final String displayName;
    private final String description;
    private final int pointCost;
    private final EntityCategory category;
    private final boolean isMonument;
    @Builder.Default
    private final List<UnitCategory> producesUnitCategories = new ArrayList<>();
    @Builder.Default
    private final List<String> tags = new ArrayList<>();
    private final String iconPath;
    
    // Stats for display
    private final int maxHealth;
    private final int baseCost;
    private final int powerValue;
    
    @Override
    public String getId() {
        return buildingType.name();
    }
    
    /**
     * Create a BuildingTemplate from a BuildingType
     */
    public static BuildingTemplate fromBuildingType(BuildingType buildingType) {
        return BuildingTemplate.builder()
            .buildingType(buildingType)
            .displayName(buildingType.getDisplayName())
            .description(generateDescription(buildingType))
            .pointCost(calculatePointCost(buildingType))
            .category(mapToCategory(buildingType))
            .isMonument(isMonument(buildingType))
            .producesUnitCategories(getProducedUnitCategories(buildingType))
            .tags(generateTags(buildingType))
            .iconPath("/icons/buildings/" + buildingType.name().toLowerCase() + ".png")
            .maxHealth((int) buildingType.getMaxHealth())
            .baseCost(buildingType.getResourceCost())
            .powerValue(buildingType.getPowerValue())
            .build();
    }
    
    /**
     * Calculate point cost based on building type and utility
     */
    private static int calculatePointCost(BuildingType buildingType) {
        // HEADQUARTERS is required and free
        if (buildingType == BuildingType.HEADQUARTERS) {
            return 0;
        }
        
        // Monument buildings get fixed high cost
        if (isMonument(buildingType)) {
            return 12;
        }
        
        // Production buildings (produce units)
        if (buildingType.isCanProduceUnits()) {
            return switch (buildingType) {
                case BARRACKS -> 3;          // Basic infantry production
                case FACTORY -> 5;            // Vehicle production
                case AIRFIELD, HANGAR -> 5;   // Air unit production
                default -> 4;
            };
        }
        
        // Defense buildings
        if (isTurret(buildingType)) {
            return switch (buildingType) {
                case TURRET -> 2;             // Basic turret
                case ROCKET_TURRET -> 3;      // Anti-air turret
                case LASER_TURRET -> 4;       // Advanced turret
                default -> 3;
            };
        }
        if (buildingType == BuildingType.BUNKER) {
            return 3;
        }
        if (buildingType == BuildingType.SHIELD_GENERATOR) {
            return 4;
        }
        if (buildingType == BuildingType.WALL) {
            return 1; // Walls are cheap
        }
        
        // Economy buildings
        if (buildingType == BuildingType.POWER_PLANT) {
            return 2; // Essential, cheap
        }
        if (buildingType == BuildingType.REFINERY) {
            return 2; // Essential, cheap
        }
        if (buildingType == BuildingType.BANK) {
            return 4; // Credit generation
        }
        
        // Tech buildings
        if (buildingType == BuildingType.RESEARCH_LAB) {
            return 4;
        }
        if (buildingType == BuildingType.TECH_CENTER) {
            return 6; // High-tier tech
        }
        
        // Default fallback
        return 3;
    }
    
    /**
     * Check if this is a monument building
     */
    private static boolean isMonument(BuildingType buildingType) {
        return List.of(
            BuildingType.SANDSTORM_GENERATOR,
            BuildingType.ANDROID_FACTORY,
            BuildingType.PHOTON_SPIRE,
            BuildingType.COMMAND_CITADEL,
            BuildingType.TEMPEST_SPIRE
        ).contains(buildingType);
    }
    
    /**
     * Check if this is a turret
     */
    private static boolean isTurret(BuildingType buildingType) {
        return buildingType == BuildingType.TURRET ||
               buildingType == BuildingType.ROCKET_TURRET ||
               buildingType == BuildingType.LASER_TURRET;
    }
    
    /**
     * Map building type to entity category
     */
    private static EntityCategory mapToCategory(BuildingType buildingType) {
        if (isMonument(buildingType)) {
            return EntityCategory.MONUMENT;
        }
        if (buildingType.isCanProduceUnits()) {
            return EntityCategory.PRODUCTION;
        }
        if (isTurret(buildingType) || 
            buildingType == BuildingType.BUNKER || 
            buildingType == BuildingType.SHIELD_GENERATOR ||
            buildingType == BuildingType.WALL) {
            return EntityCategory.DEFENSE;
        }
        if (buildingType == BuildingType.POWER_PLANT || 
            buildingType == BuildingType.REFINERY || 
            buildingType == BuildingType.BANK) {
            return EntityCategory.ECONOMY;
        }
        if (buildingType == BuildingType.RESEARCH_LAB || 
            buildingType == BuildingType.TECH_CENTER) {
            return EntityCategory.TECH;
        }
        return EntityCategory.ECONOMY; // Default
    }
    
    /**
     * Get which unit categories this building can produce
     */
    private static List<UnitCategory> getProducedUnitCategories(BuildingType buildingType) {
        List<UnitCategory> categories = new ArrayList<>();
        
        switch (buildingType) {
            case HEADQUARTERS -> categories.add(UnitCategory.WORKER);
            case BARRACKS -> categories.add(UnitCategory.INFANTRY);
            case FACTORY -> categories.add(UnitCategory.VEHICLE);
            case AIRFIELD, HANGAR -> categories.add(UnitCategory.FLYER);
        }
        
        return categories;
    }
    
    /**
     * Generate descriptive tags for filtering
     */
    private static List<String> generateTags(BuildingType buildingType) {
        List<String> tags = new ArrayList<>();
        
        // Category tag
        tags.add(mapToCategory(buildingType).name());
        
        // Production tags
        if (buildingType.isCanProduceUnits()) {
            tags.add("PRODUCTION");
            for (UnitCategory category : getProducedUnitCategories(buildingType)) {
                tags.add("PRODUCES_" + category.name());
            }
        }
        
        // Defense tags
        if (isTurret(buildingType)) {
            tags.add("TURRET");
            tags.add("DEFENSIVE");
        }
        if (buildingType == BuildingType.BUNKER) {
            tags.add("GARRISON");
            tags.add("DEFENSIVE");
        }
        if (buildingType == BuildingType.SHIELD_GENERATOR) {
            tags.add("SHIELD");
            tags.add("DEFENSIVE");
        }
        
        // Economy tags
        if (buildingType == BuildingType.POWER_PLANT) {
            tags.add("POWER");
            tags.add("ESSENTIAL");
        }
        if (buildingType == BuildingType.REFINERY) {
            tags.add("RESOURCE");
            tags.add("ESSENTIAL");
        }
        if (buildingType == BuildingType.BANK) {
            tags.add("CREDIT_GENERATION");
        }
        
        // Tech tags
        if (buildingType == BuildingType.RESEARCH_LAB || 
            buildingType == BuildingType.TECH_CENTER) {
            tags.add("RESEARCH");
        }
        
        // Monument tag
        if (isMonument(buildingType)) {
            tags.add("MONUMENT");
            tags.add("UNIQUE");
        }
        
        // Power tags
        if (buildingType.getPowerValue() > 0) {
            tags.add("GENERATES_POWER");
        } else if (buildingType.getPowerValue() < 0) {
            tags.add("CONSUMES_POWER");
        }
        
        return tags;
    }
    
    /**
     * Generate a description for the building
     */
    private static String generateDescription(BuildingType buildingType) {
        if (buildingType == BuildingType.HEADQUARTERS) {
            return "Main base building. Produces workers and serves as a tech anchor.";
        }
        if (isMonument(buildingType)) {
            return "Powerful monument building that provides faction-wide buffs.";
        }
        if (buildingType.isCanProduceUnits()) {
            List<UnitCategory> categories = getProducedUnitCategories(buildingType);
            if (!categories.isEmpty()) {
                return String.format("Produces %s units", categories.get(0).name().toLowerCase());
            }
            return "Production building";
        }
        if (isTurret(buildingType)) {
            return "Automated defense turret that attacks enemy units";
        }
        if (buildingType == BuildingType.BUNKER) {
            return "Garrison building that houses infantry units";
        }
        if (buildingType == BuildingType.POWER_PLANT) {
            return "Generates power for your base";
        }
        if (buildingType == BuildingType.REFINERY) {
            return "Enables resource harvesting from resource nodes";
        }
        if (buildingType == BuildingType.RESEARCH_LAB) {
            return "Enables research of combat and economy upgrades";
        }
        return buildingType.getDisplayName();
    }
}
