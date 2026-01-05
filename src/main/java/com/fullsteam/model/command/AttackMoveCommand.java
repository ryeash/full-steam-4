package com.fullsteam.model.command;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Building;
import com.fullsteam.model.Targetable;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

import java.util.Collection;
import java.util.List;

/**
 * Command to move to a destination while automatically engaging enemies along the way
 */
@Getter
public class AttackMoveCommand extends UnitCommand {
    private final Vector2 destination;
    // Note: path fields are now inherited from UnitCommand base class

    // Auto-acquired target (unit, building, or wall)
    @Setter
    private Targetable autoTarget = null;

    public AttackMoveCommand(Unit unit, Vector2 destination, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.destination = destination.copy();
    }

    /**
     * Set the pathfinding path for this attack-move command
     * (Override base class to maintain compatibility)
     */
    @Override
    public void setPath(List<Vector2> path) {
        super.setPath(path);
    }

    @Override
    public boolean update(double deltaTime) {
        // Check if we've reached the destination
        double distance = unit.getPosition().distance(destination);
        if (distance < unit.getBody().getRotationDiscRadius() * 0.75D) {
            unit.getBody().setLinearVelocity(0, 0);
            return false; // Command complete
        }

        // Clear invalid auto-target
        if (autoTarget != null && !autoTarget.isActive()) {
            autoTarget = null;
        }

        return true; // Still moving
    }

    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        Vector2 currentPos = unit.getPosition();

        // If we have an auto-target, move towards it
        if (autoTarget != null && autoTarget.isActive()) {
            Vector2 targetPos = autoTarget.getPosition();

            // Get weapon range
            double weaponRange = unit.getWeapon() != null ?
                    unit.getWeapon().getRange() :
                    unit.getUnitType().getAttackRange();
            double effectiveRange = weaponRange + autoTarget.getTargetSize();

            double distance = currentPos.distance(targetPos);

            // Move into range if too far
            if (distance > effectiveRange * 0.9) {
                unit.applySteeringForces(targetPos, nearbyUnits, deltaTime);
                return;
            } else {
                // In range, stop to attack
                unit.getBody().setLinearVelocity(0, 0);
                return;
            }
        }

        // No target, continue moving to destination
        if (!path.isEmpty() && currentPathIndex < path.size()) {
            Vector2 nextWaypoint = path.get(currentPathIndex);
            double distanceToWaypoint = currentPos.distance(nextWaypoint);

            // Move to next waypoint if close enough
            if (distanceToWaypoint < 20.0 && currentPathIndex < path.size() - 1) {
                currentPathIndex++;
            }

            // Apply steering forces towards current waypoint
            unit.applySteeringForces(nextWaypoint, nearbyUnits, deltaTime);
        } else if (destination != null) {
            // No path, move directly to destination
            double distance = currentPos.distance(destination);

            if (distance < 10.0) {
                // Reached destination, stop
                unit.getBody().setLinearVelocity(0, 0);
                return;
            }

            // Apply steering forces towards destination
            unit.applySteeringForces(destination, nearbyUnits, deltaTime);
        }
    }

    @Override
    public List<AbstractOrdinance> updateCombat(double deltaTime) {
        if (autoTarget == null || !autoTarget.isActive()) {
            return List.of();
        }

        Vector2 currentPos = unit.getPosition();
        Vector2 targetPos = autoTarget.getPosition();
        double distance = currentPos.distance(targetPos);

        // Get weapon range
        double weaponRange = unit.getWeapon() != null ?
                unit.getWeapon().getRange() :
                unit.getUnitType().getAttackRange();
        double effectiveRange = weaponRange + autoTarget.getTargetSize();

        // Check if in range
        if (distance <= effectiveRange * 0.9) {
            // Face target
            Vector2 direction = targetPos.copy().subtract(currentPos);
            unit.setRotation(Math.atan2(direction.y, direction.x));

            // Use predictive aiming for moving targets (units)
            Vector2 aimPosition;
            if (autoTarget instanceof Unit targetUnit) {
                aimPosition = unit.calculateInterceptPoint(targetUnit);
            } else {
                // Stationary targets (buildings, walls)
                aimPosition = targetPos;
            }

            return unit.fireAt(aimPosition, autoTarget.getElevation(), gameEntities);
        }

        return List.of();
    }

    /**
     * Scan for enemies and auto-acquire target (uses unified Targetable finder)
     * Called by RTSGameManager during enemy scanning
     */
    public boolean scanForEnemies(Collection<Unit> allUnits, Collection<Building> allBuildings) {
        if (!unit.getUnitType().canAttack()) {
            return false;
        }

        Vector2 currentPos = unit.getPosition();
        double visionRange = unit.getUnitType().getAttackRange() * 1.5;

        // Use unified targetable finder - automatically handles units, buildings, walls
        // and respects elevation targeting and cloak detection
        Targetable nearestEnemy = gameEntities.findNearestEnemyTargetable(
                unit);

        if (nearestEnemy != null) {
            autoTarget = nearestEnemy;
            return true;
        }

        return false;
    }

    @Override
    public Vector2 getTargetPosition() {
        // If engaging a target, return its position
        if (autoTarget != null && autoTarget.isActive()) {
            return autoTarget.getPosition();
        }
        // Otherwise, target is the destination
        return destination;
    }

    @Override
    public boolean isMoving() {
        return true;
    }

    @Override
    public String getDescription() {
        if (autoTarget != null && autoTarget.isActive()) {
            return String.format("Attack-move to (%.1f, %.1f) - engaging %s %d",
                    destination.x, destination.y, autoTarget.getTargetType(), autoTarget.getId());
        }
        return String.format("Attack-move to (%.1f, %.1f)", destination.x, destination.y);
    }
}

