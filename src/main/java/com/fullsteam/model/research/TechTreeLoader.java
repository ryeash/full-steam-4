package com.fullsteam.model.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullsteam.model.UnitCategory;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.Faction;
import com.fullsteam.model.research.config.ResearchNodeConfig;
import com.fullsteam.model.research.config.TechTreeConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads unit tech trees from JSON configuration files.
 * Validates the JSON structure and converts it to UnitTechTree objects.
 */
@Slf4j
public class TechTreeLoader {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Load a tech tree from a JSON file in resources/tech-trees/
     */
    public static UnitTechTree load(String filename) {
        String path = "tech-trees/" + filename;
        
        try (InputStream inputStream = TechTreeLoader.class.getClassLoader()
                                                           .getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalStateException("Tech tree file not found: " + path);
            }

            TechTreeConfig config = objectMapper.readValue(inputStream, TechTreeConfig.class);
            return buildTechTree(config);
            
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load tech tree: " + path, e);
        }
    }

    /**
     * Build UnitTechTree from configuration
     */
    private static UnitTechTree buildTechTree(TechTreeConfig config) {
        // Parse faction
        Faction faction;
        try {
            faction = Faction.valueOf(config.getFaction());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid faction: " + config.getFaction(), e);
        }

        // Parse starter units
        Set<UnitType> starterUnits = new HashSet<>();
        for (String unitName : config.getStarterUnits()) {
            try {
                starterUnits.add(UnitType.valueOf(unitName));
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid starter unit: " + unitName, e);
            }
        }

        // Build research nodes
        List<UnitResearchNode> infantryNodes = buildNodes(config.getInfantryNodes());
        List<UnitResearchNode> vehicleNodes = buildNodes(config.getVehicleNodes());
        List<UnitResearchNode> flyerNodes = buildNodes(config.getFlyerNodes());

        // Validate
        validateTechTree(faction, infantryNodes, vehicleNodes, flyerNodes);

        return UnitTechTree.builder()
                .faction(faction)
                .starterUnits(starterUnits)
                .infantryNodes(infantryNodes)
                .vehicleNodes(vehicleNodes)
                .flyerNodes(flyerNodes)
                .build();
    }

    /**
     * Build research nodes from configuration
     */
    private static List<UnitResearchNode> buildNodes(List<ResearchNodeConfig> configs) {
        List<UnitResearchNode> nodes = new ArrayList<>();
        
        for (ResearchNodeConfig config : configs) {
            try {
                UnitResearchNode node = buildNode(config);
                nodes.add(node);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to build node: " + config.getId(), e);
            }
        }
        
        return nodes;
    }

    /**
     * Build a single research node from configuration
     */
    private static UnitResearchNode buildNode(ResearchNodeConfig config) {
        // Parse unit types
        UnitType unitToUnlock = null;
        if (config.getUnitToUnlock() != null && !config.getUnitToUnlock().isEmpty()) {
            unitToUnlock = UnitType.valueOf(config.getUnitToUnlock());
        }

        UnitType unitToReplace = null;
        if (config.getUnitToReplace() != null && !config.getUnitToReplace().isEmpty()) {
            unitToReplace = UnitType.valueOf(config.getUnitToReplace());
        }

        // Parse tier
        ResearchTier tier = ResearchTier.valueOf(config.getTier());

        // Parse category
        UnitCategory category = UnitCategory.valueOf(config.getCategory());

        return UnitResearchNode.builder()
                .id(config.getId())
                .displayName(config.getDisplayName())
                .description(config.getDescription())
                .unitToUnlock(unitToUnlock)
                .unitToReplace(unitToReplace)
                .creditCost(config.getCreditCost())
                .researchTimeSeconds(config.getResearchTimeSeconds())
                .tier(tier)
                .category(category)
                .prerequisiteIds(config.getPrerequisiteIds())
                .mutuallyExclusiveIds(config.getMutuallyExclusiveIds())
                .build();
    }

    /**
     * Validate tech tree structure
     */
    private static void validateTechTree(Faction faction,
                                         List<UnitResearchNode> infantryNodes,
                                         List<UnitResearchNode> vehicleNodes,
                                         List<UnitResearchNode> flyerNodes) {
        // Collect all node IDs
        Set<String> allIds = new HashSet<>();
        List<UnitResearchNode> allNodes = new ArrayList<>();
        allNodes.addAll(infantryNodes);
        allNodes.addAll(vehicleNodes);
        allNodes.addAll(flyerNodes);

        // Check for duplicate IDs
        for (UnitResearchNode node : allNodes) {
            if (!allIds.add(node.getId())) {
                throw new IllegalStateException(
                    faction + " tech tree has duplicate node ID: " + node.getId()
                );
            }
        }

        // Validate each node
        for (UnitResearchNode node : allNodes) {
            validateNode(node, allIds, faction);
        }

        log.info("✅ Validated {} tech tree: {} infantry, {} vehicle, {} flyer nodes",
                faction, infantryNodes.size(), vehicleNodes.size(), flyerNodes.size());
    }

    /**
     * Validate a single research node
     */
    private static void validateNode(UnitResearchNode node, Set<String> allIds, Faction faction) {
        // Check prerequisites reference valid nodes
        for (String prereqId : node.getPrerequisiteIds()) {
            if (!allIds.contains(prereqId)) {
                throw new IllegalStateException(
                    faction + " node " + node.getId() + 
                    " references unknown prerequisite: " + prereqId
                );
            }
        }

        // Check mutual exclusions reference valid nodes
        for (String exclusiveId : node.getMutuallyExclusiveIds()) {
            if (!allIds.contains(exclusiveId)) {
                throw new IllegalStateException(
                    faction + " node " + node.getId() + 
                    " references unknown mutual exclusion: " + exclusiveId
                );
            }
        }

        // Check costs are positive
        if (node.getCreditCost() < 0) {
            throw new IllegalStateException(
                faction + " node " + node.getId() + " has negative cost: " + node.getCreditCost()
            );
        }

        if (node.getResearchTimeSeconds() <= 0) {
            throw new IllegalStateException(
                faction + " node " + node.getId() + 
                " has invalid time: " + node.getResearchTimeSeconds()
            );
        }

        // Check at least one unit is specified
        if (node.getUnitToUnlock() == null && node.getUnitToReplace() == null) {
            log.warn("{} node {} doesn't unlock or replace any unit (might be a prerequisite-only node)",
                    faction, node.getId());
        }
    }
}
