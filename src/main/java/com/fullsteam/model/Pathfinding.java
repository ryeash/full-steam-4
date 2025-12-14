package com.fullsteam.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.dyn4j.geometry.Vector2;

import java.util.*;

/**
 * A* pathfinding implementation for RTS units
 */
public class Pathfinding {
    private static final double GRID_SIZE = 35.0; // Grid cell size for pathfinding
    private static final int MAX_PATH_LENGTH = 100; // Maximum path nodes to prevent infinite loops
    
    /**
     * Find a path from start to goal, avoiding obstacles and buildings
     * @param isAirborne Whether the unit is airborne (flying units ignore obstacles)
     */
    public static List<Vector2> findPath(
            Vector2 start, 
            Vector2 goal, 
            Collection<Obstacle> obstacles,
            Collection<Building> buildings,
            double unitRadius,
            double worldWidth,
            double worldHeight,
            boolean isAirborne
    ) {
        // Flying units: always use direct path (ignore obstacles)
        if (isAirborne) {
            return Arrays.asList(start, goal);
        }
        
        // Quick check: if goal is in direct line of sight, return direct path
        if (isDirectPathClear(start, goal, obstacles, buildings, unitRadius)) {
            return Arrays.asList(start, goal);
        }
        
        // Convert world positions to grid coordinates
        GridNode startNode = worldToGrid(start);
        GridNode goalNode = worldToGrid(goal);
        
        // A* algorithm
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Set<GridNode> closedSet = new HashSet<>();
        Map<GridNode, GridNode> cameFrom = new HashMap<>();
        Map<GridNode, Double> gScore = new HashMap<>();
        
        gScore.put(startNode, 0.0);
        openSet.add(new AStarNode(startNode, 0.0, heuristic(startNode, goalNode)));
        
        int iterations = 0;
        while (!openSet.isEmpty() && iterations < 1000) {
            iterations++;
            
            AStarNode current = openSet.poll();
            
            // Goal reached
            if (current.node.equals(goalNode)) {
                return reconstructPath(cameFrom, current.node, start, goal);
            }
            
            closedSet.add(current.node);
            
            // Check all neighbors
            for (GridNode neighbor : getNeighbors(current.node)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }
                
                // Check if this grid cell is blocked
                Vector2 neighborWorld = gridToWorld(neighbor);
                if (isPositionBlocked(neighborWorld, obstacles, buildings, unitRadius, worldWidth, worldHeight)) {
                    continue;
                }
                
                double tentativeGScore = gScore.getOrDefault(current.node, Double.MAX_VALUE) + 
                                        distance(current.node, neighbor);
                
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    cameFrom.put(neighbor, current.node);
                    gScore.put(neighbor, tentativeGScore);
                    double fScore = tentativeGScore + heuristic(neighbor, goalNode);
                    
                    // Remove old entry if exists and add new one
                    openSet.removeIf(n -> n.node.equals(neighbor));
                    openSet.add(new AStarNode(neighbor, tentativeGScore, fScore));
                }
            }
        }
        
        // No path found, return direct path (unit will try to move as close as possible)
        return Arrays.asList(start, goal);
    }
    
    /**
     * Check if there's a direct line of sight between two points
     */
    private static boolean isDirectPathClear(
            Vector2 start, 
            Vector2 goal,
            Collection<Obstacle> obstacles,
            Collection<Building> buildings,
            double unitRadius
    ) {
        // Sample points along the line
        double distance = start.distance(goal);
        int samples = (int) Math.ceil(distance / (GRID_SIZE / 2));
        
        for (int i = 1; i < samples; i++) {
            double t = (double) i / samples;
            Vector2 point = new Vector2(
                start.x + (goal.x - start.x) * t,
                start.y + (goal.y - start.y) * t
            );
            
            // Check if this point collides with anything
            for (Obstacle obstacle : obstacles) {
                if (circleIntersectsObstacle(point, unitRadius, obstacle)) {
                    return false;
                }
            }
            
            for (Building building : buildings) {
                Vector2 buildingPos = building.getPosition();
                double dx = Math.abs(point.x - buildingPos.x);
                double dy = Math.abs(point.y - buildingPos.y);
                double size = building.getBuildingType().getSize();
                if (dx < size + unitRadius && dy < size + unitRadius) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Check if a position is blocked by obstacles or buildings
     */
    private static boolean isPositionBlocked(
            Vector2 position,
            Collection<Obstacle> obstacles,
            Collection<Building> buildings,
            double unitRadius,
            double worldWidth,
            double worldHeight
    ) {
        // Check world bounds
        double halfWidth = worldWidth / 2.0;
        double halfHeight = worldHeight / 2.0;
        if (Math.abs(position.x) > halfWidth - unitRadius || 
            Math.abs(position.y) > halfHeight - unitRadius) {
            return true;
        }
        
        // Check obstacles
        for (Obstacle obstacle : obstacles) {
            if (circleIntersectsObstacle(position, unitRadius, obstacle)) {
                return true;
            }
        }
        
        // Check buildings
        for (Building building : buildings) {
            Vector2 buildingPos = building.getPosition();
            double dx = Math.abs(position.x - buildingPos.x);
            double dy = Math.abs(position.y - buildingPos.y);
            double size = building.getBuildingType().getSize();
            if (dx < size + unitRadius && dy < size + unitRadius) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a circle intersects with an obstacle
     */
    private static boolean circleIntersectsObstacle(Vector2 position, double radius, Obstacle obstacle) {
        Vector2 obstaclePos = obstacle.getPosition();
        double dx = position.x - obstaclePos.x;
        double dy = position.y - obstaclePos.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (obstacle.getShape() == Obstacle.Shape.CIRCLE) {
            return distance < obstacle.getSize() + radius;
        } else if (obstacle.getShape() == Obstacle.Shape.RECTANGLE) {
            // AABB collision for rectangles
            double halfWidth = obstacle.getWidth() / 2.0;
            double halfHeight = obstacle.getHeight() / 2.0;
            double closestX = Math.max(obstaclePos.x - halfWidth, Math.min(position.x, obstaclePos.x + halfWidth));
            double closestY = Math.max(obstaclePos.y - halfHeight, Math.min(position.y, obstaclePos.y + halfHeight));
            double distX = position.x - closestX;
            double distY = position.y - closestY;
            return (distX * distX + distY * distY) < (radius * radius);
        } else {
            // Polygon - approximate as circle
            return distance < obstacle.getSize() + radius;
        }
    }
    
    /**
     * Reconstruct path from A* search
     */
    private static List<Vector2> reconstructPath(
            Map<GridNode, GridNode> cameFrom,
            GridNode current,
            Vector2 startWorld,
            Vector2 goalWorld
    ) {
        List<Vector2> path = new ArrayList<>();
        path.add(startWorld); // Start with actual start position
        
        // Build path from goal to start
        List<GridNode> gridPath = new ArrayList<>();
        GridNode node = current;
        while (cameFrom.containsKey(node)) {
            gridPath.add(node);
            node = cameFrom.get(node);
        }
        
        // Reverse to get start to goal
        Collections.reverse(gridPath);
        
        // Convert grid nodes to world positions and simplify path
        for (int i = 0; i < gridPath.size() && i < MAX_PATH_LENGTH; i++) {
            path.add(gridToWorld(gridPath.get(i)));
        }
        
        path.add(goalWorld); // End with actual goal position
        
        // Simplify path by removing unnecessary waypoints
        return simplifyPath(path);
    }
    
    /**
     * Simplify path by removing collinear points
     */
    private static List<Vector2> simplifyPath(List<Vector2> path) {
        if (path.size() <= 2) {
            return path;
        }
        
        List<Vector2> simplified = new ArrayList<>();
        simplified.add(path.get(0));
        
        for (int i = 1; i < path.size() - 1; i++) {
            Vector2 prev = path.get(i - 1);
            Vector2 curr = path.get(i);
            Vector2 next = path.get(i + 1);
            
            // Check if current point is necessary (not collinear)
            double angle1 = Math.atan2(curr.y - prev.y, curr.x - prev.x);
            double angle2 = Math.atan2(next.y - curr.y, next.x - curr.x);
            double angleDiff = Math.abs(angle1 - angle2);
            
            // Keep point if there's a significant direction change
            if (angleDiff > 0.1) {
                simplified.add(curr);
            }
        }
        
        simplified.add(path.get(path.size() - 1));
        return simplified;
    }
    
    /**
     * Get neighboring grid cells
     */
    private static List<GridNode> getNeighbors(GridNode node) {
        List<GridNode> neighbors = new ArrayList<>();
        
        // 8-directional movement
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                neighbors.add(new GridNode(node.x + dx, node.y + dy));
            }
        }
        
        return neighbors;
    }
    
    /**
     * Heuristic function (Euclidean distance)
     */
    private static double heuristic(GridNode a, GridNode b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Distance between two grid nodes
     */
    private static double distance(GridNode a, GridNode b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Convert world position to grid coordinates
     */
    private static GridNode worldToGrid(Vector2 world) {
        return new GridNode(
            (int) Math.round(world.x / GRID_SIZE),
            (int) Math.round(world.y / GRID_SIZE)
        );
    }
    
    /**
     * Convert grid coordinates to world position
     */
    private static Vector2 gridToWorld(GridNode grid) {
        return new Vector2(grid.x * GRID_SIZE, grid.y * GRID_SIZE);
    }
    
    /**
     * Grid node for pathfinding
     */
    @Data
    @AllArgsConstructor
    private static class GridNode {
        int x;
        int y;
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GridNode gridNode = (GridNode) o;
            return x == gridNode.x && y == gridNode.y;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
    
    /**
     * A* node with scores
     */
    @Data
    @AllArgsConstructor
    private static class AStarNode {
        GridNode node;
        double gScore;
        double fScore;
    }
}

