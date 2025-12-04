package com.fullsteam.model.research;

import com.fullsteam.model.BuildingType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages research state for a player faction.
 * Tracks completed research, active research, and applies cumulative modifiers.
 */
@Slf4j
@Getter
public class ResearchManager {

    private final int playerId;

    // Completed research
    private final Set<ResearchType> completedResearch = new HashSet<>();

    // Active research (building ID -> research progress)
    private final Map<Integer, ResearchProgress> activeResearch = new HashMap<>();

    // Cached combined modifier (recalculated when research completes)
    private ResearchModifier cumulativeModifier = new ResearchModifier();

    // Maximum simultaneous research projects (base 1, can be upgraded)
    private static final int BASE_MAX_SIMULTANEOUS_RESEARCH = 1;

    public ResearchManager(int playerId) {
        this.playerId = playerId;
    }

    /**
     * Get the maximum number of simultaneous research projects allowed
     */
    public int getMaxSimultaneousResearch() {
        int max = BASE_MAX_SIMULTANEOUS_RESEARCH;

        // Add bonuses from research upgrades
        if (completedResearch.contains(ResearchType.PARALLEL_RESEARCH_1)) {
            max += 1; // +1 simultaneous research
        }
        if (completedResearch.contains(ResearchType.PARALLEL_RESEARCH_2)) {
            max += 1; // +1 more (total +2)
        }

        return max;
    }

    /**
     * Check if a research type can be started
     */
    public boolean canStartResearch(ResearchType researchType, Set<BuildingType> playerBuildings) {
        // Already completed
        if (completedResearch.contains(researchType)) {
            log.debug("Research {} already completed for player {}", researchType, playerId);
            return false;
        }

        // Already being researched
        if (isResearching(researchType)) {
            log.debug("Research {} already in progress for player {}", researchType, playerId);
            return false;
        }

        // Check prerequisites
        if (!hasPrerequisites(researchType)) {
            log.debug("Research {} prerequisites not met for player {}", researchType, playerId);
            return false;
        }

        // Check required building
        if (!playerBuildings.contains(researchType.getRequiredBuilding())) {
            log.debug("Research {} requires building {} for player {}",
                    researchType, researchType.getRequiredBuilding(), playerId);
            return false;
        }

        return true;
    }

    /**
     * Check if prerequisites are met for a research type
     */
    public boolean hasPrerequisites(ResearchType researchType) {
        return completedResearch.containsAll(researchType.getPrerequisites());
    }

    /**
     * Check if a research type is currently being researched
     */
    public boolean isResearching(ResearchType researchType) {
        return activeResearch.values().stream()
                .anyMatch(progress -> progress.getResearchType() == researchType);
    }

    /**
     * Check if a building is currently researching
     */
    public boolean isBuildingResearching(int buildingId) {
        return activeResearch.containsKey(buildingId);
    }

    /**
     * Start research at a specific building
     */
    public boolean startResearch(ResearchType researchType, int buildingId, Set<BuildingType> playerBuildings) {
        if (!canStartResearch(researchType, playerBuildings)) {
            return false;
        }

        // Check if building is already researching
        if (isBuildingResearching(buildingId)) {
            log.warn("Building {} is already researching for player {}", buildingId, playerId);
            return false;
        }

        // Check simultaneous research limit
        if (activeResearch.size() >= getMaxSimultaneousResearch()) {
            log.warn("Player {} has reached max simultaneous research limit ({}/{})",
                    playerId, activeResearch.size(), getMaxSimultaneousResearch());
            return false;
        }

        ResearchProgress progress = new ResearchProgress(researchType, buildingId);
        activeResearch.put(buildingId, progress);

        log.info("Player {} started research {} at building {} ({}/{} active)",
                playerId, researchType, buildingId, activeResearch.size(), getMaxSimultaneousResearch());
        return true;
    }

    /**
     * Cancel active research at a building
     */
    public boolean cancelResearch(int buildingId) {
        ResearchProgress removed = activeResearch.remove(buildingId);
        if (removed != null) {
            log.info("Player {} cancelled research {} at building {}",
                    playerId, removed.getResearchType(), buildingId);
            return true;
        }
        return false;
    }

    /**
     * Update all active research progress
     *
     * @param deltaTime Time elapsed in seconds
     * @return List of completed research types
     */
    public List<ResearchType> updateResearch(double deltaTime) {
        List<ResearchType> completed = new ArrayList<>();
        List<Integer> toRemove = new ArrayList<>();

        for (Map.Entry<Integer, ResearchProgress> entry : activeResearch.entrySet()) {
            ResearchProgress progress = entry.getValue();
            if (progress.isComplete()) {
                ResearchType researchType = progress.getResearchType();
                completeResearch(researchType);
                completed.add(researchType);
                toRemove.add(entry.getKey());
                log.info("Player {} completed research {}", playerId, researchType);
            }
        }

        // Remove completed research from active list
        toRemove.forEach(activeResearch::remove);

        return completed;
    }

    /**
     * Mark research as completed and recalculate modifiers
     */
    private void completeResearch(ResearchType researchType) {
        completedResearch.add(researchType);
        recalculateModifiers();
    }

    /**
     * Recalculate cumulative modifiers from all completed research
     */
    private void recalculateModifiers() {
        cumulativeModifier = new ResearchModifier();

        for (ResearchType research : completedResearch) {
            cumulativeModifier = cumulativeModifier.combine(research.getModifier());
        }

        log.debug("Player {} recalculated research modifiers: projectileDamage={}, unitHealth={}, workerCapacity=+{}",
                playerId,
                cumulativeModifier.getProjectileDamageMultiplier(),
                cumulativeModifier.getUnitHealthMultiplier(),
                cumulativeModifier.getWorkerCapacityBonus());
    }

    /**
     * Get the number of active research projects
     */
    public int getActiveResearchCount() {
        return activeResearch.size();
    }
}

