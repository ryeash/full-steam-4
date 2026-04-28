package com.fullsteam.model;

import com.fullsteam.model.component.NukeSiloComponent;
import lombok.Data;
import org.dyn4j.geometry.Vector2;

import java.util.Optional;

/**
 * Narrow facade passed into {@link CommandAbilityEffect} so ability logic does not touch all of
 * {@link RTSGameManager} internals.
 */
@Data
public final class CommandAbilityExecutionContext {

    private final GameEntities gameEntities;
    private final int playerId;
    private final Player faction;
    private final Vector2 target;
    private final Integer sourceBuildingId;
    private final long now;

    public CommandAbilityExecutionContext(
            GameEntities gameEntities,
            int playerId,
            Player faction,
            Vector2 target,
            Integer sourceBuildingId,
            long now) {
        this.gameEntities = gameEntities;
        this.playerId = playerId;
        this.faction = faction;
        this.target = target;
        this.sourceBuildingId = sourceBuildingId;
        this.now = now;
    }

    public RTSGameManager game() {
        return gameEntities.getRtsGameManager();
    }

    public void warn(String message) {
        gameEntities.getRtsGameManager()
                .sendGameEvent(GameEvent.createPlayerEvent(message, playerId, GameEvent.EventCategory.WARNING));
    }

    public void info(String message) {
        gameEntities.getRtsGameManager()
                .sendGameEvent(GameEvent.createPlayerEvent(message, playerId, GameEvent.EventCategory.INFO));
    }

    public Optional<Vector2> targetOptional() {
        return Optional.ofNullable(target);
    }

    /**
     * @return target or {@code null} after sending a generic "requires a target location" warning.
     */
    public Vector2 requireGroundTargetOrWarn() {
        if (target == null) {
            warn("Command ability requires a target location.");
            return null;
        }
        return target;
    }

    public boolean isWithinWorldBounds(Vector2 t) {
        double halfW = gameEntities.getRtsGameManager().getGameConfig().getWorldWidth() / 2.0;
        double halfH = gameEntities.getRtsGameManager().getGameConfig().getWorldHeight() / 2.0;
        return t.x >= -halfW && t.x <= halfW && t.y >= -halfH && t.y <= halfH;
    }

    public void warnTargetOutsideBattlefield() {
        warn("Target location is outside the battlefield.");
    }

    public Optional<NukeSiloComponent> findNukeSilo() {
        return gameEntities.getRtsGameManager().findPlayerNukeSiloComponent(playerId);
    }

    public Building buildingById(int id) {
        return gameEntities.getBuildings().get(id);
    }

    /**
     * When {@code sourceBuildingId} is set, it must be a completed active relay of {@code abilityType}'s
     * {@link CommandAbilityType#getUnlockingBuilding()}.
     */
    public boolean validateSourceRelayIfPresent(CommandAbilityType abilityType) {
        if (sourceBuildingId == null) {
            return true;
        }
        Building src = buildingById(sourceBuildingId);
        if (src == null || !src.belongsTo(playerId) || src.isUnderConstruction()
                || src.getBuildingType() != abilityType.getUnlockingBuilding() || !src.isActive()) {
            warn("Invalid command relay for this ability.");
            return false;
        }
        return true;
    }

    public boolean validateNukeSiloSourceIfPresent() {
        if (sourceBuildingId == null) {
            return true;
        }
        Building src = buildingById(sourceBuildingId);
        if (src == null || !src.belongsTo(playerId) || src.isUnderConstruction()
                || src.getBuildingType() != BuildingType.NUKE_SILO || !src.isActive()) {
            warn("Invalid silo for this launch.");
            return false;
        }
        return true;
    }

    public void placeFieldEffect(CommandAbilityType abilityType, FieldEffectType fieldType, Vector2 position) {
        FieldEffect effect = new FieldEffect(
                playerId,
                fieldType,
                position,
                abilityType.getEffectRadius(),
                abilityType.getEffectDamage(),
                fieldType.getDefaultDuration(),
                faction.getTeamNumber()
        );
        gameEntities.add(effect);
    }
}
