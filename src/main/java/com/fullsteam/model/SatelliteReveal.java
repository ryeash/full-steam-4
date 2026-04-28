package com.fullsteam.model;

import org.dyn4j.geometry.Vector2;

/**
 * Temporary orbital vision used by {@link CommandAbilityType#SATELLITE_SWEEP}.
 * Merged into {@link FogOfWar} for the owning team until {@link #expiresAtEpochMs()}.
 */
public record SatelliteReveal(int teamNumber, Vector2 center, double radius, long expiresAtEpochMs) {

    public boolean isActive() {
        return System.currentTimeMillis() < expiresAtEpochMs;
    }
}
