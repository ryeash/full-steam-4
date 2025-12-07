package com.fullsteam.model;

import com.fullsteam.model.command.IdleCommand;
import com.fullsteam.model.command.SortieCommand;
import com.fullsteam.model.command.UnitCommand;
import com.fullsteam.model.component.HangarComponent;
import com.fullsteam.model.research.ResearchModifier;
import com.fullsteam.model.weapon.ProjectileWeapon;
import com.fullsteam.model.weapon.Weapon;
import com.fullsteam.model.weapon.WeaponFactory;
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
import java.util.List;

/**
 * Represents a unit (worker, infantry, vehicle, etc.)
 */
@Slf4j
@Getter
@Setter
public class Unit extends GameEntity {
    private final UnitType unitType;
    private final int ownerId; // Player who owns this unit
    private final int teamNumber;
    private UnitCommand currentCommand = null;
    private double movementSpeed;

    // LEGACY fields kept for backward compatibility with existing logic
    // TODO: These can be removed once all subsystems fully use commands
    private Vector2 targetPosition = null; // Used by legacy pathfinding
    private Unit targetUnit = null; // Used by legacy combat
    private Building targetBuilding = null; // Used by legacy combat/harvesting
    private boolean isMoving = false; // Used by legacy movement
    private boolean hasPlayerOrder = false; // Used by AI scanning

    // Pathfinding
    private List<Vector2> currentPath = new ArrayList<>();
    private int currentPathIndex = 0;

    // AI stance
    private AIStance aiStance = AIStance.DEFENSIVE; // Default stance
    private AIStance preCloakAIStance = null; // Saved AI stance before cloaking
    private Vector2 homePosition = null; // For defensive stance

    // Special ability system
    private boolean specialAbilityActive = false; // For toggle abilities (deploy, cloak, etc.)
    private long lastSpecialAbilityTime = 0; // For cooldown tracking

    // Cloak system (for Cloak Tank)
    private static final double CLOAK_DETECTION_RANGE = 50.0; // Range at which cloaked units are detected
    private long lastFireTime = 0; // Track when unit last fired (for cloak delay)

    // Android Factory tracking (for androids only)
    private Integer androidFactoryId = null; // ID of the Android Factory that produced this unit (null if not an android)

    // Multi-turret system (for Crawler when deployed)
    private List<Turret> turrets = new ArrayList<>();

    // Weapon system
    private Weapon weapon; // The weapon this unit fires (null for non-combat units)
    private double visionRange; // Modified by research

    // Resource harvesting (for workers)
    private double carriedResources = 0;
    private static final double BASE_MAX_CARRIED_RESOURCES = 100.0;
    private double maxCarriedResources = BASE_MAX_CARRIED_RESOURCES; // Can be modified by research
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
        this.visionRange = unitType.getVisionRange();
        this.weapon = WeaponFactory.getWeaponForUnitType(unitType);
    }

    private static Body createUnitBody(double x, double y, UnitType unitType) {
        Body body = new Body();

        // Create custom physics fixtures for this unit type (supports multi-fixture bodies)
        List<Convex> shapes = unitType.createPhysicsFixtures();

        // Air units are sensors (no physical collision with obstacles/units)
        boolean isAirUnit = unitType.isAirUnit();

        // Add all fixtures to the body
        for (Convex shape : shapes) {
            BodyFixture fixture = body.addFixture(shape);

            // Configure fixture properties
            fixture.setFriction(0.1);      // Low friction for smooth movement
            fixture.setRestitution(0.0);   // No bounce
            fixture.setSensor(isAirUnit);  // Air units are sensors (fly over obstacles)
        }

        // Rotate polygons to face right by default (positive X direction)
        // Circles don't need rotation, rectangles are already oriented correctly
        // Check the first fixture to determine if rotation is needed
        if (!shapes.isEmpty() && shapes.get(0) instanceof Polygon) {
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
    public void update(GameEntities gameEntities) {
        if (!active) {
            return;
        }

        // Update command (checks if command is complete and does work)
        if (currentCommand != null) {
            boolean stillActive = currentCommand.update(gameEntities.getWorld().getTimeStep().getDeltaTime());
            if (!stillActive) {
                // Special handling for sortie-based units (Bomber, etc.)
                if (currentCommand instanceof SortieCommand sortieCmd) {
                    handleSortieCompletion(sortieCmd, gameEntities);
                    return; // Unit is being removed/returned to hangar
                }
                
                // Command completed, switch to idle
                currentCommand = new IdleCommand(this);
            }
        }
    }
    
    /**
     * Handle bomber returning from sortie - update hangar and despawn
     */
    private void handleSortieCompletion(SortieCommand sortieCmd, GameEntities gameEntities) {
        int hangarId = sortieCmd.getHomeHangarId();
        Building hangar = gameEntities.getBuildings().get(hangarId);
        
        if (hangar != null && hangar.isActive()) {
            // Return to hangar component
            HangarComponent hangarComponent = hangar.getComponent(HangarComponent.class).orElse(null);
            if (hangarComponent != null) {
                hangarComponent.returnFromSortie(this); // Updates aircraft health
            }
        }
        
        // Despawn the bomber unit
        this.setActive(false);
    }

    /**
     * Update movement with steering behaviors (called from RTSGameManager with nearby units)
     */
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        if (currentCommand != null) {
            currentCommand.updateMovement(deltaTime, nearbyUnits);
        }
    }

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
        return unitType != UnitType.CRAWLER || specialAbilityActive;
    }

    /**
     * Calculate intercept point for predictive aiming
     * Uses linear prediction to lead moving targets
     *
     * @param target The target unit to intercept
     * @return The predicted intercept position
     */
    public Vector2 calculateInterceptPoint(Unit target) {
        Vector2 shooterPos = getPosition();
        Vector2 targetPos = target.getPosition();
        Vector2 targetVelocity = target.getBody().getLinearVelocity();

        // Get projectile speed
        double projectileSpeed = weapon instanceof ProjectileWeapon pw
                ? pw.getProjectileSpeed()
                : Double.MAX_VALUE;

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
     * Fire projectiles or beams at a target position using the weapon system
     *
     * @param targetPos    The position to fire at
     * @param gameEntities The game entities (provides access to world and all entities)
     * @return List of ordinances created (may be empty if unable to fire)
     */
    public List<AbstractOrdinance> fireAt(Vector2 targetPos, GameEntities gameEntities) {
        // Check if unit has a weapon
        if (weapon == null) {
            return List.of();
        }

        // Decloak a cloaked unit
        if (isCloaked()) {
            specialAbilityActive = false;
            aiStance = preCloakAIStance != null ? preCloakAIStance : AIStance.DEFENSIVE;
        }

        // Fire the weapon
        return weapon.fire(getPosition(), targetPos, getId(), teamNumber, body, gameEntities);
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
            if (carriedResources < maxCarriedResources) {
                double harvestAmount = 10.0 * deltaTime;
                double actualHarvested = deposit.harvest(harvestAmount);
                carriedResources += actualHarvested;

                // If full or deposit depleted, signal to return to refinery
                return carriedResources >= maxCarriedResources || actualHarvested == 0; // Switch to returning resources
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

    /**
     * Issue a new command to this unit (replaces current command)
     */
    public void issueCommand(UnitCommand newCommand) {
        if (currentCommand != null) {
            currentCommand.onCancel(); // Clean up old command
        }
        currentCommand = newCommand;
    }

    /**
     * Check if this unit belongs to a specific player
     */
    public boolean belongsTo(int playerId) {
        return this.ownerId == playerId;
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

        return !isMoving
                && !isHarvesting
                && !isConstructing
                && targetUnit == null
                && targetBuilding == null
                && distanceFromHome > 50.0; // Return if more than 50 units from home
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
                    double attackRange = unitType.getAttackRange() * 1.5;
                    double damage = unitType.getDamage() * 1.5;
                    double attackRate = unitType.getAttackRate();
                    movementSpeed = 0; // Immobile when deployed

                    // Create 4 turrets at corners of the Crawler (RELATIVE OFFSETS, not absolute positions)
                    turrets.clear();
                    double turretOffset = unitType.getSize() * 0.6; // 60% of unit size
                    // Store as offsets from unit center, not absolute world positions
                    // Pass weapon stats to each turret
                    turrets.add(new Turret(0, new Vector2(turretOffset, turretOffset), damage, attackRange, attackRate));   // Top-right
                    turrets.add(new Turret(1, new Vector2(-turretOffset, turretOffset), damage, attackRange, attackRate));  // Top-left
                    turrets.add(new Turret(2, new Vector2(-turretOffset, -turretOffset), damage, attackRange, attackRate)); // Bottom-left
                    turrets.add(new Turret(3, new Vector2(turretOffset, -turretOffset), damage, attackRange, attackRate));  // Bottom-right

                    log.info("Crawler {} deployed - range: {}, damage: {}, turrets: {}", id, attackRange, damage, turrets.size());
                } else {
                    // Reset to normal stats and clear turrets
                    movementSpeed = unitType.getMovementSpeed();
                    turrets.clear();
                    log.info("Crawler {} undeployed - returning to mobile mode", id);
                }
                break;

            case CLOAK:
                // Cloak mode: invisible to enemies unless detected
                if (specialAbilityActive) {
                    log.info("Unit {} activated cloak", id);
                    // Reset last fire time to start cloak immediately
                    lastFireTime = 0;
                    // Save current AI stance and switch to PASSIVE to avoid accidental reveals
                    if (aiStance != AIStance.PASSIVE) {
                        preCloakAIStance = aiStance;
                        aiStance = AIStance.PASSIVE;
                        log.info("Unit {} switched to PASSIVE stance while cloaked (saved: {})", id, preCloakAIStance);
                    }
                } else {
                    log.info("Unit {} deactivated cloak", id);
                    // Restore previous AI stance when cloak is manually deactivated
                    if (preCloakAIStance != null) {
                        aiStance = preCloakAIStance;
                        log.info("Unit {} restored AI stance to {}", id, aiStance);
                        preCloakAIStance = null;
                    }
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

    /**
     * Apply research modifiers to this unit's stats
     * Called when unit is created or when research completes
     */
    public void applyResearchModifiers(ResearchModifier modifier) {
        // Apply health modifier (increase max health and current health proportionally)
        double healthMultiplier = modifier.getUnitHealthMultiplier();
        if (healthMultiplier != 1.0) {
            double healthPercent = health / maxHealth;
            maxHealth *= healthMultiplier;
            health = maxHealth * healthPercent;
        }

        if (weapon != null) {
            this.weapon = weapon.copyWithModifiers(modifier);
        }

        // Apply speed modifiers (infantry or vehicle)
        if (isInfantry()) {
            movementSpeed *= modifier.getInfantrySpeedMultiplier();
        } else if (isVehicle()) {
            movementSpeed *= modifier.getVehicleSpeedMultiplier();
        }

        // Apply worker capacity bonus
        if (unitType.canHarvest()) {
            maxCarriedResources = BASE_MAX_CARRIED_RESOURCES + modifier.getWorkerCapacityBonus();
        }

        // Apply vision range modifier
        visionRange *= modifier.getVisionRangeMultiplier();
    }

    /**
     * Check if this unit is infantry
     */
    public boolean isInfantry() {
        return switch (unitType) {
            case INFANTRY, LASER_INFANTRY, ROCKET_SOLDIER, SNIPER, MEDIC, ENGINEER, PLASMA_TROOPER, ION_RANGER -> true;
            default -> false;
        };
    }

    /**
     * Check if this unit is a vehicle
     */
    public boolean isVehicle() {
        return switch (unitType) {
            case JEEP, TANK, CRAWLER, CLOAK_TANK, PHOTON_SCOUT, BEAM_TANK -> true;
            default -> false;
        };
    }

    /**
     * Check if this unit is currently cloaked (invisible to enemies unless detected)
     * Cloak is disabled for CLOAK_DELAY_AFTER_FIRE ms after firing
     */
    public boolean isCloaked() {
        if (unitType != UnitType.CLOAK_TANK) {
            return false;
        }
        return specialAbilityActive;
    }

    /**
     * Get the detection range for cloaked units
     * Enemies within this range can see cloaked units
     */
    public static double getCloakDetectionRange() {
        return CLOAK_DETECTION_RANGE;
    }
}

