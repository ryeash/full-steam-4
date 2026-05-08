package com.fullsteam.ai.tactics;

import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Mutable per-faction state retained across ticks by {@link AiTacticsDirector}.
 * Tracks the current squad phase and the last computed rally point so the director
 * does not need to recompute geometry on every frame.
 */
@Getter
@Setter
public final class AiSquadContext {

    private SquadPhase phase = SquadPhase.STAGING;

    /** Epoch ms when this context was first created — used for the early-game grace period. */
    private final long gameStartMs = System.currentTimeMillis();

    /**
     * Per-match multiplier applied to {@link com.fullsteam.ai.SkirmishAiProfile#assaultGracePeriodMs()}
     * so each game plays out on a slightly different timeline.  Drawn once from [0.80, 1.20].
     */
    private final double gracePeriodJitterFactor = ThreadLocalRandom.current().nextDouble(0.80, 1.20);

    /**
     * Forward staging position between the base and the current attack target.
     */
    private Vector2 rallyPoint = null;

    /**
     * Last known attack-target position; refreshed each assault tick.
     */
    private Vector2 assaultTarget = null;

    /**
     * True once the current wave's units have been dispatched toward the assault target.
     * While {@code true}, newly idle units rally at the staging point instead of joining
     * the ongoing assault, so the next wave can build up independently.
     * Reset to {@code false} whenever the phase returns to STAGING.
     */
    private boolean waveDispatched = false;
}
