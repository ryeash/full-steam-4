package com.fullsteam.model;

import com.fullsteam.model.command.AttackBuildingCommand;
import com.fullsteam.model.command.AttackGroundCommand;
import com.fullsteam.model.command.AttackUnitCommand;
import com.fullsteam.model.command.AttackWallSegmentCommand;
import com.fullsteam.model.command.ConstructCommand;
import com.fullsteam.model.command.GarrisonBunkerCommand;
import com.fullsteam.model.command.HarvestCommand;
import com.fullsteam.model.command.IdleCommand;
import com.fullsteam.model.command.MineCommand;
import com.fullsteam.model.command.MoveCommand;
import com.fullsteam.model.command.UnitCommand;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a unit in the RTS game (worker, infantry, vehicle, etc.)
 */
@Slf4j
@Getter
@Setter
public class Unit extends GameEntity {
    private final UnitType unitType;
    private final int ownerId; // Player who owns this unit
    private final int teamNumber;

    /**
     * -- GETTER --
     * Get the current command (for debugging/inspection)
     */
    // Command Pattern - all unit orders managed through commands
    private UnitCommand currentCommand = null;

    // Movement
    private double movementSpeed;

    // LEGACY fields kept for backward compatibility with existing logic
    // TODO: These can be removed once all subsystems fully use commands
    private Vector2 targetPosition = null; // Used by legacy pathfinding
    private Unit targetUnit = null; // Used by legacy combat
    private Building targetBuilding = null; // Used by legacy combat/harvesting
    private WallSegment targetWallSegment = null; // Used by legacy combat
    private ResourceDeposit targetResourceDeposit = null; // Used by harvesting
    private boolean isMoving = false; // Used by legacy movement
    private boolean hasPlayerOrder = false; // Used by AI scanning
    private boolean isAttackMoving = false; // Used by AI scanning
    private boolean attackingGround = false; // Used by legacy combat
    private Vector2 groundAttackTarget = null; // Used by legacy combat

    // Pathfinding
    private List<Vector2> currentPath = new ArrayList<>();
    private int currentPathIndex = 0;

    // AI stance
    private AIStance aiStance = AIStance.DEFENSIVE; // Default stance
    private Vector2 homePosition = null; // For defensive stance
    private static final double DEFENSIVE_LEASH_RANGE = 300.0; // Max distance from home for defensive units

    // Special ability system
    private boolean specialAbilityActive = false; // For toggle abilities (deploy, stealth, etc.)
    private long lastSpecialAbilityTime = 0; // For cooldown tracking

    // Multi-turret system (for Crawler when deployed)
    private List<UnitTurret> turrets = new ArrayList<>();

    // Combat
    private long lastAttackTime = 0;
    private double attackRange;
    private double damage;
    private double attackRate;

    // Resource harvesting (for workers)
    private double carriedResources = 0;
    private static final double MAX_CARRIED_RESOURCES = 100.0;
    private boolean isHarvesting = false;
    private boolean isReturningResources = false;
    private Building targetRefinery = null;

    // Building construction (for workers)
    private Building constructionTarget = null;
    private boolean isConstructing = false;

    // Obstacle mining (for miners)
    private Obstacle targetObstacle = null;
    private boolean isMining = false;
    private boolean isReturningForRepair = false;
    private double pickaxeDurability = 100.0; // Pickaxe condition (0-100%)
    private static final double MAX_PICKAXE_DURABILITY = 100.0;
    private static final double PICKAXE_WEAR_RATE = 5.0; // Durability lost per second of mining
    private static final double MINING_DAMAGE_RATE = 15.0; // Damage dealt to obstacle per second

    // Selection state
    private boolean selected = false;
    private boolean garrisoned = false; // True if unit is inside a building (bunker)

    public Unit(int id, UnitType unitType, double x, double y, int ownerId, int teamNumber) {
        super(id, createUnitBody(x, y, unitType), unitType.getMaxHealth());
        this.unitType = unitType;
        this.ownerId = ownerId;
        this.teamNumber = teamNumber;
        this.movementSpeed = unitType.getMovementSpeed();
        this.attackRange = unitType.getAttackRange();
        this.damage = unitType.getDamage();
        this.attackRate = unitType.getAttackRate();
    }

    private static Body createUnitBody(double x, double y, UnitType unitType) {
        Body body = new Body();

        // Create custom physics fixture for this unit type
        Convex shape = unitType.createPhysicsFixture();
        BodyFixture fixture = body.addFixture(shape);

        // Configure fixture properties
        fixture.setFriction(0.1);      // Low friction for smooth movement
        fixture.setRestitution(0.0);   // No bounce
        fixture.setSensor(false);      // Solid collision (not a sensor)

        // Rotate polygons to face right by default (positive X direction)
        // Circles don't need rotation, rectangles are already oriented correctly
        if (shape instanceof Polygon) {
            body.rotate(-Math.PI / 2.0);
        }

        // Set mass based on unit type (affects acceleration)
        body.setMass(MassType.NORMAL);

        // Set position
        body.getTransform().setTranslation(x, y);

        // Damping for natural slowdown (like air resistance/friction)
        body.setLinearDamping(4.0);   // Increased from 2.0 for less drifting
        body.setAngularDamping(10.0); // Keep high to prevent spinning

        return body;
    }

    @Override
    public void update(double deltaTime) {
        super.update(deltaTime);

        if (!active) {
            return;
        }

        // Update command (checks if command is complete and does work)
        if (currentCommand != null) {
            boolean stillActive = currentCommand.update(deltaTime);
            if (!stillActive) {
                // Command completed, switch to idle
                currentCommand = new IdleCommand(this);
            }
        }

        // Commands now handle all the work!
        // The old harvestResources(), constructBuilding(), mineObstacle() methods
        // are now called by their respective commands
    }

    /**
     * Update movement with steering behaviors (called from RTSGameManager with nearby units)
     */
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        // NEW: Delegate to command if present
        if (currentCommand != null) {
            currentCommand.updateMovement(deltaTime, nearbyUnits);
        } else if (isMoving && targetPosition != null) {
            // LEGACY: Fallback to old movement logic
            moveTowardsTarget(deltaTime, nearbyUnits);
        }
    }

    // ===== STEERING BEHAVIORS =====

    /**
     * Calculate seek steering force towards a target position
     */
    private Vector2 calculateSeek(Vector2 target) {
        Vector2 currentPos = getPosition();
        Vector2 desired = target.copy().subtract(currentPos);
        double distance = desired.getMagnitude();

        if (distance < 1.0) {
            return new Vector2(0, 0);
        }

        return desired.getNormalized().multiply(movementSpeed);
    }

    /**
     * Calculate arrival steering force (slow down as approaching target)
     */
    private Vector2 calculateArrival(Vector2 target, double slowingRadius) {
        Vector2 currentPos = getPosition();
        Vector2 desired = target.copy().subtract(currentPos);
        double distance = desired.getMagnitude();

        // Stop applying force when very close to prevent oscillation
        if (distance < 3.0) {
            return new Vector2(0, 0);
        }

        desired.normalize();

        // Slow down if within slowing radius
        if (distance < slowingRadius) {
            // Quadratic falloff for smoother deceleration
            double ratio = distance / slowingRadius;
            double speed = movementSpeed * ratio * ratio; // Quadratic instead of linear
            desired.multiply(speed);
        } else {
            desired.multiply(movementSpeed);
        }

        return desired;
    }

    /**
     * Calculate separation steering force (avoid crowding with nearby units)
     */
    private Vector2 calculateSeparation(List<Unit> nearbyUnits, double separationRadius) {
        Vector2 steeringForce = new Vector2(0, 0);
        int count = 0;
        Vector2 currentPos = getPosition();

        for (Unit other : nearbyUnits) {
            if (other == this || !other.isActive()) {
                continue;
            }

            double distance = currentPos.distance(other.getPosition());
            if (distance < separationRadius && distance > 0.1) {
                // Push away from nearby unit
                Vector2 away = currentPos.copy().subtract(other.getPosition());
                away.normalize();
                // Stronger force when closer
                away.multiply(1.0 / distance);
                steeringForce.add(away);
                count++;
            }
        }

        if (count > 0) {
            steeringForce.multiply(1.0 / count); // Average
        }

        return steeringForce;
    }

    /**
     * Apply steering forces to move towards target using physics
     * Made public for Command Pattern access
     */
    public void applySteeringForces(Vector2 target, List<Unit> nearbyUnits, double deltaTime) {
        Vector2 currentPos = getPosition();
        double distanceToTarget = currentPos.distance(target);

        // Calculate steering forces
        Vector2 separationForce = new Vector2(0, 0);

        // Use arrival behavior when close to final destination
        Vector2 seekForce;
        boolean isNearDestination = distanceToTarget < 100.0;
        if (isNearDestination) {
            seekForce = calculateArrival(target, 100.0);
        } else {
            seekForce = calculateSeek(target);
        }

        // Separation from nearby units
        if (nearbyUnits != null && !nearbyUnits.isEmpty()) {
            separationForce = calculateSeparation(nearbyUnits, unitType.getSize() * 4.0);
        }

        // Apply force to physics body
        body.applyForce(seekForce.multiply(body.getMass().getMass() * 60.0));
        body.applyForce(separationForce.multiply(body.getMass().getMass() * 80.0));

        // Clamp velocity to max speed
        Vector2 velocity = body.getLinearVelocity();
        double speed = velocity.getMagnitude();
        if (speed > movementSpeed) {
            body.setLinearVelocity(velocity.getNormalized().multiply(movementSpeed));
        }

        // Update rotation to face movement direction
        if (speed > 1.0) {
            setRotation(Math.atan2(velocity.y, velocity.x));
        }
    }

    /**
     * Move unit towards target position using pathfinding and steering behaviors
     */
    private void moveTowardsTarget(double deltaTime, List<Unit> nearbyUnits) {
        Vector2 currentPos = getPosition();

        // If we have a path, follow it
        if (!currentPath.isEmpty() && currentPathIndex < currentPath.size()) {
            Vector2 waypointTarget = currentPath.get(currentPathIndex);
            double distance = currentPos.distance(waypointTarget);

            // Use larger threshold for waypoint advancement
            double waypointThreshold = 25.0;

            // Check if we've reached the current waypoint
            if (distance < waypointThreshold) {
                currentPathIndex++;

                // Check if we've completed the path
                if (currentPathIndex >= currentPath.size()) {
                    // Stop when very close to final destination
                    if (distance < 10.0) {
                        isMoving = false;
                        hasPlayerOrder = false; // Order completed
                        // Actively stop to prevent drift
                        body.setLinearVelocity(0, 0);
                        currentPath.clear();
                        currentPathIndex = 0;
                        return;
                    }
                } else {
                    // Move to next waypoint
                    waypointTarget = currentPath.get(currentPathIndex);
                    distance = currentPos.distance(waypointTarget);
                }
            }

            // Apply steering forces to move towards waypoint
            applySteeringForces(waypointTarget, nearbyUnits, deltaTime);

        } else if (targetPosition != null) {
            // No path, simple direct movement (fallback)
            double distance = currentPos.distance(targetPosition);

            // Stop when very close to target
            if (distance < 10.0) {
                isMoving = false;
                hasPlayerOrder = false; // Order completed
                // Actively stop to prevent drift
                body.setLinearVelocity(0, 0);
                return;
            }

            // Apply steering forces to move towards target
            applySteeringForces(targetPosition, nearbyUnits, deltaTime);
        }
    }

    /**
     * Set a new path for the unit to follow
     *
     * @param path          The path to follow
     * @param isPlayerOrder True if this is a player command (not AI)
     */
    public void setPath(List<Vector2> path, boolean isPlayerOrder) {
        this.currentPath = new ArrayList<>(path);
        this.currentPathIndex = 0;
        this.hasPlayerOrder = isPlayerOrder;
        if (!path.isEmpty()) {
            this.targetPosition = path.get(path.size() - 1);
            this.isMoving = true;

            // Update home position to new destination for defensive stance
            // This allows units to move to new locations and defend there
            homePosition = path.get(path.size() - 1).copy();
        }
    }

    /**
     * Set a new path for the unit to follow (AI order)
     */
    public void setPath(List<Vector2> path) {
        setPath(path, false);
    }

    /**
     * Clear current path
     */
    public void clearPath() {
        this.currentPath.clear();
        this.currentPathIndex = 0;
    }

    /**
     * Engage and attack target unit
     * Called by AttackUnitCommand
     *
     * @param target       The target unit
     * @param deltaTime    Time since last update
     * @param gameEntities Game entities (includes World for beam raycasting)
     * @return Projectile or Beam if fired, null otherwise
     */
    public AbstractOrdinance engageTarget(Unit target, double deltaTime, GameEntities gameEntities) {
        if (target == null || !target.isActive()) {
            return null;
        }

        Vector2 currentPos = getPosition();
        Vector2 targetPos = target.getPosition();
        double distance = currentPos.distance(targetPos);

        // Movement is handled by AttackUnitCommand.updateMovement()
        // This method just does the actual combat

        // Check if in range
        if (distance <= attackRange * 0.9) { // 90% of range to account for movement
            // Stop moving when in range
            body.setLinearVelocity(0, 0);

            // Face target
            Vector2 direction = targetPos.copy().subtract(currentPos);
            setRotation(Math.atan2(direction.y, direction.x));

            // Attack if cooldown is ready
            long now = System.currentTimeMillis();
            double attackInterval = 1000.0 / attackRate;
            if (now - lastAttackTime >= attackInterval) {
                lastAttackTime = now;
                // Use predictive aiming for moving targets
                Vector2 interceptPos = calculateInterceptPoint(target);
                // Fire projectile or beam (world from gameEntities)
                return fireAt(interceptPos, gameEntities != null ? gameEntities.getWorld() : null);
            }
        }

        return null;
    }

    /**
     * Engage and attack target unit (backward compatibility - projectiles only)
     */
    public Projectile engageTarget(Unit target, double deltaTime) {
        AbstractOrdinance ordinance = engageTarget(target, deltaTime, null);
        return ordinance instanceof Projectile ? (Projectile) ordinance : null;
    }

    /**
     * Engage and attack target building
     * Called by AttackBuildingCommand
     *
     * @param target       The target building
     * @param deltaTime    Time since last update
     * @param gameEntities Game entities (includes World for beam raycasting)
     * @return Projectile or Beam if fired, null otherwise
     */
    public AbstractOrdinance engageBuilding(Building target, double deltaTime, GameEntities gameEntities) {
        if (target == null || !target.isActive()) {
            return null;
        }

        Vector2 currentPos = getPosition();
        Vector2 targetPos = target.getPosition();
        double distance = currentPos.distance(targetPos);
        double effectiveRange = attackRange + target.getBuildingType().getSize();

        // Movement is handled by AttackBuildingCommand.updateMovement()
        // This method just does the actual combat

        // Check if in range
        if (distance <= effectiveRange * 0.9) {
            // Stop moving when in range
            body.setLinearVelocity(0, 0);

            // Face target
            Vector2 direction = targetPos.copy().subtract(currentPos);
            setRotation(Math.atan2(direction.y, direction.x));

            // Attack if cooldown is ready
            long now = System.currentTimeMillis();
            double attackInterval = 1000.0 / attackRate;
            if (now - lastAttackTime >= attackInterval) {
                lastAttackTime = now;
                // Fire projectile or beam (world from gameEntities)
                return fireAt(target.getPosition(), gameEntities != null ? gameEntities.getWorld() : null);
            }
        }

        return null;
    }

    /**
     * Engage and attack target building (backward compatibility - projectiles only)
     */
    public Projectile engageBuilding(Building target, double deltaTime) {
        AbstractOrdinance ordinance = engageBuilding(target, deltaTime, null);
        return ordinance instanceof Projectile ? (Projectile) ordinance : null;
    }

    /**
     * Engage and attack target wall segment
     * Called by AttackWallSegmentCommand
     *
     * @return Projectile if fired, null otherwise
     */
    public Projectile engageWallSegment(WallSegment target, double deltaTime) {
        if (target == null || !target.isActive()) {
            return null;
        }

        Vector2 currentPos = getPosition();
        Vector2 targetPos = target.getPosition();
        double distance = currentPos.distance(targetPos);
        double effectiveRange = attackRange + 20.0; // Wall segments have ~8 thickness

        // Movement is handled by AttackWallSegmentCommand.updateMovement()
        // This method just does the actual combat

        // Check if in range
        if (distance <= effectiveRange * 0.9) {
            // Stop moving when in range
            body.setLinearVelocity(0, 0);

            // Face target
            Vector2 direction = targetPos.copy().subtract(currentPos);
            setRotation(Math.atan2(direction.y, direction.x));

            // Attack if cooldown is ready
            long now = System.currentTimeMillis();
            double attackInterval = 1000.0 / attackRate;
            if (now - lastAttackTime >= attackInterval) {
                lastAttackTime = now;
                return fireAt(targetPos);
            }
        }

        return null;
    }

    /**
     * Engage and attack ground target (force attack - CMD/CTRL + right click)
     * Fires at a specific ground location for area denial
     * Called by AttackGroundCommand
     *
     * @return Projectile if fired, null otherwise
     */
    public Projectile engageGroundTarget(Vector2 groundTarget, double deltaTime) {
        if (groundTarget == null) {
            return null;
        }

        Vector2 currentPos = getPosition();
        double distance = currentPos.distance(groundTarget);

        // Movement is handled by AttackGroundCommand.updateMovement()
        // This method just does the actual combat

        // Check if in range
        if (distance <= attackRange * 0.9) {
            // Stop moving when in range
            body.setLinearVelocity(0, 0);

            // Face target
            Vector2 direction = groundTarget.copy().subtract(currentPos);
            setRotation(Math.atan2(direction.y, direction.x));

            // Attack if cooldown is ready
            long now = System.currentTimeMillis();
            double attackInterval = 1000.0 / attackRate;
            if (now - lastAttackTime >= attackInterval) {
                lastAttackTime = now;
                // Fire projectile at ground location
                return fireAt(groundTarget);
            }
        }

        return null;
    }

    /**
     * Override setRotation to prevent deployed Crawler from rotating
     */
    @Override
    public void setRotation(double rotation) {
        // Deployed Crawler cannot rotate (locked in place)
        if (unitType == UnitType.CRAWLER && specialAbilityActive) {
            return;
        }
        super.setRotation(rotation);
    }

    /**
     * Check if this unit can currently attack
     * Crawlers can only attack when deployed
     */
    public boolean canCurrentlyAttack() {
        if (!unitType.canAttack()) {
            return false;
        }

        // Crawler can only attack when deployed (turrets do the work)
        if (unitType == UnitType.CRAWLER && !specialAbilityActive) {
            return false;
        }

        return true;
    }

    /**
     * Attack a target unit - fires a projectile or beam with predictive aiming
     *
     * @return Projectile to be added to world, or null if can't attack
     */
    public Projectile attack(Unit target) {
        if (target == null || !target.isActive()) {
            return null;
        }

        // Crawler cannot attack with main cannon (only turrets when deployed)
        if (unitType == UnitType.CRAWLER) {
            return null;
        }

        // Use predictive aiming for moving targets
        Vector2 targetPos = calculateInterceptPoint(target);
        return fireAt(targetPos);
    }

    /**
     * Attack a target building - fires a projectile or beam
     *
     * @return Projectile to be added to world, or null if can't attack
     */
    public Projectile attackBuilding(Building target) {
        if (target == null || !target.isActive()) {
            return null;
        }

        // Crawler cannot attack with main cannon (only turrets when deployed)
        if (unitType == UnitType.CRAWLER) {
            return null;
        }

        return fireAt(target.getPosition());
    }

    /**
     * Calculate intercept point for predictive aiming
     * Uses linear prediction to lead moving targets
     *
     * @param target The target unit to intercept
     * @return The predicted intercept position
     */
    private Vector2 calculateInterceptPoint(Unit target) {
        Vector2 shooterPos = getPosition();
        Vector2 targetPos = target.getPosition();
        Vector2 targetVelocity = target.getBody().getLinearVelocity();

        // Get projectile speed
        ProjectileProperties props = getProjectileProperties();
        double projectileSpeed = props.speed;

        // If target is stationary or projectile is very fast, no need to lead
        double targetSpeed = targetVelocity.getMagnitude();
        if (targetSpeed < 1.0 || projectileSpeed > 1000.0) {
            return targetPos.copy();
        }

        // Calculate time to intercept using quadratic formula
        // Based on: |targetPos + targetVel * t - shooterPos| = projectileSpeed * t

        Vector2 toTarget = targetPos.copy().subtract(shooterPos);
        double a = targetVelocity.dot(targetVelocity) - (projectileSpeed * projectileSpeed);
        double b = 2 * toTarget.dot(targetVelocity);
        double c = toTarget.dot(toTarget);

        double discriminant = b * b - 4 * a * c;

        // If no solution, aim at current position
        if (discriminant < 0 || Math.abs(a) < 0.001) {
            return targetPos.copy();
        }

        // Use the smaller positive root (earliest intercept time)
        double t1 = (-b + Math.sqrt(discriminant)) / (2 * a);
        double t2 = (-b - Math.sqrt(discriminant)) / (2 * a);

        double interceptTime;
        if (t1 > 0 && t2 > 0) {
            interceptTime = Math.min(t1, t2);
        } else if (t1 > 0) {
            interceptTime = t1;
        } else if (t2 > 0) {
            interceptTime = t2;
        } else {
            // No positive solution, aim at current position
            return targetPos.copy();
        }

        // Clamp intercept time to reasonable values (max 3 seconds ahead)
        interceptTime = Math.min(interceptTime, 3.0);

        // Calculate intercept position

        return targetPos.copy().add(targetVelocity.copy().multiply(interceptTime));
    }

    /**
     * Fire a projectile or beam at a target position
     *
     * @param targetPos The position to fire at
     * @param world     The game world (needed for beam raycasting, can be null for projectiles)
     * @return Projectile or Beam, or null if world is required but not provided
     */
    public AbstractOrdinance fireAt(Vector2 targetPos, org.dyn4j.world.World<org.dyn4j.dynamics.Body> world) {
        Vector2 currentPos = getPosition();
        Vector2 direction = targetPos.copy().subtract(currentPos);
        direction.normalize();

        // Check if this unit fires beams
        if (unitType.firesBeams()) {
            // Beams require the world for raycasting
            if (world == null) {
                return null;
            }

            // Create a beam
            return new Beam(
                    id,
                    world,
                    currentPos,
                    direction,
                    attackRange,  // max range
                    ownerId,
                    teamNumber,
                    damage,
                    new java.util.HashSet<>(), // No special bullet effects for beams yet
                    Ordinance.LASER,
                    Beam.BeamType.LASER,
                    2.0,  // width
                    0.15, // duration (150ms)
                    body  // ignore self
            );
        } else {
            // Create a projectile
            ProjectileProperties props = getProjectileProperties();
            Vector2 velocity = direction.multiply(props.speed);

            return new Projectile(
                    id,
                    currentPos.x,
                    currentPos.y,
                    velocity.x,
                    velocity.y,
                    damage,
                    attackRange,
                    teamNumber,
                    props.linearDamping,
                    props.bulletEffects,
                    props.ordinance,
                    props.size
            );
        }
    }

    /**
     * Fire at a target position (without world - projectiles only)
     * For backward compatibility
     */
    private Projectile fireAt(Vector2 targetPos) {
        AbstractOrdinance ordinance = fireAt(targetPos, null);
        return ordinance instanceof Projectile ? (Projectile) ordinance : null;
    }

    /**
     * Get projectile properties for this unit type
     */
    private ProjectileProperties getProjectileProperties() {
        ProjectileProperties props = new ProjectileProperties();

        switch (unitType) {
            case WORKER:
                // Workers don't really attack, but if they do, weak projectiles
                props.speed = 300;
                props.ordinance = Ordinance.BULLET;
                props.linearDamping = 0.5;
                props.size = 1.5; // Tiny
                break;

            case INFANTRY:
                // Standard bullets (baseline)
                props.speed = 500;
                props.ordinance = Ordinance.BULLET;
                props.linearDamping = 0.3;
                props.size = 2.0; // Baseline
                break;

            case ROCKET_SOLDIER:
                // Rockets - slower but explosive
                props.speed = 400;
                props.size = 3.0; // Larger rocket
                props.ordinance = Ordinance.ROCKET;
                props.linearDamping = 0.1;
                props.bulletEffects.add(BulletEffect.EXPLOSIVE);
                break;

            case SNIPER:
                // High-velocity sniper rounds
                props.speed = 800;
                props.ordinance = Ordinance.BULLET;
                props.linearDamping = 0.1;
                props.size = 2.5; // Slightly larger than infantry
                break;

            case JEEP:
                // Fast bullets from mounted gun
                props.speed = 600;
                props.ordinance = Ordinance.BULLET;
                props.linearDamping = 0.2;
                props.size = 2.5; // Slightly larger than infantry
                break;

            case TANK:
                // Tank shells - explosive
                props.speed = 450;
                props.ordinance = Ordinance.GRENADE; // Use grenade for tank shells
                props.linearDamping = 0.05;
                props.size = 4.0; // Noticeably larger
                props.bulletEffects.add(BulletEffect.EXPLOSIVE);
                break;

            case STEALTH_TANK:
                // Stealth tank shells
                props.speed = 500;
                props.ordinance = Ordinance.GRENADE;
                props.linearDamping = 0.05;
                props.size = 4.0; // Same as tank
                props.bulletEffects.add(BulletEffect.EXPLOSIVE);
                break;

            case MAMMOTH_TANK:
                // Massive shells from dual cannons
                props.speed = 400;
                props.ordinance = Ordinance.SHELL;
                props.linearDamping = 0.03;
                props.size = 6.0; // BIG shells!
                props.bulletEffects.add(BulletEffect.EXPLOSIVE);
                break;

            case ARTILLERY:
                // Artillery shells - high arc, explosive
                props.speed = 350;
                props.ordinance = Ordinance.GRENADE; // Use grenade for artillery
                props.linearDamping = 0.02;
                props.size = 5.0; // Large artillery shells
                props.bulletEffects.add(BulletEffect.EXPLOSIVE);
                props.bulletEffects.add(BulletEffect.BOUNCING); // Bouncy for scatter effect
                break;

            case GIGANTONAUT:
                // MASSIVE shells - slow, heavy, devastating
                props.speed = 300; // Slower than artillery
                props.ordinance = Ordinance.SHELL; // Heavy shell
                props.linearDamping = 0.01; // Very little damping (heavy!)
                props.size = 8.0; // MASSIVE projectiles! (largest in game)
                props.bulletEffects.add(BulletEffect.EXPLOSIVE);
                props.bulletEffects.add(BulletEffect.PIERCING); // Pierces through targets
                break;

            case CRAWLER:
                // Crawler turrets (handled separately in RTSGameManager)
                props.speed = 450;
                props.ordinance = Ordinance.SHELL;
                props.linearDamping = 0.2;
                props.size = 5.0; // Large turret shells
                props.bulletEffects.add(BulletEffect.EXPLOSIVE);
                break;

            default:
                props.speed = 400;
                props.ordinance = Ordinance.BULLET;
                props.linearDamping = 0.3;
                props.size = 2.0; // Default to baseline
                break;
        }

        return props;
    }

    /**
     * Helper class for projectile properties
     */
    private static class ProjectileProperties {
        double speed = 400;
        Ordinance ordinance = Ordinance.BULLET;
        double linearDamping = 0.3;
        double size = 2.0; // Projectile radius (baseline: infantry)
        Set<BulletEffect> bulletEffects = new HashSet<>();
    }

    /**
     * Harvest resources from deposit
     * Called by HarvestCommand
     *
     * @return true if should switch to returning resources
     */
    public boolean harvestResources(ResourceDeposit deposit, double deltaTime) {
        if (deposit == null || !deposit.isActive()) {
            return false;
        }

        // Movement is handled by HarvestCommand.updateMovement()
        // This method just does the actual harvesting work

        // Check if in range
        Vector2 currentPos = getPosition();
        Vector2 depositPos = deposit.getPosition();
        double distance = currentPos.distance(depositPos);

        if (distance <= deposit.getHarvestRange() + unitType.getSize()) {
            // In range - stop and harvest
            body.setLinearVelocity(0, 0);

            // Harvest at rate of 10 resources per second
            if (carriedResources < MAX_CARRIED_RESOURCES) {
                double harvestAmount = 10.0 * deltaTime;
                double actualHarvested = deposit.harvest(harvestAmount);
                carriedResources += actualHarvested;

                // If full or deposit depleted, signal to return to refinery
                return carriedResources >= MAX_CARRIED_RESOURCES || actualHarvested == 0; // Switch to returning resources
            }
        }

        return false; // Continue harvesting
    }

    /**
     * Return resources to nearest refinery
     * Called by HarvestCommand
     *
     * @return true if resources were deposited and should return to harvesting
     */
    public boolean returnResourcesToRefinery(Building refinery, double deltaTime) {
        // Wait for RTSGameManager to assign a refinery
        if (refinery == null || !refinery.isActive()) {
            // Just wait, don't move yet
            body.setLinearVelocity(0, 0);
            return false;
        }

        // Movement is handled by HarvestCommand.updateMovement()
        // This method just does the actual resource deposit work

        // Check if in range
        Vector2 currentPos = getPosition();
        Vector2 refineryPos = refinery.getPosition();
        double distance = currentPos.distance(refineryPos);

        if (distance <= refinery.getBuildingType().getSize() + unitType.getSize() + 10) {
            // In range - stop and deposit resources
            body.setLinearVelocity(0, 0);

            // Deposit resources to refinery owner's faction
            if (carriedResources > 0) {
                // Resources will be added by RTSGameManager
                carriedResources = 0;
                return true; // Signal to return to harvesting
            }
        }

        return false; // Continue moving to refinery
    }


    /**
     * Construct a building
     * Called by ConstructCommand
     */
    public void constructBuilding(Building building, double deltaTime) {
        if (building == null || !building.isActive()) {
            return;
        }

        // Movement is handled by ConstructCommand.updateMovement()
        // This method just does the actual construction work

        // Check if in range
        Vector2 currentPos = getPosition();
        Vector2 buildingPos = building.getPosition();
        double distance = currentPos.distance(buildingPos);
        double buildRange = building.getBuildingType().getSize() + unitType.getSize() + 10;

        if (distance <= buildRange) {
            // In range - stop and construct
            body.setLinearVelocity(0, 0);

            // Build at rate of 10 health per second
            double progressAdded = 10 * deltaTime;
            building.addConstructionProgress(progressAdded);
        }
    }

    /**
     * Mine an obstacle to destroy it
     * Called by MineCommand
     *
     * @return true if should switch to returning for repair
     */
    public boolean mineObstacle(Obstacle obstacle, double deltaTime) {
        if (obstacle == null || !obstacle.isActive()) {
            return false;
        }

        // Check if pickaxe needs repair
        if (pickaxeDurability <= 0) {
            log.info("Miner {} pickaxe broke, returning to headquarters for repair", id);
            return true; // Signal to return for repair
        }

        // Movement is handled by MineCommand.updateMovement()
        // This method just does the actual mining work

        // Check if in range
        Vector2 currentPos = getPosition();
        Vector2 obstaclePos = obstacle.getPosition();
        double distance = currentPos.distance(obstaclePos);
        double miningRange = obstacle.getBoundingRadius() + unitType.getSize() + 5;

        if (distance <= miningRange) {
            // In range - stop and mine
            body.setLinearVelocity(0, 0);

            // Deal damage to obstacle and wear down pickaxe
            double damageDealt = MINING_DAMAGE_RATE * deltaTime;
            obstacle.takeDamage(damageDealt);
            pickaxeDurability -= PICKAXE_WEAR_RATE * deltaTime;

            // Face the obstacle
            Vector2 direction = obstaclePos.copy().subtract(currentPos);
            setRotation(Math.atan2(direction.y, direction.x));

            // Check if obstacle is destroyed or pickaxe is low
            if (!obstacle.isActive()) {
                log.info("Miner {} destroyed obstacle {}", id, obstacle.getId());
                // If pickaxe is low, return for repair
                return pickaxeDurability < 30.0; // Signal to return for repair
            }
        }

        return false; // Continue mining
    }

    /**
     * Return to headquarters to repair pickaxe
     * Called by MineCommand
     *
     * @return true if repair is complete and should return to mining
     */
    public boolean returnForPickaxeRepair(Building headquarters, double deltaTime) {
        if (headquarters == null || !headquarters.isActive()) {
            // Just wait, don't move yet
            body.setLinearVelocity(0, 0);
            return false;
        }

        // Movement is handled by MineCommand.updateMovement()
        // This method just does the actual repair work

        // Check if we're close enough to headquarters to repair
        Vector2 currentPos = getPosition();
        Vector2 hqPos = headquarters.getPosition();
        double distance = currentPos.distance(hqPos);

        // If close enough (within ~100 units), start repairing
        if (distance < 100.0) {
            // Stop and repair pickaxe
            body.setLinearVelocity(0, 0);

            // Repair pickaxe at rate of 50 durability per second (fast repair)
            pickaxeDurability = Math.min(MAX_PICKAXE_DURABILITY, pickaxeDurability + 50.0 * deltaTime);

            // Check if repair is complete
            if (pickaxeDurability >= MAX_PICKAXE_DURABILITY) {
                log.info("Miner {} pickaxe repaired", id);
                return true; // Signal to return to mining
            }
        }

        return false; // Continue repairing
    }

    // ===== COMMAND PATTERN METHODS =====

    /**
     * Issue a new command to this unit (replaces current command)
     */
    public void issueCommand(UnitCommand newCommand) {
        if (currentCommand != null) {
            currentCommand.onCancel(); // Clean up old command
        }
        currentCommand = newCommand;
        log.debug("Unit {} issued command: {}", id, newCommand.getDescription());
    }

    // ===== ORDER METHODS =====

    /**
     * Give this unit a move order
     */
    public void orderMove(Vector2 destination) {
        MoveCommand moveCommand = new MoveCommand(this, destination, true);
        issueCommand(moveCommand);
    }

    /**
     * Give this unit a force attack order (attack ground - CMD/CTRL + right click)
     * Units will move into range and fire at the ground location
     * Useful for artillery/AoE units doing area denial
     */
    public void orderForceAttack(Vector2 groundTarget) {
        if (!unitType.canAttack()) {
            return;
        }

        AttackGroundCommand attackGroundCommand = new AttackGroundCommand(this, groundTarget, true);
        issueCommand(attackGroundCommand);
        log.info("Unit {} ordered to attack ground at ({}, {})", id, groundTarget.x, groundTarget.y);
    }

    /**
     * Give this unit an attack order on another unit
     */
    public void orderAttack(Unit target) {
        if (!unitType.canAttack()) {
            return;
        }

        AttackUnitCommand attackCommand = new AttackUnitCommand(this, target, true);
        issueCommand(attackCommand);
    }

    /**
     * Give this unit an attack order on a building
     */
    public void orderAttackBuilding(Building target) {
        if (!unitType.canAttack()) {
            return;
        }

        AttackBuildingCommand attackCommand = new AttackBuildingCommand(this, target, true);
        issueCommand(attackCommand);
    }

    /**
     * Give this unit an attack order on a wall segment
     */
    public void orderAttackWallSegment(WallSegment target) {
        if (!unitType.canAttack()) {
            return;
        }

        AttackWallSegmentCommand attackCommand = new AttackWallSegmentCommand(this, target, true);
        issueCommand(attackCommand);
    }

    /**
     * Give this unit a harvest order
     */
    public void orderHarvest(ResourceDeposit deposit) {
        if (!unitType.canHarvest()) {
            return;
        }

        HarvestCommand harvestCommand = new HarvestCommand(this, deposit, true);
        issueCommand(harvestCommand);
    }

    /**
     * Give this unit a construction order
     */
    public void orderConstruct(Building building) {
        if (!unitType.canBuild()) {
            return;
        }

        ConstructCommand constructCommand = new ConstructCommand(this, building, true);
        issueCommand(constructCommand);
    }

    /**
     * Give this unit a mining order
     */
    public void orderMine(Obstacle obstacle) {
        if (!unitType.canMine()) {
            return;
        }

        // Check if obstacle is destructible
        if (!obstacle.isDestructible()) {
            log.warn("Miner {} cannot mine indestructible obstacle {}", id, obstacle.getId());
            return;
        }

        MineCommand mineCommand = new MineCommand(this, obstacle, true);
        issueCommand(mineCommand);
        log.info("Miner {} ordered to mine obstacle {} (pickaxe: {}%)",
                id, obstacle.getId(), (int) pickaxeDurability);
    }

    /**
     * Give this unit a garrison order (enter bunker)
     */
    public void orderGarrison(Building bunker) {
        if (!unitType.isInfantry()) {
            return; // Only infantry can garrison
        }

        GarrisonBunkerCommand garrisonCommand = new GarrisonBunkerCommand(this, bunker, true);
        issueCommand(garrisonCommand);
        log.info("Unit {} ordered to garrison in bunker {}", id, bunker.getId());
    }

    /**
     * Stop all current orders
     */
    public void orderStop() {
        IdleCommand idleCommand = new IdleCommand(this);
        issueCommand(idleCommand);
    }

    /**
     * Deposit carried resources at a refinery
     *
     * @return amount of resources deposited
     */
    public int depositResources() {
        int amount = (int) Math.round(carriedResources);
        carriedResources = 0;
        return amount;
    }

    /**
     * Check if this unit belongs to a specific player
     */
    public boolean belongsTo(int playerId) {
        return this.ownerId == playerId;
    }

    /**
     * Check if this unit is on a specific team
     */
    public boolean isOnTeam(int team) {
        return this.teamNumber == team;
    }

    /**
     * AI behavior: scan for enemies and auto-attack based on stance
     *
     * @return true if an enemy was engaged
     */
    public boolean scanForEnemies(List<Unit> allUnits, List<Building> allBuildings) {
        // Only combat units with auto-attack enabled OR attack-moving units
        if (!unitType.canAttack()) {
            return false;
        }

        // Attack-move units should always scan, but respect player orders for non-attack-move
        if (!isAttackMoving) {
            if (!aiStance.isAutoAttack()) {
                return false;
            }

            // Don't interrupt player orders or existing AI orders (unless aggressive or attack-moving)
            if (hasPlayerOrder) {
                return false; // Never interrupt player commands
            }
            if (aiStance != AIStance.AGGRESSIVE && (targetUnit != null || targetBuilding != null || isHarvesting || isConstructing)) {
                return false;
            }
        }

        Vector2 currentPos = getPosition();
        double visionRange = unitType.getVisionRange(); // Vision range (varies by unit type)

        // Check for defensive leash (don't chase too far from home) - not for attack-move
        if (!isAttackMoving && aiStance == AIStance.DEFENSIVE && homePosition != null) {
            double distanceFromHome = currentPos.distance(homePosition);
            if (distanceFromHome > DEFENSIVE_LEASH_RANGE) {
                // Return to home position
                this.targetPosition = homePosition.copy();
                this.isMoving = true;
                this.targetUnit = null;
                this.targetBuilding = null;
                return false;
            }
        }

        // Find nearest enemy unit in vision range
        Unit nearestEnemy = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Unit unit : allUnits) {
            if (unit.getTeamNumber() == this.teamNumber || !unit.isActive()) {
                continue;
            }

            double distance = currentPos.distance(unit.getPosition());
            if (distance < visionRange && distance < nearestDistance) {
                // For defensive stance, only engage if enemy is close to home
                if (aiStance == AIStance.DEFENSIVE && homePosition != null) {
                    double enemyDistanceFromHome = unit.getPosition().distance(homePosition);
                    if (enemyDistanceFromHome > DEFENSIVE_LEASH_RANGE) {
                        continue;
                    }
                }

                nearestEnemy = unit;
                nearestDistance = distance;
            }
        }

        // If found an enemy unit, engage it
        if (nearestEnemy != null) {
            orderAttack(nearestEnemy);
            return true;
        }

        // Find nearest enemy building in vision range (lower priority than units)
        Building nearestEnemyBuilding = null;
        nearestDistance = Double.MAX_VALUE;

        for (Building building : allBuildings) {
            if (building.getTeamNumber() == this.teamNumber || !building.isActive()) {
                continue;
            }

            double distance = currentPos.distance(building.getPosition());
            if (distance < visionRange && distance < nearestDistance) {
                // For defensive stance, only engage if building is close to home
                if (aiStance == AIStance.DEFENSIVE && homePosition != null) {
                    double buildingDistanceFromHome = building.getPosition().distance(homePosition);
                    if (buildingDistanceFromHome > DEFENSIVE_LEASH_RANGE) {
                        continue;
                    }
                }

                nearestEnemyBuilding = building;
                nearestDistance = distance;
            }
        }

        // If found an enemy building, engage it
        if (nearestEnemyBuilding != null) {
            orderAttackBuilding(nearestEnemyBuilding);
            return true;
        }

        return false;
    }

    /**
     * AI behavior: Medics scan for damaged friendlies and auto-heal
     *
     * @return true if a heal target was found and healed
     */
    public boolean scanForHealTargets(List<Unit> allUnits) {
        // Only medics can heal
        if (!unitType.canHeal()) {
            return false;
        }

        // Don't interrupt player orders
        if (hasPlayerOrder || isMoving) {
            return false;
        }

        // Check if heal ability is ready
        SpecialAbility ability = unitType.getSpecialAbility();
        if (ability != SpecialAbility.HEAL) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastSpecialAbilityTime < ability.getCooldownMs()) {
            return false; // Still on cooldown
        }

        Vector2 currentPos = getPosition();
        double healRange = 150.0; // Medics can heal within 150 units

        // Find most damaged friendly unit in range
        Unit mostDamagedUnit = null;
        double lowestHealthPercent = 1.0;

        for (Unit unit : allUnits) {
            if (unit == this || unit.getTeamNumber() != this.teamNumber || !unit.isActive()) {
                continue;
            }

            double distance = currentPos.distance(unit.getPosition());
            if (distance > healRange) {
                continue;
            }

            double healthPercent = unit.getHealth() / unit.getMaxHealth();
            if (healthPercent < 1.0 && healthPercent < lowestHealthPercent) {
                mostDamagedUnit = unit;
                lowestHealthPercent = healthPercent;
            }
        }

        // If found a damaged unit, heal it
        if (mostDamagedUnit != null) {
            boolean healed = useSpecialAbilityOnUnit(mostDamagedUnit);
            if (healed) {
                log.debug("Medic {} auto-healed unit {} ({}% health)",
                        id, mostDamagedUnit.getId(), (int) (lowestHealthPercent * 100));
                return true;
            }
        }

        return false;
    }

    /**
     * AI behavior: Engineers scan for damaged friendly units and buildings, then auto-repair
     *
     * @return true if a repair target was found and repaired
     */
    public boolean scanForRepairTargets(List<Building> allBuildings, List<Unit> allUnits) {
        // Only engineers can repair
        if (!unitType.canRepair()) {
            return false;
        }

        // Don't interrupt player orders
        if (hasPlayerOrder || isMoving || isConstructing) {
            return false;
        }

        // Check if repair ability is ready
        SpecialAbility ability = unitType.getSpecialAbility();
        if (ability != SpecialAbility.REPAIR) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastSpecialAbilityTime < ability.getCooldownMs()) {
            return false; // Still on cooldown
        }

        Vector2 currentPos = getPosition();
        double repairRange = 150.0; // Engineers can repair within 150 units

        // Find most damaged friendly unit in range (prioritize units over buildings)
        Unit mostDamagedUnit = null;
        double lowestUnitHealthPercent = 1.0;

        for (Unit unit : allUnits) {
            if (unit == this || unit.getTeamNumber() != this.teamNumber || !unit.isActive()) {
                continue;
            }

            double distance = currentPos.distance(unit.getPosition());
            if (distance > repairRange) {
                continue;
            }

            double healthPercent = unit.getHealth() / unit.getMaxHealth();
            if (healthPercent < 1.0 && healthPercent < lowestUnitHealthPercent) {
                mostDamagedUnit = unit;
                lowestUnitHealthPercent = healthPercent;
            }
        }

        // If found a damaged unit, repair it
        if (mostDamagedUnit != null) {
            boolean repaired = useSpecialAbilityOnUnit(mostDamagedUnit);
            if (repaired) {
                log.debug("Engineer {} auto-repaired unit {} ({}% health)",
                        id, mostDamagedUnit.getId(), (int) (lowestUnitHealthPercent * 100));
                return true;
            }
        }

        // Otherwise, find most damaged friendly building in range
        Building mostDamagedBuilding = null;
        double lowestBuildingHealthPercent = 1.0;

        for (Building building : allBuildings) {
            if (building.getTeamNumber() != this.teamNumber || !building.isActive() || building.isUnderConstruction()) {
                continue;
            }

            double distance = currentPos.distance(building.getPosition());
            if (distance > repairRange) {
                continue;
            }

            double healthPercent = building.getHealth() / building.getMaxHealth();
            if (healthPercent < 1.0 && healthPercent < lowestBuildingHealthPercent) {
                mostDamagedBuilding = building;
                lowestBuildingHealthPercent = healthPercent;
            }
        }

        // If found a damaged building, repair it
        if (mostDamagedBuilding != null) {
            boolean repaired = useSpecialAbilityOnBuilding(mostDamagedBuilding);
            if (repaired) {
                log.debug("Engineer {} auto-repaired building {} ({}% health)",
                        id, mostDamagedBuilding.getId(), (int) (lowestBuildingHealthPercent * 100));
                return true;
            }
        }

        return false;
    }

    /**
     * Check if unit should return to home position (for defensive stance)
     */
    public boolean shouldReturnHome() {
        if (aiStance != AIStance.DEFENSIVE || homePosition == null) {
            return false;
        }

        // If we're idle and far from home, return
        Vector2 currentPos = getPosition();
        double distanceFromHome = currentPos.distance(homePosition);

        return !isMoving && !isHarvesting && !isConstructing &&
                targetUnit == null && targetBuilding == null &&
                distanceFromHome > 50.0; // Return if more than 50 units from home
    }

    /**
     * Activate or toggle the unit's special ability
     *
     * @return true if ability was activated/toggled, false if on cooldown or not available
     */
    public boolean activateSpecialAbility() {
        SpecialAbility ability = unitType.getSpecialAbility();
        if (ability == SpecialAbility.NONE) {
            return false;
        }

        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastSpecialAbilityTime < ability.getCooldownMs()) {
            return false; // Still on cooldown
        }

        // Handle toggle abilities
        if (ability.isToggle()) {
            specialAbilityActive = !specialAbilityActive;
            lastSpecialAbilityTime = now;
            applySpecialAbilityEffects();
            return true;
        }

        // For non-toggle abilities that require targets, they use the target-based methods
        // (e.g., healTarget, repairTarget)
        return false;
    }

    /**
     * Use special ability on a target unit (e.g., Medic heal)
     *
     * @param target The unit to heal
     * @return true if ability was used successfully
     */
    public boolean useSpecialAbilityOnUnit(Unit target) {
        SpecialAbility ability = unitType.getSpecialAbility();
        if (ability == SpecialAbility.NONE || !ability.isRequiresTarget()) {
            return false;
        }

        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastSpecialAbilityTime < ability.getCooldownMs()) {
            return false;
        }

        // Check if target is valid (same team, not at full health)
        if (target.getTeamNumber() != this.teamNumber || target.getHealth() >= target.getMaxHealth()) {
            return false;
        }

        switch (ability) {
            case HEAL:
                // Medic heals friendly units
                double healAmount = 20.0; // Heal 20 HP per use
                double newHealth = Math.min(target.getMaxHealth(), target.getHealth() + healAmount);
                target.setHealth(newHealth);
                lastSpecialAbilityTime = now;
                log.info("Medic {} healed unit {} for {} HP", id, target.getId(), healAmount);
                return true;

            case REPAIR:
                // Engineer repairs friendly vehicles/units
                double repairAmount = 25.0; // Repair 25 HP per use
                double newUnitHealth = Math.min(target.getMaxHealth(), target.getHealth() + repairAmount);
                target.setHealth(newUnitHealth);
                lastSpecialAbilityTime = now;
                log.info("Engineer {} repaired unit {} for {} HP", id, target.getId(), repairAmount);
                return true;

            default:
                return false;
        }
    }

    /**
     * Use special ability on a target building (e.g., Engineer repair)
     *
     * @param target The building to repair
     * @return true if ability was used successfully
     */
    public boolean useSpecialAbilityOnBuilding(Building target) {
        SpecialAbility ability = unitType.getSpecialAbility();
        if (ability == SpecialAbility.NONE || !ability.isRequiresTarget()) {
            return false;
        }

        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastSpecialAbilityTime < ability.getCooldownMs()) {
            return false;
        }

        // Check if target is valid (same team, not at full health, not under construction)
        if (target.getTeamNumber() != this.teamNumber ||
                target.getHealth() >= target.getMaxHealth() ||
                target.isUnderConstruction()) {
            return false;
        }

        switch (ability) {
            case REPAIR:
                // Engineer repairs friendly buildings
                double repairAmount = 30.0; // Repair 30 HP per use
                double newHealth = Math.min(target.getMaxHealth(), target.getHealth() + repairAmount);
                target.setHealth(newHealth);
                lastSpecialAbilityTime = now;
                log.info("Engineer {} repaired building {} for {} HP", id, target.getId(), repairAmount);
                return true;

            default:
                return false;
        }
    }

    /**
     * Apply stat modifications based on active special abilities
     */
    private void applySpecialAbilityEffects() {
        SpecialAbility ability = unitType.getSpecialAbility();

        switch (ability) {
            case DEPLOY:
                // Crawler deploy mode: +50% range/damage, but immobile, with 4 independent turrets
                if (specialAbilityActive) {
                    attackRange = unitType.getAttackRange() * 1.5;
                    damage = unitType.getDamage() * 1.5;
                    movementSpeed = 0; // Immobile when deployed

                    // Create 4 turrets at corners of the Crawler
                    turrets.clear();
                    double turretOffset = unitType.getSize() * 0.6; // 60% of unit size
                    turrets.add(new UnitTurret(0, new Vector2(turretOffset, turretOffset)));   // Top-right
                    turrets.add(new UnitTurret(1, new Vector2(-turretOffset, turretOffset)));  // Top-left
                    turrets.add(new UnitTurret(2, new Vector2(-turretOffset, -turretOffset))); // Bottom-left
                    turrets.add(new UnitTurret(3, new Vector2(turretOffset, -turretOffset)));  // Bottom-right

                    log.info("Crawler {} deployed - range: {}, damage: {}, turrets: {}",
                            id, attackRange, damage, turrets.size());
                } else {
                    // Reset to normal stats and clear turrets
                    attackRange = unitType.getAttackRange();
                    damage = unitType.getDamage();
                    movementSpeed = unitType.getMovementSpeed();
                    turrets.clear();
                    log.info("Crawler {} undeployed - returning to mobile mode", id);
                }
                break;

            case STEALTH:
                // Stealth mode: invisible (implementation TBD)
                if (specialAbilityActive) {
                    log.info("Unit {} entered stealth mode", id);
                } else {
                    log.info("Unit {} exited stealth mode", id);
                }
                break;

            default:
                break;
        }
    }

    /**
     * Check if special ability is ready (not on cooldown)
     */
    public boolean isSpecialAbilityReady() {
        SpecialAbility ability = unitType.getSpecialAbility();
        if (ability == SpecialAbility.NONE) {
            return false;
        }
        long now = System.currentTimeMillis();
        return (now - lastSpecialAbilityTime) >= ability.getCooldownMs();
    }
}

