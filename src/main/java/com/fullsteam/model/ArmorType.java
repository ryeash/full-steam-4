package com.fullsteam.model;

import lombok.Getter;

/**
 * Armor classification for units and buildings, used in the damage matrix.
 *
 * <pre>
 * Damage matrix – multiplier applied to incoming damage:
 *
 *               BALLISTIC  EXPLOSIVE  ENERGY  SIEGE
 * UNARMORED       1.00       1.25      1.00   0.75
 * LIGHT           1.00       1.10      0.90   0.75
 * MEDIUM          0.85       1.00      0.85   1.00
 * HEAVY           0.65       0.90      0.85   1.25
 * FORTIFIED       0.50       0.75      0.75   1.50
 * </pre>
 *
 * Energy shields (perk-based) apply a separate multiplier before armor:
 *   ENERGY  → 1.5×  (shields are weak to energy)
 *   BALLISTIC / EXPLOSIVE → 0.8×  (shields absorb kinetic/blast well)
 *   SIEGE   → 1.0×
 */
@Getter
public enum ArmorType {
    UNARMORED("Unarmored"),
    LIGHT("Light"),
    MEDIUM("Medium"),
    HEAVY("Heavy"),
    FORTIFIED("Fortified");

    private final String displayName;

    // [armorOrdinal][damageOrdinal] → multiplier
    // Rows: UNARMORED, LIGHT, MEDIUM, HEAVY, FORTIFIED
    // Cols: BALLISTIC, EXPLOSIVE, ENERGY, SIEGE
    private static final double[][] MATRIX = {
            { 1.00, 1.25, 1.00, 0.75 }, // UNARMORED
            { 1.00, 1.10, 0.90, 0.75 }, // LIGHT
            { 0.85, 1.00, 0.85, 1.00 }, // MEDIUM
            { 0.65, 0.90, 0.85, 1.25 }, // HEAVY
            { 0.50, 0.75, 0.75, 1.50 }, // FORTIFIED
    };

    /** Multiplier applied to incoming damage for this armor vs a given damage type. */
    public double multiplierFor(DamageType damageType) {
        return MATRIX[this.ordinal()][damageType.ordinal()];
    }

    ArmorType(String displayName) {
        this.displayName = displayName;
    }
}
