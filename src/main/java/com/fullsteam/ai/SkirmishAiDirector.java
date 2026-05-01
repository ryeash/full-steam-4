package com.fullsteam.ai;

import com.fullsteam.ai.behavior.DefendBaseBehavior;
import com.fullsteam.ai.behavior.HarvestEconomyBehavior;
import com.fullsteam.ai.behavior.PlaceBarracksBehavior;
import com.fullsteam.ai.behavior.PlacePowerPlantBehavior;
import com.fullsteam.ai.behavior.PlaceRefineryBehavior;
import com.fullsteam.ai.behavior.PlaceResearchLabBehavior;
import com.fullsteam.ai.behavior.PlaceFactoryBehavior;
import com.fullsteam.ai.behavior.ProduceUnitsBehavior;
import com.fullsteam.ai.behavior.ResumeConstructionBehavior;
import com.fullsteam.ai.behavior.SkirmishAiBehavior;
import com.fullsteam.ai.behavior.SquadAttackMoveBehavior;
import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Player;
import com.fullsteam.model.RTSGameManager;

import java.util.List;

/**
 * Per tick: builds a {@link SkirmishAiTickContext}, derives {@link AiConcernSnapshot} pressures,
 * then runs ordered {@link SkirmishAiBehavior}s until one emits an {@link com.fullsteam.model.RTSPlayerInput}.
 * All actions go through {@link RTSGameManager#acceptPlayerInput(int, com.fullsteam.model.RTSPlayerInput)}.
 */
public final class SkirmishAiDirector {

    private final List<SkirmishAiBehavior> behaviors = List.of(
            new DefendBaseBehavior(),
            new ResumeConstructionBehavior(),
            new PlacePowerPlantBehavior(),
            new PlaceBarracksBehavior(),
            new PlaceRefineryBehavior(),
            new PlaceResearchLabBehavior(),
            new PlaceFactoryBehavior(),
            new HarvestEconomyBehavior(),
            new ProduceUnitsBehavior(),
            new SquadAttackMoveBehavior()
    );

    public void contributeInputs(RTSGameManager game, int frameCount) {
        if (game.isGameOver()) {
            return;
        }
        GameEntities entities = game.getGameEntities();
        for (Player faction : entities.getPlayerFactions().values()) {
            if (!AiPlayerPredicate.isAiFaction(faction)) {
                continue;
            }
            int playerId = faction.getPlayerId();
            SkirmishAiTickContext ctx = SkirmishAiTickContext.build(entities, playerId, frameCount);
            AiConcernSnapshot concerns = AiConcernSnapshot.from(ctx);
            for (SkirmishAiBehavior behavior : behaviors) {
                if (!behavior.isReadyThisFrame(ctx)) {
                    continue;
                }
                if (!behavior.isApplicable(entities, ctx, concerns)) {
                    continue;
                }
                var input = behavior.propose(entities, ctx);
                if (input.isPresent()) {
                    game.acceptPlayerInput(playerId, input.get());
                    break;
                }
            }
        }
    }
}
