package com.fullsteam.model;

import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.World;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

/**
 * Container for all game entities and the physics world.
 * Provides convenient access to units, buildings, obstacles, etc.
 * Used by commands and AI to make intelligent decisions.
 */
@Getter
public class GameEntities {
    private final GameConfig gameConfig;
    private final Map<Integer, PlayerFaction> playerFactions;
    private final Map<Integer, Unit> units;
    private final Map<Integer, Building> buildings;
    private final Map<Integer, ResourceDeposit> resourceDeposits;
    private final Map<Integer, WallSegment> wallSegments;
    private final Map<Integer, Obstacle> obstacles;
    private final Map<Integer, Projectile> projectiles;
    private final Map<Integer, Beam> beams;
    private final Map<Integer, FieldEffect> fieldEffects;
    private final Consumer<GameEvent> gameEventSender;

    @Setter
    private World<Body> world; // The physics world (for raycasting, etc.)

    public GameEntities(GameConfig gameConfig, Consumer<GameEvent> gameEventSender) {
        this.gameConfig = gameConfig;
        this.gameEventSender = gameEventSender;
        this.playerFactions = new ConcurrentSkipListMap<>();
        this.units = new ConcurrentSkipListMap<>();
        this.buildings = new ConcurrentSkipListMap<>();
        this.resourceDeposits = new ConcurrentSkipListMap<>();
        this.wallSegments = new ConcurrentSkipListMap<>();
        this.obstacles = new ConcurrentSkipListMap<>();
        this.projectiles = new ConcurrentSkipListMap<>();
        this.beams = new ConcurrentSkipListMap<>();
        this.fieldEffects = new ConcurrentSkipListMap<>();
        this.world = null; // Set by RTSGameManager
    }

    public void add(GameEntity e) {
        if (e == null) {
            return;
        }

        if (e instanceof Unit u) {
            units.put(u.getId(), u);
        } else if (e instanceof Building b) {
            buildings.put(b.getId(), b);
        } else if (e instanceof ResourceDeposit rd) {
            resourceDeposits.put(rd.getId(), rd);
        } else if (e instanceof Obstacle o) {
            obstacles.put(o.getId(), o);
        } else if (e instanceof Projectile p) {
            projectiles.put(p.getId(), p);
        } else if (e instanceof Beam b) {
            beams.put(b.getId(), b);
            createBeamFieldEffects(b);
        } else if (e instanceof FieldEffect fe) {
            fieldEffects.put(fe.getId(), fe);
        }
//         TODO: why isn't wall segment a GameEntity?
//        else if(e instanceof WallSegment ws){
//            wallSegments.put(ws.getId(), ws);
//        }
        else {
            throw new UnsupportedOperationException();
        }

        world.addBody(e.getBody());
    }

    /**
     * Find nearest enemy unit to a position (respects cloak detection)
     */
    public Unit findNearestEnemyUnit(Vector2 position, int teamNumber, double maxRange) {
        return units.values().stream()
                .filter(u -> u.isActive() && u.getTeamNumber() != teamNumber)
                .filter(u -> {
                    double distance = u.getPosition().distance(position);
                    // Cloaked units can only be detected within cloak detection range
                    if (u.isCloaked()) {
                        return distance <= Math.min(maxRange, Unit.getCloakDetectionRange());
                    }
                    return distance <= maxRange;
                })
                .min(Comparator.comparingDouble(u -> u.getPosition().distance(position)))
                .orElse(null);
    }

    /**
     * Find nearest enemy building to a position
     */
    public Building findNearestEnemyBuilding(Vector2 position, int teamNumber, double maxRange) {
        return buildings.values().stream()
                .filter(b -> b.isActive() && b.getTeamNumber() != teamNumber)
                .filter(b -> b.getPosition().distance(position) <= maxRange)
                .min(Comparator.comparingDouble(b -> b.getPosition().distance(position)))
                .orElse(null);
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

