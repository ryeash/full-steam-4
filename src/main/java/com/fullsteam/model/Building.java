package com.fullsteam.model;

import com.fullsteam.model.component.AndroidFactoryComponent;
import com.fullsteam.model.component.BankComponent;
import com.fullsteam.model.component.DefenseComponent;
import com.fullsteam.model.component.GarrisonComponent;
import com.fullsteam.model.component.HangarComponent;
import com.fullsteam.model.component.IBuildingComponent;
import com.fullsteam.model.component.ProductionComponent;
import com.fullsteam.model.component.ResearchComponent;
import com.fullsteam.model.component.SandstormComponent;
import com.fullsteam.model.component.ShieldComponent;
import com.fullsteam.model.research.ResearchModifier;
import com.fullsteam.model.weapon.WeaponFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a building in the RTS game.
 */
@Slf4j
@Getter
@Setter
public class Building extends GameEntity implements Targetable {
    private final BuildingType buildingType;
    private final int ownerId; // Player who owns this building
    private final int teamNumber;
    private final PlayerFaction faction; // Reference to owner's faction for dynamic stat calculations

    // Component system for modular building behavior
    private final Map<Class<? extends IBuildingComponent>, IBuildingComponent> components = new HashMap<>();

    // Construction state
    private boolean underConstruction = true;
    private double constructionProgress = 0; // 0 to maxHealth

    // Monument aura fields
    private static final int COMMAND_CITADEL_UPKEEP_BONUS = 50; // +50 max upkeep

    /**
     * Constructor with custom max health (for faction modifiers)
     */
    public Building(int id, GameEntities gameEntities, BuildingType buildingType, double x, double y, int ownerId, int teamNumber, PlayerFaction faction, double maxHealth) {
        super(id, createBuildingBody(x, y, buildingType), maxHealth);
        this.buildingType = buildingType;
        this.ownerId = ownerId;
        this.teamNumber = teamNumber;
        this.faction = faction;

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

        // Initialize components based on building type
        initializeComponents(gameEntities);
    }

    /**
     * Initialize components based on building type.
     * This method adds the appropriate components to buildings that need them.
     */
    private void initializeComponents(GameEntities gameEntities) {
        // Add ProductionComponent to buildings that can produce units (except Android Factory and Hangar)
        // Android Factory uses AndroidFactoryComponent for autonomous production
        // Hangar uses HangarComponent for bomber production (one bomber per hangar)
        if (buildingType.isCanProduceUnits()
                && buildingType != BuildingType.ANDROID_FACTORY
                && buildingType != BuildingType.HANGAR) {
            addComponent(new ProductionComponent(null));
            log.debug("Building {} ({}) initialized with ProductionComponent", id, buildingType.getDisplayName());
        }

        if (buildingType == BuildingType.BANK) {
            addComponent(new BankComponent());
            log.debug("Building {} ({}) initialized with BankComponent", id, buildingType.getDisplayName());
        }

        if (buildingType == BuildingType.TURRET) {
            addComponent(new DefenseComponent(WeaponFactory.getTurretWeapon()));
            log.debug("Building {} ({}) initialized with DefenseComponent", id, buildingType.getDisplayName());
        }

        if (buildingType == BuildingType.ROCKET_TURRET) {
            addComponent(new DefenseComponent(WeaponFactory.getRocketTurretWeapon()));
            log.debug("Building {} ({}) initialized with DefenseComponent (Rocket)", id, buildingType.getDisplayName());
        }

        if (buildingType == BuildingType.LASER_TURRET) {
            addComponent(new DefenseComponent(WeaponFactory.getLaserTurretWeapon()));
            log.debug("Building {} ({}) initialized with DefenseComponent (Laser)", id, buildingType.getDisplayName());
        }

        if (buildingType == BuildingType.PHOTON_SPIRE) {
            addComponent(new DefenseComponent(WeaponFactory.getPhotonSpireWeapon()));
            log.debug("Building {} ({}) initialized with DefenseComponent", id, buildingType.getDisplayName());
        }

        if (buildingType == BuildingType.SHIELD_GENERATOR) {
            addComponent(new ShieldComponent());
            log.debug("Building {} ({}) initialized with ShieldComponent", id, buildingType.getDisplayName());
        }

        if (buildingType == BuildingType.ANDROID_FACTORY) {
            addComponent(new AndroidFactoryComponent());
            log.debug("Building {} ({}) initialized with AndroidFactoryComponent", id, buildingType.getDisplayName());
        }

        if (buildingType == BuildingType.RESEARCH_LAB || buildingType == BuildingType.TECH_CENTER) {
            addComponent(new ResearchComponent(this));
            log.debug("Building {} ({}) initialized with ResearchComponent", id, buildingType.getDisplayName());
        }

        if (buildingType == BuildingType.SANDSTORM_GENERATOR) {
            addComponent(new SandstormComponent());
        }
        
        // Tempest Spire (Storm Wings monument) - Anti-air defense tower
        if (buildingType == BuildingType.TEMPEST_SPIRE) {
            addComponent(new DefenseComponent(WeaponFactory.getTempestSpireWeapon()));
            log.debug("Building {} ({}) initialized with DefenseComponent (anti-air)", id, buildingType.getDisplayName());
        }

        if (buildingType == BuildingType.BUNKER) {
            addComponent(new GarrisonComponent(6));
        }

        if (buildingType == BuildingType.HANGAR) {
            addComponent(new HangarComponent());
            log.debug("Building {} ({}) initialized with HangarComponent", id, buildingType.getDisplayName());
        }

        // More components will be added here as we extract them:
        // - AuraComponent for monuments

        // initialize each building component
        components.values().forEach(c -> c.init(gameEntities, this));
    }

    // ============================================================================
    // Targetable Interface Implementation
    // ============================================================================

    // getId(), getTeamNumber(), getPosition(), isActive(), getHealth(), getMaxHealth()
    // are inherited from GameEntity

    /**
     * Override takeDamage to also reduce construction progress for buildings
     */
    @Override
    public void takeDamage(double damage) {
        if (!active) {
            return;
        }

        health -= damage;
        constructionProgress -= damage; // Damage also reduces construction progress

        if (health <= 0) {
            health = 0;
            active = false;
        }
    }

    /**
     * Implements Targetable - buildings are always at GROUND elevation
     */
    @Override
    public Elevation getElevation() {
        return Elevation.GROUND;
    }

    /**
     * Implements Targetable - returns the building's size as target size
     */
    @Override
    public double getTargetSize() {
        return buildingType.getSize();
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
                completeConstruction(gameEntities);
            }
            return; // Don't do anything else while under construction
        }

        // Update all components
        for (IBuildingComponent component : components.values()) {
            component.update(hasLowPower);
        }
    }

    /**
     * Complete construction of this building
     *
     * @return true if construction was just completed (for triggering post-construction logic)
     */
    public boolean completeConstruction(GameEntities gameEntities) {
        if (underConstruction) {
            underConstruction = false;
            health = maxHealth;
            constructionProgress = maxHealth;

            // Notify all components that construction is complete
            for (IBuildingComponent component : components.values()) {
                component.onConstructionComplete();
            }

            // Note: Research modifiers are now applied dynamically via getMaxHealth()
            // No need to retroactively apply them here

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
        return getComponent(ProductionComponent.class)
                .map(p -> p.queueUnitProduction(unitType))
                .orElse(false);
    }

    /**
     * Cancel current production and refund resources
     */
    public UnitType cancelCurrentProduction() {
        return getComponent(ProductionComponent.class)
                .map(ProductionComponent::cancelCurrentProduction)
                .orElse(null);
    }

    /**
     * Get production progress as a percentage
     */
    public double getProductionPercent() {
        return getComponent(ProductionComponent.class)
                .map(ProductionComponent::getProductionPercent)
                .orElseGet(() -> getComponent(AndroidFactoryComponent.class)
                        .map(AndroidFactoryComponent::getProductionProgressPercent)
                        .orElse(0.0D));

    }

    /**
     * Get the number of units in the production queue (excluding current production)
     */
    public int getProductionQueueSize() {
        return getComponent(ProductionComponent.class)
                .map(ProductionComponent::getProductionQueueSize)
                .orElse(0);
    }

    /**
     * Set rally point for produced units
     */
    public void setRallyPoint(Vector2 point) {
        getComponent(ProductionComponent.class)
                .ifPresent(pc -> pc.setRallyPoint(point));
        getComponent(AndroidFactoryComponent.class)
                .ifPresent(pc -> pc.setRallyPoint(point));
    }

    /**
     * Check if this building belongs to a specific player
     */
    public boolean belongsTo(int playerId) {
        return this.ownerId == playerId;
    }

    /**
     * Check if this is a monument building
     */
    public boolean isMonument() {
        return buildingType == BuildingType.PHOTON_SPIRE ||
                buildingType == BuildingType.ANDROID_FACTORY ||
                buildingType == BuildingType.SANDSTORM_GENERATOR ||
                buildingType == BuildingType.COMMAND_CITADEL ||
                buildingType == BuildingType.TEMPEST_SPIRE;
    }

    /**
     * Get the upkeep bonus from Command Citadel
     */
    public int getUpkeepBonus() {
        if (buildingType == BuildingType.COMMAND_CITADEL) {
            return COMMAND_CITADEL_UPKEEP_BONUS;
        }
        return 0;
    }

    /**
     * Garrison a unit inside this building (for Bunker)
     *
     * @return true if successfully garrisoned, false if full or not allowed
     */
    public boolean garrisonUnit(Unit unit) {
        return getComponent(GarrisonComponent.class)
                .map(c -> c.garrisonUnit(unit))
                .orElse(false);
    }

    /**
     * Ungarrison a unit from this building
     *
     * @param unit The unit to ungarrison (null = ungarrison first unit)
     * @return The ungarrisoned unit, or null if none available
     */
    public Unit ungarrisonUnit(Unit unit) {
        return getComponent(GarrisonComponent.class)
                .map(c -> c.ungarrisonUnit(unit))
                .orElse(null);
    }

    /**
     * Un-garrison all units from this building
     *
     * @return List of ungarrisoned units
     */
    public List<Unit> ungarrisonAllUnits() {
        return getComponent(GarrisonComponent.class)
                .map(GarrisonComponent::ungarrisonAllUnits)
                .orElse(List.of());
    }

    /**
     * Get number of garrisoned units
     */
    public int getGarrisonCount() {
        return getComponent(GarrisonComponent.class)
                .map(GarrisonComponent::getGarrisonCount)
                .orElse(0);
    }

    public int getMaxGarrisonCapacity() {
        return getComponent(GarrisonComponent.class)
                .map(GarrisonComponent::getMaxGarrisonCapacity)
                .orElse(0);
    }

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
        }
    }

    /**
     * Get a component by its class type.
     *
     * @param componentClass The class of the component to retrieve
     * @param <T>            The component type
     * @return The component, or null if not present
     */
    public <T extends IBuildingComponent> Optional<T> getComponent(Class<T> componentClass) {
        return Optional.ofNullable(componentClass.cast(components.get(componentClass)));
    }

    public Vector2 getRallyPoint() {
        return getComponent(ProductionComponent.class)
                .map(ProductionComponent::getRallyPoint)
                .or(() -> getComponent(AndroidFactoryComponent.class)
                        .map(AndroidFactoryComponent::getRallyPoint))
                .orElse(null);
    }

    /**
     * Get effective max health with research modifiers applied.
     * Overrides GameEntity.getMaxHealth() to apply research bonuses.
     */
    @Override
    public double getMaxHealth() {
        return buildingType.getMaxHealth() * 
            faction.getResearchManager().getCumulativeModifier().getBuildingHealthMultiplier();
    }
}


