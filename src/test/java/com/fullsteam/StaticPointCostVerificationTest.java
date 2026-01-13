package com.fullsteam;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.customization.BuildingTemplate;
import com.fullsteam.model.customization.UnitTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verify that point costs are now static in enums and templates use them correctly
 */
public class StaticPointCostVerificationTest extends BaseTestClass {

    @Test
    public void verifyUnitPointCostsAreStaticAndAccessible() {
        // Test a few units to ensure the pointCost field is accessible
        assertEquals(0, UnitType.WORKER.getPointCost(), "WORKER should cost 0 points");
        assertEquals(2, UnitType.INFANTRY.getPointCost(), "INFANTRY should cost 2 points");
        assertEquals(10, UnitType.GIGANTONAUT.getPointCost(), "GIGANTONAUT should cost 10 points");
        assertEquals(9, UnitType.PHOTON_TITAN.getPointCost(), "PHOTON_TITAN should cost 9 points");
        assertEquals(8, UnitType.RAIDER.getPointCost(), "RAIDER should cost 8 points");
    }

    @Test
    public void verifyBuildingPointCostsAreStaticAndAccessible() {
        // Test a few buildings to ensure the pointCost field is accessible
        assertEquals(0, BuildingType.HEADQUARTERS.getPointCost(), "HEADQUARTERS should cost 0 points");
        assertEquals(3, BuildingType.BARRACKS.getPointCost(), "BARRACKS should cost 3 points");
        assertEquals(12, BuildingType.ANDROID_FACTORY.getPointCost(), "ANDROID_FACTORY should cost 12 points");
        assertEquals(5, BuildingType.FACTORY.getPointCost(), "FACTORY should cost 5 points");
    }

    @Test
    public void verifyTemplatesUseStaticPointCosts() {
        // Verify that templates now use the static values from enums
        for (UnitType unitType : UnitType.values()) {
            UnitTemplate template = UnitTemplate.fromUnitType(unitType);
            assertEquals(unitType.getPointCost(), template.getPointCost(),
                    String.format("Template for %s should use static point cost from enum", unitType.name()));
        }

        for (BuildingType buildingType : BuildingType.values()) {
            BuildingTemplate template = BuildingTemplate.fromBuildingType(buildingType);
            assertEquals(buildingType.getPointCost(), template.getPointCost(),
                    String.format("Template for %s should use static point cost from enum", buildingType.name()));
        }
    }

    @Test
    public void verifyPointCostConsistency() {
        // Ensure no negative point costs
        for (UnitType unitType : UnitType.values()) {
            assertTrue(unitType.getPointCost() >= 0,
                    String.format("%s has negative point cost: %d", unitType.name(), unitType.getPointCost()));
        }

        for (BuildingType buildingType : BuildingType.values()) {
            assertTrue(buildingType.getPointCost() >= 0,
                    String.format("%s has negative point cost: %d", buildingType.name(), buildingType.getPointCost()));
        }
    }

    @Test
    public void verifyFreeUnitsAndBuildings() {
        // Worker and Android should be free
        assertEquals(0, UnitType.WORKER.getPointCost(), "WORKER must be free");
        assertEquals(0, UnitType.ANDROID.getPointCost(), "ANDROID must be free");

        // HQ and Power Plant should be free
        assertEquals(0, BuildingType.HEADQUARTERS.getPointCost(), "HEADQUARTERS must be free");
        assertEquals(0, BuildingType.POWER_PLANT.getPointCost(), "POWER_PLANT must be free");
    }

    @Test
    public void verifyHeroUnitCosts() {
        // Hero units should be expensive (8-10 points)
        assertEquals(8, UnitType.RAIDER.getPointCost(), "RAIDER hero should cost 8 points");
        assertEquals(10, UnitType.COLOSSUS.getPointCost(), "COLOSSUS hero should cost 10 points");
        assertEquals(9, UnitType.PHOTON_TITAN.getPointCost(), "PHOTON_TITAN hero should cost 9 points");
        assertEquals(9, UnitType.GUNSHIP.getPointCost(), "GUNSHIP hero should cost 9 points");
        assertEquals(10, UnitType.GIGANTONAUT.getPointCost(), "GIGANTONAUT hero should cost 10 points");
    }

    @Test
    public void verifySpecialBuildingCosts() {
        // Special faction buildings should be expensive (12 points)
        assertEquals(12, BuildingType.SANDSTORM_GENERATOR.getPointCost());
        assertEquals(12, BuildingType.ANDROID_FACTORY.getPointCost());
        assertEquals(12, BuildingType.PHOTON_SPIRE.getPointCost());
        assertEquals(12, BuildingType.COMMAND_CITADEL.getPointCost());
        assertEquals(12, BuildingType.TEMPEST_SPIRE.getPointCost());
    }
}
