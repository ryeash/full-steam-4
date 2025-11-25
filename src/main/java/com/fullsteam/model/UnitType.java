package com.fullsteam.model;

import lombok.Getter;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Defines the different types of units available in the RTS game.
 * Each unit type has specific attributes like cost, health, speed, damage, etc.
 * Shape rendering is handled by the number of sides (3=triangle, 4=rectangle, etc.)
 */
@Getter
public enum UnitType {
    // Worker unit - can harvest resources and construct buildings
    WORKER(
            "Worker",
            50,      // resource cost
            10,      // build time (seconds)
            75,      // max health (+50%)
            100.0,   // movement speed
            5,       // damage
            1.0,     // attack rate
            100,     // attack range
            15.0,    // size (radius)
            16,      // sides (16-sided polygon approximates circle)
            0xFFFF00, // yellow
            BuildingType.HEADQUARTERS,
            5,       // upkeep cost
            300.0    // vision range (moderate)
    ),

    // Infantry - basic combat unit
    INFANTRY(
            "Infantry",
            75,      // resource cost
            5,       // build time (seconds)
            128,     // max health (+60%)
            120.0,   // movement speed
            18,      // damage (+30% vs beam infantry)
            2.0,     // attack rate
            170,     // attack range (+15% vs beam infantry)
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x00FF00, // green
            BuildingType.BARRACKS,
            10,      // upkeep cost
            350.0    // vision range (standard infantry)
    ),

    // Laser Infantry - advanced infantry with beam weapons
    LASER_INFANTRY(
            "Laser Infantry",
            125,     // resource cost (more expensive than regular infantry)
            7,       // build time (seconds)
            128,     // max health (+60%)
            120.0,   // movement speed (same as infantry)
            20,      // damage (higher than infantry)
            1.5,     // attack rate (faster than infantry)
            180,     // attack range (longer than infantry)
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x00FFFF, // cyan (to distinguish from regular infantry)
            BuildingType.BARRACKS,
            12,      // upkeep cost (higher than infantry)
            360.0    // vision range (slightly better than infantry)
    ),

    // Medic - support unit that heals nearby friendlies
    MEDIC(
            "Medic",
            100,     // resource cost
            8,       // build time (seconds)
            90,      // max health (+50%)
            110.0,   // movement speed
            0,       // damage (cannot attack)
            0.0,     // attack rate
            0,       // attack range
            12.0,    // size (radius)
            6,       // sides (hexagon)
            0xFFFFFF, // white
            BuildingType.BARRACKS,
            8,       // upkeep cost
            340.0    // vision range (support unit, moderate)
    ),

    // Rocket Soldier - anti-vehicle infantry
    ROCKET_SOLDIER(
            "Rocket Soldier",
            150,     // resource cost
            8,       // build time (seconds)
            112,     // max health (+60%)
            110.0,   // movement speed
            40,      // damage
            0.8,     // attack rate (slower)
            200,     // attack range
            12.0,    // size (radius)
            3,       // sides (triangle)
            0xFF8800, // orange
            BuildingType.WEAPONS_DEPOT,
            15,      // upkeep cost
            370.0    // vision range (good, needs to spot vehicles)
    ),

    // Sniper - long-range precision unit
    SNIPER(
            "Sniper",
            200,     // resource cost
            10,      // build time (seconds)
            80,      // max health (+60%, still fragile)
            100.0,   // movement speed
            65,      // damage (+30% vs beam sniper)
            0.5,     // attack rate (slow, precise shots)
            345,     // attack range (+15% vs beam sniper)
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x8B4513, // brown
            BuildingType.WEAPONS_DEPOT,
            12,      // upkeep cost
            500.0    // vision range (excellent, sniper needs vision)
    ),

    // Engineer - repairs buildings and vehicles
    ENGINEER(
            "Engineer",
            150,     // resource cost
            12,      // build time (seconds)
            105,     // max health (+50%)
            105.0,   // movement speed
            0,       // damage (cannot attack)
            0.0,     // attack rate
            0,       // attack range
            13.0,    // size (radius)
            6,       // sides (hexagon)
            0x00CED1, // dark turquoise (distinct from yellow worker)
            BuildingType.WEAPONS_DEPOT,
            10,      // upkeep cost
            330.0    // vision range (support unit)
    ),

    // Miner - destroys obstacles to clear paths
    MINER(
            "Miner",
            100,     // resource cost
            10,      // build time (seconds)
            90,      // max health (+50%)
            95.0,    // movement speed (slower than worker)
            0,       // damage (cannot attack)
            0.0,     // attack rate
            0,       // attack range
            14.0,    // size (radius)
            8,       // sides (octagon)
            0x8B4513, // brown (like dirt/stone)
            BuildingType.HEADQUARTERS,
            8,       // upkeep cost
            320.0    // vision range (utility unit)
    ),

    // Jeep - fast light vehicle
    JEEP(
            "Jeep",
            200,     // resource cost
            10,      // build time (seconds)
            168,     // max health (+40%)
            180.0,   // movement speed (fast!)
            26,      // damage (+30% vs beam scout)
            3.0,     // attack rate
            207,     // attack range (+15% vs beam scout)
            20.0,    // size (radius)
            4,       // sides (rectangle)
            0x00FFFF, // cyan
            BuildingType.FACTORY,
            20,      // upkeep cost
            450.0    // vision range (scout vehicle, excellent vision)
    ),

    // Tank - heavy armored vehicle
    TANK(
            "Tank",
            400,     // resource cost
            15,      // build time (seconds)
            390,     // max health (+30%)
            80.0,    // movement speed (slow)
            68,      // damage (+30% vs beam tank)
            1.2,     // attack rate
            240,     // attack range (+15% vs beam tank)
            30.0,    // size (radius)
            5,       // sides (pentagon)
            0x8888FF, // light blue
            BuildingType.FACTORY,
            30,      // upkeep cost
            400.0    // vision range (good, main battle tank)
    ),

    // Artillery - long range siege unit
    ARTILLERY(
            "Artillery",
            500,     // resource cost
            20,      // build time (seconds)
            180,     // max health (+20%)
            60.0,    // movement speed (very slow)
            117,     // damage (+30% vs beam artillery)
            0.5,     // attack rate (very slow)
            437,     // attack range (+15% vs beam artillery)
            25.0,    // size (radius)
            6,       // sides (hexagon)
            0xFF00FF, // magenta
            BuildingType.ADVANCED_FACTORY,
            40,      // upkeep cost
            420.0    // vision range (good, needs to spot targets)
    ),

    // GIGANTONAUT - Super heavy artillery
    GIGANTONAUT(
            "Gigantonaut",
            1200,    // resource cost (VERY EXPENSIVE!)
            35,      // build time (seconds) (LONG!)
            360,     // max health (+20%)
            30.0,    // movement speed (SLOWEST!)
            250,     // damage (MASSIVE!)
            0.3,     // attack rate (EXTREMELY SLOW!)
            450,     // attack range (LONGEST!)
            35.0,    // size (radius) (HUGE!)
            8,       // sides (octagon)
            0x8B0000, // dark red
            BuildingType.ADVANCED_FACTORY,
            60,      // upkeep cost (VERY HIGH!)
            200.0    // poor vision, needs a spotter to hit distant targets
    ),

    // CRAWLER - Mobile fortress with 4 turrets (THE STAR UNIT!)
    CRAWLER(
            "Crawler",
            1500,    // resource cost (EXPENSIVE!)
            45,      // build time (seconds) (LONG!)
            2300,    // max health (+15%)
            40.0,    // movement speed (VERY SLOW!)
            35,      // damage (per turret) - reduced from 60
            1.5,     // attack rate - reduced from 1.0 (slower fire rate)
            250,     // attack range
            50.0,    // size (radius) (HUGE!)
            8,       // sides (octagon)
            0x4A4A4A, // dark gray
            BuildingType.ADVANCED_FACTORY,
            80,      // upkeep cost (HIGHEST!)
            480.0    // vision range (excellent, mobile fortress)
    ),

    // Stealth Tank - invisible until attacking
    STEALTH_TANK(
            "Stealth Tank",
            800,     // resource cost
            25,      // build time (seconds)
            260,     // max health (+30%)
            100.0,   // movement speed
            50,      // damage
            1.5,     // attack rate
            200,     // attack range
            28.0,    // size (radius)
            5,       // sides (pentagon)
            0x2F4F4F, // dark slate gray
            BuildingType.ADVANCED_FACTORY,
            45,      // upkeep cost
            380.0    // vision range (moderate, stealth unit)
    ),

    // Mammoth Tank - dual-cannon heavy assault
    MAMMOTH_TANK(
            "Mammoth Tank",
            1200,    // resource cost
            35,      // build time (seconds)
            1800,    // max health (+20%)
            50.0,    // movement speed (very slow)
            80,      // damage (high)
            0.8,     // attack rate (slow but powerful)
            230,     // attack range
            40.0,    // size (radius) (large)
            6,       // sides (hexagon)
            0x556B2F, // dark olive green
            BuildingType.ADVANCED_FACTORY,
            60,      // upkeep cost
            430.0    // vision range (good, heavy assault)
    ),

    // ===== HERO UNITS =====

    // PALADIN - Terran hero unit, balanced powerhouse
    PALADIN(
            "Paladin",
            1100,    // resource cost
            32,      // build time (seconds)
            1105,    // max health (+30%)
            95.0,    // movement speed (moderate)
            70,      // damage (high)
            1.5,     // attack rate (balanced)
            220,     // attack range
            32.0,    // size (radius)
            8,       // sides (octagon - balanced)
            0xC0C0C0, // silver (Terran hero)
            BuildingType.ADVANCED_FACTORY,
            55,      // upkeep cost
            500.0    // vision range (hero unit, excellent vision)
    ),

    // RAIDER - Nomads hero unit, fast hit-and-run cavalry
    RAIDER(
            "Raider",
            900,     // resource cost
            28,      // build time (seconds)
            364,     // max health (+30%)
            220.0,   // movement speed (VERY FAST - fastest unit!)
            55,      // damage (high)
            2.2,     // attack rate (fast)
            180,     // attack range
            22.0,    // size (radius)
            3,       // sides (triangle - agile)
            0xDC143C, // crimson (raider red)
            BuildingType.ADVANCED_FACTORY,
            45,      // upkeep cost
            520.0    // vision range (hero scout, exceptional vision)
    ),

    // COLOSSUS - Synthesis hero unit, massive walker
    COLOSSUS(
            "Colossus",
            1600,    // resource cost (VERY EXPENSIVE!)
            45,      // build time (seconds) (VERY LONG!)
            2640,    // max health (+20%)
            40.0,    // movement speed (VERY SLOW)
            95,      // damage (very high)
            0.9,     // attack rate (moderate)
            250,     // attack range
            50.0,    // size (radius) (MASSIVE!)
            6,       // sides (hexagon)
            0x4B0082, // indigo (synthesis purple)
            BuildingType.ADVANCED_FACTORY,
            75,      // upkeep cost (VERY HIGH!)
            490.0    // vision range (hero unit, excellent vision)
    ),

    // ===== TECH ALLIANCE BEAM WEAPON UNITS =====

    // PLASMA_TROOPER - Basic beam infantry (Tech Alliance equivalent of Infantry)
    PLASMA_TROOPER(
            "Plasma Trooper",
            100,     // resource cost
            6,       // build time (seconds)
            136,     // max health (+60%)
            115.0,   // movement speed
            14,      // damage (instant hit beam weapon)
            2.0,     // attack rate (fast)
            148,     // attack range (instant hit beam weapon)
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x00FF7F, // spring green (plasma color)
            BuildingType.BARRACKS,
            11,      // upkeep cost
            355.0    // vision range (standard beam infantry)
    ),

    // ION_RANGER - Long-range beam sniper
    ION_RANGER(
            "Ion Ranger",
            250,     // resource cost
            12,      // build time (seconds)
            96,      // max health (+60%)
            105.0,   // movement speed
            50,      // damage (instant hit beam weapon)
            0.6,     // attack rate (slow, precise)
            300,     // attack range (instant hit beam weapon)
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x9370DB, // medium purple (ion beam)
            BuildingType.WEAPONS_DEPOT,
            14,      // upkeep cost
            510.0    // vision range (excellent, beam sniper)
    ),

    // PHOTON_SCOUT - Fast beam vehicle
    PHOTON_SCOUT(
            "Photon Scout",
            220,     // resource cost
            11,      // build time (seconds)
            154,     // max health (+40%)
            190.0,   // movement speed (very fast!)
            20,      // damage (instant hit beam weapon)
            2.5,     // attack rate (rapid fire)
            180,     // attack range (instant hit beam weapon)
            18.0,    // size (radius)
            4,       // sides (rectangle)
            0x7FFF00, // chartreuse (bright energy)
            BuildingType.FACTORY,
            22,      // upkeep cost
            460.0    // vision range (excellent, scout vehicle)
    ),

    // BEAM_TANK - Heavy beam vehicle
    BEAM_TANK(
            "Beam Tank",
            450,     // resource cost
            16,      // build time (seconds)
            416,     // max health (+30%)
            75.0,    // movement speed (slow)
            52,      // damage (instant hit beam weapon)
            1.3,     // attack rate
            209,     // attack range (instant hit beam weapon)
            30.0,    // size (radius)
            6,       // sides (hexagon)
            0x00FA9A, // medium spring green
            BuildingType.FACTORY,
            32,      // upkeep cost
            410.0    // vision range (good, beam tank)
    ),

    // PULSE_ARTILLERY - Long-range beam artillery
    PULSE_ARTILLERY(
            "Pulse Artillery",
            550,     // resource cost
            22,      // build time (seconds)
            168,     // max health (+20%)
            55.0,    // movement speed (very slow)
            90,      // damage (instant hit beam weapon)
            0.6,     // attack rate (slow)
            380,     // attack range (instant hit beam weapon)
            26.0,    // size (radius)
            6,       // sides (hexagon)
            0xFFD700, // gold (energy pulse)
            BuildingType.ADVANCED_FACTORY,
            42,      // upkeep cost
            430.0    // vision range (good, beam artillery)
    ),

    // PHOTON_TITAN - Hero unit, massive beam platform
    PHOTON_TITAN(
            "Photon Titan",
            1400,    // resource cost (VERY EXPENSIVE!)
            40,      // build time (seconds) (LONG!)
            420,     // max health (+20%)
            35.0,    // movement speed (VERY SLOW!)
            280,     // damage (MASSIVE!)
            0.4,     // attack rate (slow but devastating)
            460,     // attack range (LONGEST!)
            38.0,    // size (radius) (HUGE!)
            8,       // sides (octagon)
            0x00FF00, // bright green (pure energy)
            BuildingType.ADVANCED_FACTORY,
            65,      // upkeep cost (VERY HIGH!)
            480.0    // vision range (hero unit, excellent vision)
    ),

    // ANDROID - Autonomous combat unit produced by Android Factory
    // Free to produce, zero upkeep, controlled by Android Factory
    ANDROID(
            "Android",
            0,       // resource cost (FREE!)
            15,      // build time (seconds)
            100,     // max health (moderate)
            110.0,   // movement speed (moderate)
            22,      // damage (decent)
            1.5,     // attack rate (good)
            180,     // attack range (good)
            13.0,    // size (radius)
            4,       // sides (square/diamond)
            0x00CED1, // dark turquoise (Synthesis faction color)
            BuildingType.ANDROID_FACTORY,
            0,       // upkeep cost (ZERO!)
            340.0    // vision range (moderate, autonomous unit)
    );

    private final String displayName;
    private final int resourceCost;
    private final int buildTimeSeconds;
    private final double maxHealth;
    private final double movementSpeed;
    private final double damage;
    private final double attackRate; // attacks per second
    private final double attackRange;
    private final double size; // radius for collision
    private final int sides; // number of sides for polygon rendering
    private final int color; // hex color for rendering
    private final BuildingType producedBy; // which building produces this unit
    private final int upkeepCost; // supply/upkeep cost
    /**
     * -- GETTER --
     *  Get vision range for this unit type
     *  Most units: 1.5x attack range
     *  Gigantonaut: terrible vision (0.5x attack range)
     */
    private final double visionRange; // vision radius for fog of war

    /**
     * Create physics fixtures for this unit type
     * This allows each unit to have custom shapes (including multi-fixture compound shapes)
     * Returns a list of Convex shapes that will be added to the unit's physics body
     * Most units return a single fixture, but complex units can return multiple fixtures
     * for compound shapes (e.g., hourglass, dumbbell, star shapes)
     */
    public List<Convex> createPhysicsFixtures() {
        return switch (this) {
            // Basic Infantry - standard triangle (pointing forward)
            case INFANTRY, LASER_INFANTRY, PLASMA_TROOPER -> List.of(Geometry.createPolygonalCircle(3, size));

            // Rocket Soldier - wider pentagon (anti-vehicle specialist with launcher)
            case ROCKET_SOLDIER -> {
                // Pentagon with wider shoulders for rocket launcher
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 0.8, -size * 0.6),// Back left
                        new Vector2(size * 0.3, -size * 0.9), // Left shoulder (wide)
                        new Vector2(size * 1.1, 0),           // Front point (pointing right)
                        new Vector2(size * 0.3, size * 0.9),  // Right shoulder (wide)
                        new Vector2(-size * 0.8, size * 0.6)  // Back right
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Sniper - elongated narrow triangle (long rifle look)
            case SNIPER -> {
                // Symmetric triangle like infantry, but longer and narrower
                // Pointing right (positive X direction, 0 radians) to match other infantry
                // Counter-clockwise winding: back-bottom -> front -> back-top
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 0.7, -size * 0.5),// Back bottom (narrow base)
                        new Vector2(size * 1.3, 0),           // Front point (long barrel, pointing right)
                        new Vector2(-size * 0.7, size * 0.5)  // Back top (narrow base)
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Ion Ranger - elongated pentagon (advanced beam sniper)
            case ION_RANGER -> {
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.5),// Back left
                        new Vector2(size * 0.2, -size * 0.7), // Left side
                        new Vector2(size * 1.3, 0),           // Front point (beam emitter, pointing right)
                        new Vector2(size * 0.2, size * 0.7),  // Right side
                        new Vector2(-size * 0.9, size * 0.5)  // Back right
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Worker/Support units - circular for easy navigation
            case WORKER, MINER, MEDIC, ENGINEER -> List.of(Geometry.createCircle(size));

            // Android - diamond/square shape (synthetic unit)
            case ANDROID -> List.of(Geometry.createPolygonalCircle(4, size));

            // Light vehicles - elongated rectangle (fast, nimble)
            case JEEP, PHOTON_SCOUT -> List.of(Geometry.createRectangle(size * 1.6, size));

            // Standard Tank - hexagonal turret platform (balanced)
            case TANK -> List.of(Geometry.createPolygonalCircle(6, size));

            // Beam Tank - wider hexagon (beam weapon platform)
            case BEAM_TANK -> {
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 0.9, 0),          // Back
                        new Vector2(-size * 0.4, -size * 1.1),// Back left (wider)
                        new Vector2(size * 0.4, -size * 1.1), // Front left (wider)
                        new Vector2(size * 0.9, 0),           // Front (pointing right)
                        new Vector2(size * 0.4, size * 1.1),  // Front right (wider)
                        new Vector2(-size * 0.4, size * 1.1)  // Back right (wider)
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Stealth Tank - sleek diamond (low profile, streamlined)
            case STEALTH_TANK -> {
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 1.0, 0),          // Back point
                        new Vector2(0, -size * 0.7),          // Left point (narrow)
                        new Vector2(size * 1.0, 0),           // Front point (sleek, pointing right)
                        new Vector2(0, size * 0.7)            // Right point (narrow)
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Mammoth Tank - massive wide rectangle (dual-cannon heavy assault)
            case MAMMOTH_TANK -> {
                // Wide, imposing rectangle with angled front
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.8),// Back left
                        new Vector2(-size * 0.6, -size * 1.1),// Left side back
                        new Vector2(size * 0.6, -size * 1.1), // Left side front
                        new Vector2(size * 1.1, -size * 0.8), // Front left (angled, pointing right)
                        new Vector2(size * 1.1, size * 0.8),  // Front right (angled)
                        new Vector2(size * 0.6, size * 1.1),  // Right side front
                        new Vector2(-size * 0.6, size * 1.1), // Right side back
                        new Vector2(-size * 0.9, size * 0.8)  // Back right
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Artillery - elongated pentagon (long barrel siege weapon)
            case ARTILLERY -> {
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 0.8, -size * 0.7),// Back left
                        new Vector2(size * 0.3, -size * 0.8), // Left side
                        new Vector2(size * 1.3, 0),           // Front (long barrel, pointing right)
                        new Vector2(size * 0.3, size * 0.8),  // Right side
                        new Vector2(-size * 0.8, size * 0.7)  // Back right
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Pulse Artillery - wide hexagon (beam artillery platform)
            case PULSE_ARTILLERY -> {
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 1.0, 0),          // Back
                        new Vector2(-size * 0.5, -size * 1.0),// Back left (wide)
                        new Vector2(size * 0.5, -size * 1.0), // Front left (wide)
                        new Vector2(size * 1.2, 0),           // Front (beam emitter, pointing right)
                        new Vector2(size * 0.5, size * 1.0),  // Front right (wide)
                        new Vector2(-size * 0.5, size * 1.0)  // Back right (wide)
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Gigantonaut - trapezoid (wide at back, tapered at front for heavy artillery look)
            case GIGANTONAUT -> {
                // Create a trapezoid: wide at back, narrow at front
                // Vertices must be in counter-clockwise order for dyn4j
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 0.8, -size * 0.9),  // Back left (wide)
                        new Vector2(size * 1.2, -size * 0.6),  // Front left (tapered)
                        new Vector2(size * 1.2, size * 0.6),  // Front right (tapered)
                        new Vector2(-size * 0.8, size * 0.9)   // Back right (wide)
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Crawler - dumbbell shape (MULTI-FIXTURE PROOF OF CONCEPT!)
            // This is a compound shape: two circular "treads" connected by a rectangular body
            case CRAWLER -> {
                // Create a dumbbell/tank-tread shape with 3 fixtures
                Convex frontTread = Geometry.createCircle(size * .6);
                frontTread.translate(size * .3, 0);
                Convex rearTread = Geometry.createCircle(size * .6);
                rearTread.translate(-size * .3, 0);
                Convex body = Geometry.createRectangle(size * 1.8, size);
                yield List.of(frontTread, rearTread, body);
            }

            // Paladin - shield shape (Terran hero knight)
            case PALADIN -> {
                // Shield-like hexagon
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 1.0, 0),          // Bottom point (back)
                        new Vector2(-size * 0.6, -size * 0.9),// Left bottom
                        new Vector2(size * 0.5, -size * 1.0), // Left shoulder
                        new Vector2(size * 1.2, 0),           // Top point (shield front, pointing right)
                        new Vector2(size * 0.5, size * 1.0),  // Right shoulder
                        new Vector2(-size * 0.6, size * 0.9)  // Right bottom
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Raider - elongated arrow (fast cavalry)
            case RAIDER -> {
                // Stretched triangle like an arrow (fast, agile)
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.8),// Back left (wide stance)
                        new Vector2(size * 1.4, 0),           // Front point (very long, pointing right)
                        new Vector2(-size * 0.9, size * 0.8)  // Back right (wide stance)
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Photon Titan - energy platform hexagon (massive beam hero)
            case PHOTON_TITAN -> {
                // Wide hexagonal energy platform
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 1.1, 0),          // Back
                        new Vector2(-size * 0.6, -size * 1.2),// Back left (very wide)
                        new Vector2(size * 0.6, -size * 1.2), // Front left (very wide)
                        new Vector2(size * 1.1, 0),           // Front (pointing right)
                        new Vector2(size * 0.6, size * 1.2),  // Front right (very wide)
                        new Vector2(-size * 0.6, size * 1.2)  // Back right (very wide)
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Colossus - wide diamond (imposing massive walker)
            case COLOSSUS -> {
                // Create a wide diamond: wider than it is long for imposing presence
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size * 0.7, 0),          // Back point (rear)
                        new Vector2(0, -size * 1.3),          // Left point (wide)
                        new Vector2(size * 0.7, 0),           // Front point (forward, pointing right)
                        new Vector2(0, size * 1.3)            // Right point (wide)
                };
                yield List.of(Geometry.createPolygon(vertices));
            }
        };
    }

    UnitType(String displayName, int resourceCost, int buildTimeSeconds, double maxHealth,
             double movementSpeed, double damage, double attackRate, double attackRange,
             double size, int sides, int color, BuildingType producedBy, int upkeepCost, double visionRange) {
        this.displayName = displayName;
        this.resourceCost = resourceCost;
        this.buildTimeSeconds = buildTimeSeconds;
        this.maxHealth = maxHealth;
        this.movementSpeed = movementSpeed;
        this.damage = damage;
        this.attackRate = attackRate;
        this.attackRange = attackRange;
        this.size = size;
        this.sides = sides;
        this.color = color;
        this.producedBy = producedBy;
        this.upkeepCost = upkeepCost;
        this.visionRange = visionRange;
    }

    /**
     * Get the special ability for this unit type
     */
    public SpecialAbility getSpecialAbility() {
        return switch (this) {
            case CRAWLER -> SpecialAbility.DEPLOY;
            case MEDIC -> SpecialAbility.HEAL;
            case ENGINEER -> SpecialAbility.REPAIR;
            case STEALTH_TANK -> SpecialAbility.STEALTH;
            default -> SpecialAbility.NONE;
        };
    }

    /**
     * Check if this unit has a special ability
     */
    public boolean hasSpecialAbility() {
        return getSpecialAbility() != SpecialAbility.NONE;
    }

    /**
     * Check if this unit can attack
     */
    public boolean canAttack() {
        return this != WORKER && this != MEDIC && this != ENGINEER && this != MINER;
    }

    /**
     * Check if this unit can harvest resources
     */
    public boolean canHarvest() {
        return this == WORKER;
    }

    /**
     * Check if this unit can construct buildings
     */
    public boolean canBuild() {
        return this == WORKER || this == ENGINEER;
    }

    /**
     * Check if this unit can heal other units
     */
    public boolean canHeal() {
        return this == MEDIC;
    }

    /**
     * Check if this unit can repair buildings/vehicles
     */
    public boolean canRepair() {
        return this == ENGINEER;
    }

    /**
     * Check if this unit can mine obstacles
     */
    public boolean canMine() {
        return this == MINER;
    }

    /**
     * Check if this is a support unit (non-combat)
     */
    public boolean isSupport() {
        return this == MEDIC || this == ENGINEER || this == MINER;
    }

    /**
     * Check if this is the Crawler (for special multi-turret logic)
     */
    public boolean isCrawler() {
        return this == CRAWLER;
    }

    /**
     * Check if this unit fires beam weapons instead of projectiles
     */
    public boolean firesBeams() {
        return this == LASER_INFANTRY ||
                this == PLASMA_TROOPER ||
                this == ION_RANGER ||
                this == PHOTON_SCOUT ||
                this == BEAM_TANK ||
                this == PULSE_ARTILLERY ||
                this == PHOTON_TITAN;
    }

    /**
     * Check if this is an infantry unit (can garrison in bunkers)
     */
    public boolean isInfantry() {
        return this == INFANTRY ||
                this == LASER_INFANTRY ||
                this == PLASMA_TROOPER ||
                this == ROCKET_SOLDIER ||
                this == SNIPER ||
                this == ION_RANGER ||
                this == MEDIC ||
                this == ENGINEER ||
                this == ANDROID;
    }
}

