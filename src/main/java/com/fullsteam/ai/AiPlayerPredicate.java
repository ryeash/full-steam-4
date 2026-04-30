package com.fullsteam.ai;

import com.fullsteam.model.Player;

/**
 * Identifies factions controlled by server-side AI (no live WebSocket session).
 */
public final class AiPlayerPredicate {

    private AiPlayerPredicate() {
    }

    public static boolean isAiFaction(Player player) {
        return player != null && player.getWebSocketSession() == null;
    }
}
