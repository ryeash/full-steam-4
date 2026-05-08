package com.fullsteam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullsteam.games.GameConstants;
import com.fullsteam.games.IdGenerator;
import com.fullsteam.model.Biome;
import com.fullsteam.model.GameConfig;
import com.fullsteam.model.MatchmakingJoinRequest;
import com.fullsteam.model.ObstacleDensity;
import com.fullsteam.model.RTSGameManager;
import com.fullsteam.model.SkirmishMatchConfig;
import com.fullsteam.model.SkirmishSlotConfig;
import com.fullsteam.model.SkirmishSlotKind;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
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
                .worldHeight(4000)
                .worldWidth(4000)
                .skirmishSlots(SkirmishMatchConfig.defaultFfaHumanSlots(4))
                .build());
    }

    /**
     * Create a new RTS game with custom configuration (skirmish roster resolved and validated).
     */
    public RTSGameManager createGameWithConfig(GameConfig gameConfig) {
        if (activeGames.size() >= GameConstants.MAX_GLOBAL_GAMES) {
            throw new IllegalStateException("Maximum number of RTS games reached");
        }

        GameConfig resolved = SkirmishMatchConfig.resolve(gameConfig);
        String gameId = IdGenerator.nextGameId();
        RTSGameManager game = new RTSGameManager(gameId, resolved, objectMapper);
        activeGames.put(gameId, game);

        log.info("Created new RTS game: {} with config: humanSlots={}, totalSlots={}, mapTeams={}, world={}x{}",
                gameId, resolved.getMaxPlayers(), resolved.getTotalSlotCount(),
                resolved.getEffectiveMapTeamCount(), resolved.getWorldWidth(), resolved.getWorldHeight());

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
     * Join or create a matchmaking game from a client {@link MatchmakingJoinRequest}.
     */
    public synchronized Map<String, String> joinMatchmaking(MatchmakingJoinRequest request) {
        MatchmakingJoinRequest in = request != null ? request : new MatchmakingJoinRequest();
        String gameId = emptyToNull(in.getGameId());
        String biome = in.getBiome();
        String obstacleDensity = in.getObstacleDensity();
        String faction = in.getFaction();
        Double configuredWorldWidth = in.getWorldWidth();
        Double configuredWorldHeight = in.getWorldHeight();
        List<SkirmishSlotConfig> skirmishSlots = in.getSkirmishSlots();
        Integer mapTeamCount = in.getMapTeamCount();

        if (skirmishSlots == null || skirmishSlots.isEmpty()) {
            skirmishSlots = SkirmishMatchConfig.defaultFfaHumanSlots(2);
        }

        int rosterSizeForWorldSizing = skirmishSlots.size();

        Map<String, String> map = new HashMap<>();

        if (gameId != null) {
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

        Biome selectedBiome = Biome.GRASSLAND;
        if (biome != null) {
            try {
                selectedBiome = Biome.valueOf(biome);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid biome '{}', defaulting to GRASSLAND", biome);
            }
        }

        ObstacleDensity selectedDensity = ObstacleDensity.MEDIUM;
        if (obstacleDensity != null) {
            try {
                selectedDensity = ObstacleDensity.valueOf(obstacleDensity);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid obstacle density '{}', defaulting to MEDIUM", obstacleDensity);
            }
        }

        double worldWidth = calculateWorldSize(rosterSizeForWorldSizing);
        double worldHeight = worldWidth;
        if (configuredWorldWidth != null && configuredWorldHeight != null
                && configuredWorldWidth >= 3000.0 && configuredWorldWidth <= 10000.0
                && configuredWorldHeight >= 3000.0 && configuredWorldHeight <= 10000.0) {
            worldWidth = configuredWorldWidth;
            worldHeight = configuredWorldHeight;
        }

        GameConfig partial = GameConfig.builder()
                .worldWidth(worldWidth)
                .worldHeight(worldHeight)
                .biome(selectedBiome)
                .obstacleDensity(selectedDensity)
                .mapTeamCount(mapTeamCount)
                .skirmishSlots(skirmishSlots)
                .build();
        RTSGameManager game = createGameWithConfig(partial);
        MatchmakingGame matchmakingGame = new MatchmakingGame(game.getGameId(), game.getGameConfig());
        String sessionToken = matchmakingGame.reserveSlot(faction);
        matchmakingGames.put(game.getGameId(), matchmakingGame);

        log.info("Created new matchmaking game: {} biome {} density {} roster {} humans {}/{}, session: {}",
                game.getGameId(), selectedBiome, selectedDensity,
                SkirmishMatchConfig.describeSlots(game.getGameConfig().getSkirmishSlots()),
                matchmakingGame.getCurrentPlayers(), matchmakingGame.getMaxPlayers(), sessionToken);
        map.put("gameId", game.getGameId());
        map.put("sessionToken", sessionToken);
        return map;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
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
            if (sessionToken != null) {
                game.releaseReservation(sessionToken);
            }
            log.info("Player left matchmaking game: {}, session: {}, players: {}/{}",
                    gameId, sessionToken, game.getCurrentPlayers(), game.getMaxPlayers());

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
     * Inner class to track matchmaking game state
     */
    public static class MatchmakingGame {
        private static final Logger SLOT_LOG = LoggerFactory.getLogger(MatchmakingGame.class);

        @Getter
        private final String gameId;
        @Getter
        private final GameConfig resolvedGameConfig;
        @Getter
        private final long createdTime;
        private final List<SkirmishSlotConfig> skirmishSlots;
        private final int humanSlotsTotal;
        private final Map<String, Integer> sessionTokenToSlotIndex = new ConcurrentSkipListMap<>();
        private final Set<Integer> reservedHumanSlotIndices = new ConcurrentSkipListSet<>();

        public MatchmakingGame(String gameId, GameConfig resolvedGameConfig) {
            this.gameId = gameId;
            this.resolvedGameConfig = resolvedGameConfig;
            this.skirmishSlots = List.copyOf(resolvedGameConfig.getSkirmishSlots());
            this.humanSlotsTotal = (int) skirmishSlots.stream()
                    .filter(s -> s.getKind() == SkirmishSlotKind.HUMAN)
                    .count();
            this.createdTime = System.currentTimeMillis();
        }

        /**
         * Human reservation count (lobby seats taken).
         */
        public int getCurrentPlayers() {
            return reservedHumanSlotIndices.size();
        }

        /**
         * Human slots that must fill before the match starts.
         */
        public int getMaxPlayers() {
            return humanSlotsTotal;
        }

        public int getTotalSkirmishSlots() {
            return skirmishSlots.size();
        }

        public int getMapTeamCount() {
            return resolvedGameConfig.getEffectiveMapTeamCount();
        }

        public synchronized Optional<Integer> getSlotIndexForToken(String sessionToken) {
            if (sessionToken == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(sessionTokenToSlotIndex.get(sessionToken));
        }

        public synchronized void releaseReservation(String sessionToken) {
            Integer idx = sessionTokenToSlotIndex.remove(sessionToken);
            if (idx != null) {
                reservedHumanSlotIndices.remove(idx);
            }
        }

        /**
         * Reserve the next open human skirmish slot and return a unique session token
         */
        public synchronized String reserveSlot(String faction) {
            for (int i = 0; i < skirmishSlots.size(); i++) {
                if (skirmishSlots.get(i).getKind() != SkirmishSlotKind.HUMAN) {
                    continue;
                }
                if (reservedHumanSlotIndices.contains(i)) {
                    continue;
                }
                String sessionToken = IdGenerator.nextGameId();
                reservedHumanSlotIndices.add(i);
                sessionTokenToSlotIndex.put(sessionToken, i);
                SLOT_LOG.info("Reserved human skirmish slot {} for session {} with faction {}", i, sessionToken, faction);
                return sessionToken;
            }
            return null;
        }

        public synchronized void markSessionConnected(String sessionToken) {
            SLOT_LOG.info("Session {} connected to game", sessionToken);
        }

        /**
         * True when every human skirmish slot has a lobby reservation (AI slots never wait on joiners).
         */
        public boolean isReady() {
            return reservedHumanSlotIndices.size() >= humanSlotsTotal;
        }
    }

    /**
     * Drop stale matchmaking rows when the RTS game is gone or has ended.
     * (Previously this removed any row whose game existed in {@link #activeGames}, which broke
     * status polling as soon as a match was created.)
     */
    private void cleanupFinishedGames() {
        try {
            matchmakingGames.entrySet().removeIf(e -> {
                String gameId = e.getKey();
                RTSGameManager game = activeGames.get(gameId);
                return game == null || game.isGameOver();
            });
        } catch (Throwable t) {
            log.error("error cleaning up inactive games", t);
        }
    }
}


