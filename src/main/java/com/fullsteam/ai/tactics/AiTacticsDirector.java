package com.fullsteam.ai.tactics;

import com.fullsteam.ai.AiPlayerPredicate;
import com.fullsteam.ai.SkirmishAiProfile;
import com.fullsteam.ai.support.AiWorldQueries;
import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Player;
import com.fullsteam.model.RTSGameManager;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.command.AttackMoveCommand;
import com.fullsteam.model.command.AttackTargetableCommand;
import com.fullsteam.model.command.IdleCommand;
import com.fullsteam.model.command.MoveCommand;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Issues unit commands directly (bypassing {@link com.fullsteam.model.RTSPlayerInput}) to achieve
 * continuous, responsive tactical behavior that the once-per-N-frames input queue cannot provide.
 *
 * <p>Runs every {@link SkirmishAiProfile#tacticsTickPeriod()} frames, staggered per player.
 * For each AI faction it maintains a small {@link AiSquadContext} state machine:
 *
 * <pre>
 *   STAGING  ──► ASSAULTING  (idle force &ge; assaultMinForce)
 *   ASSAULTING ──► STAGING   (no attack target remaining)
 *   any  ──► DEFENDING       (enemies inside defenseRadius)
 *   DEFENDING ──► STAGING    (threat cleared)
 * </pre>
 * <p>
 * Economic / building / production decisions are deliberately left to the
 * {@link com.fullsteam.ai.SkirmishAiDirector} economic channels.
 */
@Slf4j
public final class AiTacticsDirector {

    /**
     * Near-rally threshold: units within this distance are considered "at the rally point".
     */
    private static final double RALLY_ARRIVAL_RADIUS = 140.0;

    private final Map<Integer, AiSquadContext> squads = new HashMap<>();

    /**
     * Called once per game-loop tick. Staggered execution is handled internally per faction.
     */
    public void tick(RTSGameManager game, int frameCount) {
        if (game.isGameOver()) {
            return;
        }
        GameEntities entities = game.getGameEntities();
        for (Player faction : entities.getPlayers().values()) {
            if (!AiPlayerPredicate.isAiFaction(faction)) {
                continue;
            }
            int playerId = faction.getPlayerId();
            SkirmishAiProfile profile = SkirmishAiProfile.forDifficulty(faction.getSkirmishAiDifficulty());

            // Stagger ticks across players so work is spread across frames
            if ((frameCount + playerId) % profile.tacticsTickPeriod() != 0) {
                continue;
            }

            AiSquadContext ctx = squads.computeIfAbsent(playerId, id -> new AiSquadContext());
            tickFaction(entities, faction, ctx, profile);
        }
    }

    // -------------------------------------------------------------------------
    // Per-faction tick
    // -------------------------------------------------------------------------

    private void tickFaction(GameEntities entities, Player faction,
                              AiSquadContext ctx, SkirmishAiProfile profile) {
        int playerId = faction.getPlayerId();
        int team = faction.getTeamNumber();

        Vector2 base = resolveBase(entities, playerId);
        if (base == null) {
            return;
        }

        // Categorise owned combat units
        List<Unit> idleCombat = new ArrayList<>();
        List<Unit> activeCombat = new ArrayList<>();
        for (Unit u : entities.getUnits().values()) {
            if (!u.belongsTo(playerId) || !u.isActive() || u.isGarrisoned()) {
                continue;
            }
            if (!isTacticalUnit(u)) {
                continue;
            }
            boolean idle = u.getCurrentCommand() == null || u.getCurrentCommand() instanceof IdleCommand;
            if (idle) {
                idleCombat.add(u);
            } else {
                activeCombat.add(u);
            }
        }

        // Detect base threat
        Unit nearestThreat = nearestEnemyNearBase(entities, team, base, profile.defenseRadius());
        boolean threatened = nearestThreat != null;

        // Phase transitions
        updatePhase(ctx, idleCombat, activeCombat, threatened, entities, team, profile);

        // Refresh rally point
        ctx.setRallyPoint(computeRallyPoint(base, entities, team, profile.rallyDistanceFromHq()));

        // Issue commands for each phase
        switch (ctx.getPhase()) {
            case DEFENDING -> handleDefending(idleCombat, nearestThreat, entities);
            case ASSAULTING -> handleAssaulting(idleCombat, ctx, entities);
            case STAGING -> handleStaging(idleCombat, ctx, entities);
        }
    }

    // -------------------------------------------------------------------------
    // Phase transitions
    // -------------------------------------------------------------------------

    private void updatePhase(AiSquadContext ctx,
                              List<Unit> idleCombat, List<Unit> activeCombat,
                              boolean threatened, GameEntities entities,
                              int myTeam, SkirmishAiProfile profile) {
        if (threatened) {
            if (ctx.getPhase() != SquadPhase.DEFENDING) {
                log.debug("AI tactics: → DEFENDING (base threatened)");
            }
            ctx.setPhase(SquadPhase.DEFENDING);
            return;
        }

        if (ctx.getPhase() == SquadPhase.DEFENDING) {
            transitionToStaging(ctx, "threat cleared");
        }

        if (ctx.getPhase() == SquadPhase.ASSAULTING) {
            // Assault is over when the target is gone
            Vector2 target = AiWorldQueries.nearestAssaultTarget(entities, myTeam, ctx.getAssaultTarget());
            if (target == null) {
                transitionToStaging(ctx, "no assault target remaining");
            }
            return;
        }

        // STAGING → ASSAULTING: grace period + force threshold
        if (ctx.getPhase() == SquadPhase.STAGING) {
            long elapsedMs = System.currentTimeMillis() - ctx.getGameStartMs();
            long jitteredGrace = (long) (profile.assaultGracePeriodMs() * ctx.getGracePeriodJitterFactor());
            if (elapsedMs < jitteredGrace) {
                return; // respect the per-match jittered grace period
            }
            int totalForce = idleCombat.size() + activeCombat.size();
            if (totalForce >= profile.assaultMinForce()) {
                Vector2 target = AiWorldQueries.randomisedAssaultTarget(entities, myTeam, null);
                if (target != null) {
                    ctx.setAssaultTarget(target);
                    ctx.setWaveDispatched(false);
                    ctx.setPhase(SquadPhase.ASSAULTING);
                    log.debug("AI tactics: → ASSAULTING (force={}, target={}, elapsed={}ms, grace={}ms)",
                            totalForce, target, elapsedMs, jitteredGrace);
                }
            }
        }
    }

    /** Centralise all transitions back to STAGING so waveDispatched is always reset. */
    private static void transitionToStaging(AiSquadContext ctx, String reason) {
        ctx.setPhase(SquadPhase.STAGING);
        ctx.setAssaultTarget(null);
        ctx.setWaveDispatched(false);
        log.debug("AI tactics: → STAGING ({})", reason);
    }

    // -------------------------------------------------------------------------
    // Per-phase command issuance
    // -------------------------------------------------------------------------

    private void handleDefending(List<Unit> idleCombat, Unit threat, GameEntities entities) {
        if (threat == null || !threat.isActive()) {
            return;
        }
        for (Unit u : idleCombat) {
            u.issueCommand(new AttackTargetableCommand(u, threat, false), entities);
        }
    }

    private void handleAssaulting(List<Unit> idleCombat, AiSquadContext ctx, GameEntities entities) {
        if (ctx.getAssaultTarget() == null) {
            return;
        }
        if (!ctx.isWaveDispatched()) {
            // First tick of this assault: send all staged units toward the target
            for (Unit u : idleCombat) {
                u.issueCommand(new AttackMoveCommand(u, ctx.getAssaultTarget(), false), entities);
            }
            ctx.setWaveDispatched(true);
            log.debug("AI tactics: assault wave dispatched ({} units)", idleCombat.size());
        } else {
            // Wave is already en route — rally newly produced units for the next wave
            handleStaging(idleCombat, ctx, entities);
        }
    }

    private void handleStaging(List<Unit> idleCombat, AiSquadContext ctx, GameEntities entities) {
        Vector2 rally = ctx.getRallyPoint();
        if (rally == null) {
            return;
        }
        for (Unit u : idleCombat) {
            if (u.getPosition().distance(rally) > RALLY_ARRIVAL_RADIUS) {
                u.issueCommand(new MoveCommand(u, rally, false), entities);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * A tactical unit is an active, non-worker, non-sortie combat unit that is not garrisoned.
     */
    private static boolean isTacticalUnit(Unit u) {
        UnitType type = u.getUnitType();
        return type.canAttack() && type != UnitType.WORKER && !type.isSortieBased();
    }

    /**
     * Returns the nearest enemy unit inside {@code radius} of {@code base}, or {@code null}.
     */
    private static Unit nearestEnemyNearBase(GameEntities entities, int myTeam,
                                             Vector2 base, double radius) {
        Unit nearest = null;
        double nearestDist = radius * radius;
        for (Unit u : entities.getUnits().values()) {
            if (!u.isActive() || u.getTeamNumber() == myTeam) {
                continue;
            }
            double d = u.getPosition().distanceSquared(base);
            if (d <= nearestDist) {
                nearestDist = d;
                nearest = u;
            }
        }
        return nearest;
    }

    /**
     * Computes a rally point {@code distance} units forward from {@code base} toward the nearest
     * enemy structure. Falls back to a fixed northward offset if no enemies exist.
     */
    private static Vector2 computeRallyPoint(Vector2 base, GameEntities entities,
                                             int myTeam, double distance) {
        Vector2 target = AiWorldQueries.nearestAssaultTarget(entities, myTeam, base);
        if (target != null) {
            double dx = target.x - base.x;
            double dy = target.y - base.y;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len > 1.0) {
                return new Vector2(base.x + (dx / len) * distance,
                        base.y + (dy / len) * distance);
            }
        }
        return new Vector2(base.x, base.y + distance);
    }

    /**
     * Returns the HQ position, or the position of any owned active building as fallback.
     */
    private static Vector2 resolveBase(GameEntities entities, int playerId) {
        Building hq = entities.getBuildings().values().stream()
                .filter(b -> b.belongsTo(playerId)
                        && b.getBuildingType() == BuildingType.HEADQUARTERS
                        && b.isActive())
                .findFirst()
                .orElse(null);
        if (hq != null) {
            return hq.getPosition().copy();
        }
        return entities.getBuildings().values().stream()
                .filter(b -> b.belongsTo(playerId) && b.isActive())
                .min(Comparator.comparingInt(Building::getId))
                .map(b -> b.getPosition().copy())
                .orElse(null);
    }
}
