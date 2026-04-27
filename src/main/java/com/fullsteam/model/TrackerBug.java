package com.fullsteam.model;

import com.fullsteam.games.IdGenerator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

/**
 * Represents a tracking device attached to an enemy unit or building by a Spy.
 * Provides vision of the tagged entity's location to the spy's team.
 * <p>
 * TrackerBugs are not physical entities in the world — they're metadata that grant
 * vision at the host entity's position.
 */
@Slf4j
@Getter
@Setter
public class TrackerBug {

    public enum AttachmentKind {
        UNIT,
        BUILDING
    }

    private final int id;
    /** ID of the tagged unit or building (see {@link #attachmentKind}). */
    private final int targetId;
    private final AttachmentKind attachmentKind;
    private final int ownerPlayerId;
    private final int ownerTeamNumber;
    private final double visionRange;
    private final long createdTime;
    private final long duration;
    private boolean active = true;

    /**
     * @param targetId          ID of tagged unit or building
     * @param attachmentKind    whether {@code targetId} refers to a unit or building
     * @param ownerPlayerId     Player who owns the spy
     * @param ownerTeamNumber   Team that receives vision
     * @param visionRange       Vision radius around tagged entity
     * @param durationMs        Bug duration (milliseconds)
     */
    public TrackerBug(int targetId, AttachmentKind attachmentKind, int ownerPlayerId, int ownerTeamNumber,
                      double visionRange, long durationMs) {
        this.id = IdGenerator.nextEntityId();
        this.targetId = targetId;
        this.attachmentKind = attachmentKind;
        this.ownerPlayerId = ownerPlayerId;
        this.ownerTeamNumber = ownerTeamNumber;
        this.visionRange = visionRange;
        this.createdTime = System.currentTimeMillis();
        this.duration = durationMs;
    }

    public boolean update() {
        if (!active) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - createdTime >= duration) {
            active = false;
            log.info("Tracker bug {} expired after {} seconds", id, duration / 1000.0);
            return false;
        }

        return true;
    }

    /**
     * World position used for vision (center of tagged unit or building).
     *
     * @return position, or null if the tagged entity is gone or inactive
     */
    public Vector2 getPosition(GameEntities gameEntities) {
        return switch (attachmentKind) {
            case UNIT -> {
                Unit target = gameEntities.getUnits().get(targetId);
                yield (target != null && target.isActive()) ? target.getPosition() : null;
            }
            case BUILDING -> {
                Building b = gameEntities.getBuildings().get(targetId);
                yield (b != null && b.isActive()) ? b.getPosition() : null;
            }
        };
    }

    public boolean isTargetAlive(GameEntities gameEntities) {
        return getPosition(gameEntities) != null;
    }

    public void deactivate() {
        active = false;
        log.info("Tracker bug {} deactivated", id);
    }
}
