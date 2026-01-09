package com.fullsteam.model;

/**
 * Wrapper for shield sensor body user data.
 * This allows us to distinguish shield sensors from regular building/unit bodies
 * in collision detection.
 * Can represent either a building shield (SHIELD_GENERATOR) or a unit shield (SHIELD_TANK).
 */
public record ShieldSensor(Targetable shieldOwner) {
    /**
     * Get the team number of the shield owner.
     */
    public int getTeamNumber() {
        return shieldOwner.getTeamNumber();
    }
}
