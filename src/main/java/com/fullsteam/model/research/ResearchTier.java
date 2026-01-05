package com.fullsteam.model.research;

import com.fullsteam.model.BuildingType;
import lombok.Getter;

import java.util.Set;

/**
 * Research tiers determine what buildings are required to unlock research.
 * Higher tiers require more advanced tech buildings to be constructed.
 */
@Getter
public enum ResearchTier {
    /**
     * Basic research - no research building required, only production building
     * Examples: MEDIC, TANK, HELICOPTER
     */
    BASIC("Basic", "No research building required", Set.of()),

    /**
     * Advanced research - requires RESEARCH_LAB to be built and powered
     * Examples: SNIPER, ARTILLERY, BOMBER
     */
    ADVANCED("Advanced", "Requires Research Lab", Set.of(BuildingType.RESEARCH_LAB)),

    /**
     * Elite research - requires both RESEARCH_LAB and TECH_CENTER
     * Examples: CRAWLER, ELITE_SNIPER, INTERCEPTOR
     */
    ELITE("Elite", "Requires Research Lab and Tech Center",
            Set.of(BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER));

    private final String displayName;
    private final String description;
    private final Set<BuildingType> requiredBuildings;

    ResearchTier(String displayName, String description, Set<BuildingType> requiredBuildings) {
        this.displayName = displayName;
        this.description = description;
        this.requiredBuildings = requiredBuildings;
    }

    /**
     * Check if player has all required buildings for this tier
     */
    public boolean hasRequiredBuildings(Set<BuildingType> playerBuildings) {
        return playerBuildings.containsAll(requiredBuildings);
    }
}
