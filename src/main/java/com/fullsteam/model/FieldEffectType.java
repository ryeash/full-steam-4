package com.fullsteam.model;

import lombok.Getter;

@Getter
public enum FieldEffectType {
    // Combat effects (existing)
    EXPLOSION(0.5, true),      // Short duration, instant damage
    FIRE(3.0, false),          // Long duration, damage over time
    ELECTRIC(1.0, false),      // Medium duration, chain damage
    FRAGMENTATION(0.3, true),  // Very short, creates multiple projectiles
    SANDSTORM(Double.MAX_VALUE, false); // Persistent until building destroyed, damage over time

    private final double defaultDuration;
    private final boolean instantaneous;

    FieldEffectType(double defaultDuration, boolean instantaneous) {
        this.defaultDuration = defaultDuration;
        this.instantaneous = instantaneous;
    }
}
