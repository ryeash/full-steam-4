package com.fullsteam.model.command;

import com.fullsteam.model.AIStance;
import com.fullsteam.model.Targetable;
import com.fullsteam.model.Unit;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Command for idle units (no active orders)
 * Idle combat units will scan for enemies based on their AI stance
 */
public class IdleCommand extends UnitCommand {

    private int scanCounter = 0; // Scan every N updates
    private final double defensiveLeashRange;

    public IdleCommand(Unit unit) {
        super(unit, false);
        this.defensiveLeashRange = unit.getVisionRange();
    }

    @Override
    public boolean update(double deltaTime) {
        // Idle combat units should scan for enemies based on AI stance
        if (unit.canCurrentlyAttack() && unit.getAiStance().isAutoAttack() && gameEntities != null) {
            scanCounter++;
            // Scan every 30 frames (~0.5 seconds at 60fps)
            if (scanCounter >= 30) {
                scanCounter = 0;
                scanForEnemies();
            }
        }

        return true;
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

        // Check for defensive leash (don't chase too far from home)
        if (unit.getAiStance() == AIStance.DEFENSIVE && unit.getHomePosition() != null) {
            double distanceFromHome = currentPos.distance(unit.getHomePosition());
            if (distanceFromHome > defensiveLeashRange) {
                unit.setCurrentCommand(new AttackMoveCommand(unit, unit.getHomePosition(), false));
                return;
            }
        }

        Targetable nearestEnemy = gameEntities.findNearestEnemyTargetable(unit);
        // For defensive stance, check if enemy is within leash range of home
        if (nearestEnemy != null && unit.getAiStance() == AIStance.DEFENSIVE && unit.getHomePosition() != null) {
            double enemyDistanceFromHome = nearestEnemy.getPosition().distance(unit.getHomePosition());
            if (enemyDistanceFromHome > defensiveLeashRange) {
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

