package com.fullsteam.model.component;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Building;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Component garrison unit behaviors
 * <p>
 * Used by: BUNKER
 */
@Getter
@Setter
public class GarrisonComponent extends AbstractBuildingComponent {

    private final List<Unit> garrisonedUnits = new ArrayList<>();
    private final int maxGarrisonCapacity;

    public GarrisonComponent(int maxGarrisonCapacity) {
        this.maxGarrisonCapacity = maxGarrisonCapacity;
    }

    @Override
    public void update(boolean hasLowPower) {
        if (!building.isActive()) {
            return;
        }
        fireBunkerWeapons(gameEntities, building);
    }

    /**
     * Garrison a unit inside this building (for Bunker)
     *
     * @return true if successfully garrisoned, false if full or not allowed
     */
    public boolean garrisonUnit(Unit unit) {
        if (garrisonedUnits.size() >= maxGarrisonCapacity) {
            return false; // Garrison is full
        }

        if (unit.getTeamNumber() != building.getTeamNumber()) {
            return false; // Can only garrison friendly units
        }

        // Only infantry units can be garrisoned
        if (!unit.getUnitType().isInfantry()) {
            return false;
        }

        garrisonedUnits.add(unit);
        unit.setGarrisoned(true);
        // set the unit's position with some entropy so they are in different "positions" inside the bunker
        // (This is mainly for visualization/debugging - actual firing happens from bunker position)
        Vector2 jitter = new Vector2(
                building.getPosition().x + (4 * ThreadLocalRandom.current().nextDouble() - 2),
                building.getPosition().y + (4 * ThreadLocalRandom.current().nextDouble() - 2)
        );
        unit.setPosition(jitter);
        // Don't set active=false! That would cause the unit to be deleted by removeInactiveEntities()
        // Instead, we'll filter garrisoned units from serialization
        unit.getBody().setEnabled(false); // Disable physics
        return true;
    }

    public Unit ungarrisonUnit(Unit unit) {
        if (garrisonedUnits.isEmpty()) {
            return null;
        }

        Unit toUngarrison;
        if (unit != null && garrisonedUnits.contains(unit)) {
            toUngarrison = unit;
            garrisonedUnits.remove(unit);
        } else {
            // Ungarrison first unit in list
            toUngarrison = garrisonedUnits.remove(0);
        }

        // Place unit near building exit
        Vector2 exitPos = calculateExitPosition();
        toUngarrison.getBody().getTransform().setTranslation(exitPos.x, exitPos.y);
        toUngarrison.setGarrisoned(false);
        // Unit is already active, just re-enable physics
        toUngarrison.getBody().setEnabled(true);
        return toUngarrison;
    }

    /**
     * Ungarrison all units from this building
     *
     * @return List of ungarrisoned units
     */
    public List<Unit> ungarrisonAllUnits() {
        List<Unit> ungarrisoned = new ArrayList<>();
        while (!garrisonedUnits.isEmpty()) {
            Unit unit = ungarrisonUnit(null);
            if (unit != null) {
                ungarrisoned.add(unit);
            }
        }
        return ungarrisoned;
    }

    /**
     * Calculate exit position to un-garrison units
     */
    private Vector2 calculateExitPosition() {
        Vector2 pos = building.getPosition();
        double size = building.getBody().getFixture(0).getShape().getRadius();
        // Place units at a random angle around the building
        double angle = Math.random() * Math.PI * 2;
        double distance = size + 30.0; // Place outside building radius
        return new Vector2(
                pos.x + Math.cos(angle) * distance,
                pos.y + Math.sin(angle) * distance
        );
    }

    /**
     * Get number of garrisoned units
     */
    public int getGarrisonCount() {
        return garrisonedUnits.size();
    }

    /**
     * Fire weapons from garrisoned units (called by RTSGameManager)
     * Each unit independently acquires targets and fires based on its own stats
     */
    public void fireBunkerWeapons(GameEntities gameEntities, Building building) {
        if (garrisonedUnits.isEmpty() || gameEntities == null) {
            return;
        }

        Vector2 bunkerPos = building.getPosition();

        // Each garrisoned unit operates independently
        for (Unit garrisonedUnit : garrisonedUnits) {
            // Skip if unit can't attack
            if (!garrisonedUnit.getUnitType().canAttack()) {
                continue;
            }

            // Skip if unit has no weapon
            if (garrisonedUnit.getWeapon() == null) {
                continue;
            }

            // Clear invalid target
            if (garrisonedUnit.getTargetUnit() != null && !garrisonedUnit.getTargetUnit().isActive()) {
                garrisonedUnit.setTargetUnit(null);
            }

            // Acquire target if needed (each unit finds its own target)
            if (garrisonedUnit.getTargetUnit() == null) {
                Unit target = findBunkerTargetForUnit(garrisonedUnit, gameEntities);
                garrisonedUnit.setTargetUnit(target);
            }

            Unit target = garrisonedUnit.getTargetUnit();
            if (target == null || !target.isActive()) {
                continue;
            }

            // Check if target is in range (use garrisoned unit's range)
            Vector2 targetPos = target.getPosition();
            double distance = bunkerPos.distance(targetPos);
            double attackRange = garrisonedUnit.getWeapon().getRange();

            if (distance > attackRange) {
                garrisonedUnit.setTargetUnit(null); // Target out of range
                continue;
            }

            // Fire weapon from bunker position (not from garrisoned unit's disabled body position)
            AbstractOrdinance ordinance = garrisonedUnit.getWeapon().fire(
                    bunkerPos,
                    targetPos,
                    garrisonedUnit.getId(),
                    garrisonedUnit.getTeamNumber(),
                    garrisonedUnit.getBody(),
                    gameEntities
            );
            
            if (ordinance != null) {
                gameEntities.add(ordinance);
            }
        }
    }

    /**
     * Find a target for a specific garrisoned unit
     * Each unit independently scans for enemies in its range
     */
    private Unit findBunkerTargetForUnit(Unit garrisonedUnit, GameEntities gameEntities) {
        Vector2 bunkerPos = garrisonedUnit.getPosition();
        double attackRange = garrisonedUnit.getUnitType().getAttackRange();

        Unit closestEnemy = null;
        double closestDistance = Double.MAX_VALUE;

        // Scan for enemies in range
        for (Unit enemy : gameEntities.getUnits().values()) {
            if (!enemy.isActive() || enemy.getTeamNumber() == garrisonedUnit.getTeamNumber()) {
                continue;
            }

            double distance = bunkerPos.distance(enemy.getPosition());
            if (distance <= attackRange && distance < closestDistance) {
                closestEnemy = enemy;
                closestDistance = distance;
            }
        }

        return closestEnemy;
    }
}




