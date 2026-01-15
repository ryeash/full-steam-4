package com.fullsteam.dto;

import com.fullsteam.model.UnitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for unit template information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitTemplateDTO {
    private String id;
    private String unitType;
    private String displayName;
    private String description;
    private int pointCost;
    private String category;
    private String unitCategory;
    private List<String> tags;
    private String iconPath;
    private List<String> techRequirements; // NEW: Required buildings to unlock this

    // Stats
    private double maxHealth;
    private double damage;
    private double speed;
    private double range;
    private int baseCost;
    private int upkeep;

    public static UnitTemplateDTO fromType(UnitType template) {
        return UnitTemplateDTO.builder()
                .id(template.getDisplayName())
                .unitType(template.name())
                .displayName(template.getDisplayName())
                .description(generateDescription(template))
                .pointCost(template.getPointCost())
                .category(template.getCategory().name())
                .unitCategory(template.getCategory().name())
                .tags(generateTags(template))
                .techRequirements(template.getRequiredBuildings()
                        .stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .maxHealth(template.getMaxHealth())
                .damage(template.getDamage())
                .speed(template.getMovementSpeed())
                .range(template.getAttackRange())
                .baseCost(template.getResourceCost())
                .upkeep(template.getUpkeepCost())
                .build();
    }

    /**
     * Generate descriptive tags for filtering
     */
    private static List<String> generateTags(UnitType unitType) {
        List<String> tags = new ArrayList<>();

        // Category tag
        tags.add(unitType.getCategory().name());

        // Combat tags
        if (unitType.canAttack()) {
            tags.add("COMBAT");
            if (unitType.getDamage() > 30) {
                tags.add("HEAVY_DAMAGE");
            }
        }

        // Support tags
        if (unitType.isSupport()) {
            tags.add("SUPPORT");
        }
        if (unitType.canHarvest()) {
            tags.add("HARVESTER");
        }
        if (unitType.canBuild()) {
            tags.add("BUILDER");
        }

        // Range tags
        if (unitType.getAttackRange() > 150) {
            tags.add("LONG_RANGE");
        }

        // Elevation tag
        tags.add(unitType.getElevation().name());

        return tags;
    }

    /**
     * Generate a description for the unit
     */
    private static String generateDescription(UnitType unitType) {
        if (unitType.canHarvest()) {
            return "Harvests resources and constructs buildings";
        }
        if (unitType.isSupport()) {
            return "Support unit that heals nearby allies";
        }
        if (unitType.canAttack()) {
            String damageType = unitType.getDamage() > 30 ? "Heavy" : "Standard";
            String rangeType = unitType.getAttackRange() > 150 ? "long-range" : "close-range";
            return String.format("%s %s %s combat unit",
                    damageType, rangeType, unitType.getCategory().name().toLowerCase());
        }
        return unitType.getDisplayName();
    }
}
