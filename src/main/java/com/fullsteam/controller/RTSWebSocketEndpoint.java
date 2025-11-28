package com.fullsteam.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullsteam.model.PlayerSession;
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
        
        // Extract session token from query parameters
        // Query parameters are in the URI, not in URI variables (which are path variables)
        String sessionToken = null;
        String uri = session.getRequestURI().toString();
        log.info("DEBUG: Full WebSocket URI: {}", uri);
        
        // Parse query string manually
        if (uri.contains("?sessionToken=")) {
            int startIndex = uri.indexOf("?sessionToken=") + "?sessionToken=".length();
            int endIndex = uri.indexOf("&", startIndex);
            if (endIndex == -1) {
                sessionToken = uri.substring(startIndex);
            } else {
                sessionToken = uri.substring(startIndex, endIndex);
            }
        }
        
        log.info("DEBUG: Extracted session token from query string: {}", sessionToken);
        
        if (!connectionService.connectPlayer(session, gameId, sessionToken)) {
            log.warn("Failed to connect player to RTS game {}, closing session", gameId);
            session.close();
        } else {
            log.info("Player successfully connected to RTS game {} with session token {}", gameId, sessionToken);
        }
    }
    
    @OnMessage
    public void onMessage(byte[] message, WebSocketSession session) {
        PlayerSession playerSession = session.get(SESSION_KEY, PlayerSession.class).orElse(null);
        
        if (playerSession == null) {
            return;
        }
        
        RTSGameManager game = session.get("rtsGame", RTSGameManager.class).orElse(null);
        int playerId = playerSession.getPlayerId();
        
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

