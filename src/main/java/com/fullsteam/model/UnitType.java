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
            75,      // max health
            100.0,   // movement speed
            5,       // damage
            1.0,     // attack rate
            100,     // attack range
            15.0,    // size (radius)
            16,      // sides (16-sided polygon approximates circle)
            0xFFFF00, // yellow
            BuildingType.HEADQUARTERS,
            5,       // upkeep cost
            300.0,   // vision range (moderate)
            Elevation.GROUND // elevation
    ),

    // Infantry - basic combat unit
    INFANTRY(
            "Infantry",
            75,      // resource cost
            5,       // build time (seconds)
            128,     // max health
            120.0,   // movement speed
            18,      // damage (+30% vs beam infantry)
            2.0,     // attack rate
            170,     // attack range (+15% vs beam infantry)
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x00FF00, // green
            BuildingType.BARRACKS,
            10,      // upkeep cost
            350.0,   // vision range (standard infantry),
            Elevation.GROUND
    ),

    // Laser Infantry - advanced infantry with beam weapons
    LASER_INFANTRY(
            "Laser Infantry",
            125,     // resource cost (more expensive than regular infantry)
            7,       // build time (seconds)
            128,     // max health
            120.0,   // movement speed (same as infantry)
            20,      // damage (higher than infantry)
            1.5,     // attack rate (faster than infantry)
            180,     // attack range (longer than infantry)
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x00FFFF, // cyan (to distinguish from regular infantry)
            BuildingType.BARRACKS,
            12,      // upkeep cost (higher than infantry)
            360.0,   // vision range (slightly better than infantry),
            Elevation.GROUND
    ),

    // Medic - support unit that heals nearby friendlies
    MEDIC(
            "Medic",
            100,     // resource cost
            8,       // build time (seconds)
            90,      // max health
            110.0,   // movement speed
            0,       // damage (cannot attack)
            0.0,     // attack rate
            0,       // attack range
            12.0,    // size (radius)
            6,       // sides (hexagon)
            0xFFFFFF, // white
            BuildingType.BARRACKS,
            8,       // upkeep cost
            340.0,  // vision range (support unit, moderate),
            Elevation.GROUND
    ),

    // Rocket Soldier - anti-vehicle infantry
    ROCKET_SOLDIER(
            "Rocket Soldier",
            150,     // resource cost
            8,       // build time (seconds)
            112,     // max health
            110.0,   // movement speed
            40,      // damage
            0.8,     // attack rate (slower)
            200,     // attack range
            12.0,    // size (radius)
            3,       // sides (triangle)
            0xFF8800, // orange
            BuildingType.WEAPONS_DEPOT,
            15,      // upkeep cost
            370.0,    // vision range (good, needs to spot vehicles),
            Elevation.GROUND
    ),

    // Sniper - long-range precision unit
    SNIPER(
            "Sniper",
            200,     // resource cost
            10,      // build time (seconds)
            80,      // max health
            100.0,   // movement speed
            65,      // damage (+30% vs beam sniper)
            0.5,     // attack rate (slow, precise shots)
            345,     // attack range (+15% vs beam sniper)
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x8B4513, // brown
            BuildingType.WEAPONS_DEPOT,
            12,      // upkeep cost
            500.0,    // vision range (excellent, sniper needs vision),
            Elevation.GROUND
    ),

    // Engineer - repairs buildings and vehicles
    ENGINEER(
            "Engineer",
            150,     // resource cost
            12,      // build time (seconds)
            105,     // max health
            105.0,   // movement speed
            0,       // damage (cannot attack)
            0.0,     // attack rate
            0,       // attack range
            13.0,    // size (radius)
            6,       // sides (hexagon)
            0x00CED1, // dark turquoise (distinct from yellow worker)
            BuildingType.WEAPONS_DEPOT,
            10,      // upkeep cost
            330.0,    // vision range (support unit),
            Elevation.GROUND
    ),

    // Jeep - fast light vehicle
    JEEP(
            "Jeep",
            200,     // resource cost
            10,      // build time (seconds)
            168,     // max health
            180.0,   // movement speed (fast!)
            26,      // damage (+30% vs beam scout)
            3.0,     // attack rate
            207,     // attack range (+15% vs beam scout)
            20.0,    // size (radius)
            4,       // sides (rectangle)
            0x00FFFF, // cyan
            BuildingType.FACTORY,
            20,      // upkeep cost
            450.0,    // vision range (scout vehicle, excellent vision),
            Elevation.GROUND
    ),

    // Tank - heavy armored vehicle
    TANK(
            "Tank",
            400,     // resource cost
            15,      // build time (seconds)
            390,     // max health
            80.0,    // movement speed (slow)
            68,      // damage (+30% vs beam tank)
            1.2,     // attack rate
            240,     // attack range (+15% vs beam tank)
            27.0,    // size (radius)
            5,       // sides (pentagon)
            0x8888FF, // light blue
            BuildingType.FACTORY,
            30,      // upkeep cost
            400.0,    // vision range (good, main battle tank),
            Elevation.GROUND
    ),

    // Flak Tank - early-game anti-air vehicle
    FLAK_TANK(
            "Flak Tank",
            350,     // resource cost (cheaper than tank, more than jeep)
            12,      // build time (seconds)
            280,     // max health (lighter than main tank)
            90.0,    // movement speed (faster than tank, slower than jeep)
            30,      // damage (moderate direct hit damage)
            1.5,     // attack rate (decent fire rate)
            300,     // attack range (longer than tank for AA role)
            24.0,    // size (radius)
            6,       // sides (hexagon)
            0xA0A0A0, // gray (flak color)
            BuildingType.FACTORY,
            25,      // upkeep cost
            420.0,    // vision range (good, needs to spot aircraft),
            Elevation.GROUND
    ),

    // Artillery - long range siege unit
    ARTILLERY(
            "Artillery",
            500,     // resource cost
            20,      // build time (seconds)
            180,     // max health
            60.0,    // movement speed (very slow)
            117,     // damage (+30% vs beam artillery)
            0.5,     // attack rate (very slow)
            437,     // attack range (+15% vs beam artillery)
            25.0,    // size (radius)
            6,       // sides (hexagon)
            0xFF00FF, // magenta
            BuildingType.ADVANCED_FACTORY,
            40,      // upkeep cost
            420.0,    // vision range (good, needs to spot targets),
            Elevation.GROUND
    ),

    // GIGANTONAUT - Super heavy artillery
    GIGANTONAUT(
            "Gigantonaut",
            1200,    // resource cost (VERY EXPENSIVE!)
            35,      // build time (seconds) (LONG!)
            360,     // max health
            30.0,    // movement speed (SLOWEST!)
            250,     // damage (MASSIVE!)
            0.3,     // attack rate (EXTREMELY SLOW!)
            450,     // attack range (LONGEST!)
            35.0,    // size (radius) (HUGE!)
            8,       // sides (octagon)
            0x8B0000, // dark red
            BuildingType.ADVANCED_FACTORY,
            60,      // upkeep cost (VERY HIGH!)
            200.0,    // poor vision, needs a spotter to hit distant targets
            Elevation.GROUND
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
            480.0,    // vision range (excellent, mobile fortress),
            Elevation.GROUND
    ),

    // Cloak Tank - invisible until attacking or detected
    CLOAK_TANK(
            "Cloak Tank",
            800,     // resource cost
            25,      // build time (seconds)
            260,     // max health
            100.0,   // movement speed
            28,      // damage
            1.5,     // attack rate
            200,     // attack range
            28.0,    // size (radius)
            5,       // sides (pentagon)
            0x2F4F4F, // dark slate gray
            BuildingType.ADVANCED_FACTORY,
            45,      // upkeep cost
            380.0,    // vision range (moderate, cloak unit),
            Elevation.GROUND
    ),

    // ===== HERO UNITS =====

    // RAIDER - Nomads hero unit, fast hit-and-run cavalry
    RAIDER(
            "Raider",
            900,     // resource cost
            28,      // build time (seconds)
            364,     // max health
            220.0,   // movement speed (VERY FAST - fastest unit!)
            55,      // damage (high)
            2.2,     // attack rate (fast)
            180,     // attack range
            22.0,    // size (radius)
            3,       // sides (triangle - agile)
            0xDC143C, // crimson (raider red)
            BuildingType.ADVANCED_FACTORY,
            45,      // upkeep cost
            520.0,    // vision range (hero scout, exceptional vision),
            Elevation.GROUND
    ),

    // COLOSSUS - Synthesis hero unit, massive walker
    COLOSSUS(
            "Colossus",
            1600,    // resource cost (VERY EXPENSIVE!)
            45,      // build time (seconds) (VERY LONG!)
            2640,    // max health
            40.0,    // movement speed (VERY SLOW)
            95,      // damage (very high)
            0.9,     // attack rate (moderate)
            250,     // attack range
            50.0,    // size (radius) (MASSIVE!)
            6,       // sides (hexagon)
            0x4B0082, // indigo (synthesis purple)
            BuildingType.ADVANCED_FACTORY,
            75,      // upkeep cost (VERY HIGH!)
            490.0,    // vision range (hero unit, excellent vision),
            Elevation.GROUND
    ),

    // ===== TECH ALLIANCE BEAM WEAPON UNITS =====

    // PLASMA_TROOPER - Basic beam infantry (Tech Alliance equivalent of Infantry)
    PLASMA_TROOPER(
            "Plasma Trooper",
            100,     // resource cost
            6,       // build time (seconds)
            136,     // max health
            115.0,   // movement speed
            14,      // damage (instant hit beam weapon)
            2.0,     // attack rate (fast)
            148,     // attack range (instant hit beam weapon)
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x00FF7F, // spring green (plasma color)
            BuildingType.BARRACKS,
            11,      // upkeep cost
            355.0,    // vision range (standard beam infantry),
            Elevation.GROUND
    ),

    // ION_RANGER - Long-range beam sniper
    ION_RANGER(
            "Ion Ranger",
            250,     // resource cost
            12,      // build time (seconds)
            96,      // max health
            105.0,   // movement speed
            50,      // damage (instant hit beam weapon)
            0.6,     // attack rate (slow, precise)
            300,     // attack range (instant hit beam weapon)
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x9370DB, // medium purple (ion beam)
            BuildingType.WEAPONS_DEPOT,
            14,      // upkeep cost
            500.0,    // vision range (excellent, beam sniper),
            Elevation.GROUND
    ),

    // PHOTON_SCOUT - Fast beam vehicle
    PHOTON_SCOUT(
            "Photon Scout",
            220,     // resource cost
            11,      // build time (seconds)
            154,     // max health
            190.0,   // movement speed (very fast!)
            20,      // damage (instant hit beam weapon)
            2.5,     // attack rate (rapid fire)
            180,     // attack range (instant hit beam weapon)
            18.0,    // size (radius)
            4,       // sides (rectangle)
            0x7FFF00, // chartreuse (bright energy)
            BuildingType.FACTORY,
            22,      // upkeep cost
            460.0,    // vision range (excellent, scout vehicle),
            Elevation.GROUND
    ),

    // BEAM_TANK - Heavy beam vehicle
    BEAM_TANK(
            "Beam Tank",
            450,     // resource cost
            16,      // build time (seconds)
            416,     // max health
            75.0,    // movement speed (slow)
            52,      // damage (instant hit beam weapon)
            1.3,     // attack rate
            209,     // attack range (instant hit beam weapon)
            30.0,    // size (radius)
            6,       // sides (hexagon)
            0x00FA9A, // medium spring green
            BuildingType.FACTORY,
            32,      // upkeep cost
            410.0,    // vision range (good, beam tank),
            Elevation.GROUND
    ),

    // PULSE_ARTILLERY - Long-range beam artillery
    PULSE_ARTILLERY(
            "Pulse Artillery",
            550,     // resource cost
            22,      // build time (seconds)
            168,     // max health
            55.0,    // movement speed (very slow)
            90,      // damage (instant hit beam weapon)
            0.6,     // attack rate (slow)
            380,     // attack range (instant hit beam weapon)
            26.0,    // size (radius)
            6,       // sides (hexagon)
            0xFFD700, // gold (energy pulse)
            BuildingType.ADVANCED_FACTORY,
            42,      // upkeep cost
            430.0,    // vision range (good, beam artillery),
            Elevation.GROUND
    ),

    // PHOTON_TITAN - Hero unit, massive beam platform
    PHOTON_TITAN(
            "Photon Titan",
            1400,    // resource cost (VERY EXPENSIVE!)
            40,      // build time (seconds) (LONG!)
            420,     // max health
            35.0,    // movement speed (VERY SLOW!)
            280,     // damage (MASSIVE!)
            0.4,     // attack rate (slow but devastating)
            460,     // attack range (LONGEST!)
            32.0,    // size (radius) (HUGE!)
            8,       // sides (octagon)
            0x00FF00, // bright green (pure energy)
            BuildingType.ADVANCED_FACTORY,
            65,      // upkeep cost (VERY HIGH!)
            480.0,    // vision range (hero unit, excellent vision),
            Elevation.GROUND
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
            340.0,    // vision range (moderate, autonomous unit),
            Elevation.GROUND
    ),

    // ===== AIR UNITS =====

    // SCOUT_DRONE - Fast reconnaissance VTOL drone
    // First air unit, cheap and fast, excellent vision
    SCOUT_DRONE(
            "Scout Drone",
            150,     // resource cost (cheap for air unit)
            12,      // build time (seconds)
            80,      // max health (fragile)
            200.0,   // movement speed (VERY FAST)
            8,       // damage (light weapons)
            2.5,     // attack rate (rapid fire)
            150,     // attack range (moderate)
            12.0,    // size (radius) - small
            4,       // sides (square/diamond shape)
            0x87CEEB, // light sky blue (air unit color)
            BuildingType.AIRFIELD,
            15,      // upkeep cost
            600.0,   // vision range (EXCELLENT - scout unit!)
            Elevation.LOW // VTOL - can hover, vulnerable to rockets
    ),

    // HELICOPTER - Attack helicopter with dual rockets
    // Low-altitude VTOL gunship, controllable like standard units
    // Fires dual rockets, slower than scout drone but more powerful
    HELICOPTER(
            "Attack Helicopter",
            350,     // resource cost (moderate)
            18,      // build time (seconds)
            150,     // max health (fragile but more durable than scout)
            150.0,   // movement speed (slower than scout, faster than tanks)
            35,      // damage per rocket (dual rockets = 70 total per volley)
            1.8,     // attack rate (decent fire rate)
            220,     // attack range (good range for air-to-ground)
            16.0,    // size (radius) - medium aircraft
            5,       // sides (pentagon shape)
            0x8B4513, // saddle brown (military helicopter color)
            BuildingType.AIRFIELD,
            25,      // upkeep cost (moderate)
            450.0,   // vision range (good, attack aircraft)
            Elevation.LOW // VTOL - can hover, vulnerable to rockets
    ),

    // BOMBER - Sortie-based heavy bomber aircraft
    // Housed in Hangar, executes bombing runs on command, then returns to base
    // NOT controllable like regular units - sortie-based only
    BOMBER(
            "Bomber",
            800,     // resource cost (EXPENSIVE - strategic asset)
            60,      // build time (seconds)
            250,     // max health (more durable than scout)
            220.0,   // movement speed (slower than scout drone)
            200,     // damage (MASSIVE - area effect bombs)
            0.5,     // attack rate (slow - payload limitation)
            0,       // attack range (N/A - bombs are dropped, not fired)
            18.0,    // size (radius) - larger aircraft
            6,       // sides (hexagonal fuselage)
            0x2F4F4F, // dark slate gray (bomber color)
            BuildingType.HANGAR, // Housed in hangar, not produced at airfield
            50,      // upkeep cost (HIGH - strategic bomber)
            400.0,   // vision range (good but not scout-level)
            Elevation.HIGH // Fixed-wing - requires AA weapons
    ),

    // INTERCEPTOR - Sortie-based fighter aircraft
    // Housed in Hangar, auto-deploys to intercept enemy aircraft (SCRAMBLE)
    // Can be sent to patrol areas (ON_STATION), limited fuel and ammo
    INTERCEPTOR(
            "Interceptor",
            600,     // resource cost (expensive)
            45,      // build time (seconds)
            200,     // max health (moderate durability)
            290.0,   // movement speed (FASTEST air unit)
            100,      // damage per seeking rocket
            2.0,     // attack rate (fast for air-to-air)
            300,     // attack range (long-range seeking missiles)
            14.0,    // size (radius) - sleek fighter
            3,       // sides (triangle - delta wing)
            0xFF4500, // orange-red (fighter jet color)
            BuildingType.HANGAR, // Housed in hangar
            40,      // upkeep cost (high)
            500.0,   // vision range (excellent, interceptor)
            Elevation.HIGH // Fixed-wing - high-altitude fighter
    ),

    // GUNSHIP - Heavy sortie-based attack aircraft with dual weapons
    // Storm Wings hero unit - can engage both ground and air targets
    // Heavy MG for ground targets, flak cannons for air targets
    GUNSHIP(
            "Gunship",
            1100,    // resource cost (expensive heavy aircraft, hero unit)
            50,      // build time (seconds)
            380,     // max health (durable for sustained combat)
            160.0,   // slowest sortie air unit
            40,      // damage (primary weapon - heavy MG)
            2.0,     // attack rate (decent fire rate)
            280,     // attack range (good engagement range)
            50.0,    // size (radius) - medium heavy aircraft
            6,       // sides (hexagon - gunship)
            0x8B0000, // dark red (intimidating gunship color)
            BuildingType.HANGAR, // Produced at Hangar (sortie-based)
            55,      // upkeep cost (high, hero unit)
            480.0,   // vision range (excellent, attack helicopter)
            Elevation.HIGH // Fixed-wing sortie aircraft
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
     * Get vision range for this unit type
     * Most units: 1.5x attack range
     * Gigantonaut: terrible vision (0.5x attack range)
     */
    private final double visionRange; // vision radius for fog of war

    /**
     * -- GETTER --
     * Get elevation level for this unit type.
     * Determines which weapons can target this unit.
     * GROUND = standard units, LOW = VTOLs, HIGH = fixed-wing aircraft
     */
    private final Elevation elevation;

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
            case INFANTRY -> List.of(Geometry.createPolygonalCircle(3, size));

            // Laser Infantry - angular prism design with crystalline focusing arrays
            case LASER_INFANTRY -> {
                // Create a multi-faceted prism shape with focusing crystals
                // Main body: elongated diamond (prism core)
                Vector2[] mainPrism = new Vector2[]{
                        new Vector2(-size * 0.7, -size * 0.3),  // Back left
                        new Vector2(size * 0.9, -size * 0.4),   // Front left (angular)
                        new Vector2(size * 1.1, 0),             // Front point (beam emitter)
                        new Vector2(size * 0.9, size * 0.4),    // Front right (angular)
                        new Vector2(-size * 0.7, size * 0.3)    // Back right
                };
                Convex core = Geometry.createPolygon(mainPrism);

                // Left focusing crystal (small triangle)
                Vector2[] leftCrystal = new Vector2[]{
                        new Vector2(size * 0.2, -size * 0.5),
                        new Vector2(size * 0.6, -size * 0.6),
                        new Vector2(size * 0.5, -size * 0.3)
                };
                Convex leftFocus = Geometry.createPolygon(leftCrystal);

                // Right focusing crystal (small triangle)
                Vector2[] rightCrystal = new Vector2[]{
                        new Vector2(size * 0.2, size * 0.5),
                        new Vector2(size * 0.5, size * 0.3),
                        new Vector2(size * 0.6, size * 0.6)
                };
                Convex rightFocus = Geometry.createPolygon(rightCrystal);

                yield List.of(core, leftFocus, rightFocus);
            }

            // Plasma Trooper - energy prism with plasma containment geometry
            case PLASMA_TROOPER -> {
                // Main plasma containment chamber (hexagonal prism)
                Vector2[] chamber = new Vector2[]{
                        new Vector2(-size * 0.7, 0),
                        new Vector2(-size * 0.3, -size * 0.5),
                        new Vector2(size * 0.5, -size * 0.5),
                        new Vector2(size, 0),             // Front emitter
                        new Vector2(size * 0.5, size * 0.5),
                        new Vector2(-size * 0.3, size * 0.5)
                };
                Convex mainChamber = Geometry.createPolygon(chamber);

                // Plasma accelerator spikes (fractal-like angular protrusions)
                Vector2[] topSpike = new Vector2[]{
                        new Vector2(size * 0.1, -size * 0.6),
                        new Vector2(size * 0.3, -size * 0.8),
                        new Vector2(size * 0.5, -size * 0.6)
                };
                Convex topAccel = Geometry.createPolygon(topSpike);

                Vector2[] bottomSpike = new Vector2[]{
                        new Vector2(size * 0.1, size * 0.6),
                        new Vector2(size * 0.5, size * 0.6),
                        new Vector2(size * 0.3, size * 0.8)
                };
                Convex bottomAccel = Geometry.createPolygon(bottomSpike);

                yield List.of(mainChamber, topAccel, bottomAccel);
            }

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

            // Ion Ranger - complex multi-lens focusing array for long-range ion beam
            case ION_RANGER -> {
                // Main body: elongated focusing chamber
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.4),
                        new Vector2(size * 0.4, -size * 0.5),
                        new Vector2(size * 1.3, 0),             // Primary lens tip
                        new Vector2(size * 0.4, size * 0.5),
                        new Vector2(-size * 0.9, size * 0.4)
                };
                Convex focusingBody = Geometry.createPolygon(mainBody);

                // Primary focusing lens (forward angular prism)
                Vector2[] primaryLens = new Vector2[]{
                        new Vector2(size * 0.7, -size * 0.3),
                        new Vector2(size * 1.1, -size * 0.2),
                        new Vector2(size * 1.2, 0),
                        new Vector2(size * 1.1, size * 0.2),
                        new Vector2(size * 0.7, size * 0.3)
                };
                Convex lens1 = Geometry.createPolygon(primaryLens);

                // Secondary refractor (top)
                Vector2[] topRefractor = new Vector2[]{
                        new Vector2(size * 0.0, -size * 0.6),
                        new Vector2(size * 0.4, -size * 0.75),
                        new Vector2(size * 0.6, -size * 0.55)
                };
                Convex refractorTop = Geometry.createPolygon(topRefractor);

                // Secondary refractor (bottom)
                Vector2[] bottomRefractor = new Vector2[]{
                        new Vector2(size * 0.0, size * 0.6),
                        new Vector2(size * 0.6, size * 0.55),
                        new Vector2(size * 0.4, size * 0.75)
                };
                Convex refractorBottom = Geometry.createPolygon(bottomRefractor);

                // Tertiary focusing crystals (fractal detail)
                Vector2[] leftCrystal = new Vector2[]{
                        new Vector2(-size * 0.2, -size * 0.5),
                        new Vector2(size * 0.1, -size * 0.65),
                        new Vector2(size * 0.2, -size * 0.5)
                };
                Convex crystalLeft = Geometry.createPolygon(leftCrystal);

                Vector2[] rightCrystal = new Vector2[]{
                        new Vector2(-size * 0.2, size * 0.5),
                        new Vector2(size * 0.2, size * 0.5),
                        new Vector2(size * 0.1, size * 0.65)
                };
                Convex crystalRight = Geometry.createPolygon(rightCrystal);

                yield List.of(focusingBody, lens1, refractorTop, refractorBottom, crystalLeft, crystalRight);
            }

            // Worker/Support units - circular for easy navigation
            case WORKER, MEDIC, ENGINEER -> List.of(Geometry.createCircle(size));

            // Android - diamond/square shape (synthetic unit)
            case ANDROID -> List.of(Geometry.createPolygonalCircle(4, size));

            // Scout Drone - X-shaped quadcopter with 4 rotors
            case SCOUT_DRONE -> {
                // Central hub (small diamond)
                Convex hub = Geometry.createPolygonalCircle(4, size * 0.4);

                // Four rotor arms extending from center
                // Front-left rotor arm
                Vector2[] armFL = new Vector2[]{
                        new Vector2(-size * 0.2, size * 0.2),
                        new Vector2(-size * 0.7, size * 0.7),
                        new Vector2(-size * 0.85, size * 0.55),
                        new Vector2(-size * 0.35, size * 0.05)
                };
                Convex rotorArmFL = Geometry.createPolygon(armFL);

                // Front-right rotor arm
                Vector2[] armFR = new Vector2[]{
                        new Vector2(size * 0.2, size * 0.2),
                        new Vector2(size * 0.35, size * 0.05),
                        new Vector2(size * 0.85, size * 0.55),
                        new Vector2(size * 0.7, size * 0.7)
                };
                Convex rotorArmFR = Geometry.createPolygon(armFR);

                // Rear-left rotor arm
                Vector2[] armRL = new Vector2[]{
                        new Vector2(-size * 0.2, -size * 0.2),
                        new Vector2(-size * 0.35, -size * 0.05),
                        new Vector2(-size * 0.85, -size * 0.55),
                        new Vector2(-size * 0.7, -size * 0.7)
                };
                Convex rotorArmRL = Geometry.createPolygon(armRL);

                // Rear-right rotor arm
                Vector2[] armRR = new Vector2[]{
                        new Vector2(size * 0.2, -size * 0.2),
                        new Vector2(size * 0.7, -size * 0.7),
                        new Vector2(size * 0.85, -size * 0.55),
                        new Vector2(size * 0.35, -size * 0.05)
                };
                Convex rotorArmRR = Geometry.createPolygon(armRR);

                yield List.of(hub, rotorArmFL, rotorArmFR, rotorArmRL, rotorArmRR);
            }

            // Bomber - Heavy aircraft with fuselage and wings
            case BOMBER -> {
                // Main fuselage (elongated hexagon - aircraft body)
                Vector2[] fuselage = new Vector2[]{
                        new Vector2(size * 0.9, 0),                    // Nose (front)
                        new Vector2(size * 0.3, size * 0.35),          // Top-front
                        new Vector2(-size * 0.7, size * 0.35),         // Top-rear
                        new Vector2(-size * 0.9, 0),                   // Tail (rear)
                        new Vector2(-size * 0.7, -size * 0.35),        // Bottom-rear
                        new Vector2(size * 0.3, -size * 0.35)          // Bottom-front
                };
                Convex body = Geometry.createPolygon(fuselage);

                // Left wing (swept back)
                Vector2[] leftWing = new Vector2[]{
                        new Vector2(-size * 0.2, size * 0.4),          // Inner front
                        new Vector2(-size * 0.6, size * 0.95),         // Outer tip
                        new Vector2(-size * 0.75, size * 0.85),        // Outer rear
                        new Vector2(-size * 0.35, size * 0.3)          // Inner rear
                };
                Convex wingLeft = Geometry.createPolygon(leftWing);

                // Right wing (swept back, mirrored)
                Vector2[] rightWing = new Vector2[]{
                        new Vector2(-size * 0.2, -size * 0.4),         // Inner front
                        new Vector2(-size * 0.35, -size * 0.3),        // Inner rear
                        new Vector2(-size * 0.75, -size * 0.85),       // Outer rear
                        new Vector2(-size * 0.6, -size * 0.95)         // Outer tip
                };
                Convex wingRight = Geometry.createPolygon(rightWing);

                yield List.of(body, wingLeft, wingRight);
            }

            // Helicopter - Attack helicopter with main fuselage, tail boom, and rotor
            case HELICOPTER -> {
                // Main fuselage (bulbous cockpit/body)
                Vector2[] fuselage = new Vector2[]{
                        new Vector2(size * 0.7, 0),                    // Nose (front)
                        new Vector2(size * 0.4, size * 0.4),           // Top-front
                        new Vector2(-size * 0.2, size * 0.45),         // Top-mid
                        new Vector2(-size * 0.5, size * 0.25),         // Top-rear
                        new Vector2(-size * 0.5, -size * 0.25),        // Bottom-rear
                        new Vector2(-size * 0.2, -size * 0.45),        // Bottom-mid
                        new Vector2(size * 0.4, -size * 0.4)           // Bottom-front
                };
                Convex body = Geometry.createPolygon(fuselage);

                // Tail boom (thin elongated section extending back)
                Vector2[] tailBoom = new Vector2[]{
                        new Vector2(-size * 0.4, size * 0.15),
                        new Vector2(-size * 0.95, size * 0.12),
                        new Vector2(-size * 0.95, -size * 0.12),
                        new Vector2(-size * 0.4, -size * 0.15)
                };
                Convex tail = Geometry.createPolygon(tailBoom);

                // Left landing skid
                Vector2[] leftSkid = new Vector2[]{
                        new Vector2(size * 0.3, size * 0.5),
                        new Vector2(-size * 0.3, size * 0.6),
                        new Vector2(-size * 0.35, size * 0.5),
                        new Vector2(size * 0.25, size * 0.4)
                };
                Convex skidLeft = Geometry.createPolygon(leftSkid);

                // Right landing skid
                Vector2[] rightSkid = new Vector2[]{
                        new Vector2(size * 0.3, -size * 0.5),
                        new Vector2(size * 0.25, -size * 0.4),
                        new Vector2(-size * 0.35, -size * 0.5),
                        new Vector2(-size * 0.3, -size * 0.6)
                };
                Convex skidRight = Geometry.createPolygon(rightSkid);

                yield List.of(body, tail, skidLeft, skidRight);
            }

            // Interceptor - Sleek delta-wing fighter jet
            case INTERCEPTOR -> {
                // Main fuselage (streamlined triangle body)
                Vector2[] fuselage = new Vector2[]{
                        new Vector2(size * 0.95, 0),                   // Nose (front, sharp point)
                        new Vector2(size * 0.2, size * 0.25),          // Top-mid
                        new Vector2(-size * 0.8, size * 0.2),          // Top-rear
                        new Vector2(-size * 0.95, 0),                  // Tail (rear center)
                        new Vector2(-size * 0.8, -size * 0.2),         // Bottom-rear
                        new Vector2(size * 0.2, -size * 0.25)          // Bottom-mid
                };
                Convex body = Geometry.createPolygon(fuselage);

                // Left delta wing (swept-back triangular)
                Vector2[] leftWing = new Vector2[]{
                        new Vector2(size * 0.3, size * 0.3),           // Inner front
                        new Vector2(-size * 0.4, size * 0.95),         // Outer tip
                        new Vector2(-size * 0.7, size * 0.75),         // Outer rear
                        new Vector2(-size * 0.3, size * 0.25)          // Inner rear
                };
                Convex wingLeft = Geometry.createPolygon(leftWing);

                // Right delta wing (swept-back triangular, mirrored)
                Vector2[] rightWing = new Vector2[]{
                        new Vector2(size * 0.3, -size * 0.3),          // Inner front
                        new Vector2(-size * 0.3, -size * 0.25),        // Inner rear
                        new Vector2(-size * 0.7, -size * 0.75),        // Outer rear
                        new Vector2(-size * 0.4, -size * 0.95)         // Outer tip
                };
                Convex wingRight = Geometry.createPolygon(rightWing);

                // Tail fins (vertical stabilizers)
                Vector2[] tailFin = new Vector2[]{
                        new Vector2(-size * 0.6, 0),
                        new Vector2(-size * 0.85, size * 0.15),
                        new Vector2(-size * 0.95, 0),
                        new Vector2(-size * 0.85, -size * 0.15)
                };
                Convex fin = Geometry.createPolygon(tailFin);

                yield List.of(body, wingLeft, wingRight, fin);
            }

            // Gunship - Heavy attack aircraft with prominent swept wings
            case GUNSHIP -> {
                // Main fuselage (heavy gunship body) - hexagon, counter-clockwise
                Vector2[] fuselage = new Vector2[]{
                        new Vector2(size * 0.85, 0),                   // 1. Nose (rightmost)
                        new Vector2(size * 0.5, size * 0.35),          // 2. Top-front (narrower body)
                        new Vector2(-size * 0.6, size * 0.35),         // 3. Top-rear
                        new Vector2(-size * 0.95, 0),                  // 4. Tail (leftmost)
                        new Vector2(-size * 0.6, -size * 0.35),        // 5. Bottom-rear
                        new Vector2(size * 0.5, -size * 0.35)          // 6. Bottom-front
                };
                Convex body = Geometry.createPolygon(fuselage);

                // Left weapon pod (top side) - quad, counter-clockwise
                // Visualize: This is ABOVE the fuselage (positive Y)
                // Start from bottom-left (closest to fuselage), go counter-clockwise
                Vector2[] leftPod = new Vector2[]{
                        new Vector2(size * 0.35, size * 0.3),          // 1. Inner-front (bottom-left)
                        new Vector2(size * 0.75, size * 0.4),          // 2. Outer-front (bottom-right)
                        new Vector2(size * 0.7, size * 0.55),          // 3. Outer-rear (top-right)
                        new Vector2(size * 0.3, size * 0.45)           // 4. Inner-rear (top-left)
                };
                Convex podLeft = Geometry.createPolygon(leftPod);

                // Right weapon pod (bottom side) - quad, counter-clockwise
                // Visualize: This is BELOW the fuselage (negative Y)
                // Start from top-left (closest to fuselage), go counter-clockwise
                Vector2[] rightPod = new Vector2[]{
                        new Vector2(size * 0.3, -size * 0.45),         // 1. Inner-rear (top-left)
                        new Vector2(size * 0.7, -size * 0.55),         // 2. Outer-rear (top-right)
                        new Vector2(size * 0.75, -size * 0.4),         // 3. Outer-front (bottom-right)
                        new Vector2(size * 0.35, -size * 0.3)          // 4. Inner-front (bottom-left)
                };
                Convex podRight = Geometry.createPolygon(rightPod);

                // Tail stabilizer - triangle, counter-clockwise
                // Start from rightmost point, go counter-clockwise (up then down)
                Vector2[] tail = new Vector2[]{
                        new Vector2(-size * 0.85, 0),                  // 1. Front point (rightmost)
                        new Vector2(-size * 1.05, size * 0.25),        // 2. Top point (larger)
                        new Vector2(-size * 1.05, -size * 0.25)        // 3. Bottom point
                };
                Convex stabilizer = Geometry.createPolygon(tail);

                // Left main wing (top side) - LARGE swept wing, counter-clockwise
                // Visualize: This is ABOVE the fuselage, dramatic swept-back design
                // Start from inner-front (closest to fuselage), go counter-clockwise
                Vector2[] wing = new Vector2[]{
                        new Vector2(-size * 0.1, size * 1.1),          // 1. top right
                        new Vector2(-size * 0.4, size * 1.1),          // 2. top left
                        new Vector2(-size * 0.4, -size * 1.1),          // 4. bottom left
                        new Vector2(-size * 0.1, -size * 1.1),         // 5. bottom right
                        new Vector2(size * 0.4, 0)           // 6. mid-point on fuselage
                };
                Convex wingPoly = Geometry.createPolygon(wing);

                yield List.of(wingPoly, body, podLeft, podRight, stabilizer);
            }

            // Jeep - fast light vehicle with angular chassis and armor plating
            case JEEP -> {
                // Main chassis: elongated hexagon (streamlined body)
                Vector2[] chassis = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.4),
                        new Vector2(size * 0.3, -size * 0.6),
                        new Vector2(size * 1.1, 0),
                        new Vector2(size * 0.3, size * 0.6),
                        new Vector2(-size * 0.9, size * 0.4)
                };
                Convex mainBody = Geometry.createPolygon(chassis);

                // Top armor plate (small angular detail)
                Vector2[] topPlate = new Vector2[]{
                        new Vector2(-size * 0.2, -size * 0.65),
                        new Vector2(size * 0.4, -size * 0.75),
                        new Vector2(size * 0.6, -size * 0.55)
                };
                Convex plateTop = Geometry.createPolygon(topPlate);

                // Bottom armor plate (small angular detail)
                Vector2[] bottomPlate = new Vector2[]{
                        new Vector2(-size * 0.2, size * 0.65),
                        new Vector2(size * 0.6, size * 0.55),
                        new Vector2(size * 0.4, size * 0.75)
                };
                Convex plateBottom = Geometry.createPolygon(bottomPlate);

                yield List.of(mainBody, plateTop, plateBottom);
            }

            // Photon Scout - angular reflector array vehicle with prismatic beam deflectors
            case PHOTON_SCOUT -> {
                // Main chassis: elongated hexagon
                Vector2[] chassis = new Vector2[]{
                        new Vector2(-size, -size * 0.4),
                        new Vector2(size * 0.5, -size * 0.6),
                        new Vector2(size * 1.2, 0),
                        new Vector2(size * 0.5, size * 0.6),
                        new Vector2(-size, size * 0.4)
                };
                Convex mainChassis = Geometry.createPolygon(chassis);

                // Front reflector array (angular prism)
                Vector2[] frontReflector = new Vector2[]{
                        new Vector2(size * 0.8, -size * 0.4),
                        new Vector2(size * 1.4, -size * 0.3),
                        new Vector2(size * 1.5, 0),
                        new Vector2(size * 1.4, size * 0.3),
                        new Vector2(size * 0.8, size * 0.4)
                };
                Convex frontArray = Geometry.createPolygon(frontReflector);

                // Top beam deflector (fractal wing)
                Vector2[] topDeflector = new Vector2[]{
                        new Vector2(size * 0.2, -size * 0.7),
                        new Vector2(size * 0.8, -size * 0.85),
                        new Vector2(size * 0.9, -size * 0.6)
                };
                Convex deflectorTop = Geometry.createPolygon(topDeflector);

                // Bottom beam deflector (fractal wing)
                Vector2[] bottomDeflector = new Vector2[]{
                        new Vector2(size * 0.2, size * 0.7),
                        new Vector2(size * 0.9, size * 0.6),
                        new Vector2(size * 0.8, size * 0.85)
                };
                Convex deflectorBottom = Geometry.createPolygon(bottomDeflector);

                // Rear energy collector (small angular piece)
                Vector2[] rearCollector = new Vector2[]{
                        new Vector2(-size * 1.1, -size * 0.25),
                        new Vector2(-size * 0.7, -size * 0.3),
                        new Vector2(-size * 0.7, size * 0.3),
                        new Vector2(-size * 1.1, size * 0.25)
                };
                Convex collector = Geometry.createPolygon(rearCollector);

                yield List.of(mainChassis, frontArray, deflectorTop, deflectorBottom, collector);
            }

            // Flak Tank - anti-aircraft vehicle with flak cannon and stabilizers
            case FLAK_TANK -> {
                // Main hull: hexagonal platform
                Vector2[] hull = new Vector2[]{
                        new Vector2(-size * 0.85, -size * 0.35),
                        new Vector2(-size * 0.35, -size * 0.75),
                        new Vector2(size * 0.35, -size * 0.75),
                        new Vector2(size * 0.85, -size * 0.35),
                        new Vector2(size * 0.85, size * 0.35),
                        new Vector2(size * 0.35, size * 0.75),
                        new Vector2(-size * 0.35, size * 0.75),
                        new Vector2(-size * 0.85, size * 0.35)
                };
                Convex mainHull = Geometry.createPolygon(hull);

                // Flak cannon mount (elevated turret platform)
                Vector2[] cannon = new Vector2[]{
                        new Vector2(-size * 0.2, -size * 0.5),
                        new Vector2(size * 0.4, -size * 0.55),
                        new Vector2(size * 0.7, -size * 0.25),
                        new Vector2(size * 0.7, size * 0.25),
                        new Vector2(size * 0.4, size * 0.55),
                        new Vector2(-size * 0.2, size * 0.5)
                };
                Convex cannonMount = Geometry.createPolygon(cannon);

                // Left stabilizer (for recoil absorption)
                Vector2[] leftStab = new Vector2[]{
                        new Vector2(-size * 0.4, -size * 0.8),
                        new Vector2(size * 0.0, -size * 0.9),
                        new Vector2(size * 0.2, -size * 0.75)
                };
                Convex stabLeft = Geometry.createPolygon(leftStab);

                // Right stabilizer (for recoil absorption)
                Vector2[] rightStab = new Vector2[]{
                        new Vector2(-size * 0.4, size * 0.8),
                        new Vector2(size * 0.2, size * 0.75),
                        new Vector2(size * 0.0, size * 0.9)
                };
                Convex stabRight = Geometry.createPolygon(rightStab);

                yield List.of(mainHull, cannonMount, stabLeft, stabRight);
            }

            // Tank - main battle tank with turret platform and armor plating
            case TANK -> {
                // Main hull: octagonal platform
                Vector2[] hull = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.4),
                        new Vector2(-size * 0.4, -size * 0.8),
                        new Vector2(size * 0.4, -size * 0.8),
                        new Vector2(size * 0.9, -size * 0.4),
                        new Vector2(size * 0.9, size * 0.4),
                        new Vector2(size * 0.4, size * 0.8),
                        new Vector2(-size * 0.4, size * 0.8),
                        new Vector2(-size * 0.9, size * 0.4)
                };
                Convex mainHull = Geometry.createPolygon(hull);

                // Turret base (hexagon on top of hull)
                Vector2[] turretBase = new Vector2[]{
                        new Vector2(-size * 0.3, -size * 0.5),
                        new Vector2(size * 0.2, -size * 0.6),
                        new Vector2(size * 0.6, -size * 0.3),
                        new Vector2(size * 0.6, size * 0.3),
                        new Vector2(size * 0.2, size * 0.6),
                        new Vector2(-size * 0.3, size * 0.5)
                };
                Convex turret = Geometry.createPolygon(turretBase);

                // Side armor plates (left)
                Vector2[] leftArmor = new Vector2[]{
                        new Vector2(-size * 0.5, -size * 0.85),
                        new Vector2(size * 0.1, -size * 0.95),
                        new Vector2(size * 0.3, -size * 0.8)
                };
                Convex armorLeft = Geometry.createPolygon(leftArmor);

                // Side armor plates (right)
                Vector2[] rightArmor = new Vector2[]{
                        new Vector2(-size * 0.5, size * 0.85),
                        new Vector2(size * 0.3, size * 0.8),
                        new Vector2(size * 0.1, size * 0.95)
                };
                Convex armorRight = Geometry.createPolygon(rightArmor);

                yield List.of(mainHull, turret, armorLeft, armorRight);
            }

            // Beam Tank - multi-faceted prism platform with crystalline beam array
            case BEAM_TANK -> {
                // Main platform: wide octagonal base
                Vector2[] platform = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.5),
                        new Vector2(-size * 0.3, -size * 0.9),
                        new Vector2(size * 0.3, -size * 0.9),
                        new Vector2(size * 0.9, -size * 0.5),
                        new Vector2(size, 0),
                        new Vector2(size * 0.9, size * 0.5),
                        new Vector2(size * 0.3, size * 0.9),
                        new Vector2(-size * 0.3, size * 0.9),
                        new Vector2(-size * 0.9, size * 0.5)
                };
                Convex mainPlatform = Geometry.createPolygon(platform);

                // Primary focusing prism (center front)
                Vector2[] primaryPrism = new Vector2[]{
                        new Vector2(size * 0.4, -size * 0.3),
                        new Vector2(size * 1.1, -size * 0.2),
                        new Vector2(size * 1.2, 0),
                        new Vector2(size * 1.1, size * 0.2),
                        new Vector2(size * 0.4, size * 0.3)
                };
                Convex centerPrism = Geometry.createPolygon(primaryPrism);

                // Upper beam array (fractal antenna)
                Vector2[] upperArray = new Vector2[]{
                        new Vector2(-size * 0.1, -size),
                        new Vector2(size * 0.3, -size * 1.15),
                        new Vector2(size * 0.6, -size),
                        new Vector2(size * 0.4, -size * 0.8)
                };
                Convex topArray = Geometry.createPolygon(upperArray);

                // Lower beam array (fractal antenna)
                Vector2[] lowerArray = new Vector2[]{
                        new Vector2(-size * 0.1, size),
                        new Vector2(size * 0.4, size * 0.8),
                        new Vector2(size * 0.6, size),
                        new Vector2(size * 0.3, size * 1.15)
                };
                Convex bottomArray = Geometry.createPolygon(lowerArray);

                // Left energy lens
                Vector2[] leftLens = new Vector2[]{
                        new Vector2(size * 0.0, -size * 0.7),
                        new Vector2(size * 0.3, -size * 0.85),
                        new Vector2(size * 0.5, -size * 0.7),
                        new Vector2(size * 0.3, -size * 0.55)
                };
                Convex lensLeft = Geometry.createPolygon(leftLens);

                // Right energy lens
                Vector2[] rightLens = new Vector2[]{
                        new Vector2(size * 0.0, size * 0.7),
                        new Vector2(size * 0.3, size * 0.55),
                        new Vector2(size * 0.5, size * 0.7),
                        new Vector2(size * 0.3, size * 0.85)
                };
                Convex lensRight = Geometry.createPolygon(rightLens);

                // Rear power crystal (angular)
                Vector2[] powerCrystal = new Vector2[]{
                        new Vector2(-size, -size * 0.3),
                        new Vector2(-size * 0.6, -size * 0.4),
                        new Vector2(-size * 0.6, size * 0.4),
                        new Vector2(-size, size * 0.3)
                };
                Convex crystal = Geometry.createPolygon(powerCrystal);

                yield List.of(mainPlatform, centerPrism, topArray, bottomArray, lensLeft, lensRight, crystal);
            }

            // Cloak Tank - sleek diamond (low profile, streamlined)
            case CLOAK_TANK -> {
                // Pointing right (positive X direction)
                Vector2[] vertices = new Vector2[]{
                        new Vector2(-size, 0),          // Back point
                        new Vector2(0, -size * 0.7),          // Left point (narrow)
                        new Vector2(size, 0),           // Front point (sleek, pointing right)
                        new Vector2(0, size * 0.7)            // Right point (narrow)
                };
                yield List.of(Geometry.createPolygon(vertices));
            }

            // Artillery - long-range siege weapon with stabilizer outriggers
            case ARTILLERY -> {
                // Main body: elongated hexagon (gun platform)
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.5),
                        new Vector2(size * 0.2, -size * 0.7),
                        new Vector2(size * 0.8, -size * 0.4),
                        new Vector2(size * 0.8, size * 0.4),
                        new Vector2(size * 0.2, size * 0.7),
                        new Vector2(-size * 0.9, size * 0.5)
                };
                Convex platform = Geometry.createPolygon(mainBody);

                // Left stabilizer outrigger (for recoil stability)
                Vector2[] leftStabilizer = new Vector2[]{
                        new Vector2(-size * 0.4, -size * 0.75),
                        new Vector2(size * 0.0, -size * 0.95),
                        new Vector2(size * 0.3, -size * 0.8),
                        new Vector2(size * 0.1, -size * 0.65)
                };
                Convex stabLeft = Geometry.createPolygon(leftStabilizer);

                // Right stabilizer outrigger (for recoil stability)
                Vector2[] rightStabilizer = new Vector2[]{
                        new Vector2(-size * 0.4, size * 0.75),
                        new Vector2(size * 0.1, size * 0.65),
                        new Vector2(size * 0.3, size * 0.8),
                        new Vector2(size * 0.0, size * 0.95)
                };
                Convex stabRight = Geometry.createPolygon(rightStabilizer);

                // Rear counterweight (balance for long barrel)
                Vector2[] counterweight = new Vector2[]{
                        new Vector2(-size, -size * 0.3),
                        new Vector2(-size * 0.6, -size * 0.4),
                        new Vector2(-size * 0.6, size * 0.4),
                        new Vector2(-size, size * 0.3)
                };
                Convex weight = Geometry.createPolygon(counterweight);

                yield List.of(platform, stabLeft, stabRight, weight);
            }

            // Pulse Artillery - complex fractal lens array with multi-stage beam amplification
            case PULSE_ARTILLERY -> {
                // Main body: large angular platform (convex octagon)
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 1.1, 0),
                        new Vector2(-size * 0.6, -size * 0.7),
                        new Vector2(size * 0.3, -size * 0.8),
                        new Vector2(size, -size * 0.4),
                        new Vector2(size * 1.2, 0),
                        new Vector2(size, size * 0.4),
                        new Vector2(size * 0.3, size * 0.8),
                        new Vector2(-size * 0.6, size * 0.7)
                };
                Convex artilleryBody = Geometry.createPolygon(mainBody);

                // Primary lens chamber (forward focusing array - simplified convex)
                Vector2[] primaryChamber = new Vector2[]{
                        new Vector2(size * 0.7, -size * 0.4),
                        new Vector2(size * 1.3, -size * 0.3),
                        new Vector2(size * 1.4, 0),
                        new Vector2(size * 1.3, size * 0.3),
                        new Vector2(size * 0.7, size * 0.4)
                };
                Convex primaryLens = Geometry.createPolygon(primaryChamber);

                // Upper amplifier array (convex trapezoid)
                Vector2[] topAmplifier = new Vector2[]{
                        new Vector2(size * 0.0, -size * 0.85),
                        new Vector2(size * 0.4, -size),
                        new Vector2(size * 0.8, -size * 0.95),
                        new Vector2(size * 0.6, -size * 0.75)
                };
                Convex ampTop = Geometry.createPolygon(topAmplifier);

                // Lower amplifier array (convex trapezoid)
                Vector2[] bottomAmplifier = new Vector2[]{
                        new Vector2(size * 0.0, size * 0.85),
                        new Vector2(size * 0.6, size * 0.75),
                        new Vector2(size * 0.8, size * 0.95),
                        new Vector2(size * 0.4, size)
                };
                Convex ampBottom = Geometry.createPolygon(bottomAmplifier);

                // Left side crystal array (triangle)
                Vector2[] leftArray = new Vector2[]{
                        new Vector2(-size * 0.3, -size * 0.85),
                        new Vector2(size * 0.1, -size),
                        new Vector2(size * 0.2, -size * 0.8)
                };
                Convex arrayLeft = Geometry.createPolygon(leftArray);

                // Right side crystal array (triangle)
                Vector2[] rightArray = new Vector2[]{
                        new Vector2(-size * 0.3, size * 0.85),
                        new Vector2(size * 0.2, size * 0.8),
                        new Vector2(size * 0.1, size)
                };
                Convex arrayRight = Geometry.createPolygon(rightArray);

                // Upper focusing crystal (small triangle)
                Vector2[] upperCrystal = new Vector2[]{
                        new Vector2(size * 0.3, -size * 0.6),
                        new Vector2(size * 0.6, -size * 0.7),
                        new Vector2(size * 0.5, -size * 0.5)
                };
                Convex crystalU = Geometry.createPolygon(upperCrystal);

                // Lower focusing crystal (small triangle)
                Vector2[] lowerCrystal = new Vector2[]{
                        new Vector2(size * 0.3, size * 0.6),
                        new Vector2(size * 0.5, size * 0.5),
                        new Vector2(size * 0.6, size * 0.7)
                };
                Convex crystalL = Geometry.createPolygon(lowerCrystal);

                // Rear power core (convex pentagon)
                Vector2[] powerCore = new Vector2[]{
                        new Vector2(-size * 1.2, 0),
                        new Vector2(-size * 0.8, -size * 0.5),
                        new Vector2(-size * 0.6, 0),
                        new Vector2(-size * 0.8, size * 0.5)
                };
                Convex core = Geometry.createPolygon(powerCore);

                yield List.of(artilleryBody, primaryLens, ampTop, ampBottom,
                        arrayLeft, arrayRight, crystalU, crystalL, core);
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

            case CRAWLER -> {
                Convex frontTread = Geometry.createCircle(size * .6);
                frontTread.translate(size * .3, 0);
                Convex rearTread = Geometry.createCircle(size * .6);
                rearTread.translate(-size * .3, 0);
                Convex body = Geometry.createRectangle(size * 1.8, size);
                yield List.of(frontTread, rearTread, body);
            }

            // Raider - Nomads hero cavalry with aggressive bladed design
            case RAIDER -> {
                // Main body: aggressive arrow-like chassis (stretched forward)
                Vector2[] mainBody = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.5),
                        new Vector2(size * 0.5, -size * 0.6),
                        new Vector2(size * 1.3, 0),
                        new Vector2(size * 0.5, size * 0.6),
                        new Vector2(-size * 0.9, size * 0.5)
                };
                Convex chassis = Geometry.createPolygon(mainBody);

                // Upper wing blade (aggressive swept-back design)
                Vector2[] upperWing = new Vector2[]{
                        new Vector2(-size * 0.5, -size * 0.7),
                        new Vector2(size * 0.2, -size),
                        new Vector2(size * 0.8, -size * 0.85),
                        new Vector2(size * 0.6, -size * 0.6)
                };
                Convex wingUpper = Geometry.createPolygon(upperWing);

                // Lower wing blade (aggressive swept-back design)
                Vector2[] lowerWing = new Vector2[]{
                        new Vector2(-size * 0.5, size * 0.7),
                        new Vector2(size * 0.6, size * 0.6),
                        new Vector2(size * 0.8, size * 0.85),
                        new Vector2(size * 0.2, size)
                };
                Convex wingLower = Geometry.createPolygon(lowerWing);

                // Upper secondary blade (fractal aggression)
                Vector2[] upperSecondary = new Vector2[]{
                        new Vector2(size * 0.1, -size * 0.75),
                        new Vector2(size * 0.5, -size * 0.95),
                        new Vector2(size * 0.7, -size * 0.75)
                };
                Convex bladeUS = Geometry.createPolygon(upperSecondary);

                // Lower secondary blade (fractal aggression)
                Vector2[] lowerSecondary = new Vector2[]{
                        new Vector2(size * 0.1, size * 0.75),
                        new Vector2(size * 0.7, size * 0.75),
                        new Vector2(size * 0.5, size * 0.95)
                };
                Convex bladeLS = Geometry.createPolygon(lowerSecondary);

                // Rear stabilizer fins (speed aesthetic - upper)
                Vector2[] rearFinUpper = new Vector2[]{
                        new Vector2(-size, -size * 0.4),
                        new Vector2(-size * 0.5, -size * 0.8),
                        new Vector2(-size * 0.3, -size * 0.6),
                        new Vector2(-size * 0.6, -size * 0.4)
                };
                Convex finU = Geometry.createPolygon(rearFinUpper);

                // Rear stabilizer fins (speed aesthetic - lower)
                Vector2[] rearFinLower = new Vector2[]{
                        new Vector2(-size, size * 0.4),
                        new Vector2(-size * 0.6, size * 0.4),
                        new Vector2(-size * 0.3, size * 0.6),
                        new Vector2(-size * 0.5, size * 0.8)
                };
                Convex finL = Geometry.createPolygon(rearFinLower);

                // Engine/power core (rear energy source)
                Vector2[] powerCore = new Vector2[]{
                        new Vector2(-size * 1.1, 0),
                        new Vector2(-size * 0.8, -size * 0.35),
                        new Vector2(-size * 0.6, 0),
                        new Vector2(-size * 0.8, size * 0.35)
                };
                Convex engine = Geometry.createPolygon(powerCore);

                yield List.of(chassis, wingUpper, wingLower, bladeUS, bladeLS, finU, finL, engine);
            }

            // Photon Titan - massive crystalline energy platform with nested prism arrays
            case PHOTON_TITAN -> {
                // Main platform: large convex crystalline base (octagon)
                Vector2[] mainPlatform = new Vector2[]{
                        new Vector2(-size * 1.2, 0),
                        new Vector2(-size * 0.8, -size * 0.9),
                        new Vector2(size * 0.0, -size * 1.1),
                        new Vector2(size * 0.8, -size * 0.9),
                        new Vector2(size * 1.2, 0),
                        new Vector2(size * 0.8, size * 0.9),
                        new Vector2(size * 0.0, size * 1.1),
                        new Vector2(-size * 0.8, size * 0.9)
                };
                Convex titanBase = Geometry.createPolygon(mainPlatform);

                // Primary energy core (central convex hexagon)
                Vector2[] energyCore = new Vector2[]{
                        new Vector2(-size * 0.4, 0),
                        new Vector2(-size * 0.2, -size * 0.5),
                        new Vector2(size * 0.3, -size * 0.5),
                        new Vector2(size * 0.6, 0),
                        new Vector2(size * 0.3, size * 0.5),
                        new Vector2(-size * 0.2, size * 0.5)
                };
                Convex core = Geometry.createPolygon(energyCore);

                // Primary beam emitter (forward prism - pentagon)
                Vector2[] beamEmitter = new Vector2[]{
                        new Vector2(size * 0.7, -size * 0.3),
                        new Vector2(size * 1.4, -size * 0.2),
                        new Vector2(size * 1.5, 0),
                        new Vector2(size * 1.4, size * 0.2),
                        new Vector2(size * 0.7, size * 0.3)
                };
                Convex emitter = Geometry.createPolygon(beamEmitter);

                // Upper crystalline array (convex quadrilateral)
                Vector2[] upperCrystalArray = new Vector2[]{
                        new Vector2(size * 0.0, -size * 1.15),
                        new Vector2(size * 0.5, -size * 1.25),
                        new Vector2(size * 0.7, -size * 0.95),
                        new Vector2(size * 0.3, -size * 0.85)
                };
                Convex upperArray = Geometry.createPolygon(upperCrystalArray);

                // Lower crystalline array (convex quadrilateral)
                Vector2[] lowerCrystalArray = new Vector2[]{
                        new Vector2(size * 0.0, size * 1.15),
                        new Vector2(size * 0.3, size * 0.85),
                        new Vector2(size * 0.7, size * 0.95),
                        new Vector2(size * 0.5, size * 1.25)
                };
                Convex lowerArray = Geometry.createPolygon(lowerCrystalArray);

                // Left wing prism array (convex quadrilateral)
                Vector2[] leftWingArray = new Vector2[]{
                        new Vector2(-size * 0.8, -size * 0.85),
                        new Vector2(-size * 0.2, -size * 1.05),
                        new Vector2(size * 0.2, -size * 0.95),
                        new Vector2(size * 0.0, -size * 0.75)
                };
                Convex leftWing = Geometry.createPolygon(leftWingArray);

                // Right wing prism array (convex quadrilateral)
                Vector2[] rightWingArray = new Vector2[]{
                        new Vector2(-size * 0.8, size * 0.85),
                        new Vector2(size * 0.0, size * 0.75),
                        new Vector2(size * 0.2, size * 0.95),
                        new Vector2(-size * 0.2, size * 1.05)
                };
                Convex rightWing = Geometry.createPolygon(rightWingArray);

                // Upper focusing prism (triangle)
                Vector2[] upperPrism = new Vector2[]{
                        new Vector2(size * 0.3, -size * 0.65),
                        new Vector2(size * 0.7, -size * 0.75),
                        new Vector2(size * 0.6, -size * 0.55)
                };
                Convex prismU = Geometry.createPolygon(upperPrism);

                // Lower focusing prism (triangle)
                Vector2[] lowerPrism = new Vector2[]{
                        new Vector2(size * 0.3, size * 0.65),
                        new Vector2(size * 0.6, size * 0.55),
                        new Vector2(size * 0.7, size * 0.75)
                };
                Convex prismL = Geometry.createPolygon(lowerPrism);

                // Upper micro-crystal (triangle)
                Vector2[] microTop = new Vector2[]{
                        new Vector2(size * 0.4, -size * 0.95),
                        new Vector2(size * 0.6, -size * 1.05),
                        new Vector2(size * 0.7, -size * 0.9)
                };
                Convex microT = Geometry.createPolygon(microTop);

                // Lower micro-crystal (triangle)
                Vector2[] microBottom = new Vector2[]{
                        new Vector2(size * 0.4, size * 0.95),
                        new Vector2(size * 0.7, size * 0.9),
                        new Vector2(size * 0.6, size * 1.05)
                };
                Convex microB = Geometry.createPolygon(microBottom);

                // Rear power resonator (convex pentagon)
                Vector2[] powerResonator = new Vector2[]{
                        new Vector2(-size * 1.3, 0),
                        new Vector2(-size * 0.9, -size * 0.5),
                        new Vector2(-size * 0.7, 0),
                        new Vector2(-size * 0.9, size * 0.5)
                };
                Convex resonator = Geometry.createPolygon(powerResonator);

                yield List.of(titanBase, core, emitter, upperArray, lowerArray,
                        leftWing, rightWing, prismU, prismL, microT, microB, resonator);
            }

            // Colossus - massive robotic walker with mechanical leg assemblies
            case COLOSSUS -> {
                // Central chassis (main robotic body)
                Vector2[] centralBody = new Vector2[]{
                        new Vector2(-size * 0.6, -size * 0.7),
                        new Vector2(size * 0.3, -size * 0.8),
                        new Vector2(size * 0.7, -size * 0.4),
                        new Vector2(size * 0.7, size * 0.4),
                        new Vector2(size * 0.3, size * 0.8),
                        new Vector2(-size * 0.6, size * 0.7)
                };
                Convex chassis = Geometry.createPolygon(centralBody);

                // Upper torso (head/sensor assembly)
                Vector2[] upperTorso = new Vector2[]{
                        new Vector2(-size * 0.3, -size * 0.85),
                        new Vector2(size * 0.2, -size),
                        new Vector2(size * 0.6, -size * 0.75),
                        new Vector2(size * 0.4, -size * 0.55)
                };
                Convex torso = Geometry.createPolygon(upperTorso);

                // Lower torso (power core section)
                Vector2[] lowerTorso = new Vector2[]{
                        new Vector2(-size * 0.3, size * 0.85),
                        new Vector2(size * 0.4, size * 0.55),
                        new Vector2(size * 0.6, size * 0.75),
                        new Vector2(size * 0.2, size)
                };
                Convex lowerCore = Geometry.createPolygon(lowerTorso);

                // Left leg assembly (upper - mechanical joint)
                Vector2[] leftLegUpper = new Vector2[]{
                        new Vector2(-size * 0.8, -size * 0.9),
                        new Vector2(-size * 0.3, -size * 1.15),
                        new Vector2(size * 0.0, -size * 1.1),
                        new Vector2(-size * 0.2, -size * 0.85)
                };
                Convex legLU = Geometry.createPolygon(leftLegUpper);

                // Right leg assembly (upper - mechanical joint)
                Vector2[] rightLegUpper = new Vector2[]{
                        new Vector2(-size * 0.8, size * 0.9),
                        new Vector2(-size * 0.2, size * 0.85),
                        new Vector2(size * 0.0, size * 1.1),
                        new Vector2(-size * 0.3, size * 1.15)
                };
                Convex legRU = Geometry.createPolygon(rightLegUpper);

                // Left leg extension (lower segment)
                Vector2[] leftLegLower = new Vector2[]{
                        new Vector2(-size * 0.5, -size * 1.2),
                        new Vector2(-size * 0.1, -size * 1.3),
                        new Vector2(size * 0.2, -size * 1.15),
                        new Vector2(size * 0.1, -size)
                };
                Convex legLL = Geometry.createPolygon(leftLegLower);

                // Right leg extension (lower segment)
                Vector2[] rightLegLower = new Vector2[]{
                        new Vector2(-size * 0.5, size * 1.2),
                        new Vector2(size * 0.1, size),
                        new Vector2(size * 0.2, size * 1.15),
                        new Vector2(-size * 0.1, size * 1.3)
                };
                Convex legRL = Geometry.createPolygon(rightLegLower);

                // Front arm/weapon mount (left)
                Vector2[] frontArmLeft = new Vector2[]{
                        new Vector2(size * 0.5, -size * 0.6),
                        new Vector2(size * 0.9, -size * 0.7),
                        new Vector2(size, -size * 0.4),
                        new Vector2(size * 0.7, -size * 0.35)
                };
                Convex armFL = Geometry.createPolygon(frontArmLeft);

                // Front arm/weapon mount (right)
                Vector2[] frontArmRight = new Vector2[]{
                        new Vector2(size * 0.5, size * 0.6),
                        new Vector2(size * 0.7, size * 0.35),
                        new Vector2(size, size * 0.4),
                        new Vector2(size * 0.9, size * 0.7)
                };
                Convex armFR = Geometry.createPolygon(frontArmRight);

                // Rear stabilizer strut (mechanical support - left)
                Vector2[] rearStrutLeft = new Vector2[]{
                        new Vector2(-size * 0.9, -size * 0.5),
                        new Vector2(-size * 0.5, -size * 0.75),
                        new Vector2(-size * 0.3, -size * 0.6),
                        new Vector2(-size * 0.6, -size * 0.4)
                };
                Convex strutL = Geometry.createPolygon(rearStrutLeft);

                // Rear stabilizer strut (mechanical support - right)
                Vector2[] rearStrutRight = new Vector2[]{
                        new Vector2(-size * 0.9, size * 0.5),
                        new Vector2(-size * 0.6, size * 0.4),
                        new Vector2(-size * 0.3, size * 0.6),
                        new Vector2(-size * 0.5, size * 0.75)
                };
                Convex strutR = Geometry.createPolygon(rearStrutRight);

                // Back power unit (reactor/engine)
                Vector2[] backPower = new Vector2[]{
                        new Vector2(-size * 0.9, 0),
                        new Vector2(-size * 0.6, -size * 0.35),
                        new Vector2(-size * 0.4, 0),
                        new Vector2(-size * 0.6, size * 0.35)
                };
                Convex reactor = Geometry.createPolygon(backPower);

                yield List.of(chassis, torso, lowerCore, legLU, legRU, legLL, legRL,
                        armFL, armFR, strutL, strutR, reactor);
            }
        };
    }

    UnitType(String displayName, int resourceCost, int buildTimeSeconds, double maxHealth,
             double movementSpeed, double damage, double attackRate, double attackRange,
             double size, int sides, int color, BuildingType producedBy, int upkeepCost, double visionRange,
             Elevation elevation) {
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
        this.elevation = elevation;
    }

    /**
     * Get the special ability for this unit type
     */
    public SpecialAbility getSpecialAbility() {
        return switch (this) {
            case CRAWLER -> SpecialAbility.DEPLOY;
            case MEDIC -> SpecialAbility.HEAL;
            case ENGINEER -> SpecialAbility.REPAIR;
            case CLOAK_TANK -> SpecialAbility.CLOAK;
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
        return this != WORKER && this != MEDIC && this != ENGINEER;
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
     * Check if this unit can mine obstacles (REMOVED - workers now harvest from obstacles)
     */
    @Deprecated
    public boolean canMine() {
        return false; // Mining removed - workers harvest resources from obstacles instead
    }

    /**
     * Check if this is a support unit (non-combat)
     */
    public boolean isSupport() {
        return this == MEDIC || this == ENGINEER;
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

    /**
     * Check if this is an air unit (can fly over obstacles, different rendering)
     */
    public boolean isAirUnit() {
        return this == SCOUT_DRONE || this == HELICOPTER || this == BOMBER || this == INTERCEPTOR || this == GUNSHIP;
    }

    /**
     * Check if this is a sortie-based unit (not player-controllable, executes missions and returns to base)
     */
    public boolean isSortieBased() {
        return this == BOMBER || this == INTERCEPTOR || this == GUNSHIP;
    }
}

