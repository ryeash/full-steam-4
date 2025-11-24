package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.research.ResearchProgress;
import com.fullsteam.model.research.ResearchType;
import lombok.extern.slf4j.Slf4j;

/**
 * Component for buildings that can perform research.
 * Handles research queue and progress for RESEARCH_LAB and TECH_CENTER buildings.
 */
@Slf4j
public class ResearchComponent implements IBuildingComponent {
    
    private final Building building;
    
    // Current research progress (null if not researching)
    private ResearchProgress currentResearch = null;
    
    public ResearchComponent(Building building) {
        this.building = building;
    }
    
    @Override
    public void update(double deltaTime, Building building, boolean isUnderConstruction) {
        // Research progress is managed by ResearchManager in RTSGameManager
        // This component just tracks which building is researching what
        // The actual progress update happens in ResearchManager.updateResearch()
    }
    
    /**
     * Start research at this building
     */
    public boolean startResearch(ResearchType researchType) {
        if (currentResearch != null) {
            log.warn("Building {} is already researching {}", building.getId(), currentResearch.getResearchType());
            return false;
        }
        
        // Create research progress
        currentResearch = new ResearchProgress(researchType, building.getId());
        log.info("Building {} started research {}", building.getId(), researchType);
        return true;
    }
    
    /**
     * Cancel current research
     */
    public boolean cancelResearch() {
        if (currentResearch == null) {
            return false;
        }
        
        log.info("Building {} cancelled research {}", building.getId(), currentResearch.getResearchType());
        currentResearch = null;
        return true;
    }
    
    /**
     * Complete current research
     */
    public void completeResearch() {
        if (currentResearch != null) {
            log.info("Building {} completed research {}", building.getId(), currentResearch.getResearchType());
            currentResearch = null;
        }
    }
    
    /**
     * Check if this building is currently researching
     */
    public boolean isResearching() {
        return currentResearch != null;
    }
    
    /**
     * Get current research progress
     */
    public ResearchProgress getCurrentResearch() {
        return currentResearch;
    }
    
    /**
     * Get current research type (null if not researching)
     */
    public ResearchType getCurrentResearchType() {
        return currentResearch != null ? currentResearch.getResearchType() : null;
    }
    
    /**
     * Get research progress percentage (0-100)
     */
    public int getResearchProgress() {
        return currentResearch != null ? currentResearch.getProgressPercent() : 0;
    }
    
    /**
     * Update research progress (called by ResearchManager)
     */
    public void updateResearchProgress(ResearchProgress progress) {
        this.currentResearch = progress;
    }
}

