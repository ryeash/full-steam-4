package com.fullsteam.model;

import com.fullsteam.model.factions.FactionDefinition;
import io.micronaut.websocket.WebSocketSession;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a player's faction/base in the RTS game.
 * Tracks resources, buildings, units, production queues, and (for human players) the live WebSocket session.
 */
@Data
public class Player {

    private final int playerId;
    private final int teamNumber;
    private FactionDefinition factionDefinition;
    private WebSocketSession webSocketSession;
    private final Map<ResourceType, Integer> resources = new HashMap<>();
    private int currentUpkeep = 0;
    private int powerGenerated = 0;
    private int powerConsumed = 0;
    private boolean hasLowPower = false;
    private SatelliteReveal satelliteReveal;

    /**
     * Wall-clock time (ms since epoch) when each command ability cooldown ends; absent if ready.
     */
    private final Map<CommandAbilityType, Long> commandAbilityCooldownEndsAtMs = new ConcurrentHashMap<>();

    /**
     * Constructor for AI or tests (no WebSocket).
     */
    public Player(int playerId, int teamNumber, FactionDefinition customDefinition) {
        this(playerId, teamNumber, customDefinition, null);
    }

    /**
     * Constructor with faction selection and optional live session (human players).
     */
    public Player(int playerId, int teamNumber, FactionDefinition customDefinition,
                  WebSocketSession webSocketSession) {
        this.playerId = playerId;
        this.teamNumber = teamNumber;
        this.factionDefinition = customDefinition;
        this.webSocketSession = webSocketSession;
        this.resources.put(ResourceType.CREDITS, 1000); // Starting credits
    }

    /**
     * Add resources to this faction
     */
    public void addResources(ResourceType type, int amount) {
        resources.merge(type, amount, Integer::sum);
    }

    /**
     * Remove resources from this faction
     *
     * @return true if resources were available and removed, false otherwise
     */
    public boolean removeResources(ResourceType type, int amount) {
        int current = resources.getOrDefault(type, 0);
        if (current >= amount) {
            resources.put(type, current - amount);
            return true;
        }
        return false;
    }

    /**
     * Check if this faction has enough resources
     */
    public boolean hasResources(ResourceType type, int amount) {
        return resources.getOrDefault(type, 0) >= amount;
    }

    /**
     * Get current resource amount
     */
    public int getResourceAmount(ResourceType type) {
        return resources.getOrDefault(type, 0);
    }

    /**
     * Check if this faction can build a specific unit type
     *
     * @deprecated Use canProduceUnit() which respects research unlocks instead
     */
    @Deprecated
    public boolean canBuildUnit(UnitType unitType) {
        return factionDefinition.canBuildUnit(unitType);
    }

    /**
     * Check if this faction can build a specific building type
     */
    public boolean canBuildBuilding(BuildingType buildingType) {
        return factionDefinition.canBuildBuilding(buildingType);
    }

    /**
     * Get the cost for a unit (with faction modifiers applied)
     */
    public int getUnitCost(UnitType unitType) {
        return factionDefinition.getUnitCost(unitType);
    }

    /**
     * Get the cost for a building (with faction modifiers applied)
     */
    public int getBuildingCost(BuildingType buildingType) {
        return factionDefinition.getBuildingCost(buildingType);
    }

    /**
     * Get the health for a building (with faction modifiers applied)
     */
    public double getBuildingHealth(BuildingType buildingType) {
        return factionDefinition.getBuildingHealth(buildingType);
    }

    /**
     * Check if a unit can be produced (based on custom faction selection)
     */
    public boolean canProduceUnit(UnitType unitType) {
        return factionDefinition.getUnitTypes().contains(unitType);
    }

    /**
     * Check if the player has all required tech buildings to produce this unit.
     * This is separate from faction selection - even if a unit is selected in the
     * custom faction, the player must build the required tech buildings first.
     *
     * @param unitType        The unit type to check
     * @param playerBuildings Set of building types the player has constructed
     * @return true if all required buildings are present (or no requirements)
     */
    public boolean hasRequiredTechBuildings(UnitType unitType, Set<BuildingType> playerBuildings) {
        Set<BuildingType> required = unitType.getRequiredBuildings();
        if (required.isEmpty()) {
            return true; // No tech requirements
        }
        return playerBuildings.containsAll(required);
    }

    public long getCommandAbilityCooldownEndsAt(CommandAbilityType type) {
        return commandAbilityCooldownEndsAtMs.getOrDefault(type, 0L);
    }

    public void setCommandAbilityCooldownEndsAt(CommandAbilityType type, long endsAtEpochMs) {
        commandAbilityCooldownEndsAtMs.put(type, endsAtEpochMs);
    }

    /**
     * When a command-ability unlock building is destroyed, drop cooldown state so the UI shows the ability gone.
     */
    public void clearCommandAbilityCooldownsForUnlockBuilding(BuildingType destroyedBuildingType) {
        for (CommandAbilityType t : CommandAbilityType.values()) {
            if (t.getUnlockingBuilding() == destroyedBuildingType) {
                commandAbilityCooldownEndsAtMs.remove(t);
            }
        }
    }
}

