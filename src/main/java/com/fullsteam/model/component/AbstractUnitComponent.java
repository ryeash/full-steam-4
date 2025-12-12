package com.fullsteam.model.component;

import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Unit;

/**
 * Abstract base class for unit components.
 * Provides common fields and initialization logic.
 */
public abstract class AbstractUnitComponent implements IUnitComponent {
    protected Unit unit;
    protected GameEntities gameEntities;

    @Override
    public void init(Unit unit, GameEntities gameEntities) {
        this.unit = unit;
        this.gameEntities = gameEntities;
    }

    /**
     * Helper method to get delta time from the game world.
     * Convenience method for components that need to track time-based operations.
     */
    protected double getDeltaTime() {
        return gameEntities.getWorld().getTimeStep().getDeltaTime();
    }
}




