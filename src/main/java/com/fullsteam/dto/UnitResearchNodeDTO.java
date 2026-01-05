package com.fullsteam.dto;

import com.fullsteam.model.research.UnitResearchNode;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object for a single unit research node.
 * Includes research data and current player state.
 */
@Data
@Builder
public class UnitResearchNodeDTO {
    
    // Research node data
    private String id;                      // e.g., "TERRAN_INF_MEDIC"
    private String displayName;             // e.g., "Medic Training"
    private String description;             // e.g., "Unlock Medic units..."
    private String unitToUnlock;            // e.g., "MEDIC" (UnitType name)
    private String unitToReplace;           // e.g., "INFANTRY" or null
    private int creditCost;                 // e.g., 250
    private int researchTimeSeconds;        // e.g., 35
    private String tier;                    // e.g., "BASIC", "ADVANCED", "ELITE"
    private String category;                // e.g., "INFANTRY", "VEHICLE", "FLYER"
    private List<String> prerequisiteIds;   // e.g., ["TERRAN_INF_SPECIALIZATION"]
    private List<String> mutuallyExclusiveIds; // e.g., ["TERRAN_INF_MARKSMAN_PATH"]
    
    // Player state (computed at request time)
    private String state;                   // "LOCKED", "AVAILABLE", "RESEARCHING", "COMPLETED"
    private Double progressPercent;         // 0 to 100 (only if RESEARCHING)
    private Integer remainingSeconds;       // Estimated time remaining (only if RESEARCHING)
    private List<String> lockReasons;       // Why node is locked (if state == LOCKED)
    
    /**
     * Build DTO from UnitResearchNode with no player state
     * (used for initial tree display before player state is known)
     */
    public static UnitResearchNodeDTO fromNode(UnitResearchNode node) {
        return UnitResearchNodeDTO.builder()
                .id(node.getId())
                .displayName(node.getDisplayName())
                .description(node.getDescription())
                .unitToUnlock(node.getUnitToUnlock() != null ? node.getUnitToUnlock().name() : null)
                .unitToReplace(node.getUnitToReplace() != null ? node.getUnitToReplace().name() : null)
                .creditCost(node.getCreditCost())
                .researchTimeSeconds(node.getResearchTimeSeconds())
                .tier(node.getTier().name())
                .category(node.getCategory().name())
                .prerequisiteIds(node.getPrerequisiteIds())
                .mutuallyExclusiveIds(node.getMutuallyExclusiveIds())
                .state("UNKNOWN")  // Will be filled in by withPlayerState()
                .build();
    }
    
    /**
     * Add player state to the DTO
     * This should be called after fromNode() to add current player's state
     */
    public UnitResearchNodeDTO withPlayerState(
            String state, 
            Double progressPercent, 
            Integer remainingSeconds,
            List<String> lockReasons) {
        this.state = state;
        this.progressPercent = progressPercent;
        this.remainingSeconds = remainingSeconds;
        this.lockReasons = lockReasons;
        return this;
    }
}
