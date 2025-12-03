package com.fullsteam.model.command;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Projectile;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

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
     * Set by RTSGameManager each frame for AI and decision-making
     */
    @Setter
    protected GameEntities gameEntities;
    
    public UnitCommand(Unit unit, boolean isPlayerOrder) {
        this.unit = unit;
        this.isPlayerOrder = isPlayerOrder;
    }
    
    /**
     * Update this command (called every frame)
     * @param deltaTime Time since last update in seconds
     * @return true if command is still active, false if completed/cancelled
     */
    public abstract boolean update(double deltaTime);
    
    /**
     * Execute movement logic for this command
     * @param deltaTime Time since last update
     * @param nearbyUnits Units near this unit (for steering behaviors)
     */
    public abstract void updateMovement(double deltaTime, List<Unit> nearbyUnits);
    
    /**
     * Check if this command should engage in combat
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

