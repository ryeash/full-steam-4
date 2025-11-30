package com.fullsteam.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullsteam.games.GameConstants;
import com.fullsteam.games.IdGenerator;
import com.fullsteam.model.command.AttackBuildingCommand;
import com.fullsteam.model.command.AttackGroundCommand;
import com.fullsteam.model.command.AttackMoveCommand;
import com.fullsteam.model.command.AttackUnitCommand;
import com.fullsteam.model.command.AttackWallSegmentCommand;
import com.fullsteam.model.command.ConstructCommand;
import com.fullsteam.model.command.GarrisonBunkerCommand;
import com.fullsteam.model.command.HarvestCommand;
import com.fullsteam.model.command.MineCommand;
import com.fullsteam.model.command.MoveCommand;
import com.fullsteam.model.component.AndroidFactoryComponent;
import com.fullsteam.model.component.IBuildingComponent;
import com.fullsteam.model.component.ShieldComponent;
import com.fullsteam.model.factions.Faction;
import com.fullsteam.model.research.ResearchManager;
import com.fullsteam.model.research.ResearchModifier;
import com.fullsteam.model.research.ResearchType;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.exceptions.WebSocketSessionException;
import lombok.Getter;
import org.dyn4j.collision.AxisAlignedBounds;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.Settings;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Main game manager for RTS gameplay.
 * Handles units, buildings, resources, and game logic.
 */
public class RTSGameManager {
    protected static final Logger log = LoggerFactory.getLogger(RTSGameManager.class);

    @Getter
    protected final String gameId;
    @Getter
    protected final GameConfig gameConfig;
    protected final ObjectMapper objectMapper;

    // Game world
    private final World<Body> world;
    @Getter
    private final RTSWorld rtsWorld;
    private double lastUpdateTime = System.nanoTime() / 1e9;
    private long frameCount = 0;

    // Player management
    private final Map<Integer, PlayerSession> playerSessions = new ConcurrentHashMap<>();
    /**
     * -- GETTER --
     * Get player factions map
     */
    @Getter
    private final Set<Integer> eliminatedTeams = ConcurrentHashMap.newKeySet(); // Track eliminated teams for events

    // Event throttling - track last notification times per player (in milliseconds)
    private final Map<Integer, Long> lastUnitDeathNotification = new ConcurrentSkipListMap<>();
    private static final long UNIT_DEATH_NOTIFICATION_COOLDOWN = 5000; // 5 seconds

    // Game entities - bundled together for easy passing to commands/AI
    @Getter
    private final GameEntities gameEntities;

    // Convenience accessors for internal use
    private final Map<Integer, PlayerFaction> playerFactions;
    private final Map<Integer, Unit> units;
    private final Map<Integer, Building> buildings;
    private final Map<Integer, ResourceDeposit> resourceDeposits;
    private final Map<Integer, Obstacle> obstacles;
    private final Map<Integer, Projectile> projectiles;
    private final Map<Integer, Beam> beams;
    private final Map<Integer, FieldEffect> fieldEffects;
    private final Map<Integer, WallSegment> wallSegments;

    // Collision processor
    private final RTSCollisionProcessor collisionProcessor;

    // Player inputs
    private final Map<Integer, RTSPlayerInput> playerInputs = new ConcurrentHashMap<>();

    // Game state
    @Getter
    protected long gameStartTime;
    @Getter
    protected boolean gameOver = false;
    protected int winningTeam = -1;
    private final ScheduledFuture<?> updateTask;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    // Track if game has started with full roster (prevent late joins)
    private boolean gameStartedWithFullRoster = false;

    // Track disconnected players (for victory condition)
    private final Set<Integer> disconnectedPlayers = ConcurrentHashMap.newKeySet();

    public RTSGameManager(String gameId, GameConfig gameConfig, ObjectMapper objectMapper) {
        this.gameId = gameId;
        this.gameConfig = gameConfig;
        this.objectMapper = objectMapper;
        this.gameStartTime = System.currentTimeMillis();

        this.gameEntities = new GameEntities(gameConfig, this::sendGameEvent);
        this.playerFactions = gameEntities.getPlayerFactions();
        this.units = gameEntities.getUnits();
        this.buildings = gameEntities.getBuildings();
        this.resourceDeposits = gameEntities.getResourceDeposits();
        this.obstacles = gameEntities.getObstacles();
        this.projectiles = gameEntities.getProjectiles();
        this.beams = gameEntities.getBeams();
        this.fieldEffects = gameEntities.getFieldEffects();
        this.wallSegments = gameEntities.getWallSegments();

        // Initialize RTS world with symmetric layout
        long worldSeed = System.currentTimeMillis();
        this.rtsWorld = new RTSWorld(
                gameConfig.getWorldWidth(),
                gameConfig.getWorldHeight(),
                gameConfig.getMaxPlayers(),
                gameConfig.getBiome(),
                gameConfig.getObstacleDensity().getMultiplier(),
                worldSeed
        );

        // Initialize physics world
        this.world = new World<>();
        Settings settings = new Settings();
        settings.setMaximumTranslation(300.0);
        this.world.setSettings(settings);
        this.world.setGravity(new Vector2(0, 0));

        // Set world reference in gameEntities for beam raycasting
        this.gameEntities.setWorld(this.world);

        // Initialize collision processor
        this.collisionProcessor = new RTSCollisionProcessor(gameEntities);

        // Register collision listener to prevent friendly fire physics collisions
        this.world.addCollisionListener(this.collisionProcessor);

        this.world.setBounds(new AxisAlignedBounds(gameConfig.getWorldWidth(), gameConfig.getWorldHeight()));

        // Initialize world entities
        rtsWorld.placeResourceDeposits();
        rtsWorld.placeObstacles();
        rtsWorld.createWorldBoundaries();
        rtsWorld.getResourceDeposits().forEach(gameEntities::add);
        rtsWorld.getObstacles().forEach(gameEntities::add);

        // Start update loop
        this.updateTask = GameConstants.EXECUTOR.scheduleAtFixedRate(this::update, 0, 20, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop the game and cleanup resources
     */
    public void stopGame() {
        shutdown.set(true);
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel(true);
        }
        log.info("Game {} stopped", gameId);
    }

    /**
     * Main update loop
     */
    protected void update() {
        if (shutdown.get()) {
            return;
        }

        try {
            double currentTime = System.nanoTime() / 1e9;
            double deltaTime = currentTime - lastUpdateTime;
            lastUpdateTime = currentTime;
            frameCount++;

            // Recalculate upkeep and power for all factions every 60 frames (~1 second)
            if (frameCount % 60 == 0) {
                recalculateUpkeep();
                recalculatePower();
            }

            // Update research progress for all players
            updateResearch(deltaTime);

            // Process player inputs
            playerInputs.forEach(this::processPlayerInput);

            // Update all units and collect projectiles they fire
            units.values().forEach(unit -> {
                // Skip garrisoned units (they're inside bunkers)
                if (unit.isGarrisoned()) {
                    return;
                }

                // Provide gameEntities to the unit's command for intelligent decision-making
                if (unit.getCurrentCommand() != null) {
                    unit.getCurrentCommand().setGameEntities(gameEntities);
                }

                // Help workers find/update their target refinery when returning resources
                if (unit.getCurrentCommand() instanceof HarvestCommand harvestCmd) {
                    if (harvestCmd.isReturningResources()) {
                        Building currentRefinery = harvestCmd.getTargetRefinery();

                        // Re-evaluate refinery if:
                        // 1. No refinery assigned yet
                        // 2. Current refinery is no longer valid (destroyed or under construction)
                        // 3. Periodically check for a closer one (every 60 frames / ~1 second)
                        boolean needsReevaluation = currentRefinery == null ||
                                !currentRefinery.isActive() ||
                                currentRefinery.isUnderConstruction() ||
                                (frameCount % 60 == 0);

                        if (needsReevaluation) {
                            Building refinery = findNearestRefinery(unit);
                            if (refinery != currentRefinery) {
                                harvestCmd.setTargetRefinery(refinery);
                            }
                        }
                    }
                }

                // Check if worker is at refinery to deposit resources
                if (unit.getUnitType().canHarvest() && unit.getCarriedResources() > 0) {
                    if (unit.getCurrentCommand() instanceof HarvestCommand harvestCmd) {
                        if (harvestCmd.isReturningResources()) {
                            Building refinery = harvestCmd.getTargetRefinery();
                            if (refinery != null && refinery.isActive()) {
                                double distance = unit.getPosition().distance(refinery.getPosition());
                                if (distance <= refinery.getBuildingType().getSize() + unit.getUnitType().getSize() + 10) {
                                    // Deposit resources to player's faction
                                    PlayerFaction faction = playerFactions.get(unit.getOwnerId());
                                    if (faction != null) {
                                        int depositAmount = (int) Math.round(unit.getCarriedResources());
                                        faction.addResources(ResourceType.CREDITS, depositAmount);
                                        log.debug("Player {} deposited {} resources", unit.getOwnerId(), depositAmount);
                                    }
                                }
                            }
                        }
                    }
                }

                // Help miners find headquarters when returning for pickaxe repair
                if (unit.getCurrentCommand() instanceof MineCommand mineCmd) {

                    if (mineCmd.isReturningForRepair()) {
                        Building currentHQ = mineCmd.getTargetHeadquarters();

                        // Re-evaluate HQ if needed
                        boolean needsReevaluation = currentHQ == null ||
                                !currentHQ.isActive() ||
                                currentHQ.isUnderConstruction();

                        if (needsReevaluation) {
                            Building headquarters = findNearestHeadquarters(unit);
                            if (headquarters != null && headquarters != currentHQ) {
                                mineCmd.setTargetHeadquarters(headquarters);
                            }
                        }
                    }
                }

                unit.update(gameEntities);

                // Update movement with steering behaviors (pass nearby units for separation)
                List<Unit> nearbyUnits = units.values().stream()
                        .filter(u -> u.isActive() && u != unit)
                        .filter(u -> unit.getPosition().distance(u.getPosition()) < 150.0) // Within 150 units
                        .collect(Collectors.toList());
                unit.updateMovement(deltaTime, nearbyUnits);

                // Check if unit fired a projectile
                if (unit.getUnitType().canAttack()) {
                    // Clear invalid targets
                    if (unit.getTargetUnit() != null && !unit.getTargetUnit().isActive()) {
                        unit.setTargetUnit(null);
                    }
                    if (unit.getTargetBuilding() != null && !unit.getTargetBuilding().isActive()) {
                        unit.setTargetBuilding(null);
                    }

                    // Check if target is too far away (out of vision range)
                    if (unit.getTargetUnit() != null) {
                        double distance = unit.getPosition().distance(unit.getTargetUnit().getPosition());
                        double visionRange = unit.getUnitType().getAttackRange() * 2.0; // 2x attack range
                        if (distance > visionRange) {
                            unit.setTargetUnit(null); // Target escaped
                        }
                    }
                    if (unit.getTargetBuilding() != null) {
                        double distance = unit.getPosition().distance(unit.getTargetBuilding().getPosition());
                        double visionRange = unit.getUnitType().getAttackRange() * 2.0; // 2x attack range
                        if (distance > visionRange) {
                            unit.setTargetBuilding(null); // Target too far
                        }
                    }

                    AbstractOrdinance ordinance = null;
                    if (unit.getCurrentCommand() != null) {
                        ordinance = unit.getCurrentCommand().updateCombat(deltaTime);
                    }

                    // Add projectile or beam to world
                    if (ordinance != null) {
                        gameEntities.add(ordinance);
                    }
                }

                // Multi-turret AI for deployed units (e.g., Crawler)
                if (!unit.getTurrets().isEmpty()) {
                    for (Turret turret : unit.getTurrets()) {
                        turret.update(unit, gameEntities, frameCount);
                    }
                }

                // AttackMoveCommand still uses the old scanForEnemies method (legacy)
                if (unit.getCurrentCommand() instanceof AttackMoveCommand attackMoveCmd) {
                    attackMoveCmd.scanForEnemies(Collections.unmodifiableCollection(units.values()), Collections.unmodifiableCollection(buildings.values()));
                }
                unit.scanForHealTargets(new ArrayList<>(units.values()));
                unit.scanForRepairTargets(new ArrayList<>(buildings.values()), new ArrayList<>(units.values()));

                // AI behavior: return to home position if needed (defensive stance)
                if (unit.shouldReturnHome()) {
                    List<Vector2> path = Pathfinding.findPath(
                            unit.getPosition(),
                            unit.getHomePosition(),
                            obstacles.values(),
                            buildings.values(),
                            unit.getUnitType().getSize(),
                            gameConfig.getWorldWidth(),
                            gameConfig.getWorldHeight()
                    );
                    unit.setPath(path);
                }
            });

            // Update all buildings and collect projectiles from turrets
            buildings.values().forEach(building -> {
                // Check if owner has low power
                PlayerFaction faction = playerFactions.get(building.getOwnerId());
                boolean hasLowPower = faction != null && faction.isHasLowPower();

                // Check if construction just completed
                boolean wasUnderConstruction = building.isUnderConstruction();

                building.update(gameEntities, gameEntities.getWorld().getTimeStep().getDeltaTime(), hasLowPower);

                // If construction just completed, send notification and handle post-construction logic
                if (wasUnderConstruction && !building.isUnderConstruction()) {
                    // Send construction complete notification (except for walls - too spammy)
                    if (building.getBuildingType() != BuildingType.WALL && faction != null) {
                        String buildingName = building.getBuildingType().name()
                                .replace("_", " ")
                                .toLowerCase();
                        // Capitalize first letter
                        buildingName = buildingName.substring(0, 1).toUpperCase() + buildingName.substring(1);

                        sendGameEvent(GameEvent.createPlayerEvent(
                                "🏗️ " + buildingName + " construction complete",
                                faction.getPlayerId(),
                                GameEvent.EventCategory.INFO
                        ));
                    }

                    // Create wall segments for wall posts
                    if (building.getBuildingType() == BuildingType.WALL) {
                        createWallSegments(building);
                        log.debug("Wall post {} construction completed, creating wall segments", building.getId());
                    }
                }
            });

            // Update projectiles
            projectiles.entrySet().removeIf(entry -> {
                Projectile projectile = entry.getValue();
                projectile.update(gameEntities);

                // Create explosion if projectile reached max range and is explosive
                if (!projectile.isActive() && isExplosiveProjectile(projectile)) {
                    createExplosionAtProjectile(projectile);
                }

                if (!projectile.isActive()) {
                    world.removeBody(projectile.getBody());
                    return true;
                }
                return false;
            });

            // Update beams (they fade out over time, but don't remove yet)
            beams.values().forEach(beam -> beam.update(gameEntities));

            // Update physics world (handles collisions via CollisionListener)
            world.updatev(deltaTime);

            // Process field effects (explosions, etc.)
            processFieldEffects(deltaTime);

            // Remove inactive entities
            removeInactiveEntities();

            // Check for disconnected players
            checkDisconnectedPlayers();

            // Check win conditions
            if (!gameOver) {
                checkWinConditions();
            }

            // Send game state to all players
            sendGameState();

        } catch (Throwable t) {
            log.error("Error in update loop", t);
        }
    }

    /**
     * Process player input
     */
    private void processPlayerInput(Integer playerId, RTSPlayerInput input) {
        PlayerFaction faction = playerFactions.get(playerId);
        if (faction == null) {
            return;
        }

        // Remove input immediately to prevent reprocessing on subsequent frames
        // (Important: do this BEFORE any early returns to avoid message spam)
        playerInputs.remove(playerId);

        // Handle unit selection
        if (input.getSelectUnits() != null && !input.getSelectUnits().isEmpty()) {
            // Clear previous selections
            units.values().stream()
                    .filter(u -> u.belongsTo(playerId))
                    .forEach(u -> u.setSelected(false));

            // Select new units
            input.getSelectUnits().forEach(unitId -> {
                Unit unit = units.get(unitId);
                if (unit != null && unit.belongsTo(playerId)) {
                    unit.setSelected(true);
                }
            });
        }

        // Handle move orders with pathfinding
        if (input.getMoveOrder() != null) {
            Vector2 destination = input.getMoveOrder();
            units.values().stream()
                    .filter(u -> u.belongsTo(playerId) && u.isSelected())
                    .forEach(u -> {
                        // Calculate path using A* pathfinding
                        List<Vector2> path = Pathfinding.findPath(
                                u.getPosition(),
                                destination,
                                obstacles.values(),
                                buildings.values(),
                                u.getUnitType().getSize(),
                                gameConfig.getWorldWidth(),
                                gameConfig.getWorldHeight()
                        );

                        // Use command pattern
                        u.issueCommand(new MoveCommand(u, destination, true));

                        // Set the pathfinding path on the command
                        if (u.getCurrentCommand() instanceof MoveCommand) {
                            ((MoveCommand) u.getCurrentCommand()).setPath(path);
                        }
                    });
        }

        // Handle attack-move orders
        if (input.getAttackMoveOrder() != null) {
            Vector2 destination = input.getAttackMoveOrder();
            units.values().stream()
                    .filter(u -> u.belongsTo(playerId) && u.isSelected())
                    .forEach(u -> {
                        // Calculate path using A* pathfinding
                        List<Vector2> path = Pathfinding.findPath(
                                u.getPosition(),
                                destination,
                                obstacles.values(),
                                buildings.values(),
                                u.getUnitType().getSize(),
                                gameConfig.getWorldWidth(),
                                gameConfig.getWorldHeight()
                        );
                        AttackMoveCommand cmd = new AttackMoveCommand(u, destination, true);
                        cmd.setPath(path);
                        u.issueCommand(cmd);
                    });
        }

        // Handle attack orders
        if (input.getAttackUnitOrder() != null) {
            Unit target = units.get(input.getAttackUnitOrder());
            if (target != null) {
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected())
                        .forEach(u -> u.issueCommand(new AttackUnitCommand(u, target, true)));
            }
        }

        if (input.getAttackBuildingOrder() != null) {
            Building target = buildings.get(input.getAttackBuildingOrder());
            if (target != null) {
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected())
                        .forEach(u -> u.issueCommand(new AttackBuildingCommand(u, target, true)));
            }
        }

        if (input.getAttackWallSegmentOrder() != null) {
            WallSegment target = wallSegments.get(input.getAttackWallSegmentOrder());
            if (target != null) {
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected())
                        .forEach(u -> u.issueCommand(new AttackWallSegmentCommand(u, target, true)));
            }
        }

        // Handle force attack orders (attack ground - CMD/CTRL + right click)
        if (input.getForceAttackOrder() != null) {
            Vector2 targetPosition = input.getForceAttackOrder();
            units.values().stream()
                    .filter(u -> u.belongsTo(playerId) && u.isSelected() && u.getUnitType().canAttack())
                    .forEach(u -> {
                        // Issue force attack order first (sets all the flags correctly)
                        u.issueCommand(new AttackGroundCommand(u, targetPosition, true));

                        // Then calculate and set the path
                        List<Vector2> path = Pathfinding.findPath(
                                u.getPosition(),
                                targetPosition,
                                obstacles.values(),
                                buildings.values(),
                                u.getUnitType().getSize(),
                                gameConfig.getWorldWidth(),
                                gameConfig.getWorldHeight()
                        );
                        u.setPath(path, true);
                    });
            log.info("Player {} issued force attack order to position ({}, {})",
                    playerId, targetPosition.x, targetPosition.y);
        }

        // Handle harvest orders
        if (input.getHarvestOrder() != null) {
            ResourceDeposit deposit = resourceDeposits.get(input.getHarvestOrder());
            if (deposit != null) {
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected() && u.getUnitType().canHarvest())
                        .forEach(u -> u.issueCommand(new HarvestCommand(u, deposit, true)));
            }
        }

        // Handle mine orders
        if (input.getMineOrder() != null) {
            Obstacle obstacle = obstacles.get(input.getMineOrder());
            if (obstacle != null && obstacle.isDestructible()) {
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected() && u.getUnitType().canMine())
                        .forEach(u -> u.issueCommand(new MineCommand(u, obstacle, true)));
                log.info("Player {} ordered miners to destroy obstacle {}", playerId, obstacle.getId());
            }
        }

        // Handle construct orders (resume building construction)
        if (input.getConstructOrder() != null) {
            Building building = buildings.get(input.getConstructOrder());
            if (building != null && building.isUnderConstruction() && building.belongsTo(playerId)) {
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected() && u.getUnitType().canBuild())
                        .forEach(u -> u.issueCommand(new ConstructCommand(u, building, true)));
            }
        }

        // Handle AI stance changes
        if (input.getSetStance() != null) {
            units.values().stream()
                    .filter(u -> u.belongsTo(playerId) && u.isSelected())
                    .forEach(u -> u.setAiStance(input.getSetStance()));
        }

        // Handle special ability activation
        if (input.isActivateSpecialAbility()) {
            // Check if this is a targeted ability
            if (input.getSpecialAbilityTargetUnit() != null) {
                // Heal or other unit-targeted ability
                Integer targetUnitId = input.getSpecialAbilityTargetUnit();
                Unit targetUnit = units.get(targetUnitId);

                if (targetUnit != null && targetUnit.isActive()) {
                    units.values().stream()
                            .filter(u -> u.belongsTo(playerId) && u.isSelected())
                            .forEach(unit -> {
                                if (unit.getUnitType().hasSpecialAbility() &&
                                        unit.getUnitType().getSpecialAbility().isRequiresTarget()) {
                                    boolean success = unit.useSpecialAbilityOnUnit(targetUnit);
                                    if (success) {
                                        SpecialAbility ability = unit.getUnitType().getSpecialAbility();
                                        sendGameEvent(GameEvent.createPlayerEvent(
                                                ability.getDisplayName() + " used on " + targetUnit.getUnitType().getDisplayName(),
                                                playerId,
                                                GameEvent.EventCategory.INFO
                                        ));
                                    }
                                }
                            });
                }
            } else if (input.getSpecialAbilityTargetBuilding() != null) {
                // Repair or other building-targeted ability
                Integer targetBuildingId = input.getSpecialAbilityTargetBuilding();
                Building targetBuilding = buildings.get(targetBuildingId);

                if (targetBuilding != null && targetBuilding.isActive()) {
                    units.values().stream()
                            .filter(u -> u.belongsTo(playerId) && u.isSelected())
                            .forEach(unit -> {
                                if (unit.getUnitType().hasSpecialAbility() &&
                                        unit.getUnitType().getSpecialAbility().isRequiresTarget()) {
                                    boolean success = unit.useSpecialAbilityOnBuilding(targetBuilding);
                                    if (success) {
                                        SpecialAbility ability = unit.getUnitType().getSpecialAbility();
                                        sendGameEvent(GameEvent.createPlayerEvent(
                                                ability.getDisplayName() + " used on " + targetBuilding.getBuildingType().getDisplayName(),
                                                playerId,
                                                GameEvent.EventCategory.INFO
                                        ));
                                    }
                                }
                            });
                }
            } else {
                // Non-targeted ability (toggle like deploy)
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected())
                        .forEach(unit -> {
                            if (unit.getUnitType().hasSpecialAbility()) {
                                boolean activated = unit.activateSpecialAbility();
                                if (activated) {
                                    SpecialAbility ability = unit.getUnitType().getSpecialAbility();
                                    String message = unit.isSpecialAbilityActive()
                                            ? ability.getDisplayName() + " activated"
                                            : ability.getDisplayName() + " deactivated";
                                    sendGameEvent(GameEvent.createPlayerEvent(
                                            message,
                                            playerId,
                                            GameEvent.EventCategory.INFO
                                    ));
                                }
                            }
                        });
            }
        }

        // Handle garrison orders
        if (input.getGarrisonOrder() != null) {
            Building bunker = buildings.get(input.getGarrisonOrder());
            if (bunker != null && bunker.getBuildingType() == BuildingType.BUNKER &&
                    bunker.belongsTo(playerId) && !bunker.isUnderConstruction()) {
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected() && u.getUnitType().isInfantry())
                        .forEach(u -> u.issueCommand(new GarrisonBunkerCommand(u, bunker, true)));
            }
        }

        // Handle ungarrison orders
        if (input.getUngarrisonBuildingId() != null) {
            Building bunker = buildings.get(input.getUngarrisonBuildingId());
            if (bunker != null && bunker.getBuildingType() == BuildingType.BUNKER &&
                    bunker.belongsTo(playerId)) {
                if (input.isUngarrisonAll()) {
                    // Ungarrison all units
                    List<Unit> ungarrisoned = bunker.ungarrisonAllUnits();
                    // Re-add units to the physics world
                    for (Unit unit : ungarrisoned) {
                        if (!world.containsBody(unit.getBody())) {
                            world.addBody(unit.getBody());
                        }
                    }
                    log.info("Player {} ungarrisoned {} units from bunker {}",
                            playerId, ungarrisoned.size(), bunker.getId());
                } else {
                    // Ungarrison one unit
                    Unit ungarrisoned = bunker.ungarrisonUnit(null);
                    if (ungarrisoned != null) {
                        // Re-add unit to the physics world
                        if (!world.containsBody(ungarrisoned.getBody())) {
                            world.addBody(ungarrisoned.getBody());
                        }
                        log.info("Player {} ungarrisoned unit {} from bunker {}",
                                playerId, ungarrisoned.getId(), bunker.getId());
                    }
                }
            }
        }

        // Handle build orders
        if (input.getBuildOrder() != null) {
            BuildingType buildingType = input.getBuildOrder();
            Vector2 location = input.getBuildLocation();

            // Validate tech requirements first
            if (!hasTechRequirements(playerId, buildingType)) {
                log.warn("Player {} attempted to build {} without meeting tech requirements",
                        playerId, buildingType);
                sendGameEvent(GameEvent.createPlayerEvent(
                        "Cannot build " + buildingType.getDisplayName() + " - missing required tech buildings",
                        playerId,
                        GameEvent.EventCategory.WARNING
                ));
                return; // Reject the build order
            }

            if (location != null) {
                // Check affordability first and provide feedback
                if (!canAffordBuilding(faction, buildingType)) {
                    int cost = faction.getBuildingCost(buildingType);
                    int currentCredits = faction.getResources().get(ResourceType.CREDITS);
                    log.warn("Player {} tried to build {} but cannot afford it (cost: {}, has: {})",
                            playerId, buildingType, cost, currentCredits);
                    sendGameEvent(GameEvent.createPlayerEvent(
                            String.format("💰 Insufficient funds! %s costs %d credits (you have %d)",
                                    buildingType.getDisplayName(), cost, currentCredits),
                            playerId,
                            GameEvent.EventCategory.WARNING
                    ));
                    return;
                }

                // Check valid build location
                if (!isValidBuildLocation(location, buildingType, playerId)) {
                    log.warn("Player {} tried to build {} at invalid location ({}, {})",
                            playerId, buildingType, location.x, location.y);
                    sendGameEvent(GameEvent.createPlayerEvent(
                            "⚠️ Cannot place building here - location is blocked or too close to other structures",
                            playerId,
                            GameEvent.EventCategory.WARNING
                    ));
                    return;
                }

                // All checks passed - proceed with building
                // Deduct resources (use faction-modified cost)
                int cost = faction.getBuildingCost(buildingType);
                faction.removeResources(ResourceType.CREDITS, cost);

                // Create building under construction (with faction-modified health)
                double maxHealth = faction.getBuildingHealth(buildingType);
                Building building = new Building(
                        IdGenerator.nextEntityId(),
                        gameEntities,
                        buildingType,
                        location.x, location.y,
                        playerId,
                        faction.getTeamNumber(),
                        maxHealth
                );
                buildings.put(building.getId(), building);
                world.addBody(building.getBody());

                // Note: Wall segments are created when construction completes, not at placement

                // Order selected workers to construct it
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected() && u.getUnitType().canBuild())
                        .forEach(u -> u.issueCommand(new ConstructCommand(u, building, true)));

                log.debug("Player {} placed {} at ({}, {})", playerId, buildingType, location.x, location.y);
            }
        }

        // Handle unit production orders
        if (input.getProduceUnitOrder() != null) {
            UnitType unitType = input.getProduceUnitOrder();
            Integer buildingId = input.getProduceBuildingId();

            log.info("Player {} requesting to produce {} at building {}", playerId, unitType, buildingId);

            if (buildingId != null) {
                Building building = buildings.get(buildingId);
                log.info("Building found: {}, belongs to player: {}, can afford: {}",
                        building != null,
                        building != null && building.belongsTo(playerId),
                        canAffordUnit(faction, unitType));

                if (building != null && building.belongsTo(playerId)) {
                    // Check if this building can produce this unit for this faction
                    if (!faction.canBuildingProduceUnit(building.getBuildingType(), unitType)) {
                        log.warn("Player {} tried to produce {} at {} but faction doesn't allow it",
                                playerId, unitType, building.getBuildingType());
                        return;
                    }

                    // Check if player has low power
                    if (faction.isHasLowPower()) {
                        log.warn("Player {} tried to produce {} but has LOW POWER", playerId, unitType);
                        sendGameEvent(GameEvent.createPlayerEvent(
                                "⚡ Cannot start production: LOW POWER! Build more Power Plants!",
                                playerId,
                                GameEvent.EventCategory.WARNING
                        ));
                        return;
                    }

                    // Check if player can afford the upkeep
                    if (!faction.canAffordUpkeep(unitType.getUpkeepCost())) {
                        log.warn("Player {} tried to produce {} but upkeep limit reached ({}/{})",
                                playerId, unitType, faction.getCurrentUpkeep(), faction.getMaxUpkeep());
                        sendGameEvent(GameEvent.createPlayerEvent(
                                String.format("⚠️ Cannot produce unit: Upkeep limit reached (%d/%d)!",
                                        faction.getCurrentUpkeep(), faction.getMaxUpkeep()),
                                playerId,
                                GameEvent.EventCategory.WARNING
                        ));
                        return;
                    }

                    // Check if player can afford the unit
                    if (!canAffordUnit(faction, unitType)) {
                        int cost = faction.getUnitCost(unitType);
                        int currentCredits = faction.getResources().get(ResourceType.CREDITS);
                        log.warn("Player {} tried to produce {} but cannot afford it (cost: {}, has: {})",
                                playerId, unitType, cost, currentCredits);
                        sendGameEvent(GameEvent.createPlayerEvent(
                                String.format("💰 Insufficient funds! %s costs %d credits (you have %d)",
                                        unitType.getDisplayName(), cost, currentCredits),
                                playerId,
                                GameEvent.EventCategory.WARNING
                        ));
                        return;
                    }

                    // Deduct resources (use faction-modified cost)
                    int cost = faction.getUnitCost(unitType);
                    faction.removeResources(ResourceType.CREDITS, cost);

                    // Queue production
                    building.queueUnitProduction(unitType);
                    log.info("Player {} queued {} production at building {} (cost: {})",
                            playerId, unitType, buildingId, cost);
                }
            }
        }

        // Handle rally point orders
        if (input.getSetRallyBuildingId() != null && input.getRallyPoint() != null) {
            Integer buildingId = input.getSetRallyBuildingId();
            Vector2 rallyPoint = input.getRallyPoint();

            Building building = buildings.get(buildingId);
            if (building != null && building.belongsTo(playerId) && building.getBuildingType().isCanProduceUnits()) {
                building.setRallyPoint(rallyPoint);
                log.info("Player {} set rally point for building {} to ({}, {})",
                        playerId, buildingId, rallyPoint.x, rallyPoint.y);
            }
        }

        // Handle research orders
        if (input.getStartResearchOrder() != null) {
            ResearchType researchType = input.getStartResearchOrder();
            Integer buildingId = input.getResearchBuildingId();

            log.info("Player {} requesting to start research {} at building {}", playerId, researchType, buildingId);

            if (buildingId != null) {
                Building building = buildings.get(buildingId);

                if (building != null && building.belongsTo(playerId)) {
                    // Validate building type (must be RESEARCH_LAB or TECH_CENTER)
                    if (building.getBuildingType() != BuildingType.RESEARCH_LAB &&
                            building.getBuildingType() != BuildingType.TECH_CENTER) {
                        log.warn("Player {} tried to research at invalid building type {}",
                                playerId, building.getBuildingType());
                        return;
                    }

                    // Check if building is under construction
                    if (building.isUnderConstruction()) {
                        log.warn("Player {} tried to research at building {} that is under construction",
                                playerId, buildingId);
                        sendGameEvent(GameEvent.createPlayerEvent(
                                "⚠️ Cannot research: Building is still under construction!",
                                playerId,
                                GameEvent.EventCategory.WARNING
                        ));
                        return;
                    }

                    // Get player's buildings for tech requirement checks
                    Set<BuildingType> playerBuildings = buildings.values().stream()
                            .filter(b -> b.getOwnerId() == playerId &&
                                    b.isActive() &&
                                    !b.isUnderConstruction())
                            .map(Building::getBuildingType)
                            .collect(Collectors.toSet());

                    // Validate research can be started (via ResearchManager)
                    if (!faction.getResearchManager().canStartResearch(researchType, playerBuildings)) {
                        log.warn("Player {} cannot start research {} - requirements not met",
                                playerId, researchType);
                        sendGameEvent(GameEvent.createPlayerEvent(
                                "⚠️ Cannot research: Requirements not met!",
                                playerId,
                                GameEvent.EventCategory.WARNING
                        ));
                        return;
                    }

                    // Check if player can afford the research
                    int cost = researchType.getCreditCost();
                    if (!faction.hasResources(ResourceType.CREDITS, cost)) {
                        int currentCredits = faction.getResourceAmount(ResourceType.CREDITS);
                        log.warn("Player {} cannot afford research {} (cost: {}, has: {})",
                                playerId, researchType, cost, currentCredits);
                        sendGameEvent(GameEvent.createPlayerEvent(
                                String.format("💰 Insufficient funds! %s costs %d credits (you have %d)",
                                        researchType.getDisplayName(), cost, currentCredits),
                                playerId,
                                GameEvent.EventCategory.WARNING
                        ));
                        return;
                    }

                    // Check simultaneous research limit before deducting resources
                    ResearchManager researchMgr = faction.getResearchManager();
                    if (researchMgr.getActiveResearchCount() >= researchMgr.getMaxSimultaneousResearch()) {
                        log.warn("Player {} has reached max simultaneous research limit ({}/{})",
                                playerId, researchMgr.getActiveResearchCount(), researchMgr.getMaxSimultaneousResearch());
                        sendGameEvent(GameEvent.createPlayerEvent(
                                String.format("⚠️ Maximum simultaneous research limit reached (%d/%d)! Complete or cancel existing research, or research Parallel Research upgrades.",
                                        researchMgr.getActiveResearchCount(), researchMgr.getMaxSimultaneousResearch()),
                                playerId,
                                GameEvent.EventCategory.WARNING
                        ));
                        return;
                    }

                    // Deduct resources
                    faction.removeResources(ResourceType.CREDITS, cost);

                    // Start research
                    boolean started = researchMgr.startResearch(researchType, buildingId, playerBuildings);

                    if (started) {
                        log.info("Player {} started research {} at building {} (cost: {}, {}/{} active)",
                                playerId, researchType, buildingId, cost,
                                researchMgr.getActiveResearchCount(), researchMgr.getMaxSimultaneousResearch());
                        sendGameEvent(GameEvent.createPlayerEvent(
                                String.format("🔬 Research started: %s (%d/%d active)",
                                        researchType.getDisplayName(),
                                        researchMgr.getActiveResearchCount(),
                                        researchMgr.getMaxSimultaneousResearch()),
                                playerId,
                                GameEvent.EventCategory.INFO
                        ));
                    } else {
                        // Refund if research couldn't start
                        faction.addResources(ResourceType.CREDITS, cost);
                        log.warn("Player {} failed to start research {} at building {}",
                                playerId, researchType, buildingId);
                    }
                }
            }
        }

        // Handle cancel research orders
        if (input.getCancelResearchBuildingId() != null) {
            Integer buildingId = input.getCancelResearchBuildingId();
            Building building = buildings.get(buildingId);

            if (building != null && building.belongsTo(playerId)) {
                // Cancel research via ResearchManager
                boolean cancelled = faction.getResearchManager().cancelResearch(buildingId);

                if (cancelled) {
                    log.info("Player {} cancelled research at building {}", playerId, buildingId);
                    sendGameEvent(GameEvent.createPlayerEvent(
                            "❌ Research cancelled",
                            playerId,
                            GameEvent.EventCategory.INFO
                    ));
                    // Note: No refund for cancelled research (design decision)
                }
            }
        }
    }

    /**
     * Create wall segments connecting a new wall post to nearby wall posts
     */
    private void createWallSegments(Building newWallPost) {
        if (newWallPost.getBuildingType() != BuildingType.WALL) {
            return;
        }

        Vector2 newPos = newWallPost.getPosition();
        double maxConnectionDistance = 200.0; // Maximum distance to connect wall posts
        double minConnectionDistance = 40.0; // Minimum distance (prevent overlapping posts)

        // Find all nearby wall posts from the same team
        for (Building building : buildings.values()) {
            if (building.getId() == newWallPost.getId()) {
                continue; // Skip self
            }

            if (building.getBuildingType() != BuildingType.WALL) {
                continue; // Only connect to other wall posts
            }

            if (building.getTeamNumber() != newWallPost.getTeamNumber()) {
                continue; // Only connect to same team walls
            }

            // Only connect to completed wall posts
            if (building.isUnderConstruction()) {
                continue; // Skip wall posts still under construction
            }

            double distance = newPos.distance(building.getPosition());

            // Check if within connection range
            if (distance >= minConnectionDistance && distance <= maxConnectionDistance) {
                // Check if segment doesn't already exist
                if (!wallSegmentExists(newWallPost, building)) {
                    createWallSegment(newWallPost, building);
                }
            }
        }
    }

    /**
     * Check if a wall segment already exists between two posts
     */
    private boolean wallSegmentExists(Building post1, Building post2) {
        for (WallSegment segment : wallSegments.values()) {
            if ((segment.getPost1() == post1 && segment.getPost2() == post2) ||
                    (segment.getPost1() == post2 && segment.getPost2() == post1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a wall segment between two wall posts
     */
    private void createWallSegment(Building post1, Building post2) {
        int segmentId = IdGenerator.nextEntityId();
        WallSegment segment = new WallSegment(
                segmentId,
                post1,
                post2,
                post1.getOwnerId(),
                post1.getTeamNumber()
        );

        wallSegments.put(segmentId, segment);
        world.addBody(segment.getBody());

        log.info("Created wall segment {} connecting posts {} and {}",
                segmentId, post1.getId(), post2.getId());
    }

    /**
     * Remove wall segments connected to a destroyed post
     */
    private void removeWallSegmentsForPost(Building wallPost) {
        wallSegments.entrySet().removeIf(entry -> {
            WallSegment segment = entry.getValue();
            if (segment.getPost1() == wallPost || segment.getPost2() == wallPost) {
                world.removeBody(segment.getBody());
                log.debug("Removed wall segment {} (connected post destroyed)", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Find the nearest refinery for a worker unit
     */
    private Building findNearestRefinery(Unit worker) {
        log.debug("Finding refinery for worker {} (owner {}), total buildings: {}",
                worker.getId(), worker.getOwnerId(), buildings.size());

        Building nearestDropoff = null;
        double nearestDistance = Double.MAX_VALUE;

        // Search for both refineries AND headquarters, pick the nearest one
        for (Building building : buildings.values()) {
            if (building.isActive() &&
                    building.getOwnerId() == worker.getOwnerId() &&
                    !building.isUnderConstruction() &&
                    (building.getBuildingType() == BuildingType.REFINERY ||
                            building.getBuildingType() == BuildingType.HEADQUARTERS)) {

                double distance = worker.getPosition().distance(building.getPosition());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestDropoff = building;
                    log.debug("  Found {} {} at distance {}",
                            building.getBuildingType(), building.getId(), distance);
                }
            }
        }

        log.debug("Returning nearest dropoff: {} (type: {})",
                nearestDropoff != null ? nearestDropoff.getId() : "null",
                nearestDropoff != null ? nearestDropoff.getBuildingType() : "none");
        return nearestDropoff;
    }

    /**
     * Find nearest headquarters for a miner to repair pickaxe
     */
    private Building findNearestHeadquarters(Unit miner) {
        Building nearestHQ = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Building building : buildings.values()) {
            if (building.isActive() &&
                    building.getOwnerId() == miner.getOwnerId() &&
                    !building.isUnderConstruction() &&
                    building.getBuildingType() == BuildingType.HEADQUARTERS) {

                double distance = miner.getPosition().distance(building.getPosition());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestHQ = building;
                }
            }
        }

        return nearestHQ;
    }

    /**
     * Check if a projectile is explosive (creates explosions)
     */
    private boolean isExplosiveProjectile(Projectile projectile) {
        Ordinance ordinance = projectile.getOrdinance();
        return ordinance == Ordinance.ROCKET ||
                ordinance == Ordinance.GRENADE ||
                ordinance == Ordinance.SHELL;
    }

    /**
     * Create an explosion at a projectile's position (when it reaches max range)
     */
    private void createExplosionAtProjectile(Projectile projectile) {
        FieldEffect effect = new FieldEffect(
                projectile.getOwnerId(),
                FieldEffectType.EXPLOSION,
                projectile.getPosition().copy(),
                projectile.getOrdinance().getSize() * 15,
                projectile.getDamage() * 0.5,
                FieldEffectType.EXPLOSION.getDefaultDuration(),
                projectile.getOwnerTeam()
        );
        gameEntities.getFieldEffects().put(effect.getId(), effect);
        world.addBody(effect.getBody());
    }

    /**
     * Process field effect updates and cleanup
     * Damage is now handled by the collision processor using physics-based detection
     */
    private void processFieldEffects(double deltaTime) {
        for (FieldEffect effect : fieldEffects.values()) {
            if (!effect.isActive()) {
                continue;
            }

            // Update field effect state (radius growth, expiration, etc.)
            effect.update(gameEntities);
        }

        // Remove inactive field effects and their physics bodies
        fieldEffects.entrySet().removeIf(entry -> {
            FieldEffect effect = entry.getValue();
            if (!effect.isActive() || effect.isExpired()) {
                world.removeBody(effect.getBody());
                log.debug("Removed expired field effect {} ({})", effect.getId(), effect.getType());
                return true;
            }
            return false;
        });
    }

    /**
     * Check for disconnected players and mark them as eliminated
     */
    private void checkDisconnectedPlayers() {
        // Check if any player sessions are closed/disconnected
        playerSessions.entrySet().removeIf(entry -> {
            int playerId = entry.getKey();
            PlayerSession session = entry.getValue();

            // Check if session is closed
            if (!session.getSession().isOpen()) {
                if (!disconnectedPlayers.contains(playerId)) {
                    disconnectedPlayers.add(playerId);

                    PlayerFaction faction = playerFactions.get(playerId);
                    if (faction != null) {
                        log.info("Player {} ({}) disconnected from game {}",
                                playerId, faction.getPlayerName(), gameId);

                        // Notify all players of disconnect
                        sendGameEvent(GameEvent.builder()
                                .message(String.format("⚠️ %s has disconnected from the game",
                                        faction.getPlayerName()))
                                .category(GameEvent.EventCategory.SYSTEM)
                                .color("#FFA500")
                                .target(GameEvent.EventTarget.builder()
                                        .type(GameEvent.EventTarget.TargetType.ALL)
                                        .build())
                                .displayDuration(5000L)
                                .build()
                        );
                    }
                }
                return true; // Remove from active sessions
            }
            return false;
        });
    }

    /**
     * Check win conditions (HQ destruction or last player standing)
     */
    private void checkWinConditions() {
        // Don't check win conditions for first 5 seconds (let players join)
        if (System.currentTimeMillis() - gameStartTime < 5000) {
            return;
        }

        // Count active HQs per team
        Map<Integer, Boolean> teamHasHQ = new HashMap<>();

        for (Building building : buildings.values()) {
            if (building.isActive() && building.getBuildingType() == BuildingType.HEADQUARTERS) {
                teamHasHQ.put(building.getTeamNumber(), true);
                log.debug("Found active HQ for team {}", building.getTeamNumber());
            }
        }

        log.debug("Win condition check: {} teams have HQs: {}", teamHasHQ.size(), teamHasHQ.keySet());

        // Count how many teams actually have ACTIVE (non-disconnected) players
        Set<Integer> teamsWithActivePlayers = new HashSet<>();
        for (PlayerFaction faction : playerFactions.values()) {
            if (faction.getTeamNumber() > 0 && !disconnectedPlayers.contains(faction.getPlayerId())) {
                teamsWithActivePlayers.add(faction.getTeamNumber());
            }
        }

        // Don't check win conditions if game just started (need at least 2 teams with players)
        if (teamsWithActivePlayers.size() < 2 && !gameStartedWithFullRoster) {
            log.debug("Not enough teams with active players ({}), waiting for more players", teamsWithActivePlayers.size());
            return; // Wait for all players to join
        }

        // Check for last player standing (all other players disconnected)
        if (gameStartedWithFullRoster && teamsWithActivePlayers.size() == 1) {
            winningTeam = teamsWithActivePlayers.iterator().next();
            gameOver = true;

            log.info("Game Over! Team {} wins - all opponents disconnected", winningTeam);

            // Send game over message
            Map<String, Object> gameOverMsg = new HashMap<>();
            gameOverMsg.put("type", "gameOver");
            gameOverMsg.put("winningTeam", winningTeam);
            gameOverMsg.put("reason", "Victory - All opponents disconnected");
            broadcast(gameOverMsg);
            return;
        }

        // Check if all players disconnected
        if (teamsWithActivePlayers.isEmpty()) {
            gameOver = true;
            winningTeam = -1;

            log.info("Game Over! All players disconnected");

            Map<String, Object> gameOverMsg = new HashMap<>();
            gameOverMsg.put("type", "gameOver");
            gameOverMsg.put("winningTeam", -1);
            gameOverMsg.put("reason", "Game ended - All players disconnected");
            broadcast(gameOverMsg);
            return;
        }

        // Check if only one team has an HQ remaining
        if (teamHasHQ.size() == 1) {
            winningTeam = teamHasHQ.keySet().iterator().next();
            gameOver = true;

            log.info("Game Over! Team {} wins by destroying all enemy headquarters", winningTeam);

            // Send game over message
            Map<String, Object> gameOverMsg = new HashMap<>();
            gameOverMsg.put("type", "gameOver");
            gameOverMsg.put("winningTeam", winningTeam);
            gameOverMsg.put("reason", "All enemy headquarters destroyed");
            broadcast(gameOverMsg);

        } else if (teamHasHQ.isEmpty()) {
            // All HQs destroyed - draw
            gameOver = true;
            winningTeam = -1;

            log.info("Game Over! Draw - all headquarters destroyed");

            Map<String, Object> gameOverMsg = new HashMap<>();
            gameOverMsg.put("type", "gameOver");
            gameOverMsg.put("winningTeam", -1);
            gameOverMsg.put("reason", "Draw - all headquarters destroyed");
            broadcast(gameOverMsg);
        }
    }

    /**
     * Recalculate upkeep for all factions by counting all active units
     * This ensures accuracy even if units are created outside normal spawn flow
     */
    private void recalculateUpkeep() {
        // Reset all faction upkeep and unit counts
        playerFactions.values().forEach(faction -> {
            faction.setCurrentUpkeep(0);
            faction.setUnitCount(0);
        });

        // Count all active units
        units.values().stream()
                .filter(Unit::isActive)
                .forEach(unit -> {
                    PlayerFaction faction = playerFactions.get(unit.getOwnerId());
                    if (faction != null) {
                        faction.addUpkeep(unit.getUnitType().getUpkeepCost());
                        faction.incrementUnitCount();
                    }
                });

        // Apply upkeep bonuses from Command Citadels
        applyUpkeepBonuses();
    }

    /**
     * Apply upkeep bonuses from Command Citadels
     */
    private void applyUpkeepBonuses() {
        playerFactions.forEach((playerId, faction) -> {
            // Calculate base max upkeep from faction definition
            int baseMaxUpkeep = faction.getFactionDefinition().getUpkeepLimit(250);

            // Add bonuses from active Command Citadels
            int upkeepBonus = buildings.values().stream()
                    .filter(b -> b.getOwnerId() == playerId)
                    .filter(b -> b.getBuildingType() == BuildingType.COMMAND_CITADEL)
                    .filter(b -> !b.isUnderConstruction())
                    .mapToInt(Building::getUpkeepBonus)
                    .sum();

            faction.setMaxUpkeep(baseMaxUpkeep + upkeepBonus);
        });
    }

    /**
     * Recalculate power generation and consumption for all factions
     * Low power stops all unit production
     */
    private void recalculatePower() {
        playerFactions.values().forEach(faction -> {
            int generated = 0;
            int consumed = 0;

            // Count power from all completed buildings (not under construction)
            for (Building building : buildings.values()) {
                if (building.getOwnerId() == faction.getPlayerId() &&
                        building.isActive() &&
                        !building.isUnderConstruction()) {

                    int powerValue = building.getBuildingType().getPowerValue();
                    if (powerValue > 0) {
                        generated += powerValue;
                    } else if (powerValue < 0) {
                        consumed += Math.abs(powerValue);
                    }
                }
            }

            boolean previousLowPower = faction.isHasLowPower();
            faction.setPowerGenerated(generated);
            faction.setPowerConsumed(consumed);
            faction.setHasLowPower(consumed > generated);

            // Send warning when power becomes low
            if (!previousLowPower && faction.isHasLowPower()) {
                log.warn("Player {} has LOW POWER: {}/{}",
                        faction.getPlayerId(), generated, consumed);
                sendGameEvent(GameEvent.createPlayerEvent(
                        "⚡ LOW POWER! All production stopped. Build more Power Plants!",
                        faction.getPlayerId(),
                        GameEvent.EventCategory.WARNING
                ));
            }

            // Send info when power is restored
            if (previousLowPower && !faction.isHasLowPower()) {
                log.info("Player {} power restored: {}/{}",
                        faction.getPlayerId(), generated, consumed);
                sendGameEvent(GameEvent.createPlayerEvent(
                        "✓ Power restored! Production resumed.",
                        faction.getPlayerId(),
                        GameEvent.EventCategory.INFO
                ));
            }
        });
    }

    /**
     * Update research progress for all players
     */
    private void updateResearch(double deltaTime) {
        playerFactions.forEach((playerId, faction) -> {
            if (faction.getResearchManager() == null) {
                return;
            }

            // Update research progress and get completed research
            List<ResearchType> completedResearch =
                    faction.getResearchManager().updateResearch(deltaTime);

            // Notify player of completed research and apply modifiers to existing units
            for (ResearchType research : completedResearch) {
                log.info("Player {} completed research: {}", playerId, research.getDisplayName());

                // Send notification to player
                sendGameEvent(GameEvent.createPlayerEvent(
                        "🔬 Research Complete: " + research.getDisplayName(),
                        playerId,
                        GameEvent.EventCategory.INFO
                ));

                // Apply research modifiers to all existing units owned by this player
                ResearchModifier modifier = research.getModifier();
                units.values().stream()
                        .filter(unit -> unit.getOwnerId() == playerId)
                        .forEach(unit -> unit.applyResearchModifiers(modifier));

                // Note: Buildings don't need retroactive modifier application
                // Research affects new buildings via faction modifiers at creation time
            }
        });
    }

    /**
     * Remove inactive entities
     */
    private void removeInactiveEntities() {
        projectiles.entrySet().removeIf(e -> {
            Projectile value = e.getValue();
            if (!value.isActive()) {
                world.removeBody(value.getBody());
                return true;
            }
            return false;
        });

        beams.entrySet().removeIf(e -> {
            Beam beam = e.getValue();
            if (!beam.isActive()) {
                world.removeBody(beam.getBody());
                return true;
            }
            return false;
        });

        units.entrySet().removeIf(entry -> {
            Unit unit = entry.getValue();
            if (!unit.isActive()) {
                // Unregister android from its factory (if it's an android)
                if (unit.getAndroidFactoryId() != null) {
                    Building factory = buildings.get(unit.getAndroidFactoryId());
                    if (factory != null && factory.isActive()) {
                        factory.getComponent(AndroidFactoryComponent.class)
                                .ifPresent(a -> a.unregisterAndroid(unit.getId()));
                    }
                }

                // Send unit death notification (throttled to avoid spam)
                int ownerId = unit.getOwnerId();
                long currentTime = System.currentTimeMillis();
                Long lastNotification = lastUnitDeathNotification.get(ownerId);

                if (lastNotification == null || (currentTime - lastNotification) >= UNIT_DEATH_NOTIFICATION_COOLDOWN) {
                    PlayerFaction faction = playerFactions.get(ownerId);
                    if (faction != null) {
                        String unitName = unit.getUnitType().name()
                                .replace("_", " ")
                                .toLowerCase();
                        // Capitalize first letter
                        unitName = unitName.substring(0, 1).toUpperCase() + unitName.substring(1);

                        sendGameEvent(GameEvent.createPlayerEvent(
                                "⚠️ Your " + unitName + " was destroyed!",
                                ownerId,
                                GameEvent.EventCategory.WARNING
                        ));
                        lastUnitDeathNotification.put(ownerId, currentTime);
                    }
                }

                world.removeBody(unit.getBody());
                // Note: Upkeep is now recalculated periodically, no need to adjust here
                return true;
            }
            return false;
        });

        buildings.entrySet().removeIf(entry -> {
            Building building = entry.getValue();
            if (!building.isActive()) {
                // Send event for HQ destruction
                if (building.getBuildingType() == BuildingType.HEADQUARTERS &&
                        !eliminatedTeams.contains(building.getTeamNumber())) {

                    eliminatedTeams.add(building.getTeamNumber());

                    String teamName = "Team " + building.getTeamNumber();
                    sendGameEvent(GameEvent.builder()
                            .message(String.format("💥 %s's Headquarters has been destroyed!", teamName))
                            .category(GameEvent.EventCategory.SYSTEM)
                            .color("#FF4444")
                            .target(GameEvent.EventTarget.builder()
                                    .type(GameEvent.EventTarget.TargetType.ALL)
                                    .build())
                            .displayDuration(5000L)
                            .build()
                    );
                }

                // Send notification for important building destructions (to the owner)
                PlayerFaction faction = playerFactions.get(building.getOwnerId());
                if (faction != null && !building.isUnderConstruction()) {
                    // Notify for important buildings only
                    boolean shouldNotify = switch (building.getBuildingType()) {
                        case REFINERY -> true;  // Resource collection
                        case FACTORY, ADVANCED_FACTORY -> true;  // Unit production
                        case BARRACKS -> true;  // Unit production
                        case TECH_CENTER -> true;  // Tech unlocks
                        case RESEARCH_LAB -> true;  // Research
                        case POWER_PLANT -> true;  // Power generation
                        case BANK -> true;  // Economy
                        case SHIELD_GENERATOR -> true;  // Defense
                        case TURRET -> true;  // Defense
                        case BUNKER -> true;  // Defense
                        // Monuments
                        case PHOTON_SPIRE, ANDROID_FACTORY, SANDSTORM_GENERATOR, COMMAND_CITADEL -> true;
                        default -> false;  // Don't notify for walls, etc.
                    };

                    if (shouldNotify) {
                        String buildingName = building.getBuildingType().name()
                                .replace("_", " ")
                                .toLowerCase();
                        // Capitalize first letter
                        buildingName = buildingName.substring(0, 1).toUpperCase() + buildingName.substring(1);

                        sendGameEvent(GameEvent.createPlayerEvent(
                                "🔥 Your " + buildingName + " was destroyed!",
                                building.getOwnerId(),
                                GameEvent.EventCategory.WARNING
                        ));
                    }
                }

                // Remove wall segments connected to this wall post
                if (building.getBuildingType() == BuildingType.WALL) {
                    removeWallSegmentsForPost(building);
                }

                // Ungarrison all units from bunkers when destroyed
                if (building.getBuildingType() == BuildingType.BUNKER && building.getGarrisonCount() > 0) {
                    List<Unit> ungarrisonedUnits = building.ungarrisonAllUnits();
                    log.info("Bunker {} destroyed - ungarrisoned {} units", building.getId(), ungarrisonedUnits.size());

                    // Re-enable units in physics world
                    for (Unit unit : ungarrisonedUnits) {
                        if (!units.containsKey(unit.getId())) {
                            // Add unit back to the units map if it was removed
                            units.put(unit.getId(), unit);
                        }
                        // Ensure body is added to world (it should already be enabled by ungarrisonUnit)
                        if (!world.containsBody(unit.getBody())) {
                            world.addBody(unit.getBody());
                        }
                    }
                }

                // Call onDestroy for all components (handles sandstorm cleanup, etc.)
                for (IBuildingComponent component : building.getComponents().values()) {
                    component.onDestroy();
                }

                world.removeBody(building.getBody());
                return true;
            }
            return false;
        });

        // Remove wall segments with destroyed posts or that are themselves destroyed
        wallSegments.entrySet().removeIf(entry -> {
            WallSegment segment = entry.getValue();
            if (!segment.isActive() || segment.hasDestroyedPost()) {
                world.removeBody(segment.getBody());
                log.debug("Removed wall segment {}", entry.getKey());
                return true;
            }
            return false;
        });

        resourceDeposits.entrySet().removeIf(entry -> {
            ResourceDeposit deposit = entry.getValue();
            if (!deposit.isActive()) {
                world.removeBody(deposit.getBody());
                return true;
            }
            return false;
        });

        // Remove destroyed obstacles
        obstacles.entrySet().removeIf(entry -> {
            Obstacle obstacle = entry.getValue();
            if (!obstacle.isActive()) {
                world.removeBody(obstacle.getBody());
                log.debug("Removed destroyed obstacle {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Check if faction can afford a building (uses faction-modified cost)
     */
    private boolean canAffordBuilding(PlayerFaction faction, BuildingType buildingType) {
        // Check if faction can build this building type
        if (!faction.canBuildBuilding(buildingType)) {
            return false;
        }

        // Use faction-modified cost
        int cost = faction.getBuildingCost(buildingType);
        return faction.hasResources(ResourceType.CREDITS, cost);
    }

    /**
     * Check if faction can afford a unit (uses faction-modified cost)
     */
    private boolean canAffordUnit(PlayerFaction faction, UnitType unitType) {
        // Check if faction can build this unit type
        if (!faction.canBuildUnit(unitType)) {
            return false;
        }

        // Use faction-modified cost
        int cost = faction.getUnitCost(unitType);
        return faction.hasResources(ResourceType.CREDITS, cost)
                && faction.canBuildMoreUnits()
                && faction.canAffordUpkeep(unitType.getUpkeepCost());
    }

    /**
     * Check if player has the required tech buildings to construct this building
     */
    private boolean hasTechRequirements(int playerId, BuildingType buildingType) {
        // Get player's completed buildings
        Set<BuildingType> playerBuildings = buildings.values().stream()
                .filter(b -> b.getOwnerId() == playerId && b.isActive() && !b.isUnderConstruction())
                .map(Building::getBuildingType)
                .collect(Collectors.toSet());

        // Define tech requirements (must match FactionInfoService)
        return switch (buildingType) {
            // T1 - Always available
            case POWER_PLANT, BARRACKS, REFINERY, BUNKER, WALL -> true;

            // T2 - Requires Power Plant
            case RESEARCH_LAB, FACTORY, WEAPONS_DEPOT, TURRET, SHIELD_GENERATOR ->
                    playerBuildings.contains(BuildingType.POWER_PLANT);

            // T3 - Requires Power Plant + Research Lab
            case TECH_CENTER, ADVANCED_FACTORY, BANK -> playerBuildings.contains(BuildingType.POWER_PLANT) &&
                    playerBuildings.contains(BuildingType.RESEARCH_LAB);

            // Monument Buildings - Requires Power Plant + Research Lab (T3)
            case SANDSTORM_GENERATOR, ANDROID_FACTORY, PHOTON_SPIRE, COMMAND_CITADEL ->
                    playerBuildings.contains(BuildingType.POWER_PLANT) &&
                            playerBuildings.contains(BuildingType.RESEARCH_LAB);

            // Headquarters is special (only one, starting building)
            case HEADQUARTERS -> false; // Cannot build additional HQs
        };
    }

    /**
     * Validate building placement location
     */
    private boolean isValidBuildLocation(Vector2 location, BuildingType buildingType, int playerId) {
        // Check if player has a worker selected that can build
        boolean hasWorkerSelected = units.values().stream()
                .anyMatch(u -> u.belongsTo(playerId) && u.isSelected() && u.getUnitType().canBuild());

        if (!hasWorkerSelected) {
            log.debug("No worker selected to build");
            return false;
        }

        // Use collision processor for all spatial validation
        return collisionProcessor.isValidBuildLocation(
                location,
                buildingType,
                resourceDeposits,
                gameConfig.getWorldWidth(),
                gameConfig.getWorldHeight()
        );
    }

    /**
     * Send game state to all players (with fog of war)
     */
    private void sendGameState() {
        // Send personalized game state to each player based on their vision
        playerSessions.forEach((playerId, playerSession) -> {
            PlayerFaction faction = playerFactions.get(playerId);
            if (faction != null) {
                Map<String, Object> gameState = createGameStateForTeam(faction.getTeamNumber());
                send(playerSession.getSession(), gameState);
            }
        });
    }

    /**
     * Create game state for a specific team (with fog of war applied)
     */
    private Map<String, Object> createGameStateForTeam(int teamNumber) {
        Map<String, Object> state = new HashMap<>();
        state.put("type", "gameState");
        state.put("timestamp", System.currentTimeMillis());

        // Apply fog of war - only send visible units and buildings
        List<Unit> visibleUnits = FogOfWar.getVisibleUnits(
                units.values(),
                buildings.values(),
                teamNumber
        );

        List<Building> visibleBuildings = FogOfWar.getVisibleBuildings(
                units.values(),
                buildings.values(),
                teamNumber
        );

        // Serialize visible units (exclude garrisoned units)
        List<Map<String, Object>> unitsList = visibleUnits.stream()
                .filter(u -> !u.isGarrisoned()) // Don't render garrisoned units
                .map(this::serializeUnit)
                .collect(Collectors.toList());
        state.put("units", unitsList);

        // Serialize visible buildings
        List<Map<String, Object>> buildingsList = visibleBuildings.stream()
                .map(this::serializeBuilding)
                .collect(Collectors.toList());
        state.put("buildings", buildingsList);

        // Projectiles are always visible (they're fast-moving)
        List<Map<String, Object>> projectilesList = projectiles.values().stream()
                .filter(Projectile::isActive)
                .map(this::serializeProjectile)
                .collect(Collectors.toList());
        state.put("projectiles", projectilesList);

        // Beams (instant-hit weapons)
        List<Map<String, Object>> beamsList = beams.values().stream()
                .filter(Beam::isActive)
                .map(this::serializeBeam)
                .collect(Collectors.toList());
        state.put("beams", beamsList);

        // Field effects (explosions, etc.)
        List<Map<String, Object>> fieldEffectsList = fieldEffects.values().stream()
                .filter(FieldEffect::isActive)
                .map(this::serializeFieldEffect)
                .collect(Collectors.toList());
        state.put("fieldEffects", fieldEffectsList);

        // Resource deposits and obstacles are always visible (map terrain)
        List<Map<String, Object>> depositsList = resourceDeposits.values().stream()
                .map(this::serializeResourceDeposit)
                .collect(Collectors.toList());
        state.put("resourceDeposits", depositsList);

        List<Map<String, Object>> obstaclesList = obstacles.values().stream()
                .map(this::serializeObstacle)
                .collect(Collectors.toList());
        state.put("obstacles", obstaclesList);

        // Wall segments are always visible (like obstacles)
        List<Map<String, Object>> wallSegmentsList = wallSegments.values().stream()
                .filter(WallSegment::isActive)
                .map(this::serializeWallSegment)
                .collect(Collectors.toList());
        state.put("wallSegments", wallSegmentsList);

        // Player factions - only send own faction's detailed info
        Map<Integer, Map<String, Object>> factionsMap = new HashMap<>();
        playerFactions.forEach((playerId, faction) -> {
            if (faction.getTeamNumber() == teamNumber) {
                // Full info for own team
                factionsMap.put(playerId, serializeFaction(faction));
            } else {
                // Limited info for other teams (just team number and name)
                Map<String, Object> limitedInfo = new HashMap<>();
                limitedInfo.put("playerId", faction.getPlayerId());
                limitedInfo.put("playerName", faction.getPlayerName());
                limitedInfo.put("team", faction.getTeamNumber());
                factionsMap.put(playerId, limitedInfo);
            }
        });
        state.put("factions", factionsMap);

        // Add biome info for client-side rendering
        Map<String, Object> biomeInfo = new HashMap<>();
        biomeInfo.put("name", rtsWorld.getBiome().name());
        biomeInfo.put("groundColor", rtsWorld.getBiome().getGroundColor());
        biomeInfo.put("obstacleColor", rtsWorld.getBiome().getObstacleColor());
        state.put("biome", biomeInfo);

        // Add world dimensions for client-side camera bounds
        state.put("worldWidth", gameConfig.getWorldWidth());
        state.put("worldHeight", gameConfig.getWorldHeight());

        return state;
    }

    /**
     * Extract vertices from all fixtures in a physics body
     * Returns a list of fixtures, where each fixture is a list of vertices (x,y pairs)
     * This supports multi-fixture bodies for compound shapes
     */
    private List<List<List<Double>>> extractBodyVertices(Body body) {
        List<List<List<Double>>> allFixtures = new ArrayList<>();

        if (body.getFixtureCount() == 0) {
            return allFixtures;
        }

        // Iterate through all fixtures in the body
        for (int i = 0; i < body.getFixtureCount(); i++) {
            List<List<Double>> fixtureVertices = new ArrayList<>();
            Convex convex = body.getFixture(i).getShape();

            // Check if it's a polygon
            if (convex instanceof Polygon polygon) {
                Vector2[] polyVertices = polygon.getVertices();
                for (Vector2 vertex : polyVertices) {
                    List<Double> point = new ArrayList<>();
                    point.add(vertex.x);
                    point.add(vertex.y);
                    fixtureVertices.add(point);
                }
            } else if (convex instanceof Circle circle) {
                // Approximate circle with vertices (16-sided polygon)
                int segments = 16;
                double radius = circle.getRadius();
                Vector2 center = circle.getCenter();

                for (int j = 0; j < segments; j++) {
                    double angle = (2.0 * Math.PI * j) / segments;
                    double x = center.x + radius * Math.cos(angle);
                    double y = center.y + radius * Math.sin(angle);

                    List<Double> point = new ArrayList<>();
                    point.add(x);
                    point.add(y);
                    fixtureVertices.add(point);
                }
            }

            // Only add non-empty fixtures
            if (!fixtureVertices.isEmpty()) {
                allFixtures.add(fixtureVertices);
            }
        }

        return allFixtures;
    }

    /**
     * Serialize a unit for network transmission
     */
    private Map<String, Object> serializeUnit(Unit unit) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", unit.getId());
        data.put("type", unit.getUnitType().name());
        data.put("x", unit.getPosition().x);
        data.put("y", unit.getPosition().y);
        data.put("rotation", unit.getRotation());
        data.put("health", unit.getHealth());
        data.put("maxHealth", unit.getMaxHealth());
        data.put("ownerId", unit.getOwnerId());
        data.put("team", unit.getTeamNumber());
        data.put("size", unit.getUnitType().getSize());
        data.put("visionRange", unit.getVisionRange()); // Use modified vision range
        data.put("selected", unit.isSelected());
        data.put("isMoving", unit.isMoving());
        data.put("aiStance", unit.getAiStance().name());
        data.put("specialAbility", unit.getUnitType().getSpecialAbility().name());
        data.put("specialAbilityActive", unit.isSpecialAbilityActive());
        data.put("specialAbilityReady", unit.isSpecialAbilityReady());

        // Cloak status (for Cloak Tank)
        data.put("cloaked", unit.isCloaked());

        // Add physics body vertices for accurate client-side rendering
        data.put("vertices", extractBodyVertices(unit.getBody()));

        // Serialize turrets if unit has them (e.g., deployed Crawler)
        if (!unit.getTurrets().isEmpty()) {
            List<Map<String, Object>> turretsData = new ArrayList<>();
            for (Turret turret : unit.getTurrets()) {
                Map<String, Object> turretData = new HashMap<>();
                turretData.put("index", turret.getIndex());
                turretData.put("offsetX", turret.getOffset().x);
                turretData.put("offsetY", turret.getOffset().y);
                turretData.put("rotation", turret.getRotation());
                turretsData.add(turretData);
            }
            data.put("turrets", turretsData);
        }

        // Serialize pickaxe durability for miners
        if (unit.getUnitType().canMine()) {
            data.put("pickaxeDurability", unit.getPickaxeDurability());
        }

        return data;
    }

    /**
     * Serialize a building for network transmission
     */
    private Map<String, Object> serializeBuilding(Building building) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", building.getId());
        data.put("type", building.getBuildingType().name());
        data.put("x", building.getPosition().x);
        data.put("y", building.getPosition().y);
        data.put("rotation", building.getRotation());
        data.put("health", building.getHealth());
        data.put("maxHealth", building.getMaxHealth());
        data.put("ownerId", building.getOwnerId());
        data.put("team", building.getTeamNumber());
        data.put("size", building.getBuildingType().getSize());
        data.put("visionRange", building.getBuildingType().getVisionRange());
        data.put("active", building.isActive());
        data.put("underConstruction", building.isUnderConstruction());
        data.put("constructionPercent", building.getConstructionPercent());
        data.put("productionPercent", building.getProductionPercent());
        data.put("productionQueueSize", building.getProductionQueueSize());
        data.put("canProduceUnits", building.getBuildingType().isCanProduceUnits());

        // Add physics body vertices for accurate client-side rendering
        data.put("vertices", extractBodyVertices(building.getBody()));

        // Rally point
        if (building.getRallyPoint() != null) {
            Map<String, Object> rallyData = new HashMap<>();
            rallyData.put("x", building.getRallyPoint().x);
            rallyData.put("y", building.getRallyPoint().y);
            data.put("rallyPoint", rallyData);
        }

        // Shield state (for Shield Generator buildings)
        building.getComponent(ShieldComponent.class)
                .ifPresent(component -> {
                    data.put("shieldActive", component.getSensorBody() != null);
                    data.put("shieldRadius", component.getSensorBody() != null ?
                            component.getSensorBody().getFixture(0).getShape().getRadius()
                            : 0);
                });

        // Garrison state (for Bunker)
        if (building.getBuildingType() == BuildingType.BUNKER) {
            data.put("garrisonCount", building.getGarrisonCount());
            data.put("maxGarrisonCapacity", building.getMaxGarrisonCapacity());
        }

        return data;
    }

    /**
     * Serialize a resource deposit for network transmission
     */
    private Map<String, Object> serializeResourceDeposit(ResourceDeposit deposit) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", deposit.getId());
        data.put("type", deposit.getResourceType().name());
        data.put("x", deposit.getPosition().x);
        data.put("y", deposit.getPosition().y);
        data.put("size", 40.0); // Radius for rendering and click detection
        data.put("remaining", deposit.getRemainingResources());
        data.put("max", deposit.getMaxResources());
        data.put("percent", deposit.getResourcePercent());
        return data;
    }

    /**
     * Serialize a faction for network transmission
     */
    private Map<String, Object> serializeFaction(PlayerFaction faction) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerId", faction.getPlayerId());
        data.put("playerName", faction.getPlayerName());
        data.put("team", faction.getTeamNumber());
        data.put("credits", faction.getResourceAmount(ResourceType.CREDITS));
        data.put("unitCount", faction.getUnitCount());
        data.put("maxUnits", faction.getMaxUnits());
        data.put("currentUpkeep", faction.getCurrentUpkeep());
        data.put("maxUpkeep", faction.getMaxUpkeep());
        data.put("powerGenerated", faction.getPowerGenerated());
        data.put("powerConsumed", faction.getPowerConsumed());
        data.put("hasLowPower", faction.isHasLowPower());

        // Faction-specific information
        data.put("factionType", faction.getFaction().name());
        data.put("factionName", faction.getFaction().getDisplayName());
        data.put("factionColor", faction.getFaction().getThemeColor());

        // Research information
        if (faction.getResearchManager() != null) {
            List<String> completedResearch = new ArrayList<>();
            for (ResearchType research :
                    faction.getResearchManager().getCompletedResearch()) {
                completedResearch.add(research.name());
            }
            data.put("completedResearch", completedResearch);

            // Active research (building ID -> research info)
            Map<Integer, Map<String, Object>> activeResearch = new HashMap<>();
            faction.getResearchManager().getActiveResearch().forEach((buildingId, progress) -> {
                Map<String, Object> researchInfo = new HashMap<>();
                researchInfo.put("researchType", progress.getResearchType().name());
                researchInfo.put("displayName", progress.getResearchType().getDisplayName());
                researchInfo.put("progress", progress.getProgressPercent());
                researchInfo.put("timeRemaining", progress.getTimeRemaining());
                activeResearch.put(buildingId, researchInfo);
            });
            data.put("activeResearch", activeResearch);
        }

        // Available units and buildings for this faction
        List<String> availableUnits = new ArrayList<>();
        for (UnitType unitType : UnitType.values()) {
            if (faction.canBuildUnit(unitType)) {
                availableUnits.add(unitType.name());
            }
        }
        data.put("availableUnits", availableUnits);

        List<String> availableBuildings = new ArrayList<>();
        for (BuildingType buildingType : BuildingType.values()) {
            if (faction.canBuildBuilding(buildingType)) {
                availableBuildings.add(buildingType.name());
            }
        }
        data.put("availableBuildings", availableBuildings);

        // Faction-modified costs for units (client needs this for UI)
        Map<String, Integer> unitCosts = new HashMap<>();
        Map<String, Integer> unitUpkeep = new HashMap<>();
        for (UnitType unitType : UnitType.values()) {
            if (faction.canBuildUnit(unitType)) {
                unitCosts.put(unitType.name(), faction.getUnitCost(unitType));
                unitUpkeep.put(unitType.name(), unitType.getUpkeepCost());
            }
        }
        data.put("unitCosts", unitCosts);
        data.put("unitUpkeep", unitUpkeep);

        // Faction-modified costs for buildings
        Map<String, Integer> buildingCosts = new HashMap<>();
        for (BuildingType buildingType : BuildingType.values()) {
            if (faction.canBuildBuilding(buildingType)) {
                buildingCosts.put(buildingType.name(), faction.getBuildingCost(buildingType));
            }
        }
        data.put("buildingCosts", buildingCosts);

        return data;
    }

    /**
     * Serialize an obstacle for network transmission
     */
    private Map<String, Object> serializeObstacle(Obstacle obstacle) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", obstacle.getId());
        data.put("x", obstacle.getPosition().x);
        data.put("y", obstacle.getPosition().y);
        data.put("size", obstacle.getSize());
        data.put("shape", obstacle.getShape().name());
        data.put("width", obstacle.getWidth());
        data.put("height", obstacle.getHeight());
        data.put("sides", obstacle.getSides());
        data.put("destructible", obstacle.isDestructible());
        data.put("health", obstacle.getHealth());
        data.put("maxHealth", obstacle.getMaxHealth());

        // Include vertices for irregular polygons
        if (obstacle.getShape() == Obstacle.Shape.IRREGULAR_POLYGON && obstacle.getVertices() != null) {
            List<Map<String, Double>> verticesList = new ArrayList<>();
            for (Vector2 vertex : obstacle.getVertices()) {
                Map<String, Double> vertexData = new HashMap<>();
                vertexData.put("x", vertex.x);
                vertexData.put("y", vertex.y);
                verticesList.add(vertexData);
            }
            data.put("vertices", verticesList);
        }

        return data;
    }

    /**
     * Serialize a wall segment for network transmission
     */
    private Map<String, Object> serializeWallSegment(WallSegment segment) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", segment.getId());
        data.put("x", segment.getPosition().x);
        data.put("y", segment.getPosition().y);
        data.put("rotation", segment.getRotation());
        data.put("length", segment.getLength());
        data.put("health", segment.getHealth());
        data.put("maxHealth", segment.getMaxHealth());
        data.put("team", segment.getTeamNumber());
        data.put("post1Id", segment.getPost1().getId());
        data.put("post2Id", segment.getPost2().getId());
        return data;
    }

    /**
     * Serialize a projectile for network transmission
     */
    private Map<String, Object> serializeProjectile(Projectile projectile) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", projectile.getId());
        data.put("x", projectile.getPosition().x);
        data.put("y", projectile.getPosition().y);
        // Get velocity from physics body (single source of truth)
        Vector2 velocity = projectile.getBody().getLinearVelocity();
        data.put("vx", velocity.x);
        data.put("vy", velocity.y);
        data.put("rotation", projectile.getRotation());
        data.put("ownerId", projectile.getOwnerId());
        data.put("team", projectile.getOwnerTeam());
        data.put("ordinance", projectile.getOrdinance().name());
        data.put("size", projectile.getSize()); // Use actual projectile size
        return data;
    }

    /**
     * Serialize a field effect for network transmission
     */
    private Map<String, Object> serializeFieldEffect(FieldEffect effect) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", effect.getId());
        data.put("type", effect.getType().name());
        data.put("x", effect.getPosition().x);
        data.put("y", effect.getPosition().y);
        data.put("radius", effect.getRadius());
        data.put("progress", effect.getProgress());
        data.put("team", effect.getOwnerTeam());
        return data;
    }

    /**
     * Add an AI player for testing/debug games
     */
    public void addAIPlayer() {
        // Create a dummy player session for AI
        int aiPlayerId = -1; // Negative ID for AI

        // Assign AI to team 2
        int aiTeam = 2;

        // Create AI faction
        PlayerFaction aiFaction = new PlayerFaction(
                aiPlayerId,
                aiTeam,
                "AI Player"
        );
        playerFactions.put(aiPlayerId, aiFaction);

        // Create AI starting base
        Vector2 aiStartPosition = getStartingPosition(aiTeam);
        createStartingBase(aiPlayerId, aiTeam, aiStartPosition);

        log.info("AI Player added to team {} at position ({}, {}). Total buildings: {}",
                aiTeam, aiStartPosition.x, aiStartPosition.y, buildings.size());

        // Log all HQs
        buildings.values().stream()
                .filter(b -> b.getBuildingType() == BuildingType.HEADQUARTERS)
                .forEach(hq -> log.info("HQ exists for team {} at ({}, {})",
                        hq.getTeamNumber(), hq.getPosition().x, hq.getPosition().y));
    }

    /**
     * Add a player to the game
     */
    public synchronized boolean addPlayer(PlayerSession playerSession, String factionName) {
        // Prevent late joins if game has started with full roster
        if (gameStartedWithFullRoster) {
            log.warn("Player {} attempted to join game {} after it started with full roster",
                    playerSession.getPlayerId(), gameId);
            return false;
        }

        if (playerSessions.size() >= gameConfig.getMaxPlayers()) {
            return false;
        }

        playerSessions.put(playerSession.getPlayerId(), playerSession);

        // Check if game is now at full capacity
        if (playerSessions.size() == gameConfig.getMaxPlayers()) {
            gameStartedWithFullRoster = true;
            log.info("Game {} has reached full capacity ({} players) - late joins now prevented",
                    gameId, gameConfig.getMaxPlayers());

            // Notify all players that game is starting with full roster
            sendGameEvent(GameEvent.builder()
                    .message("🎮 Game starting with full roster! Late joins disabled.")
                    .category(GameEvent.EventCategory.SYSTEM)
                    .color("#00FF00")
                    .target(GameEvent.EventTarget.builder()
                            .type(GameEvent.EventTarget.TargetType.ALL)
                            .build())
                    .displayDuration(5000L)
                    .build()
            );
        }

        log.info("Adding player {} to game {}, current factions: {}, selected faction: {}",
                playerSession.getPlayerId(), gameId, playerFactions.keySet(), factionName);

        // Assign team
        int teamNumber = assignPlayerToTeam();

        log.info("Assigned player {} to team {}", playerSession.getPlayerId(), teamNumber);

        // Parse faction (default to TERRAN if invalid)
        Faction selectedFaction = Faction.TERRAN;
        log.info("DEBUG: addPlayer called with factionName={}", factionName);
        if (factionName != null) {
            try {
                selectedFaction = Faction.valueOf(factionName);
                log.info("DEBUG: Successfully parsed faction: {}", selectedFaction);
            } catch (IllegalArgumentException e) {
                log.warn("DEBUG: Invalid faction '{}', defaulting to TERRAN. Valid values: {}",
                        factionName, java.util.Arrays.toString(Faction.values()));
            }
        } else {
            log.warn("DEBUG: factionName is null, defaulting to TERRAN");
        }

        // Create faction with selected faction type
        PlayerFaction faction = new PlayerFaction(
                playerSession.getPlayerId(),
                teamNumber,
                playerSession.getPlayerName(),
                selectedFaction
        );
        playerFactions.put(playerSession.getPlayerId(), faction);

        log.info("DEBUG: Created PlayerFaction for player {}: faction={}, factionDefinition={}",
                playerSession.getPlayerId(), faction.getFaction(), faction.getFactionDefinition() != null);

        // Create starting base
        Vector2 startPosition = getStartingPosition(teamNumber);
        createStartingBase(playerSession.getPlayerId(), teamNumber, startPosition);

        log.info("Player {} joined RTS game {} on team {} with faction {}",
                playerSession.getPlayerName(), gameId, teamNumber, selectedFaction);

        return true;
    }

    /**
     * Assign player to team with fewest members
     */
    private int assignPlayerToTeam() {
        // Count HUMAN players per team (exclude AI player -1)
        int[] teamCounts = new int[gameConfig.getMaxPlayers() + 1];
        playerFactions.values().forEach(faction -> {
            // Only count human players (playerId >= 0)
            if (faction.getPlayerId() >= 0) {
                int team = faction.getTeamNumber();
                if (team > 0 && team <= gameConfig.getMaxPlayers()) {
                    teamCounts[team]++;
                }
            }
        });

        log.info("Team counts (human players only): {}", Arrays.toString(teamCounts));

        // Find team with fewest players
        int bestTeam = 1;
        int minCount = Integer.MAX_VALUE;
        for (int team = 1; team <= gameConfig.getMaxPlayers(); team++) {
            if (teamCounts[team] < minCount) {
                minCount = teamCounts[team];
                bestTeam = team;
            }
        }

        log.info("Assigning to team {} (minCount: {})", bestTeam, minCount);

        return bestTeam;
    }

    /**
     * Get starting position for a team from RTSWorld
     */
    private Vector2 getStartingPosition(int teamNumber) {
        // Team numbers are 1-indexed, but world uses 0-indexed
        return rtsWorld.getTeamStartPoint(teamNumber - 1);
    }

    /**
     * Create starting base for a player
     */
    private void createStartingBase(int playerId, int teamNumber, Vector2 position) {
        // Get player faction for modifiers
        PlayerFaction faction = playerFactions.get(playerId);

        // Create headquarters (with faction-modified health)
        double hqMaxHealth = faction != null
                ? faction.getBuildingHealth(BuildingType.HEADQUARTERS)
                : BuildingType.HEADQUARTERS.getMaxHealth();
        Building hq = new Building(
                IdGenerator.nextEntityId(),
                gameEntities,
                BuildingType.HEADQUARTERS,
                position.x, position.y,
                playerId,
                teamNumber,
                hqMaxHealth
        );
        buildings.put(hq.getId(), hq);
        world.addBody(hq.getBody());

        log.info("Created HQ {} for player {} (team {}): active={}, underConstruction={}",
                hq.getId(), playerId, teamNumber, hq.isActive(), hq.isUnderConstruction());

        // Create starting workers
        for (int i = 0; i < 3; i++) {
            double angle = (Math.PI * 2 * i) / 3;
            double offset = 100;
            double x = position.x + Math.cos(angle) * offset;
            double y = position.y + Math.sin(angle) * offset;

            Unit worker = new Unit(
                    IdGenerator.nextEntityId(),
                    UnitType.WORKER,
                    x, y,
                    playerId,
                    teamNumber
            );

            // Apply research modifiers (though starting units won't have research yet)
            if (faction != null && faction.getResearchManager() != null) {
                worker.applyResearchModifiers(faction.getResearchManager().getCumulativeModifier());
            }

            units.put(worker.getId(), worker);
            world.addBody(worker.getBody());
        }

        log.info("Created starting base for player {} at ({}, {})", playerId, position.x, position.y);
    }

    /**
     * Remove a player from the game
     */
    public void removePlayer(int playerId) {
        playerSessions.remove(playerId);
        playerFactions.remove(playerId);

        // Remove player's units and buildings
        units.entrySet().removeIf(entry -> {
            if (entry.getValue().belongsTo(playerId)) {
                world.removeBody(entry.getValue().getBody());
                return true;
            }
            return false;
        });

        buildings.entrySet().removeIf(entry -> {
            if (entry.getValue().belongsTo(playerId)) {
                world.removeBody(entry.getValue().getBody());
                return true;
            }
            return false;
        });

        log.info("Player {} removed from RTS game {}", playerId, gameId);
    }

    /**
     * Accept player input
     */
    public void acceptPlayerInput(int playerId, RTSPlayerInput input) {
        if (input != null) {
            playerInputs.put(playerId, input);
        }
    }

    /**
     * Broadcast message to all players
     */
    public void broadcast(Object message) {
        playerSessions.values().forEach(player -> {
            if (player.getSession().isOpen()) {
                send(player.getSession(), message);
            }
        });
    }

    /**
     * Send a GameEvent to specific players based on targeting
     */
    public void sendGameEvent(GameEvent event) {
        GameEvent.EventTarget target = event.getTarget();

        if (target == null || target.getType() == GameEvent.EventTarget.TargetType.ALL) {
            // Broadcast to all players
            broadcast(event);
            return;
        }

        playerSessions.values().forEach(player -> {
            if (!player.getSession().isOpen()) {
                return;
            }

            int playerId = player.getPlayerId();
            PlayerFaction faction = playerFactions.get(playerId);
            if (faction == null) {
                return;
            }

            boolean shouldReceive = false;

            switch (target.getType()) {
                case TEAM:
                    if (target.getTeamIds() != null && target.getTeamIds().contains(faction.getTeamNumber())) {
                        shouldReceive = true;
                    }
                    break;
                case SPECIFIC:
                    if (target.getPlayerIds() != null && target.getPlayerIds().contains(playerId)) {
                        shouldReceive = true;
                    }
                    break;
                case SPECTATORS:
                    // TODO: Implement spectator system
                    break;
            }

            // Check exclusions
            if (shouldReceive && target.getExcludePlayerIds() != null &&
                    target.getExcludePlayerIds().contains(playerId)) {
                shouldReceive = false;
            }

            if (shouldReceive) {
                send(player.getSession(), event);
            }
        });
    }

    /**
     * Send message to specific session
     */
    public void send(WebSocketSession session, Object message) {
        try {
            if (session.isWritable() && session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendAsync(json);
            }
        } catch (JsonProcessingException e) {
            log.error("Error serializing message", e);
        } catch (WebSocketSessionException e) {
            if (!(e.getCause() instanceof InterruptedException)) {
                log.error("Error sending message", e);
            }
        }
    }

    /**
     * Serialize a beam for client rendering
     */
    private Map<String, Object> serializeBeam(Beam beam) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", beam.getId());
        data.put("startX", beam.getStartPosition().x);
        data.put("startY", beam.getStartPosition().y);
        data.put("endX", beam.getEndPosition().x);
        data.put("endY", beam.getEndPosition().y);
        data.put("width", beam.getWidth());
        data.put("beamType", beam.getBeamType().name());
        data.put("duration", beam.getDuration());
        data.put("elapsed", System.currentTimeMillis() - beam.getCreated());
        return data;
    }

    /**
     * Create electric field effect at beam impact point if beam has ELECTRIC bullet effect
     */
    private void createBeamElectricField(Beam beam) {
        // Create electric field if beam has ELECTRIC bullet effect (area denial)
        if (beam.getBulletEffects().contains(BulletEffect.ELECTRIC)) {
            Vector2 hitPosition = beam.getEndPosition(); // Where the beam ended (hit or max range)
            double electricDamage = beam.getDamage() * 0.3; // 30% of beam damage per second
            double electricRadius = 40.0; // Fixed radius for beam electric fields

            int effectId = IdGenerator.nextEntityId();
            FieldEffect electricField = new FieldEffect(
                    beam.getOwnerId(),
                    FieldEffectType.ELECTRIC,
                    hitPosition,
                    electricRadius,
                    electricDamage,
                    FieldEffectType.ELECTRIC.getDefaultDuration(),
                    beam.getOwnerTeam()
            );

            fieldEffects.put(effectId, electricField);
            world.addBody(electricField.getBody());
            log.debug("Created electric field from beam at ({}, {}) with radius {} and {} DPS",
                    hitPosition.x, hitPosition.y, electricRadius, electricDamage);
        }
    }

    /**
     * Shutdown the game
     */
    public void shutdown() {
        shutdown.set(true);
        updateTask.cancel(true);
        log.info("RTS Game {} shut down", gameId);
    }

    /**
     * Get player count
     */
    public int getPlayerCount() {
        return playerSessions.size();
    }
}


