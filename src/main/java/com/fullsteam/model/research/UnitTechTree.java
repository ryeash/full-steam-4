package com.fullsteam.model.research;

import com.fullsteam.model.UnitCategory;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.Faction;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Complete unit tech tree for a faction.
 * Contains all research nodes organized by unit category.
 */
@Getter
@Builder
public class UnitTechTree {
    private final Faction faction;

    @Builder.Default
    private final List<UnitResearchNode> infantryNodes = List.of();

    @Builder.Default
    private final List<UnitResearchNode> vehicleNodes = List.of();

    @Builder.Default
    private final List<UnitResearchNode> flyerNodes = List.of();

    @Builder.Default
    private final Set<UnitType> starterUnits = Set.of();

    /**
     * Get a research node by ID
     */
    public Optional<UnitResearchNode> getNode(String id) {
        return getAllNodes().stream()
                .filter(node -> node.getId().equals(id))
                .findFirst();
    }

    /**
     * Get all nodes for a specific category
     */
    public List<UnitResearchNode> getNodesByCategory(UnitCategory category) {
        return switch (category) {
            case INFANTRY -> infantryNodes;
            case VEHICLE -> vehicleNodes;
            case FLYER -> flyerNodes;
            case WORKER -> List.of(); // Workers have no research
        };
    }

    /**
     * Get all research nodes across all categories
     */
    public List<UnitResearchNode> getAllNodes() {
        List<UnitResearchNode> all = new java.util.ArrayList<>();
        all.addAll(infantryNodes);
        all.addAll(vehicleNodes);
        all.addAll(flyerNodes);
        return all;
    }

    /**
     * Get total number of research nodes in this tech tree
     */
    public int getTotalNodeCount() {
        return infantryNodes.size() + vehicleNodes.size() + flyerNodes.size();
    }
}
