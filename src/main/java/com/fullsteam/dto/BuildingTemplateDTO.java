package com.fullsteam.dto;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for building template information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingTemplateDTO {
    private String id;
    private String name;
    private double size;
    private String buildingType;
    private String displayName;
    private String description;
    private int pointCost;
    private String category;
    private List<String> producesUnitCategories;
    private List<String> tags;
    private String iconPath;
    private List<String> techRequirements;

    // Stats
    private double maxHealth;
    private int baseCost;
    private int powerValue;
    /** Short map label (matches in-game HUD). */
    private String label;
    /** Menu / picker emoji (matches in-game UI). */
    private String menuIcon;

    public static BuildingTemplateDTO fromType(BuildingType template) {
        return BuildingTemplateDTO.builder()
                .id(template.name())
                .name(template.getDisplayName())
                .size(template.getSize())
                .buildingType(template.name())
                .displayName(template.getDisplayName())
                .description(template.getDescription())
                .pointCost(template.getPointCost())
                .category(template.getBuildingCategory().name())
                .producesUnitCategories(getProducedUnitCategories(template)
                        .stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .tags(generateTags(template))
                .techRequirements(template.getTechRequirements()
                        .stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .maxHealth(template.getMaxHealth())
                .baseCost(template.getResourceCost())
                .powerValue(template.getPowerValue())
                .label(template.getLabel())
                .menuIcon(template.getMenuIcon())
                .build();
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
            case AIRFIELD -> categories.add(UnitCategory.FLYER);
        }
        return categories;
    }

    /**
     * Generate descriptive tags for filtering
     */
    private static List<String> generateTags(BuildingType buildingType) {
        List<String> tags = new ArrayList<>();

        // Category tag
        tags.add(buildingType.getBuildingCategory().name());

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
        }
        if (buildingType == BuildingType.BUNKER) {
            tags.add("GARRISON");
        }
        if (buildingType == BuildingType.SHIELD_GENERATOR) {
            tags.add("SHIELD");
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

    private static boolean isTurret(BuildingType buildingType) {
        return buildingType == BuildingType.TURRET ||
                buildingType == BuildingType.ROCKET_TURRET ||
                buildingType == BuildingType.FLAK_TURRET ||
                buildingType == BuildingType.LASER_TURRET ||
                buildingType == BuildingType.TEMPEST_SPIRE;
    }
}
