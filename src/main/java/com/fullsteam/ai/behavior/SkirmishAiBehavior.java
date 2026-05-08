package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.RTSPlayerInput;

import java.util.List;

/**
 * One economic policy: decides whether it applies, then emits zero or more {@link RTSPlayerInput}s per tick.
 * Reads world state only from {@link GameEntities}; submission is handled by {@link com.fullsteam.ai.SkirmishAiDirector}.
 *
 * <p>Most behaviors return at most one input. {@link ProduceUnitsBehavior} may return one per
 * idle production building so all queues fill in the same tick.
 *
 * <p>Unit tactical commands (attack, move, defend) are handled by
 * {@link com.fullsteam.ai.tactics.AiTacticsDirector}, which issues commands directly.
 */
public interface SkirmishAiBehavior {

    boolean isReadyThisFrame(SkirmishAiTickContext ctx);

    boolean isApplicable(GameEntities entities, SkirmishAiTickContext ctx, AiConcernSnapshot concerns);

    List<RTSPlayerInput> propose(GameEntities entities, SkirmishAiTickContext ctx);
}
