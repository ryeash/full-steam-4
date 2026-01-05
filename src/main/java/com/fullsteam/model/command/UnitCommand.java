package com.fullsteam.model.command;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Pathfinding;
import com.fullsteam.model.Unit;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all unit commands (move, attack, harvest, etc.)
 * Implements the Command Pattern to encapsulate unit behaviors
 */
@Getter
public abstract class UnitCommand {
    protected final Unit unit;
    protected final boolean isPlayerOrder;

    /**
     * Game entities - provides access to all units, buildings, obstacles, etc.
     * Initialized via init() method when command is issued.
     */
    protected GameEntities gameEntities;

    /**
     * Pathfinding support - shared by all commands that need to navigate
     */
    protected List<Vector2> path = new ArrayList<>();
    protected int currentPathIndex = 0;
    protected Vector2 lastPathTarget = null; // Track when we need to recompute path
    protected long lastPathComputeTime = 0; // Throttle path recomputation

    public UnitCommand(Unit unit, boolean isPlayerOrder) {
        this.unit = unit;
        this.isPlayerOrder = isPlayerOrder;
    }

    /**
     * Initialize this command with game entities context.
     * Called by Unit.issueCommand() immediately after construction.
     *
     * @param gameEntities Reference to all game entities
     */
    public void init(GameEntities gameEntities) {
        this.gameEntities = gameEntities;
    }

    /**
     * Set a pre-computed path for this command.
     * Used when pathfinding is done externally (e.g., in RTSGameManager for player orders).
     */
    public void setPath(List<Vector2> path) {
        this.path = new ArrayList<>(path);
        this.currentPathIndex = 0;
        if (!path.isEmpty()) {
            this.lastPathTarget = path.get(path.size() - 1).copy();
        }
    }

    /**
     * Compute a path to the given destination using A* pathfinding.
     * Automatically accounts for obstacles, buildings, and unit elevation.
     *
     * @param destination Target position
     */
    protected void computePathTo(Vector2 destination) {
        if (gameEntities == null || destination == null) {
            return;
        }

        // Throttle path computation to avoid performance issues (max once per 0.5 seconds)
        long now = System.currentTimeMillis();
        if (now - lastPathComputeTime < 500) {
            return;
        }
        lastPathComputeTime = now;

        // Compute path using A* algorithm
        List<Vector2> newPath = Pathfinding.findPath(
                unit.getPosition(),
                destination,
                gameEntities.getObstacles().values(),
                gameEntities.getBuildings().values(),
                unit.getUnitType().getSize(),
                gameEntities.getGameConfig().getWorldWidth(),
                gameEntities.getGameConfig().getWorldHeight(),
                unit.getUnitType().getElevation().isAirborne()
        );

        setPath(newPath);
    }

    /**
     * Follow the current path by moving toward the next waypoint.
     * Call this from updateMovement() implementations.
     *
     * @param destination      Final destination (for direct path if no path exists)
     * @param nearbyUnits      Units near this unit (for separation steering)
     * @param arrivalThreshold Distance to consider "arrived" at waypoint
     */
    protected void followPathTo(Vector2 destination, List<Unit> nearbyUnits, double arrivalThreshold) {
        if (destination == null) {
            unit.getBody().setLinearVelocity(0, 0);
            return;
        }

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
            unit.applySteeringForces(nextWaypoint, nearbyUnits, 0.016); // ~60 FPS
        } else {
            // No path or path completed, move directly to destination
            double distance = currentPos.distance(destination);

            if (distance < arrivalThreshold) {
                // Reached destination, stop
                unit.getBody().setLinearVelocity(0, 0);
                return;
            }

            // Apply steering forces towards destination
            unit.applySteeringForces(destination, nearbyUnits, 0.016);
        }
    }

    /**
     * Update this command (called every frame)
     *
     * @param deltaTime Time since last update in seconds
     * @return true if command is still active, false if completed/cancelled
     */
    public abstract boolean update(double deltaTime);

    /**
     * Execute movement logic for this command
     *
     * @param deltaTime   Time since last update
     * @param nearbyUnits Units near this unit (for steering behaviors)
     */
    public abstract void updateMovement(double deltaTime, List<Unit> nearbyUnits);

    /**
     * Check if this command should engage in combat
     *
     * @param deltaTime Time since last update
     * @return List of AbstractOrdinances (Projectiles or Beams) if fired, empty list otherwise
     */
    public List<AbstractOrdinance> updateCombat(double deltaTime) {
        return List.of(); // Override in combat commands
    }

    /**
     * Called when command is cancelled/replaced by a new command
     */
    public void onCancel() {
        // Stop movement by default
        unit.getBody().setLinearVelocity(0, 0);
    }

    /**
     * Get the target position for this command (for pathfinding, AI, etc.)
     *
     * @return Target position, or null if command has no spatial target
     */
    public Vector2 getTargetPosition() {
        return null; // Override in commands with spatial targets
    }

    /**
     * Check if this command involves movement
     */
    public boolean isMoving() {
        return false; // Override in movement commands
    }

    /**
     * Get a human-readable description of this command (for debugging)
     */
    public abstract String getDescription();
}

