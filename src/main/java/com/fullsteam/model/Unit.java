package com.fullsteam.model;

import com.fullsteam.model.command.IdleCommand;
import com.fullsteam.model.command.SortieCommand;
import com.fullsteam.model.command.UnitCommand;
import com.fullsteam.model.component.AndroidComponent;
import com.fullsteam.model.component.CloakComponent;
import com.fullsteam.model.component.DeployComponent;
import com.fullsteam.model.component.HangarComponent;
import com.fullsteam.model.component.HarvestComponent;
import com.fullsteam.model.component.HealComponent;
import com.fullsteam.model.component.InterceptorComponent;
import com.fullsteam.model.component.IUnitComponent;
import com.fullsteam.model.component.MineComponent;
import com.fullsteam.model.component.RepairComponent;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    // Component system for modular behaviors
    private final Map<Class<? extends IUnitComponent>, IUnitComponent> components = new LinkedHashMap<>();

    // Legacy fields still in use - will be migrated to command system over time
    private Unit targetUnit = null; // Used by GarrisonComponent and serialization
    private Building targetBuilding = null; // Used by GarrisonComponent and serialization
    private boolean isMoving = false; // Used by pathfinding and serialization

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

    // Weapon system
    private Weapon weapon; // The weapon this unit fires (null for non-combat units)
    private double visionRange; // Modified by research

    // Building construction (for workers) - TODO: Move to ConstructComponent in future
    private boolean isConstructing = false;

    // Selection state
    private boolean selected = false;
    private boolean garrisoned = false; // True if unit is inside a building

    public Unit(int id, UnitType unitType, double x, double y, int ownerId, int teamNumber) {
        super(id, createUnitBody(x, y, unitType), unitType.getMaxHealth());
        this.unitType = unitType;
        this.ownerId = ownerId;
        this.teamNumber = teamNumber;
        this.movementSpeed = unitType.getMovementSpeed();
        this.visionRange = unitType.getVisionRange();
        this.weapon = WeaponFactory.getWeaponForUnitType(unitType);
        // Note: Components are initialized via initializeComponents() after construction
    }

    /**
     * Initialize components based on unit type.
     * Should be called immediately after construction with GameEntities reference.
     *
     * @param gameEntities Reference to all game entities
     */
    public void initializeComponents(GameEntities gameEntities) {
        // Add components based on unit capabilities
        if (unitType.canHarvest()) {
            addComponent(new HarvestComponent(), gameEntities);
        }

        if (unitType.canMine()) {
            addComponent(new MineComponent(), gameEntities);
        }

        if (unitType == UnitType.ANDROID) {
            addComponent(new AndroidComponent(), gameEntities);
        }

        if (unitType.canHeal()) {
            addComponent(new HealComponent(), gameEntities);
        }

        if (unitType.canRepair()) {
            addComponent(new RepairComponent(), gameEntities);
        }

        // Special abilities
        SpecialAbility ability = unitType.getSpecialAbility();
        if (ability == SpecialAbility.DEPLOY) {
            addComponent(new DeployComponent(), gameEntities);
        } else if (ability == SpecialAbility.CLOAK) {
            addComponent(new CloakComponent(), gameEntities);
        }
        
        // Air unit specific components
        if (unitType == UnitType.INTERCEPTOR) {
            addComponent(new InterceptorComponent(this), gameEntities);
        }

        log.debug("Unit {} initialized with {} components", id, components.size());
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

    // ==================== Component Management ====================

    /**
     * Add a component to this unit.
     * The component will be initialized immediately.
     *
     * @param component The component to add
     * @param <T>       The component type
     */
    public <T extends IUnitComponent> void addComponent(T component, GameEntities gameEntities) {
        components.put(component.getClass(), component);
        component.init(this, gameEntities);
        log.debug("Added component {} to unit {}", component.getClass().getSimpleName(), id);
    }

    /**
     * Get a component from this unit.
     *
     * @param componentClass The component class
     * @param <T>            The component type
     * @return The component, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T extends IUnitComponent> Optional<T> getComponent(Class<T> componentClass) {
        return Optional.ofNullable((T) components.get(componentClass));
    }

    /**
     * Check if this unit has a specific component.
     *
     * @param componentClass The component class
     * @return true if the component is present
     */
    public boolean hasComponent(Class<? extends IUnitComponent> componentClass) {
        return components.containsKey(componentClass);
    }

    /**
     * Remove a component from this unit.
     *
     * @param componentClass The component class to remove
     */
    public void removeComponent(Class<? extends IUnitComponent> componentClass) {
        IUnitComponent component = components.remove(componentClass);
        if (component != null) {
            component.onDestroy();
            log.debug("Removed component {} from unit {}", componentClass.getSimpleName(), id);
        }
    }

    // ==================== Update Logic ====================

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

        // Update all components
        for (IUnitComponent component : components.values()) {
            component.update(gameEntities);
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
        // Air units use gentler separation (can get close but slowly drift apart)
        // Ground units use stronger separation (avoid collisions more aggressively)
        boolean isAirborne = unitType.getElevation().isAirborne();
        if (nearbyUnits != null && !nearbyUnits.isEmpty()) {
            if (isAirborne) {
                // Air units: gentle separation, smaller radius, allows close formation
                separationForce = calculateSeparation(nearbyUnits, unitType.getSize() * 2.0);
            } else {
                // Ground units: stronger separation to avoid collisions
                separationForce = calculateSeparation(nearbyUnits, unitType.getSize() * 4.0);
            }
        }

        // Apply force to physics body
        body.applyForce(seekForce.multiply(body.getMass().getMass() * 60.0));
        
        // Air units get much weaker separation force (30 vs 80 for ground)
        if (isAirborne) {
            body.applyForce(separationForce.multiply(body.getMass().getMass() * 30.0));
        } else {
            body.applyForce(separationForce.multiply(body.getMass().getMass() * 80.0));
        }

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
        if (!path.isEmpty()) {
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

        // Notify cloak component of firing (will break cloak temporarily)
        getComponent(CloakComponent.class).ifPresent(CloakComponent::onFire);

        // Fire the weapon
        List<AbstractOrdinance> ordinances = weapon.fire(getPosition(), targetPos, getId(), teamNumber, body, gameEntities);
        
        // Notify interceptor component of weapon fire (consumes ammo)
        if (!ordinances.isEmpty()) {
            getComponent(InterceptorComponent.class).ifPresent(InterceptorComponent::onWeaponFired);
        }
        
        return ordinances;
    }

    /**
     * Harvest resources from deposit
     * Called by HarvestCommand
     *
     * @return true if should switch to returning resources
     */
    public boolean harvestResources(ResourceDeposit deposit, double deltaTime) {
        HarvestComponent harvestComp = getComponent(HarvestComponent.class).orElse(null);
        if (harvestComp == null) {
            log.warn("Unit {} attempted to harvest but has no HarvestComponent", id);
            return false;
        }

        // Check if in range
        Vector2 currentPos = getPosition();
        Vector2 depositPos = deposit.getPosition();
        double distance = currentPos.distance(depositPos);

        if (distance <= deposit.getHarvestRange() + unitType.getSize()) {
            // In range - stop and harvest
            body.setLinearVelocity(0, 0);
            return harvestComp.harvestFrom(deposit);
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
            HarvestComponent harvestComp = getComponent(HarvestComponent.class).orElse(null);
            if (harvestComp != null && harvestComp.hasResources()) {
                harvestComp.depositResources(refinery);
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
        MineComponent mineComp = getComponent(MineComponent.class).orElse(null);
        if (mineComp == null) {
            log.warn("Unit {} attempted to mine but has no MineComponent", id);
            return false;
        }

        // Check if in range
        Vector2 currentPos = getPosition();
        Vector2 obstaclePos = obstacle.getPosition();
        double distance = currentPos.distance(obstaclePos);
        double miningRange = obstacle.getBoundingRadius() + unitType.getSize() + 5;

        if (distance <= miningRange) {
            // In range - stop and mine
            body.setLinearVelocity(0, 0);

            // Face the obstacle
            Vector2 direction = obstaclePos.copy().subtract(currentPos);
            setRotation(Math.atan2(direction.y, direction.x));

            return mineComp.mineObstacle(obstacle);
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

            // Delegate to MineComponent
            MineComponent mineComp = getComponent(MineComponent.class).orElse(null);
            if (mineComp != null) {
                return mineComp.repairPickaxe(headquarters);
            }
        }

        return false; // Continue repairing
    }

    /**
     * Issue a new command to this unit (replaces current command)
     */
    public void issueCommand(UnitCommand newCommand, GameEntities gameEntities) {
        if (currentCommand != null) {
            currentCommand.onCancel(); // Clean up old command
        }
        newCommand.init(gameEntities); // Initialize command with game context
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
        if (currentCommand != null && currentCommand.isPlayerOrder()) {
            return false;
        }
        // Also check legacy isMoving flag (used by GarrisonComponent)
        if (isMoving) {
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

        // Don't interrupt player orders or construction
        if (currentCommand != null && currentCommand.isPlayerOrder()) {
            return false;
        }
        // Also check legacy isMoving flag (used by GarrisonComponent)
        if (isMoving || isConstructing) {
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

        // Check if unit is idle (no command or idle command) and not constructing
        // Also check legacy target fields (used by GarrisonComponent)
        boolean isIdle = (currentCommand == null || currentCommand instanceof IdleCommand);

        return isIdle
                && !isMoving
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
                    getComponent(DeployComponent.class).ifPresent(DeployComponent::toggleDeploy);
                }
                break;

            case CLOAK:
                getComponent(CloakComponent.class).ifPresent(CloakComponent::toggleCloak);
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

        // Worker capacity bonus is now handled by HarvestComponent
        // (already delegated via component.applyResearchModifiers)

        // Apply vision range modifier
        visionRange *= modifier.getVisionRangeMultiplier();

        // Apply modifiers to all components
        for (IUnitComponent component : components.values()) {
            component.applyResearchModifiers(modifier);
        }
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
     * Delegates to CloakComponent
     */
    public boolean isCloaked() {
        return getComponent(CloakComponent.class)
                .map(CloakComponent::isCloaked)
                .orElse(false);
    }

    /**
     * Check if this unit's weapon can target another unit at its elevation.
     * 
     * @param target The potential target unit
     * @return true if this unit's weapon can hit the target's elevation
     */
    public boolean canTargetElevation(Unit target) {
        if (weapon == null || target == null) {
            return false;
        }
        return weapon.getElevationTargeting().canTarget(target.getUnitType().getElevation());
    }
    
    /**
     * Check if a given weapon can target a unit at its elevation.
     * Static helper for use by buildings/turrets.
     * 
     * @param weapon The weapon attempting to target
     * @param target The potential target unit
     * @return true if the weapon can hit the target's elevation
     */
    public static boolean canWeaponTargetUnit(Weapon weapon, Unit target) {
        if (weapon == null || target == null) {
            return false;
        }
        return weapon.getElevationTargeting().canTarget(target.getUnitType().getElevation());
    }

    /**
     * Get the detection range for cloaked units
     * Enemies within this range can see cloaked units
     */
    public static double getCloakDetectionRange() {
        return CloakComponent.getDetectionRange();
    }
}

