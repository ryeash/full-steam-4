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
    private final long endTime;

    public ResearchProgress(ResearchType researchType, int buildingId) {
        this.researchType = researchType;
        this.buildingId = buildingId;
        this.startTime = System.currentTimeMillis();
        this.endTime = System.currentTimeMillis() + (researchType.getResearchTimeSeconds() * 1000L);
    }

    /**
     * Check if research is complete
     */
    public boolean isComplete() {
        return System.currentTimeMillis() > endTime;
    }

    /**
     * Get progress as a percentage (0-100)
     */
    public int getProgressPercent() {
        long totalDuration = researchType.getResearchTimeSeconds() * 1000L;
        long elapsed = System.currentTimeMillis() - startTime;
        return (int) Math.min(100, (elapsed * 100) / totalDuration);
    }

    /**
     * Get estimated time remaining in seconds
     */
    public int getTimeRemaining() {
        return (int) Math.max((endTime - System.currentTimeMillis()) / 1000, 0);
    }
}

