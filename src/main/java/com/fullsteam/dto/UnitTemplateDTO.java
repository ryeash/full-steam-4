package com.fullsteam.dto;

import com.fullsteam.model.ArmyEconomy;
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
    private List<String> techRequirements;

    // Stats
    private double maxHealth;
    private double damage;
    private double speed;
    private double range;
    private int baseCost;
    /** Approximate periodic army upkeep from base build cost (before faction modifiers). */
    private int upkeep;

    public static UnitTemplateDTO fromType(UnitType template) {
        return UnitTemplateDTO.builder()
                .id(template.getDisplayName())
                .unitType(template.name())
                .displayName(template.getDisplayName())
                .description(template.getDescription())
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
                .upkeep(ArmyEconomy.periodicUpkeepFromBuildCost(template, template.getResourceCost()))
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
}
