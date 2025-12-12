package com.fullsteam.model.component;

import com.fullsteam.model.AIStance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Component for units that can cloak (Cloak Tank).
 * Handles invisibility mechanics, detection range, and AI stance management while cloaked.
 */
@Slf4j
@Getter
public class CloakComponent extends AbstractUnitComponent {

    public static final double CLOAK_DETECTION_RANGE = 50.0; // Range at which cloaked units are detected
    private static final long TOGGLE_COOLDOWN_MS = 3000; // 3 seconds
    private static final long CLOAK_DELAY_AFTER_FIRE_MS = 2000; // 2 seconds delay after firing before cloak activates

    private boolean cloaked = false;
    private long lastToggleTime = 0;
    private long lastFireTime = 0;
    private AIStance preCloakAIStance = null; // Saved AI stance before cloaking

    @Override
    public void update(com.fullsteam.model.GameEntities gameEntities) {
        // Cloak component doesn't need passive updates
        // Cloak state is managed by toggle and fire actions
    }

    /**
     * Toggle cloak state.
     *
     * @return true if successfully toggled, false if on cooldown
     */
    public boolean toggleCloak() {
        long now = System.currentTimeMillis();
        if (now - lastToggleTime < TOGGLE_COOLDOWN_MS) {
            return false; // Still on cooldown
        }

        cloaked = !cloaked;
        lastToggleTime = now;

        if (cloaked) {
            activateCloak();
        } else {
            deactivateCloak();
        }

        return true;
    }

    /**
     * Activate cloaking (turn invisible).
     */
    private void activateCloak() {
        log.info("Unit {} activated cloak", unit.getId());

        // Reset last fire time to start cloak immediately
        lastFireTime = 0;

        // Save current AI stance and switch to PASSIVE to avoid accidental reveals
        AIStance currentStance = unit.getAiStance();
        if (currentStance != AIStance.PASSIVE) {
            preCloakAIStance = currentStance;
            unit.setAiStance(AIStance.PASSIVE);
            log.info("Unit {} switched to PASSIVE stance while cloaked (saved: {})",
                    unit.getId(), preCloakAIStance);
        }
    }

    /**
     * Deactivate cloaking (become visible).
     */
    private void deactivateCloak() {
        log.info("Unit {} deactivated cloak", unit.getId());

        // Restore previous AI stance
        if (preCloakAIStance != null) {
            unit.setAiStance(preCloakAIStance);
            log.info("Unit {} restored AI stance to {}", unit.getId(), preCloakAIStance);
            preCloakAIStance = null;
        }
    }

    /**
     * Check if this unit is currently cloaked.
     *
     * @return true if cloaked and enough time has passed since last fire
     */
    public boolean isCloaked() {
        if (!cloaked) {
            return false;
        }

        // Cloak doesn't activate immediately after firing (delay period)
        long timeSinceLastFire = System.currentTimeMillis() - lastFireTime;
        return timeSinceLastFire >= CLOAK_DELAY_AFTER_FIRE_MS;
    }

    /**
     * Called when this unit fires a weapon.
     * Firing breaks cloak temporarily.
     */
    public void onFire() {
        lastFireTime = System.currentTimeMillis();
        onDetected();
    }

    /**
     * Called when this unit is detected by an enemy.
     * Forces cloak to deactivate.
     */
    public void onDetected() {
        if (cloaked) {
            log.info("Unit {} detected by enemy - cloak deactivated", unit.getId());
            cloaked = false;
            deactivateCloak();
        }
    }

    /**
     * Check if cloak is currently active (toggle is on, even if temporarily broken by firing).
     *
     * @return true if cloak is toggled on
     */
    public boolean isCloakActive() {
        return cloaked;
    }

    /**
     * Get the detection range for cloaked units.
     *
     * @return Detection range in world units
     */
    public static double getDetectionRange() {
        return CLOAK_DETECTION_RANGE;
    }

    @Override
    public void onDestroy() {
        // Clean up
        cloaked = false;
        preCloakAIStance = null;
    }

    @Override
    public void onGarrison() {
        // Force decloak if garrisoned
        if (cloaked) {
            cloaked = false;
            deactivateCloak();
            log.debug("Unit {} decloaked due to garrison", unit.getId());
        }
    }
}




