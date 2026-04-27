package com.fullsteam.model.customization;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.FactionDefinition;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builds a runtime FactionDefinition from a CustomFactionConfig.
 * Applies all perk effects and creates the tech tree.
 */
@Singleton
@Slf4j
public class CustomFactionBuilder {

    /**
     * Build a FactionDefinition from a custom configuration
     */
    public FactionDefinition buildFromConfig(CustomFactionConfig config) {
        log.info("Building custom faction: {}", config.getDisplayName());

        // Get effective perks (highest tier only in each chain)
        Set<FactionPerk> effectivePerks = config.getEffectivePerks();

        log.info("Applying {} effective perks (filtered from {} selected)",
                effectivePerks.size(), config.getSelectedPerks().size());

        // Accumulate modifiers from all perks
        Map<UnitType, FactionDefinition.UnitStatModifier> accumulatedUnitMods = new HashMap<>();
        Map<BuildingType, FactionDefinition.BuildingStatModifier> accumulatedBuildingMods = new HashMap<>();
        Map<UnitType, Double> accumulatedUnitCosts = new HashMap<>();

        // Start with default values
        FactionDefinition.FactionDefinitionBuilder builder = FactionDefinition.builder()
                .unitTypes(new HashSet<>(config.getSelectedUnits()))
                .buildingTypes(new HashSet<>(config.getSelectedBuildings()))
                .activePerks(effectivePerks);

        // Apply each perk's effects using a temporary builder to extract modifiers
        for (FactionPerk perk : effectivePerks) {
            // Create a temporary builder to capture this perk's modifiers
            FactionDefinition.FactionDefinitionBuilder tempBuilder = FactionDefinition.builder();
            perk.applyToDefinition(tempBuilder, config);
            FactionDefinition tempDef = tempBuilder.build();

            // Merge unit stat modifiers
            for (Map.Entry<UnitType, FactionDefinition.UnitStatModifier> entry : tempDef.getUnitStatModifiers().entrySet()) {
                accumulatedUnitMods.merge(entry.getKey(), entry.getValue(), this::mergeUnitModifiers);
            }

            // Merge building stat modifiers
            for (Map.Entry<BuildingType, FactionDefinition.BuildingStatModifier> entry : tempDef.getBuildingStatModifiers().entrySet()) {
                accumulatedBuildingMods.merge(entry.getKey(), entry.getValue(), this::mergeBuildingModifiers);
            }

            // Merge unit cost modifiers
            for (Map.Entry<UnitType, Double> entry : tempDef.getUnitCostModifiers().entrySet()) {
                accumulatedUnitCosts.merge(entry.getKey(), entry.getValue(), (a, b) -> a * b);
            }

            // Apply scalar multipliers (last one wins for these)
            if (tempDef.getPowerEfficiencyMultiplier() != 1.0) {
                builder.powerEfficiencyMultiplier(tempDef.getPowerEfficiencyMultiplier());
            }
            if (tempDef.getUnitCostMultiplier() != 1.0) {
                builder.unitCostMultiplier(tempDef.getUnitCostMultiplier());
            }
            if (tempDef.getBuildingCostMultiplier() != 1.0) {
                builder.buildingCostMultiplier(tempDef.getBuildingCostMultiplier());
            }
            if (tempDef.getArmyRentCostMultiplier() != 1.0) {
                builder.armyRentCostMultiplier(tempDef.getArmyRentCostMultiplier());
            }
            if (tempDef.getBuildingHealthMultiplier() != 1.0) {
                builder.buildingHealthMultiplier(tempDef.getBuildingHealthMultiplier());
            }

            log.debug("Applied perk: {}", perk.getDisplayName());
        }

        // Apply accumulated modifiers to the main builder
        builder.unitStatModifiers(accumulatedUnitMods);
        builder.buildingStatModifiers(accumulatedBuildingMods);
        builder.unitCostModifiers(accumulatedUnitCosts);

        FactionDefinition definition = builder.build();

        log.info("Custom faction built: {} with {} units, {} buildings, {} perks ({} effective)",
                config.getDisplayName(),
                config.getSelectedUnits().size(),
                config.getSelectedBuildings().size(),
                config.getSelectedPerks().size(),
                effectivePerks.size());

        return definition;
    }

    /**
     * Merge two UnitStatModifiers by multiplying their values
     */
    private FactionDefinition.UnitStatModifier mergeUnitModifiers(
            FactionDefinition.UnitStatModifier a,
            FactionDefinition.UnitStatModifier b) {
        return FactionDefinition.UnitStatModifier.builder()
                .healthMultiplier(a.getHealthMultiplier() * b.getHealthMultiplier())
                .speedMultiplier(a.getSpeedMultiplier() * b.getSpeedMultiplier())
                .damageMultiplier(a.getDamageMultiplier() * b.getDamageMultiplier())
                .rangeMultiplier(a.getRangeMultiplier() * b.getRangeMultiplier())
                .attackRateMultiplier(a.getAttackRateMultiplier() * b.getAttackRateMultiplier())
                .resourceCollectionMultiplier(a.getResourceCollectionMultiplier() * b.getResourceCollectionMultiplier())
                .build();
    }

    /**
     * Merge two BuildingStatModifiers by multiplying their values (additive for garrison capacity)
     */
    private FactionDefinition.BuildingStatModifier mergeBuildingModifiers(
            FactionDefinition.BuildingStatModifier a,
            FactionDefinition.BuildingStatModifier b) {
        return FactionDefinition.BuildingStatModifier.builder()
                .healthMultiplier(a.getHealthMultiplier() * b.getHealthMultiplier())
                .buildTimeMultiplier(a.getBuildTimeMultiplier() * b.getBuildTimeMultiplier())
                .productionSpeedMultiplier(a.getProductionSpeedMultiplier() * b.getProductionSpeedMultiplier())
                .costMultiplier(a.getCostMultiplier() * b.getCostMultiplier())
                .damageMultiplier(a.getDamageMultiplier() * b.getDamageMultiplier())
                .garrisonCapacityBonus(a.getGarrisonCapacityBonus() + b.getGarrisonCapacityBonus())
                .build();
    }
}
