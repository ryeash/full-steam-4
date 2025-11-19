package com.fullsteam.model;

import io.micronaut.websocket.WebSocketSession;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a player's WebSocket session.
 */
@Data
@AllArgsConstructor
public class PlayerSession {
    private int playerId;
    private WebSocketSession session;
    private String playerName;
}

