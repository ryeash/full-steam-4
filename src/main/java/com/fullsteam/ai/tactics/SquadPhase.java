package com.fullsteam.ai.tactics;

/**
 * Phase state for one AI faction's combat squad.
 * Transitions are driven by {@link AiTacticsDirector}.
 */
public enum SquadPhase {
    /**
     * Idle units rally to a forward staging point near the base.
     */
    STAGING,
    /**
     * Staged force has hit the threshold; all available units push toward the attack target.
     */
    ASSAULTING,
    /**
     * Enemies detected near the base; units defend until the threat clears.
     */
    DEFENDING
}
