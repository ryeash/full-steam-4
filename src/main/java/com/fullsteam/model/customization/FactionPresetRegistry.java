package com.fullsteam.model.customization;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of preset faction configurations.
 * These serve as starting points for players to customize from.
 */
@Singleton
public class FactionPresetRegistry {

    private static final Map<String, CustomFactionConfig> PRESETS = new LinkedHashMap<>();

    static {
        // ===== TERRAN VARIANTS =====
        PRESETS.put("TERRAN_STANDARD", createTerranStandard());
        PRESETS.put("TERRAN_INFANTRY", createTerranInfantry());
        PRESETS.put("TERRAN_MECHANIZED", createTerranMechanized());

        // ===== NOMADS VARIANTS =====
        PRESETS.put("NOMADS_RAIDERS", createNomadsRaiders());
        PRESETS.put("NOMADS_GUERRILLA", createNomadsGuerrilla());

        // ===== SYNTHESIS VARIANTS =====
        PRESETS.put("SYNTHESIS_SHIELDED", createSynthesisShielded());
        PRESETS.put("SYNTHESIS_ANDROIDS", createSynthesisAndroids());

        // ===== TECH ALLIANCE VARIANTS =====
        PRESETS.put("TECH_ALLIANCE_BEAMS", createTechAllianceBeams());

        // ===== STORM WINGS VARIANTS =====
        PRESETS.put("STORM_WINGS_BOMBERS", createStormWingsBombers());
        PRESETS.put("STORM_WINGS_INTERCEPTORS", createStormWingsInterceptors());
    }

    /**
     * Get all available presets
     */
    public static Collection<CustomFactionConfig> getAllPresets() {
        return Collections.unmodifiableCollection(PRESETS.values());
    }

    /**
     * Get a specific preset by ID
     */
    public static CustomFactionConfig getPreset(String presetId) {
        return PRESETS.get(presetId);
    }

    /**
     * Get preset IDs
     */
    public static Set<String> getPresetIds() {
        return Collections.unmodifiableSet(PRESETS.keySet());
    }

    /**
     * Check if a preset exists
     */
    public static boolean hasPreset(String presetId) {
        return PRESETS.containsKey(presetId);
    }

    // ===== TERRAN PRESETS =====

    /**
     * Terran Coalition - Balanced all-rounder faction
     */
    private static CustomFactionConfig createTerranStandard() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("TERRAN_STANDARD")
                .displayName("Terran Coalition")
                .themeColor("#4A90E2")
                .icon("🛡️")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        UnitType.WORKER,         // 0 pt (required)
                        UnitType.INFANTRY,       // 2 pt
                        UnitType.ROCKET_SOLDIER, // 3 pt
                        UnitType.MEDIC,          // 3 pt
                        UnitType.JEEP,           // 3 pt
                        UnitType.TANK,           // 5 pt
                        UnitType.ARTILLERY,      // 6 pt
                        UnitType.FLAK_TANK,      // 5 pt - anti-air coverage
                        UnitType.SCOUT_DRONE,    // 2 pt
                        UnitType.HELICOPTER,     // 4 pt
                        UnitType.CRAWLER         // 10 pt (hero)
                )))
                .selectedBuildings(new HashSet<>(Arrays.asList(
                        BuildingType.HEADQUARTERS,     // 0 pt (required)
                        BuildingType.POWER_PLANT,      // 2 pt
                        BuildingType.BARRACKS,         // 3 pt
                        BuildingType.FACTORY,          // 5 pt
                        BuildingType.AIRFIELD,         // 5 pt
                        BuildingType.RESEARCH_LAB,     // 4 pt
                        BuildingType.TECH_CENTER,      // 6 pt
                        BuildingType.REFINERY,         // 2 pt
                        BuildingType.TURRET,           // 2 pt
                        BuildingType.BUNKER,           // 3 pt
                        BuildingType.COMMAND_CITADEL   // 12 pt (monument)
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.FORTIFIED_1,       // 3 pt
                        FactionPerk.UPKEEP_INCREASE_1, // 3 pt
                        FactionPerk.VETERAN_UNITS_1,   // 3 pt
                        FactionPerk.TURRET_EFFICIENCY, // 4 pt - synergizes with turrets
                        FactionPerk.RESOURCE_BOOST_1,  // 4 pt - economy boost
                        FactionPerk.RAPID_DEPLOYMENT_1 // 3 pt - faster building
                )))
                .basedOnPreset(null)
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    /**
     * Terran Infantry Focus - Emphasizes ground troops
     */
    private static CustomFactionConfig createTerranInfantry() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("TERRAN_INFANTRY")
                .displayName("Terran Infantry Corps")
                .themeColor("#2E8B57")
                .icon("⚔️")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        UnitType.WORKER,
                        UnitType.INFANTRY,
                        UnitType.ROCKET_SOLDIER,
                        UnitType.MEDIC,
                        UnitType.SNIPER,         // 3 pt
                        UnitType.ENGINEER,       // 3 pt
                        UnitType.SHOTGUN_INFANTRY, // 3 pt - more infantry variety
                        UnitType.JEEP,
                        UnitType.TANK,           // 5 pt - heavy support
                        UnitType.ARTILLERY,      // 6 pt - siege support
                        UnitType.SCOUT_DRONE,
                        UnitType.CRAWLER
                )))
                .selectedBuildings(new HashSet<>(Arrays.asList(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.BARRACKS,
                        BuildingType.FACTORY,    // 5 pt - vehicle support
                        BuildingType.RESEARCH_LAB,
                        BuildingType.REFINERY,
                        BuildingType.TURRET,
                        BuildingType.BUNKER,
                        BuildingType.COMMAND_CITADEL
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.INFANTRY_DOCTRINE_1,  // 4 pt
                        FactionPerk.INFANTRY_DOCTRINE_2,  // 7 pt (requires level 1)
                        FactionPerk.BUNKER_MASTERY,       // 4 pt
                        FactionPerk.UPKEEP_INCREASE_2,    // 6 pt (requires level 1)
                        FactionPerk.UPKEEP_INCREASE_1,    // 3 pt (prerequisite)
                        FactionPerk.INFANTRY_TRAINING_1,  // 3 pt - faster production
                        FactionPerk.VETERAN_UNITS_1,      // 3 pt - tougher units
                        FactionPerk.FORTIFIED_1           // 3 pt - defensive synergy
                )))
                .basedOnPreset("TERRAN_STANDARD")
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    /**
     * Terran Mechanized - Heavy vehicle focus
     */
    private static CustomFactionConfig createTerranMechanized() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("TERRAN_MECHANIZED")
                .displayName("Terran Armored Division")
                .themeColor("#8B4513")
                .icon("🚜")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        UnitType.WORKER,
                        UnitType.INFANTRY,
                        UnitType.ENGINEER,       // 3 pt - repair capability
                        UnitType.JEEP,
                        UnitType.TANK,
                        UnitType.ARTILLERY,
                        UnitType.FLAK_TANK,      // 5 pt
                        UnitType.SCOUT_DRONE,
                        UnitType.HELICOPTER,     // 4 pt - air support
                        UnitType.CRAWLER
                )))
                .selectedBuildings(new HashSet<>(Arrays.asList(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.BARRACKS,
                        BuildingType.FACTORY,
                        BuildingType.AIRFIELD,   // 5 pt - enables helicopter
                        BuildingType.RESEARCH_LAB,
                        BuildingType.TECH_CENTER,
                        BuildingType.REFINERY,
                        BuildingType.TURRET,
                        BuildingType.COMMAND_CITADEL
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.MECHANIZED_WARFARE_1,  // 4 pt
                        FactionPerk.MECHANIZED_WARFARE_2,  // 7 pt
                        FactionPerk.RAPID_DEPLOYMENT_1,    // 3 pt
                        FactionPerk.LOGISTICS_NETWORK,     // 6 pt
                        FactionPerk.VEHICLE_PRODUCTION_1   // 3 pt - faster vehicle production
                )))
                .basedOnPreset("TERRAN_STANDARD")
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    // ===== NOMADS PRESETS =====

    /**
     * Nomad Raiders - Fast, mobile warfare
     */
    private static CustomFactionConfig createNomadsRaiders() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("NOMADS_RAIDERS")
                .displayName("Nomad Raiders")
                .themeColor("#D4A044")
                .icon("🏎️")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        UnitType.WORKER,
                        UnitType.INFANTRY,
                        UnitType.JEEP,
                        UnitType.TANK,
                        UnitType.CLOAK_TANK,     // 6 pt
                        UnitType.ARTILLERY,      // 6 pt - siege capability
                        UnitType.SCOUT_DRONE,
                        UnitType.RAIDER          // 10 pt (hero)
                )))
                .selectedBuildings(new HashSet<>(Arrays.asList(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.BARRACKS,
                        BuildingType.FACTORY,
                        BuildingType.RESEARCH_LAB,
                        BuildingType.TECH_CENTER,         // 6 pt - advanced tech
                        BuildingType.REFINERY,
                        BuildingType.TURRET,
                        BuildingType.SANDSTORM_GENERATOR  // 12 pt (monument)
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.UPKEEP_INCREASE_1,     // 3 pt
                        FactionPerk.UPKEEP_INCREASE_2,     // 6 pt
                        FactionPerk.RAPID_DEPLOYMENT_1,    // 3 pt
                        FactionPerk.RAPID_DEPLOYMENT_2,    // 6 pt
                        FactionPerk.MECHANIZED_WARFARE_1,  // 4 pt
                        FactionPerk.MECHANIZED_WARFARE_2,  // 7 pt - already has level 1
                        FactionPerk.COST_REDUCTION_1,      // 5 pt
                        FactionPerk.DAMAGE_BOOST_1         // 4 pt - more firepower
                )))
                .basedOnPreset(null)
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    /**
     * Nomad Guerrilla - Hit and run tactics
     */
    private static CustomFactionConfig createNomadsGuerrilla() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("NOMADS_GUERRILLA")
                .displayName("Nomad Guerrilla Force")
                .themeColor("#8B6914")
                .icon("🎯")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        UnitType.WORKER,
                        UnitType.INFANTRY,
                        UnitType.SNIPER,
                        UnitType.ENGINEER,       // 3 pt - support
                        UnitType.JEEP,
                        UnitType.TANK,           // 5 pt - heavy support
                        UnitType.CLOAK_TANK,
                        UnitType.SCOUT_DRONE,
                        UnitType.RAIDER
                )))
                .selectedBuildings(new HashSet<>(Arrays.asList(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.BARRACKS,
                        BuildingType.FACTORY,
                        BuildingType.RESEARCH_LAB,         // 4 pt - tech access
                        BuildingType.TECH_CENTER,          // 6 pt - advanced tech
                        BuildingType.REFINERY,
                        BuildingType.TURRET,
                        BuildingType.BUNKER,
                        BuildingType.SANDSTORM_GENERATOR
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.UPKEEP_INCREASE_2,     // 6 pt (with prerequisite)
                        FactionPerk.UPKEEP_INCREASE_1,     // 3 pt
                        FactionPerk.RAPID_DEPLOYMENT_2,    // 6 pt (with prerequisite)
                        FactionPerk.RAPID_DEPLOYMENT_1,    // 3 pt
                        FactionPerk.DAMAGE_BOOST_1,        // 4 pt
                        FactionPerk.DAMAGE_BOOST_2,        // 8 pt - already has level 1
                        FactionPerk.SALVAGE_OPERATIONS     // 5 pt
                )))
                .basedOnPreset("NOMADS_RAIDERS")
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    // ===== SYNTHESIS PRESETS =====

    /**
     * Synthesis Shielded - High-tech defensive focus
     */
    private static CustomFactionConfig createSynthesisShielded() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("SYNTHESIS_SHIELDED")
                .displayName("Synthesis Shield Corps")
                .themeColor("#9370DB")
                .icon("⚡")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        UnitType.WORKER,
                        UnitType.LASER_INFANTRY, // 3 pt
                        UnitType.JEEP,
                        UnitType.TANK,
                        UnitType.SCOUT_DRONE,
                        UnitType.HELICOPTER,
                        UnitType.COLOSSUS        // 10 pt (hero)
                )))
                .selectedBuildings(new HashSet<>(Arrays.asList(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.BARRACKS,
                        BuildingType.FACTORY,
                        BuildingType.AIRFIELD,
                        BuildingType.RESEARCH_LAB,
                        BuildingType.TECH_CENTER,
                        BuildingType.REFINERY,
                        BuildingType.SHIELD_GENERATOR,    // 4 pt
                        BuildingType.LASER_TURRET,        // 4 pt
                        BuildingType.BUNKER,              // 3 pt - defense
                        BuildingType.ANDROID_FACTORY      // 12 pt (monument)
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.POWER_EFFICIENCY_1,    // 3 pt
                        FactionPerk.POWER_EFFICIENCY_2,    // 6 pt
                        FactionPerk.POWER_EFFICIENCY_3,    // 10 pt - already has level 2
                        FactionPerk.FORTIFIED_1,           // 3 pt
                        FactionPerk.VETERAN_UNITS_1        // 3 pt - tougher units
                )))
                .basedOnPreset(null)
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    /**
     * Synthesis Android Army - Monument-focused strategy
     */
    private static CustomFactionConfig createSynthesisAndroids() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("SYNTHESIS_ANDROIDS")
                .displayName("Synthesis Android Legion")
                .themeColor("#4B0082")
                .icon("🤖")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        UnitType.WORKER,
                        UnitType.LASER_INFANTRY,
                        UnitType.ANDROID,        // 0 pt (bundled with monument)
                        UnitType.JEEP,           // 3 pt - mobility
                        UnitType.TANK,
                        UnitType.HELICOPTER,     // 4 pt - air support
                        UnitType.SCOUT_DRONE,
                        UnitType.COLOSSUS
                )))
                .selectedBuildings(new HashSet<>(Arrays.asList(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.BARRACKS,
                        BuildingType.FACTORY,
                        BuildingType.AIRFIELD,            // 5 pt - enables helicopter
                        BuildingType.RESEARCH_LAB,
                        BuildingType.TECH_CENTER,
                        BuildingType.REFINERY,
                        BuildingType.SHIELD_GENERATOR,
                        BuildingType.LASER_TURRET,
                        BuildingType.TURRET,              // 2 pt - basic defense
                        BuildingType.BUNKER,              // 3 pt - additional defense
                        BuildingType.ANDROID_FACTORY
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.POWER_EFFICIENCY_2,    // 6 pt (with prerequisite)
                        FactionPerk.POWER_EFFICIENCY_1,    // 3 pt
                        FactionPerk.FORTIFIED_2,           // 6 pt (with prerequisite)
                        FactionPerk.FORTIFIED_1,           // 3 pt
                        FactionPerk.RESOURCE_BOOST_1,      // 4 pt - economy boost
                        FactionPerk.DAMAGE_BOOST_1         // 4 pt - more firepower
                )))
                .basedOnPreset("SYNTHESIS_SHIELDED")
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    // ===== TECH ALLIANCE PRESETS =====

    /**
     * Tech Alliance Beams - Advanced beam weapon technology
     */
    private static CustomFactionConfig createTechAllianceBeams() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("TECH_ALLIANCE_BEAMS")
                .displayName("Tech Alliance Beam Division")
                .themeColor("#00CED1")
                .icon("🔬")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        UnitType.WORKER,
                        UnitType.PLASMA_TROOPER, // 3 pt
                        UnitType.ION_RANGER,     // 3 pt
                        UnitType.PHOTON_SCOUT,   // 3 pt
                        UnitType.LASER_INFANTRY, // 3 pt - beam infantry
                        UnitType.BEAM_TANK,      // 5 pt
                        UnitType.SCOUT_DRONE,
                        UnitType.HELICOPTER,     // 4 pt - air unit
                        UnitType.PHOTON_TITAN    // 10 pt (hero)
                )))
                .selectedBuildings(new HashSet<>(Arrays.asList(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.BARRACKS,
                        BuildingType.FACTORY,
                        BuildingType.AIRFIELD,            // 5 pt - air production
                        BuildingType.RESEARCH_LAB,
                        BuildingType.TECH_CENTER,
                        BuildingType.REFINERY,
                        BuildingType.LASER_TURRET,
                        BuildingType.SHIELD_GENERATOR,
                        BuildingType.TURRET,              // 2 pt - defense
                        BuildingType.PHOTON_SPIRE         // 12 pt (monument)
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.COST_REDUCTION_1,      // 5 pt
                        FactionPerk.DAMAGE_BOOST_1,        // 4 pt
                        FactionPerk.DAMAGE_BOOST_2,        // 8 pt - even more firepower
                        FactionPerk.POWER_EFFICIENCY_1,    // 3 pt - beam weapons need power
                        FactionPerk.VETERAN_UNITS_1,       // 3 pt - unit durability
                        FactionPerk.FORTIFIED_1            // 3 pt - building durability
                )))
                .basedOnPreset(null)
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    // ===== STORM WINGS PRESETS =====

    /**
     * Storm Wings Bombers - Heavy air strike focus
     */
    private static CustomFactionConfig createStormWingsBombers() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("STORM_WINGS_BOMBERS")
                .displayName("Storm Wings Bomber Command")
                .themeColor("#4169E1")
                .icon("✈️")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        UnitType.WORKER,
                        UnitType.INFANTRY,
                        UnitType.ROCKET_SOLDIER, // 3 pt - anti-air infantry
                        UnitType.JEEP,
                        UnitType.FLAK_TANK,      // 5 pt - ground-based anti-air
                        UnitType.SCOUT_DRONE,
                        UnitType.HELICOPTER,
                        UnitType.BOMBER,         // 5 pt
                        UnitType.GUNSHIP         // 10 pt (hero)
                )))
                .selectedBuildings(new HashSet<>(Arrays.asList(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.BARRACKS,
                        BuildingType.FACTORY,            // 5 pt - enables Flak Tank
                        BuildingType.AIRFIELD,
                        BuildingType.HANGAR,             // 5 pt
                        BuildingType.RESEARCH_LAB,
                        BuildingType.REFINERY,
                        BuildingType.TURRET,
                        BuildingType.ROCKET_TURRET,      // 3 pt
                        BuildingType.TEMPEST_SPIRE       // 12 pt (monument)
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.AIR_SUPERIORITY_1,   // 4 pt
                        FactionPerk.AIR_SUPERIORITY_2,   // 7 pt
                        FactionPerk.UPKEEP_INCREASE_1,   // 3 pt
                        FactionPerk.RAPID_DEPLOYMENT_1,  // 3 pt
                        FactionPerk.COST_REDUCTION_1,    // 5 pt
                        FactionPerk.VETERAN_UNITS_1      // 3 pt - tougher units
                )))
                .basedOnPreset(null)
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    /**
     * Storm Wings Interceptors - Air superiority fighters
     */
    private static CustomFactionConfig createStormWingsInterceptors() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("STORM_WINGS_INTERCEPTORS")
                .displayName("Storm Wings Fighter Wing")
                .themeColor("#1E90FF")
                .icon("🛩️")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        UnitType.WORKER,
                        UnitType.INFANTRY,
                        UnitType.SCOUT_DRONE,
                        UnitType.HELICOPTER,
                        UnitType.INTERCEPTOR,    // 5 pt
                        UnitType.BOMBER,         // 5 pt - bombing capability
                        UnitType.GUNSHIP
                )))
                .selectedBuildings(new HashSet<>(Arrays.asList(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.BARRACKS,
                        BuildingType.AIRFIELD,
                        BuildingType.HANGAR,
                        BuildingType.RESEARCH_LAB,
                        BuildingType.TECH_CENTER,
                        BuildingType.REFINERY,
                        BuildingType.ROCKET_TURRET,
                        BuildingType.TEMPEST_SPIRE
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.AIR_SUPERIORITY_2,     // 7 pt (with prerequisite)
                        FactionPerk.AIR_SUPERIORITY_1,     // 4 pt
                        FactionPerk.UPKEEP_INCREASE_1,     // 3 pt
                        FactionPerk.DAMAGE_BOOST_1,        // 4 pt
                        FactionPerk.LOGISTICS_NETWORK,     // 6 pt
                        FactionPerk.RAPID_DEPLOYMENT_1     // 3 pt - faster building
                )))
                .basedOnPreset("STORM_WINGS_BOMBERS")
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }
}
