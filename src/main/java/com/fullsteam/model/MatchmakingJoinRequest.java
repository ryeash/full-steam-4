package com.fullsteam.model;

import io.micronaut.core.annotation.Introspected;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * JSON body for {@code POST /api/rts/matchmaking/join}: create a new skirmish lobby or join an existing one.
 * <p>
 * When {@code skirmishSlots} is omitted or empty, the server uses a default 2-player FFA human roster
 * ({@link SkirmishMatchConfig#defaultFfaHumanSlots(int)} with count 2).
 * </p>
 */
@Data
@Introspected
public class MatchmakingJoinRequest {

    /**
     * When set, join this existing matchmaking game instead of creating one.
     */
    private String gameId;

    private String biome;
    private String obstacleDensity;
    private String faction;

    private Double worldWidth;
    private Double worldHeight;

    /**
     * Full skirmish roster; when absent, a default FFA roster is applied in {@link com.fullsteam.RTSLobby}.
     */
    private List<SkirmishSlotConfig> skirmishSlots;

    /**
     * Optional map symmetry team count; when null, inferred from skirmish slots.
     */
    private Integer mapTeamCount;

    /**
     * Full faction configuration for the joining player, as a raw JSON object.
     * When present, the server validates the configuration and rejects the request
     * with HTTP 400 if the faction is invalid — preventing the player from being
     * redirected to the game page with a broken faction.
     */
    private Map<String, Object> factionConfig;
}
