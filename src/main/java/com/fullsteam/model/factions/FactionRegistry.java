package com.fullsteam.model.factions;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry for all faction definitions.
 * This is where faction tech trees and bonuses are configured.
 */
public class FactionRegistry {

    private static final Map<Faction, FactionDefinition> FACTION_DEFINITIONS = new HashMap<>();

    static {
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
     * TERRAN - Balanced faction with all standard units
     */
    private static FactionDefinition createTerranDefinition() {
        FactionTechTree techTree = FactionTechTree.builder()
                .buildingsAndUnits(Map.ofEntries(
                        Map.entry(BuildingType.HEADQUARTERS, List.of(
                                UnitType.WORKER,
                                UnitType.MINER
                        )),
                        Map.entry(BuildingType.POWER_PLANT, List.of()),
                        Map.entry(BuildingType.BARRACKS, List.of(
                                UnitType.INFANTRY,
                                UnitType.MEDIC
                        )),
                        Map.entry(BuildingType.REFINERY, List.of()),
                        Map.entry(BuildingType.WALL, List.of()),
                        Map.entry(BuildingType.RESEARCH_LAB, List.of()),
                        Map.entry(BuildingType.FACTORY, List.of(
                                UnitType.JEEP,
                                UnitType.TANK,
                                UnitType.ARTILLERY,
                                UnitType.FLAK_TANK
                        )),
                        Map.entry(BuildingType.WEAPONS_DEPOT, List.of(
                                UnitType.ROCKET_SOLDIER,
                                UnitType.SNIPER,
                                UnitType.ENGINEER
                        )),
                        Map.entry(BuildingType.TURRET, List.of()),
                        Map.entry(BuildingType.ROCKET_TURRET, List.of()),
                        Map.entry(BuildingType.BUNKER, List.of()),
                        Map.entry(BuildingType.SHIELD_GENERATOR, List.of()),
                        Map.entry(BuildingType.TECH_CENTER, List.of()),
                        Map.entry(BuildingType.ADVANCED_FACTORY, List.of(
                                UnitType.GIGANTONAUT,
                                UnitType.CLOAK_TANK,
                                UnitType.CRAWLER
                        )),
                        Map.entry(BuildingType.AIRFIELD, List.of(
                                UnitType.SCOUT_DRONE,
                                UnitType.HELICOPTER
                        )),
                        Map.entry(BuildingType.HANGAR, List.of(
                                UnitType.BOMBER,
                                UnitType.INTERCEPTOR
                        )),
                        Map.entry(BuildingType.BANK, List.of()),
                        Map.entry(BuildingType.COMMAND_CITADEL, List.of())
                ))
                .build();

        return FactionDefinition.builder()
                .faction(Faction.TERRAN)
                .techTree(techTree)
                .heroUnit(UnitType.CRAWLER)
                .monumentBuilding(BuildingType.COMMAND_CITADEL)
                .buildingHealthMultiplier(1.1)
                .build();
    }

    /**
     * NOMADS - Mobile warfare faction
     */
    private static FactionDefinition createNomadsDefinition() {
        FactionTechTree techTree = FactionTechTree.builder()
                .buildingsAndUnits(Map.ofEntries(
                        Map.entry(BuildingType.HEADQUARTERS, List.of(
                                UnitType.WORKER,
                                UnitType.MINER
                        )),
                        Map.entry(BuildingType.POWER_PLANT, List.of()),
                        Map.entry(BuildingType.BARRACKS, List.of(
                                UnitType.INFANTRY,
                                UnitType.MEDIC
                        )),
                        Map.entry(BuildingType.REFINERY, List.of()),
                        Map.entry(BuildingType.WALL, List.of()),
                        Map.entry(BuildingType.RESEARCH_LAB, List.of()),
                        Map.entry(BuildingType.FACTORY, List.of(
                                UnitType.JEEP,
                                UnitType.TANK,
                                UnitType.FLAK_TANK
                        )),
                        Map.entry(BuildingType.WEAPONS_DEPOT, List.of(
                                UnitType.ROCKET_SOLDIER,
                                UnitType.SNIPER,
                                UnitType.ENGINEER
                        )),
                        Map.entry(BuildingType.TURRET, List.of()),
                        Map.entry(BuildingType.ROCKET_TURRET, List.of()),
                        Map.entry(BuildingType.BUNKER, List.of()),
                        Map.entry(BuildingType.SHIELD_GENERATOR, List.of()),
                        Map.entry(BuildingType.TECH_CENTER, List.of()),
                        Map.entry(BuildingType.ADVANCED_FACTORY, List.of(
                                UnitType.CLOAK_TANK,
                                UnitType.RAIDER
                        )),
                        Map.entry(BuildingType.BANK, List.of()),
                        Map.entry(BuildingType.SANDSTORM_GENERATOR, List.of())
                ))
                .build();

        // Nomad-specific unit cost modifiers (vehicles cheaper)
        Map<UnitType, Double> costModifiers = new HashMap<>();
        costModifiers.put(UnitType.JEEP, 0.8);  // -20%
        costModifiers.put(UnitType.TANK, 0.8);  // -20%
        costModifiers.put(UnitType.CLOAK_TANK, 0.8);  // -20%

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
        FactionTechTree techTree = FactionTechTree.builder()
                .buildingsAndUnits(Map.ofEntries(
                        Map.entry(BuildingType.HEADQUARTERS, List.of(
                                UnitType.WORKER,
                                UnitType.MINER
                        )),
                        Map.entry(BuildingType.POWER_PLANT, List.of()),
                        Map.entry(BuildingType.REFINERY, List.of()),
                        Map.entry(BuildingType.WALL, List.of()),
                        Map.entry(BuildingType.RESEARCH_LAB, List.of()),
                        Map.entry(BuildingType.FACTORY, List.of(
                                UnitType.TANK,
                                UnitType.ARTILLERY,
                                UnitType.FLAK_TANK
                        )),
                        Map.entry(BuildingType.WEAPONS_DEPOT, List.of(
                                UnitType.ROCKET_SOLDIER,
                                UnitType.SNIPER,
                                UnitType.ENGINEER
                        )),
                        Map.entry(BuildingType.TURRET, List.of()),
                        Map.entry(BuildingType.LASER_TURRET, List.of()),
                        Map.entry(BuildingType.BUNKER, List.of()),
                        Map.entry(BuildingType.SHIELD_GENERATOR, List.of()),
                        Map.entry(BuildingType.TECH_CENTER, List.of()),
                        Map.entry(BuildingType.ADVANCED_FACTORY, List.of(
                                UnitType.GIGANTONAUT,
                                UnitType.CLOAK_TANK,
                                UnitType.COLOSSUS
                        )),
                        Map.entry(BuildingType.AIRFIELD, List.of(
                                UnitType.SCOUT_DRONE,
                                UnitType.HELICOPTER
                        )),
                        Map.entry(BuildingType.HANGAR, List.of(
                                UnitType.BOMBER,
                                UnitType.INTERCEPTOR
                        )),
                        Map.entry(BuildingType.BANK, List.of()),
                        Map.entry(BuildingType.ANDROID_FACTORY, List.of(UnitType.ANDROID))
                ))
                .build();

        return FactionDefinition.builder()
                .faction(Faction.SYNTHESIS)
                .techTree(techTree)
                .heroUnit(UnitType.COLOSSUS)
                .monumentBuilding(BuildingType.ANDROID_FACTORY)
                .powerEfficiencyMultiplier(0.7)  // -30% power consumption
                .unitCostMultiplier(1.3)  // +30% unit costs
                .buildingHealthMultiplier(1.15)  // +15% building health
                .build();
    }

    /**
     * TECH ALLIANCE - High-tech faction specializing in beam weapons
     */
    private static FactionDefinition createTechAllianceDefinition() {
        FactionTechTree techTree = FactionTechTree.builder()
                .buildingsAndUnits(Map.ofEntries(
                        Map.entry(BuildingType.HEADQUARTERS, List.of(
                                UnitType.WORKER,
                                UnitType.MINER
                        )),
                        Map.entry(BuildingType.POWER_PLANT, List.of()),
                        Map.entry(BuildingType.REFINERY, List.of()),
                        Map.entry(BuildingType.BARRACKS, List.of(
                                UnitType.PLASMA_TROOPER,
                                UnitType.MEDIC
                        )),
                        Map.entry(BuildingType.FACTORY, List.of(
                                UnitType.PHOTON_SCOUT,
                                UnitType.BEAM_TANK
                        )),
                        Map.entry(BuildingType.TURRET, List.of()),
                        Map.entry(BuildingType.LASER_TURRET, List.of()),
                        Map.entry(BuildingType.BUNKER, List.of()),
                        Map.entry(BuildingType.WEAPONS_DEPOT, List.of(
                                UnitType.ION_RANGER,
                                UnitType.ENGINEER
                        )),
                        Map.entry(BuildingType.ADVANCED_FACTORY, List.of(
                                UnitType.PULSE_ARTILLERY,
                                UnitType.PHOTON_TITAN
                        )),
                        Map.entry(BuildingType.AIRFIELD, List.of(
                                UnitType.SCOUT_DRONE,
                                UnitType.HELICOPTER
                        )),
                        Map.entry(BuildingType.HANGAR, List.of(
                                UnitType.BOMBER,
                                UnitType.INTERCEPTOR
                        )),
                        Map.entry(BuildingType.SHIELD_GENERATOR, List.of()),
                        Map.entry(BuildingType.BANK, List.of()),
                        Map.entry(BuildingType.RESEARCH_LAB, List.of()),
                        Map.entry(BuildingType.TECH_CENTER, List.of()),
                        Map.entry(BuildingType.WALL, List.of()),
                        Map.entry(BuildingType.PHOTON_SPIRE, List.of())
                ))
                .build();

        return FactionDefinition.builder()
                .faction(Faction.TECH_ALLIANCE)
                .techTree(techTree)
                .heroUnit(UnitType.PHOTON_TITAN)
                .monumentBuilding(BuildingType.PHOTON_SPIRE)
                .unitCostMultiplier(1.15)  // +15% unit costs (advanced tech)
                .buildingCostMultiplier(0.9)  // -10% building costs
                .powerEfficiencyMultiplier(1.2)  // +20% power consumption (high-tech energy weapons)
                .build();
    }
}
