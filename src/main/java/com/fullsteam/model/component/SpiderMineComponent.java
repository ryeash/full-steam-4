package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.FieldEffect;
import com.fullsteam.model.FieldEffectType;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Unit;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

/**
 * Component for SPIDER_MINE units.
 * Handles proximity detection and self-destruct explosion mechanics.
 * <p>
 * Spider mines continuously scan for nearby enemy units/buildings.
 * When an enemy is within detonation range, the mine explodes, dealing massive AOE damage
 * and destroying itself in the process.
 */
@Slf4j
public class SpiderMineComponent extends AbstractUnitComponent {

    private static final double DETONATION_RANGE = 25.0; // Range to trigger detonation
    private static final double EXPLOSION_RADIUS = 60.0; // Radius of the explosion
    private static final double ARMING_TIME = 0.5; // Time before mine becomes active (seconds)

    private double timeAlive = 0.0; // Track how long mine has been active
    private boolean armed = false;  // Mine must arm before it can detonate
    private boolean detonated = false; // Prevent multiple detonations

    @Override
    public void update(GameEntities gameEntities) {
        if (detonated || !unit.isActive()) {
            return;
        }

        // Update time alive
        timeAlive += getDeltaTime();

        // Check if mine should arm
        if (!armed && timeAlive >= ARMING_TIME) {
            armed = true;
            log.info("Spider Mine {} armed and ready", unit.getId());
        }

        // Only check for targets if armed
        if (armed) {
            checkForEnemiesInRange(gameEntities);
        }
    }

    /**
     * Check if any enemy units or buildings are within detonation range.
     * If found, detonate the mine.
     */
    private void checkForEnemiesInRange(GameEntities gameEntities) {
        Vector2 position = unit.getPosition();

        // Check enemy units
        for (Unit enemyUnit : gameEntities.getUnits().values()) {
            if (isValidTarget(enemyUnit) && isInRange(position, enemyUnit.getPosition())) {
                log.info("Spider Mine {} detected enemy unit {} - detonating!",
                        unit.getId(), enemyUnit.getId());
                detonate(gameEntities);
                return;
            }
        }

        // Check enemy buildings
        for (Building enemyBuilding : gameEntities.getBuildings().values()) {
            if (isValidTarget(enemyBuilding) && isInRange(position, enemyBuilding.getPosition())) {
                log.info("Spider Mine {} detected enemy building {} - detonating!",
                        unit.getId(), enemyBuilding.getId());
                detonate(gameEntities);
                return;
            }
        }
    }

    /**
     * Check if a unit is a valid target (enemy, active, not garrisoned).
     */
    private boolean isValidTarget(Unit target) {
        return target.getTeamNumber() != unit.getTeamNumber()
                && target.isActive()
                && !target.isGarrisoned();
    }

    /**
     * Check if a building is a valid target (enemy, active, not under construction).
     */
    private boolean isValidTarget(Building target) {
        return target.getTeamNumber() != unit.getTeamNumber()
                && target.isActive()
                && !target.isUnderConstruction();
    }

    /**
     * Check if target position is within detonation range.
     */
    private boolean isInRange(Vector2 minePos, Vector2 targetPos) {
        double distance = minePos.distance(targetPos);
        return distance <= DETONATION_RANGE;
    }

    /**
     * Detonate the spider mine, creating a large explosion and destroying the mine.
     */
    private void detonate(GameEntities gameEntities) {
        if (detonated) {
            return; // Already detonated
        }

        detonated = true;
        Vector2 position = unit.getPosition();

        // Create massive explosion field effect
        FieldEffect explosion = new FieldEffect(
                unit.getId(),
                FieldEffectType.EXPLOSION,
                position.copy(),
                EXPLOSION_RADIUS,
                unit.getUnitType().getDamage(), // Use unit's damage stat (150)
                FieldEffectType.EXPLOSION.getDefaultDuration(),
                unit.getTeamNumber()
        );
        gameEntities.add(explosion);

        log.info("Spider Mine {} detonated at ({}, {}) with {} damage and {} radius",
                unit.getId(), position.x, position.y, unit.getUnitType().getDamage(), EXPLOSION_RADIUS);

        // Destroy the spider mine
        unit.setActive(false);
        unit.setHealth(0);
    }

    @Override
    public void onDestroy() {
        // If destroyed before detonating (e.g., shot by enemy), do NOT create explosion
        // This prevents exploitation and makes mines more strategic
        if (!detonated) {
            log.info("Spider Mine {} destroyed before detonation (no explosion)", unit.getId());
        }
    }

    /**
     * Check if the mine is armed and ready to detonate.
     */
    public boolean isArmed() {
        return armed;
    }

    /**
     * Check if the mine has detonated.
     */
    public boolean hasDetonated() {
        return detonated;
    }

    /**
     * Get the detonation range for spider mines.
     */
    public static double getDetonationRange() {
        return DETONATION_RANGE;
    }

    /**
     * Get the explosion radius for spider mines.
     */
    public static double getExplosionRadius() {
        return EXPLOSION_RADIUS;
    }
}
