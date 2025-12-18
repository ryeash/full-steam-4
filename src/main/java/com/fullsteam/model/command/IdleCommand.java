package com.fullsteam.model.command;

import com.fullsteam.model.AIStance;
import com.fullsteam.model.Unit;
import org.dyn4j.geometry.Vector2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Command for idle units (no active orders)
 * Idle combat units will scan for enemies based on their AI stance
 */
public class IdleCommand extends UnitCommand {

    private static final Logger log = LoggerFactory.getLogger(IdleCommand.class);

    private int scanCounter = 0; // Scan every N updates

    public IdleCommand(Unit unit) {
        super(unit, false); // Idle is never a player order
    }

    @Override
    public boolean update(double deltaTime) {
        // Idle combat units should scan for enemies based on AI stance
        // Use canCurrentlyAttack() to handle special cases (e.g., Crawler must be deployed)
        if (unit.canCurrentlyAttack() && unit.getAiStance().isAutoAttack() && gameEntities != null) {
            scanCounter++;

            // Scan every 30 frames (~0.5 seconds at 60fps)
            if (scanCounter >= 30) {
                scanCounter = 0;
                scanForEnemies();
            }
        }

        return true; // Always active (until replaced by another command)
    }

    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        // No movement for idle units
        unit.getBody().setLinearVelocity(0, 0);
    }

    /**
     * Scan for enemies and auto-engage based on AI stance
     * Uses gameEntities to find nearby enemies
     */
    private void scanForEnemies() {
        Vector2 currentPos = unit.getPosition();
        double visionRange = unit.getUnitType().getVisionRange(); // Vision range (varies by unit type)

        // Debug logging for flak tanks
        if (unit.getUnitType() == com.fullsteam.model.UnitType.FLAK_TANK) {
            log.info("Flak Tank {} scanning for enemies (vision range: {}, position: ({}, {}))",
                    unit.getId(), visionRange, currentPos.x, currentPos.y);
        }

        // Check for defensive leash (don't chase too far from home)
        if (unit.getAiStance() == AIStance.DEFENSIVE && unit.getHomePosition() != null) {
            double distanceFromHome = currentPos.distance(unit.getHomePosition());
            if (distanceFromHome > 300.0) { // DEFENSIVE_LEASH_RANGE
                // Too far from home, don't engage
                return;
            }
        }

        // Use unified targetable finder - automatically handles units, buildings, walls
        // and respects elevation targeting and cloak detection
        com.fullsteam.model.Targetable nearestEnemy = gameEntities.findNearestEnemyTargetable(
                unit);

        // Debug logging for flak tanks
        if (unit.getUnitType() == com.fullsteam.model.UnitType.FLAK_TANK) {
            if (nearestEnemy != null) {
                log.info("Flak Tank {} found target: {} {} (elevation: {}, distance: {})",
                        unit.getId(), nearestEnemy.getTargetType(), nearestEnemy.getId(),
                        nearestEnemy.getElevation(),
                        currentPos.distance(nearestEnemy.getPosition()));
            } else {
                log.info("Flak Tank {} found no targetable enemies in range", unit.getId());
            }
        }

        // For defensive stance, check if enemy is within leash range of home
        if (nearestEnemy != null && unit.getAiStance() == AIStance.DEFENSIVE && unit.getHomePosition() != null) {
            double enemyDistanceFromHome = nearestEnemy.getPosition().distance(unit.getHomePosition());
            if (enemyDistanceFromHome > 300.0) { // DEFENSIVE_LEASH_RANGE
                nearestEnemy = null; // Ignore this enemy
            }
        }

        // If found an enemy, engage it with unified attack command
        if (nearestEnemy != null) {
            unit.issueCommand(new AttackTargetableCommand(unit, nearestEnemy, false), gameEntities);
        }
    }

    @Override
    public String getDescription() {
        return "Idle";
    }
}

