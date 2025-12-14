package com.fullsteam.model;

import com.fullsteam.model.weapon.ElevationTargeting;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class for all weapon ordinance (projectiles, beams, etc.)
 * Provides common functionality for damage dealing and effects.
 */
@Getter
@Setter
public abstract class AbstractOrdinance extends GameEntity {
    protected int id;
    protected int ownerId; // Player that fired this
    protected int ownerTeam;
    protected Vector2 origin; // Position where ordinance was created
    protected double damage;
    protected Set<BulletEffect> bulletEffects;
    protected Ordinance ordinanceType;
    protected double size; // Visual size
    protected boolean active = true;
    protected Set<Integer> affectedEntities = new HashSet<>(); // For piercing/multi-hit tracking
    protected ElevationTargeting elevationTargeting; // Which elevations this ordinance can hit
    
    /**
     * The elevation this ordinance is flying at.
     * This determines which objects it can collide with:
     * - GROUND ordinance hits ground units, buildings, obstacles, walls
     * - LOW ordinance hits low-altitude aircraft only
     * - HIGH ordinance hits high-altitude aircraft only
     */
    protected Elevation currentElevation;

    public AbstractOrdinance(int id, Body body, int ownerId, int ownerTeam,
                             Vector2 origin, double damage,
                             Set<BulletEffect> bulletEffects,
                             Ordinance ordinanceType, double size,
                             ElevationTargeting elevationTargeting,
                             Elevation currentElevation) {
        super(id, body, 0);
        this.id = id;
        this.ownerId = ownerId;
        this.ownerTeam = ownerTeam;
        this.origin = origin != null ? origin.copy() : new Vector2(0, 0);
        this.damage = damage;
        this.bulletEffects = bulletEffects != null ? new HashSet<>(bulletEffects) : new HashSet<>();
        this.ordinanceType = ordinanceType;
        this.size = size;
        this.elevationTargeting = elevationTargeting != null ? elevationTargeting : ElevationTargeting.GROUND_ONLY;
        this.currentElevation = currentElevation != null ? currentElevation : Elevation.GROUND;
    }

    /**
     * Update the ordinance state
     */
    public abstract void update(GameEntities gameEntities);
}

