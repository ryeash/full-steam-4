package com.fullsteam.model;

import com.fullsteam.games.IdGenerator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;

/**
 * Represents a wall segment that connects two wall posts.
 * Wall segments are automatically created when two wall posts are close enough together.
 * Wall segments are destructible entities that can be targeted and destroyed.
 */
@Slf4j
@Getter
@Setter
public class WallSegment extends GameEntity implements Targetable {
    private final int ownerId;
    private final int teamNumber;
    private final Building post1;
    private final Building post2;

    // Wall segment dimensions
    private static final double WALL_THICKNESS = 8.0;

    public WallSegment(Building post1, Building post2, int ownerId, int teamNumber) {
        super(IdGenerator.nextEntityId(), createWallBody(post1, post2), calculateWallHealth(post1, post2));

        this.post1 = post1;
        this.post2 = post2;
        this.ownerId = ownerId;
        this.teamNumber = teamNumber;

        log.debug("Created wall segment {} connecting posts {} and {} (length: {}, health: {})",
                id, post1.getId(), post2.getId(), getLength(), maxHealth);
    }

    /**
     * Calculate wall health based on length (10 HP per unit of length)
     */
    private static double calculateWallHealth(Building post1, Building post2) {
        double length = post1.getPosition().distance(post2.getPosition());
        return length * 10.0;
    }

    /**
     * Create the physics body for the wall segment
     */
    private static Body createWallBody(Building post1, Building post2) {
        Vector2 pos1 = post1.getPosition();
        Vector2 pos2 = post2.getPosition();
        double length = pos1.distance(pos2);

        // Create physics body
        Vector2 midpoint = new Vector2(
                (pos1.x + pos2.x) / 2.0,
                (pos1.y + pos2.y) / 2.0
        );

        Body body = new Body();

        // Create rectangle fixture for the wall segment
        BodyFixture fixture = body.addFixture(new Rectangle(length, WALL_THICKNESS));

        // Configure fixture properties
        fixture.setFriction(0.1);      // Low friction
        fixture.setRestitution(0.0);   // No bounce
        fixture.setSensor(false);      // Solid collision (not a sensor)
        body.setMass(MassType.INFINITE); // Walls don't move

        // Calculate rotation angle to align with the two posts
        double angle = Math.atan2(pos2.y - pos1.y, pos2.x - pos1.x);

        // IMPORTANT: Rotate first, then translate
        // If you translate first, rotation happens around origin (0,0)
        body.rotate(angle);
        body.translate(midpoint);

        return body;
    }

    /**
     * Get the length of this wall segment
     */
    public double getLength() {
        return post1.getPosition().distance(post2.getPosition());
    }

    /**
     * Take damage (overrides GameEntity method)
     * Logs when wall segment is destroyed
     */
    @Override
    public void takeDamage(double amount) {
        super.takeDamage(amount);
        if (!active) {
            log.debug("Wall segment {} destroyed", id);
        }
    }

    /**
     * Check if this wall segment belongs to a specific player
     */
    public boolean belongsTo(int playerId) {
        return this.ownerId == playerId;
    }

    /**
     * Check if either connected post is destroyed
     */
    public boolean hasDestroyedPost() {
        return !post1.isActive() || !post2.isActive();
    }

    // ==================== Targetable Interface Implementation ====================

    @Override
    public Elevation getElevation() {
        return Elevation.GROUND; // Walls are always at ground level
    }

    @Override
    public double getTargetSize() {
        return 10.0; // Wall segment "thickness" for targeting
    }

    // Note: getId(), getPosition(), getRotation(), isActive(), takeDamage(), 
    // getHealth(), getMaxHealth() are inherited from GameEntity
}

