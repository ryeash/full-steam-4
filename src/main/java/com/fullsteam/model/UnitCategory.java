package com.fullsteam.model;

import lombok.Getter;

/**
 * Categories for organizing units into production building types.
 * Each category maps to a specific production building.
 */
@Getter
public enum UnitCategory {
    WORKER("Worker", "Economy and construction units"),
    INFANTRY("Infantry", "Ground infantry units"),
    VEHICLE("Vehicle", "Ground vehicle units"),
    FLYER("Flyer", "Air units (VTOLs and fixed-wing)");

    private final String displayName;
    private final String description;

    UnitCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get the production building type that produces units of this category
     */
    public BuildingType getProductionBuilding() {
        return switch (this) {
            case WORKER -> BuildingType.HEADQUARTERS;
            case INFANTRY -> BuildingType.BARRACKS;
            case VEHICLE -> BuildingType.FACTORY;
            case FLYER -> null; // Special case: AIRFIELD or HANGAR
        };
    }

    /**
     * Check if a building can produce units of this category
     */
    public boolean isProducedBy(BuildingType buildingType) {
        return switch (this) {
            case WORKER -> buildingType == BuildingType.HEADQUARTERS;
            case INFANTRY -> buildingType == BuildingType.BARRACKS;
            case VEHICLE -> buildingType == BuildingType.FACTORY;
            case FLYER -> buildingType == BuildingType.AIRFIELD || buildingType == BuildingType.HANGAR;
        };
    }
}
