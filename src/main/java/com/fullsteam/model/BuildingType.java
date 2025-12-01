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
            true,    // can produce units
            50,      // power generation
            500.0    // vision range (excellent, main base)
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
            false,   // cannot produce units
            -10,     // power consumption
            350.0    // vision range (moderate, economic building)
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
            true,    // can produce units
            -25,     // power consumption
            380.0    // vision range (good, production building)
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
            false,   // cannot produce units
            100,     // power generation
            360.0    // vision range (moderate, utility building)
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
            true,    // can produce units
            -30,     // power consumption
            390.0    // vision range (good, production building)
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
            false,   // cannot produce units
            -35,     // power consumption
            400.0    // vision range (good, tech building)
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
            true,    // can produce units
            -25,     // power consumption
            390.0    // vision range (good, production building)
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
            false,   // cannot produce units
            -50,     // power consumption
            420.0    // vision range (excellent, advanced tech)
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
            true,    // can produce units
            -45,     // power consumption
            400.0    // vision range (good, advanced production)
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
            false,   // cannot produce units
            0,       // no power needed
            250.0    // vision range (limited, small defensive structure)
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
            false,   // cannot produce units
            -20,     // power consumption
            450.0    // vision range (excellent, needs to spot threats)
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
            // can garrison 6 infantry units
            -15,     // power consumption
            420.0    // vision range (excellent, defensive structure)
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
            false,   // cannot produce units
            -40,     // power consumption
            380.0    // vision range (good, defensive utility)
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
            // no garrison capacity
            -30,     // power consumption
            350.0    // vision range (moderate, economic building)
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
            false,   // cannot produce units
            -40,     // power consumption
            430.0    // vision range (good, monument)
    ),

    // Monument - Synthesis faction - autonomous android production facility
    ANDROID_FACTORY(
            "Android Factory",
            700,     // resource cost
            40,      // build time (seconds)
            900,     // max health
            42.0,    // size (radius)
            8,       // sides (octagon)
            0x00CED1, // dark turquoise (Synthesis faction color)
            true,    // can produce units (Androids!)
            -60,     // power consumption
            420.0    // vision range (excellent, monument production)
    ),

    // Monument - Tech Alliance faction - powerful defensive laser tower (Obelisk of Light style)
    PHOTON_SPIRE(
            "Photon Spire",
            650,     // resource cost
            38,      // build time (seconds)
            1200,    // max health (very durable for a defensive structure)
            48.0,    // size (radius)
            6,       // sides (hexagon)
            0x00FF00, // bright green (photon energy)
            false,   // cannot produce units
            -75,     // power consumption
            480.0    // vision range (excellent, defensive monument)
    ),

    // Monument - Terran faction - ultimate command center
    COMMAND_CITADEL(
            "Command Citadel",
            700,     // resource cost (expensive monument)
            40,      // build time (seconds)
            1050,    // max health (+5%)d
            55.0,    // size (radius) - large and imposing
            8,       // sides (octagon)
            0x4169E1, // royal blue (command authority)
            false,   // cannot produce units
            -50,     // power consumption
            800.0    // vision range (HUGE, command center bonus)
    );

    private final String displayName;
    private final int resourceCost;
    private final int buildTimeSeconds;
    private final double maxHealth;
    private final double size; // radius for collision
    private final int sides; // number of sides for polygon rendering
    private final int color; // hex color for rendering
    private final boolean canProduceUnits;
    private final int powerValue; // Power generation (positive) or consumption (negative)
    private final double visionRange; // vision radius for fog of war

    BuildingType(String displayName, int resourceCost, int buildTimeSeconds, double maxHealth,
                 double size, int sides, int color, boolean canProduceUnits, int powerValue, double visionRange) {
        this.displayName = displayName;
        this.resourceCost = resourceCost;
        this.buildTimeSeconds = buildTimeSeconds;
        this.maxHealth = maxHealth;
        this.size = size;
        this.sides = sides;
        this.color = color;
        this.canProduceUnits = canProduceUnits;
        this.powerValue = powerValue;
        this.visionRange = visionRange;
    }

    /**
     * Check if this building is defensive (turret)
     */
    public boolean isDefensive() {
        return this == TURRET;
    }

    /**
     * Get the tech tier required to build this building
     */
    public int getRequiredTechTier() {
        return switch (this) {
            case HEADQUARTERS, REFINERY, BARRACKS, POWER_PLANT, BUNKER, WALL -> 1;
            case FACTORY, RESEARCH_LAB, WEAPONS_DEPOT, TURRET, SHIELD_GENERATOR -> 2;
            case TECH_CENTER, ADVANCED_FACTORY, BANK, SANDSTORM_GENERATOR, ANDROID_FACTORY, PHOTON_SPIRE,
                 COMMAND_CITADEL -> 3;
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
        };
    }
}

