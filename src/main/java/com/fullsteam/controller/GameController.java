package com.fullsteam.controller;

import com.fullsteam.dto.FactionInfoDTO;
import com.fullsteam.model.factions.Faction;
import com.fullsteam.model.RTSGameManager;
import com.fullsteam.model.GameConfig;
import com.fullsteam.games.FactionInfoService;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.server.types.files.StreamedFile;
import io.micronaut.context.annotation.Context;
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
            rtsLobby.createDebugMatchmakingEntry(game.getGameId(), factionName);
            
            // Add AI player for debug games (when called directly, not through matchmaking)
            game.addAIPlayer();
            
            return Map.of(
                    "gameId", game.getGameId(),
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
            String biome = config != null ? config.get("biome") : null;
            String obstacleDensity = config != null ? config.get("obstacleDensity") : null;
            String faction = config != null ? config.get("faction") : null;
            
            Map<String, String> result = rtsLobby.joinMatchmaking(biome, obstacleDensity, faction);
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
            
            // Log HEADQUARTERS specifically
            result.getAvailableBuildings().stream()
                .filter(b -> "HEADQUARTERS".equals(b.getBuildingType()))
                .findFirst()
                .ifPresent(hq -> log.info("HEADQUARTERS produces: {}", hq.getProducedUnits()));
            
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
