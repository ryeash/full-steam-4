package com.fullsteam;

import com.fullsteam.model.BuildingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Satellite sweep and carpet bomb support")
class SatelliteSweepAndCarpetBombTest extends BaseTestClass {

    @Test
    @Disabled("Satellite reveal state moved off GameEntities; exercise RTSGameManager / Player paths instead")
    void pruneExpiredSatelliteRevealsRemovesStale() {
        // GameEntities no longer hosts satellite reveal lists.
    }

    @Test
    @Disabled("Satellite reveal state moved off GameEntities; exercise RTSGameManager / Player paths instead")
    void satelliteRevealGrantsVisionOfDistantEnemy() {
        // GameEntities no longer hosts satellite reveal lists.
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
