package com.fullsteam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullsteam.RTSLobby;
import com.fullsteam.games.IdGenerator;
import com.fullsteam.model.PlayerSession;
import com.fullsteam.model.RTSGameManager;
import io.micronaut.websocket.WebSocketSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing RTS player connections.
 */
@Singleton
public class RTSPlayerConnectionService {
    private static final Logger log = LoggerFactory.getLogger(RTSPlayerConnectionService.class);
    public static final String SESSION_KEY = "playerSession";

    private final RTSLobby rtsLobby;
    private final ObjectMapper objectMapper;

    @Inject
    public RTSPlayerConnectionService(RTSLobby rtsLobby, ObjectMapper objectMapper) {
        this.rtsLobby = rtsLobby;
        this.objectMapper = objectMapper;
    }

    /**
     * Connect a player to an RTS game
     *
     * @param session      The WebSocket session
     * @param gameId       The game ID
     * @param sessionToken The session token from matchmaking (optional, for tracking faction)
     */
    public boolean connectPlayer(WebSocketSession session, String gameId, String sessionToken) {
        RTSGameManager game = rtsLobby.getGame(gameId);

        if (game == null) {
            log.warn("Attempted to connect to non-existent RTS game: {}", gameId);
            return false;
        }

        // Get faction selection from matchmaking game using session token
        RTSLobby.MatchmakingGame matchmakingGame = rtsLobby.getMatchmakingGame(gameId);
        String factionName = "TERRAN"; // Default

        if (matchmakingGame != null && sessionToken != null) {
            // Use session token to get the correct faction
            factionName = matchmakingGame.getFactionForSession(sessionToken);
            matchmakingGame.markSessionConnected(sessionToken);
            log.info("Retrieved faction {} for session token {}", factionName, sessionToken);
        } else {
            log.warn("No matchmaking game found for gameId {}, defaulting to TERRAN", gameId);
        }

        // Create player session
        int playerId = IdGenerator.nextPlayerId();

        PlayerSession playerSession = new PlayerSession(playerId, session, "Player" + playerId);

        // Store game reference in session attributes
        session.put("rtsGame", game);

        // Add player to game with faction
        if (!game.addPlayer(playerSession, factionName)) {
            log.warn("Failed to add player {} to RTS game {} (game may be full or started)", playerId, gameId);

            // Send error message to player
            try {
                session.sendSync(objectMapper.writeValueAsString(java.util.Map.of(
                        "type", "error",
                        "message", "Cannot join game - game is full or has already started with full roster"
                )));
            } catch (Exception e) {
                log.error("Error sending join failure message", e);
            }

            return false;
        }

        // Store session
        session.put(SESSION_KEY, playerSession);

        // Send player their ID
        game.send(session, java.util.Map.of(
                "type", "playerId",
                "playerId", playerId
        ));

        rtsLobby.incrementPlayerCount();

        log.info("Player {} connected to RTS game {} with faction {}", playerId, gameId, factionName);
        return true;
    }

    /**
     * Disconnect a player from their game
     */
    public void disconnectPlayer(WebSocketSession session) {
        PlayerSession playerSession = session.get(SESSION_KEY, PlayerSession.class).orElse(null);

        if (playerSession == null) {
            return;
        }

        RTSGameManager game = session.get("rtsGame", RTSGameManager.class).orElse(null);

        if (game != null) {
            game.removePlayer(playerSession.getPlayerId());
            rtsLobby.decrementPlayerCount();

            log.info("Player {} disconnected from RTS game", playerSession.getPlayerId());
        }
    }
}

