package com.fullsteam.model;

import com.fullsteam.model.component.AndroidFactoryComponent;
import com.fullsteam.model.component.BankComponent;
import com.fullsteam.model.component.DefenseComponent;
import com.fullsteam.model.component.IBuildingComponent;
import com.fullsteam.model.component.ProductionComponent;
import com.fullsteam.model.component.ResearchComponent;
import com.fullsteam.model.component.SandstormComponent;
import com.fullsteam.model.component.ShieldComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a building in the RTS game.
 */
@Slf4j
@Getter
@Setter
public class Building extends GameEntity {
    private final BuildingType buildingType;
    private final int ownerId; // Player who owns this building
    private final int teamNumber;

    // Component system for modular building behavior
    private final Map<Class<? extends IBuildingComponent>, IBuildingComponent> components = new HashMap<>();

    // Construction state
    private boolean underConstruction = true;
    private double constructionProgress = 0; // 0 to maxHealth

    // Rally point for produced units
    private Vector2 rallyPoint = null;

    // Garrison fields (for Bunker)
    private final List<Unit> garrisonedUnits = new ArrayList<>();
    /**
     * -- SETTER --
     * Set the maximum garrison capacity
     */
    private int maxGarrisonCapacity = 0; // Set by building type

    // Monument aura fields
    private Body auraSensorBody = null;
    private boolean auraActive = false;
    private static final int COMMAND_CITADEL_UPKEEP_BONUS = 50; // +50 max upkeep

    // Photon Spire combat stats (Obelisk of Light style)
    private static final double PHOTON_SPIRE_DAMAGE = 250.0; // Massive damage per shot
    private static final double PHOTON_SPIRE_ATTACK_RATE = 3.5; // Very slow fire rate (one shot every 3.5 seconds)
    private static final double PHOTON_SPIRE_RANGE = 400.0; // Very long range
    private Unit photonSpireTarget = null; // Current target for Photon Spire
    private double photonSpireAttackCooldown = 0.0; // Time until next shot

    public Building(int id, BuildingType buildingType, double x, double y, int ownerId, int teamNumber) {
        this(id, buildingType, x, y, ownerId, teamNumber, buildingType.getMaxHealth());
    }

    /**
     * Constructor with custom max health (for faction modifiers)
     */
    public Building(int id, BuildingType buildingType, double x, double y, int ownerId, int teamNumber, double maxHealth) {
        super(id, createBuildingBody(x, y, buildingType), maxHealth);
        this.buildingType = buildingType;
        this.ownerId = ownerId;
        this.teamNumber = teamNumber;

        // Set garrison capacity from building type
        this.maxGarrisonCapacity = buildingType.getGarrisonCapacity();

        // Headquarters starts fully constructed
        if (buildingType == BuildingType.HEADQUARTERS) {
            underConstruction = false;
            constructionProgress = maxHealth;
            health = maxHealth;
        } else {
            // Buildings start with minimal health and construction progress during construction
            // Must be > 0 to stay active and be targetable
            health = 1;
            constructionProgress = 1;
        }

        // Set rally point to building position by default
        this.rallyPoint = new Vector2(x, y);

        // Initialize components based on building type
        initializeComponents(new Vector2(x, y));
    }

    /**
     * Initialize components based on building type.
     * This method adds the appropriate components to buildings that need them.
     */
    private void initializeComponents(Vector2 position) {
        // Add ProductionComponent to buildings that can produce units (except Android Factory)
        if (buildingType.isCanProduceUnits() && buildingType != BuildingType.ANDROID_FACTORY) {
            addComponent(new ProductionComponent(position));
            log.debug("Building {} ({}) initialized with ProductionComponent", id, buildingType.getDisplayName());
        }

        // Add BankComponent to bank buildings
        if (buildingType == BuildingType.BANK) {
            addComponent(new BankComponent());
            log.debug("Building {} ({}) initialized with BankComponent", id, buildingType.getDisplayName());
        }

        // Add DefenseComponent to defensive buildings
        if (buildingType.isDefensive()) {
            addComponent(new DefenseComponent());
            log.debug("Building {} ({}) initialized with DefenseComponent", id, buildingType.getDisplayName());
        }

        // Add ShieldComponent to shield generator buildings
        if (buildingType == BuildingType.SHIELD_GENERATOR) {
            addComponent(new ShieldComponent());
            log.debug("Building {} ({}) initialized with ShieldComponent", id, buildingType.getDisplayName());
        }

        // Add AndroidFactoryComponent to Android Factory
        if (buildingType == BuildingType.ANDROID_FACTORY) {
            addComponent(new AndroidFactoryComponent());
            log.debug("Building {} ({}) initialized with AndroidFactoryComponent", id, buildingType.getDisplayName());
        }

        // Add ResearchComponent to research buildings
        if (buildingType == BuildingType.RESEARCH_LAB || buildingType == BuildingType.TECH_CENTER) {
            addComponent(new ResearchComponent(this));
            log.debug("Building {} ({}) initialized with ResearchComponent", id, buildingType.getDisplayName());
        }

        // Add SandstormComponent to Sandstorm Generator
        if (buildingType == BuildingType.SANDSTORM_GENERATOR) {
            addComponent(new SandstormComponent());
            log.debug("Building {} ({}) initialized with SandstormComponent", id, buildingType.getDisplayName());
        }

        // More components will be added here as we extract them:
        // - GarrisonComponent for BUNKER
        // - AuraComponent for monuments
    }

    private static Body createBuildingBody(double x, double y, BuildingType buildingType) {
        Body body = new Body();

        // Use custom physics fixtures from BuildingType (supports multi-fixture compound shapes)
        List<Convex> shapes = buildingType.createPhysicsFixtures();

        // Add all fixtures to the body
        for (Convex shape : shapes) {
            BodyFixture fixture = body.addFixture(shape);

            // Configure fixture properties
            fixture.setFriction(0.1);      // Low friction
            fixture.setRestitution(0.0);   // No bounce
            fixture.setSensor(false);      // Solid collision (not a sensor)
        }

        body.setMass(MassType.INFINITE); // Buildings don't move
        body.getTransform().setTranslation(x, y);
        return body;
    }

    @Override
    public void update(GameEntities gameEntities) {
        update(gameEntities, gameEntities.getWorld().getTimeStep().getDeltaTime(), false); // Default: no low power
    }

    /**
     * Update building with power status
     */
    public void update(GameEntities gameEntities, double deltaTime, boolean hasLowPower) {
        if (!active) {
            return;
        }

        // Update construction progress
        if (underConstruction) {
            // Construction is handled by worker units
            // Health increases as construction progresses
            // Ensure health is at least 1 so building stays active and targetable
            health = Math.max(1, constructionProgress);

            if (constructionProgress >= maxHealth) {
                completeConstruction();
            }
            return; // Don't do anything else while under construction
        }

        // Update all components
        for (IBuildingComponent component : components.values()) {
            component.update(gameEntities, this, hasLowPower);
        }

        // Update bunker behavior (garrisoned units fire)
        if (buildingType == BuildingType.BUNKER) {
            updateBunkerBehavior(deltaTime);
        }

        // Update monument auras (deactivate if low power)
        if (isMonument()) {
            updateMonumentAura(hasLowPower);
        }
    }

    /**
     * Check if bank is ready to pay interest and reset timer
     * Returns the interest rate to apply (0.0 if not ready)
     */
    public double checkAndResetBankInterest() {
        BankComponent bankComp =
                getComponent(BankComponent.class);
        if (bankComp != null) {
            return bankComp.checkAndResetInterest();
        }
        return 0.0; // No bank component
    }

    /**
     * Complete construction of this building
     *
     * @return true if construction was just completed (for triggering post-construction logic)
     */
    public boolean completeConstruction() {
        if (underConstruction) {
            underConstruction = false;
            health = maxHealth;
            constructionProgress = maxHealth;

            // Notify all components that construction is complete
            for (IBuildingComponent component : components.values()) {
                component.onConstructionComplete(this);
            }

            return true; // Construction just completed
        }
        return false; // Already completed
    }

    /**
     * Add construction progress (called by worker units)
     */
    public void addConstructionProgress(double amount) {
        if (!underConstruction) {
            return;
        }
        constructionProgress = Math.min(maxHealth, constructionProgress + amount);
        // Update health to match construction progress
        health = constructionProgress;
    }

    /**
     * Check if construction is complete
     */
    public boolean isConstructionComplete() {
        return !underConstruction || constructionProgress >= maxHealth;
    }

    /**
     * Get construction progress as a percentage
     */
    public double getConstructionPercent() {
        return constructionProgress / maxHealth;
    }


    /**
     * Queue a unit for production
     * Note: Validation should be done by the caller (RTSGameManager) using faction tech tree
     *
     * @return true if successfully queued
     */
    public boolean queueUnitProduction(UnitType unitType) {
        ProductionComponent prodComp =
                getComponent(ProductionComponent.class);
        if (prodComp != null) {
            return prodComp.queueUnitProduction(unitType);
        }
        return false; // Building doesn't have production capability
    }

    /**
     * Cancel current production and refund resources
     */
    public UnitType cancelCurrentProduction() {
        ProductionComponent prodComp =
                getComponent(ProductionComponent.class);
        if (prodComp != null) {
            return prodComp.cancelCurrentProduction();
        }
        return null; // No production component
    }

    /**
     * Get production progress as a percentage
     */
    public double getProductionPercent() {
        // Check ProductionComponent first
        ProductionComponent prodComp =
                getComponent(ProductionComponent.class);
        if (prodComp != null) {
            return prodComp.getProductionPercent();
        }

        // Check AndroidFactoryComponent
        AndroidFactoryComponent androidComp =
                getComponent(AndroidFactoryComponent.class);
        if (androidComp != null) {
            return androidComp.getProductionProgressPercent();
        }

        return 0.0; // No production component
    }

    /**
     * Get the number of units in the production queue (excluding current production)
     */
    public int getProductionQueueSize() {
        ProductionComponent prodComp =
                getComponent(ProductionComponent.class);
        if (prodComp != null) {
            return prodComp.getProductionQueueSize();
        }
        return 0; // No production component
    }

    /**
     * Check if a unit is ready to be spawned
     */
    public boolean hasCompletedUnit() {
        ProductionComponent prodComp =
                getComponent(ProductionComponent.class);
        if (prodComp != null) {
            return prodComp.hasCompletedUnit();
        }
        return false; // No production component
    }

    /**
     * Get the completed unit type and clear production
     */
    public UnitType getCompletedUnitType() {
        ProductionComponent prodComp =
                getComponent(ProductionComponent.class);
        if (prodComp != null) {
            return prodComp.getCompletedUnitType();
        }
        return null; // No production component
    }

    /**
     * Set rally point for produced units
     */
    public void setRallyPoint(Vector2 point) {
        this.rallyPoint = point != null ? point.copy() : null;

        // Also update the ProductionComponent if present
        ProductionComponent prodComp =
                getComponent(ProductionComponent.class);
        if (prodComp != null) {
            prodComp.setRallyPoint(point);
        }
    }

    /**
     * Check if this building belongs to a specific player
     */
    public boolean belongsTo(int playerId) {
        return this.ownerId == playerId;
    }

    /**
     * Check if this building is on a specific team
     */
    public boolean isOnTeam(int team) {
        return this.teamNumber == team;
    }


    // ==================== MONUMENT METHODS ====================

    /**
     * Check if this is a monument building
     */
    public boolean isMonument() {
        return buildingType == BuildingType.PHOTON_SPIRE ||
                buildingType == BuildingType.ANDROID_FACTORY ||
                buildingType == BuildingType.SANDSTORM_GENERATOR ||
                buildingType == BuildingType.COMMAND_CITADEL;
    }

    /**
     * Create aura sensor body for monument buildings
     */
    public Body createAuraSensorBody() {
        if (!isMonument()) {
            return null;
        }

        double radius = getAuraRadius();
        if (radius == 0.0) {
            return null;
        }

        Body sensor = new Body();
        sensor.addFixture(Geometry.createCircle(radius), 0.0, 0.0, 0.0);
        sensor.getFixture(0).setSensor(true); // Make it a sensor (no collision response)
        sensor.setMass(MassType.INFINITE);
        sensor.getTransform().setTranslation(getPosition().x, getPosition().y);
        sensor.setUserData(this); // Link back to building

        return sensor;
    }

    /**
     * Update monument aura state (activate/deactivate based on power)
     */
    private void updateMonumentAura(boolean hasLowPower) {
        boolean shouldBeActive = !hasLowPower && !underConstruction;
        if (shouldBeActive && !auraActive) {
            activateAura();
        } else if (!shouldBeActive && auraActive) {
            deactivateAura();
        }
    }

    /**
     * Activate the monument aura
     */
    public void activateAura() {
        if (!isMonument() || auraActive) {
            return;
        }
        auraActive = true;
        log.debug("Monument aura activated for building {} ({})", id, buildingType);
    }

    /**
     * Deactivate the monument aura
     */
    public void deactivateAura() {
        if (!auraActive) {
            return;
        }

        auraActive = false;
        log.debug("Monument aura deactivated for building {}", id);
    }

    /**
     * Get the aura radius for this monument
     */
    public double getAuraRadius() {
        return switch (buildingType) {
            case SANDSTORM_GENERATOR -> {
                SandstormComponent comp = getComponent(SandstormComponent.class);
                yield comp != null ? 300.0 : 0.0; // Sandstorm radius
            }
            default -> 0.0;
        };
    }

    /**
     * Get the upkeep bonus from Command Citadel
     */
    public int getUpkeepBonus() {
        if (buildingType == BuildingType.COMMAND_CITADEL && auraActive) {
            return COMMAND_CITADEL_UPKEEP_BONUS;
        }
        return 0;
    }

    /**
     * Get attack range for Photon Spire
     */
    public double getPhotonSpireRange() {
        return PHOTON_SPIRE_RANGE;
    }

    /**
     * Get target for Photon Spire or turrets
     */
    public Unit getTargetUnit() {
        // Check if this is a Photon Spire
        if (buildingType == BuildingType.PHOTON_SPIRE) {
            return photonSpireTarget;
        }

        // Otherwise delegate to DefenseComponent
        DefenseComponent defComp =
                getComponent(DefenseComponent.class);
        if (defComp != null) {
            return defComp.getTargetUnit();
        }
        return null;
    }

    /**
     * Set target for Photon Spire or turrets
     */
    public void setTargetUnit(Unit target) {
        // Check if this is a Photon Spire
        if (buildingType == BuildingType.PHOTON_SPIRE) {
            this.photonSpireTarget = target;
            return;
        }

        // Otherwise delegate to DefenseComponent
        DefenseComponent defComp =
                getComponent(DefenseComponent.class);
        if (defComp != null) {
            defComp.setTargetUnit(target);
        }
    }

    /**
     * Update Photon Spire attack behavior - fires powerful beam at high-health targets
     *
     * @param world The physics world for raycasting
     * @return Beam if fired, null otherwise
     */
    public Beam updatePhotonSpireBehavior(double deltaTime, World<Body> world) {
        if (buildingType != BuildingType.PHOTON_SPIRE || underConstruction) {
            return null;
        }

        // Update attack cooldown
        if (photonSpireAttackCooldown > 0) {
            photonSpireAttackCooldown -= deltaTime;
        }

        // Check if we have a valid target and can fire
        if (photonSpireTarget != null && photonSpireTarget.isActive() && photonSpireAttackCooldown <= 0) {
            // Check if target is in range
            double distance = getPosition().distance(photonSpireTarget.getPosition());
            if (distance <= PHOTON_SPIRE_RANGE) {
                // Calculate direction to target
                Vector2 direction = photonSpireTarget.getPosition().copy().subtract(getPosition());

                // Fire beam!
                Beam beam = new Beam(
                        world,
                        getPosition().copy(),
                        direction,
                        PHOTON_SPIRE_RANGE,
                        getOwnerId(),
                        getTeamNumber(),
                        PHOTON_SPIRE_DAMAGE,
                        Set.of(), // No special effects
                        Ordinance.LASER,
                        Beam.BeamType.LASER,
                        3.0, // Beam width (thick, powerful beam)
                        0.3, // Duration (visible for 0.3 seconds)
                        getBody() // Ignore self
                );

                // Reset cooldown
                photonSpireAttackCooldown = PHOTON_SPIRE_ATTACK_RATE;

                log.debug("Photon Spire {} fired beam at unit {} for {} damage",
                        getId(), photonSpireTarget.getId(), PHOTON_SPIRE_DAMAGE);

                return beam;
            }
        }

        return null;
    }

    /**
     * Get sandstorm damage per second (for continuous damage)
     *
     * @deprecated Sandstorm damage is now handled by SandstormComponent
     */
    @Deprecated
    public double getSandstormDPS() {
        return 15.0; // Legacy constant for backwards compatibility
    }

    /**
     * Garrison a unit inside this building (for Bunker)
     *
     * @return true if successfully garrisoned, false if full or not allowed
     */
    public boolean garrisonUnit(Unit unit) {
        if (maxGarrisonCapacity == 0) {
            return false; // Building doesn't support garrison
        }

        if (garrisonedUnits.size() >= maxGarrisonCapacity) {
            return false; // Garrison is full
        }

        if (unit.getTeamNumber() != teamNumber) {
            return false; // Can only garrison friendly units
        }

        // Only infantry units can be garrisoned
        if (!unit.getUnitType().isInfantry()) {
            return false;
        }

        garrisonedUnits.add(unit);
        unit.setGarrisoned(true);
        // Don't set active=false! That would cause the unit to be deleted by removeInactiveEntities()
        // Instead, we'll filter garrisoned units from serialization
        unit.getBody().setEnabled(false); // Disable physics

        log.info("Unit {} garrisoned in building {}", unit.getId(), id);
        return true;
    }

    /**
     * Ungarrison a unit from this building
     *
     * @param unit The unit to ungarrison (null = ungarrison first unit)
     * @return The ungarrisoned unit, or null if none available
     */
    public Unit ungarrisonUnit(Unit unit) {
        if (garrisonedUnits.isEmpty()) {
            return null;
        }

        Unit toUngarrison;
        if (unit != null && garrisonedUnits.contains(unit)) {
            toUngarrison = unit;
            garrisonedUnits.remove(unit);
        } else {
            // Ungarrison first unit in list
            toUngarrison = garrisonedUnits.remove(0);
        }

        // Place unit near building exit
        Vector2 exitPos = calculateExitPosition();
        toUngarrison.getBody().getTransform().setTranslation(exitPos.x, exitPos.y);
        toUngarrison.setGarrisoned(false);
        // Unit is already active, just re-enable physics
        toUngarrison.getBody().setEnabled(true);

        log.info("Unit {} ungarrisoned from building {}", toUngarrison.getId(), id);
        return toUngarrison;
    }

    /**
     * Ungarrison all units from this building
     *
     * @return List of ungarrisoned units
     */
    public List<Unit> ungarrisonAllUnits() {
        List<Unit> ungarrisoned = new ArrayList<>();
        while (!garrisonedUnits.isEmpty()) {
            Unit unit = ungarrisonUnit(null);
            if (unit != null) {
                ungarrisoned.add(unit);
            }
        }
        return ungarrisoned;
    }

    /**
     * Calculate exit position for ungarrisoning units
     */
    private Vector2 calculateExitPosition() {
        Vector2 pos = getPosition();
        double size = buildingType.getSize();

        // Place units at a random angle around the building
        double angle = Math.random() * Math.PI * 2;
        double distance = size + 30.0; // Place outside building radius

        return new Vector2(
                pos.x + Math.cos(angle) * distance,
                pos.y + Math.sin(angle) * distance
        );
    }

    /**
     * Get number of garrisoned units
     */
    public int getGarrisonCount() {
        return garrisonedUnits.size();
    }

    /**
     * Check if garrison is full
     */
    public boolean isGarrisonFull() {
        return garrisonedUnits.size() >= maxGarrisonCapacity;
    }

    /**
     * Check if this building can garrison units
     */
    public boolean canGarrison() {
        return maxGarrisonCapacity > 0;
    }

    /**
     * Get list of garrisoned units (read-only)
     */
    public List<Unit> getGarrisonedUnits() {
        return new ArrayList<>(garrisonedUnits);
    }

    /**
     * Update bunker behavior - garrisoned units independently acquire targets and fire
     */
    private void updateBunkerBehavior(double deltaTime) {
        // Each garrisoned unit operates independently
        // Target acquisition and firing is handled in fireBunkerWeapons()
    }

    /**
     * Fire weapons from garrisoned units (called by RTSGameManager)
     * Each unit independently acquires targets and fires based on its own stats
     */
    public List<AbstractOrdinance> fireBunkerWeapons(GameEntities gameEntities) {
        List<AbstractOrdinance> ordinances = new ArrayList<>();

        if (buildingType != BuildingType.BUNKER || garrisonedUnits.isEmpty() || gameEntities == null) {
            return ordinances;
        }

        Vector2 bunkerPos = getPosition();

        // Each garrisoned unit operates independently
        for (Unit garrisonedUnit : garrisonedUnits) {
            // Skip if unit can't attack
            if (!garrisonedUnit.getUnitType().canAttack()) {
                continue;
            }

            // Clear invalid target
            if (garrisonedUnit.getTargetUnit() != null && !garrisonedUnit.getTargetUnit().isActive()) {
                garrisonedUnit.setTargetUnit(null);
            }

            // Acquire target if needed (each unit finds its own target)
            if (garrisonedUnit.getTargetUnit() == null) {
                Unit target = findBunkerTargetForUnit(garrisonedUnit, gameEntities);
                garrisonedUnit.setTargetUnit(target);
            }

            Unit target = garrisonedUnit.getTargetUnit();
            if (target == null || !target.isActive()) {
                continue;
            }

            // Check if target is in range (use garrisoned unit's range)
            Vector2 targetPos = target.getPosition();
            double distance = bunkerPos.distance(targetPos);
            double attackRange = garrisonedUnit.getUnitType().getAttackRange();

            if (distance > attackRange) {
                garrisonedUnit.setTargetUnit(null); // Target out of range
                continue;
            }

            // Check attack cooldown (each unit has its own)
            long now = System.currentTimeMillis();
            double attackInterval = 1000.0 / garrisonedUnit.getUnitType().getAttackRate();
            if (now - garrisonedUnit.getLastAttackTime() < attackInterval) {
                continue; // Still on cooldown
            }

            // Fire weapon from bunker position
            AbstractOrdinance ordinance = garrisonedUnit.fireAt(targetPos, gameEntities.getWorld());
            if (ordinance != null) {
                // Override ordinance position to fire from bunker
                if (ordinance instanceof Projectile projectile) {
                    projectile.getBody().getTransform().setTranslation(bunkerPos.x, bunkerPos.y);
                } else if (ordinance instanceof Beam beam) {
                    // Beams are created with raycasting from origin
                    // The beam was already created from the garrisoned unit's position
                    // but we want it to appear from the bunker
                    // This is handled correctly by the beam's raycasting
                }
                ordinances.add(ordinance);

                // Update unit's last attack time
                garrisonedUnit.setLastAttackTime(now);
            }
        }

        return ordinances;
    }

    /**
     * Find a target for a specific garrisoned unit
     * Each unit independently scans for enemies in its range
     */
    private Unit findBunkerTargetForUnit(Unit garrisonedUnit, GameEntities gameEntities) {
        Vector2 bunkerPos = getPosition();
        double attackRange = garrisonedUnit.getUnitType().getAttackRange();

        Unit closestEnemy = null;
        double closestDistance = Double.MAX_VALUE;

        // Scan for enemies in range
        for (Unit enemy : gameEntities.getUnits().values()) {
            if (!enemy.isActive() || enemy.getTeamNumber() == teamNumber || enemy.isGarrisoned()) {
                continue;
            }

            double distance = bunkerPos.distance(enemy.getPosition());
            if (distance <= attackRange && distance < closestDistance) {
                closestEnemy = enemy;
                closestDistance = distance;
            }
        }

        return closestEnemy;
    }

    // ========================================
    // Component Management
    // ========================================

    /**
     * Add a component to this building.
     * Components provide modular functionality (production, defense, etc.)
     *
     * @param component The component to add
     * @param <T>       The component type
     */
    public <T extends IBuildingComponent> void addComponent(T component) {
        if (component != null) {
            components.put(component.getClass(), component);
            log.debug("Added component {} to building {}", component.getClass().getSimpleName(), id);
        }
    }

    /**
     * Get a component by its class type.
     *
     * @param componentClass The class of the component to retrieve
     * @param <T>            The component type
     * @return The component, or null if not present
     */
    public <T extends IBuildingComponent> T getComponent(Class<T> componentClass) {
        return componentClass.cast(components.get(componentClass));
    }

    /**
     * Check if this building has a specific component.
     *
     * @param componentClass The class of the component to check
     * @return true if the component is present
     */
    public boolean hasComponent(Class<? extends IBuildingComponent> componentClass) {
        return components.containsKey(componentClass);
    }

    /**
     * Remove a component from this building.
     *
     * @param componentClass The class of the component to remove
     * @param <T>            The component type
     * @return The removed component, or null if not present
     */
    public <T extends IBuildingComponent> T removeComponent(Class<T> componentClass) {
        IBuildingComponent removed = components.remove(componentClass);
        if (removed != null) {
            log.debug("Removed component {} from building {}", componentClass.getSimpleName(), id);
        }
        return componentClass.cast(removed);
    }

    /**
     * Get all components attached to this building.
     *
     * @return Collection of all components
     */
    public Iterable<IBuildingComponent> getAllComponents() {
        return components.values();
    }

    /**
     * Represents a unit production order
     */
    @Getter
    public static class ProductionOrder {
        private final UnitType unitType;
        private final long queueTime;

        public ProductionOrder(UnitType unitType) {
            this.unitType = unitType;
            this.queueTime = System.currentTimeMillis();
        }
    }
}

