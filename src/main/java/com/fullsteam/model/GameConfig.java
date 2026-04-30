package com.fullsteam.model;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Configuration for an RTS game. Human capacity is derived from {@link #skirmishSlots} (HUMAN entries).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Introspected
public class GameConfig {

    /**
     * Symmetry / corner layout count passed to {@link RTSWorld} (2, 3, or 4). When null, inferred from skirmish slots.
     */
    @Min(2)
    @Max(4)
    private Integer mapTeamCount;

    /**
     * Full skirmish roster (humans + AI). After lobby resolution this is non-null with 2..4 entries.
     */
    private List<SkirmishSlotConfig> skirmishSlots;

    @Builder.Default
    @DecimalMin("3000.0")
    @DecimalMax("10000.0")
    private double worldWidth = 4000.0;

    @Builder.Default
    @DecimalMin("3000.0")
    @DecimalMax("10000.0")
    private double worldHeight = 4000.0;

    @Builder.Default
    private Biome biome = Biome.GRASSLAND;

    @Builder.Default
    private ObstacleDensity obstacleDensity = ObstacleDensity.MEDIUM;

    @Builder.Default
    private boolean fogOfWarEnabled = false;

    @Builder.Default
    @Min(100)
    @Max(1000)
    private int startingResources = 500;

    // Faction selection for debug games (optional, defaults to TERRAN)
    private String faction;

    /**
     * World symmetry team count used by {@link RTSWorld}. Resolved configs set {@link #mapTeamCount} explicitly.
     */
    public int getEffectiveMapTeamCount() {
        if (mapTeamCount != null) {
            return mapTeamCount;
        }
        if (skirmishSlots != null && !skirmishSlots.isEmpty()) {
            return skirmishSlots.stream().mapToInt(SkirmishSlotConfig::getTeamId).max().orElse(2);
        }
        return 2;
    }

    /**
     * Human lobby seats / join capacity — count of {@link SkirmishSlotKind#HUMAN} entries in {@link #skirmishSlots}.
     */
    public int getMaxPlayers() {
        return countHumanSkirmishSlots();
    }

    public List<SkirmishSlotConfig> getSkirmishSlotsOrEmpty() {
        return skirmishSlots != null ? skirmishSlots : Collections.emptyList();
    }

    public int getTotalSlotCount() {
        return skirmishSlots != null ? skirmishSlots.size() : 0;
    }

    public int countHumanSkirmishSlots() {
        if (skirmishSlots == null) {
            return 0;
        }
        return (int) skirmishSlots.stream().filter(s -> s.getKind() == SkirmishSlotKind.HUMAN).count();
    }
}

