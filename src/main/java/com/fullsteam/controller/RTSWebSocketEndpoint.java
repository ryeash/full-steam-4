package com.fullsteam.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullsteam.model.Player;
import com.fullsteam.model.RTSGameManager;
import com.fullsteam.model.RTSPlayerInput;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import static com.fullsteam.controller.RTSPlayerConnectionService.SESSION_KEY;

/**
 * WebSocket endpoint for RTS game connections.
 * URL format: /rts/{gameId}?sessionToken={token}
 */
@ServerWebSocket("/rts/{gameId}")
public class RTSWebSocketEndpoint {
    private static final Logger log = LoggerFactory.getLogger(RTSWebSocketEndpoint.class);

    private final RTSPlayerConnectionService connectionService;
    private final ObjectMapper objectMapper;

    @Inject
    public RTSWebSocketEndpoint(RTSPlayerConnectionService connectionService, ObjectMapper objectMapper) {
        this.connectionService = connectionService;
        this.objectMapper = objectMapper;
    }

    @OnOpen
    public void onOpen(WebSocketSession session, String gameId) {
        log.info("RTS WebSocket connection opened for gameId: {}", gameId);

        String sessionToken;
        String factionConfigJson;
        try {
            sessionToken = session.getRequestParameters().get("sessionToken");
            if (sessionToken == null || sessionToken.isBlank()) {
                throw new IllegalArgumentException("Missing sessionToken parameter");
            }
            String factionConfigParam = session.getRequestParameters().get("factionConfig");
            if (factionConfigParam == null || factionConfigParam.isBlank()) {
                throw new IllegalArgumentException("Missing factionConfig parameter");
            }
            factionConfigJson = new String(
                    Base64.getDecoder().decode(URLDecoder.decode(factionConfigParam, StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Could not extract connection parameters for game {}: {}", gameId, e.getMessage());
            sendErrorAndClose(session, "INVALID_PARAMS",
                    "Missing or invalid connection parameters. Please return to the lobby and try again.");
            return;
        }

        if (!connectionService.connectPlayer(session, gameId, sessionToken, factionConfigJson)) {
            log.warn("Failed to connect player to RTS game {}, closing session", gameId);
            session.close();
        } else {
            log.info("Player successfully connected to RTS game {} with session token {}", gameId, sessionToken);
        }
    }

    /**
     * Send a plain JSON error message synchronously (the client handles both gzip and plain frames)
     * then close the session.  Used before the player is formally added to a game.
     */
    private void sendErrorAndClose(WebSocketSession session, String code, String message) {
        try {
            session.sendSync(objectMapper.writeValueAsString(Map.of(
                    "type", "error",
                    "code", code,
                    "message", message
            )));
        } catch (Exception e) {
            log.error("Error sending pre-connection error to session: {}", e.getMessage());
        }
        session.close();
    }

    @OnMessage
    public void onMessage(byte[] message, WebSocketSession session) {
        Player player = session.get(SESSION_KEY, Player.class).orElse(null);

        if (player == null) {
            return;
        }

        RTSGameManager game = session.get("rtsGame", RTSGameManager.class).orElse(null);
        int playerId = player.getPlayerId();

        if (game == null) {
            log.warn("Received message from session without RTS game context. Closing.");
            session.close();
            return;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(message);
            String type = rootNode.path("type").asText("rtsInput");

            switch (type) {
                case "ping":
                    game.send(session, Map.of("type", "pong"));
                    break;
                case "rtsInput":
                    RTSPlayerInput input = objectMapper.treeToValue(rootNode, RTSPlayerInput.class);
                    game.acceptPlayerInput(playerId, input);
                    break;
                default:
                    log.warn("Received unknown message type '{}' from player {}", type, playerId);
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing message from player {}: {}", playerId, e.getMessage());
        }
    }

    @OnClose
    public void onClose(WebSocketSession session) {
        log.info("RTS WebSocket connection closed for session: {}", session.getId());
        connectionService.disconnectPlayer(session);
    }
}

