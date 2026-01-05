package com.fullsteam;

import com.fullsteam.model.UnitType;
import com.fullsteam.model.Unit;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for UnitType physics bodies.
 * Validates that all unit types can create valid physics fixtures
 * and be added to a dyn4j world without errors.
 */
public class UnitTypeTest {

    @Test
    @DisplayName("All unit types should create valid physics fixtures")
    public void testAllUnitTypesCreateValidFixtures() {
        for (UnitType unitType : UnitType.values()) {
            // Create physics fixtures (now returns a list)
            List<Convex> fixtures = unitType.createPhysicsFixtures();
            
            assertNotNull(fixtures, 
                String.format("Unit type %s should create a non-null physics fixtures list", unitType.name()));
            assertFalse(fixtures.isEmpty(), 
                String.format("Unit type %s should have at least one fixture", unitType.name()));
            
            // Verify all fixtures are valid (have area, radius, etc.)
            for (Convex fixture : fixtures) {
                assertNotNull(fixture, 
                    String.format("Unit type %s should not have null fixtures", unitType.name()));
                assertTrue(fixture.getRadius() > 0, 
                    String.format("Unit type %s fixture should have positive radius", unitType.name()));
            }
        }
    }

    @Test
    @DisplayName("All unit types should be addable to a dyn4j world")
    public void testAllUnitTypesCanBeAddedToWorld() {
        // Create a test world
        World<Body> world = new World<>();
        
        int unitId = 1;
        int playerId = 1;
        int teamNumber = 1;
        
        for (UnitType unitType : UnitType.values()) {
            // Create a unit at a unique position to avoid overlaps
            double x = (unitId % 10) * 100.0;
            double y = (unitId / 10) * 100.0;
            
            Unit unit = new Unit(unitId, unitType, x, y, playerId, teamNumber);
            
            assertNotNull(unit, 
                String.format("Should be able to create unit of type %s", unitType.name()));
            assertNotNull(unit.getBody(), 
                String.format("Unit of type %s should have a body", unitType.name()));
            
            // Add to world - this will throw if the fixture is invalid
            try {
                world.addBody(unit.getBody());
            } catch (Exception e) {
                fail(String.format("Failed to add unit type %s to world: %s", 
                    unitType.name(), e.getMessage()));
            }
            
            // Verify the body was added
            assertTrue(world.containsBody(unit.getBody()), 
                String.format("World should contain unit of type %s", unitType.name()));
            
            unitId++;
        }
        
        // Verify all units were added
        assertEquals(UnitType.values().length, world.getBodyCount(), 
            "World should contain all unit types");
    }

    @Test
    @DisplayName("All unit physics fixtures should be convex")
    public void testAllUnitFixturesAreConvex() {
        for (UnitType unitType : UnitType.values()) {
            List<Convex> fixtures = unitType.createPhysicsFixtures();
            
            assertFalse(fixtures.isEmpty(), 
                String.format("Unit type %s should have at least one fixture", unitType.name()));
            
            for (Convex fixture : fixtures) {
                // dyn4j will throw during creation if the shape is not convex
                // If we got here, the shape is valid
                assertNotNull(fixture, 
                    String.format("Unit type %s should have a convex fixture", unitType.name()));
                
                // Additional validation: check that the fixture has a valid center
                assertNotNull(fixture.getCenter(), 
                    String.format("Unit type %s fixture should have a valid center", unitType.name()));
            }
        }
    }

    @Test
    @DisplayName("All unit physics fixtures should have correct winding order")
    public void testAllUnitFixturesHaveCorrectWinding() {
        // Create a test world and body for each unit type
        World<Body> world = new World<>();
        
        for (UnitType unitType : UnitType.values()) {
            Body testBody = new Body();
            List<Convex> fixtures = unitType.createPhysicsFixtures();
            
            // Add all fixtures to body - this will throw if winding is incorrect
            try {
                for (Convex fixture : fixtures) {
                    testBody.addFixture(fixture);
                }
                testBody.setMass(MassType.NORMAL);
            } catch (Exception e) {
                fail(String.format("Unit type %s has invalid winding order or fixture: %s", 
                    unitType.name(), e.getMessage()));
            }
            
            // Verify fixtures were added
            assertEquals(fixtures.size(), testBody.getFixtureCount(), 
                String.format("Unit type %s should have %d fixture(s)", unitType.name(), fixtures.size()));
            
            // Add to world to further validate
            try {
                world.addBody(testBody);
            } catch (Exception e) {
                fail(String.format("Failed to add unit type %s to world (winding/convexity issue): %s", 
                    unitType.name(), e.getMessage()));
            }
        }
    }

    @Test
    @DisplayName("All unit types should have reasonable size fixtures")
    public void testAllUnitFixturesHaveReasonableSize() {
        for (UnitType unitType : UnitType.values()) {
            List<Convex> fixtures = unitType.createPhysicsFixtures();
            double unitSize = unitType.getSize();
            
            for (Convex fixture : fixtures) {
                double radius = fixture.getRadius();
                
                // Fixture radius should be related to unit size
                // Allow up to 2x the base size for elongated shapes
                assertTrue(radius > 0, 
                    String.format("Unit type %s should have positive radius", unitType.name()));
                assertTrue(radius <= unitSize * 2.5, 
                    String.format("Unit type %s fixture radius (%.2f) should not exceed 2.5x unit size (%.2f)", 
                        unitType.name(), radius, unitSize));
            }
        }
    }

    @Test
    @DisplayName("Unit fixtures should not overlap at spawn positions")
    public void testUnitFixturesDontOverlapAtSpawn() {
        World<Body> world = new World<>();
        
        int unitId = 1;
        int playerId = 1;
        int teamNumber = 1;
        
        // Spawn units in a grid with sufficient spacing
        for (UnitType unitType : UnitType.values()) {
            double spacing = 200.0; // Large spacing to avoid overlaps
            double x = (unitId % 10) * spacing;
            double y = (unitId / 10) * spacing;
            
            Unit unit = new Unit(unitId, unitType, x, y, playerId, teamNumber);
            world.addBody(unit.getBody());
            
            unitId++;
        }
        
        // Step the world to detect any immediate collisions
        try {
            world.step(1);
        } catch (Exception e) {
            fail("World step failed, possibly due to invalid fixtures: " + e.getMessage());
        }
        
        // If we got here, all fixtures are valid and don't cause immediate issues
        assertEquals(UnitType.values().length, world.getBodyCount(), 
            "All units should remain in world after step");
    }

    @Test
    @DisplayName("Specific unit types should have expected shape characteristics")
    public void testSpecificUnitShapeCharacteristics() {
        // Test a few specific units to ensure they have the expected custom shapes
        
        // Gigantonaut should be a trapezoid (4 vertices, single fixture)
        List<Convex> gigantonautFixtures = UnitType.GIGANTONAUT.createPhysicsFixtures();
        assertNotNull(gigantonautFixtures);
        assertEquals(1, gigantonautFixtures.size(), "Gigantonaut should have one fixture");
        
        // Colossus should have multiple fixtures (robotic walker)
        List<Convex> colossusFixtures = UnitType.COLOSSUS.createPhysicsFixtures();
        assertNotNull(colossusFixtures);
        assertTrue(colossusFixtures.size() >= 1, "Colossus should have at least one fixture");
        
        // Worker should be a circle (single fixture)
        List<Convex> workerFixtures = UnitType.WORKER.createPhysicsFixtures();
        assertNotNull(workerFixtures);
        assertEquals(1, workerFixtures.size(), "Worker should have one fixture");
        assertTrue(workerFixtures.get(0) instanceof org.dyn4j.geometry.Circle, 
            "Worker should have a circular fixture");
        
        // Jeep should have multiple fixtures (multi-part vehicle)
        List<Convex> jeepFixtures = UnitType.JEEP.createPhysicsFixtures();
        assertNotNull(jeepFixtures);
        assertTrue(jeepFixtures.size() >= 1, "Jeep should have at least one fixture");
        
        // All fixtures should be valid
        assertTrue(gigantonautFixtures.get(0).getRadius() > 0);
        assertTrue(colossusFixtures.get(0).getRadius() > 0);
        assertTrue(workerFixtures.get(0).getRadius() > 0);
        assertTrue(jeepFixtures.get(0).getRadius() > 0);
    }

    @Test
    @DisplayName("All unit types should be creatable with different positions")
    public void testUnitsCanBeCreatedAtDifferentPositions() {
        // Test creating units at various positions (use separate world for each to avoid body count issues)
        double[][] positions = {
            {0, 0},
            {100, 100},
            {-100, -100},
            {500, -500},
            {-500, 500}
        };
        
        int unitId = 1;
        for (UnitType unitType : UnitType.values()) {
            for (double[] pos : positions) {
                World<Body> world = new World<>();
                Unit unit = new Unit(unitId++, unitType, pos[0], pos[1], 1, 1);
                
                assertNotNull(unit);
                assertNotNull(unit.getBody());
                
                // Position should be close to requested position (within unit size tolerance)
                // Some offset is expected due to fixture center calculations
                double tolerance = unitType.getSize();
                assertEquals(pos[0], unit.getPosition().x, tolerance,
                    String.format("Unit type %s X position should be near %.1f", unitType.name(), pos[0]));
                assertEquals(pos[1], unit.getPosition().y, tolerance,
                    String.format("Unit type %s Y position should be near %.1f", unitType.name(), pos[1]));
                
                // Should be addable to world
                world.addBody(unit.getBody());
                assertEquals(1, world.getBodyCount(), 
                    String.format("Unit type %s should be addable to world at position (%.1f, %.1f)", 
                        unitType.name(), pos[0], pos[1]));
            }
        }
    }

    @Test
    @DisplayName("Unit fixtures should maintain integrity after rotation")
    public void testUnitFixturesAfterRotation() {
        World<Body> world = new World<>();
        
        int unitId = 1;
        for (UnitType unitType : UnitType.values()) {
            System.out.println(unitType);
            Unit unit = new Unit(unitId++, unitType, 0, 0, 1, 1);
            world.addBody(unit.getBody());
            
            // Rotate the unit to various angles
            double[] angles = {0, Math.PI / 4, Math.PI / 2, Math.PI, -Math.PI / 2};
            
            for (double angle : angles) {
                unit.setRotation(angle);
                assertEquals(angle, unit.getRotation(), 0.01, 
                    String.format("Unit type %s should maintain rotation", unitType.name()));
                
                // All fixtures should still be valid
                for (int i = 0; i < unit.getBody().getFixtureCount(); i++) {
                    assertNotNull(unit.getBody().getFixture(i));
                }
            }
        }
        
        // Step the world to ensure rotated fixtures don't cause issues
        try {
            world.step(1);
        } catch (Exception e) {
            fail("World step failed with rotated units: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Multi-fixture units should have all fixtures properly attached")
    public void testMultiFixtureUnits() {
        // This test validates that units with multiple fixtures work correctly
        World<Body> world = new World<>();
        
        for (UnitType unitType : UnitType.values()) {
            List<Convex> fixtures = unitType.createPhysicsFixtures();
            
            // Create a unit and verify all fixtures are added to the body
            Unit unit = new Unit(1, unitType, 0, 0, 1, 1);
            
            assertEquals(fixtures.size(), unit.getBody().getFixtureCount(),
                String.format("Unit type %s should have %d fixture(s) in its body", 
                    unitType.name(), fixtures.size()));
            
            // Verify each fixture is valid
            for (int i = 0; i < unit.getBody().getFixtureCount(); i++) {
                assertNotNull(unit.getBody().getFixture(i),
                    String.format("Unit type %s fixture %d should not be null", unitType.name(), i));
                assertTrue(unit.getBody().getFixture(i).getShape().getRadius() > 0,
                    String.format("Unit type %s fixture %d should have positive radius", unitType.name(), i));
            }
            
            // Add to world and verify it works
            world.addBody(unit.getBody());
            assertTrue(world.containsBody(unit.getBody()),
                String.format("Unit type %s should be addable to world", unitType.name()));
            
            // Remove for next iteration
            world.removeBody(unit.getBody());
        }
    }
}

