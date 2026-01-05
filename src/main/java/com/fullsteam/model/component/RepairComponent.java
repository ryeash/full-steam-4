package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Component for units that can repair buildings and vehicles (Engineer).
 * Handles repair abilities with cooldown management.
 */
@Slf4j
@Getter
public class RepairComponent extends AbstractUnitComponent {

    private static final double REPAIR_AMOUNT_UNIT = 25.0;       // HP repaired per use (units)
    private static final double REPAIR_AMOUNT_BUILDING = 30.0;   // HP repaired per use (buildings)
    private static final double REPAIR_RANGE = 100.0;            // Range for repairing
    private static final long REPAIR_COOLDOWN_MS = 500;          // 0.5 seconds between repairs

    private long lastRepairTime = 0;

    @Override
    public void update(com.fullsteam.model.GameEntities gameEntities) {
        // Repair component doesn't do passive repairs - repairs are command-driven or explicit
    }

    /**
     * Repair a target unit (vehicle).
     *
     * @param target The unit to repair
     * @return true if repair was successful
     */
    public boolean repairUnit(Unit target) {
        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastRepairTime < REPAIR_COOLDOWN_MS) {
            return false; // Still on cooldown
        }

        // Validate target
        if (target == null || !target.isActive()) {
            return false;
        }

        // Can't repair enemies
        if (target.getTeamNumber() != unit.getTeamNumber()) {
            log.warn("Engineer {} attempted to repair enemy unit {}", unit.getId(), target.getId());
            return false;
        }

        // Can't repair if already at full health
        if (target.getHealth() >= target.getMaxHealth()) {
            return false;
        }

        // Check range
        double distance = unit.getPosition().distance(target.getPosition());
        if (distance > REPAIR_RANGE) {
            log.debug("Engineer {} out of range to repair {}", unit.getId(), target.getId());
            return false;
        }

        // Repair the target
        double newHealth = Math.min(target.getMaxHealth(), target.getHealth() + REPAIR_AMOUNT_UNIT);
        target.setHealth(newHealth);
        lastRepairTime = now;

        log.debug("Engineer {} repaired unit {} for {} HP (now at {}/{})",
                unit.getId(), target.getId(), REPAIR_AMOUNT_UNIT, (int) newHealth, (int) target.getMaxHealth());
        return true;
    }

    /**
     * Repair a target building.
     *
     * @param target The building to repair
     * @return true if repair was successful
     */
    public boolean repairBuilding(Building target) {
        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastRepairTime < REPAIR_COOLDOWN_MS) {
            return false; // Still on cooldown
        }

        // Validate target
        if (target == null || !target.isActive()) {
            return false;
        }

        // Can't repair enemies
        if (target.getTeamNumber() != unit.getTeamNumber()) {
            log.warn("Engineer {} attempted to repair enemy building {}", unit.getId(), target.getId());
            return false;
        }

        // Can't repair if already at full health
        if (target.getHealth() >= target.getMaxHealth()) {
            return false;
        }

        // Check range
        double distance = unit.getPosition().distance(target.getPosition());
        if (distance > REPAIR_RANGE + target.getBuildingType().getSize()) {
            log.debug("Engineer {} out of range to repair building {}", unit.getId(), target.getId());
            return false;
        }

        // Repair the building
        double newHealth = Math.min(target.getMaxHealth(), target.getHealth() + REPAIR_AMOUNT_BUILDING);
        target.setHealth(newHealth);
        lastRepairTime = now;

        log.debug("Engineer {} repaired building {} for {} HP (now at {}/{})",
                unit.getId(), target.getId(), REPAIR_AMOUNT_BUILDING, (int) newHealth, (int) target.getMaxHealth());
        return true;
    }

    /**
     * Check if this engineer can repair a unit.
     *
     * @param target The target to check
     * @return true if target is valid and in range
     */
    public boolean canRepairUnit(Unit target) {
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
        return distance <= REPAIR_RANGE;
    }

    /**
     * Check if this engineer can repair a building.
     *
     * @param target The target to check
     * @return true if target is valid and in range
     */
    public boolean canRepairBuilding(Building target) {
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
        return distance <= REPAIR_RANGE + target.getBuildingType().getSize();
    }

    /**
     * Check if repair is off cooldown.
     *
     * @return true if ready to repair
     */
    public boolean isReady() {
        long now = System.currentTimeMillis();
        return now - lastRepairTime >= REPAIR_COOLDOWN_MS;
    }

    /**
     * Get the repair range.
     *
     * @return Repair range in world units
     */
    public static double getRepairRange() {
        return REPAIR_RANGE;
    }
}
