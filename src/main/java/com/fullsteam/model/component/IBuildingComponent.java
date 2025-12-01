package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.research.ResearchModifier;

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

    void init(GameEntities gameEntities, Building building);

    /**
     * Update this component's state.
     * Called every game tick from the building's update method.
     *
     * @param hasLowPower Whether the building is suffering from low power
     */
    void update(boolean hasLowPower);

    /**
     * Called when the building completes construction.
     * Components can use this to initialize or activate features.
     */
    default void onConstructionComplete() {
        // Default: do nothing
    }

    /**
     * Called when the building is destroyed.
     * Components can use this to clean up resources or trigger effects.
     */
    default void onDestroy() {
        // Default: do nothing
    }

    /**
     * Apply research modifiers for this building components.
     *
     * @param modifier the cumulative modifiers for the building owner
     */
    default void applyResearchModifiers(ResearchModifier modifier) {
        // TODO: ensure this is properly working for all components
    }
}

