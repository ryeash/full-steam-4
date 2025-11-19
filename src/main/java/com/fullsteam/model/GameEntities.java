package com.fullsteam.model;

import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * Container for all game entities and the physics world.
 * Provides convenient access to units, buildings, obstacles, etc.
 * Used by commands and AI to make intelligent decisions.
 */
@Getter
public class GameEntities {
    private final Map<Integer, Unit> units;
    private final Map<Integer, Building> buildings;
    private final Map<Integer, ResourceDeposit> resourceDeposits;
    private final Map<Integer, Obstacle> obstacles;
    private final Map<Integer, Projectile> projectiles;
    private final Map<Integer, FieldEffect> fieldEffects;
    private final Map<Integer, WallSegment> wallSegments;
    
    @Setter
    private World<Body> world; // The physics world (for raycasting, etc.)

    public GameEntities() {
        this.units = new ConcurrentSkipListMap<>();
        this.buildings = new ConcurrentSkipListMap<>();
        this.resourceDeposits = new ConcurrentSkipListMap<>();
        this.obstacles = new ConcurrentSkipListMap<>();
        this.projectiles = new ConcurrentSkipListMap<>();
        this.fieldEffects = new ConcurrentSkipListMap<>();
        this.wallSegments = new ConcurrentSkipListMap<>();
        this.world = null; // Set by RTSGameManager
    }

    /**
     * Get all active units as a list
     */
    public List<Unit> getAllUnits() {
        return new ArrayList<>(units.values());
    }

    /**
     * Get all active buildings as a list
     */
    public List<Building> getAllBuildings() {
        return new ArrayList<>(buildings.values());
    }

    /**
     * Get all active obstacles as a list
     */
    public List<Obstacle> getAllObstacles() {
        return new ArrayList<>(obstacles.values());
    }

    /**
     * Get all active resource deposits as a list
     */
    public List<ResourceDeposit> getAllResourceDeposits() {
        return new ArrayList<>(resourceDeposits.values());
    }

    /**
     * Get all active wall segments as a list
     */
    public List<WallSegment> getAllWallSegments() {
        return new ArrayList<>(wallSegments.values());
    }

    /**
     * Get enemy units for a given team
     */
    public List<Unit> getEnemyUnits(int teamNumber) {
        return units.values().stream()
                .filter(u -> u.getTeamNumber() != teamNumber && u.isActive())
                .collect(Collectors.toList());
    }

    /**
     * Get enemy buildings for a given team
     */
    public List<Building> getEnemyBuildings(int teamNumber) {
        return buildings.values().stream()
                .filter(b -> b.getTeamNumber() != teamNumber && b.isActive())
                .collect(Collectors.toList());
    }

    /**
     * Get friendly units for a given team
     */
    public List<Unit> getFriendlyUnits(int teamNumber) {
        return units.values().stream()
                .filter(u -> u.getTeamNumber() == teamNumber && u.isActive())
                .collect(Collectors.toList());
    }

    /**
     * Get friendly buildings for a given team
     */
    public List<Building> getFriendlyBuildings(int teamNumber) {
        return buildings.values().stream()
                .filter(b -> b.getTeamNumber() == teamNumber && b.isActive())
                .collect(Collectors.toList());
    }

    /**
     * Get nearby units within a certain range
     */
    public List<Unit> getNearbyUnits(org.dyn4j.geometry.Vector2 position, double range) {
        return units.values().stream()
                .filter(u -> u.isActive() && u.getPosition().distance(position) <= range)
                .collect(Collectors.toList());
    }

    /**
     * Get nearby enemy units within a certain range
     */
    public List<Unit> getNearbyEnemyUnits(org.dyn4j.geometry.Vector2 position, double range, int teamNumber) {
        return units.values().stream()
                .filter(u -> u.isActive() &&
                        u.getTeamNumber() != teamNumber &&
                        u.getPosition().distance(position) <= range)
                .collect(Collectors.toList());
    }

    /**
     * Get nearby enemy buildings within a certain range
     */
    public List<Building> getNearbyEnemyBuildings(org.dyn4j.geometry.Vector2 position, double range, int teamNumber) {
        return buildings.values().stream()
                .filter(b -> b.isActive() &&
                        b.getTeamNumber() != teamNumber &&
                        b.getPosition().distance(position) <= range)
                .collect(Collectors.toList());
    }

    /**
     * Find nearest enemy unit to a position
     */
    public Unit findNearestEnemyUnit(org.dyn4j.geometry.Vector2 position, int teamNumber, double maxRange) {
        return units.values().stream()
                .filter(u -> u.isActive() && u.getTeamNumber() != teamNumber)
                .filter(u -> u.getPosition().distance(position) <= maxRange)
                .min((u1, u2) -> Double.compare(
                        u1.getPosition().distance(position),
                        u2.getPosition().distance(position)))
                .orElse(null);
    }

    /**
     * Find nearest enemy building to a position
     */
    public Building findNearestEnemyBuilding(org.dyn4j.geometry.Vector2 position, int teamNumber, double maxRange) {
        return buildings.values().stream()
                .filter(b -> b.isActive() && b.getTeamNumber() != teamNumber)
                .filter(b -> b.getPosition().distance(position) <= maxRange)
                .min((b1, b2) -> Double.compare(
                        b1.getPosition().distance(position),
                        b2.getPosition().distance(position)))
                .orElse(null);
    }

    /**
     * Find nearest resource deposit to a position
     */
    public ResourceDeposit findNearestResourceDeposit(org.dyn4j.geometry.Vector2 position, ResourceType resourceType) {
        return resourceDeposits.values().stream()
                .filter(rd -> rd.isActive() && rd.getResourceType() == resourceType)
                .min((rd1, rd2) -> Double.compare(
                        rd1.getPosition().distance(position),
                        rd2.getPosition().distance(position)))
                .orElse(null);
    }

    /**
     * Find nearest refinery for a team
     */
    public Building findNearestRefinery(org.dyn4j.geometry.Vector2 position, int teamNumber) {
        return buildings.values().stream()
                .filter(b -> b.isActive() &&
                        b.getTeamNumber() == teamNumber &&
                        b.getBuildingType() == BuildingType.REFINERY &&
                        !b.isUnderConstruction())
                .min((b1, b2) -> Double.compare(
                        b1.getPosition().distance(position),
                        b2.getPosition().distance(position)))
                .orElse(null);
    }

    /**
     * Find nearest headquarters for a team
     */
    public Building findNearestHeadquarters(org.dyn4j.geometry.Vector2 position, int teamNumber) {
        return buildings.values().stream()
                .filter(b -> b.isActive() &&
                        b.getTeamNumber() == teamNumber &&
                        b.getBuildingType() == BuildingType.HEADQUARTERS &&
                        !b.isUnderConstruction())
                .min((b1, b2) -> Double.compare(
                        b1.getPosition().distance(position),
                        b2.getPosition().distance(position)))
                .orElse(null);
    }
}

