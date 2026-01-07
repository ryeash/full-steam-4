package com.fullsteam.model.factions;

import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.PlayerFaction;
import com.fullsteam.model.RTSGameManager;
import com.fullsteam.model.Targetable;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.customization.FactionPerk;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Complete definition of a faction including tech tree, modifiers, and bonuses.
 * This is the central configuration for faction behavior.
 */
@Data
@Builder
public class FactionDefinition {
    private static final Logger log = LoggerFactory.getLogger(FactionDefinition.class);
    @Builder.Default
    private Set<UnitType> unitTypes = Set.of();
    @Builder.Default
    private Set<BuildingType> buildingTypes = Set.of();
    @Builder.Default
    private Set<FactionPerk> activePerks = Set.of();

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
        BuildingStatModifier modifier = buildingStatModifiers.get(buildingType);
        double multiplier = modifier != null && modifier.costMultiplier != 1.0
                ? modifier.costMultiplier
                : buildingCostMultiplier;
        return (int) Math.round(baseCost * multiplier);
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
     * Check if a unit is available to this faction (checks if any building produces it)
     *
     * @deprecated Use ResearchManager.canProduceUnit() for research-based unit availability
     */
    @Deprecated
    public boolean canBuildUnit(UnitType unitType) {
        return unitTypes.contains(unitType);
    }

    /**
     * Check if a building is available to this faction
     * Note: HEADQUARTERS is always available (you start with one), but for
     * custom factions we check the buildingsAndUnits map to see if they selected it
     */
    public boolean canBuildBuilding(BuildingType buildingType) {
        return buildingTypes.contains(buildingType);
    }

    /**
     * Get which starter units a building produces for this faction (initial units only)
     *
     * @deprecated Use ResearchManager for actual unit production validation
     */
    @Deprecated
    public List<UnitType> getUnitsProducedBy(BuildingType buildingType) {
        return unitTypes.stream()
                .filter(u -> u.getProducedBy() == buildingType)
                .toList();
    }

    /**
     * Check if a building can produce a specific unit for this faction
     *
     * @deprecated Use ResearchManager.canProduceUnit() + UnitType.getProducedBy() instead
     */
    @Deprecated
    public boolean canBuildingProduceUnit(BuildingType buildingType, UnitType unitType) {
        return unitType.getProducedBy() == buildingType;
    }

    /**
     * Called when a unit is created for this faction
     */
    public void onUnitCreated(Unit unit, PlayerFaction faction, RTSGameManager game) {
        for (FactionPerk perk : activePerks) {
            try {
                perk.onUnitCreated(unit, faction, game);
            } catch (Exception e) {
                log.error("Error in perk {} onUnitCreated", perk, e);
            }
        }
    }

    /**
     * Called when a unit belonging to this faction is destroyed
     */
    public void onUnitDestroyed(Unit unit, PlayerFaction faction, RTSGameManager game) {
        for (FactionPerk perk : activePerks) {
            try {
                perk.onUnitDestroyed(unit, faction, game);
            } catch (Exception e) {
                log.error("Error in perk {} onUnitDestroyed", perk, e);
            }
        }
    }

    /**
     * Called when a building is created for this faction
     */
    public void onBuildingCreated(Building building, PlayerFaction faction, RTSGameManager game) {
        for (FactionPerk perk : activePerks) {
            try {
                perk.onBuildingCreated(building, faction, game);
            } catch (Exception e) {
                log.error("Error in perk {} onBuildingCreated", perk, e);
            }
        }
    }

    /**
     * Called when a building belonging to this faction is destroyed
     */
    public void onBuildingDestroyed(Building building, PlayerFaction faction, RTSGameManager game) {
        for (FactionPerk perk : activePerks) {
            try {
                perk.onBuildingDestroyed(building, faction, game);
            } catch (Exception e) {
                log.error("Error in perk {} onBuildingDestroyed", perk, e);
            }
        }
    }

    /**
     * Called when a unit belonging to this faction deals damage
     */
    public void onUnitDealsDamage(Unit attacker, Targetable target, double damage, PlayerFaction faction, RTSGameManager game) {
        for (FactionPerk perk : activePerks) {
            try {
                perk.onUnitDealsDamage(attacker, target, damage, faction, game);
            } catch (Exception e) {
                log.error("Error in perk {} onUnitDealsDamage", perk, e);
            }
        }
    }

    /**
     * Modify income produced by a building
     */
    public double modifyBuildingIncome(Building building, PlayerFaction faction, double baseIncome) {
        double modified = baseIncome;
        for (FactionPerk perk : activePerks) {
            try {
                modified = perk.modifyBuildingIncome(building, faction, modified);
            } catch (Exception e) {
                log.error("Error in perk {} modifyBuildingIncome", perk, e);
            }
        }
        return modified;
    }

    /**
     * Modify monument buff strength
     */
    public double modifyMonumentBuffStrength(Building monument, PlayerFaction faction, double baseStrength) {
        double modified = baseStrength;
        for (FactionPerk perk : activePerks) {
            try {
                modified = perk.modifyMonumentBuffStrength(monument, faction, modified);
            } catch (Exception e) {
                log.error("Error in perk {} modifyMonumentBuffStrength", perk, e);
            }
        }
        return modified;
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
        @Builder.Default
        private final double resourceCollectionMultiplier = 1.0;
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
        private final double buildTimeMultiplier = 1.0; // Time to construct the building itself
        @Builder.Default
        private final double productionSpeedMultiplier = 1.0; // Speed of unit production (higher = faster)
        @Builder.Default
        private final double costMultiplier = 1.0;
        @Builder.Default
        private final double damageMultiplier = 1.0;
        @Builder.Default
        private final int garrisonCapacityBonus = 0;
    }
}
