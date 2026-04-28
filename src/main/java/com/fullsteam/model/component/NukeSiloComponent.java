package com.fullsteam.model.component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Nuclear silo arm / launch state. Used by {@link com.fullsteam.model.BuildingType#NUKE_SILO}.
 * <p>Flow: {@link NukeArmState#IDLE} → {@link NukeArmState#ARMING} (timed) → {@link NukeArmState#ARMED} → launch → {@link NukeArmState#IDLE}.
 */
@Slf4j
public class NukeSiloComponent extends AbstractBuildingComponent {

    /** Wall-clock arming duration (ms). */
    public static final long ARM_DURATION_MS = 120_000L;

    @Getter
    private NukeArmState armState = NukeArmState.IDLE;

    private long armCompletesAtEpochMs;

    @Override
    public void update(boolean hasLowPower) {
        if (building.isUnderConstruction() || !building.isActive()) {
            return;
        }
        if (armState == NukeArmState.ARMING && System.currentTimeMillis() >= armCompletesAtEpochMs) {
            armState = NukeArmState.ARMED;
            log.debug("Nuke silo {} warhead armed and ready to launch", building.getId());
        }
    }

    /**
     * @return true if arming started from {@link NukeArmState#IDLE}
     */
    public boolean tryStartArming() {
        if (armState != NukeArmState.IDLE) {
            return false;
        }
        armState = NukeArmState.ARMING;
        armCompletesAtEpochMs = System.currentTimeMillis() + ARM_DURATION_MS;
        log.info("Nuke silo {} began arming; completes at {}", building.getId(), armCompletesAtEpochMs);
        return true;
    }

    public boolean isLaunchReady() {
        return armState == NukeArmState.ARMED;
    }

    public boolean canStartArming() {
        return armState == NukeArmState.IDLE;
    }

    public boolean isArming() {
        return armState == NukeArmState.ARMING;
    }

    /** Remaining arming time (ms), or 0 if not {@link NukeArmState#ARMING}. */
    public long getArmingRemainingMs(long nowEpochMs) {
        if (armState != NukeArmState.ARMING) {
            return 0L;
        }
        return Math.max(0L, armCompletesAtEpochMs - nowEpochMs);
    }

    /** After a successful launch, return to idle so the player must arm again. */
    public void consumeLaunchReadiness() {
        armState = NukeArmState.IDLE;
        armCompletesAtEpochMs = 0L;
    }

    @Override
    public void onDestroy() {
        armState = NukeArmState.IDLE;
        armCompletesAtEpochMs = 0L;
    }

    public enum NukeArmState {
        IDLE,
        ARMING,
        ARMED
    }
}
