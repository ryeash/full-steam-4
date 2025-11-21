package com.fullsteam.model;

import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Centralized collision detection and handling for RTS game mode.
 * Handles projectile collisions with units, buildings, and obstacles.
 */
@Slf4j
public class RTSCollisionProcessor {
    
    private final Map<Integer, Unit> units;
    private final Map<Integer, Building> buildings;
    private final Map<Integer, Obstacle> obstacles;
    private final Map<Integer, WallSegment> wallSegments;
    private final BiConsumer<Vector2, ExplosionParams> explosionCreator;
    
    /**
     * Parameters for creating explosions
     */
    public record ExplosionParams(double damage, double radius, int ownerTeam) {}
    
    public RTSCollisionProcessor(
            Map<Integer, Unit> units,
            Map<Integer, Building> buildings,
            Map<Integer, Obstacle> obstacles,
            Map<Integer, WallSegment> wallSegments,
            BiConsumer<Vector2, ExplosionParams> explosionCreator
    ) {
        this.units = units;
        this.buildings = buildings;
        this.obstacles = obstacles;
        this.wallSegments = wallSegments;
        this.explosionCreator = explosionCreator;
    }
    
    /**
     * Process all projectile collisions for this frame
     */
    public void processProjectileCollisions(Map<Integer, Projectile> projectiles) {
        for (Projectile projectile : projectiles.values()) {
            if (!projectile.isActive()) {
                continue;
            }
            
            // Check shield collisions FIRST (shields destroy projectiles before other collisions)
            if (checkProjectileShieldCollisions(projectile)) {
                // Projectile was destroyed by shield
                continue;
            }
            
            // Check unit collisions
            if (checkProjectileUnitCollisions(projectile)) {
                // Projectile may have been deactivated
                if (!projectile.isActive()) {
                    continue;
                }
            }
            
            // Check building collisions
            if (checkProjectileBuildingCollisions(projectile)) {
                // Projectile may have been deactivated
                if (!projectile.isActive()) {
                    continue;
                }
            }
            
            // Check wall segment collisions (wall segments take damage like buildings)
            if (checkProjectileWallSegmentCollisions(projectile)) {
                // Projectile may have been deactivated
                if (!projectile.isActive()) {
                    continue;
                }
            }
            
            // Check obstacle collisions (projectiles should be destroyed by obstacles)
            checkProjectileObstacleCollisions(projectile);
        }
    }
    
    /**
     * Check projectile collisions with units
     * @return true if any collision occurred
     */
    private boolean checkProjectileUnitCollisions(Projectile projectile) {
        Vector2 projPos = projectile.getPosition();
        double projRadius = projectile.getOrdinance().getSize();
        boolean hitSomething = false;
        
        for (Unit unit : units.values()) {
            if (!unit.isActive()) {
                continue;
            }
            
            // Skip friendly fire - projectiles pass through friendly units
            if (unit.getTeamNumber() == projectile.getOwnerTeam()) {
                continue;
            }
            
            // Check if already hit this unit (important for piercing projectiles)
            if (projectile.getAffectedPlayers().contains(unit.getId())) {
                continue;
            }
            
            // Circle-circle collision detection
            double distance = projPos.distance(unit.getPosition());
            double collisionThreshold = projRadius + unit.getUnitType().getSize();
            
            if (distance <= collisionThreshold) {
                // Hit detected!
                handleProjectileUnitHit(projectile, unit, projPos);
                hitSomething = true;
                
                // Check if projectile should be deactivated
                if (!shouldProjectilePierce(projectile)) {
                    projectile.setActive(false);
                    break; // Stop checking collisions for this projectile
                }
            }
        }
        
        return hitSomething;
    }
    
    /**
     * Check projectile collisions with buildings
     * @return true if any collision occurred
     */
    private boolean checkProjectileBuildingCollisions(Projectile projectile) {
        Vector2 projPos = projectile.getPosition();
        double projRadius = projectile.getOrdinance().getSize();
        boolean hitSomething = false;
        
        for (Building building : buildings.values()) {
            if (!building.isActive()) {
                continue;
            }
            
            // Skip friendly fire - projectiles pass through friendly buildings
            if (building.getTeamNumber() == projectile.getOwnerTeam()) {
                continue;
            }
            
            // Check if already hit this building (for piercing projectiles)
            if (projectile.getAffectedPlayers().contains(building.getId())) {
                continue;
            }
            
            // Circle-square collision detection (approximate buildings as circles for simplicity)
            double distance = projPos.distance(building.getPosition());
            double collisionThreshold = projRadius + building.getBuildingType().getSize();
            
            if (distance <= collisionThreshold) {
                // Hit detected!
                handleProjectileBuildingHit(projectile, building, projPos);
                hitSomething = true;
                
                // Buildings always stop projectiles (even piercing ones)
                projectile.setActive(false);
                break;
            }
        }
        
        return hitSomething;
    }
    
    /**
     * Check projectile collisions with wall segments
     * @return true if any collision occurred
     */
    private boolean checkProjectileWallSegmentCollisions(Projectile projectile) {
        Vector2 projPos = projectile.getPosition();
        double projRadius = projectile.getOrdinance().getSize();
        boolean hitSomething = false;
        
        for (WallSegment segment : wallSegments.values()) {
            if (!segment.isActive()) {
                continue;
            }
            
            // Skip friendly fire - projectiles pass through friendly wall segments
            if (segment.getTeamNumber() == projectile.getOwnerTeam()) {
                continue;
            }
            
            // Check if already hit this segment (for piercing projectiles)
            if (projectile.getAffectedPlayers().contains(segment.getId())) {
                continue;
            }
            
            // Rectangle collision detection for wall segments
            // Wall segments are rotated rectangles, so we need to check distance to the line
            Vector2 segmentPos = segment.getPosition();
            double distance = projPos.distance(segmentPos);
            
            // Approximate collision: if projectile is within segment length/2 + thickness/2 + projectile radius
            double collisionThreshold = projRadius + segment.getLength() / 2.0 + 10; // 10 for some buffer
            
            if (distance <= collisionThreshold) {
                // More precise check: distance to the wall segment's center line
                // For simplicity, we'll use the approximate check above
                // Hit detected!
                handleProjectileWallSegmentHit(projectile, segment, projPos);
                hitSomething = true;
                
                // Wall segments always stop projectiles (like buildings)
                projectile.setActive(false);
                break;
            }
        }
        
        return hitSomething;
    }
    
    /**
     * Check projectile collisions with obstacles
     * @return true if collision occurred
     */
    private boolean checkProjectileObstacleCollisions(Projectile projectile) {
        Vector2 projPos = projectile.getPosition();
        double projRadius = projectile.getOrdinance().getSize();
        
        for (Obstacle obstacle : obstacles.values()) {
            boolean collision = false;
            
            // Different collision detection based on obstacle shape
            switch (obstacle.getShape()) {
                case CIRCLE -> {
                    double distance = projPos.distance(obstacle.getPosition());
                    collision = distance <= projRadius + obstacle.getSize();
                }
                case RECTANGLE -> {
                    // AABB collision
                    double halfWidth = obstacle.getWidth() / 2.0;
                    double halfHeight = obstacle.getHeight() / 2.0;
                    
                    collision = Math.abs(projPos.x - obstacle.getPosition().x) < halfWidth + projRadius &&
                               Math.abs(projPos.y - obstacle.getPosition().y) < halfHeight + projRadius;
                }
                case POLYGON -> {
                    // Approximate polygon as circle for projectile collision
                    double distance = projPos.distance(obstacle.getPosition());
                    collision = distance <= projRadius + obstacle.getSize();
                }
            }
            
            if (collision) {
                // Obstacles destroy projectiles (no damage to obstacle)
                log.debug("Projectile {} hit obstacle {} at ({}, {})", 
                         projectile.getId(), obstacle.getId(), projPos.x, projPos.y);
                
                // Create explosion if it's an explosive projectile
                if (isExplosiveProjectile(projectile)) {
                    createExplosionEffect(projPos, projectile);
                }
                
                projectile.setActive(false);
                return true;
            }
        }
        
        return false;
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
    
    /**
     * Check if a projectile collides with any active shields
     * @return true if projectile was destroyed by a shield
     */
    private boolean checkProjectileShieldCollisions(Projectile projectile) {
        Vector2 projectilePos = projectile.getPosition();
        Vector2 projectileOrigin = projectile.getOrigin();
        
        // Check all buildings for active shields
        for (Building building : buildings.values()) {
            if (!building.isActive() || building.getBuildingType() != BuildingType.SHIELD_GENERATOR) {
                continue;
            }
            
            if (!building.isShieldActive()) {
                continue;
            }
            
            // Don't destroy projectiles from the same team
            if (building.getTeamNumber() == projectile.getOwnerTeam()) {
                continue;
            }
            
            // Check if projectile is currently inside the shield
            boolean currentlyInShield = building.isPositionInsideShield(projectilePos);
            
            // Check if projectile originated inside this shield
            boolean originatedInShield = building.isPositionInsideShield(projectileOrigin);
            
            // Destroy projectile if:
            // 1. It's currently inside the shield AND
            // 2. It did NOT originate inside the shield (incoming projectile)
            if (currentlyInShield && !originatedInShield) {
                // Apply reduced damage to the shield generator (10% of projectile damage)
                double reducedDamage = projectile.getDamage() * 0.10;
                building.takeDamage(reducedDamage);
                
                log.debug("Projectile {} blocked by shield from building {} (dealt {} reduced damage)", 
                         projectile.getId(), building.getId(), reducedDamage);
                projectile.setActive(false);
                return true;
            }
        }
        
        return false;
    }
}

