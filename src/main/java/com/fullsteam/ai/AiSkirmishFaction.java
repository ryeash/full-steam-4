package com.fullsteam.ai;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.FactionDefinition;

import java.util.Set;

/**
 * Default faction definition for skirmish AI opponents so production/harvest checks match human rules.
 */
public final class AiSkirmishFaction {

    private AiSkirmishFaction() {
    }

    /**
     * Full tech-tree roster for skirmish AI opponents:
     * T1 core economy/infantry, T2 vehicles, and T3 heavy hitters unlocked via TECH_CENTER.
     */
    public static FactionDefinition baselineOpponent() {
        return FactionDefinition.builder()
                .unitTypes(Set.of(
                        // T1 — always available
                        UnitType.WORKER,
                        UnitType.INFANTRY,
                        UnitType.JEEP,
                        // T2 — requires RESEARCH_LAB
                        UnitType.TANK,
                        UnitType.FLAK_TANK,
                        // T3 — requires RESEARCH_LAB + TECH_CENTER
                        UnitType.RAIDER,       // fast flanker
                        UnitType.BEAM_TANK,    // durable sustained-fire armor
                        UnitType.GIGANTONAUT,  // heavy siege piece
                        UnitType.SAM_LAUNCHER, // anti-air
                        UnitType.ION_RANGER    // long-range barracks sniper
                ))
                .buildingTypes(Set.of(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.REFINERY,
                        BuildingType.BARRACKS,
                        BuildingType.RESEARCH_LAB,
                        BuildingType.FACTORY,
                        BuildingType.TECH_CENTER
                ))
                .build();
    }
}
