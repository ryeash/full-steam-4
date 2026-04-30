package com.fullsteam.ai.behavior;

/**
 * Staggers AI behaviors across frames so multiple factions do not spike work on the same tick.
 */
public final class AiTickPhasing {

    private AiTickPhasing() {
    }

    public static boolean every(int frameCount, int playerId, int period, int salt) {
        return period > 0 && (frameCount + playerId + salt) % period == 0;
    }
}
