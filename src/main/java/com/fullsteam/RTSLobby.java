package com.fullsteam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullsteam.games.GameConstants;
import com.fullsteam.games.IdGenerator;
import com.fullsteam.model.Biome;
import com.fullsteam.model.GameConfig;
import com.fullsteam.model.ObstacleDensity;
import com.fullsteam.model.RTSGameManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lobby for managing RTS games and matchmaking.
 */
@Singleton
public class RTSLobby {
    private static final Logger log = LoggerFactory.getLogger(RTSLobby.class);

    private final Map<String, RTSGameManager> activeGames = new ConcurrentSkipListMap<>();
    private final Map<String, MatchmakingGame> matchmakingGames = new ConcurrentSkipListMap<>();
    private final AtomicLong globalPlayerCount = new AtomicLong(0);
    private final ObjectMapper objectMapper;

    @Inject
    public RTSLobby(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        GameConstants.EXECUTOR.scheduleAtFixedRate(this::cleanupFinishedGames, 5, 5, TimeUnit.SECONDS);
    }

    public long getGlobalPlayerCount() {
        return globalPlayerCount.get();
    }

    /**
     * Create a new RTS game with default configuration
     */
    public RTSGameManager createGame() {
        return createGameWithConfig(GameConfig.builder()
                .maxPlayers(4) // 4 players for RTS
                .worldHeight(4000)
                .worldWidth(4000)
                .build());
    }

    /**
     * Create a new RTS game with custom configuration
     */
    public RTSGameManager createGameWithConfig(GameConfig gameConfig) {
        if (activeGames.size() >= GameConstants.MAX_GLOBAL_GAMES) {
            throw new IllegalStateException("Maximum number of RTS games reached");
        }

        String gameId = IdGenerator.nextGameId();
        RTSGameManager game = new RTSGameManager(gameId, gameConfig, objectMapper);
        activeGames.put(gameId, game);

        log.info("Created new RTS game: {} with config: maxPlayers={}, world={}x{}",
                gameId, gameConfig.getMaxPlayers(), gameConfig.getWorldWidth(), gameConfig.getWorldHeight());

        return game;
    }

    /**
     * Get an RTS game by ID
     */
    public RTSGameManager getGame(String gameId) {
        return activeGames.get(gameId);
    }

    /**
     * Remove an RTS game
     */
    public void removeGame(String gameId) {
        RTSGameManager removed = activeGames.remove(gameId);
        if (removed != null) {
            log.info("Removed RTS game: {}", gameId);
            removed.shutdown();
        }
    }

    public void incrementPlayerCount() {
        globalPlayerCount.incrementAndGet();
    }

    public void decrementPlayerCount() {
        globalPlayerCount.decrementAndGet();
    }

    /**
     * Join or create a matchmaking game
     *
     * @param gameId          Optional - if provided, join this specific game
     * @param biome           The map biome
     * @param obstacleDensity The obstacle density
     * @param faction         The player's selected faction
     * @param maxPlayers      Optional - if creating a new game, the max players (default 2)
     * @return Map containing gameId and sessionToken
     */
    public synchronized Map<String, String> joinMatchmaking(String gameId, String biome, String obstacleDensity,
                                                            String faction, Integer maxPlayers) {
        Map<String, String> map = new HashMap<>();

        // If gameId is specified, join that specific game
        if (gameId != null && !gameId.isEmpty()) {
            MatchmakingGame specificGame = matchmakingGames.get(gameId);
            if (specificGame != null && specificGame.getCurrentPlayers() < specificGame.getMaxPlayers()) {
                String sessionToken = specificGame.reserveSlot(faction);
                if (sessionToken != null) {
                    log.info("Player joined specific game: {}, players: {}/{}, faction: {}, session: {}",
                            gameId, specificGame.getCurrentPlayers(),
                            specificGame.getMaxPlayers(), faction, sessionToken);
                    map.put("gameId", specificGame.getGameId());
                    map.put("sessionToken", sessionToken);
                    return map;
                } else {
                    throw new IllegalStateException("Game is full or unable to join");
                }
            } else {
                throw new IllegalArgumentException("Game not found or is full");
            }
        }

        // Otherwise, try to find an existing game waiting for players with matching settings
        // (For now, we skip auto-matching and just create a new game)
        // In future, we could match based on biome/density/playerCount

        // Parse biome (default to GRASSLAND if not provided or invalid)
        Biome selectedBiome = Biome.GRASSLAND;
        if (biome != null) {
            try {
                selectedBiome = Biome.valueOf(biome);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid biome '{}', defaulting to GRASSLAND", biome);
            }
        }

        // Parse obstacle density (default to MEDIUM if not provided or invalid)
        ObstacleDensity selectedDensity = ObstacleDensity.MEDIUM;
        if (obstacleDensity != null) {
            try {
                selectedDensity = ObstacleDensity.valueOf(obstacleDensity);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid obstacle density '{}', defaulting to MEDIUM", obstacleDensity);
            }
        }

        // Determine max players (default to 2 if not specified, max 4)
        int players = (maxPlayers != null && maxPlayers >= 2 && maxPlayers <= 4) ? maxPlayers : 2;

        // Determine world size based on player count
        int worldSize = calculateWorldSize(players);

        // Create a new matchmaking game with selected configuration
        GameConfig config = GameConfig.builder()
                .maxPlayers(players)
                .worldWidth(worldSize)
                .worldHeight(worldSize)
                .biome(selectedBiome)
                .obstacleDensity(selectedDensity)
                .build();

        RTSGameManager game = createGameWithConfig(config);
        MatchmakingGame matchmakingGame = new MatchmakingGame(game.getGameId(), players);
        String sessionToken = matchmakingGame.reserveSlot(faction);
        matchmakingGames.put(game.getGameId(), matchmakingGame);

        log.info("Created new matchmaking game: {} with biome {}, density {}, maxPlayers {}, faction {}, session: {}",
                game.getGameId(), selectedBiome, selectedDensity, players, faction, sessionToken);
        map.put("gameId", game.getGameId());
        map.put("sessionToken", sessionToken);
        return map;
    }

    /**
     * Calculate appropriate world size based on player count
     */
    private int calculateWorldSize(int playerCount) {
        if (playerCount <= 2) return 3000;
        if (playerCount <= 3) return 3500;
        return 4000; // 4 players
    }

    /**
     * Leave a matchmaking game
     */
    public synchronized void leaveMatchmaking(String gameId, String sessionToken) {
        MatchmakingGame game = matchmakingGames.get(gameId);
        if (game != null) {
            game.releaseSlot(sessionToken);
            log.info("Player left matchmaking game: {}, session: {}, players: {}/{}",
                    gameId, sessionToken, game.getCurrentPlayers(), game.getMaxPlayers());

            // If no players left, remove the game
            if (game.getCurrentPlayers() <= 0) {
                matchmakingGames.remove(gameId);
                removeGame(gameId);
                log.info("Removed empty matchmaking game: {}", gameId);
            }
        }
    }

    /**
     * Get matchmaking game status
     */
    public MatchmakingGame getMatchmakingGame(String gameId) {
        return matchmakingGames.get(gameId);
    }

    /**
     * Get all matchmaking games
     */
    public List<MatchmakingGame> getMatchmakingGames() {
        return new ArrayList<>(matchmakingGames.values());
    }

    /**
     * Check if a matchmaking game is ready to start
     */
    public boolean isGameReady(String gameId) {
        MatchmakingGame game = matchmakingGames.get(gameId);
        return game != null && game.getCurrentPlayers() >= game.getMaxPlayers();
    }

    /**
     * Create a matchmaking entry for a debug game (to track faction selection)
     *
     * @return The session token for the player
     */
    public String createDebugMatchmakingEntry(String gameId, String faction) {
        MatchmakingGame matchmakingGame = new MatchmakingGame(gameId, 1); // Single player debug game
        String sessionToken = matchmakingGame.reserveSlot(faction);
        matchmakingGames.put(gameId, matchmakingGame);
        log.info("Created debug matchmaking entry for game {} with faction {} and session token {}",
                gameId, faction, sessionToken);
        return sessionToken;
    }

    /**
     * Inner class to track matchmaking game state
     */
    public static class MatchmakingGame {
        @Getter
        private final String gameId;
        @Getter
        private final int maxPlayers;
        @Getter
        private int currentPlayers;
        @Getter
        private final long createdTime;

        // Map session tokens to faction selections (ensures correct faction assignment)
        private final Map<String, String> sessionTokenToFaction = new ConcurrentSkipListMap<>();
        private final Map<String, Integer> sessionTokenToSlot = new ConcurrentSkipListMap<>();

        public MatchmakingGame(String gameId, int maxPlayers) {
            this.gameId = gameId;
            this.maxPlayers = maxPlayers;
            this.currentPlayers = 0;
            this.createdTime = System.currentTimeMillis();
        }

        /**
         * Reserve a slot for a player and return a unique session token
         *
         * @param faction The faction the player selected
         * @return A unique session token for this player
         */
        public synchronized String reserveSlot(String faction) {
            if (currentPlayers >= maxPlayers) {
                return null; // Game is full
            }

            // Generate unique session token
            String sessionToken = IdGenerator.nextGameId(); // Reuse game ID generator for uniqueness
            int slot = currentPlayers;

            sessionTokenToFaction.put(sessionToken, faction != null ? faction : "TERRAN");
            sessionTokenToSlot.put(sessionToken, slot);
            currentPlayers++;

            log.info("Reserved slot {} for session {} with faction {}", slot, sessionToken, faction);
            return sessionToken;
        }

        /**
         * Release a reserved slot (when player leaves before connecting)
         */
        public synchronized void releaseSlot(String sessionToken) {
            if (sessionToken != null && sessionTokenToFaction.containsKey(sessionToken)) {
                sessionTokenToFaction.remove(sessionToken);
                sessionTokenToSlot.remove(sessionToken);
                if (currentPlayers > 0) {
                    currentPlayers--;
                }
                log.info("Released slot for session {}", sessionToken);
            }
        }

        /**
         * Get faction for a specific session token
         */
        public synchronized String getFactionForSession(String sessionToken) {
            String faction = sessionTokenToFaction.get(sessionToken);
            if (faction != null) {
                log.info("Retrieved faction {} for session {}", faction, sessionToken);
                return faction;
            }
            log.warn("No faction found for session {}, defaulting to TERRAN", sessionToken);
            return "TERRAN";
        }

        /**
         * Get slot number for a specific session token
         */
        public synchronized Integer getSlotForSession(String sessionToken) {
            return sessionTokenToSlot.get(sessionToken);
        }

        /**
         * Mark a session as connected (consumed)
         */
        public synchronized void markSessionConnected(String sessionToken) {
            // Keep the mapping for now, but we could add a "connected" flag if needed
            log.info("Session {} connected to game", sessionToken);
        }

        public boolean isReady() {
            return currentPlayers >= maxPlayers;
        }

        /**
         * Legacy method for backward compatibility (used by debug games)
         */
        @Deprecated
        public synchronized int incrementPlayers(String faction) {
            String token = reserveSlot(faction);
            return token != null ? sessionTokenToSlot.get(token) : -1;
        }
    }

    /**
     * Remove games that are finished (gameOver = true) or have no players
     */
    private void cleanupFinishedGames() {
        try {
            int removedCount = 0;

            // Remove finished games from active games
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, RTSGameManager> entry : activeGames.entrySet()) {
                RTSGameManager game = entry.getValue();

                // Remove if game is over or has been running for too long without players
                if (game.isGameOver()) {
                    toRemove.add(entry.getKey());
                    log.info("Removing finished game: {}", entry.getKey());
                } else if (game.getGameEntities().getPlayerFactions().isEmpty() &&
                        System.currentTimeMillis() - game.getGameStartTime() > 300000) { // 5 minutes
                    toRemove.add(entry.getKey());
                    log.info("Removing abandoned game: {}", entry.getKey());
                }
            }

            for (String gameId : toRemove) {
                RTSGameManager game = activeGames.remove(gameId);
                if (game != null) {
                    game.stopGame();
                    removedCount++;
                }
            }

            // Remove old matchmaking games that never filled
            List<String> oldMatchmakingGames = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (Map.Entry<String, MatchmakingGame> entry : matchmakingGames.entrySet()) {
                MatchmakingGame mmGame = entry.getValue();
                // Remove if older than 10 minutes and not full
                if (now - mmGame.getCreatedTime() > 600000 && !mmGame.isReady()) {
                    oldMatchmakingGames.add(entry.getKey());
                    log.info("Removing stale matchmaking game: {}", entry.getKey());
                }
            }

            for (String gameId : oldMatchmakingGames) {
                matchmakingGames.remove(gameId);
                RTSGameManager game = activeGames.remove(gameId);
                if (game != null) {
                    game.stopGame();
                    removedCount++;
                }
            }

            if (removedCount > 0) {
                log.info("Cleanup completed: removed {} games. Active games: {}, Matchmaking games: {}",
                        removedCount, activeGames.size(), matchmakingGames.size());
            }
        } catch (Throwable t) {
            log.error("error cleaning up inactive games", t);
        }
    }
}


