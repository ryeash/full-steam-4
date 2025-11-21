package com.fullsteam.model;

import lombok.Getter;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Vector2;

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
            50,      // max health
            100.0,   // movement speed
            5,       // damage
            1.0,     // attack rate
            100,     // attack range
            15.0,    // size (radius)
            16,      // sides (16-sided polygon approximates circle)
            0xFFFF00, // yellow
            BuildingType.HEADQUARTERS,
            5        // upkeep cost
    ),

    // Infantry - basic combat unit
    INFANTRY(
            "Infantry",
            75,      // resource cost
            5,       // build time (seconds)
            80,      // max health
            120.0,   // movement speed
            15,      // damage
            2.0,     // attack rate
            150,     // attack range
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x00FF00, // green
            BuildingType.BARRACKS,
            10       // upkeep cost
    ),

    // Laser Infantry - advanced infantry with beam weapons
    LASER_INFANTRY(
            "Laser Infantry",
            125,     // resource cost (more expensive than regular infantry)
            7,       // build time (seconds)
            80,      // max health (same as infantry)
            120.0,   // movement speed (same as infantry)
            20,      // damage (higher than infantry)
            1.5,     // attack rate (faster than infantry)
            180,     // attack range (longer than infantry)
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x00FFFF, // cyan (to distinguish from regular infantry)
            BuildingType.BARRACKS,
            12       // upkeep cost (higher than infantry)
    ),

    // Medic - support unit that heals nearby friendlies
    MEDIC(
            "Medic",
            100,     // resource cost
            8,       // build time (seconds)
            60,      // max health (fragile)
            110.0,   // movement speed
            0,       // damage (cannot attack)
            0.0,     // attack rate
            0,       // attack range
            12.0,    // size (radius)
            6,       // sides (hexagon)
            0xFFFFFF, // white
            BuildingType.BARRACKS,
            8        // upkeep cost
    ),

    // Rocket Soldier - anti-vehicle infantry
    ROCKET_SOLDIER(
            "Rocket Soldier",
            150,     // resource cost
            8,       // build time (seconds)
            70,      // max health
            110.0,   // movement speed
            40,      // damage
            0.8,     // attack rate (slower)
            200,     // attack range
            12.0,    // size (radius)
            3,       // sides (triangle)
            0xFF8800, // orange
            BuildingType.WEAPONS_DEPOT,
            15       // upkeep cost
    ),

    // Sniper - long-range precision unit
    SNIPER(
            "Sniper",
            200,     // resource cost
            10,      // build time (seconds)
            50,      // max health (very fragile)
            100.0,   // movement speed
            50,      // damage (high)
            0.5,     // attack rate (slow, precise shots)
            300,     // attack range (very long)
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x8B4513, // brown
            BuildingType.WEAPONS_DEPOT,
            12       // upkeep cost
    ),

    // Engineer - repairs buildings and vehicles
    ENGINEER(
            "Engineer",
            150,     // resource cost
            12,      // build time (seconds)
            70,      // max health
            105.0,   // movement speed
            0,       // damage (cannot attack)
            0.0,     // attack rate
            0,       // attack range
            13.0,    // size (radius)
            6,       // sides (hexagon)
            0xFFD700, // gold
            BuildingType.WEAPONS_DEPOT,
            10       // upkeep cost
    ),

    // Miner - destroys obstacles to clear paths
    MINER(
            "Miner",
            100,     // resource cost
            10,      // build time (seconds)
            60,      // max health
            95.0,    // movement speed (slower than worker)
            0,       // damage (cannot attack)
            0.0,     // attack rate
            0,       // attack range
            14.0,    // size (radius)
            8,       // sides (octagon)
            0x8B4513, // brown (like dirt/stone)
            BuildingType.HEADQUARTERS,
            8        // upkeep cost
    ),

    // Jeep - fast light vehicle
    JEEP(
            "Jeep",
            200,     // resource cost
            10,      // build time (seconds)
            120,     // max health
            180.0,   // movement speed (fast!)
            20,      // damage
            3.0,     // attack rate
            180,     // attack range
            20.0,    // size (radius)
            4,       // sides (rectangle)
            0x00FFFF, // cyan
            BuildingType.FACTORY,
            20       // upkeep cost
    ),

    // Tank - heavy armored vehicle
    TANK(
            "Tank",
            400,     // resource cost
            15,      // build time (seconds)
            300,     // max health
            80.0,    // movement speed (slow)
            60,      // damage
            1.2,     // attack rate
            220,     // attack range
            30.0,    // size (radius)
            5,       // sides (pentagon)
            0x8888FF, // light blue
            BuildingType.FACTORY,
            30       // upkeep cost
    ),

    // Artillery - long range siege unit
    ARTILLERY(
            "Artillery",
            500,     // resource cost
            20,      // build time (seconds)
            150,     // max health
            60.0,    // movement speed (very slow)
            100,     // damage (high!)
            0.5,     // attack rate (very slow)
            400,     // attack range (very long!)
            25.0,    // size (radius)
            6,       // sides (hexagon)
            0xFF00FF, // magenta
            BuildingType.ADVANCED_FACTORY,
            40       // upkeep cost
    ),

    // GIGANTONAUT - Super heavy artillery
    GIGANTONAUT(
            "Gigantonaut",
            1200,    // resource cost (VERY EXPENSIVE!)
            35,      // build time (seconds) (LONG!)
            300,     // max health (tanky!)
            30.0,    // movement speed (SLOWEST!)
            250,     // damage (MASSIVE!)
            0.3,     // attack rate (EXTREMELY SLOW!)
            450,     // attack range (LONGEST!)
            35.0,    // size (radius) (HUGE!)
            8,       // sides (octagon)
            0x8B0000, // dark red
            BuildingType.ADVANCED_FACTORY,
            60       // upkeep cost (VERY HIGH!)
    ),

    // CRAWLER - Mobile fortress with 4 turrets (THE STAR UNIT!)
    CRAWLER(
            "Crawler",
            1500,    // resource cost (EXPENSIVE!)
            45,      // build time (seconds) (LONG!)
            2000,    // max health (MASSIVE!)
            40.0,    // movement speed (VERY SLOW!)
            60,      // damage (per turret)
            1.0,     // attack rate
            250,     // attack range
            50.0,    // size (radius) (HUGE!)
            8,       // sides (octagon)
            0x4A4A4A, // dark gray
            BuildingType.ADVANCED_FACTORY,
            80       // upkeep cost (HIGHEST!)
    ),

    // Stealth Tank - invisible until attacking
    STEALTH_TANK(
            "Stealth Tank",
            800,     // resource cost
            25,      // build time (seconds)
            200,     // max health
            100.0,   // movement speed
            50,      // damage
            1.5,     // attack rate
            200,     // attack range
            28.0,    // size (radius)
            5,       // sides (pentagon)
            0x2F4F4F, // dark slate gray
            BuildingType.ADVANCED_FACTORY,
            45       // upkeep cost
    ),

    // Mammoth Tank - dual-cannon heavy assault
    MAMMOTH_TANK(
            "Mammoth Tank",
            1200,    // resource cost
            35,      // build time (seconds)
            1500,    // max health (very tanky)
            50.0,    // movement speed (very slow)
            80,      // damage (high)
            0.8,     // attack rate (slow but powerful)
            230,     // attack range
            40.0,    // size (radius) (large)
            6,       // sides (hexagon)
            0x556B2F, // dark olive green
            BuildingType.ADVANCED_FACTORY,
            60       // upkeep cost
    ),
    
    // ===== HERO UNITS =====
    
    // PALADIN - Terran hero unit, balanced powerhouse
    PALADIN(
            "Paladin",
            1100,    // resource cost
            32,      // build time (seconds)
            850,     // max health (very tanky)
            95.0,    // movement speed (moderate)
            70,      // damage (high)
            1.5,     // attack rate (balanced)
            220,     // attack range
            32.0,    // size (radius)
            8,       // sides (octagon - balanced)
            0xC0C0C0, // silver (Terran hero)
            BuildingType.ADVANCED_FACTORY,
            55       // upkeep cost
    ),
    
    // RAIDER - Nomads hero unit, fast hit-and-run cavalry
    RAIDER(
            "Raider",
            900,     // resource cost
            28,      // build time (seconds)
            280,     // max health (moderate)
            220.0,   // movement speed (VERY FAST - fastest unit!)
            55,      // damage (high)
            2.2,     // attack rate (fast)
            180,     // attack range
            22.0,    // size (radius)
            3,       // sides (triangle - agile)
            0xDC143C, // crimson (raider red)
            BuildingType.ADVANCED_FACTORY,
            45       // upkeep cost
    ),
    
    // COLOSSUS - Synthesis hero unit, massive walker
    COLOSSUS(
            "Colossus",
            1600,    // resource cost (VERY EXPENSIVE!)
            45,      // build time (seconds) (VERY LONG!)
            2200,    // max health (EXTREMELY TANKY!)
            40.0,    // movement speed (VERY SLOW)
            95,      // damage (very high)
            0.9,     // attack rate (moderate)
            250,     // attack range
            50.0,    // size (radius) (MASSIVE!)
            6,       // sides (hexagon)
            0x4B0082, // indigo (synthesis purple)
            BuildingType.ADVANCED_FACTORY,
            75       // upkeep cost (VERY HIGH!)
    ),
    
    // ===== TECH ALLIANCE BEAM WEAPON UNITS =====
    
    // PLASMA_TROOPER - Basic beam infantry (Tech Alliance equivalent of Infantry)
    PLASMA_TROOPER(
            "Plasma Trooper",
            100,     // resource cost
            6,       // build time (seconds)
            85,      // max health (slightly tankier than infantry)
            115.0,   // movement speed
            18,      // damage
            2.0,     // attack rate (fast)
            170,     // attack range
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x00FF7F, // spring green (plasma color)
            BuildingType.BARRACKS,
            11       // upkeep cost
    ),
    
    // ION_RANGER - Long-range beam sniper
    ION_RANGER(
            "Ion Ranger",
            250,     // resource cost
            12,      // build time (seconds)
            60,      // max health (fragile)
            105.0,   // movement speed
            60,      // damage (very high)
            0.6,     // attack rate (slow, precise)
            320,     // attack range (very long)
            12.0,    // size (radius)
            3,       // sides (triangle)
            0x9370DB, // medium purple (ion beam)
            BuildingType.WEAPONS_DEPOT,
            14       // upkeep cost
    ),
    
    // PHOTON_SCOUT - Fast beam vehicle
    PHOTON_SCOUT(
            "Photon Scout",
            220,     // resource cost
            11,      // build time (seconds)
            110,     // max health
            190.0,   // movement speed (very fast!)
            22,      // damage
            2.5,     // attack rate (rapid fire)
            190,     // attack range
            18.0,    // size (radius)
            4,       // sides (rectangle)
            0x7FFF00, // chartreuse (bright energy)
            BuildingType.FACTORY,
            22       // upkeep cost
    ),
    
    // BEAM_TANK - Heavy beam vehicle
    BEAM_TANK(
            "Beam Tank",
            450,     // resource cost
            16,      // build time (seconds)
            320,     // max health (tankier than regular tank)
            75.0,    // movement speed (slow)
            65,      // damage
            1.3,     // attack rate
            230,     // attack range
            30.0,    // size (radius)
            6,       // sides (hexagon)
            0x00FA9A, // medium spring green
            BuildingType.FACTORY,
            32       // upkeep cost
    ),
    
    // PULSE_ARTILLERY - Long-range beam artillery
    PULSE_ARTILLERY(
            "Pulse Artillery",
            550,     // resource cost
            22,      // build time (seconds)
            140,     // max health
            55.0,    // movement speed (very slow)
            110,     // damage (very high!)
            0.6,     // attack rate (slow)
            420,     // attack range (very long!)
            26.0,    // size (radius)
            6,       // sides (hexagon)
            0xFFD700, // gold (energy pulse)
            BuildingType.ADVANCED_FACTORY,
            42       // upkeep cost
    ),
    
    // PHOTON_TITAN - Hero unit, massive beam platform
    PHOTON_TITAN(
            "Photon Titan",
            1400,    // resource cost (VERY EXPENSIVE!)
            40,      // build time (seconds) (LONG!)
            350,     // max health (very tanky!)
            35.0,    // movement speed (VERY SLOW!)
            280,     // damage (MASSIVE!)
            0.4,     // attack rate (slow but devastating)
            460,     // attack range (LONGEST!)
            38.0,    // size (radius) (HUGE!)
            8,       // sides (octagon)
            0x00FF00, // bright green (pure energy)
            BuildingType.ADVANCED_FACTORY,
            65       // upkeep cost (VERY HIGH!)
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
     * Create a physics fixture for this unit type
     * This allows each unit to have a custom shape (not just regular polygons)
     * Returns a Convex shape that will be added to the unit's physics body
     */
    public Convex createPhysicsFixture() {
        return switch (this) {
            // Infantry units - triangular (pointing forward)
            case INFANTRY, LASER_INFANTRY, ROCKET_SOLDIER, SNIPER, PLASMA_TROOPER, ION_RANGER -> 
                    Geometry.createPolygonalCircle(3, size);

            // Worker/Support units - circular for easy navigation
            case WORKER, MINER, MEDIC, ENGINEER -> Geometry.createCircle(size);

            // Light vehicles - elongated rectangle (fast, nimble)
            case JEEP, PHOTON_SCOUT -> Geometry.createRectangle(size * 1.6, size);

            // Tanks - hexagonal (balanced)
            case TANK, STEALTH_TANK, BEAM_TANK -> Geometry.createPolygonalCircle(6, size);

            // Artillery - pentagonal (specialized)
            case ARTILLERY, PULSE_ARTILLERY -> Geometry.createPolygonalCircle(5, size);

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
                yield Geometry.createPolygon(vertices);
            }

            // Mammoth Tank - large octagon (heavy armor)
            case MAMMOTH_TANK -> Geometry.createPolygonalCircle(8, size);

            // Crawler - wide rectangle when mobile, transforms when deployed
            case CRAWLER -> Geometry.createRectangle(size * 2.0, size * 1.2);
            
            // Photon Titan - large octagon (hero unit)
            case PHOTON_TITAN -> Geometry.createPolygonalCircle(8, size);
            
            // Paladin - octagon (balanced hero)
            case PALADIN -> Geometry.createPolygonalCircle(8, size);
            
            // Raider - triangle (fast, agile)
            case RAIDER -> Geometry.createPolygonalCircle(3, size);
            
            // Colossus - large hexagon (massive walker)
            case COLOSSUS -> Geometry.createPolygonalCircle(6, size);
        };
    }

    UnitType(String displayName, int resourceCost, int buildTimeSeconds, double maxHealth,
             double movementSpeed, double damage, double attackRate, double attackRange,
             double size, int sides, int color, BuildingType producedBy, int upkeepCost) {
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
               this == ENGINEER;
    }

    /**
     * Get vision range for this unit type
     * Most units: 1.5x attack range
     * Gigantonaut: terrible vision (0.5x attack range)
     */
    public double getVisionRange() {
        if (this == GIGANTONAUT) {
            return attackRange * 0.5; // Terrible vision!
        }
        return attackRange * 1.5; // Normal vision
    }
}

