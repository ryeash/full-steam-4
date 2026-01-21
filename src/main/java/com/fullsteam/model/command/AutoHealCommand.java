package com.fullsteam.model.command;

import com.fullsteam.model.SpecialAbility;
import com.fullsteam.model.Unit;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;

/**
 * Command for medics to automatically scan for and heal damaged friendly units.
 * This is an AI behavior that runs when the medic has no player orders.
 */
@Getter
public class AutoHealCommand extends UnitCommand {
    private static final double HEAL_RANGE = 150.0; // Medics can heal within 150 units

    public AutoHealCommand(Unit unit) {
        super(unit, false); // Never a player order
    }

    @Override
    public boolean update(double deltaTime) {
        // Only medics can heal
        if (!unit.getUnitType().canHeal()) {
            return false;
        }

        // Check if heal ability is ready
        SpecialAbility ability = unit.getUnitType().getSpecialAbility();
        if (ability != SpecialAbility.HEAL) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - unit.getLastSpecialAbilityTime() < ability.getCooldownMs()) {
            // Still on cooldown, keep command active
            return true;
        }

        // Scan for damaged friendly units
        if (gameEntities != null) {
            Unit targetUnit = findMostDamagedFriendlyUnit();
            if (targetUnit != null) {
                unit.useSpecialAbilityOnUnit(targetUnit);
                // Keep command active to continue healing
                return true;
            }
        }

        // No targets found, command completes (unit will go idle or pick up another AI behavior)
        return false;
    }

    @Override
    public void updateMovement(double deltaTime) {
        // Auto-heal command doesn't move - medics stay in place while healing
        unit.getBody().setLinearVelocity(0, 0);
    }

    /**
     * Find the most damaged friendly unit within heal range
     *
     * @return Most damaged unit, or null if none found
     */
    private Unit findMostDamagedFriendlyUnit() {
        Vector2 currentPos = unit.getPosition();
        Unit mostDamagedUnit = null;
        double lowestHealthPercent = 1.0;

        for (Unit otherUnit : gameEntities.getUnits().values()) {
            if (otherUnit == unit || otherUnit.getTeamNumber() != unit.getTeamNumber() || !otherUnit.isActive()) {
                continue;
            }

            double distance = currentPos.distance(otherUnit.getPosition());
            if (distance > HEAL_RANGE) {
                continue;
            }

            double healthPercent = otherUnit.getHealth() / otherUnit.getMaxHealth();
            if (healthPercent < 1.0 && healthPercent < lowestHealthPercent) {
                mostDamagedUnit = otherUnit;
                lowestHealthPercent = healthPercent;
            }
        }

        return mostDamagedUnit;
    }

    @Override
    public String getDescription() {
        return "Auto-healing damaged friendly units";
    }
}
