package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.Obstacle;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Component for units that can mine obstacles (Miners).
 * Handles pickaxe durability, mining damage, and repair cycles.
 */
@Slf4j
@Getter
@Setter
public class MineComponent extends AbstractUnitComponent {
    
    private static final double MAX_PICKAXE_DURABILITY = 100.0;
    private static final double PICKAXE_WEAR_RATE = 5.0;       // Durability lost per second of mining
    private static final double MINING_DAMAGE_RATE = 15.0;     // Damage dealt to obstacle per second
    private static final double REPAIR_RATE = 50.0;            // Durability repaired per second
    private static final double LOW_DURABILITY_THRESHOLD = 30.0; // When to return for repair
    
    private Obstacle targetObstacle = null;
    private double pickaxeDurability = MAX_PICKAXE_DURABILITY;

    @Override
    public void update(com.fullsteam.model.GameEntities gameEntities) {
        // Component doesn't do passive mining - mining is command-driven
        // This is here for future enhancements
    }

    /**
     * Mine an obstacle.
     *
     * @param obstacle The obstacle to mine
     * @return true if should stop mining (obstacle destroyed, pickaxe broken, or low durability)
     */
    public boolean mineObstacle(Obstacle obstacle) {
        if (pickaxeDurability <= 0) {
            log.info("Miner {} pickaxe broke, needs repair", unit.getId());
            return true; // Pickaxe broken, must repair
        }

        if (obstacle == null || !obstacle.isActive()) {
            log.debug("Miner {} target obstacle is gone", unit.getId());
            return true; // Obstacle destroyed or invalid
        }

        // Deal damage to obstacle
        double deltaTime = getDeltaTime();
        double damageDealt = MINING_DAMAGE_RATE * deltaTime;
        obstacle.takeDamage(damageDealt);
        
        // Wear down pickaxe
        pickaxeDurability -= PICKAXE_WEAR_RATE * deltaTime;
        pickaxeDurability = Math.max(0, pickaxeDurability);

        // Check if obstacle is destroyed
        if (!obstacle.isActive()) {
            log.info("Miner {} destroyed obstacle {}", unit.getId(), obstacle.getId());
            targetObstacle = null;
            return true; // Obstacle destroyed
        }

        // Check if pickaxe is getting low
        if (pickaxeDurability < LOW_DURABILITY_THRESHOLD) {
            log.debug("Miner {} pickaxe durability low ({}%), returning for repair", 
                    unit.getId(), (int) pickaxeDurability);
            return true; // Should return for repair
        }

        return false; // Continue mining
    }

    /**
     * Repair the pickaxe at a headquarters.
     *
     * @param headquarters The headquarters to repair at
     * @return true if repair is complete
     */
    public boolean repairPickaxe(Building headquarters) {
        if (headquarters == null || !headquarters.isActive()) {
            log.warn("Miner {} attempted to repair at inactive/null headquarters", unit.getId());
            return false;
        }

        if (headquarters.getBuildingType() != BuildingType.HEADQUARTERS) {
            log.warn("Miner {} attempted to repair at non-headquarters building", unit.getId());
            return false;
        }

        // Repair pickaxe
        pickaxeDurability += REPAIR_RATE * getDeltaTime();
        pickaxeDurability = Math.min(MAX_PICKAXE_DURABILITY, pickaxeDurability);

        // Check if repair is complete
        if (pickaxeDurability >= MAX_PICKAXE_DURABILITY) {
            log.info("Miner {} pickaxe fully repaired", unit.getId());
            return true;
        }

        return false; // Still repairing
    }

    /**
     * Check if pickaxe needs repair.
     *
     * @return true if durability is below threshold
     */
    public boolean needsRepair() {
        return pickaxeDurability < LOW_DURABILITY_THRESHOLD;
    }

    /**
     * Check if pickaxe is broken.
     *
     * @return true if durability is 0
     */
    public boolean isPickaxeBroken() {
        return pickaxeDurability <= 0;
    }

    /**
     * Get the pickaxe durability percentage (0.0 to 1.0).
     */
    public double getDurabilityPercent() {
        return pickaxeDurability / MAX_PICKAXE_DURABILITY;
    }

    /**
     * Set the target obstacle to mine.
     *
     * @param obstacle The obstacle to mine
     */
    public void setTargetObstacle(Obstacle obstacle) {
        if (obstacle != null && obstacle.isActive()) {
            this.targetObstacle = obstacle;
        } else {
            this.targetObstacle = null;
        }
    }

    @Override
    public void onDestroy() {
        // Clear target when unit is destroyed
        targetObstacle = null;
    }
}




