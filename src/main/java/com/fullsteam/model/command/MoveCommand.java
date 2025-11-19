package com.fullsteam.model.command;

import com.fullsteam.model.Unit;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to move a unit to a specific location
 */
@Getter
public class MoveCommand extends UnitCommand {
    private final Vector2 destination;
    private List<Vector2> path = new ArrayList<>();
    private int currentPathIndex = 0;
    
    public MoveCommand(Unit unit, Vector2 destination, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.destination = destination.copy();
    }
    
    /**
     * Set the pathfinding path for this move command
     */
    public void setPath(List<Vector2> path) {
        this.path = new ArrayList<>(path);
        this.currentPathIndex = 0;
    }
    
    @Override
    public boolean update(double deltaTime) {
        // Check if we've reached the destination
        double distance = unit.getPosition().distance(destination);
        if (distance < 10.0) {
            unit.getBody().setLinearVelocity(0, 0);
            return false; // Command complete
        }
        return true; // Still moving
    }
    
    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        Vector2 currentPos = unit.getPosition();
        
        // Follow path if we have one
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
    public Vector2 getTargetPosition() {
        return destination;
    }
    
    @Override
    public boolean isMoving() {
        return true;
    }
    
    @Override
    public String getDescription() {
        return String.format("Move to (%.1f, %.1f)", destination.x, destination.y);
    }
    
    public List<Vector2> getPath() {
        return path;
    }
    
    public int getCurrentPathIndex() {
        return currentPathIndex;
    }
}

