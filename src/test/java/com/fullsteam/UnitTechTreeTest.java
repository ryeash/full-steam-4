package com.fullsteam;

import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.Faction;
import com.fullsteam.model.research.UnitTechTree;
import com.fullsteam.model.research.UnitTechTreeRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnitTechTreeTest {

    @Test
    void testTerranTechTreeLoads() {
        UnitTechTree tree = UnitTechTreeRegistry.getTechTree(Faction.TERRAN);
        
        assertNotNull(tree);
        assertEquals(Faction.TERRAN, tree.getFaction());
        
        // Check starter units
        assertTrue(tree.getStarterUnits().contains(UnitType.WORKER));
        assertTrue(tree.getStarterUnits().contains(UnitType.INFANTRY));
        assertTrue(tree.getStarterUnits().contains(UnitType.JEEP));
        assertTrue(tree.getStarterUnits().contains(UnitType.SCOUT_DRONE));
        
        // Check research nodes loaded
        assertEquals(3, tree.getInfantryNodes().size());
        assertEquals(3, tree.getVehicleNodes().size());
        assertEquals(1, tree.getFlyerNodes().size());
    }

    @Test
    void testAllFactionTechTreesLoad() {
        for (Faction faction : Faction.values()) {
            UnitTechTree tree = UnitTechTreeRegistry.getTechTree(faction);
            assertNotNull(tree, "Tech tree should be loaded for " + faction);
            assertEquals(faction, tree.getFaction());
        }
    }

    @Test
    void testTerranMedicResearchNode() {
        UnitTechTree tree = UnitTechTreeRegistry.getTechTree(Faction.TERRAN);
        
        var medicNode = tree.getNode("TERRAN_INF_MEDIC");
        assertTrue(medicNode.isPresent());
        
        var node = medicNode.get();
        assertEquals("Medic Training", node.getDisplayName());
        assertEquals(UnitType.MEDIC, node.getUnitToUnlock());
        assertNull(node.getUnitToReplace());
        assertEquals(250, node.getCreditCost());
        assertEquals(35, node.getResearchTimeSeconds());
    }
}
