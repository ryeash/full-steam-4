package com.fullsteam.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Getter
public class GameEntities {
    private final GameConfig gameConfig;
    private final Map<Integer, PlayerFaction> playerFactions;
    private final Map<Integer, Unit> units;
    private final Map<Integer, Building> buildings;
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
        } else if (e instanceof Obstacle o) {
            obstacles.put(o.getId(), o);
        } else if (e instanceof Projectile p) {
            projectiles.put(p.getId(), p);
        } else if (e instanceof Beam b) {
            beams.put(b.getId(), b);
            createBeamFieldEffects(b);
        } else if (e instanceof FieldEffect fe) {
            fieldEffects.put(fe.getId(), fe);
        } else if (e instanceof WallSegment ws) {
            wallSegments.put(ws.getId(), ws);
        } else {
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
     * Find nearest enemy unit to a position that can be targeted by the attacker's weapon.
     * Respects cloak detection and elevation targeting.
     */
    public Unit findNearestEnemyUnit(Vector2 position, int teamNumber, double maxRange, Unit attacker) {
        if (attacker == null) {
            return findNearestEnemyUnit(position, teamNumber, maxRange); // Fall back to non-elevation version
        }

        return units.values().stream()
                .filter(u -> u.isActive() && u.getTeamNumber() != teamNumber)
                .filter(u -> attacker.canTargetElevation(u)) // Check elevation targeting
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

    /**
     * Find the nearest enemy targetable entity (unit, building, or wall).
     * This is a unified method that searches across all targetable types.
     * Useful for weapon systems that can target anything.
     *
     * @param position   Position to search from
     * @param teamNumber Team number of the attacker
     * @param maxRange   Maximum search range
     * @param attacker   The attacking unit (for elevation targeting checks), can be null
     * @return The nearest enemy targetable, or null if none found
     */
    public Targetable findNearestEnemyTargetable(Vector2 position, int teamNumber, double maxRange, Unit attacker) {
        Targetable nearestTarget = null;
        double nearestDistance = maxRange;

        // Debug logging for flak tanks
        boolean isFlakTank = attacker != null && attacker.getUnitType() == UnitType.FLAK_TANK;
        if (isFlakTank) {
            log.info("Flak Tank {} searching for targets (maxRange: {}, canTargetBuildings: {})",
                    attacker.getId(), maxRange, attacker.canTargetBuildings());
        }

        // Search units
        int unitsChecked = 0;
        int unitsSkippedElevation = 0;
        int unitsSkippedCloaked = 0;
        for (Unit unit : units.values()) {
            if (!unit.isActive() || unit.getTeamNumber() == teamNumber) continue;

            unitsChecked++;

            // Check elevation targeting if attacker is specified
            if (attacker != null && !attacker.canTargetElevation(unit)) {
                unitsSkippedElevation++;
                if (isFlakTank) {
                    log.info("  Skipped unit {} - elevation mismatch (target elevation: {})",
                            unit.getId(), unit.getUnitType().getElevation());
                }
                continue;
            }

            // Check cloak detection
            double distance = unit.getPosition().distance(position);
            if (unit.isCloaked() && distance > Unit.getCloakDetectionRange()) {
                unitsSkippedCloaked++;
                continue;
            }

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestTarget = unit;
                if (isFlakTank) {
                    log.info("  Found potential target unit {} at distance {} (elevation: {})",
                            unit.getId(), distance, unit.getUnitType().getElevation());
                }
            }
        }

        if (isFlakTank) {
            log.info("  Units: checked={}, skippedElevation={}, skippedCloaked={}",
                    unitsChecked, unitsSkippedElevation, unitsSkippedCloaked);
        }

        // Search buildings (only if attacker can target ground)
        if (attacker == null || attacker.canTargetBuildings()) {
            for (Building building : buildings.values()) {
                if (!building.isActive() || building.getTeamNumber() == teamNumber) continue;

                double distance = building.getPosition().distance(position);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestTarget = building;
                }
            }
        }

        // Search wall segments (only if attacker can target ground)
        if (attacker == null || attacker.canTargetBuildings()) {
            for (WallSegment wall : wallSegments.values()) {
                if (!wall.isActive() || wall.getTeamNumber() == teamNumber) continue;

                double distance = wall.getPosition().distance(position);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestTarget = wall;
                }
            }
        }

        if (isFlakTank) {
            log.info("  Final result: {} ({})",
                    nearestTarget != null ? nearestTarget.getTargetType() + " " + nearestTarget.getId() : "null",
                    nearestTarget != null ? "distance: " + nearestDistance : "none found");
        }

        return nearestTarget;
    }

    /**
     * Find the nearest enemy targetable entity (simplified version without elevation checks).
     *
     * @param position   Position to search from
     * @param teamNumber Team number of the attacker
     * @param maxRange   Maximum search range
     * @return The nearest enemy targetable, or null if none found
     */
    public Targetable findNearestEnemyTargetable(Vector2 position, int teamNumber, double maxRange) {
        return findNearestEnemyTargetable(position, teamNumber, maxRange, null);
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

