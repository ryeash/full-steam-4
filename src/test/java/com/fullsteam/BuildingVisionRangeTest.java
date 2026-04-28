package com.fullsteam;

import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Player;
import com.fullsteam.model.factions.FactionDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for building vision range functionality, including bonuses from perks.
 */
@DisplayName("Building Vision Range Tests")
public class BuildingVisionRangeTest extends BaseTestClass {

    private Player createTestFaction() {
        // Create a minimal faction definition to avoid NPEs
        FactionDefinition factionDef = FactionDefinition.builder()
                .buildingStatModifiers(new HashMap<>())
                .unitStatModifiers(new HashMap<>())
                .build();
        return new Player(1, 1, factionDef);
    }

    @Test
    @DisplayName("Building should have base vision range from BuildingType")
    void testBaseVisionRange() {
        // Create a simple building
        GameEntities gameEntities = new GameEntities(null, null);
        Player faction = createTestFaction();
        
        Building building = new Building(
                1,
                gameEntities,
                BuildingType.TURRET,
                100, 100,
                1, 1,
                faction,
                BuildingType.TURRET.getMaxHealth()
        );

        // Verify base vision range matches BuildingType
        assertEquals(BuildingType.TURRET.getVisionRange(), building.getEffectiveVisionRange(),
                "Building should have base vision range from BuildingType");
    }

    @Test
    @DisplayName("Building vision range should increase with bonus")
    void testVisionRangeBonus() {
        GameEntities gameEntities = new GameEntities(null, null);
        Player faction = createTestFaction();
        
        Building building = new Building(
                1,
                gameEntities,
                BuildingType.TURRET,
                100, 100,
                1, 1,
                faction,
                BuildingType.TURRET.getMaxHealth()
        );

        double baseVision = building.getEffectiveVisionRange();
        double bonus = 150.0;

        // Add vision range bonus
        building.addVisionRangeBonus(bonus);

        // Verify vision range increased by bonus amount
        assertEquals(baseVision + bonus, building.getEffectiveVisionRange(),
                "Building vision range should increase by bonus amount");
    }

    @Test
    @DisplayName("Vision range bonuses should be cumulative")
    void testCumulativeVisionBonuses() {
        GameEntities gameEntities = new GameEntities(null, null);
        Player faction = createTestFaction();
        
        Building building = new Building(
                1,
                gameEntities,
                BuildingType.TURRET,
                100, 100,
                1, 1,
                faction,
                BuildingType.TURRET.getMaxHealth()
        );

        double baseVision = building.getEffectiveVisionRange();

        // Add multiple bonuses
        building.addVisionRangeBonus(50.0);
        building.addVisionRangeBonus(100.0);
        building.addVisionRangeBonus(25.0);

        // Verify all bonuses are cumulative
        assertEquals(baseVision + 175.0, building.getEffectiveVisionRange(),
                "Vision range bonuses should be cumulative");
    }

    @Test
    @DisplayName("Different building types should have different base vision ranges")
    void testDifferentBuildingTypes() {
        GameEntities gameEntities = new GameEntities(null, null);
        Player faction = createTestFaction();
        
        Building hq = new Building(
                1,
                gameEntities,
                BuildingType.HEADQUARTERS,
                100, 100,
                1, 1,
                faction,
                BuildingType.HEADQUARTERS.getMaxHealth()
        );

        Building turret = new Building(
                2,
                gameEntities,
                BuildingType.TURRET,
                200, 200,
                1, 1,
                faction,
                BuildingType.TURRET.getMaxHealth()
        );

        // Verify different building types have different vision ranges
        assertNotEquals(hq.getEffectiveVisionRange(), turret.getEffectiveVisionRange(),
                "Different building types should have different vision ranges");
        
        assertEquals(BuildingType.HEADQUARTERS.getVisionRange(), hq.getEffectiveVisionRange(),
                "HQ should have its base vision range");
        assertEquals(BuildingType.TURRET.getVisionRange(), turret.getEffectiveVisionRange(),
                "Turret should have its base vision range");
    }
}
