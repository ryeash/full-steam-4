package com.fullsteam.model;

import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Server-side fog of war system.
 * Filters units and buildings based on vision radius to prevent cheating.
 * Map terrain (obstacles, resources) is always visible.
 */
public class FogOfWar {

    /**
     * Helper class to store vision source position and range
     */
    private record VisionSource(Vector2 position, double visionRange) {
    }

    /**
     * Filter units visible to a specific team.
     * Only returns units within vision range of the team's units/buildings.
     *
     * @param gameEntities All the game entities
     * @param teamNumber   The team to calculate vision for
     * @return List of units visible to this team
     */
    public static List<Unit> getVisibleUnits(GameEntities gameEntities,
                                             int teamNumber) {
        Collection<Unit> allUnits = gameEntities.getUnits().values();
        Collection<Building> allBuildings = gameEntities.getBuildings().values();
        // Get all vision sources for this team with their specific vision ranges
        List<VisionSource> visionSources = new ArrayList<>();

        // Add vision from friendly units (use modified vision range from unit, not base type)
        for (Unit unit : allUnits) {
            if (unit.isActive() && unit.getTeamNumber() == teamNumber) {
                visionSources.add(new VisionSource(unit.getPosition(), unit.getVisionRange()));
            }
        }

        // Add vision from friendly buildings (only if construction is complete)
        for (Building building : allBuildings) {
            if (building.isActive() && building.getTeamNumber() == teamNumber && !building.isUnderConstruction()) {
                visionSources.add(new VisionSource(building.getPosition(), building.getBuildingType().getVisionRange()));
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

            // Check if enemy unit is cloaked
            if (unit.isCloaked()) {
                // Cloaked units require close detection range
                if (isPositionVisible(unit.getPosition(), visionSources, getVisionRadius(unit), Unit.getCloakDetectionRange())) {
                    visibleUnits.add(unit);
                }
                // Otherwise, cloaked unit remains invisible
            } else {
                // Normal visibility check for non-cloaked units
                if (isPositionVisible(unit.getPosition(), visionSources, getVisionRadius(unit))) {
                    visibleUnits.add(unit);
                }
            }
        }

        return visibleUnits;
    }

    /**
     * Filter buildings visible to a specific team.
     * Only returns buildings within vision range of the team's units/buildings.
     *
     * @param gameEntities All the game entities
     * @param teamNumber   The team to calculate vision for
     * @return List of buildings visible to this team
     */
    public static List<Building> getVisibleBuildings(GameEntities gameEntities, int teamNumber) {
        Collection<Unit> allUnits = gameEntities.getUnits().values();
        Collection<Building> allBuildings = gameEntities.getBuildings().values();

        // Get all vision sources for this team with their specific vision ranges
        List<VisionSource> visionSources = new ArrayList<>();

        // Add vision from friendly units (use modified vision range from unit, not base type)
        for (Unit unit : allUnits) {
            if (unit.isActive() && unit.getTeamNumber() == teamNumber) {
                visionSources.add(new VisionSource(unit.getPosition(), unit.getVisionRange()));
            }
        }

        // Add vision from friendly buildings (only if construction is complete)
        for (Building building : allBuildings) {
            if (building.isActive() && building.getTeamNumber() == teamNumber && !building.isUnderConstruction()) {
                visionSources.add(new VisionSource(building.getPosition(), building.getBuildingType().getVisionRange()));
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
     * @param position      Position to check
     * @param visionSources List of vision sources with their ranges
     * @param targetRadius  Radius of the target (for edge detection)
     * @return true if position is visible
     */
    private static boolean isPositionVisible(
            Vector2 position,
            List<VisionSource> visionSources,
            double targetRadius) {

        for (VisionSource source : visionSources) {
            double distance = source.position.distance(position);
            // Account for target size - if any part is visible, show it
            if (distance <= source.visionRange + targetRadius) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a position is visible from any vision source (with custom detection range for cloaked units).
     *
     * @param position       Position to check
     * @param visionSources  List of vision sources with their ranges
     * @param targetRadius   Radius of the target (for edge detection)
     * @param detectionRange Maximum range at which this target can be detected (for cloaked units)
     * @return true if position is visible
     */
    private static boolean isPositionVisible(
            Vector2 position,
            List<VisionSource> visionSources,
            double targetRadius,
            double detectionRange) {

        for (VisionSource source : visionSources) {
            double distance = source.position.distance(position);
            // For cloaked units, use the minimum of vision range and detection range
            double effectiveRange = Math.min(source.visionRange, detectionRange);
            // Account for target size - if any part is visible, show it
            if (distance <= effectiveRange + targetRadius) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get vision radius for a specific unit (for edge detection).
     * Larger units are easier to spot.
     */
    private static double getVisionRadius(Unit unit) {
        return unit.getUnitType().getSize();
    }

    /**
     * Filter wall segments visible to a specific team.
     * Only returns wall segments within vision range of the team's units/buildings.
     * Wall segments are player-built structures and should respect fog of war.
     *
     * @param gameEntities All the game entities
     * @param teamNumber   The team to calculate vision for
     * @return List of wall segments visible to this team
     */
    public static List<WallSegment> getVisibleWallSegments(GameEntities gameEntities, int teamNumber) {
        Collection<Unit> allUnits = gameEntities.getUnits().values();
        Collection<Building> allBuildings = gameEntities.getBuildings().values();
        Collection<WallSegment> allWallSegments = gameEntities.getWallSegments().values();

        // Get all vision sources for this team with their specific vision ranges
        List<VisionSource> visionSources = new ArrayList<>();

        // Add vision from friendly units (use modified vision range from unit, not base type)
        for (Unit unit : allUnits) {
            if (unit.isActive() && unit.getTeamNumber() == teamNumber) {
                visionSources.add(new VisionSource(unit.getPosition(), unit.getVisionRange()));
            }
        }

        // Add vision from friendly buildings (only if construction is complete)
        for (Building building : allBuildings) {
            if (building.isActive() && building.getTeamNumber() == teamNumber && !building.isUnderConstruction()) {
                visionSources.add(new VisionSource(building.getPosition(), building.getBuildingType().getVisionRange()));
            }
        }

        // If no vision sources, return only own wall segments
        if (visionSources.isEmpty()) {
            return allWallSegments.stream()
                    .filter(w -> w.isActive() && w.getTeamNumber() == teamNumber)
                    .collect(Collectors.toList());
        }

        // Filter wall segments based on vision
        List<WallSegment> visibleWallSegments = new ArrayList<>();
        for (WallSegment wallSegment : allWallSegments) {
            if (!wallSegment.isActive()) {
                continue;
            }

            // Always show own wall segments
            if (wallSegment.getTeamNumber() == teamNumber) {
                visibleWallSegments.add(wallSegment);
                continue;
            }

            // Check if enemy wall segment is within vision range of any vision source
            if (isPositionVisible(wallSegment.getPosition(), visionSources, getVisionRadius(wallSegment))) {
                visibleWallSegments.add(wallSegment);
            }
        }

        return visibleWallSegments;
    }

    /**
     * Get vision radius for a specific building (for edge detection).
     * Larger buildings are easier to spot.
     */
    private static double getVisionRadius(Building building) {
        return building.getBuildingType().getSize();
    }

    /**
     * Get vision radius for a specific wall segment (for edge detection).
     * Wall segments use their thickness as the vision radius.
     */
    private static double getVisionRadius(WallSegment wallSegment) {
        return wallSegment.getTargetSize(); // Uses the wall thickness (10.0)
    }
}


