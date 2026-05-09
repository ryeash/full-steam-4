package com.fullsteam.model;

import com.fullsteam.games.IdGenerator;
import lombok.Getter;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents the RTS game world with bounded rectangle, start points, and symmetric resource/obstacle placement.
 * Uses 90-degree rotational symmetry for all games with 3 or 4 spawn slots, and 180-degree diagonal
 * symmetry for 2-slot games.
 */
@Getter
public class RTSWorld {
    private final double width;
    private final double height;
    /**
     * Number of player spawn slots (not number of teams). Drives symmetry order and
     * how many corners are cleared of obstacles.
     */
    private final int slotCount;
    private final Biome biome;
    private final double obstacleDensityMultiplier;

    /**
     * One spawn corner per skirmish slot (up to 4), in assignment order:
     * 0 = Bottom-left, 1 = Top-right, 2 = Bottom-right, 3 = Top-left
     * <p>
     * This ordering ensures a 1-vs-1 game always gets the diagonal pair (0 & 1),
     * and a full 4-player game fills all four corners.
     */
    private final List<Vector2> slotCorners;

    /**
     * Spawn positions that must be kept clear of obstacles — one entry per active slot,
     * derived directly from {@link #slotCorners} so positions are guaranteed to match.
     */
    private final List<Vector2> teamStartPoints;

    // Obstacles (90-degree symmetric placement)
    // Some obstacles are harvestable and contain resources
    private final List<ObstacleSpawn> obstacleSpawns;

    private final List<Obstacle> obstacles = new LinkedList<>();

    // World bounds
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;

    public RTSWorld(double width, double height, int slotCount, Biome biome,
                    double obstacleDensityMultiplier, long seed) {
        this.width = width;
        this.height = height;
        this.slotCount = Math.max(2, Math.min(slotCount, 4));
        this.biome = biome;
        this.obstacleDensityMultiplier = obstacleDensityMultiplier;

        // Calculate bounds
        this.minX = -width / 2.0;
        this.maxX = width / 2.0;
        this.minY = -height / 2.0;
        this.maxY = height / 2.0;

        // slotCorners must be generated first — teamStartPoints derives from it
        this.slotCorners = generateSlotCorners();
        this.teamStartPoints = generateTeamStartPoints();
        this.obstacleSpawns = generateObstacleSpawns();
    }

    /**
     * Build the four corner spawn positions used for per-slot assignment.
     * Order: BL, TR, BR, TL — chosen so that the first two slots are always the
     * diagonal pair, giving a clean 1-vs-1 layout and filling all corners for 4 players.
     */
    private List<Vector2> generateSlotCorners() {
        double margin = Math.min(width, height) * 0.15;
        return List.of(
                new Vector2(minX + margin, minY + margin), // 0: Bottom-left
                new Vector2(maxX - margin, maxY - margin), // 1: Top-right   (diagonal from 0)
                new Vector2(maxX - margin, minY + margin), // 2: Bottom-right
                new Vector2(minX + margin, maxY - margin)  // 3: Top-left
        );
    }

    /**
     * Collect the start points that need obstacle exclusion zones — one per active slot.
     * Positions are taken directly from {@link #slotCorners} to guarantee consistency.
     */
    private List<Vector2> generateTeamStartPoints() {
        List<Vector2> startPoints = new ArrayList<>();
        for (int i = 0; i < slotCount && i < slotCorners.size(); i++) {
            startPoints.add(slotCorners.get(i).copy());
        }
        return startPoints;
    }

    /**
     * Returns the spawn corner for the given skirmish slot index (0–3).
     */
    public Vector2 getSlotCorner(int slotIndex) {
        return slotCorners.get(Math.max(0, Math.min(slotIndex, slotCorners.size() - 1))).copy();
    }

    /**
     * Generate obstacle spawns with appropriate symmetry.
     * 2 slots → 180° diagonal mirror; 3-4 slots → 90° rotational (4-way) symmetry.
     * 4-way symmetry is used for 3-slot games too so the map looks balanced and all
     * corner areas (including the unused 4th corner) are treated consistently.
     */
    private List<ObstacleSpawn> generateObstacleSpawns() {
        List<ObstacleSpawn> spawns = new ArrayList<>();

        List<ObstacleSpawn> basePattern = generateBaseObstaclePattern();

        if (slotCount == 2) {
            // 2-slot games: diagonal 180° symmetry
            spawns.addAll(basePattern);
            spawns.addAll(mirror180Obstacles(basePattern));
        } else {
            // 3- or 4-slot games: full 90° rotational symmetry (4-way)
            spawns.addAll(basePattern);
            spawns.addAll(rotate90Obstacles(basePattern));
            spawns.addAll(rotate180Obstacles(basePattern));
            spawns.addAll(rotate270Obstacles(basePattern));
        }

        return spawns;
    }

    /**
     * Generate base obstacle pattern.
     */
    private List<ObstacleSpawn> generateBaseObstaclePattern() {
        List<ObstacleSpawn> pattern = new ArrayList<>();

        // Use most of the map (90% of width/height) instead of restricted area
        double workWidth = width * 0.9;
        double workHeight = height * 0.9;

        // Calculate obstacle count based on world area and density multiplier
        // Base formula: 1 obstacle per 100,000 square units (scaled by density)
        double workArea = workWidth * workHeight;
        double baseObstacleCount = workArea / 300000.0; // 1 obstacle per 100k area

        // Apply density multiplier and add some randomness (±25%)
        double scaledCount = baseObstacleCount * obstacleDensityMultiplier;
        int minObstacles = (int) Math.ceil(scaledCount * 0.75);
        int maxObstacles = (int) Math.ceil(scaledCount * 1.25);
        int obstaclesInSection = minObstacles + ThreadLocalRandom.current().nextInt(Math.max(1, maxObstacles - minObstacles + 1));
        obstaclesInSection = Math.max(2, obstaclesInSection); // At least 2 obstacles

        // Exclusion radius around starting positions — obstacle CENTRE must stay this far from the
        // spawn corner. 500 gives comfortable room for the HQ footprint plus several nearby buildings.
        double startExclusionRadius = 500.0;

        // Generate obstacles with multiple attempts to avoid starting areas
        int attempts = 0;
        int maxAttempts = obstaclesInSection * 3; // Allow 3x attempts to find valid positions

        while (pattern.size() < obstaclesInSection && attempts < maxAttempts) {
            attempts++;

            // Generate random position within working area
            double margin = 150;
            double x, y;

            if (slotCount >= 3) {
                // 3-4 slots: generate base pattern in Q1 (positive x, positive y).
                // The three 90° rotations cover the other three quadrants symmetrically.
                x = margin + ThreadLocalRandom.current().nextDouble() * (workWidth / 2.0 - 2 * margin);
                y = margin + ThreadLocalRandom.current().nextDouble() * (workHeight / 2.0 - 2 * margin);
            } else {
                // 2 slots: generate across full width in the bottom half;
                // mirror180 covers the top half.
                x = -workWidth / 2.0 + margin + ThreadLocalRandom.current().nextDouble() * (workWidth - 2 * margin);
                y = -workHeight / 2.0 + margin + ThreadLocalRandom.current().nextDouble() * (workHeight / 2.0 - 2 * margin);
            }

            Vector2 obstaclePos = new Vector2(x, y);

            // Check if this position is too close to any team start point
            boolean tooCloseToStart = false;
            for (Vector2 startPoint : teamStartPoints) {
                double distance = obstaclePos.distance(startPoint);
                if (distance < startExclusionRadius) {
                    tooCloseToStart = true;
                    break;
                }
            }

            // If position is valid, generate the obstacle
            if (!tooCloseToStart) {
                // Vary obstacle size based on biome
                double baseSize = getBiomeObstacleSize();
                double sizeVariation = baseSize * 0.5; // ±50% variation
                double size = baseSize + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * sizeVariation;

                // Determine obstacle properties (harvestable, destructible, resources)
                // 50% chance of being harvestable (contains resources)
                // 30% chance of being destructible (can be destroyed but no resources)
                // 20% chance of being indestructible
                double roll = ThreadLocalRandom.current().nextDouble();
                boolean harvestable = roll < 0.5;
                boolean destructible = !harvestable && roll < 0.8; // 30% of remaining 50%

                // Harvestable obstacles contain resources
                int resources = 0;
                if (harvestable) {
                    resources = 5000 + ThreadLocalRandom.current().nextInt(5000); // 5000-10000 resources per obstacle
                }

                // Generate obstacle with biome-specific shape
                pattern.add(generateBiomeObstacle(obstaclePos, size, harvestable, destructible, resources, ThreadLocalRandom.current()));
            }
        }

        return pattern;
    }

    /**
     * Get obstacle size based on biome type
     */
    private double getBiomeObstacleSize() {
        return switch (biome) {
            case GRASSLAND -> 40.0;  // Medium trees
            case DESERT -> 50.0;     // Large rocks
            case SNOW -> 45.0;       // Ice formations
            case VOLCANIC -> 55.0;   // Large lava rocks
            case URBAN -> 60.0;      // Large rubble piles
        };
    }

    /**
     * Generate an obstacle with biome-specific shape characteristics
     */
    private ObstacleSpawn generateBiomeObstacle(Vector2 position, double size, boolean harvestable,
                                                boolean destructible, int resources, Random random) {
        return switch (biome) {
            case GRASSLAND -> {
                // Trees: Mix of circles (round trees) and irregular organic shapes
                if (random.nextDouble() < 0.3) {
                    yield new ObstacleSpawn(position, size, harvestable, destructible, resources); // Circle (round tree)
                } else {
                    // Bushy, organic tree shapes
                    int sides = 6 + random.nextInt(3); // 6-8 sides
                    double irregularity = 0.3 + random.nextDouble() * 0.3; // 0.3-0.6
                    double spikiness = 0.2 + random.nextDouble() * 0.3;    // 0.2-0.5
                    Vector2[] vertices = generateIrregularPolygon(size, sides, irregularity, spikiness, random);
                    yield new ObstacleSpawn(position, vertices, harvestable, destructible, resources);
                }
            }
            case DESERT -> {
                // Rocks: Very irregular, weathered shapes
                int sides = 5 + random.nextInt(4); // 5-8 sides
                double irregularity = 0.4 + random.nextDouble() * 0.4; // 0.4-0.8 (very irregular)
                double spikiness = 0.3 + random.nextDouble() * 0.4;    // 0.3-0.7 (quite spiky)
                Vector2[] vertices = generateIrregularPolygon(size, sides, irregularity, spikiness, random);
                yield new ObstacleSpawn(position, vertices, harvestable, destructible, resources);
            }
            case SNOW -> {
                // Ice: Mix of regular crystals and irregular chunks
                if (random.nextDouble() < 0.4) {
                    yield new ObstacleSpawn(position, size, 6, harvestable, destructible, resources); // Regular hexagon (ice crystal)
                } else {
                    // Irregular ice chunks
                    int sides = 5 + random.nextInt(3); // 5-7 sides
                    double irregularity = 0.2 + random.nextDouble() * 0.3; // 0.2-0.5
                    double spikiness = 0.3 + random.nextDouble() * 0.4;    // 0.3-0.7 (sharp edges)
                    Vector2[] vertices = generateIrregularPolygon(size, sides, irregularity, spikiness, random);
                    yield new ObstacleSpawn(position, vertices, harvestable, destructible, resources);
                }
            }
            case VOLCANIC -> {
                // Lava rocks: Sharp, angular, highly irregular shapes
                int sides = 4 + random.nextInt(4); // 4-7 sides
                double irregularity = 0.5 + random.nextDouble() * 0.4; // 0.5-0.9 (very angular)
                double spikiness = 0.4 + random.nextDouble() * 0.5;    // 0.4-0.9 (very spiky)
                Vector2[] vertices = generateIrregularPolygon(size, sides, irregularity, spikiness, random);
                yield new ObstacleSpawn(position, vertices, harvestable, destructible, resources);
            }
            case URBAN -> {
                // Rubble: Extremely irregular, chaotic shapes
                int sides = 4 + random.nextInt(6); // 4-9 sides
                double irregularity = 0.6 + random.nextDouble() * 0.3; // 0.6-0.9 (chaotic)
                double spikiness = 0.5 + random.nextDouble() * 0.4;    // 0.5-0.9 (very varied)
                Vector2[] vertices = generateIrregularPolygon(size, sides, irregularity, spikiness, random);
                yield new ObstacleSpawn(position, vertices, harvestable, destructible, resources);
            }
        };
    }

    // ===== Symmetry transformation methods =====

    private List<ObstacleSpawn> mirror180Obstacles(List<ObstacleSpawn> pattern) {
        List<ObstacleSpawn> mirrored = new ArrayList<>();
        for (ObstacleSpawn spawn : pattern) {
            Vector2 pos = spawn.getPosition();
            Vector2 mirroredPos = new Vector2(-pos.x, -pos.y);

            if (spawn.getShape() == Obstacle.Shape.CIRCLE) {
                mirrored.add(new ObstacleSpawn(
                        mirroredPos,
                        spawn.getSize(),
                        spawn.isHarvestable(),
                        spawn.isDestructible(),
                        spawn.getResources()
                ));
            } else if (spawn.getShape() == Obstacle.Shape.POLYGON) {
                mirrored.add(new ObstacleSpawn(
                        mirroredPos,
                        spawn.getSize(),
                        spawn.getSides(),
                        spawn.isHarvestable(),
                        spawn.isDestructible(),
                        spawn.getResources()
                ));
            } else {
                // IRREGULAR_POLYGON - mirror the vertices too
                mirrored.add(new ObstacleSpawn(
                        mirroredPos,
                        spawn.getVertices(),
                        spawn.isHarvestable(),
                        spawn.isDestructible(),
                        spawn.getResources()
                ));
            }
        }
        return mirrored;
    }

    private List<ObstacleSpawn> rotate90Obstacles(List<ObstacleSpawn> pattern) {
        List<ObstacleSpawn> rotated = new ArrayList<>();
        for (ObstacleSpawn spawn : pattern) {
            Vector2 pos = spawn.getPosition();
            Vector2 rotatedPos = new Vector2(pos.y, -pos.x);

            if (spawn.getShape() == Obstacle.Shape.CIRCLE) {
                rotated.add(new ObstacleSpawn(
                        rotatedPos,
                        spawn.getSize(),
                        spawn.isHarvestable(),
                        spawn.isDestructible(),
                        spawn.getResources()
                ));
            } else if (spawn.getShape() == Obstacle.Shape.POLYGON) {
                rotated.add(new ObstacleSpawn(
                        rotatedPos,
                        spawn.getSize(),
                        spawn.getSides(),
                        spawn.isHarvestable(),
                        spawn.isDestructible(),
                        spawn.getResources()
                ));
            } else {
                // IRREGULAR_POLYGON
                rotated.add(new ObstacleSpawn(
                        rotatedPos,
                        spawn.getVertices(),
                        spawn.isHarvestable(),
                        spawn.isDestructible(),
                        spawn.getResources()
                ));
            }
        }
        return rotated;
    }

    private List<ObstacleSpawn> rotate180Obstacles(List<ObstacleSpawn> pattern) {
        return mirror180Obstacles(pattern);
    }

    private List<ObstacleSpawn> rotate270Obstacles(List<ObstacleSpawn> pattern) {
        List<ObstacleSpawn> rotated = new ArrayList<>();
        for (ObstacleSpawn spawn : pattern) {
            Vector2 pos = spawn.getPosition();
            Vector2 rotatedPos = new Vector2(-pos.y, pos.x);

            if (spawn.getShape() == Obstacle.Shape.CIRCLE) {
                rotated.add(new ObstacleSpawn(
                        rotatedPos,
                        spawn.getSize(),
                        spawn.isHarvestable(),
                        spawn.isDestructible(),
                        spawn.getResources()
                ));
            } else if (spawn.getShape() == Obstacle.Shape.POLYGON) {
                rotated.add(new ObstacleSpawn(
                        rotatedPos,
                        spawn.getSize(),
                        spawn.getSides(),
                        spawn.isHarvestable(),
                        spawn.isDestructible(),
                        spawn.getResources()
                ));
            } else {
                // IRREGULAR_POLYGON
                rotated.add(new ObstacleSpawn(
                        rotatedPos,
                        spawn.getVertices(),
                        spawn.isHarvestable(),
                        spawn.isDestructible(),
                        spawn.getResources()
                ));
            }
        }
        return rotated;
    }

    /**
     * Get start point for a specific team (0-indexed)
     */
    public Vector2 getTeamStartPoint(int teamIndex) {
        if (teamIndex >= 0 && teamIndex < teamStartPoints.size()) {
            return teamStartPoints.get(teamIndex).copy();
        }
        return new Vector2(0, 0); // Fallback to center
    }


    /**
     * Represents an obstacle spawn location with shape information
     */
    @Getter
    public static class ObstacleSpawn {
        private final Vector2 position;
        private final double size;
        private final Obstacle.Shape shape;
        private final int sides; // For polygons
        private final Vector2[] vertices; // For irregular polygons
        private final boolean harvestable; // Whether this obstacle contains resources
        private final boolean destructible; // Whether this obstacle can be destroyed
        private final int resources; // Amount of resources (if harvestable)

        // Circle constructor
        public ObstacleSpawn(Vector2 position, double size, boolean harvestable, boolean destructible, int resources) {
            this.position = position;
            this.size = size;
            this.shape = Obstacle.Shape.CIRCLE;
            this.sides = 0;
            this.vertices = null;
            this.harvestable = harvestable;
            this.destructible = destructible;
            this.resources = resources;
        }

        // Polygon constructor
        public ObstacleSpawn(Vector2 position, double size, int sides, boolean harvestable, boolean destructible, int resources) {
            this.position = position;
            this.size = size;
            this.shape = Obstacle.Shape.POLYGON;
            this.sides = sides;
            this.vertices = null;
            this.harvestable = harvestable;
            this.destructible = destructible;
            this.resources = resources;
        }

        // Irregular polygon constructor
        public ObstacleSpawn(Vector2 position, Vector2[] vertices, boolean harvestable, boolean destructible, int resources) {
            this.position = position;
            this.shape = Obstacle.Shape.IRREGULAR_POLYGON;
            this.sides = vertices.length;
            this.vertices = vertices;
            this.harvestable = harvestable;
            this.destructible = destructible;
            this.resources = resources;

            // Calculate bounding size from vertices
            double maxDist = 0.0;
            for (Vector2 v : vertices) {
                double dist = Math.sqrt(v.x * v.x + v.y * v.y);
                maxDist = Math.max(maxDist, dist);
            }
            this.size = maxDist;
        }

        public Vector2 getPosition() {
            return position;
        }

        public double getSize() {
            return size;
        }

        public Obstacle.Shape getShape() {
            return shape;
        }

        public int getSides() {
            return sides;
        }

        public Vector2[] getVertices() {
            return vertices;
        }
    }

    /**
     * Generate a random irregular polygon with the given number of vertices
     * GUARANTEED to be convex (required by dyn4j)
     *
     * @param baseRadius   Average distance from center
     * @param vertexCount  Number of vertices (3-12)
     * @param irregularity How much vertices deviate from regular polygon (0.0-1.0)
     * @param spikiness    How much radius varies per vertex (0.0-1.0)
     * @param random       Random number generator
     * @return Array of vertices in counter-clockwise order, guaranteed convex
     */
    public static Vector2[] generateIrregularPolygon(double baseRadius, int vertexCount,
                                                     double irregularity, double spikiness,
                                                     java.util.Random random) {
        // Clamp parameters
        vertexCount = Math.max(3, Math.min(12, vertexCount));
        irregularity = Math.max(0.0, Math.min(1.0, irregularity));
        spikiness = Math.max(0.0, Math.min(1.0, spikiness));

        // Generate evenly spaced angles first
        double angleStep = (2.0 * Math.PI) / vertexCount;

        // Limit angle variation to prevent concave shapes
        // Max variation is 40% of angle step to ensure convexity
        double maxAngleVariation = angleStep * 0.4 * irregularity;

        double[] angles = new double[vertexCount];
        double currentAngle = 0.0;

        for (int i = 0; i < vertexCount; i++) {
            // Add controlled randomness to angle
            double variation = (random.nextDouble() * 2.0 - 1.0) * maxAngleVariation;
            angles[i] = currentAngle + variation;
            currentAngle += angleStep;
        }

        // Ensure angles are properly ordered (maintain convexity)
        // Clamp each angle to stay within its sector
        for (int i = 0; i < vertexCount; i++) {
            double minAngle = angleStep * i;
            double maxAngle = angleStep * (i + 1);
            angles[i] = Math.max(minAngle, Math.min(maxAngle, angles[i]));
        }

        // Generate vertices with varying radii (limited to maintain convexity)
        Vector2[] vertices = new Vector2[vertexCount];

        // Limit radius variation to prevent extreme spikes that could cause concavity
        double maxRadiusVariation = baseRadius * spikiness * 0.5; // Max 50% variation

        for (int i = 0; i < vertexCount; i++) {
            // Random radius variation (limited)
            double radiusOffset = (random.nextDouble() * 2.0 - 1.0) * maxRadiusVariation;
            double radius = baseRadius + radiusOffset;

            // Ensure minimum radius (70% of base to maintain shape)
            radius = Math.max(baseRadius * 0.7, Math.min(baseRadius * 1.3, radius));

            // Calculate vertex position
            double x = Math.cos(angles[i]) * radius;
            double y = Math.sin(angles[i]) * radius;
            vertices[i] = new Vector2(x, y);
        }

        return vertices;
    }


    /**
     * Place obstacles from RTSWorld symmetric layout
     */
    public void placeObstacles() {
        for (RTSWorld.ObstacleSpawn spawn : getObstacleSpawns()) {
            Obstacle obstacle;

            // Use pre-determined properties from spawn (ensures symmetry)
            boolean harvestable = spawn.isHarvestable();
            boolean destructible = spawn.isDestructible();
            int resources = spawn.getResources();

            // Create obstacle based on shape type
            if (spawn.getShape() == Obstacle.Shape.IRREGULAR_POLYGON) {
                // Irregular polygon obstacle (custom vertices)
                obstacle = new Obstacle(
                        IdGenerator.nextEntityId(),
                        spawn.getPosition().x,
                        spawn.getPosition().y,
                        spawn.getVertices(),
                        destructible,
                        harvestable,
                        harvestable ? ResourceType.CREDITS : null,
                        resources
                );
            } else if (spawn.getShape() == Obstacle.Shape.POLYGON) {
                // Regular polygon obstacle
                obstacle = new Obstacle(
                        IdGenerator.nextEntityId(),
                        spawn.getPosition().x,
                        spawn.getPosition().y,
                        spawn.getSize(),
                        spawn.getSides(),
                        destructible,
                        harvestable,
                        harvestable ? ResourceType.CREDITS : null,
                        resources
                );
            } else {
                // Circle obstacle (default)
                obstacle = new Obstacle(
                        IdGenerator.nextEntityId(),
                        spawn.getPosition().x,
                        spawn.getPosition().y,
                        spawn.getSize(),
                        destructible,
                        harvestable,
                        harvestable ? ResourceType.CREDITS : null,
                        resources
                );
            }
            obstacles.add(obstacle);
        }
    }

    /**
     * Create world boundaries
     */
    public void createWorldBoundaries() {
        // Create rectangular obstacle walls around the world perimeter
        double wallThickness = 50.0; // Thickness of boundary walls

        // Top wall (horizontal rectangle)
        Obstacle topWall = new Obstacle(
                IdGenerator.nextEntityId(),
                0, // centered horizontally
                height / 2 - wallThickness / 2,
                width, // full width
                wallThickness // thin height
        );
        obstacles.add(topWall);

        // Bottom wall (horizontal rectangle)
        Obstacle bottomWall = new Obstacle(
                IdGenerator.nextEntityId(),
                0,
                -height / 2 + wallThickness / 2,
                width,
                wallThickness
        );
        obstacles.add(bottomWall);

        // Left wall (vertical rectangle)
        Obstacle leftWall = new Obstacle(
                IdGenerator.nextEntityId(),
                -width / 2 + wallThickness / 2,
                0, // centered vertically
                wallThickness, // thin width
                height // full height
        );
        obstacles.add(leftWall);

        // Right wall (vertical rectangle)
        Obstacle rightWall = new Obstacle(
                IdGenerator.nextEntityId(),
                width / 2 - wallThickness / 2,
                0,
                wallThickness,
                height
        );
        obstacles.add(rightWall);
    }

}


