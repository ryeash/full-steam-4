package com.fullsteam.model;

import lombok.Getter;

/**
 * Types of resources in the RTS game.
 * For now, we'll keep it simple with a single resource type.
 */
@Getter
public enum ResourceType {
    CREDITS("Credits", 0x00FF00); // Green color

    private final String displayName;
    private final int color;

    ResourceType(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }
}


