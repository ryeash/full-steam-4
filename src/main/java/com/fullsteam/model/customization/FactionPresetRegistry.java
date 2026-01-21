package com.fullsteam.model.customization;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registry of preset faction configurations.
 * These serve as starting points for players to customize from.
 */
@Singleton
public class FactionPresetRegistry {

    private static final Map<String, CustomFactionConfig> PRESETS = Stream.of(
                    createIronFist(),
                    createThunderRoad(),
                    createStormWings(),
                    createFortressGuard(),
                    createSynthesisCore(),
                    createLongReach()
            )
            .collect(Collectors.toUnmodifiableMap(CustomFactionConfig::getFactionId, Function.identity()));


    /**
     * Get all available presets
     */
    public static Collection<CustomFactionConfig> getAllPresets() {
        return PRESETS.values();
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
        return PRESETS.keySet();
    }

    /**
     * Check if a preset exists
     */
    public static boolean hasPreset(String presetId) {
        return PRESETS.containsKey(presetId);
    }

    /**
     * Iron Fist - Infantry-focused with strong garrison and support capabilities
     * Theme: Boots on the ground with overwhelming infantry numbers and bunker defense
     */
    private static CustomFactionConfig createIronFist() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("IRON_BRIGADE")
                .displayName("Iron Brigade")
                .themeColor("#2E8B57")
                .icon("⚔️")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        // T1 Infantry core
                        UnitType.WORKER,
                        UnitType.ROCKET_SOLDIER,     // Anti-air/vehicle
                        UnitType.MINIGUNNER,
                        UnitType.MEDIC,              // Healing support
                        // T2 Infantry variety
                        UnitType.SNIPER,             // Long-range support
                        UnitType.SHOTGUN_INFANTRY,   // Close combat
                        UnitType.ENGINEER,           // Repair support
                        UnitType.GRENADIER,
                        // T2 Vehicle support (limited but essential)
                        UnitType.JEEP,               // Fast transport/scout
                        UnitType.APC          // Anti-air coverage
                )))
                .selectedBuildings(new HashSet<>(Arrays.asList(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.BARRACKS,
                        BuildingType.FACTORY,        // For Jeep and Flak Tank
                        BuildingType.RESEARCH_LAB,
                        BuildingType.TECH_CENTER,
                        BuildingType.REFINERY,
                        BuildingType.BUNKER,         // Garrison infantry
                        BuildingType.FLAK_TURRET,    // Anti-air defense
                        BuildingType.ROCKET_TURRET   // Defense
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.INFANTRY_DOCTRINE_1,  // 4 pt - cheaper infantry, more damage
                        FactionPerk.INFANTRY_DOCTRINE_2,  // 7 pt - even better
                        FactionPerk.GARRISON_MASTERY,     // 4 pt - better bunkers
                        FactionPerk.INFANTRY_TRAINING_1,  // 3 pt - faster barracks production
                        FactionPerk.INFANTRY_TRAINING_2,  // 6 pt - even faster
                        FactionPerk.UPKEEP_INCREASE_1,    // 3 pt - more units
                        FactionPerk.UPKEEP_INCREASE_2,    // 6 pt - even more units
                        FactionPerk.VETERAN_UNITS_1,      // 3 pt - tougher units
                        FactionPerk.VETERAN_UNITS_2,      // 6 pt - tougher units
                        FactionPerk.FORTIFIED_1,          // 3 pt - building durability
                        FactionPerk.RAPID_DEPLOYMENT_1    // 3 pt - faster buildings (adds +6 to get to 100)
                )))
                .basedOnPreset(null)
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    /**
     * Thunder Road - Mechanized warfare with heavy vehicles and mobility
     * Theme: Rolling thunder of tanks and armored vehicles
     */
    private static CustomFactionConfig createThunderRoad() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("TERRAN_ARMOR")
                .displayName("Terran Armored Division")
                .themeColor("#8B4513")
                .icon("🚜")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        // T1 basics
                        UnitType.WORKER,
                        UnitType.INFANTRY,           // Basic defense
                        UnitType.ENGINEER,           // Repair vehicles
                        // T2 Vehicle core
                        UnitType.TANK,               // Main battle tank
                        UnitType.SHIELD_TANK,        // defense
                        UnitType.SAM_LAUNCHER,       // anti-are
                        UnitType.ARTILLERY,          // Long-range support
                        // T3 Advanced
                        UnitType.COLOSSUS            // Super-heavy unit
                )))
                .selectedBuildings(new HashSet<>(Arrays.asList(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.BARRACKS,
                        BuildingType.FACTORY,
                        BuildingType.RESEARCH_LAB,
                        BuildingType.TECH_CENTER,
                        BuildingType.REFINERY,
                        BuildingType.TURRET,
                        BuildingType.ROCKET_TURRET,
                        BuildingType.SHIELD_GENERATOR    // Protect vehicles
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.MECHANIZED_WARFARE_1,  // 4 pt - cheaper vehicles, more HP
                        FactionPerk.MECHANIZED_WARFARE_2,  // 7 pt - even better
                        FactionPerk.VEHICLE_PRODUCTION_1,  // 3 pt - faster factory
                        FactionPerk.VEHICLE_PRODUCTION_2,  // 6 pt - faster factory
                        FactionPerk.COST_REDUCTION_1,
                        FactionPerk.VETERAN_UNITS_1,
                        FactionPerk.UPKEEP_INCREASE_1,     // 3 pt - more units
                        FactionPerk.UPKEEP_INCREASE_2     // 6 pt - more units (adds +6 to get to 99)
                )))
                .basedOnPreset(null)
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    /**
     * Storm Wings - Air superiority with bombers, interceptors, and gunships
     * Theme: Rule the skies with overwhelming air power
     */
    private static CustomFactionConfig createStormWings() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("STORM_WINGS")
                .displayName("Storm Wings Air Command")
                .themeColor("#4169E1")
                .icon("✈️")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        // T1 Ground basics
                        UnitType.WORKER,
                        UnitType.INFANTRY,           // Basic defense
                        UnitType.ROCKET_SOLDIER,     // Anti-air
                        // T2 Ground support
                        UnitType.JEEP,               // Fast ground unit
                        UnitType.FLAK_TANK,          // Ground anti-air
                        // T2-T3 Air dominance
                        UnitType.SCOUT_DRONE,        // T2 air scout
                        UnitType.HELICOPTER,         // T2 gunship
                        UnitType.BOMBER,             // T3 heavy bomber
                        UnitType.INTERCEPTOR,        // T3 air superiority
                        UnitType.GUNSHIP             // T3 heavy gunship
                )))
                .selectedBuildings(new HashSet<>(Arrays.asList(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.BARRACKS,
                        BuildingType.FACTORY,        // For ground vehicles
                        BuildingType.AIRFIELD,       // Core air production
                        BuildingType.HANGAR,         // Sortie aircraft
                        BuildingType.RESEARCH_LAB,
                        BuildingType.TECH_CENTER,
                        BuildingType.REFINERY,
                        BuildingType.ROCKET_TURRET,  // Ground anti-air
                        BuildingType.FLAK_TURRET     // Additional anti-air
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.AIR_SUPERIORITY_1,     // 4 pt - cheaper air, faster air
                        FactionPerk.AIR_SUPERIORITY_2,     // 7 pt - even better
                        FactionPerk.UPKEEP_INCREASE_1,     // 3 pt - more units
                        FactionPerk.RAPID_DEPLOYMENT_1,    // 3 pt - faster buildings
                        FactionPerk.VETERAN_UNITS_1,       // 3 pt - tougher aircraft
                        FactionPerk.COST_REDUCTION_1       // 5 pt - economy boost
                )))
                .basedOnPreset(null)
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    /**
     * Fortress Guard - Turtle defense with heavy fortifications and turrets
     * Theme: Impenetrable defense with layered fortifications
     */
    private static CustomFactionConfig createFortressGuard() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("FORTRESS_GUARD")
                .displayName("Fortress Guard")
                .themeColor("#556B2F")
                .icon("🏰")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        // T1 basics
                        UnitType.WORKER,
                        UnitType.INFANTRY,           // Garrison troops
                        UnitType.ROCKET_SOLDIER,     // Anti-air
                        // T2 defense support
                        UnitType.ENGINEER,           // Repair buildings
                        UnitType.JEEP,               // Scout
                        UnitType.TANK,               // Counter-attack force
                        UnitType.FLAK_TANK,          // Anti-air mobile
                        // T3 heavy defense
                        UnitType.SCOUT_DRONE,        // Air vision
                        UnitType.HELICOPTER         // Air defense
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
                        BuildingType.BANK,
                        BuildingType.BUNKER,             // Infantry garrisons
                        BuildingType.TURRET,             // Basic defense
                        BuildingType.SANDSTORM_GENERATOR,
                        BuildingType.LASER_TURRET,
                        BuildingType.PHOTON_SPIRE,
                        BuildingType.ROCKET_TURRET,      // Anti-air turret
                        BuildingType.SHIELD_GENERATOR    // Shield defense (remove laser turret and command citadel to save points)
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.FORTIFIED_1,           // 3 pt - stronger buildings
                        FactionPerk.FORTIFIED_2,           // 6 pt - even stronger
                        FactionPerk.TURRET_EFFICIENCY,     // 4 pt - better turrets
                        FactionPerk.GARRISON_MASTERY      // 4 pt - better bunkers
                )))
                .basedOnPreset(null)
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    /**
     * Synthesis Core - High-tech beams, androids, and energy weapons
     * Theme: Advanced technology with beam weapons and autonomous androids
     */
    private static CustomFactionConfig createSynthesisCore() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("SYNTHESIS_CORE")
                .displayName("Synthesis Core")
                .themeColor("#9370DB")
                .icon("🤖")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        // T1 basics
                        UnitType.WORKER,
                        UnitType.LASER_INFANTRY,     // Beam infantry
                        UnitType.PLASMA_TROOPER,     // Fast beam infantry
                        // T2 beam units
                        UnitType.ION_RANGER,         // Beam sniper
                        UnitType.PHOTON_SCOUT,       // Beam scout vehicle
                        UnitType.BEAM_TANK,          // Beam tank
                        // T3 advanced tech
                        UnitType.ANDROID,            // Free autonomous units
                        UnitType.LASER_GUNSHIP,        // Air attacker
                        UnitType.PHOTON_TITAN        // Beam super-unit (remove colossus to save points)
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
                        BuildingType.ANDROID_FACTORY,    // Android production
                        BuildingType.LASER_TURRET,       // Beam turret (remove photon spire to save points)
                        BuildingType.SHIELD_GENERATOR    // Energy shields
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.POWER_EFFICIENCY_1,    // 3 pt - critical for beams
                        FactionPerk.POWER_EFFICIENCY_2,    // 6 pt - even better
                        FactionPerk.DAMAGE_BOOST_1,        // 4 pt - beam damage
                        FactionPerk.VETERAN_UNITS_1,       // 3 pt - unit durability
                        FactionPerk.FORTIFIED_1,           // 3 pt - building durability
                        FactionPerk.COST_REDUCTION_1       // 5 pt - expensive tech
                )))
                .basedOnPreset(null)
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }

    /**
     * Long Reach - Artillery, snipers, and long-range focused warfare
     * Theme: Strike from afar with overwhelming long-range firepower
     */
    private static CustomFactionConfig createLongReach() {
        CustomFactionConfig config = CustomFactionConfig.builder()
                .factionId("FARSIGHT")
                .displayName("Farsight Artillery Corps")
                .themeColor("#DC143C")
                .icon("🎯")
                .selectedUnits(new HashSet<>(Arrays.asList(
                        // T1 basics
                        UnitType.WORKER,
                        UnitType.INFANTRY,           // Basic defense
                        UnitType.ROCKET_SOLDIER,     // Long-range anti-air
                        UnitType.SNIPER,             // Long-range infantry
                        // T2 long-range core
                        UnitType.ION_RANGER,         // Beam sniper
                        UnitType.JEEP,               // Scout/spotter
                        UnitType.PULSE_ARTILLERY,
                        UnitType.SPIDER_MINE,        // Surprise ambush
                        UnitType.ARTILLERY,          // Core siege weapon
                        UnitType.SAM_LAUNCHER,       // Long-range anti-air
                        // T3 ultimate range
                        UnitType.SCOUT_DRONE        // Air spotter
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
                        BuildingType.TURRET,
                        BuildingType.ROCKET_TURRET,      // Long-range defense
                        BuildingType.PHOTON_SPIRE        // Ultimate range turret
                )))
                .selectedPerks(new HashSet<>(Arrays.asList(
                        FactionPerk.DAMAGE_BOOST_1,        // 4 pt - more firepower
                        FactionPerk.DAMAGE_BOOST_2,        // 8 pt - even more
                        FactionPerk.VETERAN_UNITS_1,       // 3 pt - durability
                        FactionPerk.UPKEEP_INCREASE_1,     // 3 pt - more artillery
                        FactionPerk.COST_REDUCTION_1,      // 5 pt - expensive units
                        FactionPerk.POWER_EFFICIENCY_1     // 3 pt - beam weapons
                )))
                .basedOnPreset(null)
                .build();

        config.setTotalPointsSpent(config.calculateTotalPoints());
        return config;
    }
}
