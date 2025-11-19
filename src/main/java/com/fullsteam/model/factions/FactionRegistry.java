package com.fullsteam.model.factions;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.Faction;
import com.fullsteam.model.FactionDefinition;
import com.fullsteam.model.FactionTechTree;
import com.fullsteam.model.UnitType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Central registry for all faction definitions.
 * This is where faction tech trees and bonuses are configured.
 */
public class FactionRegistry {

    private static final Map<Faction, FactionDefinition> FACTION_DEFINITIONS = new HashMap<>();

    static {
        // Initialize all faction definitions
        FACTION_DEFINITIONS.put(Faction.TERRAN, createTerranDefinition());
        FACTION_DEFINITIONS.put(Faction.NOMADS, createNomadsDefinition());
        FACTION_DEFINITIONS.put(Faction.SYNTHESIS, createSynthesisDefinition());
    }

    /**
     * Get the definition for a faction
     */
    public static FactionDefinition getDefinition(Faction faction) {
        return FACTION_DEFINITIONS.get(faction);
    }

    /**
     * Get all available factions
     */
    public static List<Faction> getAllFactions() {
        return Arrays.asList(Faction.values());
    }

    /**
     * TERRAN - Balanced faction with all standard units
     */
    private static FactionDefinition createTerranDefinition() {
        // Terran has access to everything - use the helper method
        FactionTechTree techTree = FactionTechTree.createAllAvailable();

        return FactionDefinition.builder()
                .faction(Faction.TERRAN)
                .techTree(techTree)
                .heroUnit(null) // TODO: Add hero units in Phase 3
                .monumentBuilding(null) // TODO: Add monuments in Phase 3
                // Terran bonus: +10% building health
                .buildingHealthMultiplier(1.1)
                .build();
    }

    /**
     * NOMADS - Mobile warfare faction
     */
    private static FactionDefinition createNomadsDefinition() {
        // Nomads have all buildings
        Set<BuildingType> nomadBuildings = Arrays.stream(BuildingType.values())
                .collect(Collectors.toSet());

        // Build buildingProducers map (which units each building can produce)
        // Nomads have most units but no heavy vehicles (MAMMOTH_TANK, ARTILLERY, GIGANTONAUT)
        // and no advanced tech like LASER_INFANTRY
        Map<BuildingType, List<UnitType>> buildingProducers = new HashMap<>();
        buildingProducers.put(BuildingType.HEADQUARTERS, Arrays.asList(UnitType.WORKER, UnitType.MINER));
        buildingProducers.put(BuildingType.BARRACKS, Arrays.asList(UnitType.INFANTRY, UnitType.MEDIC)); // No LASER_INFANTRY
        buildingProducers.put(BuildingType.FACTORY, Arrays.asList(UnitType.JEEP, UnitType.TANK));
        buildingProducers.put(BuildingType.WEAPONS_DEPOT, Arrays.asList(UnitType.ROCKET_SOLDIER, UnitType.SNIPER, UnitType.ENGINEER));
        buildingProducers.put(BuildingType.ADVANCED_FACTORY, Arrays.asList(UnitType.CRAWLER, UnitType.STEALTH_TANK));

        FactionTechTree techTree = FactionTechTree.builder()
                .availableBuildings(nomadBuildings)
                .buildingProducers(buildingProducers)
                .buildingTechTiers(new HashMap<>())
                .build();

        // Nomad-specific unit cost modifiers (vehicles cheaper)
        Map<UnitType, Double> costModifiers = new HashMap<>();
        costModifiers.put(UnitType.JEEP, 0.8);  // -20%
        costModifiers.put(UnitType.TANK, 0.8);  // -20%
        costModifiers.put(UnitType.STEALTH_TANK, 0.8);  // -20%
        costModifiers.put(UnitType.CRAWLER, 0.85);  // -15%

        // Nomad-specific unit stat modifiers (vehicles faster)
        Map<UnitType, FactionDefinition.UnitStatModifier> statModifiers = new HashMap<>();
        statModifiers.put(UnitType.JEEP, FactionDefinition.UnitStatModifier.builder()
                .speedMultiplier(1.2)  // +20% speed
                .build());
        statModifiers.put(UnitType.TANK, FactionDefinition.UnitStatModifier.builder()
                .speedMultiplier(1.15)  // +15% speed
                .build());

        return FactionDefinition.builder()
                .faction(Faction.NOMADS)
                .techTree(techTree)
                .heroUnit(null) // TODO: Phase 3
                .monumentBuilding(null) // TODO: Phase 3
                // Nomad bonuses/penalties
                .upkeepMultiplier(1.5)  // +50% upkeep limit
                .buildingHealthMultiplier(0.8)  // -20% building health
                .unitCostModifiers(costModifiers)
                .unitStatModifiers(statModifiers)
                .build();
    }

    /**
     * SYNTHESIS - Advanced technology faction
     */
    private static FactionDefinition createSynthesisDefinition() {
        // Synthesis has all buildings except the barracks
        Set<BuildingType> synthesisBuildings = Arrays.stream(BuildingType.values())
                .filter(b -> b != BuildingType.BARRACKS)
                .collect(Collectors.toSet());

        // Build buildingProducers map (which units each building can produce)
        // Synthesis has advanced units only (no basic infantry/medic, no jeep)
        Map<BuildingType, List<UnitType>> buildingProducers = new HashMap<>();
        buildingProducers.put(BuildingType.HEADQUARTERS, List.of(UnitType.WORKER, UnitType.MINER));
        buildingProducers.put(BuildingType.FACTORY, List.of(UnitType.TANK)); // No Jeep
        buildingProducers.put(BuildingType.WEAPONS_DEPOT, List.of(UnitType.ROCKET_SOLDIER, UnitType.SNIPER, UnitType.ENGINEER));
        buildingProducers.put(BuildingType.ADVANCED_FACTORY, List.of(UnitType.ARTILLERY, UnitType.GIGANTONAUT, UnitType.CRAWLER, UnitType.STEALTH_TANK, UnitType.MAMMOTH_TANK));

        FactionTechTree techTree = FactionTechTree.builder()
                .availableBuildings(synthesisBuildings)
                .buildingProducers(buildingProducers)
                .buildingTechTiers(new HashMap<>())
                .build();

        return FactionDefinition.builder()
                .faction(Faction.SYNTHESIS)
                .techTree(techTree)
                .heroUnit(null) // TODO: Phase 3
                .monumentBuilding(null) // TODO: Phase 3
                // Synthesis bonuses/penalties
                .powerEfficiencyMultiplier(0.7)  // -30% power consumption
                .unitCostMultiplier(1.3)  // +30% unit costs
                .buildingHealthMultiplier(1.15)  // +15% building health
                .build();
    }
}
