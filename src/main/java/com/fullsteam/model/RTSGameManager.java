package com.fullsteam.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullsteam.games.GameConstants;
import com.fullsteam.games.IdGenerator;
import com.fullsteam.model.command.AttackGroundCommand;
import com.fullsteam.model.command.AttackMoveCommand;
import com.fullsteam.model.command.AttackTargetableCommand;
import com.fullsteam.model.command.AutoHealCommand;
import com.fullsteam.model.command.AutoRepairCommand;
import com.fullsteam.model.command.ConstructCommand;
import com.fullsteam.model.command.GarrisonAPCCommand;
import com.fullsteam.model.command.GarrisonBunkerCommand;
import com.fullsteam.model.command.HarvestCommand;
import com.fullsteam.model.command.IdleCommand;
import com.fullsteam.model.command.MoveCommand;
import com.fullsteam.model.command.OnStationCommand;
import com.fullsteam.model.command.ReturnHomeCommand;
import com.fullsteam.model.command.ReturnToHangarCommand;
import com.fullsteam.model.command.SortieCommand;
import com.fullsteam.model.command.UnitCommand;
import com.fullsteam.model.component.APCComponent;
import com.fullsteam.model.component.AirfieldAircraftHousingComponent;
import com.fullsteam.model.component.AndroidComponent;
import com.fullsteam.model.component.AndroidFactoryComponent;
import com.fullsteam.model.component.GarrisonComponent;
import com.fullsteam.model.component.GunshipComponent;
import com.fullsteam.model.component.IBuildingComponent;
import com.fullsteam.model.component.InterceptorComponent;
import com.fullsteam.model.component.ProductionComponent;
import com.fullsteam.model.component.ShieldComponent;
import com.fullsteam.model.customization.CustomFactionConfig;
import com.fullsteam.model.factions.FactionDefinition;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
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
    /**
     * Wall-clock baseline for periodic army upkeep charges ({@link ArmyEconomy#UPKEEP_INTERVAL_MS}).
     */
    private long lastArmyRentWallClockMs = System.currentTimeMillis();

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
    @Getter
    private final Map<Integer, Player> players;
    private final Map<Integer, Unit> units;
    private final Map<Integer, Building> buildings;
    private final Map<Integer, Obstacle> obstacles;
    private final Map<Integer, Projectile> projectiles;
    private final Map<Integer, Beam> beams;
    private final Map<Integer, FieldEffect> fieldEffects;

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
    private final Set<Integer> disconnectedPlayers = new ConcurrentSkipListSet<>();

    public RTSGameManager(String gameId, GameConfig gameConfig, ObjectMapper objectMapper) {
        this.gameId = gameId;
        this.gameConfig = gameConfig;
        this.objectMapper = objectMapper;
        this.gameStartTime = System.currentTimeMillis();

        this.gameEntities = new GameEntities(gameConfig, this);
        this.players = gameEntities.getPlayerFactions();
        this.units = gameEntities.getUnits();
        this.buildings = gameEntities.getBuildings();
        this.obstacles = gameEntities.getObstacles();
        this.projectiles = gameEntities.getProjectiles();
        this.beams = gameEntities.getBeams();
        this.fieldEffects = gameEntities.getFieldEffects();

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
        this.gameEntities.setWorld(this.world);
        this.collisionProcessor = new RTSCollisionProcessor(gameEntities);
        this.world.addCollisionListener(this.collisionProcessor);
        this.world.setBounds(new AxisAlignedBounds(gameConfig.getWorldWidth(), gameConfig.getWorldHeight()));

        // Initialize world entities
        rtsWorld.placeObstacles();
        rtsWorld.createWorldBoundaries();
        rtsWorld.getObstacles().forEach(gameEntities::add);

        // Start update loop
        this.updateTask = GameConstants.EXECUTOR.scheduleAtFixedRate(this::update, 0, 20, TimeUnit.MILLISECONDS);
    }

    /**
     * Main update loop
     */
    public void update() {
        if (shutdown.get()) {
            return;
        }

        try {
            double currentTime = System.nanoTime() / 1e9;
            double deltaTime = currentTime - lastUpdateTime;
            lastUpdateTime = currentTime;
            frameCount++;

            // Recalculate army upkeep projection, population, and power every 60 frames (~1.2s)
            if (frameCount % 60 == 0) {
                recalculateFactionArmyEconomy();
                recalculatePower();
            }

            long nowWall = System.currentTimeMillis();
            if (nowWall - lastArmyRentWallClockMs >= ArmyEconomy.UPKEEP_INTERVAL_MS) {
                lastArmyRentWallClockMs = nowWall;
                processArmyRentCharges();
            }

            // Research system removed - units are now selected during faction customization
            // Unit availability is managed by FactionModifierManager

            // Process player inputs
            playerInputs.forEach(this::processPlayerInput);

            // Update all units and collect projectiles they fire
            units.values().forEach(unit -> {
                // Skip garrisoned units (they're inside bunkers)
                if (unit.isGarrisoned()) {
                    return;
                }

                // Update unit state
                unit.update(gameEntities);
                if (unit.getCurrentCommand() != null) {
                    UnitCommand cmd = unit.getCurrentCommand();
                    cmd.updateMovement(deltaTime);
                    cmd.updateTargetValidation();
                    cmd.updateCombat(deltaTime).forEach(gameEntities::add);
                }

                // AI behaviors: Issue AI commands when unit has no player orders
                issueAICommandsIfNeeded(unit);
            });

            // Update all buildings and collect projectiles from turrets
            buildings.values().forEach(building -> {
                // Check if owner has low power
                Player faction = players.get(building.getOwnerId());
                boolean hasLowPower = faction != null && faction.isHasLowPower();

                // Check if construction just completed
                boolean wasUnderConstruction = building.isUnderConstruction();

                building.update(gameEntities, gameEntities.getWorld().getTimeStep().getDeltaTime(), hasLowPower);

                // If construction just completed, send notification and handle post-construction logic
                if (wasUnderConstruction && !building.isUnderConstruction()) {
                    String buildingName = building.getBuildingType().name()
                            .replace("_", " ")
                            .toLowerCase();
                    // Capitalize first letter
                    buildingName = buildingName.substring(0, 1).toUpperCase() + buildingName.substring(1);

                    if (faction != null) {
                        sendGameEvent(GameEvent.createPlayerEvent(
                                "🏗️ " + buildingName + " construction complete",
                                faction.getPlayerId(),
                                GameEvent.EventCategory.INFO
                        ));
                    }
                }
            });

            // Update projectiles
            projectiles.entrySet().removeIf(entry -> {
                Projectile projectile = entry.getValue();
                projectile.update(gameEntities);

                // Enforce world boundaries for projectiles (especially air-to-air missiles)
                if (projectile.isActive()) {
                    projectile.clampToBounds(gameConfig.getWorldWidth(), gameConfig.getWorldHeight());
                }

                if (!projectile.isActive()) {
                    collisionProcessor.handleTerminalEffects(projectile);
                    world.removeBody(projectile.getBody());
                    return true;
                }
                return false;
            });

            // Update beams (they fade out over time, but don't remove yet)
            beams.values().forEach(beam -> beam.update(gameEntities));

            // Update physics world (handles collisions via CollisionListener)
            world.updatev(deltaTime);

            // Enforce world boundaries for all units (especially air units that can fly over obstacles)
            units.values().forEach(unit -> {
                if (unit.isActive() && !unit.isGarrisoned()) {
                    unit.clampToBounds(gameConfig.getWorldWidth(), gameConfig.getWorldHeight());
                }
            });

            // Process field effects (explosions, etc.)
            processFieldEffects(deltaTime);

            // Process tracker bugs (spy intelligence)
            processTrackerBugs();

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
        Player faction = players.get(playerId);
        if (faction == null) {
            return;
        }

        // Remove input immediately to prevent reprocessing on subsequent frames
        // (Important: do this BEFORE any early returns to avoid message spam)
        playerInputs.remove(playerId);

        // Handle unit selection
        if (input.getSelectUnits() != null) {
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
                    .filter(u -> !u.getUnitType().isSortieBased()) // Sortie-based units cannot be directly commanded
                    .forEach(u -> {
                        // Calculate path using A* pathfinding
                        List<Vector2> path = Pathfinding.findPath(
                                u.getPosition(),
                                destination,
                                obstacles.values(),
                                buildings.values(),
                                u.getUnitType().getSize(),
                                gameConfig.getWorldWidth(),
                                gameConfig.getWorldHeight(),
                                u.getUnitType().getElevation().isAirborne()
                        );

                        // Use command pattern
                        u.issueCommand(new MoveCommand(u, destination, true), gameEntities);

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
                    .filter(u -> !u.getUnitType().isSortieBased()) // Sortie-based units cannot be directly commanded
                    .forEach(u -> {
                        // Calculate path using A* pathfinding
                        List<Vector2> path = Pathfinding.findPath(
                                u.getPosition(),
                                destination,
                                obstacles.values(),
                                buildings.values(),
                                u.getUnitType().getSize(),
                                gameConfig.getWorldWidth(),
                                gameConfig.getWorldHeight(),
                                u.getUnitType().getElevation().isAirborne()
                        );
                        AttackMoveCommand cmd = new AttackMoveCommand(u, destination, true);
                        cmd.setPath(path);
                        u.issueCommand(cmd, gameEntities);
                    });
        }

        // Handle attack orders
        if (input.getAttackUnitOrder() != null) {
            Unit target = units.get(input.getAttackUnitOrder());
            if (target != null) {
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected())
                        .filter(u -> !u.getUnitType().isSortieBased()) // Sortie-based units cannot be directly commanded
                        .filter(u -> u.canTargetElevation(target)) // Check if weapon can hit target's elevation
                        .forEach(u -> u.issueCommand(new AttackTargetableCommand(u, target, true), gameEntities));
            }
        }

        if (input.getAttackBuildingOrder() != null) {
            Building target = buildings.get(input.getAttackBuildingOrder());
            if (target != null) {
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected())
                        .filter(u -> !u.getUnitType().isSortieBased()) // Sortie-based units cannot be directly commanded
                        .filter(Unit::canTargetBuildings) // Check if weapon can hit GROUND elevation (buildings)
                        .forEach(u -> u.issueCommand(new AttackTargetableCommand(u, target, true), gameEntities));
            }
        }

        // Handle force attack orders (attack ground - CMD/CTRL + right click)
        if (input.getForceAttackOrder() != null) {
            Vector2 targetPosition = input.getForceAttackOrder();
            units.values().stream()
                    .filter(u -> u.belongsTo(playerId) && u.isSelected() && u.getUnitType().canAttack())
                    .filter(u -> !u.getUnitType().isSortieBased()) // Sortie-based units cannot be directly commanded
                    .forEach(u -> {
                        // Issue force attack order first (sets all the flags correctly)
                        u.issueCommand(new AttackGroundCommand(u, targetPosition, true), gameEntities);

                        // Then calculate and set the path
                        List<Vector2> path = Pathfinding.findPath(
                                u.getPosition(),
                                targetPosition,
                                obstacles.values(),
                                buildings.values(),
                                u.getUnitType().getSize(),
                                gameConfig.getWorldWidth(),
                                gameConfig.getWorldHeight(),
                                u.getUnitType().getElevation().isAirborne()
                        );
                        u.setPath(path);
                    });
            log.info("Player {} issued force attack order to position ({}, {})",
                    playerId, targetPosition.x, targetPosition.y);
        }

        // Handle harvest orders - now targets harvestable obstacles
        if (input.getHarvestOrder() != null) {
            Obstacle obstacle = obstacles.get(input.getHarvestOrder());
            if (obstacle != null && obstacle.isHarvestable()) {
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected() && u.getUnitType().canHarvest())
                        .forEach(u -> u.issueCommand(new HarvestCommand(u, obstacle, true), gameEntities));
                log.info("Player {} ordered workers to harvest obstacle {}", playerId, obstacle.getId());
            }
        }

        // Mine orders removed - workers now harvest resources from obstacles

        // Handle construct orders (resume building construction)
        if (input.getConstructOrder() != null) {
            Building building = buildings.get(input.getConstructOrder());
            if (building != null && building.isUnderConstruction() && building.belongsTo(playerId)) {
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected() && u.getUnitType().canBuild())
                        .forEach(u -> u.issueCommand(new ConstructCommand(u, building, true), gameEntities));
            }
        }

        // Handle AI stance changes
        if (input.getSetStance() != null) {
            units.values().stream()
                    .filter(u -> u.belongsTo(playerId) && u.isSelected())
                    .filter(u -> !u.getUnitType().isSortieBased()) // Sortie-based units cannot be directly commanded
                    .forEach(u -> u.setAiStance(input.getSetStance()));
        }

        // Handle scatter command
        if (input.isScatterCommand()) {
            List<Unit> selectedUnits = units.values().stream()
                    .filter(u -> u.belongsTo(playerId) && u.isSelected())
                    .filter(u -> !u.getUnitType().isSortieBased()) // Sortie-based units cannot be directly commanded
                    .toList();

            if (!selectedUnits.isEmpty()) {
                // Calculate average center position of selected units
                Vector2 center = selectedUnits.stream()
                        .map(Unit::getPosition)
                        .reduce(selectedUnits.get(0).getPosition().copy(), (a, b) -> a.sum(b).divide(2.0));

                selectedUnits.forEach(u -> {
                    Vector2 currentPos = u.getPosition().copy();
                    Vector2 directionFromCenter = currentPos.copy().subtract(center);

                    // If unit is at the center, give it a random direction
                    if (directionFromCenter.getMagnitude() < 1.0) {
                        double randomAngle = Math.random() * 2 * Math.PI;
                        directionFromCenter = new Vector2(Math.cos(randomAngle), Math.sin(randomAngle));
                    }

                    // Normalize and scale to scatter distance
                    directionFromCenter.normalize();
                    directionFromCenter.multiply(ThreadLocalRandom.current().nextDouble(31, 67));

                    Vector2 scatterDestination = currentPos.copy().add(directionFromCenter);
                    log.info("Scatter command:to ({}, {})", scatterDestination.x, scatterDestination.y);
                    List<Vector2> path = Pathfinding.findPath(
                            u.getPosition(),
                            scatterDestination,
                            obstacles.values(),
                            buildings.values(),
                            u.getUnitType().getSize(),
                            gameConfig.getWorldWidth(),
                            gameConfig.getWorldHeight(),
                            u.getUnitType().getElevation().isAirborne()
                    );
                    u.issueCommand(new MoveCommand(u, scatterDestination, true), gameEntities);
                    u.getCurrentCommand().setPath(path);
                });
            }
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
                            .filter(u -> !u.getUnitType().isSortieBased()) // Sortie-based units cannot be directly commanded
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
                            .filter(u -> !u.getUnitType().isSortieBased()) // Sortie-based units cannot be directly commanded
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
                        .filter(u -> !u.getUnitType().isSortieBased()) // Sortie-based units cannot be directly commanded
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
            // Check if it's a building (bunker) first
            Building bunker = buildings.get(input.getGarrisonOrder());
            if (bunker != null && bunker.getBuildingType() == BuildingType.BUNKER &&
                    bunker.belongsTo(playerId) && !bunker.isUnderConstruction()) {
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected() && u.getUnitType().isInfantry())
                        .forEach(u -> u.issueCommand(new GarrisonBunkerCommand(u, bunker, true), gameEntities));
            } else {
                // Check if it's an APC
                Unit apc = units.get(input.getGarrisonOrder());
                if (apc != null && apc.getUnitType() == UnitType.APC &&
                        apc.belongsTo(playerId) && apc.isActive()) {
                    units.values().stream()
                            .filter(u -> u.belongsTo(playerId) && u.isSelected() && u.getUnitType().isInfantry())
                            .forEach(u -> u.issueCommand(new GarrisonAPCCommand(u, apc, true), gameEntities));
                }
            }
        }

        // Handle ungarrison orders
        if (input.getUngarrisonBuildingId() != null) {
            // Check if it's a building (bunker) first
            Building bunker = buildings.get(input.getUngarrisonBuildingId());
            if (bunker != null && bunker.getBuildingType() == BuildingType.BUNKER && bunker.belongsTo(playerId)) {
                if (input.isUngarrisonAll()) {
                    // Ungarrison all units
                    // Re-add units to the physics world
                    for (Unit unit : bunker.ungarrisonAllUnits()) {
                        if (!world.containsBody(unit.getBody())) {
                            world.addBody(unit.getBody());
                        }
                    }
                } else if (input.getUngarrisonUnitId() != null) {
                    Unit target = units.get(input.getUngarrisonUnitId());
                    Unit ungarrisoned = bunker.ungarrisonUnit(target);
                    if (ungarrisoned != null && !world.containsBody(ungarrisoned.getBody())) {
                        world.addBody(ungarrisoned.getBody());
                    }
                } else {
                    // Ungarrison one unit (FIFO)
                    Unit ungarrisoned = bunker.ungarrisonUnit(null);
                    if (ungarrisoned != null && !world.containsBody(ungarrisoned.getBody())) {
                        world.addBody(ungarrisoned.getBody());
                    }
                }
            } else {
                // Check if it's an APC (reusing the same field for unit garrison)
                Unit apc = units.get(input.getUngarrisonBuildingId());
                if (apc != null && apc.getUnitType() == UnitType.APC &&
                        apc.belongsTo(playerId) && apc.isActive()) {
                    if (input.isUngarrisonAll()) {
                        // Ungarrison all units
                        List<Unit> ungarrisoned = apc.ungarrisonAllUnits();
                        // Re-add units to the physics world
                        for (Unit unit : ungarrisoned) {
                            if (!world.containsBody(unit.getBody())) {
                                world.addBody(unit.getBody());
                            }
                        }
                    } else if (input.getUngarrisonUnitId() != null) {
                        Unit target = units.get(input.getUngarrisonUnitId());
                        Unit ungarrisoned = apc.ungarrisonUnit(target);
                        if (ungarrisoned != null && !world.containsBody(ungarrisoned.getBody())) {
                            world.addBody(ungarrisoned.getBody());
                        }
                    } else {
                        // Ungarrison one unit
                        Unit ungarrisoned = apc.ungarrisonUnit(null);
                        if (ungarrisoned != null && !world.containsBody(ungarrisoned.getBody())) {
                            world.addBody(ungarrisoned.getBody());
                        }
                    }
                }
            }
        }

        if (input.getCancelAirfieldProductionBuildingId() != null) {
            Building b = buildings.get(input.getCancelAirfieldProductionBuildingId());
            if (b != null && b.belongsTo(playerId) && !b.isUnderConstruction()
                    && b.getBuildingType().isCanProduceUnits()) {
                UnitType cancelled = b.cancelCurrentProduction();
                if (cancelled != null) {
                    int refund = faction.getUnitCost(cancelled);
                    faction.addResources(ResourceType.CREDITS, refund);
                }
            }
        }

        if (input.getScrapFromBuildingId() != null && input.getScrapHousedUnitId() != null) {
            Building af = buildings.get(input.getScrapFromBuildingId());
            if (af != null && af.getBuildingType() == BuildingType.AIRFIELD
                    && af.belongsTo(playerId) && !af.isUnderConstruction()) {
                af.getComponent(AirfieldAircraftHousingComponent.class).ifPresent(housing -> {
                    if (housing.scrapHousedAircraft(input.getScrapHousedUnitId())) {
                        sendGameEvent(GameEvent.createPlayerEvent(
                                "Scrapped unit from airfield",
                                playerId,
                                GameEvent.EventCategory.INFO
                        ));
                    }
                });
            }
        }

        // Handle sortie / deploy (housed aircraft at airfield)
        if (input.getSortieBuildingId() != null && input.getSortieHousedUnitId() != null && input.getSortieTargetLocation() != null) {
            Building airfield = buildings.get(input.getSortieBuildingId());
            if (airfield != null && airfield.getBuildingType() == BuildingType.AIRFIELD &&
                    airfield.belongsTo(playerId) && !airfield.isUnderConstruction()) {

                AirfieldAircraftHousingComponent housing = airfield.getComponent(AirfieldAircraftHousingComponent.class).orElse(null);
                int housedUnitId = input.getSortieHousedUnitId();
                if (housing != null && housing.isReadyForSortie(housedUnitId)) {
                    Unit aircraft = housing.launchAircraft(housedUnitId);
                    if (aircraft != null) {
                        UnitType aircraftType = aircraft.getUnitType();
                        int baseId = airfield.getId();
                        if (aircraftType == UnitType.BOMBER) {
                            aircraft.setActive(true);
                            aircraft.issueCommand(new SortieCommand(aircraft, input.getSortieTargetLocation(), baseId, true), gameEntities);
                            sendGameEvent(GameEvent.createPlayerEvent(
                                    "✈️ Bomber launched on sortie",
                                    playerId,
                                    GameEvent.EventCategory.INFO
                            ));
                        } else if (aircraftType == UnitType.INTERCEPTOR) {
                            aircraft.getComponent(InterceptorComponent.class)
                                    .ifPresent(interceptorComp -> interceptorComp.deploy(baseId));
                            aircraft.issueCommand(new OnStationCommand(aircraft, input.getSortieTargetLocation(), true), gameEntities);
                            sendGameEvent(GameEvent.createPlayerEvent(
                                    "🛩️ Interceptor deployed on station",
                                    playerId,
                                    GameEvent.EventCategory.INFO
                            ));
                        } else if (aircraftType == UnitType.GUNSHIP) {
                            aircraft.getComponent(GunshipComponent.class)
                                    .ifPresent(gunshipComp -> gunshipComp.deploy(baseId));
                            aircraft.issueCommand(new OnStationCommand(aircraft, input.getSortieTargetLocation(), true), gameEntities);
                            sendGameEvent(GameEvent.createPlayerEvent(
                                    "🚁 Gunship deployed on station",
                                    playerId,
                                    GameEvent.EventCategory.INFO
                            ));
                        } else {
                            aircraft.issueCommand(new SortieCommand(aircraft, input.getSortieTargetLocation(), baseId, true), gameEntities);
                            sendGameEvent(GameEvent.createPlayerEvent(
                                    "✈️ Aircraft launched on sortie",
                                    playerId,
                                    GameEvent.EventCategory.INFO
                            ));
                        }
                    }
                } else if (housing != null) {
                    sendGameEvent(GameEvent.createPlayerEvent(
                            "⚠️ Cannot launch: aircraft not ready, wrong berth, or already deployed",
                            playerId,
                            GameEvent.EventCategory.WARNING
                    ));
                }
            }
        }

        // Return To Base — recall a deployed housed aircraft
        if (input.getRtbBuildingId() != null && input.getRtbHousedUnitId() != null) {
            Building airfield = buildings.get(input.getRtbBuildingId());
            if (airfield != null && airfield.getBuildingType() == BuildingType.AIRFIELD &&
                    airfield.belongsTo(playerId) && !airfield.isUnderConstruction()) {

                AirfieldAircraftHousingComponent housing = airfield.getComponent(AirfieldAircraftHousingComponent.class).orElse(null);
                int uid = input.getRtbHousedUnitId();
                if (housing != null && housing.isDeployed(uid)) {
                    Unit aircraft = units.get(uid);
                    if (aircraft != null) {
                        aircraft.issueCommand(new ReturnToHangarCommand(aircraft, airfield.getId(), true), gameEntities);
                        log.info("Player {} recalled aircraft {} to airfield {}",
                                playerId, aircraft.getId(), airfield.getId());
                        sendGameEvent(GameEvent.createPlayerEvent(
                                "✈️ Aircraft returning to base",
                                playerId,
                                GameEvent.EventCategory.INFO
                        ));
                    } else {
                        log.warn("Player {} RTB for unit {} not in world", playerId, uid);
                    }
                } else {
                    log.warn("Player {} RTB for airfield {} unit {} — not deployed from this field",
                            playerId, airfield.getId(), uid);
                }
            }
        }

        // Handle build orders
        if (input.getBuildOrder() != null) {
            BuildingType buildingType = input.getBuildOrder();
            Vector2 location = input.getBuildLocation();

            // Validate tech requirements first
            Set<BuildingType> missingTech = missingTechRequirements(playerId, buildingType);
            if (!missingTech.isEmpty()) {
                log.warn("Player {} attempted to build {} without meeting tech requirements",
                        playerId, buildingType);
                sendGameEvent(GameEvent.createPlayerEvent(
                        "Cannot build " + buildingType.getDisplayName() + " - missing required tech buildings: " + missingTech.stream().map(BuildingType::getDisplayName).collect(Collectors.toSet()),
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

                // Check support capacity (e.g., Hangars require nearby Airfield with capacity)
                if (!collisionProcessor.hasSupportCapacity(location, buildingType, playerId)) {
                    BuildingType requirement = buildingType.getProximityRequirement();
                    int capacity = requirement != null ? requirement.getSupportCapacity() : 0;
                    log.warn("Player {} tried to build {} but no {} with available capacity nearby",
                            playerId, buildingType, requirement);
                    sendGameEvent(GameEvent.createPlayerEvent(
                            String.format("⚠️ Cannot build %s here - need nearby %s with available capacity (max %d per %s)",
                                    buildingType.getDisplayName(),
                                    requirement != null ? requirement.getDisplayName() : "support building",
                                    capacity,
                                    requirement != null ? requirement.getDisplayName() : "building"),
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
                        faction,  // Pass faction reference for dynamic modifiers
                        maxHealth
                );
                buildings.put(building.getId(), building);
                world.addBody(building.getBody());

                // Trigger perk hooks for building creation
                faction.getFactionDefinition().onBuildingCreated(building, faction, this);

                // Order selected workers to construct it
                units.values().stream()
                        .filter(u -> u.belongsTo(playerId) && u.isSelected() && u.getUnitType().canBuild())
                        .forEach(u -> u.issueCommand(new ConstructCommand(u, building, true), gameEntities));

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
                    // Check if this unit is unlocked via the tech tree (custom faction selection)
                    if (!faction.canProduceUnit(unitType)) {
                        log.warn("Player {} tried to produce {} but it's not unlocked in custom faction",
                                playerId, unitType);
                        return;
                    }

                    // Check if player has required tech buildings
                    Set<BuildingType> playerBuildings = getPlayerBuildingTypes(playerId);
                    if (!faction.hasRequiredTechBuildings(unitType, playerBuildings)) {
                        Set<BuildingType> required = unitType.getRequiredBuildings();
                        Set<BuildingType> missing = new HashSet<>(required);
                        missing.removeAll(playerBuildings);
                        log.warn("Player {} tried to produce {} but missing required buildings: {}",
                                playerId, unitType, missing);
                        sendGameEvent(GameEvent.createPlayerEvent(
                                String.format("🔬 Tech Required! %s needs: %s",
                                        unitType.getDisplayName(),
                                        missing.stream()
                                                .map(BuildingType::getDisplayName)
                                                .collect(Collectors.joining(", "))),
                                playerId,
                                GameEvent.EventCategory.WARNING
                        ));
                        return;
                    }

                    // Check if this building type can produce this unit type
                    if (unitType.getProducedBy() != building.getBuildingType()) {
                        log.warn("Player {} tried to produce {} at {} but that building can't produce it (requires {})",
                                playerId, unitType, building.getBuildingType(), unitType.getProducedBy());
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

                    if (!building.queueUnitProduction(unitType)) {
                        faction.addResources(ResourceType.CREDITS, cost);
                        sendGameEvent(GameEvent.createPlayerEvent(
                                "⚠️ Could not queue unit production (invalid type or queue rules).",
                                playerId,
                                GameEvent.EventCategory.WARNING
                        ));
                    } else {
                        log.info("Player {} queued {} production at building {} (cost: {})",
                                playerId, unitType, buildingId, cost);
                    }
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

        // Research system removed - building research handlers deleted
        // Units and modifiers are now configured during faction customization
    }

    /**
     * Issue AI commands to units that have no player orders.
     * This includes auto-heal, auto-repair, and return-to-home behaviors.
     */
    private void issueAICommandsIfNeeded(Unit unit) {
        // Don't interrupt player orders
        if (unit.getCurrentCommand() != null && unit.getCurrentCommand().isPlayerOrder()) {
            return;
        }

        // Also check legacy isMoving flag (used by GarrisonComponent)
        if (unit.isMoving()) {
            return;
        }

        // Check if unit is idle (no command or idle command)
        boolean isIdle = (unit.getCurrentCommand() == null || unit.getCurrentCommand() instanceof IdleCommand);

        // Priority 1: Medics auto-heal damaged friendly units
        if (unit.getUnitType().canHeal() && isIdle) {
            // Issue auto-heal command
            unit.issueCommand(new AutoHealCommand(unit), gameEntities);
            return;
        }

        // Priority 2: Engineers auto-repair damaged friendly units/buildings
        if (unit.getUnitType().canRepair() && isIdle) {
            // Issue auto-repair command
            unit.issueCommand(new AutoRepairCommand(unit), gameEntities);
            return;
        }

        // Priority 3: Return to home position if in defensive stance and far from home
        if (unit.shouldReturnHome()) {
            unit.issueCommand(new ReturnHomeCommand(unit, unit.getHomePosition()), gameEntities);
            return;
        }
    }

    /**
     * Find the nearest refinery for a worker unit
     *
     * @deprecated This method is no longer used by RTSGameManager - HarvestCommand now handles refinery finding internally
     */
    @Deprecated
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
     * Process tracker bugs (spy intelligence devices)
     * Update state and remove expired/invalid bugs
     */
    private void processTrackerBugs() {
        gameEntities.getTrackerBugs().entrySet().removeIf(entry -> {
            TrackerBug bug = entry.getValue();

            // Update bug (checks expiration)
            if (!bug.update()) {
                return true; // Remove expired bugs
            }

            // Remove if target is dead or gone
            if (!bug.isTargetAlive(gameEntities)) {
                log.info("Tracker bug {} removed - tagged entity no longer valid", bug.getId());
                return true;
            }

            return false;
        });
    }

    /**
     * Check for disconnected players and mark them as eliminated
     */
    private void checkDisconnectedPlayers() {
        for (Player faction : players.values()) {
            if (faction.getPlayerId() < 0) {
                continue;
            }
            WebSocketSession ws = faction.getWebSocketSession();
            if (ws == null) {
                continue;
            }
            if (ws.isOpen()) {
                continue;
            }

            int playerId = faction.getPlayerId();
            if (!disconnectedPlayers.contains(playerId)) {
                disconnectedPlayers.add(playerId);
                log.info("Player {} disconnected from game {}", playerId, gameId);
                sendGameEvent(GameEvent.builder()
                        .message(String.format("⚠️ %s has disconnected from the game", faction.getPlayerId()))
                        .category(GameEvent.EventCategory.SYSTEM)
                        .color("#FFA500")
                        .target(GameEvent.EventTarget.builder()
                                .type(GameEvent.EventTarget.TargetType.ALL)
                                .build())
                        .displayDuration(5000L)
                        .build()
                );
            }
            faction.setWebSocketSession(null);
        }
    }

    /**
     * Check win conditions (HQ destruction or last player standing)
     */
    private void checkWinConditions() {
        // Don't check win conditions for first 5 seconds (let players join)
        if (gameOver || System.currentTimeMillis() - gameStartTime < 5000) {
            return;
        }

        // Count active HQs per team
        Map<Integer, Boolean> teamHasHQ = new LinkedHashMap<>();

        for (Building building : buildings.values()) {
            if (building.isActive() && building.getBuildingType() == BuildingType.HEADQUARTERS) {
                teamHasHQ.put(building.getTeamNumber(), true);
                log.debug("Found active HQ for team {}", building.getTeamNumber());
            }
        }

        log.debug("Win condition check: {} teams have HQs: {}", teamHasHQ.size(), teamHasHQ.keySet());

        // Count how many teams actually have ACTIVE (non-disconnected) players
        Set<Integer> teamsWithActivePlayers = new HashSet<>();
        for (Player faction : players.values()) {
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
            Map<String, Object> gameOverMsg = new LinkedHashMap<>();
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

            Map<String, Object> gameOverMsg = new LinkedHashMap<>();
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
            Map<String, Object> gameOverMsg = new LinkedHashMap<>();
            gameOverMsg.put("type", "gameOver");
            gameOverMsg.put("winningTeam", winningTeam);
            gameOverMsg.put("reason", "All enemy headquarters destroyed");
            broadcast(gameOverMsg);

        } else if (teamHasHQ.isEmpty()) {
            // All HQs destroyed - draw
            gameOver = true;
            winningTeam = -1;

            log.info("Game Over! Draw - all headquarters destroyed");

            Map<String, Object> gameOverMsg = new LinkedHashMap<>();
            gameOverMsg.put("type", "gameOver");
            gameOverMsg.put("winningTeam", -1);
            gameOverMsg.put("reason", "Draw - all headquarters destroyed");
            broadcast(gameOverMsg);
        }
    }

    /**
     * Recalculate population and projected periodic army upkeep (credits per interval) for every faction.
     * Upkeep is a fraction of each unit's build cost for the owning faction, including garrisoned and
     * airfield-berth aircraft; multiplied by faction perks and Command Citadel discounts.
     */
    private void recalculateFactionArmyEconomy() {
        for (Player f : players.values()) {
            f.setCurrentUpkeep(computeArmyRentCharge(f));
        }
    }

    private int countCompletedCommandCitadels(int playerId) {
        return (int) buildings.values().stream()
                .filter(b -> b.getOwnerId() == playerId && b.isActive() && !b.isUnderConstruction())
                .filter(b -> b.getBuildingType() == BuildingType.COMMAND_CITADEL)
                .count();
    }

    private double armyRentGlobalMultiplier(Player faction) {
        return faction.getFactionDefinition().getArmyRentCostMultiplier()
                * ArmyEconomy.commandCitadelRentMultiplier(countCompletedCommandCitadels(faction.getPlayerId()));
    }

    private int computeArmyRentCharge(Player faction) {
        int playerId = faction.getPlayerId();
        int raw = 0;
        for (Unit u : units.values()) {
            if (!u.isActive() || u.getOwnerId() != playerId) {
                continue;
            }
            raw += ArmyEconomy.periodicRentForUnit(faction, u.getUnitType());
        }
        for (Building b : buildings.values()) {
            if (!b.isActive() || b.isUnderConstruction() || b.getOwnerId() != playerId) {
                continue;
            }
            Optional<AirfieldAircraftHousingComponent> housing = b.getComponent(AirfieldAircraftHousingComponent.class);
            if (housing.isEmpty()) {
                continue;
            }
            for (AirfieldAircraftHousingComponent.Berth berth : housing.get().getBerthsView()) {
                if (berth.getHousedUnit() != null && !berth.isDeployed()) {
                    raw += ArmyEconomy.periodicRentForUnit(faction, berth.getHousedUnit().getUnitType());
                }
            }
        }
        if (raw <= 0) {
            return 0;
        }
        return Math.max(0, (int) Math.round(raw * armyRentGlobalMultiplier(faction)));
    }

    /**
     * Charge army upkeep or desert one unit when the player cannot pay.
     */
    private void processArmyRentCharges() {
        recalculateFactionArmyEconomy();
        for (Player faction : players.values()) {
            int due = faction.getCurrentUpkeep();
            if (due <= 0) {
                continue;
            }
            if (faction.hasResources(ResourceType.CREDITS, due)) {
                faction.removeResources(ResourceType.CREDITS, due);
                log.debug("Player {} paid {} credits army upkeep", faction.getPlayerId(), due);
                sendGameEvent(GameEvent.createPlayerEvent(
                        String.format("💸 Army upkeep (%d credits) paid", due),
                        faction.getPlayerId(),
                        GameEvent.EventCategory.INFO
                ));
            } else {
                log.warn("Player {} could not pay army upkeep ({} due, {} credits) — deserting one unit",
                        faction.getPlayerId(), due, faction.getResourceAmount(ResourceType.CREDITS));
                // zero out resources, player should be punished for over-extending
                faction.removeResources(ResourceType.CREDITS, faction.getResourceAmount(ResourceType.CREDITS));
                desertHighestRentUnit(faction);
                sendGameEvent(GameEvent.createPlayerEvent(
                        String.format("💸 Army upkeep (%d credits) unpaid — your most expensive unit deserted!", due),
                        faction.getPlayerId(),
                        GameEvent.EventCategory.WARNING
                ));
            }
        }
    }

    private void detachUnitFromContainmentForDesertion(Unit unit) {
        for (Building b : buildings.values()) {
            b.getComponent(GarrisonComponent.class).ifPresent(gc -> gc.removeGarrisonedUnitForDesertion(unit));
        }
        for (Unit carrier : units.values()) {
            carrier.getComponent(APCComponent.class).ifPresent(apc -> apc.removeGarrisonedUnitForDesertion(unit));
        }
    }

    /**
     * Removes the single active unit with highest per-tick upkeep slice (ties: higher unit id).
     * Includes aircraft parked in airfield berths (not deployed).
     */
    private void desertHighestRentUnit(Player faction) {
        int playerId = faction.getPlayerId();
        Unit best = null;
        int bestRent = -1;

        for (Unit u : units.values()) {
            if (!u.isActive() || u.getOwnerId() != playerId) {
                continue;
            }
            int r = ArmyEconomy.periodicRentForUnit(faction, u.getUnitType());
            if (r > bestRent || (r == bestRent && (best == null || u.getId() > best.getId()))) {
                bestRent = r;
                best = u;
            }
        }

        for (Building b : buildings.values()) {
            if (!b.isActive() || b.getOwnerId() != playerId) {
                continue;
            }
            Optional<AirfieldAircraftHousingComponent> housing = b.getComponent(AirfieldAircraftHousingComponent.class);
            if (housing.isEmpty()) {
                continue;
            }
            for (AirfieldAircraftHousingComponent.Berth berth : housing.get().getBerthsView()) {
                if (berth.getHousedUnit() == null || berth.isDeployed()) {
                    continue;
                }
                Unit u = berth.getHousedUnit();
                int r = ArmyEconomy.periodicRentForUnit(faction, u.getUnitType());
                if (r > bestRent || (r == bestRent && (best == null || u.getId() > best.getId()))) {
                    bestRent = r;
                    best = u;
                }
            }
        }

        if (best == null || bestRent <= 0) {
            return;
        }

        detachUnitFromContainmentForDesertion(best);

        final int desertUnitId = best.getId();
        if (units.containsKey(desertUnitId)) {
            best.setActive(false);
        } else {
            for (Building b : buildings.values()) {
                if (!b.isActive()) {
                    continue;
                }
                if (b.getComponent(AirfieldAircraftHousingComponent.class)
                        .map(h -> h.scrapHousedAircraft(desertUnitId))
                        .orElse(false)) {
                    break;
                }
            }
        }
    }

    /**
     * Recalculate power generation and consumption for all factions
     * Low power stops all unit production
     */
    private void recalculatePower() {
        players.values().forEach(faction -> {
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

    // Research system removed - updateResearch() method deleted
    // Units are now selected during faction customization, not unlocked through research

    // Research system removed - updateUnitResearch() method deleted
    // Units are now selected during faction customization, not unlocked through research

    // Research system removed - handleUnitResearchComplete() method deleted
    // Units are now selected during faction customization, not unlocked through research

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
                unit.getComponent(AndroidComponent.class)
                        .filter(ac -> ac.getAndroidFactoryId() != null)
                        .ifPresent(androidComp -> {
                            Building factory = buildings.get(androidComp.getAndroidFactoryId());
                            if (factory != null && factory.isActive()) {
                                factory.getComponent(AndroidFactoryComponent.class)
                                        .ifPresent(a -> a.unregisterAndroid(unit.getId()));
                            }
                        });

                // Unregister sortie aircraft from airfield housing (if sortie-based)
                if (unit.getUnitType().isSortieBased()) {
                    Integer baseBuildingId = null;

                    Optional<InterceptorComponent> interceptorComp = unit.getComponent(InterceptorComponent.class);
                    if (interceptorComp.isPresent() && interceptorComp.get().getHomeBaseBuildingId() != null) {
                        baseBuildingId = interceptorComp.get().getHomeBaseBuildingId();
                    }

                    if (baseBuildingId == null) {
                        Optional<GunshipComponent> gunshipComp = unit.getComponent(GunshipComponent.class);
                        if (gunshipComp.isPresent() && gunshipComp.get().getHomeBaseBuildingId() != null) {
                            baseBuildingId = gunshipComp.get().getHomeBaseBuildingId();
                        }
                    }

                    if (baseBuildingId == null && unit.getCurrentCommand() instanceof SortieCommand sortieCmd) {
                        baseBuildingId = sortieCmd.getHomeBaseBuildingId();
                    }

                    if (baseBuildingId != null) {
                        final int finalBaseId = baseBuildingId;
                        Building airfield = buildings.get(finalBaseId);
                        if (airfield != null && airfield.isActive()) {
                            airfield.getComponent(AirfieldAircraftHousingComponent.class)
                                    .ifPresent(hc -> {
                                        log.info("Sortie aircraft {} destroyed — freeing berth at airfield {}",
                                                unit.getId(), finalBaseId);
                                        hc.clearBerthForUnit(unit.getId());
                                    });
                        }
                    }
                }

                // Destroy garrisoned units in APC (if it's an APC)
                unit.getComponent(APCComponent.class).ifPresent(apcComp -> {
                    log.info("APC {} destroyed - destroying {} garrisoned units",
                            unit.getId(), apcComp.getGarrisonCount());
                    apcComp.onDestroy(); // This will destroy all garrisoned units
                });

                // Trigger perk hooks for unit destruction
                int ownerId = unit.getOwnerId();
                Player faction = players.get(ownerId);
                faction.getFactionDefinition().onUnitDestroyed(unit, faction, this);

                // Send unit death notification (throttled to avoid spam)
                long currentTime = System.currentTimeMillis();
                Long lastNotification = lastUnitDeathNotification.get(ownerId);

                if (lastNotification == null || (currentTime - lastNotification) >= UNIT_DEATH_NOTIFICATION_COOLDOWN) {
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
                Player faction = players.get(building.getOwnerId());
                if (faction != null && !building.isUnderConstruction()) {
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

                // Trigger perk hooks for building destruction
                Player ownerFaction = players.get(building.getOwnerId());
                ownerFaction.getFactionDefinition().onBuildingDestroyed(building, ownerFaction, this);

                // Call onDestroy for all components (handles sandstorm cleanup, etc.)
                for (IBuildingComponent component : building.getComponents().values()) {
                    component.onDestroy();
                }

                world.removeBody(building.getBody());
                return true;
            }
            return false;
        });

        // Resource deposits removed - obstacles now contain harvestable resources

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
    private boolean canAffordBuilding(Player faction, BuildingType buildingType) {
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
    private boolean canAffordUnit(Player faction, UnitType unitType) {
        // Check if unit is unlocked via research (uses new tech tree system)
        if (!faction.canProduceUnit(unitType)) {
            return false;
        }

        // Use faction-modified cost
        int cost = faction.getUnitCost(unitType);
        return faction.hasResources(ResourceType.CREDITS, cost);
    }

    /**
     * Check if player has the required tech buildings to construct this building
     */
    private Set<BuildingType> missingTechRequirements(int playerId, BuildingType buildingType) {
        Set<BuildingType> playerBuildings = buildings.values().stream()
                .filter(b -> b.getOwnerId() == playerId && b.isActive() && !b.isUnderConstruction())
                .map(Building::getBuildingType)
                .collect(Collectors.toSet());
        HashSet<BuildingType> techRequired = new HashSet<>(buildingType.getTechRequirements());
        techRequired.removeAll(playerBuildings);
        return techRequired;
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
                gameConfig.getWorldWidth(),
                gameConfig.getWorldHeight()
        );
    }

    /**
     * Send game state to all players (with fog of war)
     */
    private void sendGameState() {
        // Send personalized game state to each player based on their vision
        players.values().forEach(faction -> {
            WebSocketSession ws = faction.getWebSocketSession();
            if (ws == null || !ws.isOpen() || faction.getPlayerId() < 0) {
                return;
            }
            Map<String, Object> gameState = createGameStateForTeam(faction.getTeamNumber());
            send(ws, gameState);
        });
    }

    /**
     * Create game state for a specific team (with fog of war applied)
     * Note: Static data (obstacles, biome, world dimensions, unit/building types)
     * is now sent once via gameInitialization message
     */
    private Map<String, Object> createGameStateForTeam(int teamNumber) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("type", "gameState");
        state.put("timestamp", System.currentTimeMillis());

        // Apply fog of war - only send visible units and buildings
        List<Unit> visibleUnits = FogOfWar.getVisibleUnits(gameEntities, teamNumber);
        List<Building> visibleBuildings = FogOfWar.getVisibleBuildings(gameEntities, teamNumber);

        // Serialize visible units (exclude garrisoned units)
        List<Map<String, Object>> unitsList = visibleUnits.stream()
                .filter(u -> !u.isGarrisoned()) // Don't render garrisoned units
                .map(u -> serializeUnit(u, teamNumber))
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

        // Obstacles: Only send dynamic data (health/resources) for changed obstacles
        // Client has full static data from initialization
        List<Map<String, Object>> obstacleUpdates = obstacles.values().stream()
                .filter(o -> o.isDestructible() || o.isHarvestable()) // Only obstacles that can change
                .filter(o -> o.getHealth() < o.getMaxHealth() ||
                        (o.isHarvestable() && o.getRemainingResources() < o.getMaxResources()))
                .map(this::serializeObstacleDynamic)
                .collect(Collectors.toList());
        if (!obstacleUpdates.isEmpty()) {
            state.put("obstacleUpdates", obstacleUpdates);
        }

        // Send list of active obstacle IDs so client can remove depleted ones
        List<Integer> activeObstacleIds = obstacles.values().stream()
                .map(Obstacle::getId)
                .collect(Collectors.toList());
        state.put("activeObstacleIds", activeObstacleIds);

        // Player factions - send only dynamic resource/state info
        Map<Integer, Map<String, Object>> factionsMap = new LinkedHashMap<>();
        players.forEach((playerId, faction) -> {
            if (faction.getTeamNumber() == teamNumber) {
                // Dynamic info for own team (resources, unit counts, etc.)
                factionsMap.put(playerId, serializeFactionDynamic(faction));
            } else {
                // Limited info for other teams (just team number and name)
                Map<String, Object> limitedInfo = new LinkedHashMap<>();
                limitedInfo.put("playerId", faction.getPlayerId());
                limitedInfo.put("team", faction.getTeamNumber());
                factionsMap.put(playerId, limitedInfo);
            }
        });
        state.put("factions", factionsMap);

        return state;
    }

    /**
     * Serialize only dynamic obstacle data (health, resources)
     * Client already has static data from initialization
     */
    private Map<String, Object> serializeObstacleDynamic(Obstacle obstacle) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", obstacle.getId());

        if (obstacle.isDestructible()) {
            data.put("health", obstacle.getHealth());
        }

        if (obstacle.isHarvestable()) {
            data.put("remainingResources", obstacle.getRemainingResources());
            data.put("resourcePercent", obstacle.getResourcePercent());
        }

        return data;
    }

    /**
     * Serialize only dynamic faction data (resources, counts, power, etc.)
     * Client already has static data from initialization
     */
    private Map<String, Object> serializeFactionDynamic(Player faction) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("playerId", faction.getPlayerId());
        data.put("team", faction.getTeamNumber());
        data.put("credits", faction.getResourceAmount(ResourceType.CREDITS));
        data.put("currentUpkeep", faction.getCurrentUpkeep());
        data.put("armyUpkeepIntervalMs", ArmyEconomy.UPKEEP_INTERVAL_MS);
        data.put("powerGenerated", faction.getPowerGenerated());
        data.put("powerConsumed", faction.getPowerConsumed());
        data.put("hasLowPower", faction.isHasLowPower());
        return data;
    }

    /**
     * Create game initialization message for a player
     * Contains static data that doesn't change during the game
     */
    public Map<String, Object> createGameInitializationForPlayer(int playerId) {
        Map<String, Object> init = new LinkedHashMap<>();
        init.put("type", "gameInitialization");
        init.put("timestamp", System.currentTimeMillis());

        // Biome info (never changes)
        Map<String, Object> biomeInfo = new LinkedHashMap<>();
        biomeInfo.put("name", rtsWorld.getBiome().name());
        biomeInfo.put("groundColor", rtsWorld.getBiome().getGroundColor());
        biomeInfo.put("obstacleColor", rtsWorld.getBiome().getObstacleColor());
        init.put("biome", biomeInfo);

        // World dimensions (never change)
        init.put("worldWidth", gameConfig.getWorldWidth());
        init.put("worldHeight", gameConfig.getWorldHeight());
        init.put("armyUpkeepIntervalMs", ArmyEconomy.UPKEEP_INTERVAL_MS);

        // Obstacles - full static data (position, shape, type)
        // Only health and resources will be updated in game state
        List<Map<String, Object>> obstaclesList = obstacles.values().stream()
                .map(this::serializeObstacleStatic)
                .collect(Collectors.toList());
        init.put("obstacles", obstaclesList);

        // Unit type metadata (static properties for all unit types)
        Map<String, Map<String, Object>> unitTypes = new LinkedHashMap<>();
        for (UnitType unitType : UnitType.sorted()) {
            Map<String, Object> typeData = new LinkedHashMap<>();
            typeData.put("displayName", unitType.getDisplayName());
            typeData.put("size", unitType.getSize());
            typeData.put("maxHealth", (int) unitType.getMaxHealth());
            typeData.put("damage", (int) unitType.getDamage());
            typeData.put("speed", unitType.getMovementSpeed());
            typeData.put("range", (int) unitType.getAttackRange());
            typeData.put("buildTimeSeconds", unitType.getBuildTimeSeconds());
            typeData.put("producedBy", unitType.getProducedBy().name());
            typeData.put("category", unitType.getCategory().name());
            int upkeepBase = ArmyEconomy.periodicUpkeepFromBuildCost(unitType, unitType.getResourceCost());
            typeData.put("periodicArmyRentBase", upkeepBase);
            typeData.put("upkeep", upkeepBase);
            typeData.put("visionRange", unitType.getVisionRange());
            typeData.put("specialAbility", unitType.getSpecialAbility().name());
            typeData.put("color", unitType.getColor());
            typeData.put("elevation", unitType.getElevation().name());
            unitTypes.put(unitType.name(), typeData);
        }
        init.put("unitTypes", unitTypes);

        // Building type metadata (static properties for all building types)
        Map<String, Map<String, Object>> buildingTypes = new LinkedHashMap<>();
        for (BuildingType buildingType : BuildingType.sorted()) {
            Map<String, Object> typeData = new LinkedHashMap<>();
            typeData.put("displayName", buildingType.getDisplayName());
            typeData.put("label", buildingType.getLabel());
            typeData.put("menuIcon", buildingType.getMenuIcon());
            typeData.put("size", buildingType.getSize());
            typeData.put("maxHealth", buildingType.getMaxHealth());
            typeData.put("powerValue", buildingType.getPowerValue());
            typeData.put("buildTimeSeconds", buildingType.getBuildTimeSeconds());
            typeData.put("canProduceUnits", buildingType.isCanProduceUnits());
            typeData.put("visionRange", buildingType.getVisionRange());
            typeData.put("requiredTechTier", buildingType.getRequiredTechTier());

            // Add weapon range for defensive buildings (for UI range indicators)
            double weaponRange = buildingType.getWeaponRange();
            if (weaponRange > 0) {
                typeData.put("weaponRange", weaponRange);
            }

            // Add aura radius for buildings with area effects (for UI range indicators)
            double auraRadius = buildingType.getAuraRadius();
            if (auraRadius > 0) {
                typeData.put("auraRadius", auraRadius);
            }

            buildingTypes.put(buildingType.name(), typeData);
        }
        init.put("buildingTypes", buildingTypes);

        // Player's faction static info (if they have a faction yet)
        Player faction = players.get(playerId);
        if (faction != null) {
            Map<String, Object> factionStatic = new LinkedHashMap<>();
            factionStatic.put("playerId", faction.getPlayerId());
            factionStatic.put("team", faction.getTeamNumber());

            // Available units and buildings
            List<String> availableUnits = faction.getFactionDefinition()
                    .getUnitTypes()
                    .stream()
                    .sorted(Comparator.comparing((UnitType u) -> u.getRequiredBuildings().size())
                            .thenComparing(UnitType::getResourceCost))
                    .map(UnitType::name)
                    .toList();
            factionStatic.put("availableUnits", availableUnits);

            List<String> availableBuildings = faction.getFactionDefinition()
                    .getBuildingTypes()
                    .stream()
                    .sorted(Comparator.comparing((BuildingType u) -> u.getTechRequirements().size())
                            .thenComparing(BuildingType::getResourceCost))
                    .map(BuildingType::name)
                    .toList();
            factionStatic.put("availableBuildings", availableBuildings);

            // Build detailed unit/building info for custom factions
            List<Map<String, Object>> buildingInfo = new ArrayList<>();
            for (String buildingName : availableBuildings) {
                BuildingType buildingType = BuildingType.valueOf(buildingName);
                Map<String, Object> building = new LinkedHashMap<>();
                building.put("buildingType", buildingType.name());
                building.put("displayName", buildingType.getDisplayName());
                building.put("name", buildingType.getDisplayName());
                building.put("size", buildingType.getSize());
                building.put("cost", faction.getBuildingCost(buildingType));
                building.put("requiredTechTier", buildingType.getRequiredTechTier());
                building.put("maxHealth", buildingType.getMaxHealth());
                building.put("powerValue", buildingType.getPowerValue());
                building.put("buildTimeSeconds", buildingType.getBuildTimeSeconds());
                building.put("canProduceUnits", buildingType.isCanProduceUnits());
                building.put("visionRange", buildingType.getVisionRange());
                building.put("techRequirements", buildingType.getTechRequirements());
                buildingInfo.add(building);
            }
            factionStatic.put("buildingInfo", buildingInfo);

            List<Map<String, Object>> unitInfo = new ArrayList<>();
            for (String unitName : availableUnits) {
                UnitType unitType = UnitType.valueOf(unitName);
                Map<String, Object> unit = new LinkedHashMap<>();
                unit.put("unitType", unitType.name());
                unit.put("displayName", unitType.getDisplayName());
                unit.put("cost", faction.getUnitCost(unitType));
                int initRentSlice = ArmyEconomy.periodicRentForUnit(faction, unitType);
                int initRent = Math.max(0, (int) Math.round(initRentSlice * armyRentGlobalMultiplier(faction)));
                unit.put("periodicArmyRent", initRent);
                unit.put("upkeep", initRent);
                unit.put("maxHealth", unitType.getMaxHealth());
                unit.put("damage", unitType.getDamage());
                unit.put("speed", unitType.getMovementSpeed());
                unit.put("range", unitType.getAttackRange());
                unit.put("buildTimeSeconds", unitType.getBuildTimeSeconds());
                unit.put("producedBy", unitType.getProducedBy().name());
                unit.put("category", unitType.getCategory().name());

                // Add tech requirements (required buildings)
                List<String> techReqs = unitType.getRequiredBuildings().stream()
                        .map(BuildingType::name)
                        .toList();
                unit.put("techRequirements", techReqs);

                unitInfo.add(unit);
            }
            factionStatic.put("unitInfo", unitInfo);

            init.put("myFactionStatic", factionStatic);
        }

        return init;
    }

    /**
     * Serialize obstacle static data (doesn't include health/resources which change)
     */
    private Map<String, Object> serializeObstacleStatic(Obstacle obstacle) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", obstacle.getId());
        data.put("x", obstacle.getPosition().x);
        data.put("y", obstacle.getPosition().y);
        data.put("size", obstacle.getSize());
        data.put("destructible", obstacle.isDestructible());
        data.put("maxHealth", obstacle.getMaxHealth());
        data.put("harvestable", obstacle.isHarvestable());
        if (obstacle.isHarvestable()) {
            data.put("resourceType", obstacle.getResourceType() != null ? obstacle.getResourceType().name() : null);
            data.put("maxResources", obstacle.getMaxResources());
        }

        // Extract vertices from physics body for accurate rendering
        data.put("vertices", extractBodyVertices(obstacle.getBody()));

        return data;
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
     * Non-combat activity hint for worker/medic/engineer so the client can show work visuals.
     */
    private String computeSupportActivity(Unit unit) {
        UnitType t = unit.getUnitType();
        if (t != UnitType.WORKER && t != UnitType.MEDIC && t != UnitType.ENGINEER) {
            return null;
        }
        UnitCommand cmd = unit.getCurrentCommand();
        if (cmd instanceof ConstructCommand) {
            return "BUILD";
        }
        if (cmd instanceof HarvestCommand h) {
            return h.isReturningResources() ? "CARRY" : "MINE";
        }
        if (cmd instanceof AutoHealCommand) {
            return "HEAL";
        }
        if (cmd instanceof AutoRepairCommand) {
            return "REPAIR";
        }
        // Manual heal/repair uses special ability while on idle/move — brief pulse after each use
        SpecialAbility ab = t.getSpecialAbility();
        long last = unit.getLastSpecialAbilityTime();
        if (last > 0) {
            long elapsed = System.currentTimeMillis() - last;
            if (elapsed >= 0 && elapsed < 400) {
                if (ab == SpecialAbility.HEAL) {
                    return "HEAL";
                }
                if (ab == SpecialAbility.REPAIR) {
                    return "REPAIR";
                }
            }
        }
        return null;
    }

    /**
     * Serialize a unit for network transmission
     *
     * @param unit             The unit to serialize
     * @param viewerTeamNumber The team number of the player viewing this unit (for filtering selection state)
     */
    private Map<String, Object> serializeUnit(Unit unit, int viewerTeamNumber) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", unit.getId());
        data.put("type", unit.getUnitType().name());
        data.put("x", unit.getPosition().x);
        data.put("y", unit.getPosition().y);
        data.put("rotation", unit.getRotation());
        data.put("health", unit.getHealth());
        data.put("maxHealth", unit.getMaxHealth());
        data.put("ownerId", unit.getOwnerId());
        data.put("team", unit.getTeamNumber());
        // NOTE: size, visionRange, specialAbility moved to unitTypes in gameInitialization
        // Only include selection state for units on the viewer's team
        data.put("selected", unit.isSelected() && unit.getTeamNumber() == viewerTeamNumber);
        data.put("isMoving", unit.isMoving());
        data.put("aiStance", unit.getAiStance().name());
        data.put("specialAbilityActive", unit.isSpecialAbilityActive());
        data.put("specialAbilityReady", unit.isSpecialAbilityReady());

        // Cloak status (for Cloak Tank)
        data.put("cloaked", unit.isCloaked());

        // Shield status (for Shield Tank)
        unit.getComponent(com.fullsteam.model.component.ShieldTankComponent.class)
                .ifPresent(shieldComp -> {
                    data.put("shieldActive", shieldComp.shieldActive());
                    data.put("shieldRadius", shieldComp.getRadius());
                });

        // Garrison status (for APC)
        if (unit.getUnitType() == UnitType.APC) {
            data.put("garrisonCount", unit.getGarrisonCount());
            data.put("maxGarrisonCapacity", unit.getComponent(APCComponent.class)
                    .map(APCComponent::getMaxGarrisonCapacity)
                    .orElse(3));
        }

        String supportActivity = computeSupportActivity(unit);
        if (supportActivity != null) {
            data.put("supportActivity", supportActivity);
        }

        // Add physics body vertices for accurate client-side rendering
        data.put("vertices", extractBodyVertices(unit.getBody()));
        return data;
    }

    /**
     * Serialize a building for network transmission
     */
    private Map<String, Object> serializeBuilding(Building building) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", building.getId());
        data.put("type", building.getBuildingType().name());
        data.put("x", building.getPosition().x);
        data.put("y", building.getPosition().y);
        data.put("rotation", building.getRotation());
        data.put("health", building.getHealth());
        data.put("maxHealth", building.getMaxHealth());
        data.put("ownerId", building.getOwnerId());
        data.put("team", building.getTeamNumber());
        data.put("active", building.isActive());
        data.put("underConstruction", building.isUnderConstruction());
        data.put("constructionPercent", building.getConstructionPercent());
        data.put("productionPercent", building.getProductionPercent());
        data.put("productionQueueSize", building.getProductionQueueSize());
        data.put("vertices", extractBodyVertices(building.getBody()));

        // Rally point
        if (building.getRallyPoint() != null) {
            Map<String, Object> rallyData = new LinkedHashMap<>();
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

        if (building.getBuildingType() == BuildingType.BUNKER) {
            data.put("garrisonCount", building.getGarrisonCount());
            data.put("maxGarrisonCapacity", building.getMaxGarrisonCapacity());
            building.getComponent(GarrisonComponent.class).ifPresent(gc -> {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (Unit u : gc.getGarrisonedUnitsSnapshot()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("unitId", u.getId());
                    row.put("unitType", u.getUnitType().name());
                    row.put("health", u.getHealth());
                    row.put("maxHealth", u.getMaxHealth());
                    row.put("deployed", false);
                    row.put("actions", List.of("EXIT"));
                    rows.add(row);
                }
                data.put("housedUnits", rows);
            });
        }

        if (building.getBuildingType() == BuildingType.AIRFIELD) {
            List<Map<String, Object>> rows = new ArrayList<>();
            building.getComponent(ProductionComponent.class).ifPresent(pc -> {
                UnitType cur = pc.getCurrentProductionUnitType();
                if (cur != null && cur.isSortieBased()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("berthIndex", -1);
                    row.put("producingType", cur.name());
                    row.put("productionProgress", pc.getProductionPercent());
                    row.put("deployed", false);
                    row.put("actions", List.of("CANCEL_PRODUCTION"));
                    rows.add(row);
                }
            });
            building.getComponent(AirfieldAircraftHousingComponent.class)
                    .ifPresent(housing -> {
                        int berthIndex = 0;
                        for (AirfieldAircraftHousingComponent.Berth berth : housing.getBerthsView()) {
                            if (berth.getHousedUnit() != null) {
                                Unit u = berth.getHousedUnit();
                                Map<String, Object> row = new LinkedHashMap<>();
                                row.put("berthIndex", berthIndex);
                                row.put("unitId", u.getId());
                                row.put("unitType", u.getUnitType().name());
                                row.put("health", u.getHealth());
                                row.put("maxHealth", u.getMaxHealth());
                                row.put("deployed", berth.isDeployed());
                                row.put("actions", berth.isDeployed() ? List.of("RTB") : List.of("LAUNCH", "SCRAP"));
                                rows.add(row);
                            }
                            berthIndex++;
                        }
                        data.put("aircraftBerthCapacity", AirfieldAircraftHousingComponent.MAX_BERTHS);
                    });
            data.put("housedUnits", rows);
        }

        return data;
    }

    /**
     * Serialize a projectile for network transmission
     */
    private Map<String, Object> serializeProjectile(Projectile projectile) {
        Map<String, Object> data = new LinkedHashMap<>();
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
        data.put("ordinance", projectile.getOrdinanceType().name());
        data.put("size", projectile.getSize()); // Use actual projectile size

        // Add bullet effects for visual rendering (e.g., SEEKING missiles)
        if (!projectile.getBulletEffects().isEmpty()) {
            List<String> effects = projectile.getBulletEffects().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
            data.put("bulletEffects", effects);
        }

        // Add target ID for seeking missiles (for debugging/visualization)
        if (projectile.getTargetEntityId() != null) {
            data.put("targetEntityId", projectile.getTargetEntityId());
        }

        return data;
    }

    /**
     * Serialize a field effect for network transmission
     */
    private Map<String, Object> serializeFieldEffect(FieldEffect effect) {
        Map<String, Object> data = new LinkedHashMap<>();
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
        Player aiFaction = new Player(
                aiPlayerId,
                aiTeam,
                FactionDefinition.builder().build()
        );
        players.put(aiPlayerId, aiFaction);

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
    public synchronized boolean addPlayer(int playerId, WebSocketSession webSocketSession,
                                          CustomFactionConfig config, FactionDefinition customDefinition) {
        // Prevent late joins if game has started with full roster
        if (gameStartedWithFullRoster) {
            log.warn("Player {} attempted to join game {} after it started with full roster",
                    playerId, gameId);
            return false;
        }

        if (countHumanPlayerFactions() >= gameConfig.getMaxPlayers()) {
            return false;
        }

        log.info("Adding player {} to game {}", playerId, gameId);

        // Assign team
        int teamNumber = assignPlayerToTeam();
        // Create faction with selected faction type and WebSocket
        Player faction = new Player(
                playerId,
                teamNumber,
                customDefinition,
                webSocketSession
        );

        log.info("Assigned player {} to team {}", playerId, teamNumber);
        players.put(playerId, faction);

        if (countHumanPlayerFactions() == gameConfig.getMaxPlayers()) {
            gameStartedWithFullRoster = true;
            log.info("Game {} has reached full capacity ({} players) - late joins now prevented",
                    gameId, gameConfig.getMaxPlayers());

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
        // Create starting base
        Vector2 startPosition = getStartingPosition(teamNumber);
        createStartingBase(playerId, teamNumber, startPosition);
        log.info("Player {} joined RTS game {} on team {}", playerId, gameId, teamNumber);
        return true;
    }

    private long countHumanPlayerFactions() {
        return players.values().stream().filter(f -> f.getPlayerId() >= 0).count();
    }

    /**
     * Assign player to team with fewest members
     */
    private int assignPlayerToTeam() {
        // Count HUMAN players per team (exclude AI player -1)
        int[] teamCounts = new int[gameConfig.getMaxPlayers() + 1];
        players.values().forEach(faction -> {
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
        Player faction = Objects.requireNonNull(players.get(playerId));

        // Create headquarters (with faction-modified health)
        double hqMaxHealth = faction.getBuildingHealth(BuildingType.HEADQUARTERS);
        Building hq = new Building(
                IdGenerator.nextEntityId(),
                gameEntities,
                BuildingType.HEADQUARTERS,
                position.x, position.y,
                playerId,
                teamNumber,
                faction,  // Pass faction reference for dynamic modifiers
                hqMaxHealth
        );
        buildings.put(hq.getId(), hq);
        world.addBody(hq.getBody());

        // Trigger perk hooks for building creation
        faction.getFactionDefinition().onBuildingCreated(hq, faction, this);

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
                    teamNumber,
                    faction  // Pass faction reference for dynamic modifiers
            );

            // Initialize components
            worker.initializeComponents(gameEntities);

            // Note: Research modifiers are now applied dynamically, no need to apply retroactively

            units.put(worker.getId(), worker);
            world.addBody(worker.getBody());

            // Trigger perk hooks for unit creation
            faction.getFactionDefinition().onUnitCreated(worker, faction, this);
        }

        log.info("Created starting base for player {} at ({}, {})", playerId, position.x, position.y);
    }

    /**
     * Remove a player from the game
     */
    public void removePlayer(int playerId) {
        players.remove(playerId);

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

    // Research system removed - handleStartUnitResearch() deleted
    // Research system removed - handleCancelUnitResearch() deleted  

    /**
     * Get all building types that a player has constructed (active buildings only)
     * Used for tech building requirements validation
     */
    private Set<BuildingType> getPlayerBuildingTypes(int playerId) {
        return buildings.values().stream()
                .filter(b -> b.belongsTo(playerId) && b.isActive() && !b.isUnderConstruction())
                .map(Building::getBuildingType)
                .collect(Collectors.toSet());
    }

    /**
     * Broadcast message to all players
     */
    public void broadcast(Object message) {
        players.values().forEach(faction -> {
            WebSocketSession ws = faction.getWebSocketSession();
            if (ws != null && ws.isOpen()) {
                send(ws, message);
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

        players.values().forEach(faction -> {
            WebSocketSession ws = faction.getWebSocketSession();
            if (ws == null || !ws.isOpen()) {
                return;
            }

            int playerId = faction.getPlayerId();

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
            }

            // Check exclusions
            if (shouldReceive
                    && target.getExcludePlayerIds() != null
                    && target.getExcludePlayerIds().contains(playerId)) {
                shouldReceive = false;
            }

            if (shouldReceive) {
                send(ws, event);
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
        Map<String, Object> data = new LinkedHashMap<>();
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
     * Shutdown the game
     */
    public void shutdown() {
        shutdown.set(true);
        updateTask.cancel(true);
        log.info("RTS Game {} shut down", gameId);
    }
}


