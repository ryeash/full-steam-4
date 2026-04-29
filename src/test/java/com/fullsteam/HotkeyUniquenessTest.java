package com.fullsteam;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hotkeys are scoped to UI context (worker build menu vs each production building).
 * The same letter may appear in different contexts; within one context it must be unique or null.
 */
@DisplayName("Hotkey uniqueness per UI context")
class HotkeyUniquenessTest {

    @Test
    void workerBuildMenu_hotkeysUniqueAmongPlaceableBuildings() {
        assertNoDuplicateHotkeys(
                "worker build (placeable structures)",
                java.util.Arrays.stream(BuildingType.values())
                        .filter(bt -> bt != BuildingType.HEADQUARTERS));
    }

    @Test
    void headquartersTrain_hotkeysUnique() {
        assertNoDuplicateHotkeys(
                "Headquarters train menu",
                java.util.Arrays.stream(UnitType.values())
                        .filter(u -> u.getProducedBy() == BuildingType.HEADQUARTERS));
    }

    @Test
    void barracks_hotkeysUnique() {
        assertNoDuplicateHotkeys(
                "Barracks production",
                java.util.Arrays.stream(UnitType.values())
                        .filter(u -> u.getProducedBy() == BuildingType.BARRACKS));
    }

    @Test
    void factory_hotkeysUnique() {
        assertNoDuplicateHotkeys(
                "Factory production",
                java.util.Arrays.stream(UnitType.values())
                        .filter(u -> u.getProducedBy() == BuildingType.FACTORY));
    }

    @Test
    void airfield_hotkeysUnique() {
        assertNoDuplicateHotkeys(
                "Airfield production",
                java.util.Arrays.stream(UnitType.values())
                        .filter(u -> u.getProducedBy() == BuildingType.AIRFIELD));
    }

    @Test
    void androidFactory_hotkeysUnique() {
        assertNoDuplicateHotkeys(
                "Android Factory production",
                java.util.Arrays.stream(UnitType.values())
                        .filter(u -> u.getProducedBy() == BuildingType.ANDROID_FACTORY));
    }

    /** Client reserves WASD for camera pan ({@code RTSEngine} update loop). */
    @Test
    void hotkeysNeverUseWasdCameraPanKeys() {
        for (UnitType u : UnitType.values()) {
            Character hk = u.getHotkey();
            if (hk != null) {
                char c = Character.toUpperCase(hk);
                assertTrue(c != 'W' && c != 'A' && c != 'S' && c != 'D',
                        () -> u.name() + ": hotkey must not use W/A/S/D (reserved for camera)");
            }
        }
        for (BuildingType b : BuildingType.values()) {
            Character hk = b.getHotkey();
            if (hk != null) {
                char c = Character.toUpperCase(hk);
                assertTrue(c != 'W' && c != 'A' && c != 'S' && c != 'D',
                        () -> b.name() + ": hotkey must not use W/A/S/D (reserved for camera)");
            }
        }
    }

    private static void assertNoDuplicateHotkeys(String contextName,
                                                 java.util.stream.Stream<?> entries) {
        Map<Character, String> seen = new HashMap<>();
        entries.forEach(entry -> {
            Character hk = entry instanceof BuildingType bt ? bt.getHotkey()
                    : entry instanceof UnitType ut ? ut.getHotkey() : null;
            String name = entry instanceof BuildingType b ? b.name()
                    : entry instanceof UnitType u ? u.name() : entry.toString();
            if (hk == null) {
                return;
            }
            Character key = Character.toUpperCase(hk);
            String prev = seen.put(key, name);
            assertNull(prev,
                    () -> String.format("%s: duplicate hotkey '%s' on %s and %s",
                            contextName, key, prev, name));
        });
    }
}
