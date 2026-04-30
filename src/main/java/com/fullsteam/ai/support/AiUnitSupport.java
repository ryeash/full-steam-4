package com.fullsteam.ai.support;

import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Unit;
import com.fullsteam.model.command.ConstructCommand;
import com.fullsteam.model.command.IdleCommand;
import org.dyn4j.geometry.Vector2;

import java.util.Comparator;
import java.util.Optional;

public final class AiUnitSupport {

    private AiUnitSupport() {
    }

    /**
     * Prefers idle builders near {@code near}; skips workers already constructing something else.
     */
    public static Optional<Unit> pickBuildWorker(GameEntities entities, int playerId, Vector2 near) {
        if (near == null) {
            return Optional.empty();
        }
        return entities.getUnits().values().stream()
                .filter(u -> u.belongsTo(playerId) && u.isActive() && !u.isGarrisoned()
                        && u.getUnitType().canBuild())
                .filter(u -> !(u.getCurrentCommand() instanceof ConstructCommand))
                .filter(u -> u.getCurrentCommand() == null || u.getCurrentCommand() instanceof IdleCommand)
                .min(Comparator.comparingDouble(u -> u.getPosition().distanceSquared(near)));
    }

    /**
     * Idle worker suitable for finishing an existing foundation (may already hold {@link ConstructCommand} target — caller decides).
     */
    public static Optional<Unit> pickIdleConstructWorker(GameEntities entities, int playerId, Vector2 near) {
        if (near == null) {
            return Optional.empty();
        }
        return entities.getUnits().values().stream()
                .filter(u -> u.belongsTo(playerId) && u.isActive() && !u.isGarrisoned()
                        && u.getUnitType().canBuild())
                .filter(u -> u.getCurrentCommand() == null || u.getCurrentCommand() instanceof IdleCommand)
                .min(Comparator.comparingDouble(u -> u.getPosition().distanceSquared(near)));
    }
}
