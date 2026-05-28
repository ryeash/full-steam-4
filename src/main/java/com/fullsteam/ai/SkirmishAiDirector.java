package com.fullsteam.ai;

import com.fullsteam.ai.behavior.HarvestEconomyBehavior;
import com.fullsteam.ai.behavior.PlaceAdvancedPowerPlantBehavior;
import com.fullsteam.ai.behavior.PlaceBarracksBehavior;
import com.fullsteam.ai.behavior.PlaceFactoryBehavior;
import com.fullsteam.ai.behavior.PlacePowerPlantBehavior;
import com.fullsteam.ai.behavior.PlaceRefineryBehavior;
import com.fullsteam.ai.behavior.PlaceResearchLabBehavior;
import com.fullsteam.ai.behavior.PlaceTechCenterBehavior;
import com.fullsteam.ai.behavior.ProduceUnitsBehavior;
import com.fullsteam.ai.behavior.ResumeConstructionBehavior;
import com.fullsteam.ai.behavior.SkirmishAiBehavior;
import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.ai.tactics.AiTacticsDirector;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Player;
import com.fullsteam.model.RTSGameManager;
import com.fullsteam.model.RTSPlayerInput;

import java.util.List;

/**
 * Drives all AI decision-making each game tick by coordinating two independent subsystems:
 *
 * <h3>Economic Director (this class)</h3>
 * Three independent behavior channels so multiple economic decisions can happen in the same tick:
 * <ol>
 *   <li><b>Building channel</b> — placement behaviors run in priority order; first one that fires wins
 *       (a single worker can only be sent to one site per tick).</li>
 *   <li><b>Harvest channel</b> — assigns idle workers to resource deposits; always runs when applicable.</li>
 *   <li><b>Production channel</b> — queues one unit per idle production building; may emit multiple
 *       {@link RTSPlayerInput}s so all factories and barracks fill in the same tick.</li>
 * </ol>
 * All economic actions go through
 * {@link RTSGameManager#acceptPlayerInput(int, RTSPlayerInput)} to retain full validation.
 *
 * <h3>Tactical Director ({@link AiTacticsDirector})</h3>
 * Issues unit commands directly (bypassing the input queue) for responsive combat behavior:
 * unit rallying, assault waves, and base defense.  Runs on its own per-faction cadence.
 */
public final class SkirmishAiDirector {

    // Building channel: first-wins priority order (a worker can only be sent to one site per tick).
    // Tech tree progression: Barracks → Refinery → ResearchLab (T2) → Factory → TechCenter (T3).
    // PowerPlant runs early so the AI always covers new power demands before the next tech step.
    private final List<SkirmishAiBehavior> buildingChannel = List.of(
            new ResumeConstructionBehavior(),
            new PlacePowerPlantBehavior(),
            new PlaceAdvancedPowerPlantBehavior(),
            new PlaceBarracksBehavior(),
            new PlaceRefineryBehavior(),
            new PlaceResearchLabBehavior(),
            new PlaceFactoryBehavior(),
            new PlaceTechCenterBehavior()
    );

    // Independent channels — each may emit inputs regardless of what the other channels did
    private final HarvestEconomyBehavior harvestChannel = new HarvestEconomyBehavior();
    private final ProduceUnitsBehavior productionChannel = new ProduceUnitsBehavior();

    // Tactical director issues commands directly to units
    private final AiTacticsDirector tacticsDirector = new AiTacticsDirector();

    public void contributeInputs(RTSGameManager game, int frameCount) {
        if (game.isGameOver()) {
            return;
        }
        GameEntities entities = game.getGameEntities();

        for (Player faction : entities.getPlayers().values()) {
            if (!AiPlayerPredicate.isAiFaction(faction)) {
                continue;
            }
            int playerId = faction.getPlayerId();
            SkirmishAiTickContext ctx = SkirmishAiTickContext.build(entities, playerId, frameCount);
            AiConcernSnapshot concerns = AiConcernSnapshot.from(ctx);

            // --- Building channel (first-wins) ---
            for (SkirmishAiBehavior behavior : buildingChannel) {
                if (!behavior.isReadyThisFrame(ctx)) {
                    continue;
                }
                if (!behavior.isApplicable(entities, ctx, concerns)) {
                    continue;
                }
                List<RTSPlayerInput> inputs = behavior.propose(entities, ctx);
                if (!inputs.isEmpty()) {
                    inputs.forEach(in -> game.acceptPlayerInput(playerId, in));
                    break; // only one building action per tick (shared worker pool)
                }
            }

            // --- Harvest channel (independent) ---
            if (harvestChannel.isReadyThisFrame(ctx)
                    && harvestChannel.isApplicable(entities, ctx, concerns)) {
                harvestChannel.propose(entities, ctx)
                        .forEach(in -> game.acceptPlayerInput(playerId, in));
            }

            // --- Production channel (independent, may return multiple inputs) ---
            if (productionChannel.isReadyThisFrame(ctx)
                    && productionChannel.isApplicable(entities, ctx, concerns)) {
                productionChannel.propose(entities, ctx)
                        .forEach(in -> game.acceptPlayerInput(playerId, in));
            }
        }

        // --- Tactical director (direct command issuance, runs on its own cadence) ---
        tacticsDirector.tick(game, frameCount);
    }
}
