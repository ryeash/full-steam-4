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
    
    // Crawler's deploy mode - immobile, +50% range/damage
    DEPLOY(
            "Deploy",
            true,  // toggle ability
            false, // doesn't require target
            2000   // 2 second cooldown
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
    
    // Stealth Tank's cloak
    STEALTH(
            "Stealth",
            true,  // toggle ability
            false, // doesn't require target
            3000   // 3 second cooldown
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

