package com.fullsteam.controller;

import com.fullsteam.RTSLobby;
import com.fullsteam.model.MatchmakingJoinRequest;
import com.fullsteam.model.UnitType;
import io.micronaut.context.annotation.Context;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.server.types.files.StreamedFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Vector2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Singleton
@Controller
public class GameController {

    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    private final RTSLobby rtsLobby;
    private final ResourceResolver resourceResolver;

    @Inject
    public GameController(RTSLobby rtsLobby,
                          ResourceResolver resourceResolver) {
        this.rtsLobby = rtsLobby;
        this.resourceResolver = resourceResolver;
    }

    @Get("/api/rts/lobby")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getRTSLobby() {
        Map<String, Object> lobbyInfo = new HashMap<>();
        lobbyInfo.put("playerCount", rtsLobby.getGlobalPlayerCount());

        List<Map<String, Object>> matchmakingGames = rtsLobby.getMatchmakingGames()
                .stream()
                .filter(g -> g.getCurrentPlayers() < g.getMaxPlayers())
                .map(game -> {
                    Map<String, Object> gameInfo = new HashMap<>();
                    gameInfo.put("gameId", game.getGameId());
                    gameInfo.put("currentPlayers", game.getCurrentPlayers());
                    gameInfo.put("maxPlayers", game.getMaxPlayers());
                    gameInfo.put("totalSlots", game.getTotalSkirmishSlots());
                    gameInfo.put("mapTeamCount", game.getMapTeamCount());
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
    public Map<String, String> joinMatchmaking(@Body MatchmakingJoinRequest request) {
        try {
            MatchmakingJoinRequest body = request != null ? request : new MatchmakingJoinRequest();
            Map<String, String> result = rtsLobby.joinMatchmaking(body);
            result.put("status", "joined");
            return result;
        } catch (IllegalArgumentException e) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error joining matchmaking", e);
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
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
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to leave matchmaking: " + e.getMessage());
        }
    }

    @Get("/api/rts/matchmaking/status/{gameId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getMatchmakingStatus(String gameId) {
        RTSLobby.MatchmakingGame game = rtsLobby.getMatchmakingGame(gameId);
        if (game == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND,
                    "Matchmaking game not found");
        }

        Map<String, Object> status = new HashMap<>();
        status.put("gameId", game.getGameId());
        status.put("currentPlayers", game.getCurrentPlayers());
        status.put("maxPlayers", game.getMaxPlayers());
        status.put("totalSlots", game.getTotalSkirmishSlots());
        status.put("mapTeamCount", game.getMapTeamCount());
        status.put("ready", game.isReady());
        status.put("createdTime", game.getCreatedTime());
        return status;
    }

    /**
     * Returns each {@link UnitType}'s raw fixture data (pre-rotation) plus key metadata so a
     * static page (e.g. {@code units-preview.html}) can render every unit's body shapes
     * exactly as designed in {@link UnitType#createPhysicsFixtures()}.
     *
     * <p>The {@code shapes} field uses the same shorthand wire format the live game uses:
     * fixtures separated by ";", vertices separated by "/" as "(x,y)" tuples; a single
     * "(cx,cy,r)" tuple encodes a circle.
     */
    @Get("/api/rts/units/preview-data")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getUnitsPreviewData() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (UnitType type : UnitType.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", type.name());
            entry.put("displayName", type.getDisplayName());
            entry.put("description", type.getDescription());
            entry.put("category", type.getCategory().name());
            entry.put("armorType", type.getArmorType().name());
            entry.put("elevation", type.getElevation().name());
            entry.put("color", type.getColor());
            entry.put("size", type.getSize());
            entry.put("maxHealth", (int) type.getMaxHealth());
            entry.put("damage", (int) type.getDamage());
            entry.put("range", (int) type.getAttackRange());
            entry.put("visionRange", type.getVisionRange());
            entry.put("speed", type.getMovementSpeed());
            List<Convex> fixtures = type.createPhysicsFixtures();
            entry.put("fixtureCount", fixtures.size());
            entry.put("shapes", shapesShorthand(fixtures));
            result.add(entry);
        }
        return result;
    }

    private static final DecimalFormat PREVIEW_SHORTFORM = new DecimalFormat("#.##");

    /**
     * Mirrors {@code RTSGameManager#verticesShorthand} but operates directly on a fixture list so
     * the preview endpoint doesn't need to instantiate a dyn4j {@code Body}.
     */
    private static String shapesShorthand(List<Convex> fixtures) {
        if (fixtures.isEmpty()) {
            return "";
        }
        StringJoiner outer = new StringJoiner(";");
        for (Convex convex : fixtures) {
            StringJoiner joiner = new StringJoiner("/");
            if (convex instanceof Polygon polygon) {
                for (Vector2 v : polygon.getVertices()) {
                    joiner.add("(" + PREVIEW_SHORTFORM.format(v.x) + "," + PREVIEW_SHORTFORM.format(v.y) + ")");
                }
            } else if (convex instanceof Circle circle) {
                Vector2 c = circle.getCenter();
                joiner.add("(" + PREVIEW_SHORTFORM.format(c.x) + "," + PREVIEW_SHORTFORM.format(c.y)
                        + "," + PREVIEW_SHORTFORM.format(circle.getRadius()) + ")");
            }
            outer.add(joiner.toString());
        }
        return outer.toString();
    }

    @Get(uris = {
            "/",
            "/index.html",
            "/rts.html",
            "/rts-lobby.html",
            "/units-preview.html",
            "/js/rts/{file}",
            "/css/{file}",
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
