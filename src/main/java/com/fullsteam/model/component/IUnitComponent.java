package com.fullsteam.model.component;

import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Unit;

/**
 * Interface for unit components.
 * Components encapsulate specific behaviors that can be attached to units.
 * This enables composition over inheritance for unit functionality.
 * <p>
 * Examples:
 * - HarvestComponent: Handle resource gathering (Workers)
 * - CloakComponent: Handle stealth mechanics (Cloak Tank)
 * - HealComponent: Handle healing abilities (Medic)
 * - RepairComponent: Handle repair abilities (Engineer)
 */
public interface IUnitComponent {

    /**
     * Initialize this component with the parent unit.
     * Called when component is added to a unit.
     *
     * @param unit         The unit this component belongs to
     * @param gameEntities Reference to all game entities
     */
    void init(Unit unit, GameEntities gameEntities);

    /**
     * Update this component's state.
     * Called every game tick from the unit's update method.
     *
     * @param gameEntities Reference to all game entities (for querying nearby entities, etc.)
     */
    void update(GameEntities gameEntities);

    /**
     * Called when the unit is destroyed.
     * Components can use this to clean up resources or trigger effects.
     */
    default void onDestroy() {
        // Default: do nothing
    }
}
