package com.fullsteam.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Represents a game event that can be broadcast to players.
 * Events can be targeted to all players, specific teams, or individual players.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameEvent {

    /**
     * The type of message for WebSocket routing
     */
    @Builder.Default
    private String type = "gameEvent";

    /**
     * The event message to display
     */
    private String message;

    /**
     * Hex color code for the message (e.g., "#FF0000" for red)
     */
    private String color;

    /**
     * Event category for styling and filtering
     */
    private EventCategory category;

    /**
     * Targeting information - who should receive this event
     */
    private EventTarget target;

    /**
     * Timestamp when the event occurred
     */
    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    /**
     * Optional duration in milliseconds for how long to display the event
     * If null or 0, uses default display duration
     */
    private Long displayDuration;

    /**
     * Categories of game events for styling and organization
     */
    @Getter
    public enum EventCategory {
        KILL("#FF4444"),           // Red for kills/deaths
        CAPTURE("#00FF88"),        // Green for objectives
        SYSTEM("#FFAA00"),         // Orange for system messages
        ACHIEVEMENT("#FFD700"),    // Gold for achievements
        WARNING("#FF8800"),        // Orange for warnings
        INFO("#00AAFF"),          // Blue for information
        CHAT("#FFFFFF");          // White for chat messages

        private final String defaultColor;

        EventCategory(String defaultColor) {
            this.defaultColor = defaultColor;
        }
    }

    /**
     * Targeting information for event delivery
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventTarget {
        /**
         * Target type - determines who receives the event
         */
        private TargetType type;

        /**
         * Specific player IDs to target (for SPECIFIC type)
         */
        private Set<Integer> playerIds;

        /**
         * Specific team IDs to target (for TEAM type)
         */
        private Set<Integer> teamIds;

        /**
         * Exclude specific player IDs from receiving the event
         */
        private Set<Integer> excludePlayerIds;

        public enum TargetType {
            ALL,        // All players in the game
            TEAM,       // Specific team(s)
            SPECIFIC,   // Specific player(s)
        }
    }

    /**
     * Create a player-specific event
     */
    public static GameEvent createPlayerEvent(String message, int playerId, EventCategory category) {
        return GameEvent.builder()
                .message(message)
                .category(category)
                .color(category.getDefaultColor())
                .target(EventTarget.builder()
                        .type(EventTarget.TargetType.SPECIFIC)
                        .playerIds(Set.of(playerId))
                        .build())
                .displayDuration(3000L)
                .build();
    }
}
