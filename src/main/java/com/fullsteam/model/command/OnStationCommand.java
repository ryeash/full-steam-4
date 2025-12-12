package com.fullsteam.model.command;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Elevation;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * Command for interceptors to patrol a specific location and engage enemy aircraft.
 * The interceptor will:
 * 1. Fly to the station location
 * 2. Circle/patrol the area
 * 3. Automatically engage any enemy air units that come within range
 * 4. Return to hangar when fuel/ammo is depleted
 * <p>
 * This is similar to IdleCommand but with a specific patrol location.
 */
@Slf4j
@Getter
public class OnStationCommand extends UnitCommand {

    private static final int PATROL_SIDES = 32;
    private static final double PATROL_RADIUS = 200.0;

    private final Vector2 stationLocation;
    private boolean onStation = false;
    private final List<Vector2> patrolWaypoints;
    private int currentWaypointIndex = 0;

    public OnStationCommand(Unit unit, Vector2 stationLocation, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.stationLocation = stationLocation.copy();
        this.patrolWaypoints = calculatePatrolWaypoints();
    }

    /**
     * Calculate waypoints for a nonagon (9-sided polygon) patrol pattern.
     * This provides smoother movement than a perfect circle while avoiding jank.
     */
    private List<Vector2> calculatePatrolWaypoints() {
        List<Vector2> waypoints = new ArrayList<>();
        double angleStep = (Math.PI * 2.0) / PATROL_SIDES;

        for (int i = 0; i < PATROL_SIDES; i++) {
            double angle = i * angleStep;
            double x = stationLocation.x + Math.cos(angle) * PATROL_RADIUS;
            double y = stationLocation.y + Math.sin(angle) * PATROL_RADIUS;
            waypoints.add(new Vector2(x, y));
        }

        return waypoints;
    }

    @Override
    public boolean update(double deltaTime) {
        Vector2 currentPos = unit.getPosition();
        double distanceToStation = currentPos.distance(stationLocation);

        // Check if we've reached the station
        if (!onStation && distanceToStation < PATROL_RADIUS * 0.5) {
            onStation = true;
        }

        // Scan for enemy air units
        Unit enemyAircraft = scanForEnemyAircraft();
        if (enemyAircraft != null) {
            log.info("Interceptor {} engaging enemy aircraft {}", unit.getId(), enemyAircraft.getId());
            unit.issueCommand(new AttackUnitCommand(unit, enemyAircraft, false), gameEntities);
            return false; // Switch to attack command
        }

        return true; // Continue patrolling
    }

    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        if (!onStation) {
            // Fly to station location
            moveTowardsTarget(stationLocation, deltaTime);
        } else {
            // Patrol using nonagon waypoints
            Vector2 currentWaypoint = patrolWaypoints.get(currentWaypointIndex);
            Vector2 currentPos = unit.getPosition();
            double distanceToWaypoint = currentPos.distance(currentWaypoint);

            // Move to next waypoint when close enough
            if (distanceToWaypoint < 15.0) { // Threshold to advance to next waypoint
                currentWaypointIndex = (currentWaypointIndex + 1) % PATROL_SIDES;
                currentWaypoint = patrolWaypoints.get(currentWaypointIndex);
            }

            // Move towards current waypoint
            moveTowardsTarget(currentWaypoint, deltaTime);
        }
    }

    /**
     * Scan for enemy aircraft within attack range.
     * Prioritizes HIGH elevation targets (bombers), then LOW elevation.
     */
    private Unit scanForEnemyAircraft() {
        Vector2 currentPos = unit.getPosition();
        double attackRange = unit.getWeapon().getRange();

        // Find all enemy air units within range
        Unit nearestHighAltitude = null;
        Unit nearestLowAltitude = null;
        double nearestHighDistance = Double.MAX_VALUE;
        double nearestLowDistance = Double.MAX_VALUE;

        for (Unit other : gameEntities.getUnits().values()) {
            if (other.getTeamNumber() == unit.getTeamNumber() || !other.isActive()) {
                continue;
            }

            // Only engage air units
            Elevation targetElevation = other.getUnitType().getElevation();
            if (targetElevation == Elevation.GROUND) {
                continue;
            }

            // Check if we can target this elevation
            if (!Unit.canWeaponTargetUnit(unit.getWeapon(), other)) {
                continue;
            }

            double distance = currentPos.distance(other.getPosition());
            if (distance <= attackRange) {
                if (targetElevation == Elevation.HIGH && distance < nearestHighDistance) {
                    nearestHighAltitude = other;
                    nearestHighDistance = distance;
                } else if (targetElevation == Elevation.LOW && distance < nearestLowDistance) {
                    nearestLowAltitude = other;
                    nearestLowDistance = distance;
                }
            }
        }

        // Prioritize HIGH altitude targets (bombers are more dangerous)
        return nearestHighAltitude != null ? nearestHighAltitude : nearestLowAltitude;
    }

    /**
     * Helper method to move towards a target location.
     */
    private void moveTowardsTarget(Vector2 target, double deltaTime) {
        Vector2 currentPos = unit.getPosition();
        Vector2 direction = target.copy().subtract(currentPos);
        double distance = direction.getMagnitude();

        if (distance < 5.0) {
            return; // Close enough
        }

        direction.normalize();

        // Set velocity towards target
        double speed = unit.getUnitType().getMovementSpeed();
        unit.getBody().setLinearVelocity(
                direction.x * speed,
                direction.y * speed
        );

        // Face movement direction
        unit.setRotation(Math.atan2(direction.y, direction.x));
    }

    @Override
    public List<AbstractOrdinance> updateCombat(double deltaTime) {
        // Combat is handled by switching to AttackUnitCommand
        return List.of();
    }

    @Override
    public String getDescription() {
        if (onStation) {
            return "On Station (Patrolling)";
        }
        return String.format("Moving to Station (%.0f, %.0f)", stationLocation.x, stationLocation.y);
    }
}

