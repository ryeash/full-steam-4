package com.fullsteam.ai;

import com.fullsteam.model.AiDifficulty;

/**
 * Tunable RTS AI parameters for one opponent. Derived from {@link AiDifficulty} — no hidden cheats,
 * only reaction speed, thresholds, and risk tolerance.
 * <p>
 * {@code maxWorkers} caps total workers (on field plus HQ production pipeline) so production does not
 * flood workers when barracks infantry is available.
 * <p>
 * {@code reservedBuildWorkers} is how many idle harvest-capable units nearest the base stay off bulk harvest
 * orders so placement behaviors can retask them without everyone rushing to the ore patch.
 */
public record SkirmishAiProfile(
        double defenseRadius,
        double defenseConcernPerEnemy,
        int creditsLow,
        int creditsMid,
        int creditsHarvesterBoostBelow,
        double economyHarvesterBoostMinConcern,
        int armyIdleLowCount,
        int armyIdleMidCount,
        int defendPeriod,
        int defendPhaseSalt,
        double defendMinConcern,
        int resumeConstructionPeriod,
        int resumeConstructionSalt,
        int placePowerPlantPeriod,
        int placePowerPlantSalt,
        double placePowerMinConcern,
        int placeRefineryPeriod,
        int placeRefinerySalt,
        double placeRefineryMinEconomyConcern,
        int harvestPeriod,
        int harvestSalt,
        double harvestMinEconomyConcern,
        int producePeriod,
        int produceSalt,
        int assaultPeriod,
        int assaultSalt,
        int assaultMinForce,
        int maxWorkers,
        int reservedBuildWorkers
) {

    public static SkirmishAiProfile forDifficulty(AiDifficulty difficulty) {
        if (difficulty == null) {
            difficulty = AiDifficulty.NORMAL;
        }
        return switch (difficulty) {
            case EASY -> easy();
            case NORMAL -> normal();
            case HARD -> hard();
        };
    }

    /**
     * Slower reactions, higher action thresholds, smaller base defense radius.
     */
    private static SkirmishAiProfile easy() {
        return new SkirmishAiProfile(
                600,   // defenseRadius
                0.28,  // defenseConcernPerEnemy
                280,   // creditsLow
                650,   // creditsMid
                520,   // creditsHarvesterBoostBelow
                0.50,  // economyHarvesterBoostMinConcern
                2,     // armyIdleLowCount
                5,     // armyIdleMidCount
                20,    // defendPeriod
                0,     // defendPhaseSalt
                0.34,  // defendMinConcern
                32,    // resumeConstructionPeriod
                7,     // resumeConstructionSalt
                72,    // placePowerPlantPeriod
                2,     // placePowerPlantSalt
                0.42,  // placePowerMinConcern
                96,    // placeRefineryPeriod
                5,     // placeRefinerySalt
                0.48,  // placeRefineryMinEconomyConcern
                48,    // harvestPeriod
                3,     // harvestSalt
                0.28,  // harvestMinEconomyConcern
                60,    // producePeriod
                1,     // produceSalt
                240,   // assaultPeriod  (~12 s at 20 fps)
                3,     // assaultSalt
                7,     // assaultMinForce
                8,     // maxWorkers
                1      // reservedBuildWorkers
        );
    }

    /**
     * Baseline matching pre-profile tuning.
     */
    private static SkirmishAiProfile normal() {
        return new SkirmishAiProfile(
                760,   // defenseRadius
                0.35,  // defenseConcernPerEnemy
                280,   // creditsLow
                650,   // creditsMid
                520,   // creditsHarvesterBoostBelow
                0.55,  // economyHarvesterBoostMinConcern
                4,     // armyIdleLowCount
                9,     // armyIdleMidCount
                12,    // defendPeriod
                0,     // defendPhaseSalt
                0.22,  // defendMinConcern
                22,    // resumeConstructionPeriod
                7,     // resumeConstructionSalt
                54,    // placePowerPlantPeriod
                2,     // placePowerPlantSalt
                0.32,  // placePowerMinConcern
                72,    // placeRefineryPeriod
                5,     // placeRefinerySalt
                0.38,  // placeRefineryMinEconomyConcern
                36,    // harvestPeriod
                3,     // harvestSalt
                0.18,  // harvestMinEconomyConcern
                48,    // producePeriod
                1,     // produceSalt
                160,   // assaultPeriod  (~8 s at 20 fps)
                5,     // assaultSalt
                5,     // assaultMinForce
                10,    // maxWorkers
                2      // reservedBuildWorkers
        );
    }

    /**
     * Faster ticks, tighter thresholds, larger defense awareness, more aggressive attacks.
     */
    private static SkirmishAiProfile hard() {
        return new SkirmishAiProfile(
                920,   // defenseRadius
                0.42,  // defenseConcernPerEnemy
                280,   // creditsLow
                650,   // creditsMid
                520,   // creditsHarvesterBoostBelow
                0.58,  // economyHarvesterBoostMinConcern
                6,     // armyIdleLowCount
                11,    // armyIdleMidCount
                8,     // defendPeriod
                0,     // defendPhaseSalt
                0.16,  // defendMinConcern
                16,    // resumeConstructionPeriod
                7,     // resumeConstructionSalt
                40,    // placePowerPlantPeriod
                2,     // placePowerPlantSalt
                0.24,  // placePowerMinConcern
                54,    // placeRefineryPeriod
                5,     // placeRefinerySalt
                0.30,  // placeRefineryMinEconomyConcern
                28,    // harvestPeriod
                3,     // harvestSalt
                0.12,  // harvestMinEconomyConcern
                40,    // producePeriod
                1,     // produceSalt
                100,   // assaultPeriod  (~5 s at 20 fps)
                7,     // assaultSalt
                4,     // assaultMinForce
                12,    // maxWorkers
                2      // reservedBuildWorkers
        );
    }
}
