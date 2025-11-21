package com.fullsteam.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
    
    // Construction state
    private boolean underConstruction = true;
    private double constructionProgress = 0; // 0 to maxHealth
    
    // Production queue (for buildings that produce units)
    private final Queue<ProductionOrder> productionQueue = new LinkedList<>();
    private ProductionOrder currentProduction = null;
    private double productionProgress = 0; // seconds
    
    // Turret behavior (for defensive buildings)
    private Unit targetUnit = null;
    private long lastAttackTime = 0;
    private static final double TURRET_DAMAGE = 25;
    private static final double TURRET_ATTACK_RATE = 2.0; // attacks per second
    private static final double TURRET_RANGE = 300;
    
    // Rally point for produced units
    private Vector2 rallyPoint = null;
    
    // Shield Generator fields
    private Body shieldSensorBody = null;
    private boolean shieldActive = false;
    private static final double SHIELD_RADIUS = 200.0; // Shield projection radius
    
    // Bank fields
    private double bankInterestTimer = 0; // Time accumulator for interest payments
    private static final double BANK_INTEREST_INTERVAL = 30.0; // Pay interest every 30 seconds
    private static final double BANK_INTEREST_RATE = 0.02; // 2% interest per interval
    
    // Garrison fields (for Bunker)
    private final List<Unit> garrisonedUnits = new ArrayList<>();
    private int maxGarrisonCapacity = 0; // Set by building type
    
    // Monument aura fields
    private Body auraSensorBody = null;
    private boolean auraActive = false;
    private static final double PHOTON_SPIRE_RADIUS = 250.0; // Beam damage amplification radius
    private static final double QUANTUM_NEXUS_RADIUS = 220.0; // Shield/armor bonus radius
    private static final double SANDSTORM_RADIUS = 200.0; // Sandstorm damage radius
    private static final double PHOTON_SPIRE_DAMAGE_MULTIPLIER = 1.35; // +35% beam damage
    private static final double QUANTUM_NEXUS_HEALTH_MULTIPLIER = 1.25; // +25% max health
    private double sandstormTimer = 0; // Time accumulator for sandstorm pulses
    private static final double SANDSTORM_INTERVAL = 3.0; // Damage every 3 seconds
    private static final double SANDSTORM_DAMAGE = 15.0; // Damage per pulse
    
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
            health = 1; // Buildings start with minimal health during construction
        }
        
        // Set rally point to building position by default
        this.rallyPoint = new Vector2(x, y);
    }
    
    private static Body createBuildingBody(double x, double y, BuildingType buildingType) {
        Body body = new Body();
        
        // Create polygon fixture based on building sides (like units)
        int sides = buildingType.getSides();
        double radius = buildingType.getSize();
        org.dyn4j.geometry.Convex shape = Geometry.createPolygonalCircle(sides, radius);
        org.dyn4j.dynamics.BodyFixture fixture = body.addFixture(shape);
        
        // Configure fixture properties
        fixture.setFriction(0.1);      // Low friction
        fixture.setRestitution(0.0);   // No bounce
        fixture.setSensor(false);      // Solid collision (not a sensor)
        
        body.setMass(MassType.INFINITE); // Buildings don't move
        body.getTransform().setTranslation(x, y);
        return body;
    }
    
    @Override
    public void update(double deltaTime) {
        update(deltaTime, false); // Default: no low power
    }
    
    /**
     * Update building with power status
     */
    public void update(double deltaTime, boolean hasLowPower) {
        super.update(deltaTime);
        
        if (!active) {
            return;
        }
        
        // Update construction progress
        if (underConstruction) {
            // Construction is handled by worker units
            // Health increases as construction progresses
            health = constructionProgress;
            
            if (constructionProgress >= maxHealth) {
                completeConstruction();
            }
            return; // Don't do anything else while under construction
        }
        
        // Update production queue (only if not low power)
        if (buildingType.isCanProduceUnits()) {
            updateProduction(deltaTime, hasLowPower);
        }
        
        // Update turret behavior (projectile firing handled by RTSGameManager)
        // This just updates targeting and rotation
        if (buildingType.isDefensive()) {
            updateTurretBehavior(deltaTime);
        }
        
        // Update bunker behavior (garrisoned units fire)
        if (buildingType == BuildingType.BUNKER) {
            updateBunkerBehavior(deltaTime);
        }
        
        // Update shield generator (deactivate if low power)
        if (buildingType == BuildingType.SHIELD_GENERATOR) {
            updateShield(hasLowPower);
        }
        
        // Update bank (accumulate interest timer)
        if (buildingType == BuildingType.BANK && !hasLowPower) {
            bankInterestTimer += deltaTime;
        }
        
        // Update monument auras (deactivate if low power)
        if (isMonument()) {
            updateMonumentAura(hasLowPower);
        }
        
        // Update sandstorm timer
        if (buildingType == BuildingType.SANDSTORM_GENERATOR && !hasLowPower && !underConstruction) {
            sandstormTimer += deltaTime;
        }
    }
    
    /**
     * Check if bank is ready to pay interest and reset timer
     * Returns the interest rate to apply (0.0 if not ready)
     */
    public double checkAndResetBankInterest() {
        if (buildingType != BuildingType.BANK || underConstruction) {
            return 0.0;
        }
        
        if (bankInterestTimer >= BANK_INTEREST_INTERVAL) {
            bankInterestTimer = 0;
            return BANK_INTEREST_RATE;
        }
        
        return 0.0;
    }
    
    /**
     * Complete construction of this building
     * @return true if construction was just completed (for triggering post-construction logic)
     */
    public boolean completeConstruction() {
        if (underConstruction) {
            underConstruction = false;
            health = maxHealth;
            constructionProgress = maxHealth;
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
     * Update unit production
     */
    private void updateProduction(double deltaTime, boolean hasLowPower) {
        // Start next production if none active
        if (currentProduction == null && !productionQueue.isEmpty()) {
            currentProduction = productionQueue.poll();
            productionProgress = 0;
            log.info("Building {} started producing {}", id, currentProduction.unitType);
        }
        
        // Update current production (only if not low power)
        if (currentProduction != null && !hasLowPower) {
            productionProgress += deltaTime;
            
            // Check if production is complete
            if (productionProgress >= currentProduction.unitType.getBuildTimeSeconds()) {
                log.info("Building {} completed producing {} (progress: {}/{})", 
                        id, currentProduction.unitType, productionProgress, currentProduction.unitType.getBuildTimeSeconds());
                // Production complete - unit will be spawned by RTSGameManager
                // Don't clear it here - let getCompletedUnitType() handle it
            }
        } else if (hasLowPower && currentProduction != null) {
            // Production is paused due to low power
            log.debug("Building {} production paused due to LOW POWER", id);
        }
    }
    
    /**
     * Queue a unit for production
     * Note: Validation should be done by the caller (RTSGameManager) using faction tech tree
     * @return true if successfully queued
     */
    public boolean queueUnitProduction(UnitType unitType) {
        productionQueue.add(new ProductionOrder(unitType));
        return true;
    }
    
    /**
     * Cancel current production and refund resources
     */
    public UnitType cancelCurrentProduction() {
        if (currentProduction != null) {
            UnitType cancelled = currentProduction.unitType;
            currentProduction = null;
            productionProgress = 0;
            return cancelled;
        }
        return null;
    }
    
    /**
     * Get production progress as a percentage
     */
    public double getProductionPercent() {
        if (currentProduction == null) {
            return 0;
        }
        return productionProgress / currentProduction.unitType.getBuildTimeSeconds();
    }
    
    /**
     * Get the number of units in the production queue (excluding current production)
     */
    public int getProductionQueueSize() {
        return productionQueue.size();
    }
    
    /**
     * Check if a unit is ready to be spawned
     */
    public boolean hasCompletedUnit() {
        return currentProduction != null && productionProgress >= currentProduction.unitType.getBuildTimeSeconds();
    }
    
    /**
     * Get the completed unit type and clear production
     */
    public UnitType getCompletedUnitType() {
        if (hasCompletedUnit()) {
            UnitType completed = currentProduction.unitType;
            currentProduction = null;
            productionProgress = 0;
            return completed;
        }
        return null;
    }
    
    /**
     * Update turret targeting and attack behavior
     * @return Projectile if fired, null otherwise
     */
    public Projectile updateTurretBehavior(double deltaTime) {
        // Target acquisition is handled by RTSGameManager
        if (targetUnit != null && targetUnit.isActive()) {
            Vector2 turretPos = getPosition();
            Vector2 targetPos = targetUnit.getPosition();
            double distance = turretPos.distance(targetPos);
            
            // Check if target is in range
            if (distance <= TURRET_RANGE) {
                // Face target
                Vector2 direction = targetPos.copy().subtract(turretPos);
                setRotation(Math.atan2(direction.y, direction.x));
                
                // Attack if cooldown is ready
                long now = System.currentTimeMillis();
                double attackInterval = 1000.0 / TURRET_ATTACK_RATE;
                if (now - lastAttackTime >= attackInterval) {
                    lastAttackTime = now;
                    return attackTarget();
                }
            } else {
                // Target out of range
                targetUnit = null;
            }
        }
        return null;
    }
    
    /**
     * Attack current target - fires a projectile
     * @return Projectile to be added to world, or null if can't attack
     */
    private Projectile attackTarget() {
        if (targetUnit == null || !targetUnit.isActive()) {
            return null;
        }
        
        Vector2 turretPos = getPosition();
        Vector2 targetPos = targetUnit.getPosition();
        Vector2 direction = targetPos.copy().subtract(turretPos);
        direction.normalize();
        
        // Turrets fire bullets
        double projectileSpeed = 600;
        Vector2 velocity = direction.multiply(projectileSpeed);
        
        Set<BulletEffect> effects = new HashSet<>();
        
        return new Projectile(
                id,
                turretPos.x,
                turretPos.y,
                velocity.x,
                velocity.y,
                TURRET_DAMAGE,
                TURRET_RANGE,
                teamNumber,
                0.2, // linear damping
                effects,
                Ordinance.BULLET,
                3.5 // Building turrets fire medium-sized projectiles
        );
    }
    
    /**
     * Set rally point for produced units
     */
    public void setRallyPoint(Vector2 point) {
        this.rallyPoint = point.copy();
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
    
    /**
     * Create shield sensor body for Shield Generator
     */
    public Body createShieldSensorBody() {
        if (buildingType != BuildingType.SHIELD_GENERATOR) {
            return null;
        }
        
        Body sensor = new Body();
        sensor.addFixture(Geometry.createCircle(SHIELD_RADIUS), 0.0, 0.0, 0.0);
        sensor.getFixture(0).setSensor(true); // Make it a sensor (no collision response)
        sensor.setMass(MassType.INFINITE);
        sensor.getTransform().setTranslation(getPosition().x, getPosition().y);
        sensor.setUserData(this); // Link back to building
        
        return sensor;
    }
    
    /**
     * Update shield state (activate/deactivate based on power)
     */
    private void updateShield(boolean hasLowPower) {
        boolean shouldBeActive = !hasLowPower && !underConstruction;
        
        if (shouldBeActive && !shieldActive) {
            activateShield();
        } else if (!shouldBeActive && shieldActive) {
            deactivateShield();
        }
    }
    
    /**
     * Activate the shield
     */
    public void activateShield() {
        if (buildingType != BuildingType.SHIELD_GENERATOR || shieldActive) {
            return;
        }
        
        shieldActive = true;
        log.debug("Shield activated for building {}", id);
    }
    
    /**
     * Deactivate the shield
     */
    public void deactivateShield() {
        if (!shieldActive) {
            return;
        }
        
        shieldActive = false;
        log.debug("Shield deactivated for building {}", id);
    }
    
    /**
     * Check if a position is inside this building's shield
     */
    public boolean isPositionInsideShield(Vector2 position) {
        if (!shieldActive || buildingType != BuildingType.SHIELD_GENERATOR) {
            return false;
        }
        
        double distance = getPosition().distance(position);
        return distance <= SHIELD_RADIUS;
    }
    
    /**
     * Get the shield radius
     */
    public double getShieldRadius() {
        return SHIELD_RADIUS;
    }
    
    /**
     * Check if this is a monument building
     */
    public boolean isMonument() {
        return buildingType == BuildingType.PHOTON_SPIRE ||
               buildingType == BuildingType.QUANTUM_NEXUS ||
               buildingType == BuildingType.SANDSTORM_GENERATOR;
    }
    
    /**
     * Create aura sensor body for monument buildings
     */
    public Body createAuraSensorBody() {
        if (!isMonument()) {
            return null;
        }
        
        double radius = switch (buildingType) {
            case PHOTON_SPIRE -> PHOTON_SPIRE_RADIUS;
            case QUANTUM_NEXUS -> QUANTUM_NEXUS_RADIUS;
            case SANDSTORM_GENERATOR -> SANDSTORM_RADIUS;
            default -> 0.0;
        };
        
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
            case PHOTON_SPIRE -> PHOTON_SPIRE_RADIUS;
            case QUANTUM_NEXUS -> QUANTUM_NEXUS_RADIUS;
            case SANDSTORM_GENERATOR -> SANDSTORM_RADIUS;
            default -> 0.0;
        };
    }
    
    /**
     * Get the beam damage multiplier from Photon Spire
     */
    public double getBeamDamageMultiplier() {
        if (buildingType == BuildingType.PHOTON_SPIRE && auraActive) {
            return PHOTON_SPIRE_DAMAGE_MULTIPLIER;
        }
        return 1.0;
    }
    
    /**
     * Get the health multiplier from Quantum Nexus
     */
    public double getHealthMultiplier() {
        if (buildingType == BuildingType.QUANTUM_NEXUS && auraActive) {
            return QUANTUM_NEXUS_HEALTH_MULTIPLIER;
        }
        return 1.0;
    }
    
    /**
     * Check if sandstorm is ready to pulse and reset timer
     * @return true if sandstorm should pulse
     */
    public boolean checkAndResetSandstorm() {
        if (buildingType != BuildingType.SANDSTORM_GENERATOR || !auraActive) {
            return false;
        }
        
        if (sandstormTimer >= SANDSTORM_INTERVAL) {
            sandstormTimer = 0;
            return true;
        }
        
        return false;
    }
    
    /**
     * Get sandstorm damage amount
     */
    public double getSandstormDamage() {
        return SANDSTORM_DAMAGE;
    }
    
    /**
     * Garrison a unit inside this building (for Bunker)
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
     * Set the maximum garrison capacity
     */
    public void setMaxGarrisonCapacity(int capacity) {
        this.maxGarrisonCapacity = capacity;
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

