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
}

