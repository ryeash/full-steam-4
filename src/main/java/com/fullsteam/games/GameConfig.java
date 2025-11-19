package com.fullsteam.games;

import com.fullsteam.model.Biome;
import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
@Introspected
public class GameConfig {
    @Min(2)
    @Max(4)
    @Builder.Default
    private int maxPlayers = 10;
    
    @Min(0)
    @Max(4)
    @Builder.Default
    private int teamCount = 2; // 0 = FFA, 1 = invalid, 2-4 = team modes
    
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

    /**
     * Check if this configuration uses teams.
     *
     * @return true if team-based, false if FFA
     */
    public boolean isTeamMode() {
        return teamCount >= 2;
    }

    /**
     * Check if this configuration is Free For All mode.
     *
     * @return true if FFA, false if team-based
     */
    public boolean isFreeForAll() {
        return teamCount == 0;
    }
    
    /**
     * Obstacle density levels
     */
    public enum ObstacleDensity {
        SPARSE(0.5),   // 50% of normal
        MEDIUM(1.0),   // 100% (default)
        DENSE(1.5),    // 150% of normal
        VERY_DENSE(2.0); // 200% of normal
        
        private final double multiplier;
        
        ObstacleDensity(double multiplier) {
            this.multiplier = multiplier;
        }
        
        public double getMultiplier() {
            return multiplier;
        }
    }
}


