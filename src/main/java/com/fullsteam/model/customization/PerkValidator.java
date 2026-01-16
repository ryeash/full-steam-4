package com.fullsteam.model.customization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility for validating faction perk selections and checking dependencies
 */
public class PerkValidator {

    /**
     * Validate a set of selected perks
     */
    public static ValidationResult validate(Set<FactionPerk> selectedPerks) {
        List<String> errors = new ArrayList<>();

        // Check dependencies for each selected perk
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
     * Get perks that can be added given current selection
     */
    public static Set<FactionPerk> getAvailablePerks(Set<FactionPerk> selectedPerks) {
        Set<FactionPerk> available = new HashSet<>();

        for (FactionPerk perk : FactionPerk.values()) {
            // Skip if already selected
            if (selectedPerks.contains(perk)) {
                continue;
            }

            // Check if dependencies are met
            if (perk.canSelect(selectedPerks)) {
                available.add(perk);
            }
        }

        return available;
    }

    /**
     * Get perks that are currently locked (dependencies not met)
     */
    public static Set<FactionPerk> getLockedPerks(Set<FactionPerk> selectedPerks) {
        Set<FactionPerk> locked = new HashSet<>();

        for (FactionPerk perk : FactionPerk.values()) {
            // Skip if already selected
            if (selectedPerks.contains(perk)) {
                continue;
            }

            // Check if dependencies are NOT met
            if (!perk.canSelect(selectedPerks)) {
                locked.add(perk);
            }
        }

        return locked;
    }

    /**
     * Get perks that will become unavailable if the given perk is removed.
     * This includes direct dependents and their transitive dependents.
     */
    public static Set<FactionPerk> getDependentPerks(
            FactionPerk perk,
            Set<FactionPerk> selectedPerks) {

        Set<FactionPerk> dependents = new HashSet<>();

        for (FactionPerk selected : selectedPerks) {
            if (selected.getDependsOn().contains(perk)) {
                dependents.add(selected);
                // Recursively check dependents of this dependent
                dependents.addAll(getDependentPerks(selected, selectedPerks));
            }
        }

        return dependents;
    }

    /**
     * Get the missing dependencies for a perk
     */
    public static Set<FactionPerk> getMissingDependencies(
            FactionPerk perk,
            Set<FactionPerk> selectedPerks) {

        Set<FactionPerk> missing = new HashSet<>(perk.getDependsOn());
        missing.removeAll(selectedPerks);
        return missing;
    }

    /**
     * Check if a perk can be added to the current selection
     */
    public static boolean canAdd(FactionPerk perk, Set<FactionPerk> selectedPerks) {
        return !selectedPerks.contains(perk) && perk.canSelect(selectedPerks);
    }

    /**
     * Check if a perk can be removed from the current selection
     * (returns false if other selected perks depend on it)
     */
    public static boolean canRemove(FactionPerk perk, Set<FactionPerk> selectedPerks) {
        return getDependentPerks(perk, selectedPerks).isEmpty();
    }

    /**
     * Get perks grouped by category
     */
    public static Map<FactionPerk.PerkCategory, List<FactionPerk>> getPerksbyCategory() {
        return Arrays.stream(FactionPerk.values())
                .collect(Collectors.groupingBy(FactionPerk::getCategory));
    }

    /**
     * Get a detailed status for a perk given the current selection
     */
    public static PerkStatus getStatus(FactionPerk perk, Set<FactionPerk> selectedPerks) {
        if (selectedPerks.contains(perk)) {
            Set<FactionPerk> dependents = getDependentPerks(perk, selectedPerks);
            return new PerkStatus(
                    PerkState.SELECTED,
                    Collections.emptySet(),
                    dependents
            );
        }

        if (perk.canSelect(selectedPerks)) {
            return new PerkStatus(
                    PerkState.AVAILABLE,
                    Collections.emptySet(),
                    Collections.emptySet()
            );
        }

        Set<FactionPerk> missing = getMissingDependencies(perk, selectedPerks);
        return new PerkStatus(
                PerkState.LOCKED,
                missing,
                Collections.emptySet()
        );
    }

    /**
     * State of a perk
     */
    public enum PerkState {
        SELECTED,   // Currently selected
        AVAILABLE,  // Can be selected (dependencies met)
        LOCKED      // Cannot be selected (missing dependencies)
    }

    /**
     * Status information for a perk
     */
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class PerkStatus {
        private final PerkState state;
        private final Set<FactionPerk> missingDependencies;
        private final Set<FactionPerk> dependents;

        public boolean isSelected() {
            return state == PerkState.SELECTED;
        }

        public boolean isAvailable() {
            return state == PerkState.AVAILABLE;
        }

        public boolean isLocked() {
            return state == PerkState.LOCKED;
        }

        public boolean hasDependents() {
            return !dependents.isEmpty();
        }
    }
}
