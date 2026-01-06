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
        FACTION_DEFINITIONS.put(Faction.STORM_WINGS, createStormWingsDefinition());
    }

    /**
     * Get the definition for a faction
     * Note: CUSTOM faction definitions are created dynamically and not stored here
     */
    public static FactionDefinition getDefinition(Faction faction) {
        if (faction == Faction.CUSTOM) {
            // Custom factions are built dynamically, not stored in registry
            // Return a default/empty definition for CUSTOM
            // The actual custom definition will be applied via PlayerFaction.applyCustomFaction()
            return createEmptyCustomDefinition();
        }
        return FACTION_DEFINITIONS.get(faction);
    }
    
    /**
     * Create an empty definition for CUSTOM faction (placeholder)
     */
    private static FactionDefinition createEmptyCustomDefinition() {
        FactionTechTree emptyTechTree = FactionTechTree.builder()
            .buildingsAndUnits(Map.of())
            .build();
        
        return FactionDefinition.builder()
            .faction(Faction.CUSTOM)
            .techTree(emptyTechTree)
            .heroUnit(null)
            .monumentBuilding(null)
            .build();
    }

    /**
     * TERRAN - Balanced faction with all standard units
     * Simplified: Research system removed, unit lists no longer needed
     */
    private static FactionDefinition createTerranDefinition() {
        // All standard buildings available (empty lists = no preset units, handled by custom system)
        FactionTechTree techTree = FactionTechTree.builder()
                .buildingsAndUnits(Map.ofEntries(
                        Map.entry(BuildingType.HEADQUARTERS, List.of()),
                        Map.entry(BuildingType.POWER_PLANT, List.of()),
                        Map.entry(BuildingType.BARRACKS, List.of()),
                        Map.entry(BuildingType.REFINERY, List.of()),
                        Map.entry(BuildingType.WALL, List.of()),
                        Map.entry(BuildingType.RESEARCH_LAB, List.of()),
                        Map.entry(BuildingType.FACTORY, List.of()),
                        Map.entry(BuildingType.TURRET, List.of()),
                        Map.entry(BuildingType.ROCKET_TURRET, List.of()),
                        Map.entry(BuildingType.BUNKER, List.of()),
                        Map.entry(BuildingType.SHIELD_GENERATOR, List.of()),
                        Map.entry(BuildingType.TECH_CENTER, List.of()),
                        Map.entry(BuildingType.AIRFIELD, List.of()),
                        Map.entry(BuildingType.HANGAR, List.of()),
                        Map.entry(BuildingType.BANK, List.of()),
                        Map.entry(BuildingType.COMMAND_CITADEL, List.of())
                ))
                .build();

        return FactionDefinition.builder()
                .faction(Faction.TERRAN)
                .techTree(techTree)
                .heroUnit(UnitType.CRAWLER)
                .monumentBuilding(BuildingType.COMMAND_CITADEL)
                .buildingHealthMultiplier(1.1)  // +10% building health
                .build();
    }

    /**
     * NOMADS - Mobile warfare faction
     * Simplified: Research system removed, focuses on vehicle bonuses
     */
    private static FactionDefinition createNomadsDefinition() {
        // Standard buildings (no air units - Nomads are ground-focused)
        FactionTechTree techTree = FactionTechTree.builder()
                .buildingsAndUnits(Map.ofEntries(
                        Map.entry(BuildingType.HEADQUARTERS, List.of()),
                        Map.entry(BuildingType.POWER_PLANT, List.of()),
                        Map.entry(BuildingType.BARRACKS, List.of()),
                        Map.entry(BuildingType.REFINERY, List.of()),
                        Map.entry(BuildingType.WALL, List.of()),
                        Map.entry(BuildingType.RESEARCH_LAB, List.of()),
                        Map.entry(BuildingType.FACTORY, List.of()),
                        Map.entry(BuildingType.TURRET, List.of()),
                        Map.entry(BuildingType.ROCKET_TURRET, List.of()),
                        Map.entry(BuildingType.BUNKER, List.of()),
                        Map.entry(BuildingType.SHIELD_GENERATOR, List.of()),
                        Map.entry(BuildingType.TECH_CENTER, List.of()),
                        Map.entry(BuildingType.BANK, List.of()),
                        Map.entry(BuildingType.SANDSTORM_GENERATOR, List.of())
                ))
                .build();

        // Nomad-specific vehicle bonuses
        Map<UnitType, Double> costModifiers = new HashMap<>();
        costModifiers.put(UnitType.JEEP, 0.8);  // -20% cost
        costModifiers.put(UnitType.TANK, 0.8);  // -20% cost
        costModifiers.put(UnitType.CLOAK_TANK, 0.8);  // -20% cost

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
                .buildingHealthMultiplier(0.8)  // -20% building health (mobile, not defensive)
                .unitCostModifiers(costModifiers)
                .unitStatModifiers(statModifiers)
                .build();
    }

    /**
     * SYNTHESIS - Advanced technology faction
     * Simplified: Research system removed, focuses on high-tech bonuses
     */
    private static FactionDefinition createSynthesisDefinition() {
        // High-tech buildings (no barracks - uses androids)
        FactionTechTree techTree = FactionTechTree.builder()
                .buildingsAndUnits(Map.ofEntries(
                        Map.entry(BuildingType.HEADQUARTERS, List.of()),
                        Map.entry(BuildingType.POWER_PLANT, List.of()),
                        Map.entry(BuildingType.REFINERY, List.of()),
                        Map.entry(BuildingType.WALL, List.of()),
                        Map.entry(BuildingType.RESEARCH_LAB, List.of()),
                        Map.entry(BuildingType.FACTORY, List.of()),
                        Map.entry(BuildingType.TURRET, List.of()),
                        Map.entry(BuildingType.LASER_TURRET, List.of()),
                        Map.entry(BuildingType.BUNKER, List.of()),
                        Map.entry(BuildingType.SHIELD_GENERATOR, List.of()),
                        Map.entry(BuildingType.TECH_CENTER, List.of()),
                        Map.entry(BuildingType.AIRFIELD, List.of()),
                        Map.entry(BuildingType.HANGAR, List.of()),
                        Map.entry(BuildingType.BANK, List.of()),
                        Map.entry(BuildingType.ANDROID_FACTORY, List.of())
                ))
                .build();

        return FactionDefinition.builder()
                .faction(Faction.SYNTHESIS)
                .techTree(techTree)
                .heroUnit(UnitType.COLOSSUS)
                .monumentBuilding(BuildingType.ANDROID_FACTORY)
                .powerEfficiencyMultiplier(0.7)  // -30% power consumption (efficient tech)
                .unitCostMultiplier(1.3)  // +30% unit costs (expensive advanced units)
                .buildingHealthMultiplier(1.15)  // +15% building health (durable structures)
                .build();
    }

    /**
     * TECH ALLIANCE - High-tech faction specializing in beam weapons
     * Simplified: Research system removed, focuses on advanced tech bonuses
     */
    private static FactionDefinition createTechAllianceDefinition() {
        // Beam weapon-focused buildings
        FactionTechTree techTree = FactionTechTree.builder()
                .buildingsAndUnits(Map.ofEntries(
                        Map.entry(BuildingType.HEADQUARTERS, List.of()),
                        Map.entry(BuildingType.POWER_PLANT, List.of()),
                        Map.entry(BuildingType.REFINERY, List.of()),
                        Map.entry(BuildingType.BARRACKS, List.of()),
                        Map.entry(BuildingType.FACTORY, List.of()),
                        Map.entry(BuildingType.TURRET, List.of()),
                        Map.entry(BuildingType.LASER_TURRET, List.of()),
                        Map.entry(BuildingType.BUNKER, List.of()),
                        Map.entry(BuildingType.AIRFIELD, List.of()),
                        Map.entry(BuildingType.HANGAR, List.of()),
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
                .powerEfficiencyMultiplier(1.2)  // +20% power consumption (energy weapons)
                .build();
    }

    /**
     * STORM_WINGS - Air superiority faction focused on aircraft dominance
     * Simplified: Research system removed, focuses on air unit bonuses
     */
    private static FactionDefinition createStormWingsDefinition() {
        // Air-focused buildings (limited ground options)
        FactionTechTree techTree = FactionTechTree.builder()
                .buildingsAndUnits(Map.ofEntries(
                        Map.entry(BuildingType.HEADQUARTERS, List.of()),
                        Map.entry(BuildingType.POWER_PLANT, List.of()),
                        Map.entry(BuildingType.REFINERY, List.of()),
                        Map.entry(BuildingType.WALL, List.of()),
                        Map.entry(BuildingType.BARRACKS, List.of()),
                        Map.entry(BuildingType.FACTORY, List.of()),
                        Map.entry(BuildingType.RESEARCH_LAB, List.of()),
                        Map.entry(BuildingType.TECH_CENTER, List.of()),
                        Map.entry(BuildingType.TURRET, List.of()),
                        Map.entry(BuildingType.ROCKET_TURRET, List.of()),
                        Map.entry(BuildingType.BUNKER, List.of()),
                        Map.entry(BuildingType.SHIELD_GENERATOR, List.of()),
                        Map.entry(BuildingType.AIRFIELD, List.of()),
                        Map.entry(BuildingType.HANGAR, List.of()),
                        Map.entry(BuildingType.BANK, List.of()),
                        Map.entry(BuildingType.TEMPEST_SPIRE, List.of())
                ))
                .build();

        // Storm Wings air unit bonuses
        Map<UnitType, Double> costModifiers = new HashMap<>();
        costModifiers.put(UnitType.SCOUT_DRONE, 0.7);     // -30% cost
        costModifiers.put(UnitType.HELICOPTER, 0.8);      // -20% cost
        costModifiers.put(UnitType.BOMBER, 0.85);         // -15% cost
        costModifiers.put(UnitType.INTERCEPTOR, 0.85);    // -15% cost
        costModifiers.put(UnitType.GUNSHIP, 0.9);         // -10% cost (hero)

        Map<UnitType, FactionDefinition.UnitStatModifier> statModifiers = new HashMap<>();
        statModifiers.put(UnitType.SCOUT_DRONE, FactionDefinition.UnitStatModifier.builder()
                .speedMultiplier(1.25)   // +25% speed
                .build());
        statModifiers.put(UnitType.HELICOPTER, FactionDefinition.UnitStatModifier.builder()
                .speedMultiplier(1.2)    // +20% speed
                .damageMultiplier(1.15)  // +15% damage
                .build());
        statModifiers.put(UnitType.INTERCEPTOR, FactionDefinition.UnitStatModifier.builder()
                .speedMultiplier(1.15)   // +15% speed
                .build());
        statModifiers.put(UnitType.GUNSHIP, FactionDefinition.UnitStatModifier.builder()
                .healthMultiplier(1.2)   // +20% health (hero durability)
                .build());

        return FactionDefinition.builder()
                .faction(Faction.STORM_WINGS)
                .techTree(techTree)
                .heroUnit(UnitType.GUNSHIP)
                .monumentBuilding(BuildingType.TEMPEST_SPIRE)
                .unitCostMultiplier(1.1)           // +10% ground unit costs (discourages ground)
                .buildingCostMultiplier(0.9)       // -10% building costs
                .upkeepMultiplier(0.9)             // -10% upkeep (more air units!)
                .unitCostModifiers(costModifiers)
                .unitStatModifiers(statModifiers)
                .build();
    }
}
