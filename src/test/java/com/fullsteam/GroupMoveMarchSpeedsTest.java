package com.fullsteam;

import com.fullsteam.model.GroupMoveMarchSpeeds;
import com.fullsteam.model.Player;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.FactionDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Group move march speed (cohesion clusters)")
public class GroupMoveMarchSpeedsTest extends BaseTestClass {

    private static Player testPlayer() {
        FactionDefinition factionDef = FactionDefinition.builder()
                .buildingStatModifiers(new HashMap<>())
                .unitStatModifiers(new HashMap<>())
                .build();
        return new Player(1, 1, factionDef);
    }

    @Test
    @DisplayName("Tight cluster: fast and slow ground units share slowest speed cap")
    void clusterSharesMinSpeed() {
        Player p = testPlayer();
        Unit fast = new Unit(1, UnitType.INFANTRY, 0, 0, 1, 1, p);
        Unit slow = new Unit(2, UnitType.TANK, 150, 0, 1, 1, p);

        Map<Unit, Double> caps = GroupMoveMarchSpeeds.computeMarchSpeedCaps(List.of(fast, slow));

        assertEquals(2, caps.size());
        double expected = Math.min(UnitType.INFANTRY.getMovementSpeed(), UnitType.TANK.getMovementSpeed());
        assertEquals(expected, caps.get(fast), 0.001);
        assertEquals(expected, caps.get(slow), 0.001);
    }

    @Test
    @DisplayName("Units farther than link distance are not grouped; no march caps")
    void farApartNoCap() {
        Player p = testPlayer();
        Unit a = new Unit(1, UnitType.INFANTRY, 0, 0, 1, 1, p);
        Unit b = new Unit(2, UnitType.TANK, GroupMoveMarchSpeeds.CLUSTER_LINK_DISTANCE + 50, 0, 1, 1, p);

        Map<Unit, Double> caps = GroupMoveMarchSpeeds.computeMarchSpeedCaps(List.of(a, b));

        assertTrue(caps.isEmpty());
    }

    @Test
    @DisplayName("Single unit never gets a cap")
    void singleUnitNoCap() {
        Player p = testPlayer();
        Unit u = new Unit(1, UnitType.INFANTRY, 0, 0, 1, 1, p);
        assertTrue(GroupMoveMarchSpeeds.computeMarchSpeedCaps(List.of(u)).isEmpty());
    }

    @Test
    @DisplayName("Different elevation bands never link even when close")
    void differentElevationNoLink() {
        Player p = testPlayer();
        Unit ground = new Unit(1, UnitType.INFANTRY, 0, 0, 1, 1, p);
        Unit air = new Unit(2, UnitType.GUNSHIP, 50, 0, 1, 1, p);
        assertNotEquals(ground.getUnitType().getElevation(), air.getUnitType().getElevation());

        Map<Unit, Double> caps = GroupMoveMarchSpeeds.computeMarchSpeedCaps(List.of(ground, air));
        assertTrue(caps.isEmpty());
    }

    @Test
    @DisplayName("Chain of units connects into one cluster")
    void chainFormsOneCluster() {
        Player p = testPlayer();
        Unit a = new Unit(1, UnitType.INFANTRY, 0, 0, 1, 1, p);
        Unit b = new Unit(2, UnitType.INFANTRY, 200, 0, 1, 1, p);
        Unit c = new Unit(3, UnitType.TANK, 400, 0, 1, 1, p);

        Map<Unit, Double> caps = GroupMoveMarchSpeeds.computeMarchSpeedCaps(List.of(a, b, c));
        double expected = UnitType.TANK.getMovementSpeed();

        assertEquals(3, caps.size());
        assertEquals(expected, caps.get(a), 0.001);
        assertEquals(expected, caps.get(b), 0.001);
        assertEquals(expected, caps.get(c), 0.001);
    }
}
