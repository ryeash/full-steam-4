package com.fullsteam.model.factions;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the tech tree for a faction - what buildings are available.
 * 
 * NOTE: Unit availability is now managed by the UnitTechTree and ResearchManager systems.
 * This class only manages BUILDING availability.
 * 
 * @deprecated for unit tech - use com.fullsteam.model.research.UnitTechTree instead
 */
@Getter
@Builder
public class FactionTechTree {

    /**
     * Buildings and their starter units (only used for initialization, not production validation)
     * Unit production validation now uses the ResearchManager system.
     */
    @Builder.Default
    private final Map<BuildingType, List<UnitType>> buildingsAndUnits = new LinkedHashMap<>();

    /**
     * Check if a unit is available to this faction (checks if any building produces it)
     * 
     * @deprecated Use ResearchManager.canProduceUnit() for research-based unit availability
     */
    @Deprecated
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
     * Get which starter units a building produces for this faction (initial units only)
     * 
     * @deprecated Use ResearchManager for actual unit production validation
     */
    @Deprecated
    public List<UnitType> getUnitsProducedBy(BuildingType buildingType) {
        return buildingsAndUnits.getOrDefault(buildingType, List.of());
    }

    /**
     * Check if a building can produce a specific unit for this faction
     * 
     * @deprecated Use ResearchManager.canProduceUnit() + UnitType.getProducedBy() instead
     */
    @Deprecated
    public boolean canBuildingProduceUnit(BuildingType buildingType, UnitType unitType) {
        return buildingsAndUnits.getOrDefault(buildingType, List.of()).contains(unitType);
    }
}
