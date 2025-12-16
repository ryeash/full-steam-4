package com.fullsteam.model.command;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Targetable;
import com.fullsteam.model.Unit;
import com.fullsteam.model.component.GunshipComponent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * Command for interceptors/gunships to patrol a specific location and engage enemies.
 * The aircraft will:
 * 1. Fly to the station location
 * 2. Circle/patrol the area
 * 3. Automatically engage any enemy units that come within range
 * 4. Return to hangar when fuel/ammo is depleted
 * <p>
 * Unlike IdleCommand, this maintains patrol behavior and engages targets without switching commands.
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

    // Target tracking for combat
    private Targetable currentTarget = null;
    private boolean isEngaging = false;

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
            log.debug("Aircraft {} reached station", unit.getId());
        }

        // Gunships handle their own targeting via GunshipComponent
        // Only interceptors need manual target tracking
        boolean isGunship = unit.getComponent(GunshipComponent.class).isPresent();

        if (!isGunship) {
            // Check if current target is still valid
            if (currentTarget != null && (!currentTarget.isActive() || currentTarget.getTeamNumber() == unit.getTeamNumber())) {
                log.debug("Aircraft {} lost target, resuming patrol", unit.getId());
                currentTarget = null;
                isEngaging = false;
            }

            // Scan for new target if we don't have one
            if (currentTarget == null) {
                // Get attack range for scanning
                double attackRange = unit.getWeapon() != null ?
                        unit.getWeapon().getRange() :
                        unit.getUnitType().getAttackRange();

                currentTarget = gameEntities.findNearestEnemyTargetable(
                        unit.getPosition(), unit.getTeamNumber(), attackRange, unit);

                if (currentTarget != null) {
                    log.debug("Aircraft {} acquired target {}", unit.getId(), currentTarget.getId());
                    isEngaging = true;
                }
            }
        }

        return true; // Command continues indefinitely (until fuel runs out)
    }

    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        // Gunships just patrol - they don't chase targets (engage while moving)
        boolean isGunship = unit.getComponent(GunshipComponent.class).isPresent();

        if (!isGunship && isEngaging && currentTarget != null) {
            // Interceptors: Chase the target
            moveTowardsTarget(currentTarget.getPosition(), deltaTime);
        } else if (!onStation) {
            // Fly to station location
            moveTowardsTarget(stationLocation, deltaTime);
        } else {
            // Patrol using polygon waypoints
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
        // Gunships handle their own combat via GunshipComponent.attackEnemies()
        // They have dual weapons (ground + air) that fire automatically
        if (unit.getComponent(GunshipComponent.class).isPresent()) {
            return List.of(); // Component handles everything
        }

        // For interceptors and other aircraft: standard combat logic
        if (isEngaging && currentTarget != null && currentTarget.isActive()) {
            Vector2 currentPos = unit.getPosition();
            Vector2 targetPos = currentTarget.getPosition();
            double distance = currentPos.distance(targetPos);

            // Get weapon range
            double weaponRange;
            if (unit.getWeapon() != null) {
                weaponRange = unit.getWeapon().getRange();
            } else {
                weaponRange = unit.getUnitType().getAttackRange();
            }

            // Fire if in range
            if (distance <= weaponRange) {
                // Face target
                Vector2 direction = targetPos.copy().subtract(currentPos);
                unit.setRotation(Math.atan2(direction.y, direction.x));

                // Use predictive aiming for moving targets (units), direct aim for stationary (buildings, walls)
                Vector2 aimPosition;
                if (currentTarget instanceof Unit targetUnit) {
                    aimPosition = unit.calculateInterceptPoint(targetUnit);
                } else {
                    aimPosition = targetPos;
                }
                return unit.fireAt(aimPosition, gameEntities);
            }
        }

        return List.of();
    }

    @Override
    public String getDescription() {
        // Only show engaging status for interceptors (gunships engage automatically while patrolling)
        boolean isGunship = unit.getComponent(GunshipComponent.class).isPresent();

        if (!isGunship && isEngaging && currentTarget != null) {
            return String.format("Engaging Target (%.0f, %.0f)",
                    currentTarget.getPosition().x, currentTarget.getPosition().y);
        }
        if (onStation) {
            return "On Station (Patrolling)";
        }
        return String.format("Moving to Station (%.0f, %.0f)", stationLocation.x, stationLocation.y);
    }
}

