package com.fullsteam.dto;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.research.ResearchManager;
import com.fullsteam.model.research.UnifiedResearchProgress;
import com.fullsteam.model.research.UnitTechTree;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data Transfer Object for a player's complete unit tech tree.
 * Includes all research nodes with current player state.
 */
@Data
@Builder
public class UnitTechTreeDTO {
    
    private String faction;                         // e.g., "TERRAN"
    private List<String> starterUnits;              // e.g., ["WORKER", "INFANTRY", "JEEP"]
    private List<UnitResearchNodeDTO> infantryNodes;
    private List<UnitResearchNodeDTO> vehicleNodes;
    private List<UnitResearchNodeDTO> flyerNodes;
    
    // Player state
    private Set<String> completedResearch;          // IDs of completed research
    private Set<String> lockedResearch;             // IDs locked by branching choices
    private List<UnitResearchProgressDTO> activeResearch; // Currently researching
    
    /**
     * Build DTO from tech tree and player's research manager
     */
    public static UnitTechTreeDTO fromTechTree(
            UnitTechTree techTree, 
            ResearchManager researchManager,
            Set<BuildingType> playerBuildings) {
        
        // Convert starter units to names
        List<String> starterUnitNames = techTree.getStarterUnits().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        
        // Convert infantry nodes with player state
        List<UnitResearchNodeDTO> infantryDTOs = techTree.getInfantryNodes().stream()
                .map(node -> {
                    UnitResearchNodeDTO dto = UnitResearchNodeDTO.fromNode(node);
                    addPlayerStateToNode(dto, node.getId(), researchManager, playerBuildings, techTree);
                    return dto;
                })
                .collect(Collectors.toList());
        
        // Convert vehicle nodes with player state
        List<UnitResearchNodeDTO> vehicleDTOs = techTree.getVehicleNodes().stream()
                .map(node -> {
                    UnitResearchNodeDTO dto = UnitResearchNodeDTO.fromNode(node);
                    addPlayerStateToNode(dto, node.getId(), researchManager, playerBuildings, techTree);
                    return dto;
                })
                .collect(Collectors.toList());
        
        // Convert flyer nodes with player state
        List<UnitResearchNodeDTO> flyerDTOs = techTree.getFlyerNodes().stream()
                .map(node -> {
                    UnitResearchNodeDTO dto = UnitResearchNodeDTO.fromNode(node);
                    addPlayerStateToNode(dto, node.getId(), researchManager, playerBuildings, techTree);
                    return dto;
                })
                .collect(Collectors.toList());
        
        // Convert active research to DTOs
        List<UnitResearchProgressDTO> activeResearchDTOs = new ArrayList<>();
        for (UnifiedResearchProgress progress : researchManager.getActiveUnitResearch().values()) {
            if (progress.isUnitResearch()) {
                var nodeOpt = techTree.getNode(progress.getUnitResearchId());
                if (nodeOpt.isPresent()) {
                    String displayName = nodeOpt.get().getDisplayName();
                    // TODO: Check if building is powered (for isPaused flag)
                    UnitResearchProgressDTO progressDTO = UnitResearchProgressDTO.fromUnifiedProgress(
                        progress, displayName, false
                    );
                    activeResearchDTOs.add(progressDTO);
                }
            }
        }
        
        return UnitTechTreeDTO.builder()
                .faction(techTree.getFaction().name())
                .starterUnits(starterUnitNames)
                .infantryNodes(infantryDTOs)
                .vehicleNodes(vehicleDTOs)
                .flyerNodes(flyerDTOs)
                .completedResearch(researchManager.getCompletedUnitResearch())
                .lockedResearch(researchManager.getLockedUnitResearch())
                .activeResearch(activeResearchDTOs)
                .build();
    }
    
    /**
     * Determine node state and add to DTO
     */
    private static void addPlayerStateToNode(
            UnitResearchNodeDTO dto,
            String researchId,
            ResearchManager researchManager,
            Set<BuildingType> playerBuildings,
            UnitTechTree techTree) {
        
        // Check if completed
        if (researchManager.getCompletedUnitResearch().contains(researchId)) {
            dto.withPlayerState("COMPLETED", null, null, null);
            return;
        }
        
        // Check if currently researching
        for (UnifiedResearchProgress progress : researchManager.getActiveUnitResearch().values()) {
            if (progress.isUnitResearch() && progress.getUnitResearchId().equals(researchId)) {
                // Convert from 0.0-1.0 to 0-100 for frontend display
                double percent = progress.getProgressPercent() * 100.0;
                int remaining = (int) Math.ceil(progress.getRemainingSeconds());
                dto.withPlayerState("RESEARCHING", percent, remaining, null);
                return;
            }
        }
        
        // Check if locked by branching choice
        if (researchManager.getLockedUnitResearch().contains(researchId)) {
            List<String> reasons = List.of("Locked by previous research choice");
            dto.withPlayerState("LOCKED", null, null, reasons);
            return;
        }
        
        // Check if available (prerequisites met, not locked)
        if (researchManager.canStartUnitResearch(researchId, playerBuildings)) {
            dto.withPlayerState("AVAILABLE", null, null, null);
            return;
        }
        
        // Otherwise locked - determine why
        List<String> lockReasons = new ArrayList<>();
        var nodeOpt = techTree.getNode(researchId);
        if (nodeOpt.isPresent()) {
            var node = nodeOpt.get();
            
            // Check prerequisites
            for (String prereqId : node.getPrerequisiteIds()) {
                if (!researchManager.getCompletedUnitResearch().contains(prereqId)) {
                    var prereqNode = techTree.getNode(prereqId);
                    String prereqName = prereqNode.map(n -> n.getDisplayName()).orElse(prereqId);
                    lockReasons.add("Requires: " + prereqName);
                }
            }
            
            // Check tier buildings
            switch (node.getTier()) {
                case ADVANCED:
                    if (!playerBuildings.contains(BuildingType.RESEARCH_LAB)) {
                        lockReasons.add("Requires: Research Lab");
                    }
                    break;
                case ELITE:
                    if (!playerBuildings.contains(BuildingType.RESEARCH_LAB)) {
                        lockReasons.add("Requires: Research Lab");
                    }
                    if (!playerBuildings.contains(BuildingType.TECH_CENTER)) {
                        lockReasons.add("Requires: Tech Center");
                    }
                    break;
                case BASIC:
                default:
                    break;
            }
        }
        
        dto.withPlayerState("LOCKED", null, null, lockReasons);
    }
}
