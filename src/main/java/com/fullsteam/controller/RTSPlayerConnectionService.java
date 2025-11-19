package com.fullsteam.controller;

import com.fullsteam.RTSLobby;
import com.fullsteam.model.PlayerSession;
import com.fullsteam.model.RTSGameManager;
import com.fullsteam.util.IdGenerator;
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
    
    @Inject
    public RTSPlayerConnectionService(RTSLobby rtsLobby) {
        this.rtsLobby = rtsLobby;
    }
    
    /**
     * Connect a player to an RTS game
     */
    public boolean connectPlayer(WebSocketSession session, String gameId) {
        RTSGameManager game = rtsLobby.getGame(gameId);
        
        if (game == null) {
            log.warn("Attempted to connect to non-existent RTS game: {}", gameId);
            return false;
        }
        
        // Get faction selection from matchmaking game
        RTSLobby.MatchmakingGame matchmakingGame = rtsLobby.getMatchmakingGame(gameId);
        String factionName = "TERRAN"; // Default
        log.info("Looking up matchmaking game for gameId {}: found={}", gameId, matchmakingGame != null);
        if (matchmakingGame != null) {
            // Get faction for the next player slot (current player count before adding)
            int slot = game.getPlayerCount();
            factionName = matchmakingGame.getFactionForSlot(slot);
            log.info("Retrieved faction {} for player slot {} (current player count: {})", factionName, slot, slot);
        } else {
            log.warn("No matchmaking game found for gameId {}, defaulting to TERRAN", gameId);
        }
        
        // Create player session
        int playerId = IdGenerator.nextEntityId();
        
        PlayerSession playerSession = new PlayerSession(playerId, session, "Player" + playerId);
        
        // Store game reference in session attributes
        session.put("rtsGame", game);
        
        // Add player to game with faction
        if (!game.addPlayer(playerSession, factionName)) {
            log.warn("Failed to add player {} to RTS game {}", playerId, gameId);
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

