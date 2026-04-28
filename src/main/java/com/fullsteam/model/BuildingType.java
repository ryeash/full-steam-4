package com.fullsteam.model;

import com.fullsteam.model.customization.BuildingCategory;
import lombok.Getter;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Vector2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Defines the different types of buildings available in the RTS game.
 * Shape rendering is handled by the number of sides (3=triangle, 4=rectangle, etc.)
 */
@Getter
public enum BuildingType {
    // Main base - produces workers, required to win
    HEADQUARTERS(
            "Headquarters",
            "Your main base—trains workers, generates power, and must survive to stay in the fight.",
            BuildingCategory.ECONOMY,
            // free (starting building)
            0,        // no build time
            0,     // max health
            5000,     // size (radius)
            80.0,  // gold
            0xFFD700,    // can produce units
            true,       // power generation
            50,    // vision range (excellent, main base)
            500.0,         // faction customization point cost
            0, false,
            "H",
            "\uD83C\uDFDB\uFE0F"),

    // Resource collection point
    REFINERY(
            "Refinery",
            "Resource drop-off for workers; extends your economy beyond the starting stockpile.",
            BuildingCategory.ECONOMY,
            // resource cost
            300,       // build time (seconds)
            20,      // max health
            600,     // size (radius)
            50.0,  // gray
            0x808080,    // cannot produce units
            false,     // power consumption
            -10,    // vision range (moderate, economic building)
            350.0,         // faction customization point cost
            2, false,
            "R",
            "\uD83C\uDFED"),

    // Infantry production
    BARRACKS(
            "Barracks",
            "Produces infantry—from basic riflemen to medics, engineers, and elite specialists.",
            BuildingCategory.PRODUCTION,
            // resource cost
            200,       // build time (seconds)
            15,      // max health
            550,     // size (radius)
            45.0,  // brown
            0x8B4513,    // can produce units
            true,     // power consumption
            -25,    // vision range (good, production building)
            380.0,         // faction customization point cost
            3, false,
            "B",
            "\uD83C\uDFF0"),

    // Power generation - required for advanced buildings
    POWER_PLANT(
            "Power Plant",
            "Generates electricity; build more before advanced structures brown out your grid.",
            BuildingCategory.ECONOMY,
            // resource cost
            250,       // build time (seconds)
            20,      // max health
            400,     // size (radius)
            40.0,  // yellow
            0xFFFF00,    // cannot produce units
            false,      // power generation
            100,    // vision range (moderate, utility building)
            360.0,         // faction customization point cost
            0, false,
            "P",
            "\u26A1"),

    // Vehicle production
    FACTORY(
            "Factory",
            "Vehicle production—from scouts and transports to tanks, artillery, and super-heavies.",
            BuildingCategory.PRODUCTION,
            // resource cost
            400,       // build time (seconds)
            25,      // max health
            800,     // size (radius)
            55.0,  // dark gray
            0x696969,    // can produce units
            true,     // power consumption
            -30,    // vision range (good, production building)
            390.0,         // faction customization point cost
            5, false,
            "F",
            "\uD83D\uDE97"),

    // Research and tech unlocking - unlocks T2
    RESEARCH_LAB(
            "Research Lab",
            "Standard tech structure unlocks tier-2 tech.",
            BuildingCategory.TECH,
            // resource cost
            500,       // build time (seconds)
            30,      // max health
            700,     // size (radius)
            50.0,  // dark turquoise
            0x00CED1,    // cannot produce units
            false,     // power consumption
            -35,    // vision range (good, tech building)
            400.0,         // faction customization point cost
            4, false,
            "RL",
            "\uD83D\uDD2C"),

    // Elite tech unlocking - unlocks T3
    TECH_CENTER(
            "Tech Center",
            "High end research hub unlocks tier-3 tech.",
            BuildingCategory.TECH,
            // resource cost
            800,       // build time (seconds)
            40,      // max health
            900,     // size (radius)
            60.0,  // royal blue
            0x4169E1,    // cannot produce units
            false,     // power consumption
            -50,    // vision range (excellent, advanced tech)
            420.0,         // faction customization point cost
            6, false,
            "TC",
            "\uD83E\uDDEA"),


    // Defensive structure - attacks enemies with cannon
    TURRET(
            "Turret",
            "Automated cannon emplacement—reliable general-purpose base and chokepoint defense.",
            BuildingCategory.DEFENSE,
            // resource cost
            250,       // build time (seconds)
            15,      // max health
            500,     // size (radius)
            25.0,  // orange red
            0xFF4500,    // cannot produce units
            false,     // power consumption
            -35,    // vision range (excellent, needs to spot threats)
            450.0,         // faction customization point cost
            2, false,
            "T",
            "\uD83C\uDFAF"),

    // Defensive structure - fires rockets with explosive damage
    ROCKET_TURRET(
            "Rocket Turret",
            "Long-range rocket battery—explosive volleys excel versus armor and grouped targets.",
            BuildingCategory.DEFENSE,
            // resource cost (more expensive than basic turret)
            350,       // build time (seconds)
            20,      // max health (lower than basic turret)
            400,     // size (radius)
            25.0,  // tomato red
            0xFF6347,    // cannot produce units
            false,     // power consumption (higher than basic)
            -50,    // vision range (excellent, long-range targeting)
            480.0,         // faction customization point cost
            3, false,
            "RT",
            "\uD83D\uDE80"),

    // Defensive structure - dedicated anti-aircraft flak cannon
    FLAK_TURRET(
            "Flak Turret",
            "Dedicated anti-air turret shredding low-altitude aircraft with flak bursts.",
            BuildingCategory.DEFENSE,
            // resource cost (cheaper than rocket turret, accessible T2)
            300,       // build time (seconds)
            18,      // max health (moderate durability)
            450,     // size (radius)
            25.0,  // gray (flak color)
            0xA0A0A0,    // cannot produce units
            false,     // power consumption (moderate)
            -45,    // vision range (excellent, needs to spot aircraft)
            500.0,         // faction customization point cost
            3, false,
            "FT",
            "\uD83D\uDCA5"),

    // Defensive structure - fires laser beams
    LASER_TURRET(
            "Laser Turret",
            "High-tech beam turret—long reach, sustained damage, and hungry power draw.",
            BuildingCategory.DEFENSE,
            // resource cost (expensive advanced turret)
            400,       // build time (seconds)
            25,      // max health (lowest of turrets - glass cannon)
            350,     // size (radius)
            25.0,  // cyan (laser blue)
            0x00FFFF,    // cannot produce units
            false,     // power consumption (highest - energy weapon)
            -65,    // vision range (best, advanced sensors)
            500.0,         // faction customization point cost
            4, false,
            "LT",
            "\uD83D\uDD37"),

    // Defensive structure - infantry can garrison inside and fire out
    BUNKER(
            "Bunker",
            "Hardened garrison structure—infantry inside gain protection and extra defensive fireports.",
            BuildingCategory.DEFENSE,
            // resource cost - reduced to make it accessible as T1
            250,       // build time (seconds)
            18,      // max health
            1200,     // size (radius)
            35.0,  // dark olive green
            0x556B2F,    // cannot produce units
            false,     // power consumption
            -15,    // vision range (excellent, defensive structure)
            420.0,         // faction customization point cost
            3, false,
            "\u2694",
            "\uD83C\uDFF0"),

    // Defensive structure - projects shield that destroys incoming projectiles
    SHIELD_GENERATOR(
            "Shield Generator",
            "Projects a bubble shield that blocks hostile projectiles for units and structures inside.",
            BuildingCategory.DEFENSE,
            // resource cost
            400,       // build time (seconds)
            25,      // max health
            500,     // size (radius)
            30.0,  // deep sky blue
            0x00BFFF,    // cannot produce units
            false,     // power consumption
            -40,    // vision range (good, defensive utility)
            380.0,         // faction customization point cost
            4, false,
            "SG",
            "\uD83D\uDEE1\uFE0F"),

    BANK(
            "Bank",
            "Generates interest income based on current credit count.",
            BuildingCategory.ECONOMY,
            // resource cost (expensive T3 building)
            600,       // build time (seconds)
            30,      // max health
            420,     // size (radius)
            35.0,  // gold
            0xFFD700,    // cannot produce units
            false,     // power consumption
            -30,    // vision range (moderate, economic building)
            350.0,         // faction customization point cost
            4, false,
            "$",
            "\uD83D\uDCB0"),

    // Creates sandstorms for area denial
    SANDSTORM_GENERATOR(
            "Sandstorm Generator",
            "Summons a sandstorm aura that damages and disrupts enemies caught in the storm.",
            BuildingCategory.DEFENSE,
            // resource cost
            600,       // build time (seconds)
            60,      // max health
            800,     // size (radius) - reduced from 45
            35.0,  // burlywood (sandy color)
            0xDEB887,    // cannot produce units
            false,     // power consumption
            -40,    // vision range (good)
            430.0,        // faction customization point cost
            8, false,
            "\u2601",
            "\uD83C\uDF2A\uFE0F"),

    // Autonomous android production facility
    ANDROID_FACTORY(
            "Android Factory",
            "Autonomous factory that continuously builds free Android combat units without a queue.",
            BuildingCategory.PRODUCTION,
            // resource cost
            700,       // build time (seconds)
            90,      // max health
            900,     // size (radius)
            42.0,  // dark turquoise (Synthesis faction color)
            0x00CED1,    // can produce units (Androids!)
            true,     // power consumption
            -60,    // vision range (excellent)
            420.0,        // faction customization point cost
            12, false,
            "A",
            "\uD83E\uDD16"),

    // Defensive laser tower
    PHOTON_SPIRE(
            "Photon Spire",
            "Defensive photon lance tower.",
            BuildingCategory.DEFENSE,
            // resource cost
            650,       // build time (seconds)
            60,     // max health
            800,     // size (radius)
            48.0,  // bright green (photon energy)
            0x00FF00,    // cannot produce units
            false,     // power consumption
            -75,    // vision range (excellent, defensive)
            480.0,        // faction customization point cost
            10, false,
            "\u26A1",
            "\uD83D\uDC8E"),

    // Ultimate command center
    // TODO: may become obsolete with change to upkeep
    COMMAND_CITADEL(
            "Command Citadel",
            "Massive fortified command hub with huge vision—an anchor for super-late economies.",
            BuildingCategory.DEFENSE,
            // resource cost (expensive)
            700,       // build time (seconds)
            80,     // max health
            1000,     // size (radius) - large and imposing
            55.0,  // royal blue (command authority)
            0x4169E1,    // cannot produce units
            false,     // power consumption
            -50,   // vision range (HUGE, command center bonus)
            1000.0,        // faction customization point cost
            10, false,
            "CC",
            "\uD83C\uDFF0"),

    // Air unit production - requires Tech Center
    AIRFIELD(
            "Airfield",
            "Produces and houses aircraft—VTOL gunships plus sortie-based bombers and fighters.",
            BuildingCategory.PRODUCTION,
            // resource cost
            600,       // build time (seconds)
            35,      // max health
            700,     // size (radius) - large landing pad
            60.0,  // slate gray (runway color)
            0x708090,    // can produce units (air units!)
            true,     // power consumption
            -40,    // vision range (good, airfield tower)
            420.0,         // faction customization point cost
            5, false,
            "AF",
            "\u2708\uFE0F"),

    /**
     * Unlocks {@link CommandAbilityType#MARINE_DROP} for orbital infantry inserts.
     */
    JUMP_PAD(
            "Jump Pad",
            "Drop-ship landing grid and uplink—unlocks the Marine Drop command.",
            BuildingCategory.TECH,
            620,
            55,
            75.0,
            44.0,
            0x4A708B,
            false,
            -35,
            -42.0,
            3,
            true,
            "JP",
            "\uD83D\uDE81"),

    /**
     * Nuclear missile silo—required for {@link CommandAbilityType#NUKE_ARM} and {@link CommandAbilityType#NUKE_LAUNCH}.
     */
    NUKE_SILO(
            "Nuclear Silo",
            "Houses and arms a strategic warhead; arm on command, then launch at a ground target.",
            BuildingCategory.TECH,
            950,
            80,
            100.0,
            48.0,
            0x8B0000,
            false,
            -50,
            -45.0,
            10,
            true,
            "NS",
            "\u2622\uFE0F"),

    /** Unlocks command abilities (e.g. {@link CommandAbilityType#STRIKE_PACKAGE}, {@link CommandAbilityType#FLAK_BURST}). */
    STRIKE_RELAY(
            "Strike Relay",
            "Tactical uplink—unlocks the Strike Package command ability.",
            BuildingCategory.TECH,
            550,
            45,
            85.0,   // max health
            42.0,   // collision radius (was 390 by mistake — dominated the map)
            0xCD853F,
            false,
            -35,
            -40.0,
            6, true,
            "SR",
            "\uD83C\uDFAF"),

    /**
     * Unlocks {@link CommandAbilityType#SATELLITE_SWEEP} for temporary wide-area recon.
     */
    SATCOM_ARRAY(
            "Satcom Array",
            "Orbital uplink—unlocks the Satellite Sweep command for brief wide-area vision.",
            BuildingCategory.TECH,
            580,
            48,
            70.0,
            40.0,
            0x6495ED,
            false,
            -40,
            -38.0,
            6,
            true,
            "SA",
            "\uD83D\uDEF0"),

    /**
     * Unlocks {@link CommandAbilityType#CARPET_BOMB} for a multi-hit strike corridor.
     */
    CARPET_PAD(
            "Carpet Bomb Pad",
            "Bomber staging field—unlocks the Carpet Bomb command along an east–west corridor.",
            BuildingCategory.TECH,
            640,
            50,
            78.0,
            43.0,
            0x556B2F,
            false,
            -38,
            -42.0,
            7,
            true,
            "CB",
            "\u2708\uFE0F"),

    TEMPEST_SPIRE(
            "Tempest Spire",
            "Anti-air guided missile launch platform.",
            BuildingCategory.DEFENSE,
            // resource cost (expensive)
            700,       // build time (seconds)
            70,      // max health
            850,     // size (radius)
            45.0,  // steel blue (storm theme)
            0x4682B4,    // cannot produce units
            false,     // power consumption
            -60,    // vision range (excellent, weather tower)
            600.0,        // faction customization point cost
            12, false,
            "\u26C8",
            "\u26C8\uFE0F");

    private final String displayName;
    private final String description;
    private final BuildingCategory buildingCategory;
    private final int resourceCost;
    private final int buildTimeSeconds;
    private final double maxHealth;
    private final double size; // radius for collision
    private final int color; // hex color for rendering
    private final boolean canProduceUnits;
    private final int powerValue; // Power generation (positive) or consumption (negative)
    private final double visionRange; // vision radius for fog of war
    private final int pointCost; // faction customization point cost
    /**
     * When true, a human player may have at most one active instance; enforced when placing
     * (see {@link RTSGameManager}). Use for command / strategic structures (e.g. {@link #STRIKE_RELAY}).
     */
    private final boolean uniquePerPlayer;
    /** Short text on the field map (client HUD). */
    private final String label;
    /** Emoji for build menus / UI (client). */
    private final String menuIcon;

    BuildingType(String displayName,
                 String description,
                 BuildingCategory buildingCategory,
                 int resourceCost,
                 int buildTimeSeconds,
                 double maxHealth,
                 double size,
                 int color,
                 boolean canProduceUnits,
                 int powerValue,
                 double visionRange,
                 int pointCost,
                 boolean uniquePerPlayer,
                 String label,
                 String menuIcon) {
        this.displayName = displayName;
        this.description = description;
        this.buildingCategory = buildingCategory;
        this.resourceCost = resourceCost;
        this.buildTimeSeconds = buildTimeSeconds;
        this.maxHealth = maxHealth;
        this.size = size;
        this.color = color;
        this.canProduceUnits = canProduceUnits;
        this.powerValue = powerValue;
        this.visionRange = visionRange;
        this.pointCost = pointCost;
        this.uniquePerPlayer = uniquePerPlayer;
        this.label = label;
        this.menuIcon = menuIcon;
    }

    public static List<BuildingType> sorted() {
        return Arrays.stream(BuildingType.values())
                .sorted(Comparator.comparing((BuildingType u) -> u.getTechRequirements().size())
                        .thenComparing(BuildingType::getResourceCost))
                .toList();
    }

    /**
     * Check if this building requires proximity to another building type
     *
     * @return the required building type, or null if no proximity requirement
     */
    public BuildingType getProximityRequirement() {
        return null;
    }

    /**
     * Get the required proximity range for buildings that need to be near another building
     *
     * @return the maximum distance in pixels, or 0 if no proximity requirement
     */
    public double getProximityRange() {
        return 0;
    }

    /**
     * Get the number of support slots this building provides for dependent buildings
     * For example, an Airfield can support N Hangars
     *
     * @return number of dependent buildings this can support, or 0 if none
     */
    public int getSupportCapacity() {
        return 0;
    }

    /**
     * Get the tech tier required to build this building
     */
    public int getRequiredTechTier() {
        return switch (this) {
            case HEADQUARTERS, REFINERY, BARRACKS, POWER_PLANT, BUNKER -> 1;
            case FACTORY, RESEARCH_LAB, TURRET, SHIELD_GENERATOR, ROCKET_TURRET, FLAK_TURRET -> 2;
            case TECH_CENTER, BANK, SANDSTORM_GENERATOR, ANDROID_FACTORY, PHOTON_SPIRE,
                 COMMAND_CITADEL, LASER_TURRET, AIRFIELD, JUMP_PAD, NUKE_SILO, STRIKE_RELAY,
                 SATCOM_ARRAY, CARPET_PAD, TEMPEST_SPIRE -> 3;
        };
    }

    /**
     * Get the weapon range for defensive buildings (for UI range indicators)
     * Returns 0 for buildings without weapons
     */
    public double getWeaponRange() {
        return switch (this) {
            case TURRET -> 300.0;
            case ROCKET_TURRET -> 400.0;
            case FLAK_TURRET -> 350.0;
            case LASER_TURRET -> 350.0;
            case PHOTON_SPIRE -> 400.0;
            case TEMPEST_SPIRE -> 450.0;
            default -> 0.0;
        };
    }

    /**
     * Get the aura radius for buildings with area effects (for UI range indicators)
     * Returns 0 for buildings without auras
     */
    public double getAuraRadius() {
        return switch (this) {
            case SHIELD_GENERATOR -> 200.0;
            case SANDSTORM_GENERATOR -> 300.0;
            default -> 0.0;
        };
    }


    /**
     * Create physics fixtures for this building type
     * This allows each building to have custom shapes (including multi-fixture compound shapes)
     * Returns a list of Convex shapes that will be added to the building's physics body
     * Most buildings return a single fixture, but complex buildings can return multiple fixtures
     * for compound shapes (e.g., L-shaped factories, star-shaped monuments)
     */
    public List<Convex> createPhysicsFixtures() {
        return switch (this) {
            // Headquarters - large octagon (main base)
            case HEADQUARTERS -> List.of(Geometry.createPolygonalCircle(8, size));

            // Refinery - hexagonal storage tanks
            case REFINERY -> List.of(Geometry.createPolygonalCircle(6, size));

            // Barracks - rectangular barracks building
            case BARRACKS -> List.of(Geometry.createRectangle(size * 1.8, size * 1.2));

            // Power Plant - hexagonal reactor
            case POWER_PLANT -> List.of(Geometry.createPolygonalCircle(6, size));

            // Factory - large rectangular factory floor
            case FACTORY -> List.of(Geometry.createRectangle(size * 2.0, size * 1.4));

            // Research Lab - hexagonal research facility
            case RESEARCH_LAB -> List.of(Geometry.createPolygonalCircle(6, size));

            // Tech Center - large octagon (advanced tech)
            case TECH_CENTER -> List.of(Geometry.createPolygonalCircle(8, size));

            // Turret - pentagonal defensive structure
            case TURRET -> List.of(Geometry.createPolygonalCircle(5, size));

            // Rocket Turret - hexagonal rocket platform
            case ROCKET_TURRET -> List.of(Geometry.createPolygonalCircle(6, size));

            // Flak Turret - hexagonal anti-aircraft platform with elevated flak cannon
            case FLAK_TURRET -> {
                // Base platform (hexagon)
                Convex base = Geometry.createPolygonalCircle(6, size * 0.8);

                // Elevated cannon mount (smaller hexagon on top)
                Convex cannonMount = Geometry.createPolygonalCircle(6, size * 0.5);
                cannonMount.translate(size * 0.2, 0);

                // Left stabilizer strut
                Vector2[] leftStrut = new Vector2[]{
                        new Vector2(-size * 0.3, -size * 0.6),
                        new Vector2(size * 0.1, -size * 0.7),
                        new Vector2(size * 0.2, -size * 0.5)
                };
                Convex strutL = Geometry.createPolygon(leftStrut);

                // Right stabilizer strut
                Vector2[] rightStrut = new Vector2[]{
                        new Vector2(-size * 0.3, size * 0.6),
                        new Vector2(size * 0.2, size * 0.5),
                        new Vector2(size * 0.1, size * 0.7)
                };
                Convex strutR = Geometry.createPolygon(rightStrut);

                yield List.of(base, cannonMount, strutL, strutR);
            }

            // Laser Turret - octagonal advanced energy turret
            case LASER_TURRET -> List.of(Geometry.createPolygonalCircle(8, size));

            // Shield Generator - hexagonal energy projector
            case SHIELD_GENERATOR -> List.of(Geometry.createPolygonalCircle(6, size));

            // Bank - octagonal vault
            case BANK -> List.of(Geometry.createPolygonalCircle(8, size));

            // Bunker - rotated hexagonal fortified structure (distinct from barracks)
            case BUNKER -> {
                // Create a hexagon rotated 30 degrees (not aligned to any axis)
                double angle = Math.PI / 6; // 30 degree rotation
                Vector2[] vertices = new Vector2[6];
                for (int i = 0; i < 6; i++) {
                    double theta = (Math.PI * 2 * i / 6) + angle;
                    vertices[i] = new Vector2(Math.cos(theta) * size, Math.sin(theta) * size);
                }
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Sandstorm Generator - hexagonal weather control station
            case SANDSTORM_GENERATOR -> List.of(Geometry.createPolygonalCircle(6, size));

            // Android Factory - octagon (advanced manufacturing)
            case ANDROID_FACTORY -> List.of(Geometry.createPolygonalCircle(8, size));

            // Photon Spire - triangle-square-triangle design (|>0<|)
            case PHOTON_SPIRE -> {
                // Central square body
                Convex centralSquare = Geometry.createSquare(size * 0.6);

                // Left triangle (pointing left) - counter-clockwise winding
                Vector2[] leftTriangle = new Vector2[]{
                        new Vector2(-size * 0.8, 0),                    // Point (left tip)
                        new Vector2(-size * 0.3, -size * 0.4),          // Bottom right (counter-clockwise)
                        new Vector2(-size * 0.3, size * 0.4)            // Top right
                };
                Convex leftWing = Geometry.createPolygon(leftTriangle);

                // Right triangle (pointing right) - counter-clockwise winding
                Vector2[] rightTriangle = new Vector2[]{
                        new Vector2(size * 0.8, 0),                     // Point (right tip)
                        new Vector2(size * 0.3, size * 0.4),            // Top left (counter-clockwise)
                        new Vector2(size * 0.3, -size * 0.4)            // Bottom left
                };
                Convex rightWing = Geometry.createPolygon(rightTriangle);

                yield List.of(leftWing, centralSquare, rightWing);
            }

            // Command Citadel - octagonal fortress tower
            case COMMAND_CITADEL -> List.of(Geometry.createPolygonalCircle(8, size));

            // Airfield - rectangular runway with control tower
            case AIRFIELD -> {
                // Main runway (large rectangle)
                Convex runway = Geometry.createRectangle(size * 2.2, size * 1.3);

                // Control tower (small square at the side)
                Convex tower = Geometry.createSquare(size * 0.4);
                tower.translate(-size * 0.9, -size * 0.6);

                yield List.of(runway, tower);
            }

            case JUMP_PAD -> List.of(Geometry.createRectangle(size * 1.9, size * 1.05));

            case NUKE_SILO -> List.of(Geometry.createPolygonalCircle(8, size));

            case STRIKE_RELAY -> List.of(Geometry.createPolygonalCircle(6, size));

            case SATCOM_ARRAY -> List.of(Geometry.createPolygonalCircle(8, size));

            case CARPET_PAD -> List.of(Geometry.createRectangle(size * 1.8, size * 0.95));

            // Tempest Spire - weather control tower with antenna arrays
            case TEMPEST_SPIRE -> {
                // Central tower (tall octagon)
                Convex tower = Geometry.createPolygonalCircle(8, size * 0.6);

                // Weather sensor array (top)
                Vector2[] sensor = new Vector2[]{
                        new Vector2(-size * 0.4, -size * 0.7),
                        new Vector2(size * 0.4, -size * 0.7),
                        new Vector2(size * 0.5, -size * 0.5),
                        new Vector2(-size * 0.5, -size * 0.5)
                };
                Convex sensorArray = Geometry.createPolygon(sensor);

                // Left antenna
                Vector2[] leftAntenna = new Vector2[]{
                        new Vector2(-size * 0.7, -size * 0.3),
                        new Vector2(-size * 0.5, -size * 0.5),
                        new Vector2(-size * 0.4, -size * 0.2)
                };
                Convex antennaL = Geometry.createPolygon(leftAntenna);

                // Right antenna
                Vector2[] rightAntenna = new Vector2[]{
                        new Vector2(size * 0.7, -size * 0.3),
                        new Vector2(size * 0.4, -size * 0.2),
                        new Vector2(size * 0.5, -size * 0.5)
                };
                Convex antennaR = Geometry.createPolygon(rightAntenna);

                yield List.of(tower, sensorArray, antennaL, antennaR);
            }
        };
    }

    /**
     * Get tech requirements for this building type.
     * Matches the logic in RTSGameManager.hasTechRequirements()
     */
    public List<BuildingType> getTechRequirements() {
        return switch (this) {
            // T1 - Always available
            case HEADQUARTERS, POWER_PLANT, BARRACKS, REFINERY, BUNKER -> List.of();

            // T2 - Requires Power Plant
            case RESEARCH_LAB, FACTORY, TURRET, ROCKET_TURRET, FLAK_TURRET, SHIELD_GENERATOR -> List.of(POWER_PLANT);

            // T3 - Requires Power Plant + Research Lab
            case TECH_CENTER, BANK, LASER_TURRET, AIRFIELD -> List.of(POWER_PLANT, RESEARCH_LAB);

            // Requires Power Plant + Research Lab  + TECH_CENTER
            case SANDSTORM_GENERATOR, ANDROID_FACTORY, PHOTON_SPIRE, COMMAND_CITADEL, JUMP_PAD, NUKE_SILO, STRIKE_RELAY,
                 SATCOM_ARRAY, CARPET_PAD, TEMPEST_SPIRE ->
                    List.of(POWER_PLANT, RESEARCH_LAB, TECH_CENTER);
        };
    }

}

