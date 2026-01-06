package com.fullsteam.model.customization;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for faction preset configurations
 */
public class FactionPresetTest {
    
    @Test
    public void testAllPresetsExist() {
        Collection<CustomFactionConfig> presets = FactionPresetRegistry.getAllPresets();
        
        assertNotNull(presets);
        assertEquals(10, presets.size(), "Should have 10 faction presets");
    }
    
    @Test
    public void testAllPresetsAreValid() {
        for (CustomFactionConfig preset : FactionPresetRegistry.getAllPresets()) {
            System.out.println("\n=== " + preset.getDisplayName() + " ===");
            System.out.println("ID: " + preset.getFactionId());
            System.out.println("Points: " + preset.getTotalPointsSpent() + " / " + preset.getMaxPoints());
            System.out.println("Units: " + preset.getSelectedUnits().size());
            System.out.println("Buildings: " + preset.getSelectedBuildings().size());
            System.out.println("Perks: " + preset.getSelectedPerks().size());
            
            ValidationResult validation = preset.validate();
            
            if (!validation.isValid()) {
                System.out.println("ERRORS:");
                for (String error : validation.getErrors()) {
                    System.out.println("  - " + error);
                }
            }
            
            assertTrue(validation.isValid(), 
                "Preset " + preset.getDisplayName() + " should be valid. Errors: " + validation.getAllErrors());
            assertTrue(preset.getTotalPointsSpent() <= preset.getMaxPoints(),
                "Preset " + preset.getDisplayName() + " should be within budget");
        }
    }
    
    @Test
    public void testGetPresetById() {
        CustomFactionConfig terran = FactionPresetRegistry.getPreset("TERRAN_STANDARD");
        
        assertNotNull(terran);
        assertEquals("Terran Coalition", terran.getDisplayName());
        assertEquals("🛡️", terran.getIcon());
    }
    
    @Test
    public void testPresetHasRequiredEntities() {
        for (CustomFactionConfig preset : FactionPresetRegistry.getAllPresets()) {
            assertTrue(preset.getSelectedUnits().contains(com.fullsteam.model.UnitType.WORKER),
                preset.getDisplayName() + " should include WORKER");
            assertTrue(preset.getSelectedBuildings().contains(com.fullsteam.model.BuildingType.HEADQUARTERS),
                preset.getDisplayName() + " should include HEADQUARTERS");
        }
    }
    
    @Test
    public void testPerkDependencies() {
        for (CustomFactionConfig preset : FactionPresetRegistry.getAllPresets()) {
            ValidationResult validation = PerkValidator.validate(preset.getSelectedPerks());
            
            assertTrue(validation.isValid(),
                preset.getDisplayName() + " should have valid perk dependencies. Errors: " + validation.getAllErrors());
        }
    }
    
    @Test
    public void testPointCalculation() {
        CustomFactionConfig terran = FactionPresetRegistry.getPreset("TERRAN_STANDARD");
        
        int calculated = terran.calculateTotalPoints();
        int stored = terran.getTotalPointsSpent();
        
        assertEquals(calculated, stored,
            "Calculated points should match stored points for " + terran.getDisplayName());
    }
}
