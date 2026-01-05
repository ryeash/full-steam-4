package com.fullsteam.model.research;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitCategory;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.Faction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Unified research manager for a player faction.
 * Handles ALL research in a single queue: building upgrades and unit unlocks.
 * Players must choose between researching building upgrades or unlocking new units.
 */
@Slf4j
@Getter
public class ResearchManager {

    private final int playerId;
    private final Faction faction;
    private final UnitTechTree unitTechTree;

    // ========== Completed Research State ==========
    
    // Completed building research
    private final Set<ResearchType> completedResearch = new HashSet<>();
    
    // Completed unit research
    private final Set<String> completedUnitResearch = new HashSet<>();
    
    // Locked unit research (by mutually exclusive choices)
    private final Set<String> lockedUnitResearch = new HashSet<>();

    // ========== Active Research (UNIFIED QUEUE) ==========
    
    // Active research - supports both building and unit research in ONE queue
    // Key: unique identifier (building ID for building research, research ID for unit research)
    private final Map<String, UnifiedResearchProgress> activeResearch = new HashMap<>();

    // ========== Cached State ==========
    
    // Cached combined modifier (recalculated when building research completes)
    private ResearchModifier cumulativeModifier = new ResearchModifier();
    
    // Cached available units (invalidated on unit research completion)
    private Map<UnitCategory, Set<UnitType>> cachedAvailableUnits;

    // ========== Constants ==========
    
    // Maximum simultaneous research projects (base 1, can be upgraded)
    private static final int BASE_MAX_SIMULTANEOUS_RESEARCH = 1;

    public ResearchManager(int playerId, Faction faction) {
        this.playerId = playerId;
        this.faction = faction;
        this.unitTechTree = UnitTechTreeRegistry.getTechTree(faction);
        this.cachedAvailableUnits = computeAvailableUnits();
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
                .filter(UnifiedResearchProgress::isBuildingResearch)
                .anyMatch(progress -> progress.getResearchType() == researchType);
    }

    /**
     * Check if a building is currently researching
     */
    public boolean isBuildingResearching(int buildingId) {
        String key = "building:" + buildingId;
        return activeResearch.containsKey(key);
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

        // Check simultaneous research limit (UNIFIED across building and unit research)
        if (activeResearch.size() >= getMaxSimultaneousResearch()) {
            log.warn("Player {} has reached max simultaneous research limit ({}/{})",
                    playerId, activeResearch.size(), getMaxSimultaneousResearch());
            return false;
        }

        UnifiedResearchProgress progress = new UnifiedResearchProgress(researchType, buildingId);
        String key = "building:" + buildingId;
        activeResearch.put(key, progress);

        log.info("Player {} started building research {} at building {} ({}/{} total active)",
                playerId, researchType, buildingId, activeResearch.size(), getMaxSimultaneousResearch());
        return true;
    }

    /**
     * Cancel active research at a building
     */
    public boolean cancelResearch(int buildingId) {
        String key = "building:" + buildingId;
        UnifiedResearchProgress removed = activeResearch.remove(key);
        if (removed != null) {
            log.info("Player {} cancelled building research {} at building {}",
                    playerId, removed.getResearchType(), buildingId);
            return true;
        }
        return false;
    }

    /**
     * Update all active research progress
     *
     * @param deltaTime Time elapsed in seconds
     * @return List of completed building research types
     */
    public List<ResearchType> updateResearch(double deltaTime) {
        List<ResearchType> completed = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, UnifiedResearchProgress> entry : activeResearch.entrySet()) {
            UnifiedResearchProgress progress = entry.getValue();
            progress.addProgress(deltaTime);
            
            if (progress.isComplete()) {
                // Handle building research completion
                if (progress.isBuildingResearch()) {
                    ResearchType researchType = progress.getResearchType();
                    completeResearch(researchType);
                    completed.add(researchType);
                    toRemove.add(entry.getKey());
                    log.info("Player {} completed building research {}", playerId, researchType);
                }
                // Note: Unit research completion is handled by updateUnitResearch()
                // Don't remove unit research here!
            }
        }

        // Remove completed building research from active list
        toRemove.forEach(activeResearch::remove);

        return completed;
    }

    /**
     * Mark building research as completed and recalculate modifiers
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
    
    // ============================================================
    // UNIT TECH TREE RESEARCH METHODS
    // ============================================================
    
    /**
     * Check if a unit research can be started
     */
    public boolean canStartUnitResearch(String researchId, Set<BuildingType> playerBuildings) {
        if (unitTechTree == null) {
            return false;
        }
        
        Optional<UnitResearchNode> nodeOpt = unitTechTree.getNode(researchId);
        if (nodeOpt.isEmpty()) {
            log.warn("Unit research {} not found in tech tree", researchId);
            return false;
        }

        UnitResearchNode node = nodeOpt.get();

        // Already completed
        if (completedUnitResearch.contains(researchId)) {
            log.debug("Unit research {} already completed for player {}", researchId, playerId);
            return false;
        }

        // Already being researched
        if (isResearchingUnit(researchId)) {
            log.debug("Unit research {} already in progress for player {}", researchId, playerId);
            return false;
        }

        // Locked by mutually exclusive choice
        if (lockedUnitResearch.contains(researchId)) {
            log.debug("Unit research {} locked by previous choice for player {}", researchId, playerId);
            return false;
        }

        // Check prerequisites
        for (String prereqId : node.getPrerequisiteIds()) {
            if (!completedUnitResearch.contains(prereqId)) {
                log.debug("Unit research {} missing prerequisite {} for player {}", 
                         researchId, prereqId, playerId);
                return false;
            }
        }

        // Check tier building requirements
        if (!node.getTier().hasRequiredBuildings(playerBuildings)) {
            log.debug("Unit research {} missing required buildings for tier {} for player {}",
                     researchId, node.getTier(), playerId);
            return false;
        }

        return true;
    }

    /**
     * Start unit research (not tied to a specific building)
     */
    public boolean startUnitResearch(String researchId, int buildingId, Set<BuildingType> playerBuildings) {
        if (!canStartUnitResearch(researchId, playerBuildings)) {
            return false;
        }

        // Check simultaneous research limit (UNIFIED with building research)
        int maxAllowed = getMaxSimultaneousResearch();
        if (activeResearch.size() >= maxAllowed) {
            log.warn("Player {} has reached max simultaneous research limit ({}/{})",
                    playerId, activeResearch.size(), maxAllowed);
            return false;
        }

        UnitResearchNode node = unitTechTree.getNode(researchId).orElseThrow();
        UnifiedResearchProgress progress = new UnifiedResearchProgress(
            researchId,
            buildingId,  // Track for UI purposes, but not enforced
            node.getResearchTimeSeconds()
        );

        String key = "unit:" + researchId;
        activeResearch.put(key, progress);

        log.info("Player {} started unit research {} ({}/{} total active)", 
                playerId, researchId, activeResearch.size(), maxAllowed);
        return true;
    }

    /**
     * Cancel unit research by research ID
     */
    public boolean cancelUnitResearch(String researchId) {
        String key = "unit:" + researchId;
        UnifiedResearchProgress progress = activeResearch.remove(key);
        if (progress != null) {
            log.info("Player {} cancelled unit research {}", playerId, researchId);
            return true;
        }
        return false;
    }
    
    /**
     * Cancel unit research at a building (for backward compatibility)
     */
    public boolean cancelUnitResearchAtBuilding(int buildingId) {
        Optional<String> researchToCancel = activeResearch.entrySet().stream()
                .filter(e -> e.getValue().isUnitResearch())
                .filter(e -> e.getValue().getBuildingId() == buildingId)
                .map(Map.Entry::getKey)
                .findFirst();
        
        if (researchToCancel.isPresent()) {
            activeResearch.remove(researchToCancel.get());
            log.info("Player {} cancelled unit research at building {}", playerId, buildingId);
            return true;
        }
        return false;
    }

    /**
     * Update all active unit research progress
     * @param deltaTime Time elapsed in seconds
     * @return List of completed research IDs
     */
    public List<String> updateUnitResearch(double deltaTime) {
        List<String> completed = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, UnifiedResearchProgress> entry : activeResearch.entrySet()) {
            UnifiedResearchProgress progress = entry.getValue();
            
            // Only process unit research in this method
            if (!progress.isUnitResearch()) {
                continue;
            }
            
            // Progress is already updated in updateResearch(), just check completion
            if (progress.isComplete()) {
                String researchId = progress.getUnitResearchId();
                completeUnitResearch(researchId);
                completed.add(researchId);
                toRemove.add(entry.getKey());
                log.info("Player {} completed unit research {}", playerId, researchId);
            }
        }

        // Remove completed research from active list
        toRemove.forEach(activeResearch::remove);

        return completed;
    }

    /**
     * Mark unit research as completed and update available units
     */
    private void completeUnitResearch(String researchId) {
        completedUnitResearch.add(researchId);
        
        log.info("Player {} - completeUnitResearch called for: {}", playerId, researchId);
        log.info("Player {} - completedUnitResearch set now contains: {}", playerId, completedUnitResearch);

        if (unitTechTree == null) {
            log.warn("Player {} - unitTechTree is null!", playerId);
            return;
        }

        UnitResearchNode node = unitTechTree.getNode(researchId).orElse(null);
        if (node == null) {
            log.warn("Player {} - node not found for researchId: {}", playerId, researchId);
            return;
        }

        // Log unlock
        if (node.getUnitToUnlock() != null) {
            log.info("Player {} unlocked unit: {} (category: {})", 
                    playerId, node.getUnitToUnlock(), node.getUnitToUnlock().getCategory());
        }

        // Log replacement
        if (node.getUnitToReplace() != null) {
            log.info("Player {} replaced unit {} with {} (existing units remain active)",
                    playerId, node.getUnitToReplace(), node.getUnitToUnlock());
        }

        // Lock mutually exclusive research
        lockedUnitResearch.addAll(node.getMutuallyExclusiveIds());
        if (!node.getMutuallyExclusiveIds().isEmpty()) {
            log.info("Player {} locked unit research: {}", playerId, node.getMutuallyExclusiveIds());
        }

        // Invalidate cache
        cachedAvailableUnits = null;
        log.info("Player {} - cachedAvailableUnits invalidated", playerId);
    }

    /**
     * Get available units for a category (cached)
     */
    public Set<UnitType> getAvailableUnits(UnitCategory category) {
        if (cachedAvailableUnits == null) {
            log.info("Player {} - recomputing cachedAvailableUnits for category: {}", playerId, category);
            cachedAvailableUnits = computeAvailableUnits();
            log.info("Player {} - INFANTRY units after compute: {}", playerId, cachedAvailableUnits.get(UnitCategory.INFANTRY));
        }
        Set<UnitType> units = cachedAvailableUnits.getOrDefault(category, Set.of());
        log.info("Player {} - getAvailableUnits({}) returning: {}", playerId, category, units);
        return units;
    }

    /**
     * Get all available units across all categories
     */
    public Map<UnitCategory, Set<UnitType>> getAllAvailableUnits() {
        if (cachedAvailableUnits == null) {
            log.info("Player {} - recomputing all cachedAvailableUnits", playerId);
            cachedAvailableUnits = computeAvailableUnits();
            log.info("Player {} - All available units: {}", playerId, cachedAvailableUnits);
        }
        return new HashMap<>(cachedAvailableUnits);
    }

    /**
     * Compute available units based on completed research
     */
    private Map<UnitCategory, Set<UnitType>> computeAvailableUnits() {
        Map<UnitCategory, Set<UnitType>> available = new HashMap<>();

        if (unitTechTree == null) {
            log.warn("Player {} - unitTechTree is null in computeAvailableUnits", playerId);
            return available;
        }

        log.info("Player {} - computeAvailableUnits called", playerId);
        log.info("Player {} - completedUnitResearch: {}", playerId, completedUnitResearch);

        // Start with starter units
        for (UnitType starter : unitTechTree.getStarterUnits()) {
            available.computeIfAbsent(starter.getCategory(), k -> new HashSet<>())
                     .add(starter);
            log.info("Player {} - Added starter unit: {} (category: {})", 
                    playerId, starter, starter.getCategory());
        }

        // Track replaced units to remove them
        Set<UnitType> replacedUnits = new HashSet<>();

        // Add unlocked units
        for (String completedId : completedUnitResearch) {
            UnitResearchNode node = unitTechTree.getNode(completedId).orElse(null);
            if (node == null) {
                log.warn("Player {} - Could not find node for completed research: {}", playerId, completedId);
                continue;
            }

            log.info("Player {} - Processing completed research: {} -> unlocks: {}, replaces: {}, category: {}", 
                    playerId, completedId, node.getUnitToUnlock(), node.getUnitToReplace(), node.getCategory());

            // Track replaced units
            if (node.getUnitToReplace() != null) {
                replacedUnits.add(node.getUnitToReplace());
            }

            // Add unlocked unit
            if (node.getUnitToUnlock() != null) {
                available.computeIfAbsent(node.getCategory(), k -> new HashSet<>())
                         .add(node.getUnitToUnlock());
                log.info("Player {} - Added unlocked unit: {} to category: {}", 
                        playerId, node.getUnitToUnlock(), node.getCategory());
            }
        }

        // Remove replaced units from production menu
        for (Set<UnitType> units : available.values()) {
            units.removeAll(replacedUnits);
        }

        log.info("Player {} - Final available units: {}", playerId, available);
        return available;
    }

    /**
     * Check if a unit research is currently being researched
     */
    public boolean isResearchingUnit(String researchId) {
        String key = "unit:" + researchId;
        return activeResearch.containsKey(key);
    }

    /**
     * Get the number of active unit research projects
     */
    public int getActiveUnitResearchCount() {
        return (int) activeResearch.values().stream()
                .filter(UnifiedResearchProgress::isUnitResearch)
                .count();
    }
    
    /**
     * Get the number of active building research projects
     */
    public int getActiveBuildingResearchCount() {
        return (int) activeResearch.values().stream()
                .filter(UnifiedResearchProgress::isBuildingResearch)
                .count();
    }
    
    /**
     * Get active unit research map (for DTOs)
     */
    public Map<String, UnifiedResearchProgress> getActiveUnitResearch() {
        Map<String, UnifiedResearchProgress> unitResearch = new HashMap<>();
        for (Map.Entry<String, UnifiedResearchProgress> entry : activeResearch.entrySet()) {
            if (entry.getValue().isUnitResearch()) {
                unitResearch.put(entry.getKey(), entry.getValue());
            }
        }
        return unitResearch;
    }
}
