package com.fullsteam.ai;

import com.fullsteam.model.AiDifficulty;

/**
 * Tunable RTS AI parameters for one opponent. Derived from {@link AiDifficulty} — no hidden cheats,
 * only reaction speed, thresholds, and risk tolerance.
 * <p>
 * {@code maxWorkers} caps total workers (on field plus HQ production pipeline) so production does not
 * flood workers when barracks infantry is available.
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
        int squadAttackPeriod,
        int squadAttackSalt,
        double squadMaxDefenseForAttack,
        double squadMinArmyConcern,
        int squadMinCombatUnits,
        int maxWorkers
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

    /** Slower reactions, higher action thresholds, smaller base defense radius. */
    private static SkirmishAiProfile easy() {
        return new SkirmishAiProfile(
                600,
                0.28,
                280,
                650,
                520,
                0.50,
                2,
                5,
                20,
                0,
                0.34,
                32,
                7,
                72,
                2,
                0.42,
                96,
                5,
                0.48,
                48,
                3,
                0.28,
                60,
                1,
                96,
                11,
                0.28,
                0.30,
                5,
                8
        );
    }

    /** Baseline matching pre-profile tuning. */
    private static SkirmishAiProfile normal() {
        return new SkirmishAiProfile(
                760,
                0.35,
                280,
                650,
                520,
                0.55,
                4,
                9,
                12,
                0,
                0.22,
                22,
                7,
                54,
                2,
                0.32,
                72,
                5,
                0.38,
                36,
                3,
                0.18,
                48,
                1,
                72,
                11,
                0.38,
                0.20,
                3,
                10
        );
    }

    /** Faster ticks, tighter thresholds, larger defense awareness, more aggressive attacks. */
    private static SkirmishAiProfile hard() {
        return new SkirmishAiProfile(
                920,
                0.42,
                280,
                650,
                520,
                0.58,
                6,
                11,
                8,
                0,
                0.16,
                16,
                7,
                40,
                2,
                0.24,
                54,
                5,
                0.30,
                28,
                3,
                0.12,
                40,
                1,
                54,
                11,
                0.48,
                0.14,
                2,
                12
        );
    }
}
