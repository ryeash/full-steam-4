package com.fullsteam.model;

import org.dyn4j.geometry.Vector2;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Server-side fog of war system.
 * Filters units and buildings based on vision radius to prevent cheating.
 * Map terrain (obstacles, resources) is always visible.
 */
public class FogOfWar {
    
    /**
     * Filter units visible to a specific team.
     * Only returns units within vision range of the team's units/buildings.
     * 
     * @param allUnits All units in the game
     * @param allBuildings All buildings in the game
     * @param teamNumber The team to calculate vision for
     * @return List of units visible to this team
     */
    public static List<Unit> getVisibleUnits(
            Collection<Unit> allUnits,
            Collection<Building> allBuildings,
            int teamNumber) {
        
        // Get all vision sources for this team
        List<Vector2> visionSources = new ArrayList<>();
        
        // Add vision from friendly units
        for (Unit unit : allUnits) {
            if (unit.isActive() && unit.getTeamNumber() == teamNumber) {
                visionSources.add(unit.getPosition());
            }
        }
        
        // Add vision from friendly buildings (only if construction is complete)
        for (Building building : allBuildings) {
            if (building.isActive() && building.getTeamNumber() == teamNumber && !building.isUnderConstruction()) {
                visionSources.add(building.getPosition());
            }
        }
        
        // If no vision sources, return only own units
        if (visionSources.isEmpty()) {
            return allUnits.stream()
                    .filter(u -> u.isActive() && u.getTeamNumber() == teamNumber)
                    .collect(Collectors.toList());
        }
        
        // Filter units based on vision
        List<Unit> visibleUnits = new ArrayList<>();
        for (Unit unit : allUnits) {
            if (!unit.isActive()) {
                continue;
            }
            
            // Always show own units
            if (unit.getTeamNumber() == teamNumber) {
                visibleUnits.add(unit);
                continue;
            }
            
            // Check if enemy unit is within vision range of any vision source
            if (isPositionVisible(unit.getPosition(), visionSources, getVisionRadius(unit))) {
                visibleUnits.add(unit);
            }
        }
        
        return visibleUnits;
    }
    
    /**
     * Filter buildings visible to a specific team.
     * Only returns buildings within vision range of the team's units/buildings.
     * 
     * @param allUnits All units in the game (for vision sources)
     * @param allBuildings All buildings in the game
     * @param teamNumber The team to calculate vision for
     * @return List of buildings visible to this team
     */
    public static List<Building> getVisibleBuildings(
            Collection<Unit> allUnits,
            Collection<Building> allBuildings,
            int teamNumber) {
        
        // Get all vision sources for this team
        List<Vector2> visionSources = new ArrayList<>();
        
        // Add vision from friendly units
        for (Unit unit : allUnits) {
            if (unit.isActive() && unit.getTeamNumber() == teamNumber) {
                visionSources.add(unit.getPosition());
            }
        }
        
        // Add vision from friendly buildings (only if construction is complete)
        for (Building building : allBuildings) {
            if (building.isActive() && building.getTeamNumber() == teamNumber && !building.isUnderConstruction()) {
                visionSources.add(building.getPosition());
            }
        }
        
        // If no vision sources, return only own buildings
        if (visionSources.isEmpty()) {
            return allBuildings.stream()
                    .filter(b -> b.isActive() && b.getTeamNumber() == teamNumber)
                    .collect(Collectors.toList());
        }
        
        // Filter buildings based on vision
        List<Building> visibleBuildings = new ArrayList<>();
        for (Building building : allBuildings) {
            if (!building.isActive()) {
                continue;
            }
            
            // Always show own buildings
            if (building.getTeamNumber() == teamNumber) {
                visibleBuildings.add(building);
                continue;
            }
            
            // Check if enemy building is within vision range of any vision source
            if (isPositionVisible(building.getPosition(), visionSources, getVisionRadius(building))) {
                visibleBuildings.add(building);
            }
        }
        
        return visibleBuildings;
    }
    
    /**
     * Check if a position is visible from any vision source.
     * 
     * @param position Position to check
     * @param visionSources List of positions that provide vision
     * @param targetRadius Radius of the target (for edge detection)
     * @return true if position is visible
     */
    private static boolean isPositionVisible(
            Vector2 position,
            List<Vector2> visionSources,
            double targetRadius) {
        
        for (Vector2 source : visionSources) {
            double distance = source.distance(position);
            // Account for target size - if any part is visible, show it
            if (distance <= getVisionRange() + targetRadius) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get the vision range for units and buildings.
     * This is the radius around each unit/building where they can see.
     * 
     * @return Vision range in world units
     */
    public static double getVisionRange() {
        return 400.0; // Standard vision range
    }
    
    /**
     * Get vision radius for a specific unit (for edge detection).
     * Larger units are easier to spot.
     */
    private static double getVisionRadius(Unit unit) {
        return unit.getUnitType().getSize();
    }
    
    /**
     * Get vision radius for a specific building (for edge detection).
     * Larger buildings are easier to spot.
     */
    private static double getVisionRadius(Building building) {
        return building.getBuildingType().getSize();
    }
    
    /**
     * Check if a specific position would be visible to a team.
     * Useful for validating attack orders, etc.
     * 
     * @param position Position to check
     * @param allUnits All units in game
     * @param allBuildings All buildings in game
     * @param teamNumber Team to check vision for
     * @return true if position is visible to team
     */
    public static boolean isPositionVisibleToTeam(
            Vector2 position,
            Collection<Unit> allUnits,
            Collection<Building> allBuildings,
            int teamNumber) {
        
        // Get all vision sources for this team
        List<Vector2> visionSources = new ArrayList<>();
        
        for (Unit unit : allUnits) {
            if (unit.isActive() && unit.getTeamNumber() == teamNumber) {
                visionSources.add(unit.getPosition());
            }
        }
        
        // Add vision from friendly buildings (only if construction is complete)
        for (Building building : allBuildings) {
            if (building.isActive() && building.getTeamNumber() == teamNumber && !building.isUnderConstruction()) {
                visionSources.add(building.getPosition());
            }
        }
        
        return isPositionVisible(position, visionSources, 0);
    }
    
    /**
     * Get vision range for a specific unit type.
     * Some units might have extended vision (scouts, etc.)
     * 
     * @param unit The unit to get vision range for
     * @return Vision range for this unit
     */
    public static double getUnitVisionRange(Unit unit) {
        // Could be customized per unit type in the future
        // For now, all units have same vision
        return getVisionRange();
    }
    
    /**
     * Get vision range for a specific building type.
     * Some buildings might have extended vision (watchtowers, etc.)
     * 
     * @param building The building to get vision range for
     * @return Vision range for this building
     */
    public static double getBuildingVisionRange(Building building) {
        // Could be customized per building type in the future
        // Turrets might have extended vision, for example
        return getVisionRange();
    }
}


