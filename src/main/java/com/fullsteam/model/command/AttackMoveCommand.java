package com.fullsteam.model.command;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Building;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Command to move to a destination while automatically engaging enemies along the way
 */
@Getter
public class AttackMoveCommand extends UnitCommand {
    private final Vector2 destination;
    private List<Vector2> path = new ArrayList<>();
    private int currentPathIndex = 0;

    // Auto-acquired targets (unit will engage while moving)
    @Setter
    private Unit autoTargetUnit = null;
    @Setter
    private Building autoTargetBuilding = null;

    public AttackMoveCommand(Unit unit, Vector2 destination, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.destination = destination.copy();
    }

    /**
     * Set the pathfinding path for this attack-move command
     */
    public void setPath(List<Vector2> path) {
        this.path = new ArrayList<>(path);
        this.currentPathIndex = 0;
    }

    @Override
    public boolean update(double deltaTime) {
        // Check if we've reached the destination
        double distance = unit.getPosition().distance(destination);
        if (distance < unit.getBody().getRotationDiscRadius() * 0.75D) {
            unit.getBody().setLinearVelocity(0, 0);
            return false; // Command complete
        }

        // Clear invalid auto-targets
        if (autoTargetUnit != null && !autoTargetUnit.isActive()) {
            autoTargetUnit = null;
        }
        if (autoTargetBuilding != null && !autoTargetBuilding.isActive()) {
            autoTargetBuilding = null;
        }

        return true; // Still moving
    }

    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        Vector2 currentPos = unit.getPosition();

        // If we have an auto-target (unit or building), move towards it
        Vector2 targetPos = null;
        double effectiveRange = unit.getUnitType().getAttackRange();
        
        if (autoTargetUnit != null && autoTargetUnit.isActive()) {
            targetPos = autoTargetUnit.getPosition();
        } else if (autoTargetBuilding != null && autoTargetBuilding.isActive()) {
            targetPos = autoTargetBuilding.getPosition();
            effectiveRange += autoTargetBuilding.getBuildingType().getSize();
        }
        
        if (targetPos != null) {
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
        Vector2 currentPos = unit.getPosition();
        
        // Prioritize unit targets over building targets
        if (autoTargetUnit != null && autoTargetUnit.isActive()) {
            Vector2 targetPos = autoTargetUnit.getPosition();
            double distance = currentPos.distance(targetPos);

            // Check if in range
            if (distance <= unit.getWeapon().getRange() * 0.9) { // 90% of range to account for movement
                // Face target
                Vector2 direction = targetPos.copy().subtract(currentPos);
                unit.setRotation(Math.atan2(direction.y, direction.x));

                // Attack if cooldown is ready
                // Use predictive aiming for moving targets
                Vector2 interceptPos = unit.calculateInterceptPoint(autoTargetUnit);
                return unit.fireAt(interceptPos, gameEntities);
            }
        } else if (autoTargetBuilding != null && autoTargetBuilding.isActive()) {
            Vector2 targetPos = autoTargetBuilding.getPosition();
            double distance = currentPos.distance(targetPos);
            double effectiveRange = unit.getWeapon().getRange() + autoTargetBuilding.getBuildingType().getSize();

            // Check if in range
            if (distance <= effectiveRange * 0.9) {
                // Face target
                Vector2 direction = targetPos.copy().subtract(currentPos);
                unit.setRotation(Math.atan2(direction.y, direction.x));

                // Attack building (stationary target, no prediction needed)
                return unit.fireAt(targetPos, gameEntities);
            }
        }
        return List.of();
    }

    /**
     * Scan for enemies and auto-acquire target (units first, then buildings)
     * Called by RTSGameManager during enemy scanning
     */
    public boolean scanForEnemies(Collection<Unit> allUnits, Collection<Building> allBuildings) {
        if (!unit.getUnitType().canAttack()) {
            return false;
        }

        Vector2 currentPos = unit.getPosition();
        double visionRange = unit.getUnitType().getAttackRange() * 1.5;

        // Find nearest enemy unit in vision range (prioritize units over buildings)
        Unit nearestEnemyUnit = null;
        double nearestUnitDistance = Double.MAX_VALUE;

        for (Unit other : allUnits) {
            if (other.getTeamNumber() != unit.getTeamNumber() && other.isActive()) {
                // Check if this unit's weapon can target the other unit's elevation
                if (!unit.canTargetElevation(other)) {
                    continue; // Cannot target this elevation
                }
                
                double distance = currentPos.distance(other.getPosition());
                
                // Cloaked units can only be detected within cloak detection range
                if (other.isCloaked() && distance > Unit.getCloakDetectionRange()) {
                    continue;
                }
                
                if (distance < visionRange && distance < nearestUnitDistance) {
                    nearestUnitDistance = distance;
                    nearestEnemyUnit = other;
                }
            }
        }

        // If found an enemy unit, target it (units have priority)
        if (nearestEnemyUnit != null) {
            autoTargetUnit = nearestEnemyUnit;
            autoTargetBuilding = null; // Clear building target
            return true;
        }

        // If no enemy units found, look for enemy buildings
        if (unit.canTargetBuildings()) {
            Building nearestEnemyBuilding = null;
            double nearestBuildingDistance = Double.MAX_VALUE;

            for (Building building : allBuildings) {
                if (building.getTeamNumber() != unit.getTeamNumber() && building.isActive()) {
                    double distance = currentPos.distance(building.getPosition());
                    
                    if (distance < visionRange && distance < nearestBuildingDistance) {
                        nearestBuildingDistance = distance;
                        nearestEnemyBuilding = building;
                    }
                }
            }

            if (nearestEnemyBuilding != null) {
                autoTargetBuilding = nearestEnemyBuilding;
                autoTargetUnit = null; // Clear unit target
                return true;
            }
        }

        return false;
    }

    @Override
    public Vector2 getTargetPosition() {
        // If engaging unit, target position is the unit
        if (autoTargetUnit != null && autoTargetUnit.isActive()) {
            return autoTargetUnit.getPosition();
        }
        // If engaging building, target position is the building
        if (autoTargetBuilding != null && autoTargetBuilding.isActive()) {
            return autoTargetBuilding.getPosition();
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
        if (autoTargetUnit != null && autoTargetUnit.isActive()) {
            return String.format("Attack-move to (%.1f, %.1f) - engaging unit %d",
                    destination.x, destination.y, autoTargetUnit.getId());
        }
        if (autoTargetBuilding != null && autoTargetBuilding.isActive()) {
            return String.format("Attack-move to (%.1f, %.1f) - engaging building %d",
                    destination.x, destination.y, autoTargetBuilding.getId());
        }
        return String.format("Attack-move to (%.1f, %.1f)", destination.x, destination.y);
    }
}

