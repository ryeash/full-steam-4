package com.fullsteam.model;

import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

/**
 * Represents an obstacle in the RTS world.
 * Obstacles block movement and line of sight.
 * Some obstacles contain harvestable resources.
 */
@Getter
public class Obstacle extends GameEntity {
    public enum Shape {
        CIRCLE,
        RECTANGLE,
        POLYGON,
        IRREGULAR_POLYGON  // Custom vertices
    }

    private final Shape shape;
    private final double size; // For circles: radius; for rectangles: used as reference
    private final double width; // For rectangles
    private final double height; // For rectangles
    private final int sides; // For polygons
    private final Vector2[] vertices; // For irregular polygons

    private final boolean destructible;
    
    // Resource harvesting fields
    private final boolean harvestable;
    private final ResourceType resourceType;
    @Setter
    private double remainingResources;
    private final double maxResources;
    private static final double HARVEST_RANGE = 50.0;

    // Circle constructor
    public Obstacle(int id, double x, double y, double radius) {
        this(id, x, y, radius, false, false, null, 0);
    }

    // Circle constructor with destructible option
    public Obstacle(int id, double x, double y, double radius, boolean destructible) {
        this(id, x, y, radius, destructible, false, null, 0);
    }
    
    // Circle constructor with harvestable resources
    public Obstacle(int id, double x, double y, double radius, boolean destructible, 
                    boolean harvestable, ResourceType resourceType, double resources) {
        super(id, createCircleBody(x, y, radius), 
              harvestable ? Double.MAX_VALUE : (destructible ? calculateObstacleHealth(radius) : Double.MAX_VALUE));
        this.shape = Shape.CIRCLE;
        this.size = radius;
        this.width = radius * 2;
        this.height = radius * 2;
        this.sides = 0;
        this.vertices = null;
        this.destructible = destructible;
        this.harvestable = harvestable;
        this.resourceType = resourceType;
        this.remainingResources = resources;
        this.maxResources = resources;
    }

    // Rectangle constructor
    public Obstacle(int id, double x, double y, double width, double height) {
        this(id, x, y, width, height, false, false, null, 0);
    }

    // Rectangle constructor with destructible option
    public Obstacle(int id, double x, double y, double width, double height, boolean destructible) {
        this(id, x, y, width, height, destructible, false, null, 0);
    }
    
    // Rectangle constructor with harvestable resources
    public Obstacle(int id, double x, double y, double width, double height, boolean destructible,
                    boolean harvestable, ResourceType resourceType, double resources) {
        super(id, createRectangleBody(x, y, width, height), 
              harvestable ? Double.MAX_VALUE : (destructible ? calculateObstacleHealth(Math.max(width, height)) : Double.MAX_VALUE));
        this.shape = Shape.RECTANGLE;
        this.size = Math.max(width, height) / 2;
        this.width = width;
        this.height = height;
        this.sides = 4;
        this.vertices = null;
        this.destructible = destructible;
        this.harvestable = harvestable;
        this.resourceType = resourceType;
        this.remainingResources = resources;
        this.maxResources = resources;
    }

    // Polygon constructor
    public Obstacle(int id, double x, double y, double radius, int sides) {
        this(id, x, y, radius, sides, false, false, null, 0);
    }

    // Polygon constructor with destructible option
    public Obstacle(int id, double x, double y, double radius, int sides, boolean destructible) {
        this(id, x, y, radius, sides, destructible, false, null, 0);
    }
    
    // Polygon constructor with harvestable resources
    public Obstacle(int id, double x, double y, double radius, int sides, boolean destructible,
                    boolean harvestable, ResourceType resourceType, double resources) {
        super(id, createPolygonBody(x, y, radius, sides), 
              harvestable ? Double.MAX_VALUE : (destructible ? calculateObstacleHealth(radius) : Double.MAX_VALUE));
        this.shape = Shape.POLYGON;
        this.size = radius;
        this.width = radius * 2;
        this.height = radius * 2;
        this.sides = sides;
        this.vertices = null;
        this.destructible = destructible;
        this.harvestable = harvestable;
        this.resourceType = resourceType;
        this.remainingResources = resources;
        this.maxResources = resources;
    }

    // Irregular polygon constructor (custom vertices)
    public Obstacle(int id, double x, double y, Vector2[] vertices, boolean destructible) {
        this(id, x, y, vertices, destructible, false, null, 0);
    }
    
    // Irregular polygon constructor with harvestable resources
    public Obstacle(int id, double x, double y, Vector2[] vertices, boolean destructible,
                    boolean harvestable, ResourceType resourceType, double resources) {
        super(id, createIrregularPolygonBody(x, y, vertices), 
              harvestable ? Double.MAX_VALUE : (destructible ? calculateObstacleHealth(calculateBoundingRadius(vertices)) : Double.MAX_VALUE));
        this.shape = Shape.IRREGULAR_POLYGON;
        this.size = calculateBoundingRadius(vertices);
        this.width = this.size * 2;
        this.height = this.size * 2;
        this.sides = vertices.length;
        this.vertices = vertices;
        this.destructible = destructible;
        this.harvestable = harvestable;
        this.resourceType = resourceType;
        this.remainingResources = resources;
        this.maxResources = resources;
    }

    private static double calculateObstacleHealth(double size) {
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

    private static Body createIrregularPolygonBody(double x, double y, Vector2[] vertices) {
        Body body = new Body();

        // Ensure counter-clockwise winding
        Vector2[] ccwVertices = ensureCounterClockwise(vertices);

        try {
            org.dyn4j.dynamics.BodyFixture fixture = body.addFixture(Geometry.createPolygon(ccwVertices));

            // Configure fixture properties
            fixture.setFriction(0.1);      // Low friction
            fixture.setRestitution(0.0);   // No bounce
            fixture.setSensor(false);      // Solid collision (not a sensor)
        } catch (IllegalArgumentException e) {
            // If polygon is not convex, fall back to a regular polygon
            System.err.println("Failed to create irregular polygon (not convex), falling back to regular polygon");
            double radius = calculateBoundingRadius(vertices);
            int sides = vertices.length;
            org.dyn4j.dynamics.BodyFixture fixture = body.addFixture(Geometry.createPolygonalCircle(sides, radius));
            fixture.setFriction(0.1);
            fixture.setRestitution(0.0);
            fixture.setSensor(false);
        }

        body.setMass(MassType.INFINITE);
        body.getTransform().setTranslation(x, y);
        return body;
    }

    /**
     * Ensure vertices are in counter-clockwise order (required by dyn4j)
     */
    private static Vector2[] ensureCounterClockwise(Vector2[] vertices) {
        // Calculate signed area to determine winding order
        double signedArea = 0.0;
        for (int i = 0; i < vertices.length; i++) {
            Vector2 v1 = vertices[i];
            Vector2 v2 = vertices[(i + 1) % vertices.length];
            signedArea += (v2.x - v1.x) * (v2.y + v1.y);
        }

        // If area is positive, vertices are clockwise - reverse them
        if (signedArea > 0) {
            Vector2[] reversed = new Vector2[vertices.length];
            for (int i = 0; i < vertices.length; i++) {
                reversed[i] = vertices[vertices.length - 1 - i];
            }
            return reversed;
        }

        return vertices;
    }

    /**
     * Calculate bounding radius from vertices
     */
    private static double calculateBoundingRadius(Vector2[] vertices) {
        double maxDistance = 0.0;
        for (Vector2 vertex : vertices) {
            double distance = Math.sqrt(vertex.x * vertex.x + vertex.y * vertex.y);
            maxDistance = Math.max(maxDistance, distance);
        }
        return maxDistance;
    }

    /**
     * Get bounding radius for collision checks
     */
    public double getBoundingRadius() {
        return size;
    }
    
    /**
     * Harvest resources from this obstacle
     * @param amount Amount to harvest
     * @return Actual amount harvested (may be less if depleted)
     */
    public double harvest(double amount) {
        if (!harvestable || remainingResources <= 0) {
            if (remainingResources <= 0) {
                active = false; // Depleted obstacle disappears
            }
            return 0;
        }
        
        double harvested = Math.min(amount, remainingResources);
        remainingResources -= harvested;
        
        if (remainingResources <= 0) {
            active = false; // Obstacle is depleted and disappears
        }
        
        return harvested;
    }
    
    /**
     * Get remaining resources as a percentage
     */
    public double getResourcePercent() {
        if (!harvestable || maxResources <= 0) {
            return 0.0;
        }
        return remainingResources / maxResources;
    }
    
    /**
     * Get harvest range
     */
    public double getHarvestRange() {
        return HARVEST_RANGE;
    }
}

