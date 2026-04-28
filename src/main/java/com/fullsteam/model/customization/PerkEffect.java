package com.fullsteam.model.customization;

import com.fullsteam.model.Building;
import com.fullsteam.model.Player;
import com.fullsteam.model.RTSGameManager;
import com.fullsteam.model.Targetable;
import com.fullsteam.model.Unit;
import com.fullsteam.model.factions.FactionDefinition;

/**
 * Represents the implementation of a faction perk.
 * Each perk can apply static modifiers to the faction definition
 * and/or provide runtime hooks for dynamic behavior.
 * <p>
 * All hook methods have default no-op implementations, so perks only need
 * to override the methods they actually use.
 */
public interface PerkEffect {

    /**
     * Get the perk this effect implements
     */
    FactionPerk getPerk();

    /**
     * Apply this perk's static modifiers to the faction definition during initialization.
     * This is called once when the custom faction is built.
     *
     * @param builder The faction definition builder to modify
     * @param config  The custom faction configuration
     */
    void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config);

    /**
     * Called when a unit is created for this faction
     */
    default void onUnitCreated(Unit unit, Player faction, RTSGameManager game) {
    }

    /**
     * Called when a unit belonging to this faction is destroyed
     */
    default void onUnitDestroyed(Unit unit, Player faction, RTSGameManager game) {
    }

    /**
     * Called when a building is created for this faction
     */
    default void onBuildingCreated(Building building, Player faction, RTSGameManager game) {
    }

    /**
     * Called when a building belonging to this faction is destroyed
     */
    default void onBuildingDestroyed(Building building, Player faction, RTSGameManager game) {
    }

    /**
     * Called when a unit belonging to this faction deals damage
     */
    default void onUnitDealsDamage(Unit attacker, Targetable target, double damage, Player faction, RTSGameManager game) {
    }
}
