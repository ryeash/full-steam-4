package com.fullsteam;

import com.fullsteam.model.FogOfWar;
import com.fullsteam.model.GameConfig;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.Player;
import com.fullsteam.model.SatelliteReveal;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.FactionDefinition;
import org.dyn4j.geometry.Vector2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Satellite sweep and carpet bomb support")
class SatelliteSweepAndCarpetBombTest extends BaseTestClass {

    private static FactionDefinition emptyFaction() {
        return FactionDefinition.builder()
                .buildingStatModifiers(new HashMap<>())
                .unitStatModifiers(new HashMap<>())
                .build();
    }

    @Test
    void pruneExpiredSatelliteRevealsRemovesStale() {
        GameEntities ge = new GameEntities(GameConfig.builder().build(), null);
        ge.addSatelliteReveal(new SatelliteReveal(1, new Vector2(0, 0), 100, System.currentTimeMillis() - 1));
        assertEquals(1, ge.getSatelliteReveals().size());
        ge.pruneExpiredSatelliteReveals();
        assertTrue(ge.getSatelliteReveals().isEmpty());
    }

    @Test
    void satelliteRevealGrantsVisionOfDistantEnemy() {
        GameConfig cfg = GameConfig.builder().worldWidth(4000).worldHeight(4000).build();
        GameEntities ge = new GameEntities(cfg, null);
        Player p1 = new Player(1, 1, emptyFaction());
        Player p2 = new Player(2, 2, emptyFaction());

        Unit ally = new Unit(1, UnitType.INFANTRY, -2500, 0, 1, 1, p1);
        Unit enemy = new Unit(2, UnitType.INFANTRY, 0, 0, 2, 2, p2);
        ge.getUnits().put(1, ally);
        ge.getUnits().put(2, enemy);

        List<Unit> before = FogOfWar.getVisibleUnits(ge, 1);
        assertFalse(before.stream().anyMatch(u -> u.getTeamNumber() == 2));

        ge.addSatelliteReveal(new SatelliteReveal(1, new Vector2(0, 0), 800, System.currentTimeMillis() + 60_000));
        List<Unit> after = FogOfWar.getVisibleUnits(ge, 1);
        assertTrue(after.stream().anyMatch(u -> u.getId() == 2));
    }

    @Test
    void newCommandBuildingsAreTier3WithStandardTechChain() {
        assertEquals(3, BuildingType.SATCOM_ARRAY.getRequiredTechTier());
        assertEquals(3, BuildingType.CARPET_PAD.getRequiredTechTier());
        assertEquals(
                List.of(BuildingType.POWER_PLANT, BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
                BuildingType.SATCOM_ARRAY.getTechRequirements());
        assertEquals(
                List.of(BuildingType.POWER_PLANT, BuildingType.RESEARCH_LAB, BuildingType.TECH_CENTER),
                BuildingType.CARPET_PAD.getTechRequirements());
    }
}
