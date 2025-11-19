package com.fullsteam.model.factions;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import lombok.Builder;
import lombok.Getter;

import java.util.*;

/**
 * Defines the tech tree for a faction - what units and buildings are available.
 * This is the core of faction differentiation.
 */
@Getter
@Builder
public class FactionTechTree {
    
    /**
     * Units that this faction can build
     */
    @Builder.Default
    private final Set<UnitType> availableUnits = new HashSet<>();
    
    /**
     * Buildings that this faction can build
     */
    @Builder.Default
    private final Set<BuildingType> availableBuildings = new HashSet<>();
    
    /**
     * Which buildings can produce which units (faction-specific)
     * Allows factions to have different production buildings for same units
     */
    @Builder.Default
    private final Map<UnitType, List<BuildingType>> unitProducers = new HashMap<>();
    
    /**
     * Tech tier requirements for buildings (can override defaults per faction)
     */
    @Builder.Default
    private final Map<BuildingType, Integer> buildingTechTiers = new HashMap<>();
    
    /**
     * Check if a unit is available to this faction
     */
    public boolean canBuildUnit(UnitType unitType) {
        return availableUnits.contains(unitType);
    }
    
    /**
     * Check if a building is available to this faction
     */
    public boolean canBuildBuilding(BuildingType buildingType) {
        return availableBuildings.contains(buildingType);
    }
    
    /**
     * Get which buildings can produce a specific unit for this faction
     */
    public List<BuildingType> getProducersFor(UnitType unitType) {
        return unitProducers.getOrDefault(unitType, Collections.emptyList());
    }
    
    /**
     * Get the tech tier required for a building (faction-specific override)
     */
    public int getTechTierFor(BuildingType buildingType) {
        return buildingTechTiers.getOrDefault(buildingType, buildingType.getRequiredTechTier());
    }
    
    /**
     * Check if a building can produce a specific unit
     */
    public boolean canBuildingProduceUnit(BuildingType buildingType, UnitType unitType) {
        List<BuildingType> producers = getProducersFor(unitType);
        return producers.contains(buildingType);
    }
}

