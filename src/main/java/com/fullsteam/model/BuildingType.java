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
 */
@Getter
public enum BuildingType {
    HEADQUARTERS(
            "Headquarters",
            "Your main base—trains workers, generates power, and must survive to stay in the fight.",
            BuildingCategory.ECONOMY,
            0,
            0,
            5000,
            80.0,
            0xFFD700,
            true,
            50,
            500.0,
            0, false,
            "H",
            "\uD83C\uDFDB️",
            null),

    REFINERY(
            "Refinery",
            "Resource drop-off for workers; extends your economy beyond the starting stockpile.",
            BuildingCategory.ECONOMY,
            300,
            20,
            600,
            50.0,
            0x808080,
            false,
            -10,
            350.0,
            2, false,
            "R",
            "\uD83C\uDFED",
            'R'),

    BARRACKS(
            "Barracks",
            "Produces infantry—from basic riflemen to medics, engineers, and elite specialists.",
            BuildingCategory.PRODUCTION,
            200,
            15,
            550,
            45.0,
            0x8B4513,
            true,
            -25,
            380.0,
            3, false,
            "B",
            "\uD83C\uDFF0",
            'B'),

    POWER_PLANT(
            "Power Plant",
            "Generates electricity; build more before advanced structures brown out your grid.",
            BuildingCategory.ECONOMY,
            250,
            20,
            400,
            40.0,
            0xFFFF00,
            false,
            100,
            360.0,
            0, false,
            "P",
            "⚡",
            'P'),

    FACTORY(
            "Factory",
            "Vehicle production—from scouts and transports to tanks, artillery, and super-heavies.",
            BuildingCategory.PRODUCTION,
            400,
            25,
            800,
            55.0,
            0x696969,
            true,
            -30,
            390.0,
            5, false,
            "F",
            "\uD83D\uDE97",
            'F'),

    RESEARCH_LAB(
            "Research Lab",
            "Standard tech structure unlocks tier-2 tech.",
            BuildingCategory.TECH,
            500,
            30,
            700,
            50.0,
            0x00CED1,
            false,
            -35,
            400.0,
            3, false,
            "RL",
            "\uD83D\uDD2C",
            'E'),

    TECH_CENTER(
            "Tech Center",
            "High end research hub unlocks tier-3 tech.",
            BuildingCategory.TECH,
            800,
            40,
            900,
            60.0,
            0x4169E1,
            false,
            -50,
            420.0,
            5, false,
            "TC",
            "\uD83E\uDDEA",
            'C'),

    TURRET(
            "Turret",
            "Automated cannon emplacement—reliable general-purpose base and chokepoint defense.",
            BuildingCategory.DEFENSE,
            250,
            15,
            500,
            25.0,
            0xFF4500,
            false,
            -35,
            450.0,
            2, false,
            "T",
            "\uD83C\uDFAF",
            'T'),

    ROCKET_TURRET(
            "Rocket Turret",
            "Long-range rocket battery—explosive volleys excel versus armor and grouped targets.",
            BuildingCategory.DEFENSE,
            350,
            20,
            400,
            25.0,
            0xFF6347,
            false,
            -50,
            480.0,
            3, false,
            "RT",
            "\uD83D\uDE80",
            'O'),

    FLAK_TURRET(
            "Flak Turret",
            "Dedicated anti-air turret shredding low-altitude aircraft with flak bursts.",
            BuildingCategory.DEFENSE,
            300,
            18,
            450,
            25.0,
            0xA0A0A0,
            false,
            -45,
            500.0,
            3, false,
            "FT",
            "\uD83D\uDCA5",
            'X'),

    LASER_TURRET(
            "Laser Turret",
            "High-tech beam turret—long reach, sustained damage, and hungry power draw.",
            BuildingCategory.DEFENSE,
            400,
            25,
            350,
            25.0,
            0x00FFFF,
            false,
            -65,
            500.0,
            4, false,
            "LT",
            "\uD83D\uDD37",
            'L'),

    BUNKER(
            "Bunker",
            "Hardened garrison structure—infantry inside gain protection and use defensive fireports.",
            BuildingCategory.DEFENSE,
            250,
            18,
            1200,
            35.0,
            0x556B2F,
            false,
            -15,
            420.0,
            3, false,
            "⚔",
            "\uD83C\uDFF0",
            'U'),

    SHIELD_GENERATOR(
            "Shield Generator",
            "Projects a bubble shield that blocks hostile projectiles for units and structures inside.",
            BuildingCategory.DEFENSE,
            400,
            25,
            500,
            30.0,
            0x00BFFF,
            false,
            -40,
            380.0,
            4, false,
            "SG",
            "\uD83D\uDEE1️",
            'Y'),

    BANK(
            "Bank",
            "Generates interest income based on current credit count.",
            BuildingCategory.ECONOMY,
            600,
            30,
            420,
            35.0,
            0xFFD700,
            false,
            -30,
            350.0,
            4,
            false,
            "$",
            "\uD83D\uDCB0",
            'G'),

    SANDSTORM_GENERATOR(
            "Sandstorm Generator",
            "Summons a sandstorm aura that damages and disrupts enemies caught in the storm.",
            BuildingCategory.DEFENSE,
            600,
            60,
            800,
            35.0,
            0xDEB887,
            false,
            -40,
            430.0,
            8,
            false,
            "☁",
            "\uD83C\uDF2A️",
            'Q'),

    ANDROID_FACTORY(
            "Android Factory",
            "Autonomous factory that continuously builds free Android combat units without a queue.",
            BuildingCategory.PRODUCTION,
            700,
            90,
            900,
            42.0,
            0x00CED1,
            true,
            -60,
            420.0,
            12,
            false,
            "A",
            "\uD83E\uDD16",
            null),

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
            10,
            false,
            "⚡",
            "\uD83D\uDC8E",
            'H'),

    AIRFIELD(
            "Airfield",
            "Produces and houses aircraft—VTOL gunships plus sortie-based bombers and fighters.",
            BuildingCategory.PRODUCTION,
            600,
            35,
            700,
            60.0,
            0x708090,
            true,
            -40,
            420.0,
            5,
            false,
            "AF",
            "✈️",
            'I'),

    JUMP_PAD(
            "Jump Pad",
            "Drop-ship landing grid and uplink—unlocks the Marine Drop command.",
            BuildingCategory.TECH,
            2620,
            280,
            75.0,
            44.0,
            0x4A708B,
            false,
            -35,
            -42.0,
            2,
            true,
            "JP",
            "\uD83D\uDE81",
            'J'),

    NUKE_SILO(
            "Nuclear Silo",
            "Houses and arms a strategic warhead; arm on command, then launch at a ground target.",
            BuildingCategory.TECH,
            2950,
            360,
            100.0,
            48.0,
            0x8B0000,
            false,
            -150,
            -45.0,
            8,
            true,
            "NS",
            "☢️",
            'N'),

    STRIKE_RELAY(
            "Strike Relay",
            "Tactical uplink—unlocks the Strike Package command ability.",
            BuildingCategory.TECH,
            2550,
            280,
            85.0,
            42.0,
            0xCD853F,
            false,
            -35,
            -40.0,
            5,
            true,
            "SR",
            "\uD83C\uDFAF",
            'V'),

    SATCOM_ARRAY(
            "Satcom Array",
            "Orbital uplink—unlocks the Satellite Sweep command for brief wide-area vision.",
            BuildingCategory.TECH,
            2580,
            220,
            70.0,
            40.0,
            0x6495ED,
            false,
            -40,
            -38.0,
            5,
            true,
            "SA",
            "\uD83D\uDEF0",
            'M'),

    TEMPEST_SPIRE(
            "Tempest Spire",
            "Anti-air guided missile launch platform.",
            BuildingCategory.DEFENSE,
            700,
            70,
            850,
            45.0,
            0x4682B4,
            false,
            -60,
            600.0,
            12,
            false,
            "⛈",
            "⛈️",
            'Z');

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
    /**
     * Short text on the field map (client HUD).
     */
    private final String label;
    /**
     * Emoji for build menus / UI (client).
     */
    private final String menuIcon;
    /**
     * Worker build-menu hotkey (single display character). Null when unassigned; never duplicates
     * another non-null hotkey among placeable buildings (see tests).
     */
    private final Character hotkey;

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
                 String menuIcon,
                 Character hotkey) {
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
        this.hotkey = hotkey;
    }

    public static List<BuildingType> sorted() {
        return Arrays.stream(BuildingType.values())
                .sorted(Comparator.comparing((BuildingType u) -> u.getTechRequirements().size())
                        .thenComparing(BuildingType::getResourceCost))
                .toList();
    }

    /**
     * Get the tech tier required to build this building
     */
    public int getRequiredTechTier() {
        return switch (this) {
            case HEADQUARTERS, REFINERY, BARRACKS, POWER_PLANT, BUNKER -> 1;
            case FACTORY, RESEARCH_LAB, TURRET, SHIELD_GENERATOR, ROCKET_TURRET, FLAK_TURRET -> 2;
            case TECH_CENTER, BANK, SANDSTORM_GENERATOR, ANDROID_FACTORY, PHOTON_SPIRE,
                 LASER_TURRET, AIRFIELD, JUMP_PAD, NUKE_SILO, STRIKE_RELAY,
                 SATCOM_ARRAY, TEMPEST_SPIRE -> 3;
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
            case SANDSTORM_GENERATOR, ANDROID_FACTORY, PHOTON_SPIRE, JUMP_PAD, NUKE_SILO, STRIKE_RELAY,
                 SATCOM_ARRAY, TEMPEST_SPIRE -> List.of(POWER_PLANT, RESEARCH_LAB, TECH_CENTER);
        };
    }

}

