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
            0x4A7C2C,  // Ground color: green
            0x2D5016,  // Obstacle color: dark green (trees)
            ObstacleType.TREE,
            "A lush grassland with scattered trees"
    ),
    
    DESERT(
            "Desert",
            0xC2B280,  // Ground color: sandy tan
            0x8B7355,  // Obstacle color: brown (rocks)
            ObstacleType.ROCK,
            "A harsh desert with rocky outcrops"
    ),
    
    SNOW(
            "Snow",
            0xE8F4F8,  // Ground color: light blue-white
            0x4A5F6A,  // Obstacle color: dark blue-gray (ice/rocks)
            ObstacleType.ICE,
            "A frozen tundra with ice formations"
    ),
    
    VOLCANIC(
            "Volcanic",
            0x3A3A3A,  // Ground color: dark gray
            0x8B0000,  // Obstacle color: dark red (lava rocks)
            ObstacleType.LAVA_ROCK,
            "A volcanic wasteland with lava rocks"
    ),
    
    URBAN(
            "Urban",
            0x505050,  // Ground color: gray (concrete)
            0x696969,  // Obstacle color: darker gray (rubble)
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

