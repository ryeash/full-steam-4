package com.fullsteam.model;

import lombok.Getter;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Defines the different types of buildings available in the RTS game.
 * Shape rendering is handled by the number of sides (3=triangle, 4=rectangle, etc.)
 */
@Getter
public enum BuildingType {
    // Main base - produces workers and miners, required to win
    HEADQUARTERS(
            "Headquarters",
            0,       // free (starting building)
            0,       // no build time
            1050,    // max health (+5%)
            80.0,    // size (radius)
            8,       // sides (octagon)
            0xFFD700, // gold
            true     // can produce units
    ),

    // Resource collection point
    REFINERY(
            "Refinery",
            300,     // resource cost
            20,      // build time (seconds)
            525,     // max health (+5%)
            50.0,    // size (radius)
            6,       // sides (hexagon)
            0x808080, // gray
            false    // cannot produce units
    ),

    // Infantry production
    BARRACKS(
            "Barracks",
            200,     // resource cost
            15,      // build time (seconds)
            630,     // max health (+5%)
            45.0,    // size (radius)
            4,       // sides (rectangle)
            0x8B4513, // brown
            true     // can produce units
    ),

    // Power generation - required for advanced buildings
    POWER_PLANT(
            "Power Plant",
            250,     // resource cost
            20,      // build time (seconds)
            420,     // max health (+5%)
            40.0,    // size (radius)
            6,       // sides (hexagon)
            0xFFFF00, // yellow
            false    // cannot produce units
    ),

    // Vehicle production
    FACTORY(
            "Factory",
            400,     // resource cost
            25,      // build time (seconds)
            735,     // max health (+5%)
            55.0,    // size (radius)
            4,       // sides (rectangle)
            0x696969, // dark gray
            true     // can produce units
    ),

    // Research and tech unlocking - unlocks T2
    RESEARCH_LAB(
            "Research Lab",
            500,     // resource cost
            30,      // build time (seconds)
            630,     // max health (+5%)
            50.0,    // size (radius)
            6,       // sides (hexagon)
            0x00CED1, // dark turquoise
            false    // cannot produce units
    ),

    // Advanced infantry production - requires Research Lab
    WEAPONS_DEPOT(
            "Weapons Depot",
            400,     // resource cost
            25,      // build time (seconds)
            683,     // max health (+5%)
            48.0,    // size (radius)
            5,       // sides (pentagon)
            0x8B0000, // dark red
            true     // can produce units
    ),

    // Elite tech unlocking - unlocks T3
    TECH_CENTER(
            "Tech Center",
            800,     // resource cost
            40,      // build time (seconds)
            840,     // max health (+5%)
            60.0,    // size (radius)
            8,       // sides (octagon)
            0x4169E1, // royal blue
            false    // cannot produce units
    ),

    // Heavy vehicle production - requires Tech Center
    ADVANCED_FACTORY(
            "Advanced Factory",
            1000,    // resource cost
            45,      // build time (seconds)
            945,     // max health (+5%)
            65.0,    // size (radius)
            6,       // sides (hexagon)
            0x2F4F4F, // dark slate gray
            true     // can produce units
    ),

    // Defensive structure - blocks movement
    WALL(
            "Wall",
            50,      // resource cost
            5,       // build time (seconds)
            420,     // max health (+5%)
            15.0,    // size (radius) - small for tight placement
            4,       // sides (square)
            0x708090, // slate gray
            false    // cannot produce units
    ),

    // Defensive structure - attacks enemies
    TURRET(
            "Turret",
            250,     // resource cost
            15,      // build time (seconds)
            420,     // max health (+5%)
            25.0,    // size (radius)
            5,       // sides (pentagon)
            0xFF4500, // orange red
            false    // cannot produce units
    ),
    
    // Defensive structure - infantry can garrison inside and fire out
    BUNKER(
            "Bunker",
            250,     // resource cost - reduced to make it accessible as T1
            18,      // build time (seconds)
            840,     // max health (+5%)
            35.0,    // size (radius)
            4,       // sides (rectangle)
            0x556B2F, // dark olive green
            false,   // cannot produce units
            6        // can garrison 6 infantry units
    ),

    // Defensive structure - projects shield that destroys incoming projectiles
    SHIELD_GENERATOR(
            "Shield Generator",
            400,     // resource cost
            25,      // build time (seconds)
            525,     // max health (+5%)
            30.0,    // size (radius)
            6,       // sides (hexagon)
            0x00BFFF, // deep sky blue
            false    // cannot produce units
    ),
    
    // Economic building - generates passive income based on current credits (compound interest)
    BANK(
            "Bank",
            600,     // resource cost (expensive T3 building)
            30,      // build time (seconds)
            420,     // max health (+5%)
            35.0,    // size (radius)
            8,       // sides (octagon)
            0xFFD700, // gold
            false,   // cannot produce units
            0        // no garrison capacity
    ),
    
    // ===== HERO/MONUMENT BUILDINGS =====
    
    // Monument - Nomads faction - creates periodic sandstorms for area denial
    SANDSTORM_GENERATOR(
            "Sandstorm Generator",
            600,     // resource cost
            35,      // build time (seconds)
            735,     // max health (+5%)
            35.0,    // size (radius) - reduced from 45
            6,       // sides (hexagon)
            0xDEB887, // burlywood (sandy color)
            false    // cannot produce units
    ),
    
    // Monument - Synthesis faction - provides shield/armor bonus to nearby units
    QUANTUM_NEXUS(
            "Quantum Nexus",
            700,     // resource cost
            40,      // build time (seconds)
            945,     // max health (+5%)
            40.0,    // size (radius) - reduced from 50
            8,       // sides (octagon)
            0x9370DB, // medium purple (quantum energy)
            false    // cannot produce units
    ),
    
    // Monument - Tech Alliance faction - amplifies beam weapon damage
    PHOTON_SPIRE(
            "Photon Spire",
            650,     // resource cost
            38,      // build time (seconds)
            788,     // max health (+5%)
            48.0,    // size (radius)
            6,       // sides (hexagon)
            0x00FF00, // bright green (photon energy)
            false    // cannot produce units
    );

    private final String displayName;
    private final int resourceCost;
    private final int buildTimeSeconds;
    private final double maxHealth;
    private final double size; // radius for collision
    private final int sides; // number of sides for polygon rendering
    private final int color; // hex color for rendering
    private final boolean canProduceUnits;
    private final int garrisonCapacity; // Number of units that can garrison (0 = no garrison)

    BuildingType(String displayName, int resourceCost, int buildTimeSeconds, double maxHealth,
                 double size, int sides, int color, boolean canProduceUnits) {
        this(displayName, resourceCost, buildTimeSeconds, maxHealth, size, sides, color, canProduceUnits, 0);
    }
    
    BuildingType(String displayName, int resourceCost, int buildTimeSeconds, double maxHealth,
                 double size, int sides, int color, boolean canProduceUnits, int garrisonCapacity) {
        this.displayName = displayName;
        this.resourceCost = resourceCost;
        this.buildTimeSeconds = buildTimeSeconds;
        this.maxHealth = maxHealth;
        this.size = size;
        this.sides = sides;
        this.color = color;
        this.canProduceUnits = canProduceUnits;
        this.garrisonCapacity = garrisonCapacity;
    }

    /**
     * Get the units that this building can produce
     * NOTE: This method is called after enum initialization, so UnitType values are safe to reference
     */
    public UnitType[] getProducibleUnits() {
        if (!canProduceUnits) {
            return new UnitType[]{};
        }

        // Use a switch to avoid circular dependency during enum initialization
        // This returns ALL possible units a building can produce (used for "all available" factions)
        return switch (this) {
            case HEADQUARTERS -> new UnitType[]{UnitType.WORKER, UnitType.MINER};
            case BARRACKS -> new UnitType[]{
                    UnitType.INFANTRY, 
                    UnitType.LASER_INFANTRY, 
                    UnitType.PLASMA_TROOPER,  // Tech Alliance
                    UnitType.MEDIC
            };
            case FACTORY -> new UnitType[]{
                    UnitType.JEEP, 
                    UnitType.TANK,
                    UnitType.PHOTON_SCOUT,  // Tech Alliance
                    UnitType.BEAM_TANK      // Tech Alliance
            };
            case WEAPONS_DEPOT -> new UnitType[]{
                    UnitType.ROCKET_SOLDIER, 
                    UnitType.SNIPER, 
                    UnitType.ION_RANGER,  // Tech Alliance
                    UnitType.ENGINEER
            };
            case ADVANCED_FACTORY -> new UnitType[]{
                    UnitType.ARTILLERY, 
                    UnitType.GIGANTONAUT, 
                    UnitType.CRAWLER,
                    UnitType.STEALTH_TANK, 
                    UnitType.MAMMOTH_TANK,
                    UnitType.PULSE_ARTILLERY  // Tech Alliance
                    // Note: Hero units (PALADIN, RAIDER, COLOSSUS, PHOTON_TITAN) are faction-specific
                    // and defined in FactionRegistry, not in this base list
            };
            default -> new UnitType[]{};
        };
    }

    /**
     * Check if this building can produce a specific unit type
     */
    public boolean canProduce(UnitType unitType) {
        if (!canProduceUnits) {
            return false;
        }
        for (UnitType producible : getProducibleUnits()) {
            if (producible == unitType) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if this building is defensive (turret)
     */
    public boolean isDefensive() {
        return this == TURRET;
    }

    /**
     * Check if this building is a resource collector
     */
    public boolean isResourceCollector() {
        return this == REFINERY;
    }

    /**
     * Check if this building provides power
     */
    public boolean providesPower() {
        return this == POWER_PLANT;
    }

    /**
     * Check if this building is a tech building (unlocks tiers)
     */
    public boolean isTechBuilding() {
        return this == RESEARCH_LAB || this == TECH_CENTER;
    }

    /**
     * Get the tech tier this building unlocks (0 if not a tech building)
     */
    public int getTechTierUnlocked() {
        if (this == RESEARCH_LAB) {
            return 2;
        }
        if (this == TECH_CENTER) {
            return 3;
        }
        return 0;
    }

    /**
     * Get the tech tier required to build this building
     */
    public int getRequiredTechTier() {
        return switch (this) {
            case HEADQUARTERS, REFINERY, BARRACKS, POWER_PLANT, BUNKER, WALL -> 1;
            case FACTORY, RESEARCH_LAB, WEAPONS_DEPOT, TURRET, SHIELD_GENERATOR -> 2;
            case TECH_CENTER, ADVANCED_FACTORY, BANK, SANDSTORM_GENERATOR, QUANTUM_NEXUS, PHOTON_SPIRE -> 3;
        };
    }

    /**
     * Get power consumption (negative) or generation (positive)
     * HQ provides base power, Power Plants provide additional power
     */
    public int getPowerValue() {
        return switch (this) {
            // Power generators
            case HEADQUARTERS -> 50;      // Base power (enough for Barracks + Refinery)
            case POWER_PLANT -> 100;      // Main power generation

            // T1 consumers (low power)
            case BARRACKS -> -25;
            case REFINERY -> -10;
            case WALL -> 0;               // No power needed

            // T2 consumers (medium power)
            case FACTORY -> -30;
            case WEAPONS_DEPOT -> -25;
            case RESEARCH_LAB -> -35;
            case TURRET -> -20;
            case SHIELD_GENERATOR -> -40; // High power consumption for active shield

            // T3 consumers (high power)
            case TECH_CENTER -> -50;
            case ADVANCED_FACTORY -> -45;
            case BANK -> -30; // Moderate power for financial systems
            case BUNKER -> -15; // Low power for life support systems
            
            // Monument buildings (high power consumption for special effects)
            case SANDSTORM_GENERATOR -> -40; // Weather control systems
            case QUANTUM_NEXUS -> -70; // Quantum field generators (VERY high power!)
            case PHOTON_SPIRE -> -45; // Beam amplification systems
        };
    }

    /**
     * Create a physics fixture for this building type
     * This allows each building to have a custom shape (not just regular polygons)
     * Returns a Convex shape that will be added to the building's physics body
     */
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
            
            // Weapons Depot - pentagonal armory
            case WEAPONS_DEPOT -> List.of(Geometry.createPolygonalCircle(5, size));
            
            // Tech Center - large octagon (advanced tech)
            case TECH_CENTER -> List.of(Geometry.createPolygonalCircle(8, size));
            
            // Advanced Factory - massive rectangular production facility
            case ADVANCED_FACTORY -> List.of(Geometry.createRectangle(size * 2.2, size * 1.5));
            
            // Wall - small square segment
            case WALL -> List.of(Geometry.createSquare(size * 2.0));
            
            // Turret - pentagonal defensive structure
            case TURRET -> List.of(Geometry.createPolygonalCircle(5, size));
            
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
                    vertices[i] = new Vector2(
                        Math.cos(theta) * size,
                        Math.sin(theta) * size
                    );
                }
                yield List.of(Geometry.createPolygon(vertices));
            }
            
            // Sandstorm Generator - hexagonal weather control station
            case SANDSTORM_GENERATOR -> List.of(Geometry.createPolygonalCircle(6, size));
            
            // Quantum Nexus - large octagon (quantum energy)
            case QUANTUM_NEXUS -> List.of(Geometry.createPolygonalCircle(8, size));
            
            // Photon Spire - hexagonal beam amplifier
            case PHOTON_SPIRE -> List.of(Geometry.createPolygonalCircle(6, size));
        };
    }
}

