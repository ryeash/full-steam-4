package com.fullsteam.model.research;

import com.fullsteam.model.UnitCategory;
import com.fullsteam.model.UnitType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Represents a single research node in a faction's unit tech tree.
 * Each node unlocks a unit, and may optionally replace an existing unit.
 */
@Getter
@Builder
public class UnitResearchNode {
    /**
     * Unique identifier for this research node (e.g., "TERRAN_INF_MEDIC")
     */
    private final String id;

    /**
     * Display name shown to players (e.g., "Medic Training")
     */
    private final String displayName;

    /**
     * Description explaining what this research does
     */
    private final String description;

    /**
     * The unit type this research unlocks
     */
    private final UnitType unitToUnlock;

    /**
     * Optional: If set, this unit will be replaced when research completes.
     * Existing units of this type remain on the map, but it's removed from production.
     */
    private final UnitType unitToReplace;

    /**
     * Cost in credits to research this
     */
    private final int creditCost;

    /**
     * Time in seconds to complete this research
     */
    private final int researchTimeSeconds;

    /**
     * Research tier determines building requirements
     */
    private final ResearchTier tier;

    /**
     * Category of unit (INFANTRY, VEHICLE, FLYER)
     */
    private final UnitCategory category;

    /**
     * List of research IDs that must be completed before this can be researched
     */
    @Builder.Default
    private final List<String> prerequisiteIds = List.of();

    /**
     * List of research IDs that become locked if this research is completed.
     * Used for branching tech trees where choosing one path locks out another.
     */
    @Builder.Default
    private final List<String> mutuallyExclusiveIds = List.of();

    /**
     * Check if this research replaces an existing unit
     */
    public boolean isReplacement() {
        return unitToReplace != null;
    }

    /**
     * Check if this research has prerequisites
     */
    public boolean hasPrerequisites() {
        return !prerequisiteIds.isEmpty();
    }

    /**
     * Check if this research locks out other research (branching path)
     */
    public boolean hasMutualExclusions() {
        return !mutuallyExclusiveIds.isEmpty();
    }
}
