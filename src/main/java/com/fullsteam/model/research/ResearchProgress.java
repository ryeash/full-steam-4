package com.fullsteam.model.research;

import lombok.Data;

/**
 * Tracks the progress of an active research project.
 */
@Data
public class ResearchProgress {
    private final ResearchType researchType;
    private final int buildingId; // ID of the building performing the research
    private final long startTime; // System time when research started
    private double progress; // 0.0 to 1.0 (0% to 100%)
    
    public ResearchProgress(ResearchType researchType, int buildingId) {
        this.researchType = researchType;
        this.buildingId = buildingId;
        this.startTime = System.currentTimeMillis();
        this.progress = 0.0;
    }
    
    /**
     * Update research progress based on elapsed time
     * @param deltaTime Time elapsed in seconds
     */
    public void update(double deltaTime) {
        double totalTime = researchType.getResearchTimeSeconds();
        double progressPerSecond = 1.0 / totalTime;
        progress = Math.min(1.0, progress + (progressPerSecond * deltaTime));
    }
    
    /**
     * Check if research is complete
     */
    public boolean isComplete() {
        return progress >= 1.0;
    }
    
    /**
     * Get progress as a percentage (0-100)
     */
    public int getProgressPercent() {
        return (int) Math.round(progress * 100);
    }
    
    /**
     * Get estimated time remaining in seconds
     */
    public int getTimeRemaining() {
        if (progress >= 1.0) {
            return 0;
        }
        double totalTime = researchType.getResearchTimeSeconds();
        double timeElapsed = progress * totalTime;
        return (int) Math.ceil(totalTime - timeElapsed);
    }
}

