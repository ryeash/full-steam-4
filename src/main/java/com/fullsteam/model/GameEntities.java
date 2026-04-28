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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
    /** Temporary orbital vision from {@link CommandAbilityType#SATELLITE_SWEEP}. */
    private final List<SatelliteReveal> satelliteReveals = new CopyOnWriteArrayList<>();
    private final RTSGameManager rtsGameManager;

    @Setter
    private World<Body> world;

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

    public void addSatelliteReveal(SatelliteReveal reveal) {
        satelliteReveals.add(reveal);
    }

    public List<SatelliteReveal> getSatelliteReveals() {
        return satelliteReveals;
    }

    public void pruneExpiredSatelliteReveals() {
        satelliteReveals.removeIf(r -> !r.isActive());
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

