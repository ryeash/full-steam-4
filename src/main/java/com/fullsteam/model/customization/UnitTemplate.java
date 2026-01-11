package com.fullsteam.model.customization;

import com.fullsteam.model.UnitCategory;
import com.fullsteam.model.UnitType;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Template for a unit that can be selected during faction customization.
 * Contains metadata and stats for display in the UI.
 */
@Getter
@Builder
public class UnitTemplate implements CustomizableEntity {
    private final UnitType unitType;
    private final String displayName;
    private final String description;
    private final int pointCost;
    private final EntityCategory category;
    private final UnitCategory unitCategory; // INFANTRY, VEHICLE, FLYER, WORKER
    @Builder.Default
    private final List<String> tags = new ArrayList<>();
    private final String iconPath;

    // Stats for display
    private final int maxHealth;
    private final int damage;
    private final double speed;
    private final int range;
    private final int baseCost;
    private final int upkeep;

    @Override
    public String getId() {
        return unitType.name();
    }

    /**
     * Create a UnitTemplate from a UnitType
     */
    public static UnitTemplate fromUnitType(UnitType unitType) {
        return UnitTemplate.builder()
                .unitType(unitType)
                .displayName(unitType.getDisplayName())
                .description(generateDescription(unitType))
                .pointCost(unitType.getPointCost())  // Use static value from enum
                .category(mapToEntityCategory(unitType.getCategory()))
                .unitCategory(unitType.getCategory())
                .tags(generateTags(unitType))
                .iconPath("/icons/units/" + unitType.name().toLowerCase() + ".png")
                .maxHealth((int) unitType.getMaxHealth())
                .damage((int) unitType.getDamage())
                .speed(unitType.getMovementSpeed())
                .range((int) unitType.getAttackRange())
                .baseCost(unitType.getResourceCost())
                .upkeep(unitType.getUpkeepCost())
                .build();
    }

    /**
     * Map UnitCategory to EntityCategory
     */
    private static EntityCategory mapToEntityCategory(UnitCategory unitCategory) {
        return switch (unitCategory) {
            case INFANTRY -> EntityCategory.INFANTRY;
            case VEHICLE -> EntityCategory.VEHICLE;
            case FLYER -> EntityCategory.FLYER;
            case WORKER -> EntityCategory.SUPPORT;
        };
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
