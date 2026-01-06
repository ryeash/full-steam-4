package com.fullsteam.model.customization;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Configuration for a custom faction created by a player.
 * Contains all selections (units, buildings, perks) and validates against point budget.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFactionConfig {
    private static final int MAX_POINTS = 100;

    /**
     * Unique identifier for this faction configuration
     */
    @Builder.Default
    private String factionId = UUID.randomUUID().toString();

    /**
     * Display name chosen by player
     */
    private String displayName;

    /**
     * Theme color (hex string, e.g., "#4A90E2")
     */
    private String themeColor;

    /**
     * Icon emoji or path
     */
    private String icon;

    // ===== SELECTIONS =====

    /**
     * Selected unit types
     */
    @Builder.Default
    private Set<UnitType> selectedUnits = new HashSet<>();

    /**
     * Selected building types
     */
    @Builder.Default
    private Set<BuildingType> selectedBuildings = new HashSet<>();

    /**
     * Selected faction perks
     */
    @Builder.Default
    private Set<FactionPerk> selectedPerks = new HashSet<>();

    // ===== BUDGET =====

    /**
     * Total points spent (calculated)
     */
    @Setter
    private int totalPointsSpent;

    /**
     * Optional: ID of preset this was based on
     */
    private String basedOnPreset;

    // ===== VALIDATION =====

    /**
     * Check if this configuration is valid
     */
    public boolean isValid() {
        return totalPointsSpent <= MAX_POINTS
                && hasRequiredBuildings()
                && hasRequiredUnits()
                && allPerksValid();
    }

    /**
     * Check if required buildings are present
     */
    private boolean hasRequiredBuildings() {
        // Must have HEADQUARTERS (always required)
        if (!selectedBuildings.contains(BuildingType.HEADQUARTERS)) {
            return false;
        }

        // Must have at least one production building
        return hasAtLeastOneProduction();
    }

    /**
     * Check if at least one production building is present
     */
    private boolean hasAtLeastOneProduction() {
        return selectedBuildings.stream()
                .anyMatch(BuildingType::isCanProduceUnits);
    }

    /**
     * Check if required units are present
     */
    private boolean hasRequiredUnits() {
        // Must have WORKER (always required)
        if (!selectedUnits.contains(UnitType.WORKER)) {
            return false;
        }

        // Must have at least one combat unit
        return hasAtLeastOneCombat();
    }

    /**
     * Check if at least one combat unit is present
     */
    private boolean hasAtLeastOneCombat() {
        return selectedUnits.stream()
                .anyMatch(unit -> unit.canAttack() && unit != UnitType.WORKER);
    }

    /**
     * Check if all perk dependencies are satisfied
     */
    private boolean allPerksValid() {
        for (FactionPerk perk : selectedPerks) {
            if (!perk.canSelect(selectedPerks)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Ensure bundled units are included with their buildings.
     * Some units are tightly coupled to specific buildings and should be auto-included.
     */
    public void ensureBundledUnits() {
        // ANDROID is bundled with ANDROID_FACTORY (monument building)
        if (selectedBuildings.contains(BuildingType.ANDROID_FACTORY)) {
            selectedUnits.add(UnitType.ANDROID);
        }
        // If factory is removed, remove the android too
        if (!selectedBuildings.contains(BuildingType.ANDROID_FACTORY)) {
            selectedUnits.remove(UnitType.ANDROID);
        }
    }

    /**
     * Calculate total points spent
     */
    public int calculateTotalPoints() {
        int total = 0;

        // Units (ANDROID is free - bundled with ANDROID_FACTORY)
        for (UnitType unit : selectedUnits) {
            // Skip ANDROID - it's bundled with the factory
            if (unit == UnitType.ANDROID) {
                continue;
            }
            total += UnitTemplate.fromUnitType(unit).getPointCost();
        }

        // Buildings
        for (BuildingType building : selectedBuildings) {
            total += BuildingTemplate.fromBuildingType(building).getPointCost();
        }

        // Perks
        for (FactionPerk perk : selectedPerks) {
            total += perk.getPointCost();
        }

        return total;
    }

    /**
     * Get remaining points in budget
     */
    public int getRemainingPoints() {
        return MAX_POINTS - totalPointsSpent;
    }

    /**
     * Get effective perks (highest tier only in each chain).
     * Filters out lower-tier perks that are superseded by higher tiers.
     * <p>
     * Example: If player selected [POWER_EFFICIENCY_1, POWER_EFFICIENCY_2, POWER_EFFICIENCY_3],
     * this returns only [POWER_EFFICIENCY_3] since it depends on the others.
     * <p>
     * This prevents stacking of tiered perks while still requiring players to
     * select all tiers (and pay for them) to unlock higher tiers.
     *
     * @return Set of perks that should actually be applied (highest tier only)
     */
    public Set<FactionPerk> getEffectivePerks() {
        Set<FactionPerk> effective = new HashSet<>(selectedPerks);

        // For each selected perk, remove any perks it depends on
        // This keeps only the "leaf" perks in each dependency chain
        for (FactionPerk perk : selectedPerks) {
            Set<FactionPerk> dependencies = perk.getDependsOn();
            effective.removeAll(dependencies);
        }

        return effective;
    }

    /**
     * Check if we can afford to add an entity with the given cost
     */
    public boolean canAfford(int pointCost) {
        return (totalPointsSpent + pointCost) <= MAX_POINTS;
    }

    /**
     * Get validation errors (if any)
     */
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();

        if (totalPointsSpent > MAX_POINTS) {
            errors.add(String.format("Over budget: %d / %d points", totalPointsSpent, MAX_POINTS));
        }

        if (!selectedBuildings.contains(BuildingType.HEADQUARTERS)) {
            errors.add("HEADQUARTERS is required");
        }

        if (!hasAtLeastOneProduction()) {
            errors.add("At least one production building is required");
        }

        if (!selectedUnits.contains(UnitType.WORKER)) {
            errors.add("WORKER is required");
        }

        if (!hasAtLeastOneCombat()) {
            errors.add("At least one combat unit is required");
        }

        selectedPerks.removeIf(Objects::isNull);

        // Check perk dependencies
        for (FactionPerk perk : selectedPerks) {
            if (!perk.canSelect(selectedPerks)) {
                Set<FactionPerk> missing = new HashSet<>(perk.getDependsOn());
                missing.removeAll(selectedPerks);
                errors.add(String.format(
                        "%s requires: %s",
                        perk.getDisplayName(),
                        missing.stream()
                                .map(FactionPerk::getDisplayName)
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("(unknown)")
                ));
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Get a summary of this faction configuration
     */
    public String getSummary() {
        return String.format(
                "%s: %d units, %d buildings, %d perks (%d/%d points)",
                displayName,
                selectedUnits.size(),
                selectedBuildings.size(),
                selectedPerks.size(),
                totalPointsSpent,
                MAX_POINTS
        );
    }
}
