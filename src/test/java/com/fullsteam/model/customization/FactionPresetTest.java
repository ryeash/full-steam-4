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
 * Ensures all presets are valid, balanced, stay within {@link CustomFactionConfig#MAX_FACTION_POINTS},
 * and spend the full budget where {@link #ACCEPTABLE_VARIANCE} allows.
 */
@DisplayName("Faction Preset Tests")
public class FactionPresetTest extends BaseTestClass {

    /** Target spend for curated presets (matches {@link CustomFactionConfig#MAX_FACTION_POINTS}). */
    private static final int TARGET_POINTS = CustomFactionConfig.MAX_FACTION_POINTS;
    /** Allowed deviation from {@link #TARGET_POINTS} (0 = presets must hit exactly 100). */
    private static final int ACCEPTABLE_VARIANCE = 0;

    /**
     * Provide all presets for parameterized tests
     */
    static Stream<CustomFactionConfig> provideAllPresets() {
        return FactionPresetRegistry.getAllPresets().stream();
    }

    @ParameterizedTest(name = "{0} should be valid")
    @MethodSource("provideAllPresets")
    @DisplayName("Each preset should pass validation")
    void testPresetValidation(CustomFactionConfig preset) {
        ValidationResult vr = preset.validate();
        assertTrue(vr.isValid(),
                () -> String.format("Preset %s should be valid: %s",
                        preset.getDisplayName(), vr.getAllErrors()));
    }

    @ParameterizedTest(name = "{0} must not exceed faction point budget")
    @MethodSource("provideAllPresets")
    @DisplayName("Each preset stays within max faction points")
    void testPresetNotOverBudget(CustomFactionConfig preset) {
        int calculated = preset.calculateTotalPoints();
        int stored = preset.getTotalPointsSpent();

        assertTrue(calculated <= CustomFactionConfig.MAX_FACTION_POINTS,
                () -> String.format("Preset %s: calculated points %d exceed max %d",
                        preset.getDisplayName(), calculated, CustomFactionConfig.MAX_FACTION_POINTS));
        assertTrue(stored <= CustomFactionConfig.MAX_FACTION_POINTS,
                () -> String.format("Preset %s: stored points %d exceed max %d",
                        preset.getDisplayName(), stored, CustomFactionConfig.MAX_FACTION_POINTS));

        ValidationResult vr = preset.validate();
        assertFalse(vr.getErrors().stream().anyMatch(msg -> msg.contains("Over budget")),
                () -> String.format("Preset %s should not report over budget: %s",
                        preset.getDisplayName(), vr.getErrors()));
    }

    @ParameterizedTest(name = "{0} should be at or near target points")
    @MethodSource("provideAllPresets")
    @DisplayName("Each preset should spend the target point budget (within variance)")
    void testPresetPointBalance(CustomFactionConfig preset) {
        int totalPoints = preset.calculateTotalPoints();
        int storedPoints = preset.getTotalPointsSpent();

        // Verify calculated points match stored points
        assertEquals(totalPoints, storedPoints,
                String.format("Preset %s: calculated points (%d) should match stored points (%d)",
                        preset.getDisplayName(), totalPoints, storedPoints));

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
}
