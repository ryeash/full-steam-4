package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.GameEntities;

/**
 * Interface for building components.
 * Components encapsulate specific behaviors that can be attached to buildings.
 * This enables composition over inheritance for building functionality.
 * <p>
 * Examples:
 * - ProductionComponent: Handle unit production queues
 * - DefenseComponent: Handle turret targeting and firing
 * - GarrisonComponent: Handle unit garrisoning
 * - ShieldComponent: Handle shield projection
 */
public interface IBuildingComponent {

    /**
     * Update this component's state.
     * Called every game tick from the building's update method.
     *
     * @param gameEntities The game entities for the game world
     * @param building     The building this component is attached to
     * @param hasLowPower  Whether the building is suffering from low power
     */
    void update(GameEntities gameEntities, Building building, boolean hasLowPower);

    /**
     * Called when the building completes construction.
     * Components can use this to initialize or activate features.
     *
     * @param building The building that completed construction
     */
    // TODO: take GameEntities
    default void onConstructionComplete(Building building) {
        // Default: do nothing
    }

    /**
     * Called when the building is destroyed.
     * Components can use this to clean up resources or trigger effects.
     *
     * @param building The building that was destroyed
     */
    // TODO: take GameEntities
    default void onDestroy(Building building) {
        // Default: do nothing
    }
}




