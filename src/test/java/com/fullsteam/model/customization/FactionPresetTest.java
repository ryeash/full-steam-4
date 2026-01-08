package com.fullsteam.model.customization;

import com.fullsteam.BaseTestClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for faction preset configurations.
 * Ensures all presets are valid, balanced, and at or near 100 points.
 */
@DisplayName("Faction Preset Tests")
public class FactionPresetTest extends BaseTestClass {

    private static final int TARGET_POINTS = 100;
    private static final int ACCEPTABLE_VARIANCE = 5; // Allow ±5 points variance

    /**
     * Provide all presets for parameterized tests
     */
    static Stream<CustomFactionConfig> provideAllPresets() {
        return FactionPresetRegistry.getAllPresets().stream();
    }

    @Test
    @DisplayName("All presets should be registered")
    void testAllPresetsRegistered() {
        Collection<CustomFactionConfig> presets = FactionPresetRegistry.getAllPresets();
        
        assertNotNull(presets, "Presets collection should not be null");
        assertFalse(presets.isEmpty(), "Should have at least one preset");
        
        // Verify expected presets exist
        assertTrue(FactionPresetRegistry.hasPreset("TERRAN_STANDARD"), 
                "TERRAN_STANDARD preset should exist");
        assertTrue(FactionPresetRegistry.hasPreset("TERRAN_INFANTRY"), 
                "TERRAN_INFANTRY preset should exist");
        assertTrue(FactionPresetRegistry.hasPreset("TERRAN_MECHANIZED"), 
                "TERRAN_MECHANIZED preset should exist");
        assertTrue(FactionPresetRegistry.hasPreset("NOMADS_RAIDERS"), 
                "NOMADS_RAIDERS preset should exist");
        assertTrue(FactionPresetRegistry.hasPreset("NOMADS_GUERRILLA"), 
                "NOMADS_GUERRILLA preset should exist");
        assertTrue(FactionPresetRegistry.hasPreset("SYNTHESIS_SHIELDED"), 
                "SYNTHESIS_SHIELDED preset should exist");
        assertTrue(FactionPresetRegistry.hasPreset("SYNTHESIS_ANDROIDS"), 
                "SYNTHESIS_ANDROIDS preset should exist");
        assertTrue(FactionPresetRegistry.hasPreset("TECH_ALLIANCE_BEAMS"), 
                "TECH_ALLIANCE_BEAMS preset should exist");
        assertTrue(FactionPresetRegistry.hasPreset("STORM_WINGS_BOMBERS"), 
                "STORM_WINGS_BOMBERS preset should exist");
        assertTrue(FactionPresetRegistry.hasPreset("STORM_WINGS_INTERCEPTORS"), 
                "STORM_WINGS_INTERCEPTORS preset should exist");
    }

    @ParameterizedTest(name = "{0} should be valid")
    @MethodSource("provideAllPresets")
    @DisplayName("Each preset should pass validation")
    void testPresetValidation(CustomFactionConfig preset) {
        assertDoesNotThrow(() -> preset.validate(), 
                String.format("Preset %s should be valid", preset.getDisplayName()));
    }

    @ParameterizedTest(name = "{0} should be at or near 100 points")
    @MethodSource("provideAllPresets")
    @DisplayName("Each preset should be balanced around 100 points")
    void testPresetPointBalance(CustomFactionConfig preset) {
        int totalPoints = preset.calculateTotalPoints();
        int storedPoints = preset.getTotalPointsSpent();
        
        // Verify calculated points match stored points
        assertEquals(totalPoints, storedPoints,
                String.format("Preset %s: calculated points (%d) should match stored points (%d)",
                        preset.getDisplayName(), totalPoints, storedPoints));
        
        // Verify points are within acceptable range of target
        int variance = Math.abs(totalPoints - TARGET_POINTS);
        assertTrue(variance <= ACCEPTABLE_VARIANCE,
                String.format("Preset %s has %d points (target: %d ±%d). Variance: %d points",
                        preset.getDisplayName(), totalPoints, TARGET_POINTS, 
                        ACCEPTABLE_VARIANCE, variance));
    }

    @ParameterizedTest(name = "{0} should have required units and buildings")
    @MethodSource("provideAllPresets")
    @DisplayName("Each preset should include required units and buildings")
    void testPresetRequiredElements(CustomFactionConfig preset) {
        // Every faction must have WORKER
        assertTrue(preset.getSelectedUnits().stream()
                        .anyMatch(unit -> unit.name().equals("WORKER")),
                String.format("Preset %s must include WORKER unit", preset.getDisplayName()));
        
        // Every faction must have HEADQUARTERS
        assertTrue(preset.getSelectedBuildings().stream()
                        .anyMatch(building -> building.name().equals("HEADQUARTERS")),
                String.format("Preset %s must include HEADQUARTERS building", preset.getDisplayName()));
        
        // Every faction must have POWER_PLANT
        assertTrue(preset.getSelectedBuildings().stream()
                        .anyMatch(building -> building.name().equals("POWER_PLANT")),
                String.format("Preset %s must include POWER_PLANT building", preset.getDisplayName()));
    }

    @ParameterizedTest(name = "{0} should have valid perk dependencies")
    @MethodSource("provideAllPresets")
    @DisplayName("Each preset should satisfy all perk dependencies")
    void testPresetPerkDependencies(CustomFactionConfig preset) {
        for (FactionPerk perk : preset.getSelectedPerks()) {
            for (FactionPerk dependency : perk.getDependsOn()) {
                assertTrue(preset.getSelectedPerks().contains(dependency),
                        String.format("Preset %s: perk %s requires %s but it's not selected",
                                preset.getDisplayName(), perk.getDisplayName(), 
                                dependency.getDisplayName()));
            }
        }
    }

    @ParameterizedTest(name = "{0} should have valid metadata")
    @MethodSource("provideAllPresets")
    @DisplayName("Each preset should have complete metadata")
    void testPresetMetadata(CustomFactionConfig preset) {
        assertNotNull(preset.getFactionId(), 
                "Preset should have faction ID");
        assertNotNull(preset.getDisplayName(), 
                "Preset should have display name");
        assertNotNull(preset.getThemeColor(), 
                "Preset should have theme color");
        assertNotNull(preset.getIcon(), 
                "Preset should have icon");
        
        assertFalse(preset.getFactionId().isEmpty(), 
                "Faction ID should not be empty");
        assertFalse(preset.getDisplayName().isEmpty(), 
                "Display name should not be empty");
        assertFalse(preset.getThemeColor().isEmpty(), 
                "Theme color should not be empty");
        assertFalse(preset.getIcon().isEmpty(), 
                "Icon should not be empty");
    }

    @Test
    @DisplayName("All presets should have unique faction IDs")
    void testPresetUniqueIds() {
        Collection<CustomFactionConfig> presets = FactionPresetRegistry.getAllPresets();
        long uniqueIdCount = presets.stream()
                .map(CustomFactionConfig::getFactionId)
                .distinct()
                .count();
        
        assertEquals(presets.size(), uniqueIdCount,
                "All presets should have unique faction IDs");
    }

    @Test
    @DisplayName("All presets should have unique display names")
    void testPresetUniqueNames() {
        Collection<CustomFactionConfig> presets = FactionPresetRegistry.getAllPresets();
        long uniqueNameCount = presets.stream()
                .map(CustomFactionConfig::getDisplayName)
                .distinct()
                .count();
        
        assertEquals(presets.size(), uniqueNameCount,
                "All presets should have unique display names");
    }

    @ParameterizedTest(name = "{0} should have at least one combat unit")
    @MethodSource("provideAllPresets")
    @DisplayName("Each preset should have combat capability")
    void testPresetHasCombatUnits(CustomFactionConfig preset) {
        long combatUnitCount = preset.getSelectedUnits().stream()
                .filter(unit -> !unit.name().equals("WORKER"))
                .filter(unit -> unit.getDamage() > 0)
                .count();
        
        assertTrue(combatUnitCount > 0,
                String.format("Preset %s should have at least one combat unit", 
                        preset.getDisplayName()));
    }

    @ParameterizedTest(name = "{0} should have resource production capability")
    @MethodSource("provideAllPresets")
    @DisplayName("Each preset should be able to produce resources")
    void testPresetHasResourceProduction(CustomFactionConfig preset) {
        boolean hasRefinery = preset.getSelectedBuildings().stream()
                .anyMatch(building -> building.name().equals("REFINERY"));
        
        boolean hasWorker = preset.getSelectedUnits().stream()
                .anyMatch(unit -> unit.name().equals("WORKER"));
        
        assertTrue(hasRefinery && hasWorker,
                String.format("Preset %s should have both REFINERY and WORKER for resource production",
                        preset.getDisplayName()));
    }

    @Test
    @DisplayName("Point calculation should be consistent")
    void testPointCalculationConsistency() {
        for (CustomFactionConfig preset : FactionPresetRegistry.getAllPresets()) {
            int firstCalculation = preset.calculateTotalPoints();
            int secondCalculation = preset.calculateTotalPoints();
            
            assertEquals(firstCalculation, secondCalculation,
                    String.format("Preset %s should have consistent point calculations",
                            preset.getDisplayName()));
        }
    }

    @Test
    @DisplayName("Presets should be retrievable by ID")
    void testPresetRetrieval() {
        for (String presetId : FactionPresetRegistry.getPresetIds()) {
            CustomFactionConfig preset = FactionPresetRegistry.getPreset(presetId);
            
            assertNotNull(preset, 
                    String.format("Should be able to retrieve preset with ID: %s", presetId));
            assertEquals(presetId, preset.getFactionId(),
                    "Retrieved preset should have matching faction ID");
        }
    }

    @Test
    @DisplayName("Getting non-existent preset should return null")
    void testNonExistentPreset() {
        CustomFactionConfig preset = FactionPresetRegistry.getPreset("NON_EXISTENT_PRESET");
        assertNull(preset, "Non-existent preset should return null");
        
        assertFalse(FactionPresetRegistry.hasPreset("NON_EXISTENT_PRESET"),
                "hasPreset should return false for non-existent preset");
    }

    @Test
    @DisplayName("Print point breakdown for all presets")
    void printPresetPointBreakdown() {
        System.out.println("\n=== FACTION PRESET POINT BREAKDOWN ===\n");
        
        for (CustomFactionConfig preset : FactionPresetRegistry.getAllPresets()) {
            int totalPoints = preset.calculateTotalPoints();
            int variance = totalPoints - TARGET_POINTS;
            String status = Math.abs(variance) <= ACCEPTABLE_VARIANCE ? "✅" : "⚠️";
            
            System.out.printf("%s %s: %d points (target: %d, variance: %+d)\n",
                    status, preset.getDisplayName(), totalPoints, TARGET_POINTS, variance);
            System.out.printf("   Units: %d, Buildings: %d, Perks: %d\n",
                    preset.getSelectedUnits().size(),
                    preset.getSelectedBuildings().size(),
                    preset.getSelectedPerks().size());
            System.out.println();
        }
        
        System.out.println("=== END BREAKDOWN ===\n");
    }
}
