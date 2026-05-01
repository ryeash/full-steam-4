package com.fullsteam;

import com.fullsteam.model.Player;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.FactionDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests for unit speed modifier functionality, including slows and buffs from perks.
 */
@DisplayName("Unit Speed Modifier Tests")
public class UnitSpeedModifierTest extends BaseTestClass {

    private Player createTestFaction() {
        // Create a minimal faction definition to avoid NPEs
        FactionDefinition factionDef = FactionDefinition.builder()
                .buildingStatModifiers(new HashMap<>())
                .unitStatModifiers(new HashMap<>())
                .build();
        return new Player(1, 1, factionDef, null, 0);
    }

    @Test
    @DisplayName("Unit should have base movement speed from UnitType")
    void testBaseMovementSpeed() {
        Player faction = createTestFaction();

        Unit unit = new Unit(
                1,
                UnitType.INFANTRY,
                100, 100,
                1, 1,
                faction
        );

        // Verify base movement speed matches UnitType
        assertEquals(UnitType.INFANTRY.getMovementSpeed(), unit.getMovementSpeed(), 0.001,
                "Unit should have base movement speed from UnitType");
    }

    @Test
    @DisplayName("Speed multiplier should affect movement speed")
    void testSpeedMultiplier() {
        Player faction = createTestFaction();

        Unit unit = new Unit(
                1,
                UnitType.INFANTRY,
                100, 100,
                1, 1,
                faction
        );

        double baseSpeed = unit.getMovementSpeed();

        // Apply 50% slow
        unit.setSpeedMultiplier(0.5);

        assertEquals(baseSpeed * 0.5, unit.getMovementSpeed(), 0.001,
                "Movement speed should be reduced by 50%");
    }

    @Test
    @DisplayName("Speed multiplier should allow speed buffs")
    void testSpeedBuff() {
        Player faction = createTestFaction();

        Unit unit = new Unit(
                1,
                UnitType.INFANTRY,
                100, 100,
                1, 1,
                faction
        );

        double baseSpeed = unit.getMovementSpeed();

        // Apply 50% speed buff
        unit.setSpeedMultiplier(1.5);

        assertEquals(baseSpeed * 1.5, unit.getMovementSpeed(), 0.001,
                "Movement speed should be increased by 50%");
    }

    @Test
    @DisplayName("Speed multiplier can be restored to normal")
    void testSpeedRestore() {
        Player faction = createTestFaction();

        Unit unit = new Unit(
                1,
                UnitType.INFANTRY,
                100, 100,
                1, 1,
                faction
        );

        double baseSpeed = unit.getMovementSpeed();

        // Apply slow
        unit.setSpeedMultiplier(0.7);
        assertEquals(baseSpeed * 0.7, unit.getMovementSpeed(), 0.001);

        // Restore to normal
        unit.setSpeedMultiplier(1.0);
        assertEquals(baseSpeed, unit.getMovementSpeed(), 0.001,
                "Movement speed should be restored to base speed");
    }

    @Test
    @DisplayName("Speed multiplier should not allow negative values")
    void testNegativeSpeedPrevention() {
        Player faction = createTestFaction();

        Unit unit = new Unit(
                1,
                UnitType.INFANTRY,
                100, 100,
                1, 1,
                faction
        );

        // Try to set negative speed
        unit.setSpeedMultiplier(-0.5);

        // Should be clamped to 0
        assertEquals(0.0, unit.getSpeedMultiplier(), 0.001,
                "Speed multiplier should be clamped to 0 (not negative)");
        assertEquals(0.0, unit.getMovementSpeed(), 0.001,
                "Movement speed should be 0 when multiplier is 0");
    }

    @Test
    @DisplayName("Different unit types should have different base speeds")
    void testDifferentUnitTypes() {
        Player faction = createTestFaction();

        Unit infantry = new Unit(
                1,
                UnitType.INFANTRY,
                100, 100,
                1, 1,
                faction
        );

        Unit tank = new Unit(
                2,
                UnitType.TANK,
                200, 200,
                1, 1,
                faction
        );

        // Verify different unit types have different speeds
        assertNotEquals(infantry.getMovementSpeed(), tank.getMovementSpeed(),
                "Different unit types should have different movement speeds");

        assertEquals(UnitType.INFANTRY.getMovementSpeed(), infantry.getMovementSpeed(), 0.001,
                "Infantry should have its base movement speed");
        assertEquals(UnitType.TANK.getMovementSpeed(), tank.getMovementSpeed(), 0.001,
                "Tank should have its base movement speed");
    }

    @Test
    @DisplayName("Speed multiplier should stack with faction modifiers")
    void testSpeedMultiplierWithFactionModifiers() {
        // Create faction with speed modifier
        FactionDefinition.UnitStatModifier speedMod = FactionDefinition.UnitStatModifier.builder()
                .speedMultiplier(1.2) // +20% speed from faction
                .build();

        java.util.Map<UnitType, FactionDefinition.UnitStatModifier> unitMods = new HashMap<>();
        unitMods.put(UnitType.INFANTRY, speedMod);

        FactionDefinition factionDef = FactionDefinition.builder()
                .buildingStatModifiers(new HashMap<>())
                .unitStatModifiers(unitMods)
                .build();

        Player faction = new Player(1, 1, factionDef, null, 0);

        Unit unit = new Unit(
                1,
                UnitType.INFANTRY,
                100, 100,
                1, 1,
                faction
        );

        double baseSpeed = UnitType.INFANTRY.getMovementSpeed();
        double factionModifiedSpeed = baseSpeed * 1.2; // Faction gives +20%

        // Verify faction modifier is applied
        assertEquals(factionModifiedSpeed, unit.getMovementSpeed(), 0.001,
                "Unit should have faction-modified speed");

        // Apply 50% slow
        unit.setSpeedMultiplier(0.5);

        // Should be: base * faction_modifier * speed_multiplier
        assertEquals(baseSpeed * 1.2 * 0.5, unit.getMovementSpeed(), 0.001,
                "Speed multiplier should stack multiplicatively with faction modifiers");
    }

    @Test
    @DisplayName("Multiple speed changes should overwrite previous values")
    void testMultipleSpeedChanges() {
        Player faction = createTestFaction();

        Unit unit = new Unit(
                1,
                UnitType.INFANTRY,
                100, 100,
                1, 1,
                faction
        );

        double baseSpeed = unit.getMovementSpeed();

        // Apply various speed changes
        unit.setSpeedMultiplier(0.5);
        assertEquals(baseSpeed * 0.5, unit.getMovementSpeed(), 0.001);

        unit.setSpeedMultiplier(0.8);
        assertEquals(baseSpeed * 0.8, unit.getMovementSpeed(), 0.001);

        unit.setSpeedMultiplier(1.3);
        assertEquals(baseSpeed * 1.3, unit.getMovementSpeed(), 0.001);

        unit.setSpeedMultiplier(1.0);
        assertEquals(baseSpeed, unit.getMovementSpeed(), 0.001,
                "Each speed change should overwrite the previous value");
    }
}
