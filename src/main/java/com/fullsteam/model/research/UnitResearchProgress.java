package com.fullsteam.model.research;

import lombok.Getter;

/**
 * Tracks the progress of an active unit research.
 * Each research is tied to a specific building (research lab or production building).
 */
@Getter
public class UnitResearchProgress {
    private final String researchId;
    private final int buildingId;
    private final int requiredTimeSeconds;
    private double progressSeconds;

    public UnitResearchProgress(String researchId, int buildingId, int requiredTimeSeconds) {
        this.researchId = researchId;
        this.buildingId = buildingId;
        this.requiredTimeSeconds = requiredTimeSeconds;
        this.progressSeconds = 0.0;
    }

    /**
     * Add progress to this research
     * @param deltaTime Time elapsed in seconds
     */
    public void addProgress(double deltaTime) {
        this.progressSeconds += deltaTime;
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
        return Math.min(1.0, progressSeconds / requiredTimeSeconds);
    }

    /**
     * Get remaining time in seconds
     */
    public double getRemainingSeconds() {
        return Math.max(0, requiredTimeSeconds - progressSeconds);
    }
}
