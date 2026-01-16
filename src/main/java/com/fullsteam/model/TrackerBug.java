package com.fullsteam.model;

import com.fullsteam.games.IdGenerator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

/**
 * Represents a tracking device attached to an enemy unit by a Spy.
 * Provides vision of the tagged unit's location to the spy's team.
 * <p>
 * TrackerBugs are not physical entities in the world - they're metadata attached to units
 * that grant vision to enemy teams.
 */
@Slf4j
@Getter
@Setter
public class TrackerBug {
    private final int id;
    private final int targetUnitId; // Unit this bug is attached to
    private final int ownerPlayerId; // Player who owns the spy that placed this
    private final int ownerTeamNumber; // Team that gets vision from this bug
    private final double visionRange; // Vision radius granted around the tagged unit
    private final long createdTime; // When bug was placed
    private final long duration; // How long bug lasts (milliseconds)
    private boolean active = true;

    /**
     * Create a tracker bug attached to an enemy unit.
     *
     * @param targetUnitId    ID of unit this bug is attached to
     * @param ownerPlayerId   Player who owns the spy
     * @param ownerTeamNumber Team that gets vision
     * @param visionRange     Vision radius around tagged unit
     * @param durationMs      How long bug lasts (milliseconds)
     */
    public TrackerBug(int targetUnitId, int ownerPlayerId, int ownerTeamNumber,
                      double visionRange, long durationMs) {
        this.id = IdGenerator.nextEntityId();
        this.targetUnitId = targetUnitId;
        this.ownerPlayerId = ownerPlayerId;
        this.ownerTeamNumber = ownerTeamNumber;
        this.visionRange = visionRange;
        this.createdTime = System.currentTimeMillis();
        this.duration = durationMs;
    }

    /**
     * Update tracker bug state - check if it should expire.
     *
     * @return true if bug is still active
     */
    public boolean update() {
        if (!active) {
            return false;
        }

        // Check if bug has expired
        long now = System.currentTimeMillis();
        if (now - createdTime >= duration) {
            active = false;
            log.info("Tracker bug {} expired after {} seconds", id, duration / 1000.0);
            return false;
        }

        return true;
    }

    /**
     * Get the position of this tracker bug (position of tagged unit).
     *
     * @param gameEntities Game entities to look up target unit
     * @return Position of tagged unit, or null if unit is dead/missing
     */
    public Vector2 getPosition(GameEntities gameEntities) {
        Unit target = gameEntities.getUnits().get(targetUnitId);
        if (target != null && target.isActive()) {
            return target.getPosition();
        }
        return null; // Target is dead or missing
    }

    /**
     * Check if the target unit is still alive.
     *
     * @param gameEntities Game entities to look up target unit
     * @return true if target is alive
     */
    public boolean isTargetAlive(GameEntities gameEntities) {
        Unit target = gameEntities.getUnits().get(targetUnitId);
        return target != null && target.isActive();
    }

    /**
     * Deactivate this tracker bug (e.g., target died or bug was detected).
     */
    public void deactivate() {
        active = false;
        log.info("Tracker bug {} deactivated", id);
    }
}
