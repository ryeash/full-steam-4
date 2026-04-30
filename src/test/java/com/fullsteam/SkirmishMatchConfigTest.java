package com.fullsteam;

import com.fullsteam.model.AiDifficulty;
import com.fullsteam.model.Biome;
import com.fullsteam.model.GameConfig;
import com.fullsteam.model.ObstacleDensity;
import com.fullsteam.model.SkirmishMatchConfig;
import com.fullsteam.model.SkirmishSlotConfig;
import com.fullsteam.model.SkirmishSlotKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkirmishMatchConfigTest {

    @Test
    void matchmakingFfaFromDefaultHumanSlots() {
        GameConfig in = GameConfig.builder()
                .skirmishSlots(SkirmishMatchConfig.defaultFfaHumanSlots(3))
                .worldWidth(4000)
                .worldHeight(4000)
                .biome(Biome.GRASSLAND)
                .obstacleDensity(ObstacleDensity.MEDIUM)
                .build();
        GameConfig out = SkirmishMatchConfig.resolve(in);
        assertEquals(3, out.getMaxPlayers());
        assertEquals(3, out.getSkirmishSlots().size());
        assertEquals(3, out.getEffectiveMapTeamCount());
        assertEquals(SkirmishSlotKind.HUMAN, out.getSkirmishSlots().get(0).getKind());
        assertEquals(1, out.getSkirmishSlots().get(0).getTeamId());
        assertEquals(3, out.getSkirmishSlots().get(2).getTeamId());
    }

    @Test
    void twoVTwoWithAi() {
        List<SkirmishSlotConfig> slots = List.of(
                slot(SkirmishSlotKind.HUMAN, 1),
                slot(SkirmishSlotKind.HUMAN, 1),
                slot(SkirmishSlotKind.AI, 2),
                slot(SkirmishSlotKind.AI, 2)
        );
        GameConfig in = GameConfig.builder()
                .skirmishSlots(slots)
                .mapTeamCount(2)
                .worldWidth(4000)
                .worldHeight(4000)
                .biome(Biome.GRASSLAND)
                .obstacleDensity(ObstacleDensity.MEDIUM)
                .build();
        GameConfig out = SkirmishMatchConfig.resolve(in);
        assertEquals(2, out.getMaxPlayers());
        assertEquals(4, out.getTotalSlotCount());
        assertEquals(2, out.getEffectiveMapTeamCount());
        assertEquals(AiDifficulty.NORMAL, out.getSkirmishSlots().get(2).getAiDifficulty());
    }

    @Test
    void aiSlotPreservesDifficulty() {
        List<SkirmishSlotConfig> slots = List.of(
                slot(SkirmishSlotKind.HUMAN, 1),
                slot(SkirmishSlotKind.HUMAN, 2),
                SkirmishSlotConfig.builder()
                        .kind(SkirmishSlotKind.AI)
                        .teamId(2)
                        .aiDifficulty(AiDifficulty.HARD)
                        .build()
        );
        GameConfig in = GameConfig.builder()
                .skirmishSlots(slots)
                .mapTeamCount(2)
                .worldWidth(4000)
                .worldHeight(4000)
                .biome(Biome.GRASSLAND)
                .obstacleDensity(ObstacleDensity.MEDIUM)
                .build();
        GameConfig out = SkirmishMatchConfig.resolve(in);
        assertEquals(2, out.getMaxPlayers());
        assertEquals(3, out.getSkirmishSlots().size());
        assertEquals(AiDifficulty.HARD, out.getSkirmishSlots().get(2).getAiDifficulty());
    }

    @Test
    void defaultFfaTwoHumans() {
        GameConfig in = GameConfig.builder()
                .skirmishSlots(SkirmishMatchConfig.defaultFfaHumanSlots(2))
                .build();
        GameConfig out = SkirmishMatchConfig.resolve(in);
        assertEquals(2, out.getMaxPlayers());
        assertEquals(2, out.getSkirmishSlots().size());
        assertEquals(SkirmishSlotKind.HUMAN, out.getSkirmishSlots().get(1).getKind());
    }

    @Test
    void rejectsSingleTeam() {
        List<SkirmishSlotConfig> slots = List.of(
                slot(SkirmishSlotKind.HUMAN, 1),
                slot(SkirmishSlotKind.HUMAN, 1)
        );
        GameConfig in = GameConfig.builder()
                .skirmishSlots(slots)
                .mapTeamCount(2)
                .worldWidth(4000)
                .worldHeight(4000)
                .biome(Biome.GRASSLAND)
                .obstacleDensity(ObstacleDensity.MEDIUM)
                .build();
        assertThrows(IllegalArgumentException.class, () -> SkirmishMatchConfig.resolve(in));
    }

    private static SkirmishSlotConfig slot(SkirmishSlotKind kind, int teamId) {
        return SkirmishSlotConfig.builder().kind(kind).teamId(teamId).build();
    }
}
