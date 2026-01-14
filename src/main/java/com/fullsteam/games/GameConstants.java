package com.fullsteam.games;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Centralized game constants to replace magic numbers throughout the codebase.
 */
public final class GameConstants {
    private GameConstants() {
    }

    public static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(8);
    public static final int MAX_GLOBAL_GAMES = Integer.parseInt(System.getProperty("max.global.game", "10"));
}

