package com.fullsteam.model;

import com.fullsteam.model.component.ShieldComponent;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.BroadphaseCollisionData;
import org.dyn4j.world.ManifoldCollisionData;
import org.dyn4j.world.NarrowphaseCollisionData;
import org.dyn4j.world.World;
import org.dyn4j.world.listener.CollisionListener;

import java.util.Map;

/**
 * Centralized collision detection and handling for RTS game mode.
 * Handles projectile collisions with units, buildings, and obstacles.
 * Implements CollisionListener to control physics collision behavior.
 */
@Slf4j
public class RTSCollisionProcessor implements CollisionListener<Body, BodyFixture> {

    private final World<Body> world;
    private final GameEntities gameEntities;
    private final Map<Integer, Unit> units;
    private final Map<Integer, Building> buildings;
    private final Map<Integer, Obstacle> obstacles;
    private final Map<Integer, WallSegment> wallSegments;

    public RTSCollisionProcessor(GameEntities gameEntities) {
        this.world = gameEntities.getWorld();
        this.gameEntities = gameEntities;
        this.units = gameEntities.getUnits();
        this.buildings = gameEntities.getBuildings();
        this.obstacles = gameEntities.getObstacles();
        this.wallSegments = gameEntities.getWallSegments();
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

        // Create flak explosion for flak projectiles
        if (createsFlakExplosion(projectile)) {
            createFlakExplosionEffect(hitPosition, projectile);
        }

        // Create electric field for electric projectiles (area denial)
        if (hasElectricEffect(projectile)) {
            createElectricFieldEffect(hitPosition, projectile);
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

        // Create flak explosion for flak projectiles
        if (createsFlakExplosion(projectile)) {
            createFlakExplosionEffect(hitPosition, projectile);
        }

        // Create electric field for electric projectiles (area denial)
        if (hasElectricEffect(projectile)) {
            createElectricFieldEffect(hitPosition, projectile);
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

        // Create flak explosion for flak projectiles
        if (createsFlakExplosion(projectile)) {
            createFlakExplosionEffect(hitPosition, projectile);
        }

        // Create electric field for electric projectiles (area denial)
        if (hasElectricEffect(projectile)) {
            createElectricFieldEffect(hitPosition, projectile);
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
        // Check for FLAK bullet effect (handled separately)
        if (projectile.getBulletEffects().contains(BulletEffect.FLAK)) {
            return false; // FLAK is handled by createFlakExplosion()
        }
        
        Ordinance ordinance = projectile.getOrdinance();
        return ordinance == Ordinance.ROCKET ||
                ordinance == Ordinance.GRENADE ||
                ordinance == Ordinance.SHELL;
    }

    /**
     * Check if projectile creates flak explosions (anti-air bursts)
     */
    private boolean createsFlakExplosion(Projectile projectile) {
        return projectile.getBulletEffects().contains(BulletEffect.FLAK);
    }

    /**
     * Create explosion effect at hit position.
     * 
     * Note: For anti-aircraft weapons, you can create FLAK_EXPLOSION effects instead:
     * <pre>
     * // Example: Flak cannon burst (damages all elevations)
     * FieldEffect flak = new FieldEffect(
     *     projectile.getOwnerId(),
     *     FieldEffectType.FLAK_EXPLOSION,
     *     position,
     *     projectile.getOrdinance().getSize() * 12,  // Smaller radius than ground explosions
     *     projectile.getDamage() * 0.6,              // Flak damage
     *     FieldEffectType.FLAK_EXPLOSION.getDefaultDuration(),
     *     projectile.getOwnerTeam()
     * );
     * gameEntities.add(flak);
     * </pre>
     */
    private void createExplosionEffect(Vector2 position, Projectile projectile) {
        FieldEffect effect = new FieldEffect(
                projectile.getOwnerId(),
                FieldEffectType.EXPLOSION,
                position,
                projectile.getOrdinance().getSize() * 15,
                projectile.getDamage() * 0.5,
                FieldEffectType.EXPLOSION.getDefaultDuration(),
                projectile.getOwnerTeam()
        );
        gameEntities.add(effect);
    }

    /**
     * Create flak explosion effect at hit position (anti-air burst).
     * Flak explosions only damage aircraft (LOW and HIGH elevations).
     */
    private void createFlakExplosionEffect(Vector2 position, Projectile projectile) {
        FieldEffect effect = new FieldEffect(
                projectile.getOwnerId(),
                FieldEffectType.FLAK_EXPLOSION,
                position,
                projectile.getOrdinance().getSize() * 12,  // Smaller radius than ground explosions
                projectile.getDamage() * 0.6,              // 60% of projectile damage as AOE
                FieldEffectType.FLAK_EXPLOSION.getDefaultDuration(),
                projectile.getOwnerTeam()
        );
        gameEntities.add(effect);
    }

    /**
     * Check if projectile has electric effect
     */
    private boolean hasElectricEffect(AbstractOrdinance ordinance) {
        return ordinance.getBulletEffects().contains(BulletEffect.ELECTRIC);
    }

    /**
     * Create electric field effect at hit position (area denial)
     */
    private void createElectricFieldEffect(Vector2 position, AbstractOrdinance ordinance) {
        FieldEffect effect = new FieldEffect(
                ordinance.getOwnerId(),
                FieldEffectType.ELECTRIC,
                position,
                ordinance.getOrdinanceType().getSize() * 12,
                ordinance.getDamage() * 0.3,
                FieldEffectType.ELECTRIC.getDefaultDuration(),
                ordinance.getOwnerTeam()
        );
        gameEntities.add(effect);
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
        
        // Check proximity requirements (e.g., Hangar must be near Airfield)
        BuildingType proximityRequirement = buildingType.getProximityRequirement();
        if (proximityRequirement != null) {
            double requiredRange = buildingType.getProximityRange();
            boolean foundNearbyRequirement = false;
            
            for (Building building : buildings.values()) {
                if (building.getBuildingType() == proximityRequirement && building.isActive()) {
                    double dist = location.distance(building.getPosition());
                    if (dist <= requiredRange) {
                        foundNearbyRequirement = true;
                        break;
                    }
                }
            }
            
            if (!foundNearbyRequirement) {
                log.debug("Build location does not meet proximity requirement: {} must be within {} pixels of {}",
                        buildingType, requiredRange, proximityRequirement);
                return false;
            }
        }

        return true;
    }
    
    /**
     * Check if a support building can support another dependent building
     * For example, check if an Airfield has capacity for another Hangar
     * 
     * @param location Location where the new building will be placed
     * @param buildingType Type of building being placed
     * @param playerId Owner of the building
     * @return true if support capacity is available, false if at capacity
     */
    public boolean hasSupportCapacity(Vector2 location, BuildingType buildingType, int playerId) {
        BuildingType proximityRequirement = buildingType.getProximityRequirement();
        if (proximityRequirement == null) {
            return true; // No proximity requirement = no support capacity check needed
        }
        
        int supportCapacity = proximityRequirement.getSupportCapacity();
        if (supportCapacity == 0) {
            return true; // No capacity limit
        }
        
        double requiredRange = buildingType.getProximityRange();
        
        // Find the nearest support building within range
        Building nearestSupport = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (Building building : buildings.values()) {
            if (building.getBuildingType() == proximityRequirement && 
                building.isActive() && 
                building.getOwnerId() == playerId) { // Must belong to same player
                double dist = location.distance(building.getPosition());
                if (dist <= requiredRange && dist < nearestDist) {
                    nearestSupport = building;
                    nearestDist = dist;
                }
            }
        }
        
        if (nearestSupport == null) {
            return false; // No support building nearby
        }
        
        // Count how many dependent buildings this support building already has
        int currentDependents = 0;
        for (Building building : buildings.values()) {
            if (building.getBuildingType() == buildingType && 
                building.isActive() && 
                building.getOwnerId() == playerId) {
                double dist = building.getPosition().distance(nearestSupport.getPosition());
                if (dist <= requiredRange) {
                    currentDependents++;
                }
            }
        }
        
        boolean hasCapacity = currentDependents < supportCapacity;
        if (!hasCapacity) {
            log.debug("Support building {} at capacity: {}/{} dependent buildings",
                    proximityRequirement, currentDependents, supportCapacity);
        }
        
        return hasCapacity;
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
        
        // Check if either object is null (this can happen if body wasn't properly initialized)
        if (obj1 == null || obj2 == null) {
            log.warn("Collision detected with null user data: obj1={}, obj2={}", obj1, obj2);
            return false;
        }

        // Check if this is a projectile collision
        boolean body1IsProjectile = obj1 instanceof Projectile;
        boolean body2IsProjectile = obj2 instanceof Projectile;

        // Check if this is a beam collision
        boolean body1IsBeam = obj1 instanceof Beam;
        boolean body2IsBeam = obj2 instanceof Beam;

        // Check if this is a field effect collision
        boolean body1IsFieldEffect = obj1 instanceof FieldEffect;
        boolean body2IsFieldEffect = obj2 instanceof FieldEffect;

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
                ShieldComponent shieldComponent = shieldBuilding.getComponent(ShieldComponent.class).orElseThrow();

                // Check if projectile originated inside this shield
                Vector2 projectileOrigin = projectile.getOrigin();
                boolean originatedInShield = shieldComponent.isPositionInside(projectileOrigin);

                if (originatedInShield) {
                    return false; // Allow projectiles fired from inside the shield to exit
                }

                // Shield blocks the projectile
                if (shieldBuilding.getTeamNumber() == projectile.getOwnerTeam()) {
                    // the projectile is still terminated by the shield, just no damage
                    projectile.setActive(false);
                    return false;
                }

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
                
                // Check if projectile can target this unit's elevation
                if (!projectile.getElevationTargeting().canTarget(unit.getUnitType().getElevation())) {
                    return false; // Projectile passes through units at untargetable elevations
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

                // Create flak explosion for flak projectiles
                if (createsFlakExplosion(projectile)) {
                    createFlakExplosionEffect(hitPosition, projectile);
                }

                // Create electric field for electric projectiles (area denial)
                if (hasElectricEffect(projectile)) {
                    createElectricFieldEffect(hitPosition, projectile);
                }

                projectile.setActive(false);
                return true; // Allow physics collision with obstacles
            }
        }

        // Handle beam collisions (beams are sensors, so they detect but don't physically collide)
        if (body1IsBeam || body2IsBeam) {
            Beam beam = body1IsBeam ? (Beam) obj1 : (Beam) obj2;
            Object other = body1IsBeam ? obj2 : obj1;
            
            String otherType = other != null ? other.getClass().getSimpleName() : "null";
            if (other instanceof GameEntity ge) {
                otherType += " (ID=" + ge.getId() + ")";
            }

            if (!beam.isActive()) {
                return false; // Ignore inactive beams
            }

            // Beams pass through other beams and projectiles
            if (other instanceof Beam || other instanceof Projectile) {
                return false;
            }

            // Beams pass through field effects
            if (other instanceof FieldEffect) {
                return false;
            }

            // Calculate hit position (beam center)
            Vector2 hitPosition = beam.getEndPosition();

            // Check if beam hits a unit
            if (other instanceof Unit unit) {
                // Skip friendly fire
                if (unit.getTeamNumber() == beam.getOwnerTeam()) {
                    return false;
                }

                if (!unit.isActive()) {
                    return false;
                }
                
                // Check if beam can target this unit's elevation
                if (!beam.getElevationTargeting().canTarget(unit.getUnitType().getElevation())) {
                    return false; // Beam passes through units at untargetable elevations
                }

                // Check if already damaged this unit
                if (beam.getAffectedPlayers().contains(unit.getId())) {
                    return false;
                }

                // Apply damage
                unit.takeDamage(beam.getDamage());
                // Mark unit as affected
                beam.getAffectedPlayers().add(unit.getId());
                return false; // No physics collision (sensor)
            }

            // Check if beam hits a building
            if (other instanceof Building building) {
                // Skip friendly fire
                if (building.getTeamNumber() == beam.getOwnerTeam()) {
                    return false;
                }

                if (!building.isActive()) {
                    return false;
                }

                // Check if already damaged this building
                if (beam.getAffectedPlayers().contains(building.getId())) {
                    return false;
                }

                // Apply damage
                handleBeamBuildingHit(beam, building, hitPosition);
                return false; // No physics collision (sensor)
            }

            // Check if beam hits a wall segment
            if (other instanceof WallSegment segment) {
                // Skip friendly fire
                if (segment.getTeamNumber() == beam.getOwnerTeam()) {
                    return false;
                }

                if (!segment.isActive()) {
                    return false;
                }

                // Check if already damaged this segment
                if (beam.getAffectedPlayers().contains(segment.getId())) {
                    return false;
                }

                // Apply damage
                handleBeamWallSegmentHit(beam, segment, hitPosition);
                return false; // No physics collision (sensor)
            }

            // Check if beam hits an obstacle
            if (other instanceof Obstacle obstacle) {
                if (!obstacle.isActive()) {
                    return false;
                }

                // Check if already processed this obstacle
                if (beam.getAffectedPlayers().contains(obstacle.getId())) {
                    return false;
                }

                // Only non-destructible obstacles can be damaged by beams
                if (!obstacle.isDestructible()) {
                    obstacle.takeDamage(beam.getDamage());
                }

                // Mark as affected
                beam.getAffectedPlayers().add(obstacle.getId());

                log.debug("Beam {} hit obstacle at ({}, {})",
                        beam.getId(), hitPosition.x, hitPosition.y);

                return false; // No physics collision (sensor)
            }
        }

        // Handle field effect collisions (explosions, electric fields, etc.)
        if (body1IsFieldEffect || body2IsFieldEffect) {
            FieldEffect fieldEffect = body1IsFieldEffect ? (FieldEffect) obj1 : (FieldEffect) obj2;
            Object other = body1IsFieldEffect ? obj2 : obj1;

            if (!fieldEffect.isActive()) {
                return false; // Ignore inactive field effects
            }

            // Field effects pass through other field effects, beams, and projectiles
            if (other instanceof FieldEffect || other instanceof Projectile) {
                return false;
            }

            // Field effects pass through obstacles and walls (they're area effects)
            if (other instanceof Obstacle || other instanceof WallSegment) {
                return false;
            }

            // Check if field effect hits a unit
            if (other instanceof Unit unit) {
                if (fieldEffect.canAffect(unit)) {
                    handleFieldEffectUnitHit(fieldEffect, unit);
                }
                return false; // No physics collision (sensor)
            }

            // Check if field effect hits a building
            if (other instanceof Building building) {
                // Check if we can damage this building (cooldown check)
                if (fieldEffect.canAffect(building)) {
                    handleFieldEffectBuildingHit(fieldEffect, building);
                }
                // Apply damage based on distance from center
                return false;
            }
        }

        // Allow collision response for all other collisions
        return true;
    }

    /**
     * Handle a beam hitting a building
     */
    private void handleBeamBuildingHit(Beam beam, Building building, Vector2 hitPosition) {
        // Mark building as affected
        beam.getAffectedPlayers().add(building.getId());

        // Apply damage
        building.takeDamage(beam.getDamage());

        log.debug("Beam {} hit building {} for {} damage",
                beam.getId(), building.getId(), beam.getDamage());
    }

    /**
     * Handle a beam hitting a wall segment
     */
    private void handleBeamWallSegmentHit(Beam beam, WallSegment segment, Vector2 hitPosition) {
        // Mark segment as affected
        beam.getAffectedPlayers().add(segment.getId());

        // Apply damage
        segment.takeDamage(beam.getDamage());

        log.debug("Beam {} hit wall segment {} for {} damage",
                beam.getId(), segment.getId(), beam.getDamage());
    }

    /**
     * Handle a field effect hitting a unit
     * Field effects apply damage over time with distance-based intensity
     */
    private void handleFieldEffectUnitHit(FieldEffect fieldEffect, Unit unit) {
        // Get delta time from physics world
        double deltaTime = world.getTimeStep().getDeltaTime();

        // Calculate damage based on distance from field effect center
        // For instantaneous effects (EXPLOSION), damage is full amount
        // For DOT effects (ELECTRIC, FIRE, POISON), damage is DPS * deltaTime
        double baseDamage = fieldEffect.getDamageAtPosition(unit.getPosition());
        double damage = fieldEffect.getType().isInstantaneous() ? baseDamage : baseDamage * deltaTime;

        // Apply damage
        unit.takeDamage(damage);

        // Mark as affected (for instantaneous effects, prevents re-damage)
        fieldEffect.markAsAffected(unit);

        log.debug("Field effect {} ({}) damaged unit {} for {} damage (deltaTime: {})",
                fieldEffect.getId(), fieldEffect.getType(), unit.getId(), damage, deltaTime);
    }

    /**
     * Handle a field effect hitting a building
     * Field effects apply damage over time with distance-based intensity
     */
    private void handleFieldEffectBuildingHit(FieldEffect fieldEffect, Building building) {
        // Get delta time from physics world
        double deltaTime = world.getTimeStep().getDeltaTime();

        // Calculate damage based on distance from field effect center
        // For instantaneous effects (EXPLOSION), damage is full amount
        // For DOT effects (ELECTRIC, FIRE, POISON), damage is DPS * deltaTime
        double baseDamage = fieldEffect.getDamageAtPosition(building.getPosition());
        double damage = fieldEffect.getType().isInstantaneous() ? baseDamage : baseDamage * deltaTime;

        // Apply damage
        building.takeDamage(damage);

        // Mark as affected (for instantaneous effects, prevents re-damage)
        fieldEffect.markAsAffected(building);

        log.debug("Field effect {} ({}) damaged building {} for {} damage (deltaTime: {})",
                fieldEffect.getId(), fieldEffect.getType(), building.getId(), damage, deltaTime);
    }
}

