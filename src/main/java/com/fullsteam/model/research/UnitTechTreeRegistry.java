package com.fullsteam.model.research;

import com.fullsteam.model.factions.Faction;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for all faction unit tech trees.
 * Loads tech trees from JSON files on initialization.
 */
@Slf4j
public class UnitTechTreeRegistry {

    private static final Map<Faction, UnitTechTree> TECH_TREES = new HashMap<>();

    static {
        // Load all tech trees from JSON files
        TECH_TREES.put(Faction.TERRAN, TechTreeLoader.load("terran.json"));
        TECH_TREES.put(Faction.NOMADS, TechTreeLoader.load("nomads.json"));
        TECH_TREES.put(Faction.SYNTHESIS, TechTreeLoader.load("synthesis.json"));
        TECH_TREES.put(Faction.TECH_ALLIANCE, TechTreeLoader.load("tech-alliance.json"));
        TECH_TREES.put(Faction.STORM_WINGS, TechTreeLoader.load("storm-wings.json"));

        log.info("✅ Loaded {} faction tech trees", TECH_TREES.size());
    }

    /**
     * Get the tech tree for a faction
     */
    public static UnitTechTree getTechTree(Faction faction) {
        UnitTechTree tree = TECH_TREES.get(faction);
        if (tree == null) {
            throw new IllegalArgumentException("No tech tree found for faction: " + faction);
        }
        return tree;
    }

    /**
     * Check if a tech tree is loaded for a faction
     */
    public static boolean hasTechTree(Faction faction) {
        return TECH_TREES.containsKey(faction);
    }

    /**
     * Get all loaded factions
     */
    public static Map<Faction, UnitTechTree> getAllTechTrees() {
        return new HashMap<>(TECH_TREES);
    }
}
