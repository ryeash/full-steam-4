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
     * Heal a target unit.
     *
     * @param target The unit to heal
     * @return true if heal was successful
     */
    public boolean healUnit(Unit target) {
        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastHealTime < HEAL_COOLDOWN_MS) {
            return false; // Still on cooldown
        }

        // Validate target
        if (target == null || !target.isActive()) {
            return false;
        }

        // Can't heal enemies
        if (target.getTeamNumber() != unit.getTeamNumber()) {
            log.warn("Medic {} attempted to heal enemy unit {}", unit.getId(), target.getId());
            return false;
        }

        // Can't heal if already at full health
        if (target.getHealth() >= target.getMaxHealth()) {
            return false;
        }

        // Check range
        double distance = unit.getPosition().distance(target.getPosition());
        if (distance > HEAL_RANGE) {
            log.debug("Medic {} out of range to heal {}", unit.getId(), target.getId());
            return false;
        }

        // Heal the target
        double newHealth = Math.min(target.getMaxHealth(), target.getHealth() + HEAL_AMOUNT);
        target.setHealth(newHealth);
        lastHealTime = now;

        log.debug("Medic {} healed {} for {} HP (now at {}/{})",
                unit.getId(), target.getId(), HEAL_AMOUNT, (int) newHealth, (int) target.getMaxHealth());
        return true;
    }

    /**
     * Check if this medic can heal a target.
     *
     * @param target The target to check
     * @return true if target is valid and in range
     */
    public boolean canHeal(Unit target) {
        if (target == null || !target.isActive()) {
            return false;
        }
        if (target.getTeamNumber() != unit.getTeamNumber()) {
            return false;
        }
        if (target.getHealth() >= target.getMaxHealth()) {
            return false;
        }
        double distance = unit.getPosition().distance(target.getPosition());
        return distance <= HEAL_RANGE;
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

    /**
     * Get the heal range.
     *
     * @return Heal range in world units
     */
    public static double getHealRange() {
        return HEAL_RANGE;
    }
}





