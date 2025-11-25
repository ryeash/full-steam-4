package com.fullsteam.model.research;

import com.fullsteam.model.BuildingType;
import lombok.Getter;

import java.util.Set;

/**
 * Defines all available research upgrades in the game.
 * Research provides permanent stat bonuses and unlocks for a player's faction.
 */
@Getter
public enum ResearchType {

    // ==================== COMBAT UPGRADES ====================

    // Projectile Damage (3 tiers)
    PROJECTILE_DAMAGE_1(
            "Improved Ballistics I",
            "Increases projectile weapon damage by 15%",
            ResearchCategory.COMBAT,
            500, 60,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setProjectileDamageMultiplier(1.15)
    ),
    PROJECTILE_DAMAGE_2(
            "Improved Ballistics II",
            "Increases projectile weapon damage by an additional 12%",
            ResearchCategory.COMBAT,
            800, 90,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setProjectileDamageMultiplier(1.12),
            PROJECTILE_DAMAGE_1
    ),
    PROJECTILE_DAMAGE_3(
            "Improved Ballistics III",
            "Increases projectile weapon damage by an additional 10%",
            ResearchCategory.COMBAT,
            1200, 120,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setProjectileDamageMultiplier(1.10),
            PROJECTILE_DAMAGE_2
    ),

    // Beam Damage (3 tiers)
    BEAM_DAMAGE_1(
            "Enhanced Beam Weapons I",
            "Increases beam weapon damage by 15%",
            ResearchCategory.COMBAT,
            500, 60,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setBeamDamageMultiplier(1.15)
    ),
    BEAM_DAMAGE_2(
            "Enhanced Beam Weapons II",
            "Increases beam weapon damage by an additional 12%",
            ResearchCategory.COMBAT,
            800, 90,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setBeamDamageMultiplier(1.12),
            BEAM_DAMAGE_1
    ),
    BEAM_DAMAGE_3(
            "Enhanced Beam Weapons III",
            "Increases beam weapon damage by an additional 10%",
            ResearchCategory.COMBAT,
            1200, 120,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setBeamDamageMultiplier(1.10),
            BEAM_DAMAGE_2
    ),

    // Attack Range (2 tiers)
    ATTACK_RANGE_1(
            "Extended Range I",
            "Increases all unit attack range by 20%",
            ResearchCategory.COMBAT,
            600, 75,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setAttackRangeMultiplier(1.20)
    ),
    ATTACK_RANGE_2(
            "Extended Range II",
            "Increases all unit attack range by an additional 15%",
            ResearchCategory.COMBAT,
            1000, 100,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setAttackRangeMultiplier(1.15),
            ATTACK_RANGE_1
    ),

    // Attack Rate (2 tiers)
    ATTACK_RATE_1(
            "Rapid Fire I",
            "Increases attack rate by 15%",
            ResearchCategory.COMBAT,
            550, 70,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setAttackRateMultiplier(1.15)
    ),
    ATTACK_RATE_2(
            "Rapid Fire II",
            "Increases attack rate by an additional 12%",
            ResearchCategory.COMBAT,
            900, 95,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setAttackRateMultiplier(1.12),
            ATTACK_RATE_1
    ),

    // ==================== DEFENSE UPGRADES ====================

    // Unit Health (3 tiers)
    UNIT_HEALTH_1(
            "Reinforced Armor I",
            "Increases all unit health by 20%",
            ResearchCategory.DEFENSE,
            600, 70,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setUnitHealthMultiplier(1.20)
    ),
    UNIT_HEALTH_2(
            "Reinforced Armor II",
            "Increases all unit health by an additional 15%",
            ResearchCategory.DEFENSE,
            950, 95,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setUnitHealthMultiplier(1.15),
            UNIT_HEALTH_1
    ),
    UNIT_HEALTH_3(
            "Reinforced Armor III",
            "Increases all unit health by an additional 12%",
            ResearchCategory.DEFENSE,
            1400, 125,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setUnitHealthMultiplier(1.12),
            UNIT_HEALTH_2
    ),

    // Building Health (2 tiers)
    BUILDING_HEALTH_1(
            "Structural Integrity I",
            "Increases all building health by 25%",
            ResearchCategory.DEFENSE,
            700, 80,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setBuildingHealthMultiplier(1.25)
    ),
    BUILDING_HEALTH_2(
            "Structural Integrity II",
            "Increases all building health by an additional 20%",
            ResearchCategory.DEFENSE,
            1100, 110,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setBuildingHealthMultiplier(1.20),
            BUILDING_HEALTH_1
    ),

    // ==================== MOBILITY UPGRADES ====================

    // Infantry Speed (2 tiers)
    INFANTRY_SPEED_1(
            "Advanced Training I",
            "Increases infantry movement speed by 20%",
            ResearchCategory.MOBILITY,
            400, 50,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setInfantrySpeedMultiplier(1.20)
    ),
    INFANTRY_SPEED_2(
            "Advanced Training II",
            "Increases infantry movement speed by an additional 15%",
            ResearchCategory.MOBILITY,
            700, 75,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setInfantrySpeedMultiplier(1.15),
            INFANTRY_SPEED_1
    ),

    // Vehicle Speed (2 tiers)
    VEHICLE_SPEED_1(
            "Enhanced Engines I",
            "Increases vehicle movement speed by 25%",
            ResearchCategory.MOBILITY,
            500, 60,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setVehicleSpeedMultiplier(1.25)
    ),
    VEHICLE_SPEED_2(
            "Enhanced Engines II",
            "Increases vehicle movement speed by an additional 20%",
            ResearchCategory.MOBILITY,
            850, 85,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setVehicleSpeedMultiplier(1.20),
            VEHICLE_SPEED_1
    ),

    // ==================== ECONOMY UPGRADES ====================

    // Worker Carry Capacity (3 tiers)
    WORKER_CAPACITY_1(
            "Advanced Mining Equipment I",
            "Increases worker carry capacity by 50 resources",
            ResearchCategory.ECONOMY,
            300, 45,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setWorkerCapacityBonus(50)
    ),
    WORKER_CAPACITY_2(
            "Advanced Mining Equipment II",
            "Increases worker carry capacity by an additional 40 resources",
            ResearchCategory.ECONOMY,
            550, 65,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setWorkerCapacityBonus(40),
            WORKER_CAPACITY_1
    ),
    WORKER_CAPACITY_3(
            "Advanced Mining Equipment III",
            "Increases worker carry capacity by an additional 35 resources",
            ResearchCategory.ECONOMY,
            850, 90,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setWorkerCapacityBonus(35),
            WORKER_CAPACITY_2
    ),

    // Harvesting Speed (2 tiers)
    HARVESTING_SPEED_1(
            "Efficient Extraction I",
            "Increases resource harvesting speed by 30%",
            ResearchCategory.ECONOMY,
            350, 50,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setHarvestingSpeedMultiplier(1.30)
    ),
    HARVESTING_SPEED_2(
            "Efficient Extraction II",
            "Increases resource harvesting speed by an additional 25%",
            ResearchCategory.ECONOMY,
            650, 75,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setHarvestingSpeedMultiplier(1.25),
            HARVESTING_SPEED_1
    ),

    // Production Speed (2 tiers)
    PRODUCTION_SPEED_1(
            "Streamlined Production I",
            "Reduces unit build time by 20%",
            ResearchCategory.ECONOMY,
            450, 60,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setProductionSpeedMultiplier(0.80) // 0.8 = 20% faster
    ),
    PRODUCTION_SPEED_2(
            "Streamlined Production II",
            "Reduces unit build time by an additional 15%",
            ResearchCategory.ECONOMY,
            750, 85,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setProductionSpeedMultiplier(0.85), // 0.85 = 15% faster
            PRODUCTION_SPEED_1
    ),

    // Construction Speed (2 tiers)
    CONSTRUCTION_SPEED_1(
            "Rapid Construction I",
            "Reduces building construction time by 25%",
            ResearchCategory.ECONOMY,
            400, 55,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setConstructionSpeedMultiplier(0.75) // 0.75 = 25% faster
    ),
    CONSTRUCTION_SPEED_2(
            "Rapid Construction II",
            "Reduces building construction time by an additional 20%",
            ResearchCategory.ECONOMY,
            700, 80,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setConstructionSpeedMultiplier(0.80), // 0.80 = 20% faster
            CONSTRUCTION_SPEED_1
    ),

    // ==================== SPECIAL ABILITIES ====================

    // Healing Enhancement (2 tiers)
    HEALING_POWER_1(
            "Advanced Medical Training I",
            "Increases medic healing rate by 50%",
            ResearchCategory.SPECIAL,
            400, 50,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setHealingPowerMultiplier(1.50)
    ),
    HEALING_POWER_2(
            "Advanced Medical Training II",
            "Increases medic healing rate by an additional 40%",
            ResearchCategory.SPECIAL,
            700, 75,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setHealingPowerMultiplier(1.40),
            HEALING_POWER_1
    ),

    // Repair Enhancement (2 tiers)
    REPAIR_POWER_1(
            "Advanced Engineering I",
            "Increases engineer repair rate by 50%",
            ResearchCategory.SPECIAL,
            400, 50,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setRepairPowerMultiplier(1.50)
    ),
    REPAIR_POWER_2(
            "Advanced Engineering II",
            "Increases engineer repair rate by an additional 40%",
            ResearchCategory.SPECIAL,
            700, 75,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setRepairPowerMultiplier(1.40),
            REPAIR_POWER_1
    ),

    // Vision Range (2 tiers)
    VISION_RANGE_1(
            "Advanced Sensors I",
            "Increases all unit vision range by 30%",
            ResearchCategory.SPECIAL,
            350, 45,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier().setVisionRangeMultiplier(1.30)
    ),
    VISION_RANGE_2(
            "Advanced Sensors II",
            "Increases all unit vision range by an additional 25%",
            ResearchCategory.SPECIAL,
            650, 70,
            BuildingType.TECH_CENTER,
            new ResearchModifier().setVisionRangeMultiplier(1.25),
            VISION_RANGE_1
    ),
    
    // Parallel Research (2 tiers) - Increases simultaneous research capacity
    PARALLEL_RESEARCH_1(
            "Parallel Research I",
            "Allows 2 simultaneous research projects (+1)",
            ResearchCategory.SPECIAL,
            400, 50,
            BuildingType.RESEARCH_LAB,
            new ResearchModifier() // No stat modifiers, just unlocks capacity
    ),
    PARALLEL_RESEARCH_2(
            "Parallel Research II",
            "Allows 3 simultaneous research projects (+1 more)",
            ResearchCategory.SPECIAL,
            800, 80,
            BuildingType.TECH_CENTER,
            new ResearchModifier(), // No stat modifiers, just unlocks capacity
            PARALLEL_RESEARCH_1
    );

    // ==================== ENUM FIELDS ====================

    private final String displayName;
    private final String description;
    private final ResearchCategory category;
    private final int creditCost;
    private final int researchTimeSeconds;
    private final BuildingType requiredBuilding;
    private final ResearchModifier modifier;
    private final Set<ResearchType> prerequisites;

    /**
     * Constructor for research with no prerequisites
     */
    ResearchType(String displayName, String description, ResearchCategory category,
                 int creditCost, int researchTimeSeconds, BuildingType requiredBuilding,
                 ResearchModifier modifier) {
        this(displayName, description, category, creditCost, researchTimeSeconds, requiredBuilding, modifier, new ResearchType[0]);
    }

    /**
     * Constructor for research with prerequisites
     */
    ResearchType(String displayName, String description, ResearchCategory category,
                 int creditCost, int researchTimeSeconds, BuildingType requiredBuilding,
                 ResearchModifier modifier, ResearchType... prerequisites) {
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.creditCost = creditCost;
        this.researchTimeSeconds = researchTimeSeconds;
        this.requiredBuilding = requiredBuilding;
        this.modifier = modifier;
        this.prerequisites = Set.of(prerequisites);
    }

    /**
     * Check if this research has prerequisites
     */
    public boolean hasPrerequisites() {
        return !prerequisites.isEmpty();
    }
}

