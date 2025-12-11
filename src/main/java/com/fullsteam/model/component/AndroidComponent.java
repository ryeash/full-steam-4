package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Component for Android units.
 * Tracks the Android Factory that produced this unit and handles factory-specific logic.
 * When the factory is destroyed, the android is also destroyed.
 */
@Slf4j
@Getter
@Setter
public class AndroidComponent extends AbstractUnitComponent {
    
    private Integer androidFactoryId = null;

    @Override
    public void update(com.fullsteam.model.GameEntities gameEntities) {
        // Check if parent factory still exists
        if (androidFactoryId != null) {
            Building factory = gameEntities.getBuildings().get(androidFactoryId);
            if (factory == null || !factory.isActive()) {
                // Factory destroyed - android shuts down
                log.info("Android {} shutting down - factory {} destroyed", unit.getId(), androidFactoryId);
                unit.setActive(false);
            }
        }
    }

    /**
     * Set the Android Factory that produced this android.
     *
     * @param factoryId The ID of the Android Factory
     */
    public void setAndroidFactory(int factoryId) {
        this.androidFactoryId = factoryId;
        log.debug("Android {} linked to factory {}", unit.getId(), factoryId);
    }

    /**
     * Check if this android has a valid factory.
     *
     * @return true if factory ID is set
     */
    public boolean hasFactory() {
        return androidFactoryId != null;
    }

    /**
     * Get the Android Factory building.
     *
     * @return The factory building, or null if not found/set
     */
    public Building getFactory() {
        if (androidFactoryId == null) {
            return null;
        }
        return gameEntities.getBuildings().get(androidFactoryId);
    }

    @Override
    public void onDestroy() {
        // Clear factory reference
        if (androidFactoryId != null) {
            log.debug("Android {} destroyed (was linked to factory {})", unit.getId(), androidFactoryId);
            androidFactoryId = null;
        }
    }
}


