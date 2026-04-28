package com.fullsteam.model.component;

import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Component for units that can heal other units (Medic).
 * Handles healing abilities with cooldown management.
 */
@Slf4j
@Getter
public class HealComponent extends AbstractUnitComponent {

    private static final double HEAL_AMOUNT = 20.0;      // HP healed per use
    private static final double HEAL_RANGE = 100.0;      // Range for healing
    private static final long HEAL_COOLDOWN_MS = 500;    // 0.5 seconds between heals

    private long lastHealTime = 0;

    @Override
    public void update(com.fullsteam.model.GameEntities gameEntities) {
        // Heal component doesn't do passive healing - heals are command-driven or explicit
    }

    /**
     * Check if heal is off cooldown.
     *
     * @return true if ready to heal
     */
    public boolean isReady() {
        long now = System.currentTimeMillis();
        return now - lastHealTime >= HEAL_COOLDOWN_MS;
    }
}
