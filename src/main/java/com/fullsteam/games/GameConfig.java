package com.fullsteam.games;

import com.fullsteam.model.Biome;
import com.fullsteam.model.ObstacleDensity;
import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@Introspected
public class GameConfig {
    @Min(2)  // Minimum 2 players (including AI)
    @Max(4)
    @Builder.Default
    private int maxPlayers = 4;

    @Min(0)
    @Max(4)
    @Builder.Default
    private int teamCount = 0; // 0 = FFA (default), 2-4 = team modes

    @DecimalMin("800.0")
    @DecimalMax("10000.0")
    @Builder.Default
    private double worldWidth = 2000.0;

    @DecimalMin("800.0")
    @DecimalMax("10000.0")
    @Builder.Default
    private double worldHeight = 2000.0;

    @DecimalMin("10.0")
    @DecimalMax("1000.0")
    @Builder.Default
    private double playerMaxHealth = 100.0;

    @Min(1000)
    @Max(60000)
    @Builder.Default
    private long aiCheckIntervalMs = 10000;

    @NotNull
    @Builder.Default
    private boolean enableAIFilling = true;

    // RTS-specific settings
    @NotNull
    @Builder.Default
    private Biome biome = Biome.GRASSLAND;

    @NotNull
    @Builder.Default
    private ObstacleDensity obstacleDensity = ObstacleDensity.MEDIUM;

    // Faction selection (optional, for tracking in matchmaking)
    private String faction;  // Can be null, defaults to TERRAN if not specified
}


