package com.fullsteam.model.factions;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the tech tree for a faction - what units and buildings are available.
 * This is the core of faction differentiation.
 */
@Getter
@Builder
public class FactionTechTree {

    /**
     * Units that this faction can build (derived from buildingProducers)
     */
    @Builder.Default
    private final Map<BuildingType, List<UnitType>> buildingsAndUnits = new LinkedHashMap<>();

    /**
     * Check if a unit is available to this faction (checks if any building produces it)
     */
    public boolean canBuildUnit(UnitType unitType) {
        return buildingsAndUnits.values()
                .stream()
                .anyMatch(units -> units.contains(unitType));
    }

    /**
     * Check if a building is available to this faction
     */
    public boolean canBuildBuilding(BuildingType buildingType) {
        return buildingType != BuildingType.HEADQUARTERS && buildingsAndUnits.containsKey(buildingType);
    }

    /**
     * Get which units a building can produce for this faction
     * SINGLE SOURCE OF TRUTH - direct lookup in buildingProducers
     */
    public List<UnitType> getUnitsProducedBy(BuildingType buildingType) {
        return buildingsAndUnits.getOrDefault(buildingType, List.of());
    }

    /**
     * Check if a building can produce a specific unit for this faction
     * SINGLE SOURCE OF TRUTH - direct lookup in buildingProducers
     */
    public boolean canBuildingProduceUnit(BuildingType buildingType, UnitType unitType) {
        return buildingsAndUnits.getOrDefault(buildingType, List.of()).contains(unitType);
    }
}
