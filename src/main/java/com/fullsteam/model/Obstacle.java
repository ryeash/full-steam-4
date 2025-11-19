package com.fullsteam.model;
import lombok.Getter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;

/**
 * Represents an obstacle in the RTS world.
 * Obstacles block movement and line of sight.
 */
@Getter
public class Obstacle extends GameEntity {
    public enum Shape {
        CIRCLE,
        RECTANGLE,
        POLYGON
    }
    
    private final Shape shape;
    private final double size; // For circles: radius; for rectangles: used as reference
    private final double width; // For rectangles
    private final double height; // For rectangles
    private final int sides; // For polygons
    
    private final boolean destructible;
    
    // Circle constructor
    public Obstacle(int id, double x, double y, double radius) {
        this(id, x, y, radius, false);
    }
    
    // Circle constructor with destructible option
    public Obstacle(int id, double x, double y, double radius, boolean destructible) {
        super(id, createCircleBody(x, y, radius), destructible ? calculateObstacleHealth(radius) : Double.MAX_VALUE);
        this.shape = Shape.CIRCLE;
        this.size = radius;
        this.width = radius * 2;
        this.height = radius * 2;
        this.sides = 0;
        this.destructible = destructible;
    }
    
    // Rectangle constructor
    public Obstacle(int id, double x, double y, double width, double height) {
        this(id, x, y, width, height, false);
    }
    
    // Rectangle constructor with destructible option
    public Obstacle(int id, double x, double y, double width, double height, boolean destructible) {
        super(id, createRectangleBody(x, y, width, height), destructible ? calculateObstacleHealth(Math.max(width, height)) : Double.MAX_VALUE);
        this.shape = Shape.RECTANGLE;
        this.size = Math.max(width, height) / 2;
        this.width = width;
        this.height = height;
        this.sides = 4;
        this.destructible = destructible;
    }
    
    // Polygon constructor
    public Obstacle(int id, double x, double y, double radius, int sides) {
        this(id, x, y, radius, sides, false);
    }
    
    // Polygon constructor with destructible option
    public Obstacle(int id, double x, double y, double radius, int sides, boolean destructible) {
        super(id, createPolygonBody(x, y, radius, sides), destructible ? calculateObstacleHealth(radius) : Double.MAX_VALUE);
        this.shape = Shape.POLYGON;
        this.size = radius;
        this.width = radius * 2;
        this.height = radius * 2;
        this.sides = sides;
        this.destructible = destructible;
    }
    
    /**
     * Calculate obstacle health based on size
     * Larger obstacles take longer to mine
     * 
     * Miner stats: 15 damage/sec, 100 pickaxe durability, 5 wear/sec
     * = 300 HP per trip (20 seconds of mining)
     * Target: 4-6 trips per obstacle = 1200-1800 HP
     */
    private static double calculateObstacleHealth(double size) {
        // Base health scaled significantly for multiple miner trips
        // Small obstacles (radius 20) = ~1200 HP (4 trips)
        // Medium obstacles (radius 40) = ~1500 HP (5 trips)
        // Large obstacles (radius 60) = ~1800 HP (6 trips)
        return 1000.0 + (size * 15.0);
    }
    
    private static Body createCircleBody(double x, double y, double radius) {
        Body body = new Body();
        org.dyn4j.dynamics.BodyFixture fixture = body.addFixture(Geometry.createCircle(radius));
        
        // Configure fixture properties
        fixture.setFriction(0.1);      // Low friction
        fixture.setRestitution(0.0);   // No bounce
        fixture.setSensor(false);      // Solid collision (not a sensor)
        
        body.setMass(MassType.INFINITE);
        body.getTransform().setTranslation(x, y);
        return body;
    }
    
    private static Body createRectangleBody(double x, double y, double width, double height) {
        Body body = new Body();
        org.dyn4j.dynamics.BodyFixture fixture = body.addFixture(Geometry.createRectangle(width, height));
        
        // Configure fixture properties
        fixture.setFriction(0.1);      // Low friction
        fixture.setRestitution(0.0);   // No bounce
        fixture.setSensor(false);      // Solid collision (not a sensor)
        
        body.setMass(MassType.INFINITE);
        body.getTransform().setTranslation(x, y);
        return body;
    }
    
    private static Body createPolygonBody(double x, double y, double radius, int sides) {
        Body body = new Body();
        org.dyn4j.dynamics.BodyFixture fixture = body.addFixture(Geometry.createPolygonalCircle(sides, radius));
        
        // Configure fixture properties
        fixture.setFriction(0.1);      // Low friction
        fixture.setRestitution(0.0);   // No bounce
        fixture.setSensor(false);      // Solid collision (not a sensor)
        
        body.setMass(MassType.INFINITE);
        body.getTransform().setTranslation(x, y);
        return body;
    }
    
    /**
     * Get bounding radius for collision checks
     */
    public double getBoundingRadius() {
        return size;
    }
}

