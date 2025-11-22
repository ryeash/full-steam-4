package com.fullsteam.model;

import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;
import org.dyn4j.world.result.RaycastResult;
import org.dyn4j.world.DetectFilter;

import java.util.List;
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
    private double elapsed = 0.0; // Time elapsed since beam was created
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
     * Create a beam with ray tracing using dyn4j's built-in raycast
     * @param world The dyn4j World to raycast against
     * @param start Starting position
     * @param targetDirection Direction vector (will be normalized)
     * @param maxRange Maximum beam range
     * @param ignoredBody Body to ignore in raycast (typically the firing unit)
     */
    public Beam(int id,
                World<Body> world,
                Vector2 start,
                Vector2 targetDirection,
                double maxRange,
                int ownerId,
                int ownerTeam,
                double damage,
                Set<BulletEffect> bulletEffects,
                Ordinance ordinanceType,
                BeamType beamType,
                double width,
                double duration,
                Body ignoredBody) {
        // Create body with raycast results BEFORE calling super
        super(id, 
              createBeamBodyWithRaycast(world, start, targetDirection, maxRange, width, ignoredBody, ownerTeam),
              ownerId, ownerTeam, start, damage, bulletEffects, ordinanceType, width);
        
        // Now extract the data from the body's user data (we stored it there temporarily)
        BeamData data = (BeamData) body.getUserData();
        
        this.startPosition = start.copy();
        this.endPosition = data.endPosition;
        this.intendedEndPosition = start.copy().add(data.direction.copy().multiply(maxRange));
        this.maxRange = maxRange;
        this.width = width;
        this.duration = duration;
        this.beamType = beamType;
        this.angle = data.angle;
        this.length = start.distance(endPosition);
        this.hitObstacle = data.hitObstacle;
        this.hitBody = data.hitBody;
        
        // Now set the correct user data (this Beam instance)
        body.setUserData(this);
    }
    
    /**
     * Helper class to pass beam data through body creation
     */
    private static class BeamData {
        Vector2 direction;
        Vector2 endPosition;
        double angle;
        boolean hitObstacle;
        Body hitBody;
        
        BeamData(Vector2 direction, Vector2 endPosition, double angle, boolean hitObstacle, Body hitBody) {
            this.direction = direction;
            this.endPosition = endPosition;
            this.angle = angle;
            this.hitObstacle = hitObstacle;
            this.hitBody = hitBody;
        }
    }
    
    /**
     * Create beam body with raycast - static helper for constructor
     */
    private static Body createBeamBodyWithRaycast(World<Body> world, Vector2 start, Vector2 targetDirection,
                                                   double maxRange, double width, Body ignoredBody, int ownerTeam) {
        // Normalize direction
        Vector2 direction = targetDirection.copy();
        double magnitude = direction.getMagnitude();
        if (magnitude > 0) {
            direction.multiply(1.0 / magnitude);
        }
        
        // Perform raycast
        RaycastResult result = performRaycast(world, start, direction, maxRange, ignoredBody, ownerTeam);
        
        // Calculate angle and midpoint
        double angle = Math.atan2(direction.y, direction.x);
        Vector2 midpoint = new Vector2(
            (start.x + result.endPosition.x) / 2.0,
            (start.y + result.endPosition.y) / 2.0
        );
        
        // Create body
        Body body = createBeamBody(start, result.endPosition, width, midpoint, angle);
        
        // Store beam data in body temporarily (will be replaced with Beam instance)
        BeamData data = new BeamData(direction, result.endPosition, angle, result.hitObstacle, result.hitBody);
        body.setUserData(data);
        
        return body;
    }
    
    /**
     * Helper class to return raycast results
     */
    private static class RaycastResult {
        Vector2 endPosition;
        boolean hitObstacle;
        Body hitBody;
        
        RaycastResult(Vector2 endPosition, boolean hitObstacle, Body hitBody) {
            this.endPosition = endPosition;
            this.hitObstacle = hitObstacle;
            this.hitBody = hitBody;
        }
    }
    
    /**
     * Perform raycast using dyn4j's built-in raycast functionality
     * This is much more efficient and accurate than custom ray tracing
     */
    private static RaycastResult performRaycast(World<Body> world, Vector2 start, Vector2 direction, 
                                                double maxRange, Body ignoredBody, int ownerTeam) {
        // Create a ray for the raycast
        Ray ray = new Ray(start, direction);
        
        // Create a filter that excludes the firing body and friendly units/buildings
        DetectFilter<Body, BodyFixture> filter = new DetectFilter<>(true, true, null) {
            @Override
            public boolean isAllowed(Body body, BodyFixture fixture) {
                // Ignore the firing body itself
                if (body == ignoredBody) {
                    return false;
                }
                
                // Check if this body belongs to a friendly entity
                Object userData = body.getUserData();
                
                // Skip friendly fire - beams pass through friendly units
                if (userData instanceof Unit unit) {
                    if (unit.getTeamNumber() == ownerTeam) {
                        return false;
                    }
                }
                
                // Skip friendly fire - beams pass through friendly buildings
                if (userData instanceof Building building) {
                    if (building.getTeamNumber() == ownerTeam) {
                        return false;
                    }
                }
                
                // Skip friendly fire - beams pass through friendly wall segments
                if (userData instanceof WallSegment segment) {
                    if (segment.getTeamNumber() == ownerTeam) {
                        return false;
                    }
                }
                
                return true;
            }
        };
        
        // Perform the raycast and get all results
        List<org.dyn4j.world.result.RaycastResult<Body, BodyFixture>> results = world.raycast(ray, maxRange, filter);
        
        // Find the closest hit
        org.dyn4j.world.result.RaycastResult<Body, BodyFixture> closestHit = null;
        double closestDistance = maxRange;
        
        for (org.dyn4j.world.result.RaycastResult<Body, BodyFixture> result : results) {
            // Get the raycast distance
            double distance = result.getRaycast().getDistance();
            
            // Check if this is the closest hit so far
            if (distance < closestDistance) {
                closestDistance = distance;
                closestHit = result;
            }
        }
        
        // If we hit something, calculate the hit point
        if (closestHit != null) {
            // Get the hit point from the raycast result
            Vector2 hitPoint = closestHit.getRaycast().getPoint();
            return new RaycastResult(hitPoint.copy(), true, closestHit.getBody());
        }
        
        // No hit - beam travels full distance
        Vector2 endPos = start.copy().add(direction.copy().multiply(maxRange));
        return new RaycastResult(endPos, false, null);
    }
    
    /**
     * Create a physics body for the beam (rectangular sensor for hit detection)
     * @param start Beam start position
     * @param end Beam end position
     * @param width Beam width
     * @param midpoint Position to place the body (midpoint of beam)
     * @param angle Rotation angle of the beam
     */
    private static Body createBeamBody(Vector2 start, Vector2 end, double width, Vector2 midpoint, double angle) {
        Body body = new Body();
        double length = start.distance(end);
        
        // Create a thin rectangle for the beam
        org.dyn4j.dynamics.BodyFixture fixture = body.addFixture(Geometry.createRectangle(length, width));
        
        // Make it a sensor so it doesn't physically interact with other bodies
        fixture.setSensor(true);
        
        body.setMass(MassType.INFINITE); // Beams don't move
        
        // Position and rotate the body
        body.translate(midpoint.x, midpoint.y);
        body.rotate(angle);
        
        return body;
    }
    
    /**
     * Update beam state - beams fade out after their duration
     */
    @Override
    public void update(double deltaTime) {
        if (!active) {
            return;
        }
        
        elapsed += deltaTime;
        
        // Deactivate after duration expires
        if (elapsed >= duration) {
            active = false;
        }
    }
    
    /**
     * Get the progress of the beam fade (0.0 = just fired, 1.0 = about to disappear)
     */
    public double getFadeProgress() {
        return Math.min(elapsed / duration, 1.0);
    }
    
    /**
     * Get the opacity for rendering (fades out over time)
     */
    public double getOpacity() {
        return 1.0 - getFadeProgress();
    }
    
    /**
     * Check if the beam has hit a specific point
     * Used for instant hit detection when the beam is first created
     */
    public boolean intersectsPoint(Vector2 point, double radius) {
        // Calculate distance from point to line segment
        Vector2 startToPoint = point.copy().subtract(startPosition);
        Vector2 startToEnd = endPosition.copy().subtract(startPosition);
        
        double lengthSquared = startToEnd.getMagnitudeSquared();
        if (lengthSquared == 0) {
            return startPosition.distance(point) <= (width / 2.0 + radius);
        }
        
        // Project point onto line
        double t = Math.max(0, Math.min(1, startToPoint.dot(startToEnd) / lengthSquared));
        Vector2 projection = startPosition.copy().add(startToEnd.multiply(t));
        
        double distance = point.distance(projection);
        return distance <= (width / 2.0 + radius);
    }
    
    /**
     * Get the rotation angle for rendering
     */
    public double getRotation() {
        return angle;
    }
}

