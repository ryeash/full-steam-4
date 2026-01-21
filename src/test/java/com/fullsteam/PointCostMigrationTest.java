package com.fullsteam;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Test to print out current point cost values from enums
 */
public class PointCostMigrationTest extends BaseTestClass {

    @Test
    public void printAllUnitPointCosts() {
        System.out.println("\n========== UNIT POINT COSTS ==========");
        System.out.println("Format: UnitType -> Point Cost");
        System.out.println("======================================\n");

        Arrays.stream(UnitType.values())
                .sorted(Comparator.comparing(UnitType::getPointCost))
                .forEach(unitType -> {
                    System.out.printf("%-25s -> %3d points%n",
                            unitType.name(),
                            unitType.getPointCost());
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
                .sorted(Comparator.comparing(BuildingType::getPointCost))
                .forEach(buildingType -> {
                    System.out.printf("%-25s -> %3d points%n",
                            buildingType.name(),
                            buildingType.getPointCost());
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
            System.out.printf("case %s -> %d;%n", unitType.name(), unitType.getPointCost());
        });

        System.out.println("\n// ===== BUILDING POINT COSTS =====");
        Arrays.stream(BuildingType.values()).forEach(buildingType -> {
            System.out.printf("case %s -> %d;%n", buildingType.name(), buildingType.getPointCost());
        });

        System.out.println("\n=========================================================\n");
    }
}
