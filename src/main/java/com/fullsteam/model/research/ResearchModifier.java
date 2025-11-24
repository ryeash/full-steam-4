package com.fullsteam.model.research;

import lombok.Data;

/**
 * Represents the stat modifiers applied by a research upgrade.
 * Modifiers are multiplicative (1.0 = no change, 1.15 = +15%, 0.8 = -20%).
 * Bonuses are additive (e.g., +50 worker capacity).
 */
@Data
public class ResearchModifier {
    
    // Combat modifiers
    private double projectileDamageMultiplier = 1.0;
    private double beamDamageMultiplier = 1.0;
    private double attackRangeMultiplier = 1.0;
    private double attackRateMultiplier = 1.0;
    
    // Defense modifiers
    private double unitHealthMultiplier = 1.0;
    private double buildingHealthMultiplier = 1.0;
    
    // Mobility modifiers
    private double infantrySpeedMultiplier = 1.0;
    private double vehicleSpeedMultiplier = 1.0;
    
    // Economy modifiers
    private double workerCapacityBonus = 0; // Additive bonus
    private double harvestingSpeedMultiplier = 1.0;
    private double productionSpeedMultiplier = 1.0; // Lower = faster (0.8 = 20% faster)
    private double constructionSpeedMultiplier = 1.0; // Lower = faster
    
    // Special ability modifiers
    private double healingPowerMultiplier = 1.0;
    private double repairPowerMultiplier = 1.0;
    private double visionRangeMultiplier = 1.0;
    
    // Fluent setters for method chaining
    public ResearchModifier setProjectileDamageMultiplier(double value) {
        this.projectileDamageMultiplier = value;
        return this;
    }
    
    public ResearchModifier setBeamDamageMultiplier(double value) {
        this.beamDamageMultiplier = value;
        return this;
    }
    
    public ResearchModifier setAttackRangeMultiplier(double value) {
        this.attackRangeMultiplier = value;
        return this;
    }
    
    public ResearchModifier setAttackRateMultiplier(double value) {
        this.attackRateMultiplier = value;
        return this;
    }
    
    public ResearchModifier setUnitHealthMultiplier(double value) {
        this.unitHealthMultiplier = value;
        return this;
    }
    
    public ResearchModifier setBuildingHealthMultiplier(double value) {
        this.buildingHealthMultiplier = value;
        return this;
    }
    
    public ResearchModifier setInfantrySpeedMultiplier(double value) {
        this.infantrySpeedMultiplier = value;
        return this;
    }
    
    public ResearchModifier setVehicleSpeedMultiplier(double value) {
        this.vehicleSpeedMultiplier = value;
        return this;
    }
    
    public ResearchModifier setWorkerCapacityBonus(double value) {
        this.workerCapacityBonus = value;
        return this;
    }
    
    public ResearchModifier setHarvestingSpeedMultiplier(double value) {
        this.harvestingSpeedMultiplier = value;
        return this;
    }
    
    public ResearchModifier setProductionSpeedMultiplier(double value) {
        this.productionSpeedMultiplier = value;
        return this;
    }
    
    public ResearchModifier setConstructionSpeedMultiplier(double value) {
        this.constructionSpeedMultiplier = value;
        return this;
    }
    
    public ResearchModifier setHealingPowerMultiplier(double value) {
        this.healingPowerMultiplier = value;
        return this;
    }
    
    public ResearchModifier setRepairPowerMultiplier(double value) {
        this.repairPowerMultiplier = value;
        return this;
    }
    
    public ResearchModifier setVisionRangeMultiplier(double value) {
        this.visionRangeMultiplier = value;
        return this;
    }
    
    /**
     * Combine this modifier with another (multiplicative stacking).
     * Used to apply multiple research upgrades together.
     */
    public ResearchModifier combine(ResearchModifier other) {
        ResearchModifier combined = new ResearchModifier();
        
        // Combat
        combined.projectileDamageMultiplier = this.projectileDamageMultiplier * other.projectileDamageMultiplier;
        combined.beamDamageMultiplier = this.beamDamageMultiplier * other.beamDamageMultiplier;
        combined.attackRangeMultiplier = this.attackRangeMultiplier * other.attackRangeMultiplier;
        combined.attackRateMultiplier = this.attackRateMultiplier * other.attackRateMultiplier;
        
        // Defense
        combined.unitHealthMultiplier = this.unitHealthMultiplier * other.unitHealthMultiplier;
        combined.buildingHealthMultiplier = this.buildingHealthMultiplier * other.buildingHealthMultiplier;
        
        // Mobility
        combined.infantrySpeedMultiplier = this.infantrySpeedMultiplier * other.infantrySpeedMultiplier;
        combined.vehicleSpeedMultiplier = this.vehicleSpeedMultiplier * other.vehicleSpeedMultiplier;
        
        // Economy (additive for capacity, multiplicative for speeds)
        combined.workerCapacityBonus = this.workerCapacityBonus + other.workerCapacityBonus;
        combined.harvestingSpeedMultiplier = this.harvestingSpeedMultiplier * other.harvestingSpeedMultiplier;
        combined.productionSpeedMultiplier = this.productionSpeedMultiplier * other.productionSpeedMultiplier;
        combined.constructionSpeedMultiplier = this.constructionSpeedMultiplier * other.constructionSpeedMultiplier;
        
        // Special
        combined.healingPowerMultiplier = this.healingPowerMultiplier * other.healingPowerMultiplier;
        combined.repairPowerMultiplier = this.repairPowerMultiplier * other.repairPowerMultiplier;
        combined.visionRangeMultiplier = this.visionRangeMultiplier * other.visionRangeMultiplier;
        
        return combined;
    }
    
    /**
     * Check if this modifier has any non-default values
     */
    public boolean hasModifiers() {
        return projectileDamageMultiplier != 1.0
                || beamDamageMultiplier != 1.0
                || attackRangeMultiplier != 1.0
                || attackRateMultiplier != 1.0
                || unitHealthMultiplier != 1.0
                || buildingHealthMultiplier != 1.0
                || infantrySpeedMultiplier != 1.0
                || vehicleSpeedMultiplier != 1.0
                || workerCapacityBonus != 0
                || harvestingSpeedMultiplier != 1.0
                || productionSpeedMultiplier != 1.0
                || constructionSpeedMultiplier != 1.0
                || healingPowerMultiplier != 1.0
                || repairPowerMultiplier != 1.0
                || visionRangeMultiplier != 1.0;
    }
}

