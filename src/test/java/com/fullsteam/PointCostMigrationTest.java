package com.fullsteam;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.customization.BuildingTemplate;
import com.fullsteam.model.customization.UnitTemplate;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Test to print out current point cost values for migration to static enum configuration
 */
public class PointCostMigrationTest extends BaseTestClass {

    @Test
    public void printAllUnitPointCosts() {
        System.out.println("\n========== UNIT POINT COSTS ==========");
        System.out.println("Format: UnitType -> Point Cost");
        System.out.println("======================================\n");

        Arrays.stream(UnitType.values())
                .sorted(Comparator.comparing(u -> UnitTemplate.fromUnitType(u).getPointCost()))
                .forEach(unitType -> {
                    UnitTemplate template = UnitTemplate.fromUnitType(unitType);
                    System.out.printf("%-25s -> %3d points%n",
                            unitType.name(),
                            template.getPointCost());
                });

        System.out.println("\n======================================");
        System.out.println("Total Units: " + UnitType.values().length);
        System.out.println("======================================\n");
    }

    @Test
    public void printAllBuildingPointCosts() {
        System.out.println("\n========== BUILDING POINT COSTS ==========");
        System.out.println("Format: BuildingType -> Point Cost");
        System.out.println("==========================================\n");

        Arrays.stream(BuildingType.values())
                .sorted(Comparator.comparing(b -> BuildingTemplate.fromBuildingType(b).getPointCost()))
                .forEach(buildingType -> {
                    BuildingTemplate template = BuildingTemplate.fromBuildingType(buildingType);
                    System.out.printf("%-25s -> %3d points%n",
                            buildingType.name(),
                            template.getPointCost());
                });

        System.out.println("\n==========================================");
        System.out.println("Total Buildings: " + BuildingType.values().length);
        System.out.println("==========================================\n");
    }

    @Test
    public void printJavaEnumFormat() {
        System.out.println("\n========== JAVA ENUM FORMAT (Copy-Paste Ready) ==========\n");

        System.out.println("// ===== UNIT POINT COSTS =====");
        Arrays.stream(UnitType.values()).forEach(unitType -> {
            UnitTemplate template = UnitTemplate.fromUnitType(unitType);
            System.out.printf("case %s -> %d;%n", unitType.name(), template.getPointCost());
        });

        System.out.println("\n// ===== BUILDING POINT COSTS =====");
        Arrays.stream(BuildingType.values()).forEach(buildingType -> {
            BuildingTemplate template = BuildingTemplate.fromBuildingType(buildingType);
            System.out.printf("case %s -> %d;%n", buildingType.name(), template.getPointCost());
        });

        System.out.println("\n=========================================================\n");
    }
}
