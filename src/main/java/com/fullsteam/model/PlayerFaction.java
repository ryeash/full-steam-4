package com.fullsteam.model;

import com.fullsteam.model.factions.Faction;
import com.fullsteam.model.factions.FactionDefinition;
import com.fullsteam.model.factions.FactionRegistry;
import com.fullsteam.model.research.ResearchManager;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a player's faction/base in the RTS game.
 * Tracks resources, buildings, units, and production queues.
 */
@Data
public class PlayerFaction {
    private final int playerId;
    private final int teamNumber;
    private final String playerName;

    // Faction system
    private Faction faction = Faction.TERRAN; // Default faction
    private FactionDefinition factionDefinition;

    // Unified research system (handles both building and unit tech tree research)
    private ResearchManager researchManager;

    // Resources
    private final Map<ResourceType, Integer> resources = new HashMap<>();

    // Unit/building limits
    private int unitCount = 0;
    private int maxUnits = 100; // Population cap

    // Upkeep/supply system
    private int currentUpkeep = 0;
    /**
     * -- SETTER --
     * Set max upkeep (base + bonuses from monuments)
     */
    private int maxUpkeep = 250; // Supply cap (base value, modified by faction)

    // Power system
    private int powerGenerated = 0;
    private int powerConsumed = 0;
    private boolean hasLowPower = false; // True when powerConsumed > powerGenerated

    // Starting resources
    public PlayerFaction(int playerId, int teamNumber, String playerName) {
        this.playerId = playerId;
        this.teamNumber = teamNumber;
        this.playerName = playerName;

        // Initialize faction (default to TERRAN for backward compatibility)
        setFaction(Faction.TERRAN);

        // Initialize with starting resources
        resources.put(ResourceType.CREDITS, 1000); // Starting credits
    }

    /**
     * Constructor with faction selection
     */
    public PlayerFaction(int playerId, int teamNumber, String playerName, Faction faction) {
        this.playerId = playerId;
        this.teamNumber = teamNumber;
        this.playerName = playerName;

        // Initialize faction
        setFaction(faction);

        // Initialize with starting resources
        resources.put(ResourceType.CREDITS, 1000); // Starting credits
    }

    /**
     * Set the faction for this player (loads faction definition)
     */
    public void setFaction(Faction faction) {
        this.faction = faction;
        this.factionDefinition = FactionRegistry.getDefinition(faction);

        // Apply faction-specific upkeep limit
        this.maxUpkeep = factionDefinition.getUpkeepLimit(250); // Base 250
        
        // Initialize unified research manager (handles both building and unit research)
        this.researchManager = new ResearchManager(playerId, faction);
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
     * Check if this faction can build more units
     */
    public boolean canBuildMoreUnits() {
        return unitCount < maxUnits;
    }

    /**
     * Increment unit count
     */
    public void incrementUnitCount() {
        unitCount++;
    }

    /**
     * Check if this faction can afford the upkeep cost
     */
    public boolean canAffordUpkeep(int upkeepCost) {
        return currentUpkeep + upkeepCost <= maxUpkeep;
    }

    /**
     * Add upkeep cost
     */
    public void addUpkeep(int upkeepCost) {
        currentUpkeep += upkeepCost;
    }

    /**
     * Remove upkeep cost
     */
    public void removeUpkeep(int upkeepCost) {
        currentUpkeep = Math.max(0, currentUpkeep - upkeepCost);
    }

    // ===== Faction-specific methods =====

    /**
     * Check if this faction can build a specific unit type
     * @deprecated Use canProduceUnit() which respects research unlocks instead
     */
    @Deprecated
    public boolean canBuildUnit(UnitType unitType) {
        return factionDefinition.getTechTree().canBuildUnit(unitType);
    }

    /**
     * Check if this faction can build a specific building type
     */
    public boolean canBuildBuilding(BuildingType buildingType) {
        return factionDefinition.getTechTree().canBuildBuilding(buildingType);
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
     * Get the effective power value (with faction efficiency applied)
     */
    public int getPowerValue(int basePower) {
        return factionDefinition.getPowerValue(basePower);
    }

    /**
     * Check if a building can produce a specific unit for this faction
     * @deprecated Use canProduceUnit() + UnitType.getProducedBy() validation instead
     */
    @Deprecated
    public boolean canBuildingProduceUnit(BuildingType buildingType, UnitType unitType) {
        return factionDefinition.getTechTree().canBuildingProduceUnit(buildingType, unitType);
    }
    
    // ===== Unit Tech Tree Methods =====
    
    /**
     * Get available units for a specific category (from tech tree)
     */
    public Set<UnitType> getAvailableUnits(UnitCategory category) {
        if (researchManager == null) {
            return Set.of();
        }
        return researchManager.getAvailableUnits(category);
    }
    
    /**
     * Check if a unit can be produced (based on tech tree research)
     */
    public boolean canProduceUnit(UnitType unitType) {
        if (researchManager == null) {
            return false;
        }
        UnitCategory category = unitType.getCategory();
        return researchManager.getAvailableUnits(category).contains(unitType);
    }
}

