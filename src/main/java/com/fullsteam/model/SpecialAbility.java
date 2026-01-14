package com.fullsteam.model;

import lombok.Getter;

/**
 * Special abilities that units can have.
 * Provides a generalized system for unit-specific behaviors.
 */
@Getter
public enum SpecialAbility {
    NONE(
            "None",
            false,
            false,
            0
    ),

    // Medic's heal ability
    HEAL(
            "Heal",
            false, // not a toggle
            true,  // requires target
            500    // 0.5 second cooldown
    ),

    // Engineer's repair ability
    REPAIR(
            "Repair",
            false, // not a toggle
            true,  // requires target
            500    // 0.5 second cooldown
    ),

    // Cloak Tank's cloaking device
    CLOAK(
            "Cloak",
            true,  // toggle ability
            false, // doesn't require target
            3000   // 3 second cooldown
    ),

    // Spider Mine's self-destruct (automatically triggers on enemy contact)
    SPIDER_MINE(
            "Self-Destruct",
            false, // not a toggle (automatic)
            false, // doesn't require target (proximity-based)
            0      // no cooldown (one-time use)
    );

    private final String displayName;
    private final boolean isToggle; // Can be turned on/off
    private final boolean requiresTarget; // Needs a target unit/building
    private final long cooldownMs; // Cooldown in milliseconds

    SpecialAbility(String displayName, boolean isToggle, boolean requiresTarget, long cooldownMs) {
        this.displayName = displayName;
        this.isToggle = isToggle;
        this.requiresTarget = requiresTarget;
        this.cooldownMs = cooldownMs;
    }
}

