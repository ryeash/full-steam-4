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
        FACTION_DEFINITIONS.put(Faction.TECH_ALLIANCE, createTechAllianceDefinition());
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
        // Terran has access to all standard buildings (explicit list)
        // Note: HEADQUARTERS excluded - it's a starting building only, cannot be built
        Set<BuildingType> terranBuildings = Set.of(
                BuildingType.POWER_PLANT,
                BuildingType.BARRACKS,
                BuildingType.REFINERY,
                BuildingType.WALL,
                BuildingType.RESEARCH_LAB,
                BuildingType.FACTORY,
                BuildingType.WEAPONS_DEPOT,
                BuildingType.TURRET,
                BuildingType.SHIELD_GENERATOR,
                BuildingType.TECH_CENTER,
                BuildingType.ADVANCED_FACTORY,
                BuildingType.BANK,
                BuildingType.BUNKER  // Terran monument
        );
        
        // Terran has access to all standard units (explicit list)
        Map<BuildingType, List<UnitType>> buildingProducers = new HashMap<>();
        buildingProducers.put(BuildingType.HEADQUARTERS, List.of(UnitType.WORKER, UnitType.MINER));
        buildingProducers.put(BuildingType.BARRACKS, List.of(UnitType.INFANTRY, UnitType.MEDIC, UnitType.LASER_INFANTRY));
        buildingProducers.put(BuildingType.FACTORY, List.of(UnitType.JEEP, UnitType.TANK));
        buildingProducers.put(BuildingType.WEAPONS_DEPOT, List.of(UnitType.ROCKET_SOLDIER, UnitType.SNIPER, UnitType.ENGINEER));
        buildingProducers.put(BuildingType.ADVANCED_FACTORY, List.of(
                UnitType.ARTILLERY, 
                UnitType.GIGANTONAUT, 
                UnitType.CRAWLER, 
                UnitType.STEALTH_TANK, 
                UnitType.MAMMOTH_TANK,
                UnitType.PALADIN  // Terran hero
        ));
        
        FactionTechTree techTree = FactionTechTree.builder()
                .availableBuildings(terranBuildings)
                .buildingProducers(buildingProducers)
                .buildingTechTiers(new HashMap<>())
                .build();

        return FactionDefinition.builder()
                .faction(Faction.TERRAN)
                .techTree(techTree)
                .heroUnit(UnitType.PALADIN)
                .monumentBuilding(BuildingType.BUNKER)
                .buildingHealthMultiplier(1.1)  // +10% building health
                .build();
    }

    /**
     * NOMADS - Mobile warfare faction
     */
    private static FactionDefinition createNomadsDefinition() {
        // Nomads have all standard buildings (explicit list)
        // Note: HEADQUARTERS excluded - it's a starting building only, cannot be built
        Set<BuildingType> nomadBuildings = Set.of(
                BuildingType.POWER_PLANT,
                BuildingType.BARRACKS,
                BuildingType.REFINERY,
                BuildingType.WALL,
                BuildingType.RESEARCH_LAB,
                BuildingType.FACTORY,
                BuildingType.WEAPONS_DEPOT,
                BuildingType.TURRET,
                BuildingType.SHIELD_GENERATOR,
                BuildingType.TECH_CENTER,
                BuildingType.ADVANCED_FACTORY,
                BuildingType.BANK,
                BuildingType.SANDSTORM_GENERATOR  // Nomads monument
        );

        // Nomads focus on light, fast units - no heavy vehicles
        Map<BuildingType, List<UnitType>> buildingProducers = new HashMap<>();
        buildingProducers.put(BuildingType.HEADQUARTERS, List.of(UnitType.WORKER, UnitType.MINER));
        buildingProducers.put(BuildingType.BARRACKS, List.of(UnitType.INFANTRY, UnitType.MEDIC));
        buildingProducers.put(BuildingType.FACTORY, List.of(UnitType.JEEP, UnitType.TANK));
        buildingProducers.put(BuildingType.WEAPONS_DEPOT, List.of(UnitType.ROCKET_SOLDIER, UnitType.SNIPER, UnitType.ENGINEER));
        buildingProducers.put(BuildingType.ADVANCED_FACTORY, List.of(
                UnitType.CRAWLER, 
                UnitType.STEALTH_TANK, 
                UnitType.RAIDER  // Nomads hero
        ));

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
                .heroUnit(UnitType.RAIDER)
                .monumentBuilding(BuildingType.SANDSTORM_GENERATOR)
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
        // Synthesis has advanced buildings, no barracks (explicit list)
        // Note: HEADQUARTERS excluded - it's a starting building only, cannot be built
        Set<BuildingType> synthesisBuildings = Set.of(
                BuildingType.POWER_PLANT,
                BuildingType.REFINERY,
                BuildingType.WALL,
                BuildingType.RESEARCH_LAB,
                BuildingType.FACTORY,
                BuildingType.WEAPONS_DEPOT,
                BuildingType.TURRET,
                BuildingType.SHIELD_GENERATOR,
                BuildingType.TECH_CENTER,
                BuildingType.ADVANCED_FACTORY,
                BuildingType.BANK,
                BuildingType.QUANTUM_NEXUS  // Synthesis monument
        );

        // Synthesis focuses on advanced, heavy units - no basic infantry
        Map<BuildingType, List<UnitType>> buildingProducers = new HashMap<>();
        buildingProducers.put(BuildingType.HEADQUARTERS, List.of(UnitType.WORKER, UnitType.MINER));
        buildingProducers.put(BuildingType.FACTORY, List.of(UnitType.TANK));  // No Jeep
        buildingProducers.put(BuildingType.WEAPONS_DEPOT, List.of(UnitType.ROCKET_SOLDIER, UnitType.SNIPER, UnitType.ENGINEER));
        buildingProducers.put(BuildingType.ADVANCED_FACTORY, List.of(
                UnitType.ARTILLERY, 
                UnitType.GIGANTONAUT, 
                UnitType.CRAWLER, 
                UnitType.STEALTH_TANK, 
                UnitType.MAMMOTH_TANK, 
                UnitType.COLOSSUS  // Synthesis hero
        ));

        FactionTechTree techTree = FactionTechTree.builder()
                .availableBuildings(synthesisBuildings)
                .buildingProducers(buildingProducers)
                .buildingTechTiers(new HashMap<>())
                .build();

        return FactionDefinition.builder()
                .faction(Faction.SYNTHESIS)
                .techTree(techTree)
                .heroUnit(UnitType.COLOSSUS)
                .monumentBuilding(BuildingType.QUANTUM_NEXUS)
                .powerEfficiencyMultiplier(0.7)  // -30% power consumption
                .unitCostMultiplier(1.3)  // +30% unit costs
                .buildingHealthMultiplier(1.15)  // +15% building health
                .build();
    }
    
    /**
     * TECH ALLIANCE - High-tech faction specializing in beam weapons
     */
    private static FactionDefinition createTechAllianceDefinition() {
        // Tech Alliance has standard buildings (explicit list)
        // Note: HEADQUARTERS excluded - it's a starting building only, cannot be built
        Set<BuildingType> techAllianceBuildings = Set.of(
                BuildingType.POWER_PLANT,
                BuildingType.REFINERY,
                BuildingType.BARRACKS,
                BuildingType.FACTORY,
                BuildingType.TURRET,
                BuildingType.WEAPONS_DEPOT,
                BuildingType.ADVANCED_FACTORY,
                BuildingType.SHIELD_GENERATOR,
                BuildingType.BANK,
                BuildingType.RESEARCH_LAB,
                BuildingType.TECH_CENTER,
                BuildingType.WALL,
                BuildingType.PHOTON_SPIRE  // Tech Alliance monument
        );
        
        // Tech Alliance uses beam weapons exclusively (explicit list)
        Map<BuildingType, List<UnitType>> buildingProducers = new HashMap<>();
        buildingProducers.put(BuildingType.HEADQUARTERS, List.of(UnitType.WORKER, UnitType.MINER));
        buildingProducers.put(BuildingType.BARRACKS, List.of(UnitType.PLASMA_TROOPER, UnitType.MEDIC));
        buildingProducers.put(BuildingType.WEAPONS_DEPOT, List.of(UnitType.ION_RANGER, UnitType.ENGINEER));
        buildingProducers.put(BuildingType.FACTORY, List.of(UnitType.PHOTON_SCOUT, UnitType.BEAM_TANK));
        buildingProducers.put(BuildingType.ADVANCED_FACTORY, List.of(
                UnitType.PULSE_ARTILLERY, 
                UnitType.PHOTON_TITAN,  // Tech Alliance hero
                UnitType.CRAWLER
        ));
        
        FactionTechTree techTree = FactionTechTree.builder()
                .availableBuildings(techAllianceBuildings)
                .buildingProducers(buildingProducers)
                .buildingTechTiers(new HashMap<>())
                .build();
        
        return FactionDefinition.builder()
                .faction(Faction.TECH_ALLIANCE)
                .techTree(techTree)
                .heroUnit(UnitType.PHOTON_TITAN)
                .monumentBuilding(BuildingType.PHOTON_SPIRE)
                .unitCostMultiplier(1.15)  // +15% unit costs (advanced tech)
                .buildingCostMultiplier(0.9)  // -10% building costs
                .powerEfficiencyMultiplier(0.8)  // -20% power consumption
                .build();
    }
}
