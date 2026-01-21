package com.fullsteam.model.command;

import com.fullsteam.model.Building;
import com.fullsteam.model.SpecialAbility;
import com.fullsteam.model.Unit;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Command for engineers to automatically scan for and repair damaged friendly units and buildings.
 * This is an AI behavior that runs when the engineer has no player orders.
 * Prioritizes units over buildings.
 */
@Getter
public class AutoRepairCommand extends UnitCommand {
    private static final double REPAIR_RANGE = 150.0; // Engineers can repair within 150 units

    public AutoRepairCommand(Unit unit) {
        super(unit, false); // Never a player order
    }

    @Override
    public boolean update(double deltaTime) {
        // Only engineers can repair
        if (!unit.getUnitType().canRepair()) {
            return false;
        }

        // Check if repair ability is ready
        SpecialAbility ability = unit.getUnitType().getSpecialAbility();
        if (ability != SpecialAbility.REPAIR) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - unit.getLastSpecialAbilityTime() < ability.getCooldownMs()) {
            // Still on cooldown, keep command active
            return true;
        }

        // Scan for damaged friendly units and buildings
        if (gameEntities != null) {
            // Prioritize units over buildings
            Unit targetUnit = findMostDamagedFriendlyUnit();
            if (targetUnit != null) {
                unit.useSpecialAbilityOnUnit(targetUnit);
                // Keep command active to continue repairing
                return true;
            }

            // No damaged units, try buildings
            Building targetBuilding = findMostDamagedFriendlyBuilding();
            if (targetBuilding != null) {
                unit.useSpecialAbilityOnBuilding(targetBuilding);
                // Keep command active to continue repairing
                return true;
            }
        }

        // No targets found, command completes (unit will go idle or pick up another AI behavior)
        return false;
    }

    @Override
    public void updateMovement(double deltaTime) {
        // Auto-repair command doesn't move - engineers stay in place while repairing
        unit.getBody().setLinearVelocity(0, 0);
    }

    /**
     * Find the most damaged friendly unit within repair range
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
            if (distance > REPAIR_RANGE) {
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

    /**
     * Find the most damaged friendly building within repair range
     * @return Most damaged building, or null if none found
     */
    private Building findMostDamagedFriendlyBuilding() {
        Vector2 currentPos = unit.getPosition();
        Building mostDamagedBuilding = null;
        double lowestHealthPercent = 1.0;

        for (Building building : gameEntities.getBuildings().values()) {
            if (building.getTeamNumber() != unit.getTeamNumber() ||
                    !building.isActive() ||
                    building.isUnderConstruction()) {
                continue;
            }

            double distance = currentPos.distance(building.getPosition());
            if (distance > REPAIR_RANGE) {
                continue;
            }

            double healthPercent = building.getHealth() / building.getMaxHealth();
            if (healthPercent < 1.0 && healthPercent < lowestHealthPercent) {
                mostDamagedBuilding = building;
                lowestHealthPercent = healthPercent;
            }
        }

        return mostDamagedBuilding;
    }

    @Override
    public Vector2 getTargetPosition() {
        return null; // No spatial target
    }

    @Override
    public boolean isMoving() {
        return false;
    }

    @Override
    public String getDescription() {
        return "Auto-repairing damaged friendly units and buildings";
    }
}
