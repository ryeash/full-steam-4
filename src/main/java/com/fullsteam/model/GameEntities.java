package com.fullsteam.model;

import com.fullsteam.model.weapon.Weapon;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Container for all game entities and the physics world.
 * Provides convenient access to units, buildings, obstacles, etc.
 * Used by commands and AI to make intelligent decisions.
 */
@Slf4j
@Getter
public class GameEntities {
    private final GameConfig gameConfig;
    private final Map<Integer, Player> playerFactions;
    private final Map<Integer, Unit> units;
    private final Map<Integer, Building> buildings;
    private final Map<Integer, Obstacle> obstacles;
    private final Map<Integer, Projectile> projectiles;
    private final Map<Integer, Beam> beams;
    private final Map<Integer, FieldEffect> fieldEffects;
    private final Map<Integer, TrackerBug> trackerBugs; // Spy tracking devices
    private final RTSGameManager rtsGameManager;

    @Setter
    private World<Body> world;

    /**
     * Set by {@link RTSGameManager} after {@link RTSCollisionProcessor} is constructed.
     * Used for placement queries ({@link #suggestBuildLocationNear}) shared by AI and bootstrap logic.
     */
    @Setter
    private RTSCollisionProcessor collisionProcessor;

    private static final double[][] BUILD_SITE_PROBE_OFFSETS = {
            {220, 80}, {-220, 80}, {260, -120}, {-260, -120},
            {320, 40}, {-320, 40}, {180, 200}, {-180, 200},
            {0, 220}, {0, -220}, {400, 0}, {-400, 0}
    };

    public GameEntities(GameConfig gameConfig, RTSGameManager rtsGameManager) {
        this.gameConfig = gameConfig;
        this.playerFactions = new ConcurrentSkipListMap<>();
        this.units = new ConcurrentSkipListMap<>();
        this.buildings = new ConcurrentSkipListMap<>();
        this.obstacles = new ConcurrentSkipListMap<>();
        this.projectiles = new ConcurrentSkipListMap<>();
        this.beams = new ConcurrentSkipListMap<>();
        this.fieldEffects = new ConcurrentSkipListMap<>();
        this.trackerBugs = new ConcurrentSkipListMap<>();
        this.rtsGameManager = rtsGameManager;
        this.world = null;
    }

    public void add(GameEntity e) {
        if (e == null) {
            return;
        }

        if (e instanceof Unit u) {
            units.put(u.getId(), u);
        } else if (e instanceof Building b) {
            buildings.put(b.getId(), b);
        } else if (e instanceof Obstacle o) {
            obstacles.put(o.getId(), o);
        } else if (e instanceof Projectile p) {
            projectiles.put(p.getId(), p);
        } else if (e instanceof Beam b) {
            beams.put(b.getId(), b);
            createBeamFieldEffects(b);
        } else if (e instanceof FieldEffect fe) {
            fieldEffects.put(fe.getId(), fe);
        } else {
            throw new UnsupportedOperationException();
        }

        world.addBody(e.getBody());
    }

    /**
     * Add a tracker bug (doesn't have physics body).
     */
    public void addTrackerBug(TrackerBug bug) {
        trackerBugs.put(bug.getId(), bug);
        log.info("Added tracker bug {} on {} {}", bug.getId(), bug.getAttachmentKind(), bug.getTargetId());
    }

    public Targetable findNearestEnemyTargetable(Unit attacker) {
        return findNearestEnemyTargetable(attacker.getPosition(), attacker.getTeamNumber(), attacker.getWeapon());
    }

    /**
     * Find the nearest enemy targetable entity (unit, building, or wall).
     * This is a unified method that searches across all targetable types.
     * Useful for weapon systems that can target anything.
     *
     * @param position   Position to search from
     * @param teamNumber Team number of the attacker
     * @return The nearest enemy targetable, or null if none found
     */
    public Targetable findNearestEnemyTargetable(Vector2 position, int teamNumber, Weapon weapon) {
        return Stream.of(units.values(), buildings.values())
                .flatMap(Collection::stream)
                .filter(u -> u.isValidTargetFor(weapon, teamNumber, position))
                .min(Comparator.comparingDouble(u -> u.getPosition().distance(position)))
                .orElse(null);
    }

    /**
     * Completed structures only — used for tech and production eligibility (matches prior {@code getPlayerBuildingTypes} semantics).
     */
    public Set<BuildingType> getConstructedBuildingTypes(int playerId) {
        return buildings.values().stream()
                .filter(b -> b.belongsTo(playerId) && b.isActive() && !b.isUnderConstruction())
                .map(Building::getBuildingType)
                .collect(Collectors.toSet());
    }

    /**
     * Tech buildings still required before construction of {@code buildingType} can succeed.
     */
    public Set<BuildingType> getMissingTechForConstruction(int playerId, BuildingType buildingType) {
        Set<BuildingType> completed = getConstructedBuildingTypes(playerId);
        HashSet<BuildingType> techRequired = new HashSet<>(buildingType.getTechRequirements());
        techRequired.removeAll(completed);
        return techRequired;
    }

    /**
     * Whether the player has any active building of this type (includes foundations still building if marked active).
     */
    public boolean playerHasActiveBuilding(int playerId, BuildingType buildingType) {
        return buildings.values().stream()
                .anyMatch(b -> b.belongsTo(playerId)
                        && b.isActive()
                        && b.getBuildingType() == buildingType);
    }

    /**
     * Spatial + support-capacity probe around {@code anchor}. Does not validate affordability or worker selection;
     * {@link RTSGameManager#processPlayerInput} applies those when processing {@link RTSPlayerInput} build orders.
     */
    public Optional<Vector2> suggestBuildLocationNear(int playerId, BuildingType type, Vector2 anchor) {
        if (anchor == null || collisionProcessor == null || gameConfig == null) {
            return Optional.empty();
        }
        double w = gameConfig.getWorldWidth();
        double h = gameConfig.getWorldHeight();
        for (double[] d : BUILD_SITE_PROBE_OFFSETS) {
            Vector2 loc = new Vector2(anchor.x + d[0], anchor.y + d[1]);
            if (collisionProcessor.isValidBuildLocation(loc, type, w, h)
                    && collisionProcessor.hasSupportCapacity(loc, type, playerId)) {
                return Optional.of(loc);
            }
        }
        return Optional.empty();
    }

    private void createBeamFieldEffects(Beam beam) {
        for (BulletEffect bulletEffect : beam.getBulletEffects()) {
            switch (bulletEffect) {
                case ELECTRIC -> {
                    Vector2 hitPosition = beam.getEndPosition(); // Where the beam ended (hit or max range)
                    double electricDamage = beam.getDamage() * 0.3; // 30% of beam damage per second
                    double electricRadius = 40.0; // Fixed radius for beam electric fields

                    FieldEffect electricField = new FieldEffect(
                            beam.getOwnerId(),
                            FieldEffectType.ELECTRIC,
                            hitPosition,
                            electricRadius,
                            electricDamage,
                            FieldEffectType.ELECTRIC.getDefaultDuration(),
                            beam.getOwnerTeam()
                    );
                    add(electricField);
                }
                default -> throw new UnsupportedOperationException("unsupported beam effect: " + bulletEffect);
            }
        }
    }
}

