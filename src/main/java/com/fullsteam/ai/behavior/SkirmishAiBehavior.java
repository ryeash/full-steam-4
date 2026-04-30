package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.RTSPlayerInput;

import java.util.Optional;

/**
 * One tactical policy: decides whether it applies, then emits at most one {@link RTSPlayerInput} per tick.
 * Reads world state only from {@link GameEntities}; submission is handled by {@link com.fullsteam.ai.SkirmishAiDirector}.
 */
public interface SkirmishAiBehavior {

    boolean isReadyThisFrame(SkirmishAiTickContext ctx);

    boolean isApplicable(GameEntities entities, SkirmishAiTickContext ctx, AiConcernSnapshot concerns);

    Optional<RTSPlayerInput> propose(GameEntities entities, SkirmishAiTickContext ctx);
}
