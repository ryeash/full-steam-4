package com.fullsteam.model;

import lombok.extern.slf4j.Slf4j;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.BroadphaseCollisionData;
import org.dyn4j.world.ManifoldCollisionData;
import org.dyn4j.world.NarrowphaseCollisionData;
import org.dyn4j.world.listener.CollisionListener;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Centralized collision detection and handling for RTS game mode.
 * Handles projectile collisions with units, buildings, and obstacles.
 * Implements CollisionListener to control physics collision behavior.
 */
@Slf4j
public class RTSCollisionProcessor implements CollisionListener<Body, BodyFixture> {

    private final Map<Integer, Unit> units;
    private final Map<Integer, Building> buildings;
    private final Map<Integer, Obstacle> obstacles;
    private final Map<Integer, WallSegment> wallSegments;
    private final BiConsumer<Vector2, ExplosionParams> explosionCreator;

    /**
     * Parameters for creating explosions
     */
    public record ExplosionParams(double damage, double radius, int ownerTeam) {
    }

    public RTSCollisionProcessor(GameEntities gameEntities,
                                 BiConsumer<Vector2, ExplosionParams> explosionCreator) {
        this.units = gameEntities.getUnits();
        this.buildings = gameEntities.getBuildings();
        this.obstacles = gameEntities.getObstacles();
        this.wallSegments = gameEntities.getWallSegments();
        this.explosionCreator = explosionCreator;
    }

    /**
     * Handle a projectile hitting a unit
     */
    private void handleProjectileUnitHit(Projectile projectile, Unit unit, Vector2 hitPosition) {
        // Mark unit as affected to prevent multiple hits from same projectile
        projectile.getAffectedPlayers().add(unit.getId());

        // Apply damage
        boolean died = unit.takeDamage(projectile.getDamage());

        log.debug("Projectile {} hit unit {} for {} damage (died: {})",
                projectile.getId(), unit.getId(), projectile.getDamage(), died);

        // Create explosion for explosive projectiles
        if (isExplosiveProjectile(projectile)) {
            createExplosionEffect(hitPosition, projectile);
        }
    }

    /**
     * Handle a projectile hitting a building
     */
    private void handleProjectileBuildingHit(Projectile projectile, Building building, Vector2 hitPosition) {
        // Mark building as affected
        projectile.getAffectedPlayers().add(building.getId());

        // Apply damage
        boolean destroyed = building.takeDamage(projectile.getDamage());

        log.debug("Projectile {} hit building {} for {} damage (destroyed: {})",
                projectile.getId(), building.getId(), projectile.getDamage(), destroyed);

        // Create explosion for explosive projectiles
        if (isExplosiveProjectile(projectile)) {
            createExplosionEffect(hitPosition, projectile);
        }
    }

    /**
     * Handle a projectile hitting a wall segment
     */
    private void handleProjectileWallSegmentHit(Projectile projectile, WallSegment segment, Vector2 hitPosition) {
        // Mark segment as affected
        projectile.getAffectedPlayers().add(segment.getId());

        // Apply damage
        boolean destroyed = segment.takeDamage(projectile.getDamage());

        log.debug("Projectile {} hit wall segment {} for {} damage (destroyed: {})",
                projectile.getId(), segment.getId(), projectile.getDamage(), destroyed);

        // Create explosion for explosive projectiles
        if (isExplosiveProjectile(projectile)) {
            createExplosionEffect(hitPosition, projectile);
        }
    }

    /**
     * Check if projectile should pierce through targets
     */
    private boolean shouldProjectilePierce(Projectile projectile) {
        return projectile.getBulletEffects().contains(BulletEffect.PIERCING);
    }

    /**
     * Check if projectile creates explosions
     */
    private boolean isExplosiveProjectile(Projectile projectile) {
        Ordinance ordinance = projectile.getOrdinance();
        return ordinance == Ordinance.ROCKET ||
                ordinance == Ordinance.GRENADE ||
                ordinance == Ordinance.SHELL;
    }

    /**
     * Create explosion effect at hit position
     */
    private void createExplosionEffect(Vector2 position, Projectile projectile) {
        double explosionDamage = projectile.getDamage() * 0.5; // 50% of projectile damage
        double explosionRadius = projectile.getOrdinance().getSize() * 15; // Scale with projectile size

        ExplosionParams params = new ExplosionParams(
                explosionDamage,
                explosionRadius,
                projectile.getOwnerTeam()
        );

        explosionCreator.accept(position, params);
    }

    /**
     * Check if a unit can reach a position (for pathfinding/movement validation)
     */
    public boolean isPositionBlocked(Vector2 position, double unitRadius, double worldWidth, double worldHeight) {
        // Check world bounds
        double halfWidth = worldWidth / 2.0;
        double halfHeight = worldHeight / 2.0;
        if (Math.abs(position.x) > halfWidth - unitRadius ||
                Math.abs(position.y) > halfHeight - unitRadius) {
            return true;
        }

        // Check obstacles
        for (Obstacle obstacle : obstacles.values()) {
            if (circleIntersectsObstacle(position, unitRadius, obstacle)) {
                return true;
            }
        }

        // Check buildings (units can't move through buildings)
        for (Building building : buildings.values()) {
            if (!building.isActive()) {
                continue;
            }

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
    private boolean circleIntersectsObstacle(Vector2 position, double radius, Obstacle obstacle) {
        switch (obstacle.getShape()) {
            case CIRCLE -> {
                double distance = position.distance(obstacle.getPosition());
                return distance < radius + obstacle.getSize();
            }
            case RECTANGLE -> {
                // AABB vs circle collision
                double halfWidth = obstacle.getWidth() / 2.0;
                double halfHeight = obstacle.getHeight() / 2.0;

                return Math.abs(position.x - obstacle.getPosition().x) < halfWidth + radius &&
                        Math.abs(position.y - obstacle.getPosition().y) < halfHeight + radius;
            }
            case POLYGON -> {
                // Approximate polygon as circle
                double distance = position.distance(obstacle.getPosition());
                return distance < radius + obstacle.getSize();
            }
        }
        return false;
    }

    /**
     * Validate building placement location
     */
    public boolean isValidBuildLocation(
            Vector2 location,
            BuildingType buildingType,
            Map<Integer, ResourceDeposit> resourceDeposits,
            double worldWidth,
            double worldHeight
    ) {
        double size = buildingType.getSize();

        // Check if too close to other buildings
        for (Building building : buildings.values()) {
            double dist = location.distance(building.getPosition());
            double minDist = size + building.getBuildingType().getSize() + 20; // 20 unit buffer
            if (dist < minDist) {
                log.debug("Build location too close to existing building");
                return false;
            }
        }

        // Check if too close to obstacles
        for (Obstacle obstacle : obstacles.values()) {
            boolean tooClose = false;

            switch (obstacle.getShape()) {
                case RECTANGLE -> {
                    double halfWidth = obstacle.getWidth() / 2.0;
                    double halfHeight = obstacle.getHeight() / 2.0;
                    double buffer = size + 10;

                    tooClose = Math.abs(location.x - obstacle.getPosition().x) < halfWidth + buffer &&
                            Math.abs(location.y - obstacle.getPosition().y) < halfHeight + buffer;
                }
                case CIRCLE, POLYGON -> {
                    double dist = location.distance(obstacle.getPosition());
                    double minDist = size + obstacle.getSize() + 10;
                    tooClose = dist < minDist;
                }
            }

            if (tooClose) {
                log.debug("Build location too close to obstacle");
                return false;
            }
        }

        // Check if too close to resource deposits (except for refineries)
        if (buildingType != BuildingType.REFINERY) {
            for (ResourceDeposit deposit : resourceDeposits.values()) {
                double dist = location.distance(deposit.getPosition());
                double depositSize = 40.0; // Resource deposit radius
                double minDist = size + depositSize + 50; // Increased buffer to prevent overlap
                if (dist < minDist) {
                    log.debug("Build location too close to resource deposit (dist: {}, minDist: {})", dist, minDist);
                    return false;
                }
            }
        }

        // Check world bounds
        double halfWidth = worldWidth / 2.0;
        double halfHeight = worldHeight / 2.0;
        if (Math.abs(location.x) > halfWidth - size ||
                Math.abs(location.y) > halfHeight - size) {
            log.debug("Build location outside world bounds");
            return false;
        }

        return true;
    }

    /**
     * Check if a position is valid for spawning a unit
     * Used by buildings to find safe spawn locations for produced units
     */
    public boolean isValidSpawnPosition(
            Vector2 position,
            double unitSize,
            Map<Integer, ResourceDeposit> resourceDeposits,
            double worldWidth,
            double worldHeight
    ) {
        // Check world bounds
        double halfWidth = worldWidth / 2.0;
        double halfHeight = worldHeight / 2.0;
        if (Math.abs(position.x) > halfWidth - unitSize ||
                Math.abs(position.y) > halfHeight - unitSize) {
            return false; // Too close to world edge
        }

        // Check distance to obstacles
        for (Obstacle obstacle : obstacles.values()) {
            if (!obstacle.isActive()) {
                continue;
            }

            double distance = position.distance(obstacle.getPosition());
            double minDistance = unitSize + obstacle.getSize() + 5; // 5 unit buffer
            if (distance < minDistance) {
                return false; // Too close to obstacle
            }
        }

        // Check distance to buildings
        for (Building building : buildings.values()) {
            if (!building.isActive()) {
                continue;
            }

            double distance = position.distance(building.getPosition());
            double minDistance = unitSize + building.getBuildingType().getSize() + 5; // 5 unit buffer
            if (distance < minDistance) {
                return false; // Too close to building
            }
        }

        // Check distance to wall segments
        for (WallSegment segment : wallSegments.values()) {
            if (!segment.isActive()) {
                continue;
            }

            double distance = position.distance(segment.getPosition());
            double minDistance = unitSize + 15; // Wall segments are thin, use fixed buffer
            if (distance < minDistance) {
                return false; // Too close to wall
            }
        }

        // Check distance to other units (avoid spawning on top of existing units)
        for (Unit unit : units.values()) {
            if (!unit.isActive()) {
                continue;
            }

            double distance = position.distance(unit.getPosition());
            double minDistance = unitSize + unit.getUnitType().getSize() + 3; // 3 unit buffer
            if (distance < minDistance) {
                return false; // Too close to another unit
            }
        }

        // Check distance to resource deposits (don't spawn on resources)
        for (ResourceDeposit deposit : resourceDeposits.values()) {
            if (!deposit.isActive()) {
                continue;
            }

            double distance = position.distance(deposit.getPosition());
            double minDistance = unitSize + 40 + 5; // Resource deposit radius + buffer
            if (distance < minDistance) {
                return false; // Too close to resource deposit
            }
        }

        return true; // Position is valid
    }

    // ========================================
    // CollisionListener Implementation
    // ========================================

    /**
     * Called during broad-phase collision detection.
     * Allow broad-phase to proceed for all collisions.
     */
    @Override
    public boolean collision(BroadphaseCollisionData<Body, BodyFixture> collision) {
        // Allow all broad-phase collisions to proceed to narrow-phase
        return true;
    }

    /**
     * Called during narrow-phase collision detection.
     * Allow narrow-phase to proceed for all collisions.
     */
    @Override
    public boolean collision(NarrowphaseCollisionData<Body, BodyFixture> collision) {
        // Allow all narrow-phase collisions to proceed to manifold generation
        return true;
    }

    /**
     * Called when contact manifolds are generated (most fine-grained collision detection).
     * Return false to prevent collision response (physics reaction).
     * This handles all projectile collision logic including damage application.
     */
    @Override
    public boolean collision(ManifoldCollisionData<Body, BodyFixture> collision) {
        Body body1 = collision.getBody1();
        Body body2 = collision.getBody2();

        Object obj1 = body1.getUserData();
        Object obj2 = body2.getUserData();

        // Check if this is a projectile collision
        boolean body1IsProjectile = obj1 instanceof Projectile;
        boolean body2IsProjectile = obj2 instanceof Projectile;

        if (body1IsProjectile || body2IsProjectile) {
            Projectile projectile = body1IsProjectile ? (Projectile) obj1 : (Projectile) obj2;
            Object other = body1IsProjectile ? obj2 : obj1;
            
            if (!projectile.isActive()) {
                return false; // Ignore inactive projectiles
            }

            // Handle projectile-to-projectile collisions (never physically collide)
            if (other instanceof Projectile) {
                return false;
            }

            // Check for shield sensor collision FIRST
            if (other instanceof ShieldSensor shieldSensor) {
                Building shieldBuilding = shieldSensor.getBuilding();
                
                // Don't block friendly projectiles
                if (shieldBuilding.getTeamNumber() == projectile.getOwnerTeam()) {
                    return false; // Allow friendly projectiles through
                }
                
                // Only block if shield is active
                if (!shieldBuilding.isShieldActive()) {
                    return false; // Shield is down, allow projectile through
                }
                
                // Check if projectile originated inside this shield
                Vector2 projectileOrigin = projectile.getOrigin();
                boolean originatedInShield = shieldBuilding.isPositionInsideShield(projectileOrigin);
                
                if (originatedInShield) {
                    return false; // Allow projectiles fired from inside the shield to exit
                }
                
                // Shield blocks the projectile
                log.debug("Projectile {} blocked by shield from building {}", 
                         projectile.getId(), shieldBuilding.getId());
                
                // Apply reduced damage to the shield generator (10% of projectile damage)
                double reducedDamage = projectile.getDamage() * 0.10;
                shieldBuilding.takeDamage(reducedDamage);
                
                // Deactivate projectile
                projectile.setActive(false);
                
                return false; // Sensor collision, no physics response
            }

            // Get collision position for explosions
            Vector2 hitPosition = projectile.getBody().getWorldCenter();

            // Check if projectile hits a unit
            if (other instanceof Unit unit) {
                if (unit.getTeamNumber() == projectile.getOwnerTeam()) {
                    return false; // Pass through friendly units (no physics or damage)
                }
                
                if (!unit.isActive()) {
                    return false;
                }
                
                // Check if already hit this unit (for piercing projectiles)
                if (projectile.getAffectedPlayers().contains(unit.getId())) {
                    return false;
                }
                
                // Apply damage
                handleProjectileUnitHit(projectile, unit, hitPosition);
                
                // Deactivate if not piercing
                if (!shouldProjectilePierce(projectile)) {
                    projectile.setActive(false);
                }
                
                return false; // No physics collision (handled manually)
            }
            
            // Check if projectile hits a building
            if (other instanceof Building building) {
                if (building.getTeamNumber() == projectile.getOwnerTeam()) {
                    return false; // Pass through friendly buildings (no physics or damage)
                }
                
                if (!building.isActive()) {
                    return false;
                }
                
                // Check if already hit this building
                if (projectile.getAffectedPlayers().contains(building.getId())) {
                    return false;
                }
                
                // Apply damage
                handleProjectileBuildingHit(projectile, building, hitPosition);
                
                // Buildings always stop projectiles
                projectile.setActive(false);
                return false; // No physics collision
            }
            
            // Check if projectile hits a wall segment
            if (other instanceof WallSegment segment) {
                if (segment.getTeamNumber() == projectile.getOwnerTeam()) {
                    return false; // Pass through friendly walls (no physics or damage)
                }
                
                if (!segment.isActive()) {
                    return false;
                }
                
                // Check if already hit this segment
                if (projectile.getAffectedPlayers().contains(segment.getId())) {
                    return false;
                }
                
                // Apply damage
                handleProjectileWallSegmentHit(projectile, segment, hitPosition);
                
                // Walls always stop projectiles
                projectile.setActive(false);
                return false; // No physics collision
            }
            
            // Check if projectile hits an obstacle
            if (other instanceof Obstacle) {
                // Obstacles destroy projectiles
                log.debug("Projectile {} hit obstacle at ({}, {})",
                        projectile.getId(), hitPosition.x, hitPosition.y);
                
                // Create explosion if it's an explosive projectile
                if (isExplosiveProjectile(projectile)) {
                    createExplosionEffect(hitPosition, projectile);
                }
                
                projectile.setActive(false);
                return true; // Allow physics collision with obstacles
            }
        }

        // Allow collision response for all other collisions
        return true;
    }
}

