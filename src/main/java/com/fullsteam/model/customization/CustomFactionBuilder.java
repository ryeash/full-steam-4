package com.fullsteam.model.customization;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.Faction;
import com.fullsteam.model.factions.FactionDefinition;
import com.fullsteam.model.factions.FactionTechTree;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

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
        
        // Start with default values
        FactionDefinition.FactionDefinitionBuilder builder = FactionDefinition.builder()
            .faction(Faction.CUSTOM)
            .techTree(buildTechTree(config))
            .heroUnit(findHeroUnit(config))
            .monumentBuilding(findMonument(config))
            .customSelectedUnits(new HashSet<>(config.getSelectedUnits())); // Store selected units
        
        // Apply all perk effects to modify the base values
        Map<String, Double> modifiers = calculateModifiers(config.getSelectedPerks());
        
        // Set modifiers from perks
        builder.upkeepMultiplier(modifiers.getOrDefault("upkeep", 1.0));
        builder.powerEfficiencyMultiplier(modifiers.getOrDefault("powerEfficiency", 1.0));
        builder.unitCostMultiplier(modifiers.getOrDefault("unitCost", 1.0));
        builder.buildingCostMultiplier(modifiers.getOrDefault("buildingCost", 1.0));
        builder.buildingHealthMultiplier(modifiers.getOrDefault("buildingHealth", 1.0));
        
        // Per-unit modifiers (applied by perks like Infantry Doctrine)
        builder.unitCostModifiers(calculateUnitCostModifiers(config));
        builder.unitStatModifiers(calculateUnitStatModifiers(config));
        builder.buildingStatModifiers(calculateBuildingStatModifiers(config));
        
        FactionDefinition definition = builder.build();
        
        log.info("Custom faction built: {} with {} units, {} buildings, {} perks",
            config.getDisplayName(),
            config.getSelectedUnits().size(),
            config.getSelectedBuildings().size(),
            config.getSelectedPerks().size());
        
        return definition;
    }
    
    /**
     * Build a FactionTechTree from selected units and buildings
     */
    private FactionTechTree buildTechTree(CustomFactionConfig config) {
        Map<BuildingType, List<UnitType>> buildingsAndUnits = new LinkedHashMap<>();
        
        // Group units by their production building
        // Include ALL selected buildings, even if they don't produce units
        for (BuildingType building : config.getSelectedBuildings()) {
            List<UnitType> unitsForBuilding = config.getSelectedUnits().stream()
                .filter(unit -> unit.getProducedBy() == building)
                .collect(Collectors.toList());
            
            buildingsAndUnits.put(building, unitsForBuilding);
            log.info("Tech tree: {} -> {} units", building, unitsForBuilding.size());
        }
        
        log.info("Built tech tree with {} buildings", buildingsAndUnits.size());
        
        return FactionTechTree.builder()
            .buildingsAndUnits(buildingsAndUnits)
            .build();
    }
    
    /**
     * Find the hero unit (if any) in the selected units
     */
    private UnitType findHeroUnit(CustomFactionConfig config) {
        List<UnitType> heroes = Arrays.asList(
            UnitType.CRAWLER,
            UnitType.RAIDER,
            UnitType.COLOSSUS,
            UnitType.PHOTON_TITAN,
            UnitType.GUNSHIP,
            UnitType.GIGANTONAUT
        );
        
        return config.getSelectedUnits().stream()
            .filter(heroes::contains)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Find the monument building (if any) in the selected buildings
     */
    private BuildingType findMonument(CustomFactionConfig config) {
        List<BuildingType> monuments = Arrays.asList(
            BuildingType.SANDSTORM_GENERATOR,
            BuildingType.ANDROID_FACTORY,
            BuildingType.PHOTON_SPIRE,
            BuildingType.COMMAND_CITADEL,
            BuildingType.TEMPEST_SPIRE
        );
        
        return config.getSelectedBuildings().stream()
            .filter(monuments::contains)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Calculate global modifiers from selected perks
     */
    private Map<String, Double> calculateModifiers(Set<FactionPerk> perks) {
        Map<String, Double> modifiers = new HashMap<>();
        
        // Default values
        modifiers.put("upkeep", 1.0);
        modifiers.put("powerEfficiency", 1.0);
        modifiers.put("unitCost", 1.0);
        modifiers.put("buildingCost", 1.0);
        modifiers.put("buildingHealth", 1.0);
        modifiers.put("buildTime", 1.0);
        modifiers.put("unitHealth", 1.0);
        modifiers.put("unitDamage", 1.0);
        modifiers.put("researchSpeed", 1.0);
        modifiers.put("harvestRate", 1.0);
        
        // Apply each perk's effects (highest level wins for tiered perks)
        for (FactionPerk perk : perks) {
            applyPerkModifiers(perk, modifiers);
        }
        
        return modifiers;
    }
    
    /**
     * Apply a single perk's modifiers
     */
    private void applyPerkModifiers(FactionPerk perk, Map<String, Double> modifiers) {
        switch (perk) {
            // Power Efficiency
            case POWER_EFFICIENCY_1 -> modifiers.put("powerEfficiency", 0.85);
            case POWER_EFFICIENCY_2 -> modifiers.put("powerEfficiency", 0.70);
            case POWER_EFFICIENCY_3 -> modifiers.put("powerEfficiency", 0.55);
            
            // Resource Boost
            case RESOURCE_BOOST_1 -> modifiers.put("harvestRate", 1.15);
            case RESOURCE_BOOST_2 -> modifiers.put("harvestRate", 1.30);
            
            // Cost Reduction
            case COST_REDUCTION_1 -> {
                modifiers.put("unitCost", 0.90);
                modifiers.put("buildingCost", 0.90);
            }
            case COST_REDUCTION_2 -> {
                modifiers.put("unitCost", 0.80);
                modifiers.put("buildingCost", 0.80);
            }
            
            // Upkeep Increase
            case UPKEEP_INCREASE_1 -> modifiers.put("upkeep", 1.25);
            case UPKEEP_INCREASE_2 -> modifiers.put("upkeep", 1.50);
            case UPKEEP_INCREASE_3 -> modifiers.put("upkeep", 1.75);
            
            // Veteran Units
            case VETERAN_UNITS_1 -> modifiers.put("unitHealth", 1.10);
            case VETERAN_UNITS_2 -> modifiers.put("unitHealth", 1.20);
            
            // Rapid Deployment
            case RAPID_DEPLOYMENT_1 -> modifiers.put("buildTime", 0.85);
            case RAPID_DEPLOYMENT_2 -> modifiers.put("buildTime", 0.70);
            
            // Damage Boost
            case DAMAGE_BOOST_1 -> modifiers.put("unitDamage", 1.10);
            case DAMAGE_BOOST_2 -> modifiers.put("unitDamage", 1.20);
            
            // Fortified Structures
            case FORTIFIED_1 -> modifiers.put("buildingHealth", 1.15);
            case FORTIFIED_2 -> modifiers.put("buildingHealth", 1.30);
            
            // Advanced Research
            case ADVANCED_RESEARCH_1 -> modifiers.put("researchSpeed", 1.20);
            case ADVANCED_RESEARCH_2 -> modifiers.put("researchSpeed", 1.40);
            
            // Logistics Network
            case LOGISTICS_NETWORK -> {
                double currentBuildingCost = modifiers.get("buildingCost");
                double currentBuildTime = modifiers.get("buildTime");
                modifiers.put("buildingCost", currentBuildingCost * 0.85);
                modifiers.put("buildTime", currentBuildTime * 0.80);
            }
            
            // Other perks don't have direct modifiers or are handled per-unit
        }
    }
    
    /**
     * Calculate per-unit cost modifiers
     */
    private Map<UnitType, Double> calculateUnitCostModifiers(CustomFactionConfig config) {
        Map<UnitType, Double> costMods = new HashMap<>();
        
        for (FactionPerk perk : config.getSelectedPerks()) {
            switch (perk) {
                case AIR_SUPERIORITY_1 -> {
                    for (UnitType unit : config.getSelectedUnits()) {
                        if (unit.getCategory() == com.fullsteam.model.UnitCategory.FLYER) {
                            costMods.put(unit, 0.85);
                        }
                    }
                }
                case AIR_SUPERIORITY_2 -> {
                    for (UnitType unit : config.getSelectedUnits()) {
                        if (unit.getCategory() == com.fullsteam.model.UnitCategory.FLYER) {
                            costMods.put(unit, 0.75);
                        }
                    }
                }
                case MECHANIZED_WARFARE_1 -> {
                    for (UnitType unit : config.getSelectedUnits()) {
                        if (unit.getCategory() == com.fullsteam.model.UnitCategory.VEHICLE) {
                            costMods.put(unit, 0.85);
                        }
                    }
                }
                case MECHANIZED_WARFARE_2 -> {
                    for (UnitType unit : config.getSelectedUnits()) {
                        if (unit.getCategory() == com.fullsteam.model.UnitCategory.VEHICLE) {
                            costMods.put(unit, 0.75);
                        }
                    }
                }
                case INFANTRY_DOCTRINE_1 -> {
                    for (UnitType unit : config.getSelectedUnits()) {
                        if (unit.getCategory() == com.fullsteam.model.UnitCategory.INFANTRY) {
                            costMods.put(unit, 0.85);
                        }
                    }
                }
                case INFANTRY_DOCTRINE_2 -> {
                    for (UnitType unit : config.getSelectedUnits()) {
                        if (unit.getCategory() == com.fullsteam.model.UnitCategory.INFANTRY) {
                            costMods.put(unit, 0.75);
                        }
                    }
                }
            }
        }
        
        return costMods;
    }
    
    /**
     * Calculate per-unit stat modifiers
     */
    private Map<UnitType, FactionDefinition.UnitStatModifier> calculateUnitStatModifiers(CustomFactionConfig config) {
        Map<UnitType, FactionDefinition.UnitStatModifier> statMods = new HashMap<>();
        
        for (FactionPerk perk : config.getSelectedPerks()) {
            switch (perk) {
                case AIR_SUPERIORITY_1 -> {
                    for (UnitType unit : config.getSelectedUnits()) {
                        if (unit.getCategory() == com.fullsteam.model.UnitCategory.FLYER) {
                            statMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                                .speedMultiplier(1.10)
                                .build());
                        }
                    }
                }
                case AIR_SUPERIORITY_2 -> {
                    for (UnitType unit : config.getSelectedUnits()) {
                        if (unit.getCategory() == com.fullsteam.model.UnitCategory.FLYER) {
                            statMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                                .speedMultiplier(1.20)
                                .build());
                        }
                    }
                }
                case MECHANIZED_WARFARE_1 -> {
                    for (UnitType unit : config.getSelectedUnits()) {
                        if (unit.getCategory() == com.fullsteam.model.UnitCategory.VEHICLE) {
                            statMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                                .healthMultiplier(1.10)
                                .build());
                        }
                    }
                }
                case MECHANIZED_WARFARE_2 -> {
                    for (UnitType unit : config.getSelectedUnits()) {
                        if (unit.getCategory() == com.fullsteam.model.UnitCategory.VEHICLE) {
                            statMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                                .healthMultiplier(1.20)
                                .build());
                        }
                    }
                }
                case INFANTRY_DOCTRINE_1 -> {
                    for (UnitType unit : config.getSelectedUnits()) {
                        if (unit.getCategory() == com.fullsteam.model.UnitCategory.INFANTRY) {
                            statMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                                .damageMultiplier(1.10)
                                .build());
                        }
                    }
                }
                case INFANTRY_DOCTRINE_2 -> {
                    for (UnitType unit : config.getSelectedUnits()) {
                        if (unit.getCategory() == com.fullsteam.model.UnitCategory.INFANTRY) {
                            statMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                                .damageMultiplier(1.20)
                                .build());
                        }
                    }
                }
            }
        }
        
        return statMods;
    }
    
    /**
     * Calculate per-building stat modifiers
     */
    private Map<BuildingType, FactionDefinition.BuildingStatModifier> calculateBuildingStatModifiers(CustomFactionConfig config) {
        Map<BuildingType, FactionDefinition.BuildingStatModifier> statMods = new HashMap<>();
        
        // Currently no perks modify specific buildings beyond global modifiers
        // Future perks could add building-specific bonuses here
        
        return statMods;
    }
}
