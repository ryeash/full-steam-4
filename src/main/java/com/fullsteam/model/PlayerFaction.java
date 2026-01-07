package com.fullsteam.model;

import com.fullsteam.model.customization.CustomFactionConfig;
import com.fullsteam.model.customization.FactionPerk;
import com.fullsteam.model.factions.FactionDefinition;
import com.fullsteam.model.factions.FactionTechTree;
import com.fullsteam.model.research.FactionModifierManager;
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
    private FactionDefinition factionDefinition;

    // Unit availability manager (replaces old research system)
    private FactionModifierManager modifierManager;

    // Resources
    private final Map<ResourceType, Integer> resources = new HashMap<>();

    // Unit/building limits
    private int unitCount = 0;
    private int maxUnits = 100; // Population cap

    // Upkeep/supply system
    private int currentUpkeep = 0;
    private int maxUpkeep = 250; // Supply cap (base value, modified by faction)

    // Power system
    private int powerGenerated = 0;
    private int powerConsumed = 0;
    private boolean hasLowPower = false; // True when powerConsumed > powerGenerated

    /**
     * Constructor with faction selection
     */
    public PlayerFaction(int playerId, int teamNumber, String playerName) {
        this.playerId = playerId;
        this.teamNumber = teamNumber;
        this.playerName = playerName;
        this.factionDefinition = FactionDefinition.builder()
                .techTree(FactionTechTree.builder()
                        .buildingsAndUnits(Map.of())
                        .build())
                .heroUnit(null)
                .monumentBuilding(null)
                .build();
        this.maxUpkeep = factionDefinition.getUpkeepLimit(250); // Base 250
        this.modifierManager = new FactionModifierManager(playerId);
        this.resources.put(ResourceType.CREDITS, 1000); // Starting credits
    }

    /**
     * Apply a custom faction definition with perks (for player-created factions)
     */
    public void applyCustomFaction(FactionDefinition customDefinition,
                                   CustomFactionConfig config) {
        this.factionDefinition = customDefinition;

        // Apply faction-specific upkeep limit
        this.maxUpkeep = customDefinition.getUpkeepLimit(250); // Base 250

        // Initialize modifier manager
        this.modifierManager = new FactionModifierManager(playerId);

        // Set available units for custom faction
        if (!customDefinition.getCustomSelectedUnits().isEmpty()) {
            this.modifierManager.setAvailableUnits(customDefinition.getCustomSelectedUnits());
        }

        // Set active perks (effective perks only - highest tier in each chain)
        if (config != null && !config.getSelectedPerks().isEmpty()) {
            Set<FactionPerk> effectivePerks = config.getEffectivePerks();
            this.modifierManager.setActivePerks(effectivePerks);
        }
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
     * Check if this faction can build a specific unit type
     *
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
     * Check if a building can produce a specific unit for this faction
     *
     * @deprecated Use canProduceUnit() + UnitType.getProducedBy() validation instead
     */
    @Deprecated
    public boolean canBuildingProduceUnit(BuildingType buildingType, UnitType unitType) {
        return factionDefinition.getTechTree().canBuildingProduceUnit(buildingType, unitType);
    }

    /**
     * Check if a unit can be produced (based on custom faction selection)
     */
    public boolean canProduceUnit(UnitType unitType) {
        if (modifierManager == null) {
            return false;
        }
        UnitCategory category = unitType.getCategory();
        return modifierManager.getAvailableUnits(category).contains(unitType);
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
}

