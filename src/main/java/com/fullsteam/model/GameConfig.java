package com.fullsteam.model;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for an RTS game.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Introspected
public class GameConfig {

    @Builder.Default
    @Min(2)
    @Max(4)
    private int maxPlayers = 2;

    @Builder.Default
    private double worldWidth = 4000.0;

    @Builder.Default
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
}

