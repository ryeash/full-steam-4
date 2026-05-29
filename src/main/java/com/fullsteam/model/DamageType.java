package com.fullsteam.model;

import lombok.Getter;

/**
 * Weapon damage classification used in the armor/damage matrix.
 *
 * BALLISTIC – bullets, standard kinetic rounds (default for most weapons)
 * EXPLOSIVE  – rockets, grenades, shells, AoE field effects
 * ENERGY     – laser and beam weapons
 * SIEGE      – heavy artillery; bonus vs FORTIFIED structures
 */
@Getter
public enum DamageType {
    BALLISTIC("Ballistic"),
    EXPLOSIVE("Explosive"),
    ENERGY("Energy"),
    SIEGE("Siege");

    private final String displayName;

    DamageType(String displayName) {
        this.displayName = displayName;
    }
}
