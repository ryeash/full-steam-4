package com.fullsteam;

import com.fullsteam.model.GameEntity;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import org.dyn4j.geometry.Vector2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for GameEntity boundary enforcement.
 * Validates that entities (especially air units) respect world boundaries.
 */
public class GameEntityTest {

    @Test
    @DisplayName("Units should be clamped to world boundaries")
    public void testUnitBoundaryEnforcement() {
        double worldWidth = 1000.0;
        double worldHeight = 1000.0;
        
        // Create a unit at the center
        Unit unit = new Unit(1, UnitType.SCOUT_DRONE, 0, 0, 1, 1);
        
        // Test clamping when unit is within bounds (should not change position)
        unit.setPosition(new Vector2(100, 100));
        unit.clampToBounds(worldWidth, worldHeight);
        assertEquals(100.0, unit.getPosition().x, 0.01, "Unit within bounds should not move (X)");
        assertEquals(100.0, unit.getPosition().y, 0.01, "Unit within bounds should not move (Y)");
        
        // Test clamping when unit exceeds positive X boundary
        unit.setPosition(new Vector2(600, 0));
        unit.clampToBounds(worldWidth, worldHeight);
        assertEquals(500.0, unit.getPosition().x, 0.01, "Unit should be clamped to max X boundary");
        assertEquals(0.0, unit.getPosition().y, 0.01, "Y position should remain unchanged");
        
        // Test clamping when unit exceeds negative X boundary
        unit.setPosition(new Vector2(-600, 0));
        unit.clampToBounds(worldWidth, worldHeight);
        assertEquals(-500.0, unit.getPosition().x, 0.01, "Unit should be clamped to min X boundary");
        assertEquals(0.0, unit.getPosition().y, 0.01, "Y position should remain unchanged");
        
        // Test clamping when unit exceeds positive Y boundary
        unit.setPosition(new Vector2(0, 600));
        unit.clampToBounds(worldWidth, worldHeight);
        assertEquals(0.0, unit.getPosition().x, 0.01, "X position should remain unchanged");
        assertEquals(500.0, unit.getPosition().y, 0.01, "Unit should be clamped to max Y boundary");
        
        // Test clamping when unit exceeds negative Y boundary
        unit.setPosition(new Vector2(0, -600));
        unit.clampToBounds(worldWidth, worldHeight);
        assertEquals(0.0, unit.getPosition().x, 0.01, "X position should remain unchanged");
        assertEquals(-500.0, unit.getPosition().y, 0.01, "Unit should be clamped to min Y boundary");
        
        // Test clamping when unit exceeds both X and Y boundaries
        unit.setPosition(new Vector2(700, 800));
        unit.clampToBounds(worldWidth, worldHeight);
        assertEquals(500.0, unit.getPosition().x, 0.01, "Unit should be clamped to max X boundary");
        assertEquals(500.0, unit.getPosition().y, 0.01, "Unit should be clamped to max Y boundary");
        
        // Test clamping in negative corner
        unit.setPosition(new Vector2(-700, -800));
        unit.clampToBounds(worldWidth, worldHeight);
        assertEquals(-500.0, unit.getPosition().x, 0.01, "Unit should be clamped to min X boundary");
        assertEquals(-500.0, unit.getPosition().y, 0.01, "Unit should be clamped to min Y boundary");
    }

    @Test
    @DisplayName("Air units should be clamped to world boundaries")
    public void testAirUnitBoundaryEnforcement() {
        double worldWidth = 2000.0;
        double worldHeight = 1500.0;
        
        // Test with different air unit types
        UnitType[] airUnits = {
            UnitType.SCOUT_DRONE,
            UnitType.BOMBER,
            UnitType.HELICOPTER,
            UnitType.INTERCEPTOR
        };
        
        for (UnitType airUnitType : airUnits) {
            Unit airUnit = new Unit(1, airUnitType, 0, 0, 1, 1);
            
            // Try to move air unit outside boundaries (far outside)
            airUnit.setPosition(new Vector2(1500, 1000));
            
            // Verify position is outside before clamping
            double xBeforeClamp = airUnit.getPosition().x;
            double yBeforeClamp = airUnit.getPosition().y;
            
            airUnit.clampToBounds(worldWidth, worldHeight);
            
            // After clamping, should be within boundaries
            double xAfterClamp = airUnit.getPosition().x;
            double yAfterClamp = airUnit.getPosition().y;
            
            assertTrue(Math.abs(xAfterClamp) <= worldWidth / 2.0,
                String.format("%s should be within X boundary (was %.2f, now %.2f)", 
                    airUnitType.name(), xBeforeClamp, xAfterClamp));
            assertTrue(Math.abs(yAfterClamp) <= worldHeight / 2.0,
                String.format("%s should be within Y boundary (was %.2f, now %.2f)", 
                    airUnitType.name(), yBeforeClamp, yAfterClamp));
            
            assertEquals(1000.0, xAfterClamp, 0.01,
                String.format("%s should be clamped to max X boundary", airUnitType.name()));
            assertEquals(750.0, yAfterClamp, 0.01,
                String.format("%s should be clamped to max Y boundary", airUnitType.name()));
        }
    }

    @Test
    @DisplayName("Velocity should be zeroed when unit hits boundary")
    public void testVelocityResetOnBoundaryCollision() {
        double worldWidth = 1000.0;
        double worldHeight = 1000.0;
        
        Unit unit = new Unit(1, UnitType.SCOUT_DRONE, 0, 0, 1, 1);
        
        // Set unit velocity (simulating movement)
        unit.getBody().setLinearVelocity(100, 50);
        
        // Move unit outside boundary
        unit.setPosition(new Vector2(600, 0));
        
        // Clamp to bounds
        unit.clampToBounds(worldWidth, worldHeight);
        
        // Velocity should be zeroed
        assertEquals(0.0, unit.getBody().getLinearVelocity().x, 0.01,
            "X velocity should be zero after hitting boundary");
        assertEquals(0.0, unit.getBody().getLinearVelocity().y, 0.01,
            "Y velocity should be zero after hitting boundary");
    }

    @Test
    @DisplayName("Boundary clamping should work with different world sizes")
    public void testBoundaryClampingWithDifferentWorldSizes() {
        double[][] worldSizes = {
            {500.0, 500.0},
            {1000.0, 1000.0},
            {2000.0, 1500.0},
            {3000.0, 3000.0}
        };
        
        for (double[] size : worldSizes) {
            double worldWidth = size[0];
            double worldHeight = size[1];
            
            Unit unit = new Unit(1, UnitType.SCOUT_DRONE, 0, 0, 1, 1);
            
            // Test all four corners
            unit.setPosition(new Vector2(worldWidth, worldHeight));
            unit.clampToBounds(worldWidth, worldHeight);
            assertEquals(worldWidth / 2.0, unit.getPosition().x, 0.01);
            assertEquals(worldHeight / 2.0, unit.getPosition().y, 0.01);
            
            unit.setPosition(new Vector2(-worldWidth, -worldHeight));
            unit.clampToBounds(worldWidth, worldHeight);
            assertEquals(-worldWidth / 2.0, unit.getPosition().x, 0.01);
            assertEquals(-worldHeight / 2.0, unit.getPosition().y, 0.01);
        }
    }

    @Test
    @DisplayName("Boundary clamping should handle edge case at exact boundary")
    public void testBoundaryClampingAtExactBoundary() {
        double worldWidth = 1000.0;
        double worldHeight = 1000.0;
        
        Unit unit = new Unit(1, UnitType.SCOUT_DRONE, 0, 0, 1, 1);
        
        // Place unit exactly at boundary (should not change)
        unit.setPosition(new Vector2(500.0, 0));
        unit.clampToBounds(worldWidth, worldHeight);
        assertEquals(500.0, unit.getPosition().x, 0.01, "Unit at exact boundary should not move");
        assertEquals(0.0, unit.getPosition().y, 0.01);
        
        // Place unit exactly at negative boundary (should not change)
        unit.setPosition(new Vector2(-500.0, 0));
        unit.clampToBounds(worldWidth, worldHeight);
        assertEquals(-500.0, unit.getPosition().x, 0.01, "Unit at exact negative boundary should not move");
        assertEquals(0.0, unit.getPosition().y, 0.01);
    }
}

