package com.fullsteam.dto;

import com.fullsteam.model.research.UnifiedResearchProgress;
import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object for active unit research progress.
 * Sent to client to show research progress bars.
 */
@Data
@Builder
public class UnitResearchProgressDTO {
    
    private String researchId;           // e.g., "TERRAN_VEH_TANK"
    private String displayName;          // e.g., "Main Battle Tank"
    private int buildingId;              // Building performing the research
    private int requiredTimeSeconds;     // Total time required
    private double progressSeconds;      // Current progress
    private double progressPercent;      // Progress as percentage (0 to 100)
    private int remainingSeconds;        // Estimated time remaining
    private boolean isPaused;            // True if building is unpowered
    
    /**
     * Build DTO from UnifiedResearchProgress (for unit research)
     */
    public static UnitResearchProgressDTO fromUnifiedProgress(
            UnifiedResearchProgress progress, 
            String displayName,
            boolean isPaused) {
        
        if (!progress.isUnitResearch()) {
            throw new IllegalArgumentException("Progress must be unit research");
        }
        
        // Convert from 0.0-1.0 to 0-100 for frontend display
        double percent = progress.getProgressPercent() * 100.0;
        int remaining = (int) Math.ceil(progress.getRemainingSeconds());
        
        return UnitResearchProgressDTO.builder()
                .researchId(progress.getUnitResearchId())
                .displayName(displayName)
                .buildingId(progress.getBuildingId())
                .requiredTimeSeconds((int) progress.getRequiredTimeSeconds())
                .progressSeconds(progress.getProgressSeconds())
                .progressPercent(percent)
                .remainingSeconds(remaining)
                .isPaused(isPaused)
                .build();
    }
}
