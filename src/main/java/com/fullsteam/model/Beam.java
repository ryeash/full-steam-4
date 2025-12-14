package com.fullsteam.model;

import com.fullsteam.games.IdGenerator;
import com.fullsteam.model.weapon.ElevationTargeting;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

import java.util.Set;

/**
 * Represents an instant-hit beam weapon (laser, plasma beam, etc.)
 * Beams travel instantly from source to target and have a visual duration.
 * Uses dyn4j's built-in raycast to determine actual impact point (stops at obstacles).
 */
@Getter
@Setter
public class Beam extends AbstractOrdinance {
    private Vector2 startPosition;
    private Vector2 endPosition;
    private Vector2 intendedEndPosition; // Where the beam was aimed
    private double length;
    private double maxRange;
    private double angle;
    private double width; // Beam thickness for rendering
    private double duration; // How long the beam is visible (seconds)
    private BeamType beamType;
    private boolean hitObstacle = false; // Whether beam was stopped by an obstacle
    private Body hitBody = null; // The body that was hit (if any)

    /**
     * Types of beam weapons
     */
    public enum BeamType {
        LASER,      // Thin, precise beam
        PLASMA,     // Thicker, energy beam
        PARTICLE,   // Particle beam effect
        ION         // Ion beam
    }

    /**
     * Create a beam with pre-calculated raycast results (called by BeamWeapon).
     * The weapon handles raycasting, this constructor just creates the beam entity.
     *
     * @param start         Starting position
     * @param end           Ending position (from raycast)
     * @param maxRange      Maximum beam range
     * @param ownerId       Owner entity ID
     * @param ownerTeam     Owner team number
     * @param damage        Damage dealt
     * @param bulletEffects Special effects
     * @param ordinanceType Ordinance type
     * @param beamType      Visual beam type
     * @param width         Beam width
     * @param duration      Visual duration
     * @param elevationTargeting Which elevations this beam can hit
     * @param currentElevation The elevation this beam is traveling at
     */
    public Beam(Vector2 start,
                Vector2 end,
                double maxRange,
                int ownerId,
                int ownerTeam,
                double damage,
                Set<BulletEffect> bulletEffects,
                Ordinance ordinanceType,
                BeamType beamType,
                double width,
                double duration,
                ElevationTargeting elevationTargeting,
                Elevation currentElevation) {
        super(IdGenerator.nextEntityId(),
                createBeamBody(start, end, width),
                ownerId, ownerTeam, start, damage, bulletEffects, ordinanceType, width, elevationTargeting, currentElevation);

        this.startPosition = start.copy();
        this.endPosition = end.copy();
        this.intendedEndPosition = end.copy(); // Same as end since weapon already did raycast
        this.maxRange = maxRange;
        this.width = width;
        this.duration = duration;
        this.beamType = beamType;

        // Calculate angle from start to end
        Vector2 direction = end.copy().subtract(start);
        this.angle = Math.atan2(direction.y, direction.x);
        this.length = start.distance(end);
        this.expires = (long) (System.currentTimeMillis() + (duration * 1000));
    }

    /**
     * Create a physics body for the beam (rectangular sensor for hit detection).
     * Simplified version that calculates midpoint and angle internally.
     *
     * @param start Beam start position
     * @param end   Beam end position
     * @param width Beam width
     */
    private static Body createBeamBody(Vector2 start, Vector2 end, double width) {
        Body body = new Body();
        double length = start.distance(end);

        // Calculate midpoint and angle
        Vector2 direction = end.copy().subtract(start);
        double angle = Math.atan2(direction.y, direction.x);
        BodyFixture fixture = body.addFixture(Geometry.createRectangle(length, width));
        fixture.setSensor(true);
        body.setMass(MassType.INFINITE);
        // IMPORTANT: Rotate first, then translate
        // If you translate first, rotation happens around origin (0,0) and shifts the beam
        body.rotate(angle);
        Vector2 midpoint = new Vector2(
                (start.x + end.x) / 2.0,
                (start.y + end.y) / 2.0
        );
        body.translate(midpoint.x, midpoint.y);
        return body;
    }

    /**
     * Update beam state - beams fade out after their duration
     */
    @Override
    public void update(GameEntities gameEntities) {
        if (!active) {
            return;
        }
        // Deactivate after duration expires
        if (isExpired()) {
            active = false;
        }
    }

    /**
     * Get the rotation angle for rendering
     */
    public double getRotation() {
        return angle;
    }

    /**
     * Get the set of affected entity IDs (for tracking which entities have been damaged)
     * Convenience method that delegates to getAffectedEntities()
     */
    public Set<Integer> getAffectedPlayers() {
        return getAffectedEntities();
    }
}

