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
     * Minimal roster for baseline comp-stomp AI (workers, infantry, core structures, T2 tech + vehicles).
     */
    public static FactionDefinition baselineOpponent() {
        return FactionDefinition.builder()
                .unitTypes(Set.of(
                        UnitType.WORKER,
                        UnitType.INFANTRY,
                        UnitType.JEEP,
                        UnitType.TANK,
                        UnitType.FLAK_TANK
                ))
                .buildingTypes(Set.of(
                        BuildingType.HEADQUARTERS,
                        BuildingType.POWER_PLANT,
                        BuildingType.REFINERY,
                        BuildingType.BARRACKS,
                        BuildingType.RESEARCH_LAB,
                        BuildingType.FACTORY
                ))
                .build();
    }
}
