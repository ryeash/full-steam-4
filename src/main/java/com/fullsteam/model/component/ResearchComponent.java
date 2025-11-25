package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.GameEntities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Component for buildings that can perform research.
 * Handles research queue and progress for RESEARCH_LAB and TECH_CENTER buildings.
 */
@Slf4j
@Getter
public class ResearchComponent implements IBuildingComponent {

    private final Building building;

    public ResearchComponent(Building building) {
        this.building = building;
    }

    @Override
    public void update(GameEntities gameEntities, Building building, boolean isUnderConstruction) {
        // Research progress is managed by ResearchManager in RTSGameManager
        // This component just tracks which building is researching what
        // The actual progress update happens in ResearchManager.updateResearch()
    }
}

