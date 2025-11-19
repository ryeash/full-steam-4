package com.fullsteam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullsteam.model.Biome;
import com.fullsteam.model.GameConfig;
import com.fullsteam.model.ObstacleDensity;
import com.fullsteam.model.RTSGameManager;
import com.fullsteam.util.GameConstants;
import com.fullsteam.util.IdGenerator;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
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

        // Start cleanup thread
        Thread cleanupThread = new Thread(this::runCleanupLoop, "RTS-Cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
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
     * @return the game ID
     */
    public synchronized String joinMatchmaking(String biome, String obstacleDensity, String faction) {
        // Try to find an existing game waiting for players
        MatchmakingGame availableGame = matchmakingGames.values().stream()
                .filter(game -> game.getCurrentPlayers() < game.getMaxPlayers())
                .findFirst()
                .orElse(null);

        if (availableGame != null) {
            availableGame.incrementPlayers(faction);
            log.info("Player joined existing matchmaking game: {}, players: {}/{}, faction: {}",
                    availableGame.getGameId(), availableGame.getCurrentPlayers(), availableGame.getMaxPlayers(), faction);
            return availableGame.getGameId();
        }

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

        // Create a new matchmaking game with selected configuration
        GameConfig config = GameConfig.builder()
                .maxPlayers(2)  // 1v1 for now
                .worldWidth(3000)
                .worldHeight(3000)
                .biome(selectedBiome)
                .obstacleDensity(selectedDensity)
                .build();

        RTSGameManager game = createGameWithConfig(config);
        MatchmakingGame matchmakingGame = new MatchmakingGame(game.getGameId(), 2);
        matchmakingGame.incrementPlayers(faction);
        matchmakingGames.put(game.getGameId(), matchmakingGame);

        log.info("Created new matchmaking game: {} with biome {}, density {}, faction {}",
                game.getGameId(), selectedBiome, selectedDensity, faction);
        return game.getGameId();
    }

    /**
     * Leave a matchmaking game
     */
    public synchronized void leaveMatchmaking(String gameId) {
        MatchmakingGame game = matchmakingGames.get(gameId);
        if (game != null) {
            game.decrementPlayers();
            log.info("Player left matchmaking game: {}, players: {}/{}",
                    gameId, game.getCurrentPlayers(), game.getMaxPlayers());

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
     */
    public void createDebugMatchmakingEntry(String gameId, String faction) {
        MatchmakingGame matchmakingGame = new MatchmakingGame(gameId, 1); // Single player debug game
        matchmakingGame.incrementPlayers(faction);
        matchmakingGames.put(gameId, matchmakingGame);
        log.info("Created debug matchmaking entry for game {} with faction {}", gameId, faction);
    }

    /**
     * Inner class to track matchmaking game state
     */
    public static class MatchmakingGame {
        private final String gameId;
        private final int maxPlayers;
        private int currentPlayers;
        private final long createdTime;
        private final List<String> playerFactions = new ArrayList<>(); // Faction selection per player slot

        public MatchmakingGame(String gameId, int maxPlayers) {
            this.gameId = gameId;
            this.maxPlayers = maxPlayers;
            this.currentPlayers = 0;
            this.createdTime = System.currentTimeMillis();
        }

        public synchronized int incrementPlayers(String faction) {
            int slot = currentPlayers;
            playerFactions.add(faction != null ? faction : "TERRAN");
            currentPlayers++;
            return slot;
        }

        public synchronized void decrementPlayers() {
            if (currentPlayers > 0) {
                currentPlayers--;
                if (!playerFactions.isEmpty()) {
                    playerFactions.remove(playerFactions.size() - 1);
                }
            }
        }
        
        public synchronized String getFactionForSlot(int slot) {
            if (slot >= 0 && slot < playerFactions.size()) {
                return playerFactions.get(slot);
            }
            return "TERRAN";
        }

        public String getGameId() {
            return gameId;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public int getCurrentPlayers() {
            return currentPlayers;
        }

        public long getCreatedTime() {
            return createdTime;
        }

        public boolean isReady() {
            return currentPlayers >= maxPlayers;
        }
    }

    /**
     * Background cleanup loop to remove finished games
     */
    private void runCleanupLoop() {
        while (true) {
            try {
                Thread.sleep(30000); // Check every 30 seconds
                cleanupFinishedGames();
            } catch (InterruptedException e) {
                log.warn("Cleanup thread interrupted", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in cleanup loop", e);
            }
        }
    }

    /**
     * Remove games that are finished (gameOver = true) or have no players
     */
    private void cleanupFinishedGames() {
        int removedCount = 0;

        // Remove finished games from active games
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, RTSGameManager> entry : activeGames.entrySet()) {
            RTSGameManager game = entry.getValue();

            // Remove if game is over or has been running for too long without players
            if (game.isGameOver()) {
                toRemove.add(entry.getKey());
                log.info("Removing finished game: {}", entry.getKey());
            } else if (game.getPlayerFactions().isEmpty() &&
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
    }
}


