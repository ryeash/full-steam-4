package com.fullsteam.model;

import lombok.Getter;

/**
 * Defines different biome types for RTS maps.
 * Each biome has unique visual characteristics and obstacle types.
 */
@Getter
public enum Biome {
    GRASSLAND(
            "Grassland",
            0x3A5A24,  // Ground color: muted olive green (was 0x4A7C2C)
            0x2A4418,  // Obstacle color: dark muted green (trees)
            ObstacleType.TREE,
            "A lush grassland with scattered trees"
    ),

    DESERT(
            "Desert",
            0xA89968,  // Ground color: muted sandy tan (was 0xC2B280)
            0x7A6348,  // Obstacle color: muted brown (rocks)
            ObstacleType.ROCK,
            "A harsh desert with rocky outcrops"
    ),

    SNOW(
            "Snow",
            0xC8D4D8,  // Ground color: muted blue-white (was 0xE8F4F8)
            0x3A4F5A,  // Obstacle color: muted blue-gray (ice/rocks)
            ObstacleType.ICE,
            "A frozen tundra with ice formations"
    ),

    VOLCANIC(
            "Volcanic",
            0x2A2A2A,  // Ground color: darker gray (was 0x3A3A3A)
            0x6B0000,  // Obstacle color: muted dark red (lava rocks)
            ObstacleType.LAVA_ROCK,
            "A volcanic wasteland with lava rocks"
    ),

    URBAN(
            "Urban",
            0x404040,  // Ground color: muted gray (concrete)
            0x595959,  // Obstacle color: muted darker gray (rubble)
            ObstacleType.RUBBLE,
            "A ruined city with debris and rubble"
    );

    private final String displayName;
    private final int groundColor;
    private final int obstacleColor;
    private final ObstacleType obstacleType;
    private final String description;

    Biome(String displayName, int groundColor, int obstacleColor,
          ObstacleType obstacleType, String description) {
        this.displayName = displayName;
        this.groundColor = groundColor;
        this.obstacleColor = obstacleColor;
        this.obstacleType = obstacleType;
        this.description = description;
    }

    /**
     * Obstacle types for different biomes
     */
    public enum ObstacleType {
        TREE,        // Grassland
        ROCK,        // Desert
        ICE,         // Snow
        LAVA_ROCK,   // Volcanic
        RUBBLE       // Urban
    }
}

