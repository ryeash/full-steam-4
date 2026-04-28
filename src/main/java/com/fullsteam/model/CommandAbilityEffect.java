package com.fullsteam.model;

/**
 * Runtime behavior for a {@link CommandAbilityType}, similar to {@link com.fullsteam.model.customization.PerkEffect}
 * for {@link com.fullsteam.model.customization.FactionPerk}.
 * <p>
 * Each enum constant implements this contract via an anonymous subclass on {@link CommandAbilityType}.
 */
@FunctionalInterface
public interface CommandAbilityEffect {

    /**
     * Execute this command after loadout, unlock-building, and cooldown checks in {@link RTSGameManager}.
     */
    CommandAbilityOutcome execute(CommandAbilityExecutionContext context);
}
