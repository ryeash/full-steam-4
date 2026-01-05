package com.fullsteam.controller;

import com.fullsteam.dto.FactionInfoDTO;
import com.fullsteam.dto.UnitTechTreeDTO;
import com.fullsteam.games.FactionInfoService;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.GameConfig;
import com.fullsteam.model.PlayerFaction;
import com.fullsteam.model.RTSGameManager;
import com.fullsteam.model.factions.Faction;
import io.micronaut.context.annotation.Context;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.server.types.files.StreamedFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@Controller
public class GameController {

    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    private final com.fullsteam.RTSLobby rtsLobby;
    private final ResourceResolver resourceResolver;
    private final FactionInfoService factionInfoService;

    @Inject
    public GameController(com.fullsteam.RTSLobby rtsLobby, ResourceResolver resourceResolver,
                          FactionInfoService factionInfoService) {
        this.rtsLobby = rtsLobby;
        this.resourceResolver = resourceResolver;
        this.factionInfoService = factionInfoService;
    }

    @Post("/api/rts/games")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, String> createRTSGame(@Valid @Body GameConfig gameConfig) {
        try {
            RTSGameManager game;
            if (gameConfig != null) {
                game = rtsLobby.createGameWithConfig(gameConfig);
            } else {
                game = rtsLobby.createGame();
            }

            // Create a matchmaking game entry for faction tracking (even for debug games)
            String factionName = (gameConfig != null && gameConfig.getFaction() != null)
                    ? gameConfig.getFaction() : "TERRAN";
            String sessionToken = rtsLobby.createDebugMatchmakingEntry(game.getGameId(), factionName);

            // Add AI player for debug games (when called directly, not through matchmaking)
            game.addAIPlayer();

            return Map.of(
                    "gameId", game.getGameId(),
                    "sessionToken", sessionToken,
                    "status", "created"
            );
        } catch (IllegalStateException e) {
            throw new HttpStatusException(io.micronaut.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to create RTS game: " + e.getMessage());
        }
    }

    @Get("/api/rts/lobby")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getRTSLobby() {
        Map<String, Object> lobbyInfo = new HashMap<>();
        lobbyInfo.put("playerCount", rtsLobby.getGlobalPlayerCount());

        List<Map<String, Object>> matchmakingGames = rtsLobby.getMatchmakingGames().stream()
                .map(game -> {
                    Map<String, Object> gameInfo = new HashMap<>();
                    gameInfo.put("gameId", game.getGameId());
                    gameInfo.put("currentPlayers", game.getCurrentPlayers());
                    gameInfo.put("maxPlayers", game.getMaxPlayers());
                    gameInfo.put("ready", game.isReady());
                    gameInfo.put("createdTime", game.getCreatedTime());
                    return gameInfo;
                })
                .collect(Collectors.toList());

        lobbyInfo.put("matchmakingGames", matchmakingGames);
        return lobbyInfo;
    }

    @Post("/api/rts/matchmaking/join")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> joinMatchmaking(@Body Map<String, String> config) {
        try {
            // Extract configuration from request body
            String gameId = config != null ? config.get("gameId") : null;
            String biome = config != null ? config.get("biome") : null;
            String obstacleDensity = config != null ? config.get("obstacleDensity") : null;
            String faction = config != null ? config.get("faction") : null;

            // Parse maxPlayers if provided
            Integer maxPlayers = null;
            if (config != null && config.containsKey("maxPlayers")) {
                try {
                    maxPlayers = Integer.parseInt(config.get("maxPlayers"));
                } catch (NumberFormatException e) {
                    log.warn("Invalid maxPlayers value: {}", config.get("maxPlayers"));
                }
            }

            Map<String, String> result = rtsLobby.joinMatchmaking(gameId, biome, obstacleDensity, faction, maxPlayers);
            result.put("status", "joined");
            return result;
        } catch (Exception e) {
            log.error("Error joining matchmaking", e);
            throw new HttpStatusException(io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to join matchmaking: " + e.getMessage());
        }
    }

    @Post("/api/rts/matchmaking/leave/{gameId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> leaveMatchmaking(@PathVariable String gameId, @Body Map<String, String> body) {
        try {
            String sessionToken = body != null ? body.get("sessionToken") : null;
            rtsLobby.leaveMatchmaking(gameId, sessionToken);
            return Map.of("status", "left");
        } catch (Exception e) {
            log.error("Error leaving matchmaking", e);
            throw new HttpStatusException(io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to leave matchmaking: " + e.getMessage());
        }
    }

    @Get("/api/rts/matchmaking/status/{gameId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getMatchmakingStatus(String gameId) {
        com.fullsteam.RTSLobby.MatchmakingGame game = rtsLobby.getMatchmakingGame(gameId);
        if (game == null) {
            throw new HttpStatusException(io.micronaut.http.HttpStatus.NOT_FOUND,
                    "Matchmaking game not found");
        }

        Map<String, Object> status = new HashMap<>();
        status.put("gameId", game.getGameId());
        status.put("currentPlayers", game.getCurrentPlayers());
        status.put("maxPlayers", game.getMaxPlayers());
        status.put("ready", game.isReady());
        status.put("createdTime", game.getCreatedTime());
        return status;
    }

    /**
     * Get information about all available factions
     */
    @Get("/api/rts/factions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FactionInfoDTO> getAllFactions() {
        try {
            return factionInfoService.getAllFactions();
        } catch (Exception e) {
            log.error("Error fetching faction info", e);
            throw new HttpStatusException(io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch faction information: " + e.getMessage());
        }
    }

    /**
     * Get detailed information about a specific faction
     */
    @Get("/api/rts/factions/{factionName}")
    @Produces(MediaType.APPLICATION_JSON)
    public FactionInfoDTO getFactionInfo(@PathVariable String factionName) {
        try {
            log.info("Fetching faction info for: {}", factionName);
            Faction faction = Faction.valueOf(factionName.toUpperCase());
            FactionInfoDTO result = factionInfoService.getFactionInfo(faction);
            log.info("Returning faction info with {} units and {} buildings",
                    result.getAvailableUnits().size(),
                    result.getAvailableBuildings().size());

            // Log HEADQUARTERS info
            result.getAvailableBuildings().stream()
                    .filter(b -> "HEADQUARTERS".equals(b.getBuildingType()))
                    .findFirst()
                    .ifPresent(hq -> log.info("HEADQUARTERS found for faction {}", factionName));

            return result;
        } catch (IllegalArgumentException e) {
            throw new HttpStatusException(io.micronaut.http.HttpStatus.NOT_FOUND,
                    "Faction not found: " + factionName);
        } catch (Exception e) {
            log.error("Error fetching faction info for {}", factionName, e);
            throw new HttpStatusException(io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch faction information: " + e.getMessage());
        }
    }

    /**
     * Get unit tech tree for a specific player
     */
    @Get("/api/rts/games/{gameId}/players/{playerId}/tech-tree")
    @Produces(MediaType.APPLICATION_JSON)
    public UnitTechTreeDTO getPlayerTechTree(
            @PathVariable String gameId, 
            @PathVariable int playerId) {
        try {
            RTSGameManager game = rtsLobby.getGame(gameId);
            if (game == null) {
                throw new HttpStatusException(io.micronaut.http.HttpStatus.NOT_FOUND,
                        "Game not found: " + gameId);
            }

            PlayerFaction faction = game.getGameEntities().getPlayerFactions().get(playerId);
            if (faction == null) {
                throw new HttpStatusException(io.micronaut.http.HttpStatus.NOT_FOUND,
                        "Player not found: " + playerId);
            }

            if (faction.getResearchManager() == null) {
                throw new HttpStatusException(io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR,
                        "Research manager not initialized for player");
            }

            // Get player's buildings for tier validation
            java.util.Set<BuildingType> playerBuildings = game.getGameEntities().getBuildings().values().stream()
                    .filter(b -> b.belongsTo(playerId))
                    .filter(b -> b.isActive() && !b.isUnderConstruction())
                    .map(b -> b.getBuildingType())
                    .collect(java.util.stream.Collectors.toSet());

            // Build DTO with player state
            UnitTechTreeDTO dto = UnitTechTreeDTO.fromTechTree(
                    faction.getResearchManager().getUnitTechTree(),
                    faction.getResearchManager(),
                    playerBuildings
            );

            return dto;
        } catch (HttpStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching tech tree for player {} in game {}", playerId, gameId, e);
            throw new HttpStatusException(io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch tech tree: " + e.getMessage());
        }
    }

    /**
     * Start unit research
     */
    @Post("/api/rts/games/{gameId}/research/start")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, Object> startUnitResearch(
            @PathVariable String gameId,
            @Body Map<String, Object> request) {
        try {
            RTSGameManager game = rtsLobby.getGame(gameId);
            if (game == null) {
                throw new HttpStatusException(io.micronaut.http.HttpStatus.NOT_FOUND,
                        "Game not found: " + gameId);
            }

            int playerId = (int) request.get("playerId");
            String researchId = (String) request.get("researchId");
            int buildingId = (int) request.get("buildingId");

            // Call game manager to handle research
            game.handleStartUnitResearch(playerId, researchId, buildingId);

            return Map.of(
                    "success", true,
                    "message", "Research started",
                    "researchId", researchId
            );
        } catch (HttpStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error starting research in game {}", gameId, e);
            throw new HttpStatusException(io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to start research: " + e.getMessage());
        }
    }

    /**
     * Cancel unit research
     */
    @Post("/api/rts/games/{gameId}/research/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, String> cancelUnitResearch(
            @PathVariable String gameId,
            @Body Map<String, Object> request) {
        try {
            RTSGameManager game = rtsLobby.getGame(gameId);
            if (game == null) {
                throw new HttpStatusException(io.micronaut.http.HttpStatus.NOT_FOUND,
                        "Game not found: " + gameId);
            }

            int playerId = (int) request.get("playerId");
            int buildingId = (int) request.get("buildingId");

            // Call game manager to handle cancellation
            game.handleCancelUnitResearch(playerId, buildingId);

            return Map.of(
                    "success", "true",
                    "message", "Research cancelled"
            );
        } catch (HttpStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error cancelling research in game {}", gameId, e);
            throw new HttpStatusException(io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to cancel research: " + e.getMessage());
        }
    }

    @Get(uris = {
            "/",
            "/index.html",
            "/rts.html",
            "/rts-lobby.html",
            "/js/rts/{file}",
            "/unified.css",
            "/favicon.ico",
            "/robots.txt"
    }, produces = MediaType.ALL)
    public HttpResponse<StreamedFile> staticFiles(@Context HttpRequest<?> request) {
        String path = request.getPath();
        if (path.equals("/")) {
            path = "index.html";
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        String extension = StringUtils.substringAfter(path, '.');
        MediaType type = MediaType.forExtension(extension)
                .orElse(MediaType.TEXT_HTML_TYPE);

        return serveStaticFile(path, type);
    }

    private HttpResponse<StreamedFile> serveStaticFile(String path, MediaType contentType) {
        try {
            Optional<URL> resource = resourceResolver.getResource("classpath:" + path);
            if (resource.isPresent()) {
                InputStream inputStream = resource.get().openStream();
                return HttpResponse.ok(new StreamedFile(inputStream, contentType));
            } else {
                log.warn("Resource not found: {}", path);
                return HttpResponse.notFound();
            }
        } catch (Exception e) {
            log.error("Error serving static file: {}", path, e);
            return HttpResponse.serverError();
        }
    }
}
