package com.fullsteam.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object for faction information.
 * Provides all faction data needed by the client UI.
 */
@Data
@Builder
public class FactionInfoDTO {

    // Basic Info
    private String factionId;
    private String displayName;
    private String description;
    private int themeColor;
    private String icon;

    // Tech Tree
    private List<UnitInfo> availableUnits;
    private List<BuildingInfo> availableBuildings;
    private List<ResearchInfoDTO> availableResearch; // All research available to this faction

    // Global Modifiers
    private double powerEfficiencyModifier; // e.g., 0.7 = 30% less power consumption
    private double upkeepLimitModifier; // e.g., 1.5 = 50% more upkeep

    // Faction Bonuses/Penalties (for display)
    private List<String> bonuses;
    private List<String> penalties;

    /**
     * Information about a unit available to this faction
     */
    @Data
    @Builder
    public static class UnitInfo {
        private String unitType;
        private String displayName;

        // Base stats
        private int baseCost;
        private int baseUpkeep;
        private double baseHealth;
        private double baseSpeed;
        private double baseDamage;
        private double baseAttackRate;
        private double baseAttackRange;

        // Faction-modified stats
        private int cost;
        private int upkeep;
        private double health;
        private double speed;
        private double damage;
        private double attackRate;
        private double attackRange;

        // Build info
        private int buildTimeSeconds;
        private String producedBy; // Building type that produces this unit
        private int sides; // For rendering
        private int color; // For rendering
        private double size; // For rendering

        // Capabilities
        private boolean canAttack;
        private boolean canBuild;
        private boolean canHarvest;
        private boolean canMine;
        private boolean isSupport;

        // Modifiers applied (for tooltip display)
        private Double costModifier; // null if no modifier
        private Double speedModifier;
        private Double healthModifier;
        private Double damageModifier;
    }

    /**
     * Information about a building available to this faction
     */
    @Data
    @Builder
    public static class BuildingInfo {
        private String buildingType;
        private String displayName;

        // Base stats
        private int baseCost;
        private double baseHealth;
        private int basePowerValue;

        // Faction-modified stats
        private int cost;
        private double health;
        private int powerValue;

        // Build info
        private int buildTimeSeconds;
        private int sides; // For rendering
        private int color; // For rendering
        private double size; // For rendering

        // Capabilities
        private boolean canProduceUnits;
        private List<String> producedUnits; // Unit types this building can produce
        private int requiredTechTier;
        private List<String> techRequirements; // Building types required before this can be built

        // Modifiers applied (for tooltip display)
        private Double costModifier; // null if no modifier
        private Double healthModifier;
        private Double powerModifier;
    }
}

