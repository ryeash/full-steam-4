package com.fullsteam.model.research;

import lombok.Getter;

/**
 * Tracks progress for any type of research (building upgrades or unit unlocks).
 * Supports both ResearchType (building) and UnitResearchNode (unit) research.
 */
@Getter
public class UnifiedResearchProgress {
    
    // Type discriminator
    private final ResearchCategory category;
    
    // Building research fields (if category == BUILDING)
    private final ResearchType researchType;
    
    // Unit research fields (if category == UNIT)
    private final String unitResearchId;
    
    // Common fields
    private final int buildingId;              // Building where research is happening
    private final double requiredTimeSeconds;  // Total time needed
    private double progressSeconds = 0.0;      // Current progress
    
    public enum ResearchCategory {
        BUILDING,  // Building upgrade research (ResearchType)
        UNIT       // Unit tech tree research (UnitResearchNode)
    }
    
    /**
     * Constructor for building research
     */
    public UnifiedResearchProgress(ResearchType researchType, int buildingId) {
        this.category = ResearchCategory.BUILDING;
        this.researchType = researchType;
        this.unitResearchId = null;
        this.buildingId = buildingId;
        this.requiredTimeSeconds = researchType.getResearchTimeSeconds();
    }
    
    /**
     * Constructor for unit research
     */
    public UnifiedResearchProgress(String unitResearchId, int buildingId, double requiredTimeSeconds) {
        this.category = ResearchCategory.UNIT;
        this.researchType = null;
        this.unitResearchId = unitResearchId;
        this.buildingId = buildingId;
        this.requiredTimeSeconds = requiredTimeSeconds;
    }
    
    /**
     * Add progress time
     */
    public void addProgress(double deltaTime) {
        progressSeconds += deltaTime;
    }
    
    /**
     * Check if research is complete
     */
    public boolean isComplete() {
        return progressSeconds >= requiredTimeSeconds;
    }
    
    /**
     * Get progress as a percentage (0.0 to 1.0)
     */
    public double getProgressPercent() {
        if (requiredTimeSeconds <= 0) {
            return 1.0;
        }
        return Math.min(1.0, progressSeconds / requiredTimeSeconds);
    }
    
    /**
     * Get remaining time in seconds
     */
    public double getRemainingSeconds() {
        return Math.max(0, requiredTimeSeconds - progressSeconds);
    }
    
    /**
     * Check if this is building research
     */
    public boolean isBuildingResearch() {
        return category == ResearchCategory.BUILDING;
    }
    
    /**
     * Check if this is unit research
     */
    public boolean isUnitResearch() {
        return category == ResearchCategory.UNIT;
    }
}
