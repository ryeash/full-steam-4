package com.fullsteam.model.factions;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import lombok.Builder;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

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
    private final Set<UnitType> availableUnits = new HashSet<>();
    
    /**
     * Buildings that this faction can build
     */
    @Builder.Default
    private final Set<BuildingType> availableBuildings = new HashSet<>();
    
    /**
     * SINGLE SOURCE OF TRUTH: Which units can be produced by which buildings
     * Maps: BuildingType -> List of units it can produce
     * This is the authoritative mapping for all faction production capabilities
     */
    private final Map<BuildingType, List<UnitType>> buildingProducers;
    
    /**
     * Tech tier requirements for buildings (can override defaults per faction)
     */
    @Builder.Default
    private final Map<BuildingType, Integer> buildingTechTiers = new HashMap<>();
    
    /**
     * Check if a unit is available to this faction (checks if any building produces it)
     */
    public boolean canBuildUnit(UnitType unitType) {
        return buildingProducers.values().stream()
                .anyMatch(units -> units.contains(unitType));
    }
    
    /**
     * Check if a building is available to this faction
     */
    public boolean canBuildBuilding(BuildingType buildingType) {
        return availableBuildings.contains(buildingType);
    }
    
    /**
     * Get which units a building can produce for this faction
     * SINGLE SOURCE OF TRUTH - direct lookup in buildingProducers
     */
    public List<UnitType> getUnitsProducedBy(BuildingType buildingType) {
        return buildingProducers.getOrDefault(buildingType, Collections.emptyList());
    }
    
    /**
     * Check if a building can produce a specific unit for this faction
     * SINGLE SOURCE OF TRUTH - direct lookup in buildingProducers
     */
    public boolean canBuildingProduceUnit(BuildingType buildingType, UnitType unitType) {
        List<UnitType> producibleUnits = buildingProducers.get(buildingType);
        return producibleUnits != null && producibleUnits.contains(unitType);
    }
}
