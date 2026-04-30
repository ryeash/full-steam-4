package com.fullsteam.model;

/**
 * Skirmish AI opponent strength; drives {@link com.fullsteam.ai.SkirmishAiProfile} tuning.
 * Serialized on {@link SkirmishSlotConfig} for AI roster slots.
 */
public enum AiDifficulty {
    EASY,
    NORMAL,
    HARD
}
