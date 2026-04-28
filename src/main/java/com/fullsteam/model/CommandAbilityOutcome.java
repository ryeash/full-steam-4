package com.fullsteam.model;

/**
 * Result of {@link CommandAbilityEffect#execute(CommandAbilityExecutionContext)} so
 * {@link RTSGameManager} can apply cooldown / generic success messaging consistently.
 */
public enum CommandAbilityOutcome {
    /**
     * Validation failed or ability could not run; executor already sent any player warnings.
     */
    FAILED,
    /**
     * Executor performed the full flow including cooldown and custom events (e.g. nuke arm/launch).
     */
    FULLY_HANDLED,
    /**
     * Effect ran; manager should set cooldown and send the standard "deployed!" info event.
     */
    NEED_DEFAULT_WRAP_UP,
    /**
     * Effect ran and custom info was already sent; manager should only set cooldown.
     */
    NEED_COOLDOWN_ONLY
}
