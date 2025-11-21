package com.fullsteam.model.factions;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import lombok.Builder;
import lombok.Getter;

import java.util.*;

/**
 * Complete definition of a faction including tech tree, modifiers, and bonuses.
 * This is the central configuration for faction behavior.
 */
@Getter
@Builder
public class FactionDefinition {
    
    private final Faction faction;
    private final FactionTechTree techTree;
    
    /**
     * Hero unit unique to this faction (null if none)
     */
    private final UnitType heroUnit;
    
    /**
     * Monument building unique to this faction (null if none)
     */
    private final BuildingType monumentBuilding;
    
    /**
     * Upkeep limit multiplier (1.0 = normal, 1.5 = +50% upkeep)
     */
    @Builder.Default
    private final double upkeepMultiplier = 1.0;
    
    /**
     * Power efficiency multiplier (1.0 = normal, 0.7 = -30% consumption)
     */
    @Builder.Default
    private final double powerEfficiencyMultiplier = 1.0;
    
    /**
     * Building health multiplier (1.0 = normal, 1.1 = +10% health)
     */
    @Builder.Default
    private final double buildingHealthMultiplier = 1.0;
    
    /**
     * Unit cost multiplier (1.0 = normal, 0.8 = -20% cost)
     */
    @Builder.Default
    private final double unitCostMultiplier = 1.0;
    
    /**
     * Building cost multiplier (1.0 = normal, 1.2 = +20% cost)
     */
    @Builder.Default
    private final double buildingCostMultiplier = 1.0;
    
    /**
     * Per-unit type cost modifiers (overrides global multiplier)
     */
    @Builder.Default
    private final Map<UnitType, Double> unitCostModifiers = new HashMap<>();
    
    /**
     * Per-unit type stat modifiers
     */
    @Builder.Default
    private final Map<UnitType, UnitStatModifier> unitStatModifiers = new HashMap<>();
    
    /**
     * Per-building type stat modifiers
     */
    @Builder.Default
    private final Map<BuildingType, BuildingStatModifier> buildingStatModifiers = new HashMap<>();
    
    /**
     * Get the effective cost for a unit
     */
    public int getUnitCost(UnitType unitType) {
        double baseCost = unitType.getResourceCost();
        double modifier = unitCostModifiers.getOrDefault(unitType, unitCostMultiplier);
        return (int) Math.round(baseCost * modifier);
    }
    
    /**
     * Get the effective cost for a building
     */
    public int getBuildingCost(BuildingType buildingType) {
        double baseCost = buildingType.getResourceCost();
        return (int) Math.round(baseCost * buildingCostMultiplier);
    }
    
    /**
     * Get the effective health for a building
     */
    public double getBuildingHealth(BuildingType buildingType) {
        double baseHealth = buildingType.getMaxHealth();
        BuildingStatModifier modifier = buildingStatModifiers.get(buildingType);
        double multiplier = modifier != null ? modifier.healthMultiplier : buildingHealthMultiplier;
        return baseHealth * multiplier;
    }
    
    /**
     * Get the effective upkeep limit
     */
    public int getUpkeepLimit(int baseLimit) {
        return (int) Math.round(baseLimit * upkeepMultiplier);
    }
    
    /**
     * Get the effective power consumption/generation
     */
    public int getPowerValue(int basePower) {
        if (basePower < 0) {
            // Consumption - apply efficiency
            return (int) Math.round(basePower * powerEfficiencyMultiplier);
        }
        return basePower; // Generation unchanged
    }
    
    /**
     * Stat modifiers for units
     */
    @Getter
    @Builder
    public static class UnitStatModifier {
        @Builder.Default
        private final double healthMultiplier = 1.0;
        @Builder.Default
        private final double speedMultiplier = 1.0;
        @Builder.Default
        private final double damageMultiplier = 1.0;
        @Builder.Default
        private final double rangeMultiplier = 1.0;
        @Builder.Default
        private final double attackRateMultiplier = 1.0;
    }
    
    /**
     * Stat modifiers for buildings
     */
    @Getter
    @Builder
    public static class BuildingStatModifier {
        @Builder.Default
        private final double healthMultiplier = 1.0;
        @Builder.Default
        private final double buildTimeMultiplier = 1.0;
    }
}
