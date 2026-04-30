package com.fullsteam.model;

import io.micronaut.core.annotation.Introspected;

/**
 * Whether a skirmish slot is filled by a human joiner or by an in-game AI faction.
 */
@Introspected
public enum SkirmishSlotKind {
    HUMAN,
    AI
}
