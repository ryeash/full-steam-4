package com.fullsteam.model;

import lombok.Getter;

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
            1000,    // max health
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
            500,     // max health
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
            600,     // max health
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
            400,     // max health
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
            700,     // max health
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
            600,     // max health
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
            650,     // max health
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
            800,     // max health
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
            900,     // max health
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
            400,     // max health
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
            400,     // max health
            25.0,    // size (radius)
            5,       // sides (pentagon)
            0xFF4500, // orange red
            false    // cannot produce units
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
            false    // cannot produce units
    ),
    
    // Economic building - generates passive income based on current credits (compound interest)
    BANK(
            "Bank",
            600,     // resource cost (expensive T3 building)
            30,      // build time (seconds)
            400,     // max health
            35.0,    // size (radius)
            8,       // sides (octagon)
            0xFFD700, // gold
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

    BuildingType(String displayName, int resourceCost, int buildTimeSeconds, double maxHealth,
                 double size, int sides, int color, boolean canProduceUnits) {
        this.displayName = displayName;
        this.resourceCost = resourceCost;
        this.buildTimeSeconds = buildTimeSeconds;
        this.maxHealth = maxHealth;
        this.size = size;
        this.sides = sides;
        this.color = color;
        this.canProduceUnits = canProduceUnits;
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
        return switch (this) {
            case HEADQUARTERS -> new UnitType[]{UnitType.WORKER, UnitType.MINER};
            case BARRACKS -> new UnitType[]{UnitType.INFANTRY, UnitType.LASER_INFANTRY, UnitType.MEDIC};
            case FACTORY -> new UnitType[]{UnitType.JEEP, UnitType.TANK};
            case WEAPONS_DEPOT -> new UnitType[]{UnitType.ROCKET_SOLDIER, UnitType.SNIPER, UnitType.ENGINEER};
            case ADVANCED_FACTORY -> new UnitType[]{UnitType.ARTILLERY, UnitType.GIGANTONAUT, UnitType.CRAWLER,
                    UnitType.STEALTH_TANK, UnitType.MAMMOTH_TANK};
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
            case HEADQUARTERS, REFINERY, BARRACKS, POWER_PLANT, WALL -> 1;
            case FACTORY, RESEARCH_LAB, WEAPONS_DEPOT, TURRET, SHIELD_GENERATOR -> 2;
            case TECH_CENTER, ADVANCED_FACTORY, BANK -> 3;
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
        };
    }
}

