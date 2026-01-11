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
                .pointCost(buildingType.getPointCost())  // Use static value from enum
                .category(mapToCategory(buildingType))
                .producesUnitCategories(getProducedUnitCategories(buildingType))
                .tags(generateTags(buildingType))
                .iconPath("/icons/buildings/" + buildingType.name().toLowerCase() + ".png")
                .maxHealth((int) buildingType.getMaxHealth())
                .baseCost(buildingType.getResourceCost())
                .powerValue(buildingType.getPowerValue())
                .build();
    }

    /**
     * Check if this is a turret
     */
    private static boolean isTurret(BuildingType buildingType) {
        return buildingType == BuildingType.TURRET ||
                buildingType == BuildingType.ROCKET_TURRET ||
                buildingType == BuildingType.LASER_TURRET ||
                buildingType == BuildingType.TEMPEST_SPIRE;
    }

    /**
     * Map building type to entity category
     */
    private static EntityCategory mapToCategory(BuildingType buildingType) {
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
