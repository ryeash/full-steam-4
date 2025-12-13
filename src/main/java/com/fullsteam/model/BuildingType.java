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
            5000,    // max health
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
            600,     // max health
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
            550,     // max health
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
            400,     // max health
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
            800,     // max health
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
            700,     // max health
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
            700,     // max health
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
            900,     // max health
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
            1000,     // max health
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
            500,     // max health
            15.0,    // size (radius) - small for tight placement
            4,       // sides (square)
            0x708090, // slate gray
            false,   // cannot produce units
            0,       // no power needed
            250.0    // vision range (limited, small defensive structure)
    ),

    // Defensive structure - attacks enemies with machine gun
    TURRET(
            "Turret",
            250,     // resource cost
            15,      // build time (seconds)
            500,     // max health
            25.0,    // size (radius)
            5,       // sides (pentagon)
            0xFF4500, // orange red
            false,   // cannot produce units
            -35,     // power consumption
            450.0    // vision range (excellent, needs to spot threats)
    ),

    // Defensive structure - fires rockets with explosive damage
    ROCKET_TURRET(
            "Rocket Turret",
            350,     // resource cost (more expensive than basic turret)
            20,      // build time (seconds)
            400,     // max health (lower than basic turret)
            25.0,    // size (radius)
            6,       // sides (hexagon)
            0xFF6347, // tomato red
            false,   // cannot produce units
            -50,     // power consumption (higher than basic)
            480.0    // vision range (excellent, long-range targeting)
    ),

    // Defensive structure - fires laser beams
    LASER_TURRET(
            "Laser Turret",
            400,     // resource cost (expensive advanced turret)
            25,      // build time (seconds)
            350,     // max health (lowest of turrets - glass cannon)
            25.0,    // size (radius)
            8,       // sides (octagon - advanced tech)
            0x00FFFF, // cyan (laser blue)
            false,   // cannot produce units
            -65,     // power consumption (highest - energy weapon)
            500.0    // vision range (best, advanced sensors)
    ),

    // Defensive structure - infantry can garrison inside and fire out
    BUNKER(
            "Bunker",
            250,     // resource cost - reduced to make it accessible as T1
            18,      // build time (seconds)
            1200,     // max health
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
            500,     // max health
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
            420,     // max health
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
            60,      // build time (seconds)
            800,     // max health
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
            90,      // build time (seconds)
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
            60,      // build time (seconds)
            800,    // max health
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
            80,      // build time (seconds)
            1000,    // max health
            55.0,    // size (radius) - large and imposing
            8,       // sides (octagon)
            0x4169E1, // royal blue (command authority)
            false,   // cannot produce units
            -50,     // power consumption
            800.0    // vision range (HUGE, command center bonus)
    ),

    // Air unit production - requires Tech Center
    AIRFIELD(
            "Airfield",
            600,     // resource cost
            35,      // build time (seconds)
            700,     // max health
            60.0,    // size (radius) - large landing pad
            8,       // sides (octagon)
            0x708090, // slate gray (runway color)
            true,    // can produce units (air units!)
            -40,     // power consumption
            420.0    // vision range (good, airfield tower)
    ),
    
    // Aircraft housing - must be built near Airfield, houses sortie-based aircraft
    HANGAR(
            "Hangar",
            400,     // resource cost (cheaper than airfield, but requires one)
            25,      // build time (seconds)
            600,     // max health
            35.0,    // size (radius) - medium building
            4,       // sides (rectangle - hangar shape)
            0x4A5568, // dark blue-gray (hangar color)
            true,    // can produce units (produces one bomber per hangar)
            -20,     // power consumption
            350.0    // vision range (moderate)
    ),
    
    // Monument - Storm Wings faction - weather control tower
    TEMPEST_SPIRE(
            "Tempest Spire",
            700,     // resource cost (expensive monument)
            70,      // build time (seconds)
            850,     // max health
            45.0,    // size (radius)
            8,       // sides (octagon)
            0x4682B4, // steel blue (storm theme)
            false,   // cannot produce units
            -60,     // power consumption
            600.0    // vision range (excellent, weather tower)
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
        return this == TURRET || this == ROCKET_TURRET || this == LASER_TURRET;
    }
    
    /**
     * Check if this building requires proximity to another building type
     * @return the required building type, or null if no proximity requirement
     */
    public BuildingType getProximityRequirement() {
        return switch (this) {
            case HANGAR -> AIRFIELD;
            default -> null;
        };
    }
    
    /**
     * Get the required proximity range for buildings that need to be near another building
     * @return the maximum distance in pixels, or 0 if no proximity requirement
     */
    public double getProximityRange() {
        return switch (this) {
            case HANGAR -> 200.0; // Must be within 200 pixels of an Airfield
            default -> 0.0;
        };
    }
    
    /**
     * Get the number of support slots this building provides for dependent buildings
     * For example, an Airfield can support N Hangars
     * @return number of dependent buildings this can support, or 0 if none
     */
    public int getSupportCapacity() {
        return switch (this) {
            case AIRFIELD -> 4; // Each Airfield can support 4 Hangars
            default -> 0;
        };
    }

    /**
     * Get the tech tier required to build this building
     */
    public int getRequiredTechTier() {
        return switch (this) {
            case HEADQUARTERS, REFINERY, BARRACKS, POWER_PLANT, BUNKER, WALL -> 1;
            case FACTORY, RESEARCH_LAB, WEAPONS_DEPOT, TURRET, SHIELD_GENERATOR, ROCKET_TURRET -> 2;
            case TECH_CENTER, ADVANCED_FACTORY, BANK, SANDSTORM_GENERATOR, ANDROID_FACTORY, PHOTON_SPIRE,
                 COMMAND_CITADEL, LASER_TURRET, AIRFIELD, HANGAR, TEMPEST_SPIRE -> 3;
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

            // Rocket Turret - hexagonal rocket platform
            case ROCKET_TURRET -> List.of(Geometry.createPolygonalCircle(6, size));

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
            
            // Hangar - rectangular building with angled roof
            case HANGAR -> {
                // Main hangar body (wide rectangle)
                Convex body = Geometry.createRectangle(size * 1.8, size * 1.2);
                
                // Small entrance/door area (rectangle at front)
                Convex entrance = Geometry.createRectangle(size * 0.5, size * 0.3);
                entrance.translate(size * 0.65, 0);
                
                yield List.of(body, entrance);
            }
            
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
}

