package com.fullsteam.model;

import lombok.Getter;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents the RTS game world with bounded rectangle, start points, and symmetric resource/obstacle placement.
 * Uses 90-degree rotational symmetry for fair team positioning (2 or 4 teams).
 */
@Getter
public class RTSWorld {
    private final double width;
    private final double height;
    private final int teamCount;
    private final Biome biome;
    private final double obstacleDensityMultiplier;

    // Start positions for each team (90-degree symmetry)
    private final List<Vector2> teamStartPoints;

    // Resource deposits (90-degree symmetric placement)
    private final List<ResourceDepositSpawn> resourceSpawns;

    // Obstacles (90-degree symmetric placement)
    private final List<ObstacleSpawn> obstacleSpawns;

    // World bounds
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;

    public RTSWorld(double width, double height, int teamCount, Biome biome,
                    double obstacleDensityMultiplier, long seed) {
        this.width = width;
        this.height = height;
        this.teamCount = teamCount;
        this.biome = biome;
        this.obstacleDensityMultiplier = obstacleDensityMultiplier;

        // Calculate bounds
        this.minX = -width / 2.0;
        this.maxX = width / 2.0;
        this.minY = -height / 2.0;
        this.maxY = height / 2.0;

        // Generate symmetric world layout
        this.teamStartPoints = generateTeamStartPoints();
        this.resourceSpawns = generateResourceSpawns();
        this.obstacleSpawns = generateObstacleSpawns();
    }

    /**
     * Generate team start points with 90-degree symmetry.
     * Teams are placed in corners or on edges depending on team count.
     */
    private List<Vector2> generateTeamStartPoints() {
        List<Vector2> startPoints = new ArrayList<>();

        // Distance from edge to start point
        double margin = Math.min(width, height) * 0.15; // 15% from edge

        switch (teamCount) {
            case 2:
                // Two teams: opposite corners (diagonal symmetry)
                startPoints.add(new Vector2(minX + margin, minY + margin)); // Bottom-left
                startPoints.add(new Vector2(maxX - margin, maxY - margin)); // Top-right
                break;

            case 4:
                // Four teams: all four corners (90-degree rotational symmetry)
                startPoints.add(new Vector2(minX + margin, minY + margin)); // Bottom-left
                startPoints.add(new Vector2(maxX - margin, minY + margin)); // Bottom-right
                startPoints.add(new Vector2(maxX - margin, maxY - margin)); // Top-right
                startPoints.add(new Vector2(minX + margin, maxY - margin)); // Top-left
                break;

            case 3:
                // Three teams: not ideal for 90-degree symmetry, but place evenly
                double radius = Math.min(width, height) * 0.35;
                for (int i = 0; i < 3; i++) {
                    double angle = (Math.PI * 2 * i / 3) - Math.PI / 2; // Start from top
                    startPoints.add(new Vector2(
                            Math.cos(angle) * radius,
                            Math.sin(angle) * radius
                    ));
                }
                break;

            default:
                // Single team or invalid - center spawn
                startPoints.add(new Vector2(0, 0));
                break;
        }

        return startPoints;
    }

    /**
     * Generate resource deposit spawns with 90-degree symmetry.
     * Creates a base pattern in one quadrant and mirrors it to others.
     */
    private List<ResourceDepositSpawn> generateResourceSpawns() {
        List<ResourceDepositSpawn> spawns = new ArrayList<>();

        // Generate base pattern in first quadrant (or half for 2 teams)
        List<ResourceDepositSpawn> basePattern = generateBaseResourcePattern();

        // Mirror pattern based on team count
        if (teamCount == 2) {
            // 180-degree symmetry for 2 teams
            spawns.addAll(basePattern);
            spawns.addAll(mirror180(basePattern));
        } else if (teamCount == 4) {
            // 90-degree rotational symmetry for 4 teams
            spawns.addAll(basePattern);
            spawns.addAll(rotate90(basePattern));
            spawns.addAll(rotate180(basePattern));
            spawns.addAll(rotate270(basePattern));
        } else {
            // For other team counts, just use base pattern
            spawns.addAll(basePattern);
        }

        return spawns;
    }

    /**
     * Generate base resource pattern in one quadrant/section.
     */
    private List<ResourceDepositSpawn> generateBaseResourcePattern() {
        List<ResourceDepositSpawn> pattern = new ArrayList<>();

        // Define the working area (one quadrant for 4 teams, one half for 2 teams)
        double workWidth = width / 2.0;
        double workHeight = height / 2.0;

        // Place 2-3 resource deposits in this section
        int depositsInSection = 2 + ThreadLocalRandom.current().nextInt(2);

        for (int i = 0; i < depositsInSection; i++) {
            // Random position within the working area, avoiding edges
            double margin = 100; // Stay away from edges
            double x = margin + ThreadLocalRandom.current().nextDouble() * (workWidth - 2 * margin);
            double y = margin + ThreadLocalRandom.current().nextDouble() * (workHeight - 2 * margin);

            // For 4 teams, place in first quadrant (positive x, positive y)
            // For 2 teams, place in bottom half (any x, negative y)
            if (teamCount != 4) {
                x = x - workWidth / 2.0; // Center around 0
                y = -y; // Place in bottom half
            }

            int resources = 5000 + ThreadLocalRandom.current().nextInt(5000); // 5000-10000 resources
            pattern.add(new ResourceDepositSpawn(new Vector2(x, y), resources));
        }

        // Add one central resource deposit (shared/contested)
        if (pattern.isEmpty() || ThreadLocalRandom.current().nextDouble() < 0.5) {
            pattern.add(new ResourceDepositSpawn(new Vector2(0, 0), 10000));
        }

        return pattern;
    }

    /**
     * Generate obstacle spawns with 90-degree symmetry.
     */
    private List<ObstacleSpawn> generateObstacleSpawns() {
        List<ObstacleSpawn> spawns = new ArrayList<>();

        // Generate base pattern
        List<ObstacleSpawn> basePattern = generateBaseObstaclePattern();

        // Mirror pattern based on team count
        if (teamCount == 2) {
            spawns.addAll(basePattern);
            spawns.addAll(mirror180Obstacles(basePattern));
        } else if (teamCount == 4) {
            spawns.addAll(basePattern);
            spawns.addAll(rotate90Obstacles(basePattern));
            spawns.addAll(rotate180Obstacles(basePattern));
            spawns.addAll(rotate270Obstacles(basePattern));
        } else {
            spawns.addAll(basePattern);
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

        // Define exclusion radius around starting positions (to prevent blocking HQ and starting units)
        double startExclusionRadius = 400.0; // Clear 400 units around each start point

        // Generate obstacles with multiple attempts to avoid starting areas
        int attempts = 0;
        int maxAttempts = obstaclesInSection * 3; // Allow 3x attempts to find valid positions

        while (pattern.size() < obstaclesInSection && attempts < maxAttempts) {
            attempts++;

            // Generate random position within working area
            double margin = 150;
            double x, y;

            if (teamCount == 4) {
                // For 4 teams, generate in first quadrant (positive x, positive y)
                x = margin + ThreadLocalRandom.current().nextDouble() * (workWidth / 2.0 - 2 * margin);
                y = margin + ThreadLocalRandom.current().nextDouble() * (workHeight / 2.0 - 2 * margin);
            } else {
                // For 2 teams, generate across full width, bottom half
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

                // Generate obstacle with biome-specific shape
                pattern.add(generateBiomeObstacle(obstaclePos, size, ThreadLocalRandom.current()));
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
    private ObstacleSpawn generateBiomeObstacle(Vector2 position, double size, Random random) {
        return switch (biome) {
            case GRASSLAND -> {
                // Trees: Mix of circles (round trees) and irregular organic shapes
                if (random.nextDouble() < 0.3) {
                    yield new ObstacleSpawn(position, size); // Circle (round tree)
                } else {
                    // Bushy, organic tree shapes
                    int sides = 6 + random.nextInt(3); // 6-8 sides
                    double irregularity = 0.3 + random.nextDouble() * 0.3; // 0.3-0.6
                    double spikiness = 0.2 + random.nextDouble() * 0.3;    // 0.2-0.5
                    Vector2[] vertices = generateIrregularPolygon(size, sides, irregularity, spikiness, random);
                    yield new ObstacleSpawn(position, vertices);
                }
            }
            case DESERT -> {
                // Rocks: Very irregular, weathered shapes
                int sides = 5 + random.nextInt(4); // 5-8 sides
                double irregularity = 0.4 + random.nextDouble() * 0.4; // 0.4-0.8 (very irregular)
                double spikiness = 0.3 + random.nextDouble() * 0.4;    // 0.3-0.7 (quite spiky)
                Vector2[] vertices = generateIrregularPolygon(size, sides, irregularity, spikiness, random);
                yield new ObstacleSpawn(position, vertices);
            }
            case SNOW -> {
                // Ice: Mix of regular crystals and irregular chunks
                if (random.nextDouble() < 0.4) {
                    yield new ObstacleSpawn(position, size, 6); // Regular hexagon (ice crystal)
                } else {
                    // Irregular ice chunks
                    int sides = 5 + random.nextInt(3); // 5-7 sides
                    double irregularity = 0.2 + random.nextDouble() * 0.3; // 0.2-0.5
                    double spikiness = 0.3 + random.nextDouble() * 0.4;    // 0.3-0.7 (sharp edges)
                    Vector2[] vertices = generateIrregularPolygon(size, sides, irregularity, spikiness, random);
                    yield new ObstacleSpawn(position, vertices);
                }
            }
            case VOLCANIC -> {
                // Lava rocks: Sharp, angular, highly irregular shapes
                int sides = 4 + random.nextInt(4); // 4-7 sides
                double irregularity = 0.5 + random.nextDouble() * 0.4; // 0.5-0.9 (very angular)
                double spikiness = 0.4 + random.nextDouble() * 0.5;    // 0.4-0.9 (very spiky)
                Vector2[] vertices = generateIrregularPolygon(size, sides, irregularity, spikiness, random);
                yield new ObstacleSpawn(position, vertices);
            }
            case URBAN -> {
                // Rubble: Extremely irregular, chaotic shapes
                int sides = 4 + random.nextInt(6); // 4-9 sides
                double irregularity = 0.6 + random.nextDouble() * 0.3; // 0.6-0.9 (chaotic)
                double spikiness = 0.5 + random.nextDouble() * 0.4;    // 0.5-0.9 (very varied)
                Vector2[] vertices = generateIrregularPolygon(size, sides, irregularity, spikiness, random);
                yield new ObstacleSpawn(position, vertices);
            }
        };
    }

    // ===== Symmetry transformation methods =====

    /**
     * Mirror resource deposits 180 degrees (for 2-team symmetry)
     */
    private List<ResourceDepositSpawn> mirror180(List<ResourceDepositSpawn> pattern) {
        List<ResourceDepositSpawn> mirrored = new ArrayList<>();
        for (ResourceDepositSpawn spawn : pattern) {
            Vector2 pos = spawn.getPosition();
            mirrored.add(new ResourceDepositSpawn(
                    new Vector2(-pos.x, -pos.y),
                    spawn.getResources()
            ));
        }
        return mirrored;
    }

    /**
     * Rotate resource deposits 90 degrees clockwise
     */
    private List<ResourceDepositSpawn> rotate90(List<ResourceDepositSpawn> pattern) {
        List<ResourceDepositSpawn> rotated = new ArrayList<>();
        for (ResourceDepositSpawn spawn : pattern) {
            Vector2 pos = spawn.getPosition();
            // 90-degree clockwise: (x, y) -> (y, -x)
            rotated.add(new ResourceDepositSpawn(new Vector2(pos.y, -pos.x), spawn.getResources()));
        }
        return rotated;
    }

    /**
     * Rotate resource deposits 180 degrees
     */
    private List<ResourceDepositSpawn> rotate180(List<ResourceDepositSpawn> pattern) {
        return mirror180(pattern);
    }

    /**
     * Rotate resource deposits 270 degrees clockwise (90 counter-clockwise)
     */
    private List<ResourceDepositSpawn> rotate270(List<ResourceDepositSpawn> pattern) {
        List<ResourceDepositSpawn> rotated = new ArrayList<>();
        for (ResourceDepositSpawn spawn : pattern) {
            Vector2 pos = spawn.getPosition();
            // 270-degree clockwise: (x, y) -> (-y, x)
            rotated.add(new ResourceDepositSpawn(new Vector2(-pos.y, pos.x), spawn.getResources()));
        }
        return rotated;
    }

    private List<ObstacleSpawn> mirror180Obstacles(List<ObstacleSpawn> pattern) {
        List<ObstacleSpawn> mirrored = new ArrayList<>();
        for (ObstacleSpawn spawn : pattern) {
            Vector2 pos = spawn.getPosition();
            if (spawn.getShape() == Obstacle.Shape.CIRCLE) {
                mirrored.add(new ObstacleSpawn(new Vector2(-pos.x, -pos.y), spawn.getSize()));
            } else {
                mirrored.add(new ObstacleSpawn(new Vector2(-pos.x, -pos.y), spawn.getSize(), spawn.getSides()));
            }
        }
        return mirrored;
    }

    private List<ObstacleSpawn> rotate90Obstacles(List<ObstacleSpawn> pattern) {
        List<ObstacleSpawn> rotated = new ArrayList<>();
        for (ObstacleSpawn spawn : pattern) {
            Vector2 pos = spawn.getPosition();
            if (spawn.getShape() == Obstacle.Shape.CIRCLE) {
                rotated.add(new ObstacleSpawn(new Vector2(pos.y, -pos.x), spawn.getSize()));
            } else {
                rotated.add(new ObstacleSpawn(new Vector2(pos.y, -pos.x), spawn.getSize(), spawn.getSides()));
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
            if (spawn.getShape() == Obstacle.Shape.CIRCLE) {
                rotated.add(new ObstacleSpawn(new Vector2(-pos.y, pos.x), spawn.getSize()));
            } else {
                rotated.add(new ObstacleSpawn(new Vector2(-pos.y, pos.x), spawn.getSize(), spawn.getSides()));
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
     * Represents a resource deposit spawn location
     */
    @Getter
    public static class ResourceDepositSpawn {
        private final Vector2 position;
        private final int resources;

        public ResourceDepositSpawn(Vector2 position, int resources) {
            this.position = position;
            this.resources = resources;
        }
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

        // Circle constructor
        public ObstacleSpawn(Vector2 position, double size) {
            this.position = position;
            this.size = size;
            this.shape = Obstacle.Shape.CIRCLE;
            this.sides = 0;
            this.vertices = null;
        }

        // Polygon constructor
        public ObstacleSpawn(Vector2 position, double size, int sides) {
            this.position = position;
            this.size = size;
            this.shape = Obstacle.Shape.POLYGON;
            this.sides = sides;
            this.vertices = null;
        }

        // Irregular polygon constructor
        public ObstacleSpawn(Vector2 position, Vector2[] vertices) {
            this.position = position;
            this.shape = Obstacle.Shape.IRREGULAR_POLYGON;
            this.sides = vertices.length;
            this.vertices = vertices;

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
}


