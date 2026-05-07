package com.fullsteam.model;

/**
 * Global rules for periodic army upkeep (upkeep tax) on credits.
 * Tune {@link #UPKEEP_FRACTION_OF_BUILD_COST} and {@link #UPKEEP_INTERVAL_MS} for balance.
 */
public final class ArmyEconomy {

    /**
     * How often players pay army upkeep (milliseconds).
     */
    public static final long UPKEEP_INTERVAL_MS = 30_000L;

    /**
     * Each unit's upkeep per interval = round({@link Player#getUnitCost(UnitType)} * this fraction).
     * Example: 4% of build cost every 30s — a 400-credit tank pays ~16 credits per tick before discounts.
     */
    public static final double UPKEEP_FRACTION_OF_BUILD_COST = 0.04;

    private ArmyEconomy() {
    }

    /**
     * Upkeep slice from an effective build cost (e.g. faction-adjusted). Workers are exempt (0).
     */
    public static int periodicUpkeepFromBuildCost(UnitType unitType, int buildCost) {
        if (unitType == UnitType.WORKER) {
            return 0;
        }
        return (int) Math.round(buildCost * UPKEEP_FRACTION_OF_BUILD_COST);
    }

    public static int periodicRentForUnit(Player faction, UnitType unitType) {
        return periodicUpkeepFromBuildCost(unitType, faction.getUnitCost(unitType));
    }
}
