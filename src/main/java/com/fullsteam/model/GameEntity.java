package com.fullsteam.model;

import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;

/**
 * Base class for all game entities in the RTS game.
 */
@Getter
@Setter
public abstract class GameEntity {
    protected final long created = System.currentTimeMillis();

    protected int id;
    protected Body body;
    protected double health;
    protected double maxHealth;
    protected long expires = -1L;
    protected boolean active = true;

    public GameEntity(int id, Body body, double maxHealth) {
        this.id = id;
        this.body = body;
        this.health = maxHealth;
        this.maxHealth = maxHealth;

        // Configure body if provided
        if (body != null) {
            body.setAtRest(false);
            body.setAtRestDetectionEnabled(false);
            body.setEnabled(true);
            if (body.getUserData() == null) {
                body.setUserData(this);
            }
        }
    }

    public Vector2 getPosition() {
        return body != null ? body.getWorldCenter() : new Vector2();
    }

    public void setPosition(Vector2 position) {
        if (body != null) {
            body.getTransform().setTranslation(position);
        }
    }

    public double getRotation() {
        return body != null ? body.getTransform().getRotationAngle() : 0.0;
    }

    public void setRotation(double angle) {
        if (body != null) {
            body.getTransform().setRotation(angle);
        }
    }

    public void update(GameEntities gameEntities) {
        // default no-op
    }

    /**
     * Apply damage to this entity (implements Targetable interface).
     * Entities become inactive when health drops to or below zero.
     */
    public void takeDamage(double damage) {
        health -= damage;
        if (health <= 0) {
            active = false;
        }
    }
    
    /**
     * Helper method to apply damage and check if the entity was destroyed.
     * @return true if the entity became inactive as a result of this damage
     */
    public boolean takeDamageAndCheckDestroyed(double damage) {
        boolean wasActive = active;
        takeDamage(damage);
        return wasActive && !active;
    }

    public boolean isExpired() {
        if (!active) {
            return true;
        } else if (expires > 0) {
            return System.currentTimeMillis() > expires;
        } else {
            return false;
        }
    }

    /**
     * Get the remaining duration as a percentage
     */
    public double getDurationPercent() {
        if (expires <= created) {
            return 0; // No duration set
        }
        long totalDuration = expires - created;
        long remainingTime = expires - System.currentTimeMillis();
        return Math.max(0, Math.min(1, (double) remainingTime / totalDuration));
    }
}

