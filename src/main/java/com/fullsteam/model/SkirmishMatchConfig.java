package com.fullsteam.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds and validates skirmish roster + map symmetry settings for RTS games.
 * Total slots (human + AI) are capped at {@value #MAX_TOTAL_SLOTS}.
 */
public final class SkirmishMatchConfig {

    public static final int MAX_TOTAL_SLOTS = 4;

    private SkirmishMatchConfig() {
    }

    /**
     * Returns a defensive copy of skirmish slots with validation applied.
     * {@link GameConfig#getSkirmishSlots()} must be non-null and non-empty (build default FFA in the caller if needed).
     */
    public static GameConfig resolve(GameConfig input) {
        Objects.requireNonNull(input, "gameConfig");

        List<SkirmishSlotConfig> slots = input.getSkirmishSlots();
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("skirmishSlots must be non-null and non-empty");
        }
        slots = new ArrayList<>(slots);

        validateSlots(slots);

        int inferredMapTeams = slots.stream().mapToInt(SkirmishSlotConfig::getTeamId).max().orElse(2);
        int mapTeamCount = input.getMapTeamCount() != null ? input.getMapTeamCount() : inferredMapTeams;
        if (mapTeamCount < 2 || mapTeamCount > MAX_TOTAL_SLOTS) {
            throw new IllegalArgumentException("mapTeamCount must be between 2 and " + MAX_TOTAL_SLOTS);
        }
        if (inferredMapTeams > mapTeamCount) {
            throw new IllegalArgumentException("mapTeamCount must be >= max team id in skirmishSlots");
        }

        int humanSlots = (int) slots.stream().filter(s -> s.getKind() == SkirmishSlotKind.HUMAN).count();
        if (humanSlots < 1) {
            throw new IllegalArgumentException("Skirmish must include at least one HUMAN slot");
        }

        long distinctTeams = slots.stream().mapToInt(SkirmishSlotConfig::getTeamId).distinct().count();
        if (distinctTeams < 2) {
            throw new IllegalArgumentException("Skirmish must include at least two distinct team ids");
        }

        for (SkirmishSlotConfig s : slots) {
            if (s.getTeamId() < 1 || s.getTeamId() > mapTeamCount) {
                throw new IllegalArgumentException("teamId out of range for mapTeamCount");
            }
        }

        int maxHumans = humanSlots;
        if (maxHumans < 1 || maxHumans > MAX_TOTAL_SLOTS) {
            throw new IllegalArgumentException("Human slot count must be 1.." + MAX_TOTAL_SLOTS);
        }

        return GameConfig.builder()
                .mapTeamCount(mapTeamCount)
                .skirmishSlots(List.copyOf(slots))
                .worldWidth(input.getWorldWidth())
                .worldHeight(input.getWorldHeight())
                .biome(input.getBiome())
                .obstacleDensity(input.getObstacleDensity())
                .fogOfWarEnabled(input.isFogOfWarEnabled())
                .startingResources(input.getStartingResources())
                .faction(input.getFaction())
                .build();
    }

    /**
     * FFA roster: {@code humanCount} humans on teams 1..N (one per slot). Used when the client does not send a custom roster.
     */
    public static List<SkirmishSlotConfig> defaultFfaHumanSlots(int humanCount) {
        int n = humanCount;
        if (n < 2 || n > MAX_TOTAL_SLOTS) {
            n = 2;
        }
        List<SkirmishSlotConfig> ffa = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ffa.add(SkirmishSlotConfig.builder()
                    .kind(SkirmishSlotKind.HUMAN)
                    .teamId(i + 1)
                    .build());
        }
        return ffa;
    }

    private static void validateSlots(List<SkirmishSlotConfig> slots) {
        if (slots.size() < 2 || slots.size() > MAX_TOTAL_SLOTS) {
            throw new IllegalArgumentException("Skirmish must have between 2 and " + MAX_TOTAL_SLOTS + " slots");
        }
    }

    public static String describeSlots(List<SkirmishSlotConfig> slots) {
        return slots.stream()
                .map(s -> s.getKind().name().charAt(0) + String.valueOf(s.getTeamId()))
                .collect(Collectors.joining(","));
    }
}
