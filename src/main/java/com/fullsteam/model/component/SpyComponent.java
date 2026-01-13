package com.fullsteam.model.component;

import com.fullsteam.model.GameEntities;
import com.fullsteam.model.TrackerBug;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Component for SPY units.
 * Handles automatic permanent cloaking and tracker gun functionality.
 * <p>
 * The spy is permanently cloaked and automatically re-cloaks after detection.
 * The spy can fire a tracker gun to attach a "bug" to enemy units, granting vision.
 */
@Slf4j
@Getter
public class SpyComponent extends AbstractUnitComponent {

    private static final long CLOAK_REACTIVATION_DELAY_MS = 3000; // 3 seconds after detection
    private static final long TRACKER_GUN_COOLDOWN_MS = 10000; // 10 seconds between shots
    private static final long TRACKER_BUG_DURATION_MS = 60000; // 60 seconds (1 minute)
    private static final double TRACKER_BUG_VISION_RANGE = 400.0; // Vision granted around tagged unit
    private static final int MAX_TRACKER_SHOTS = 3; // Limited ammo

    private long lastDetectionTime = 0;
    private long lastTrackerShotTime = 0;
    private int remainingTrackerShots = MAX_TRACKER_SHOTS;

    @Override
    public void update(GameEntities gameEntities) {
        if (!unit.isActive()) {
            return;
        }

        // Auto-cloak system: Check if we should re-cloak after detection
        if (!unit.isCloaked()) {
            long now = System.currentTimeMillis();
            if (now - lastDetectionTime >= CLOAK_REACTIVATION_DELAY_MS) {
                activateCloak();
            }
        }
    }

    /**
     * Activate cloaking (turn invisible).
     */
    private void activateCloak() {
        unit.getComponent(CloakComponent.class)
                .filter(c -> !c.isCloaked())
                .ifPresent(CloakComponent::toggleCloak);
    }

    /**
     * Fire the tracker gun at an enemy unit, attaching a tracking device.
     *
     * @param target       The enemy unit to tag
     * @param gameEntities Game entities to add tracker bug to
     * @return true if tracker was fired successfully
     */
    public boolean fireTrackerGun(Unit target, GameEntities gameEntities) {
        // Check if we have shots remaining
        if (remainingTrackerShots <= 0) {
            log.info("Spy {} has no tracker shots remaining", unit.getId());
            return false;
        }

        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastTrackerShotTime < TRACKER_GUN_COOLDOWN_MS) {
            log.info("Spy {} tracker gun on cooldown", unit.getId());
            return false;
        }

        // Check if target is valid (enemy, alive, not garrisoned)
        if (target.getTeamNumber() == unit.getTeamNumber()) {
            log.info("Spy {} cannot tag friendly unit", unit.getId());
            return false;
        }

        if (!target.isActive() || target.isGarrisoned()) {
            log.info("Spy {} cannot tag inactive/garrisoned unit", unit.getId());
            return false;
        }

        // Check if target already has a tracker bug from this player
        boolean alreadyTagged = gameEntities.getTrackerBugs().values().stream()
                .anyMatch(bug -> bug.getTargetUnitId() == target.getId() &&
                        bug.getOwnerPlayerId() == unit.getOwnerId() &&
                        bug.isActive());

        if (alreadyTagged) {
            log.info("Spy {} target already has a tracker bug", unit.getId());
            return false;
        }

        // Fire tracker gun!
        TrackerBug bug = new TrackerBug(
                target.getId(),
                unit.getOwnerId(),
                unit.getTeamNumber(),
                TRACKER_BUG_VISION_RANGE,
                TRACKER_BUG_DURATION_MS
        );
        gameEntities.addTrackerBug(bug);

        remainingTrackerShots--;
        lastTrackerShotTime = now;

        log.info("Spy {} fired tracker gun at unit {} ({} shots remaining)",
                unit.getId(), target.getId(), remainingTrackerShots);

        // Firing reveals the spy temporarily (like Cloak Tank)
        lastDetectionTime = System.currentTimeMillis();
        unit.getComponent(CloakComponent.class)
                .filter(CloakComponent::isCloaked)
                .ifPresent(CloakComponent::toggleCloak);

        return true;
    }
}
