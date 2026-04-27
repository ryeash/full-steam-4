package com.fullsteam.model.component;

import com.fullsteam.model.GameEntities;
import com.fullsteam.model.PlayerFaction;
import com.fullsteam.model.Targetable;
import com.fullsteam.model.Unit;
import com.fullsteam.model.command.IdleCommand;
import com.fullsteam.model.customization.FactionPerk;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Component for APC units (Armored Personnel Carrier).
 * Provides mobile garrison functionality similar to bunkers.
 * <p>
 * Key differences from bunker:
 * - Mobile (moves with the APC)
 * - When APC is destroyed, all garrisoned units are destroyed
 * - Infantry can fire from inside while moving
 */
@Slf4j
@Getter
@Setter
public class APCComponent extends AbstractUnitComponent {

    private static final int BASE_GARRISON_CAPACITY = 3;
    private static final int GARRISON_MASTERY_CAPACITY = 5;

    private final List<Unit> garrisonedUnits = new ArrayList<>();

    @Override
    public void update(GameEntities gameEntities) {
        if (!unit.isActive()) {
            return;
        }

        // Update garrisoned unit positions to follow APC
        updateGarrisonedUnitPositions();

        // Fire weapons from garrisoned units
        fireGarrisonWeapons(gameEntities);
    }

    /**
     * Get max garrison capacity (affected by GARRISON_MASTERY perk).
     */
    public int getMaxGarrisonCapacity() {
        // Check if owner has GARRISON_MASTERY perk
        if (gameEntities != null) {
            PlayerFaction faction = gameEntities.getPlayerFactions().get(unit.getOwnerId());
            if (faction != null && faction.getFactionDefinition().getActivePerks().contains(FactionPerk.GARRISON_MASTERY)) {
                return GARRISON_MASTERY_CAPACITY;
            }
        }
        return BASE_GARRISON_CAPACITY;
    }

    /**
     * Garrison a unit inside this APC.
     *
     * @return true if successfully garrisoned, false if full or not allowed
     */
    public boolean garrisonUnit(Unit unitToGarrison) {
        if (garrisonedUnits.size() >= getMaxGarrisonCapacity()) {
            return false; // Garrison is full
        }

        if (unitToGarrison.getTeamNumber() != unit.getTeamNumber()) {
            return false; // Can only garrison friendly units
        }

        // Only infantry units can be garrisoned
        if (!unitToGarrison.getUnitType().isInfantry()) {
            return false;
        }

        garrisonedUnits.add(unitToGarrison);
        unitToGarrison.setGarrisoned(true);

        // Set unit position inside APC with jitter (for visualization)
        Vector2 jitter = new Vector2(
                unit.getPosition().x + (4 * ThreadLocalRandom.current().nextDouble() - 2),
                unit.getPosition().y + (4 * ThreadLocalRandom.current().nextDouble() - 2)
        );
        unitToGarrison.setPosition(jitter);

        // Disable physics but keep unit active
        unitToGarrison.getBody().setEnabled(false);

        log.info("Unit {} garrisoned in APC {} ({}/{})",
                unitToGarrison.getId(), unit.getId(), garrisonedUnits.size(), getMaxGarrisonCapacity());

        return true;
    }

    /**
     * Ungarrison a specific unit or the first unit.
     *
     * @param unitToUngarrison Specific unit to ungarrison, or null for first unit
     * @return The ungarrisoned unit, or null if garrison is empty
     */
    public Unit ungarrisonUnit(Unit unitToUngarrison) {
        if (garrisonedUnits.isEmpty()) {
            return null;
        }

        Unit toUngarrison;
        if (unitToUngarrison != null && garrisonedUnits.contains(unitToUngarrison)) {
            toUngarrison = unitToUngarrison;
            garrisonedUnits.remove(unitToUngarrison);
        } else {
            // Ungarrison first unit in list
            toUngarrison = garrisonedUnits.remove(0);
        }

        // Place unit near APC exit
        Vector2 exitPos = calculateExitPosition();
        toUngarrison.getBody().getTransform().setTranslation(exitPos.x, exitPos.y);
        toUngarrison.setGarrisoned(false);
        toUngarrison.getBody().setEnabled(true);
        toUngarrison.issueCommand(new IdleCommand(toUngarrison), gameEntities);

        log.info("Unit {} ungarrisoned from APC {} ({}/{})",
                toUngarrison.getId(), unit.getId(), garrisonedUnits.size(), getMaxGarrisonCapacity());

        return toUngarrison;
    }

    /**
     * Ungarrison all units from this APC.
     *
     * @return List of ungarrisoned units
     */
    public List<Unit> ungarrisonAllUnits() {
        List<Unit> ungarrisoned = new ArrayList<>();
        while (!garrisonedUnits.isEmpty()) {
            Unit ungarrisonedUnit = ungarrisonUnit(null);
            if (ungarrisonedUnit != null) {
                ungarrisoned.add(ungarrisonedUnit);
            }
        }
        return ungarrisoned;
    }

    /**
     * Remove a unit from this APC without spawning it (e.g. desertion / elimination).
     */
    public boolean removeGarrisonedUnitForDesertion(Unit u) {
        if (!garrisonedUnits.remove(u)) {
            return false;
        }
        u.setGarrisoned(false);
        return true;
    }

    /**
     * Calculate exit position to ungarrison units (behind the APC).
     */
    private Vector2 calculateExitPosition() {
        Vector2 pos = unit.getPosition();
        double size = unit.getUnitType().getSize();

        // Place units behind the APC (rear door/ramp)
        // Use velocity to determine "back" direction, or use random if stationary
        Vector2 velocity = unit.getBody().getLinearVelocity();
        double angle;

        if (velocity.getMagnitude() > 1.0) {
            // APC is moving - place behind movement direction
            angle = Math.atan2(velocity.y, velocity.x) + Math.PI; // Opposite of movement
        } else {
            // APC is stationary - place at random angle
            angle = Math.random() * Math.PI * 2;
        }

        double distance = size + 25.0; // Place outside APC radius
        return new Vector2(
                pos.x + Math.cos(angle) * distance,
                pos.y + Math.sin(angle) * distance
        );
    }

    /**
     * Update garrisoned unit positions to follow APC as it moves.
     */
    private void updateGarrisonedUnitPositions() {
        if (garrisonedUnits.isEmpty()) {
            return;
        }

        Vector2 apcPos = unit.getPosition();
        for (Unit garrisonedUnit : garrisonedUnits) {
            // Keep units at APC position with slight jitter
            Vector2 jitter = new Vector2(
                    apcPos.x + (4 * ThreadLocalRandom.current().nextDouble() - 2),
                    apcPos.y + (4 * ThreadLocalRandom.current().nextDouble() - 2)
            );
            garrisonedUnit.setPosition(jitter);
        }
    }

    /**
     * Fire weapons from garrisoned units.
     * Each unit independently acquires targets and fires based on its own stats.
     */
    private void fireGarrisonWeapons(GameEntities gameEntities) {
        if (garrisonedUnits.isEmpty()) {
            return;
        }

        for (Unit garrisonedUnit : garrisonedUnits) {
            // Skip if unit can't attack
            if (!garrisonedUnit.getUnitType().canAttack() || garrisonedUnit.getWeapon() == null) {
                continue;
            }

            Targetable target = gameEntities.findNearestEnemyTargetable(garrisonedUnit);
            if (target == null || !target.isActive()) {
                continue;
            }

            // Fire weapon from APC position (garrisoned unit's position tracks APC)
            garrisonedUnit.getWeapon().fire(
                    garrisonedUnit.getPosition(),
                    target.getPosition(),
                    target.getElevation(),
                    garrisonedUnit.getId(),
                    garrisonedUnit.getTeamNumber(),
                    garrisonedUnit.getBody(),
                    gameEntities
            ).forEach(gameEntities::add);
        }
    }

    /**
     * Get number of garrisoned units.
     */
    public int getGarrisonCount() {
        return garrisonedUnits.size();
    }

    /**
     * Check if garrison is full.
     */
    public boolean isFull() {
        return garrisonedUnits.size() >= getMaxGarrisonCapacity();
    }

    /**
     * Called when APC is destroyed.
     * All garrisoned units are destroyed with the APC.
     */
    @Override
    public void onDestroy() {
        if (garrisonedUnits.isEmpty()) {
            return;
        }

        log.info("APC {} destroyed with {} garrisoned units - destroying all passengers",
                unit.getId(), garrisonedUnits.size());

        // Destroy all garrisoned units
        for (Unit garrisonedUnit : garrisonedUnits) {
            garrisonedUnit.setActive(false);
            garrisonedUnit.setHealth(0);
            log.info("Unit {} destroyed with APC", garrisonedUnit.getId());
        }

        garrisonedUnits.clear();
    }
}
