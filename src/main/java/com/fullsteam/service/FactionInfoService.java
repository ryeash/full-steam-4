package com.fullsteam.service;

import com.fullsteam.dto.FactionInfoDTO;
import com.fullsteam.model.*;
import com.fullsteam.model.factions.FactionRegistry;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for building faction information DTOs for the client.
 */
@Singleton
public class FactionInfoService {
    
    /**
     * Get information for all factions
     */
    public List<FactionInfoDTO> getAllFactions() {
        return Arrays.stream(Faction.values())
                .map(this::buildFactionInfo)
                .collect(Collectors.toList());
    }
    
    /**
     * Get information for a specific faction
     */
    public FactionInfoDTO getFactionInfo(Faction faction) {
        return buildFactionInfo(faction);
    }
    
    /**
     * Build complete faction information DTO
     */
    private FactionInfoDTO buildFactionInfo(Faction faction) {
        FactionDefinition definition = FactionRegistry.getDefinition(faction);
        
        // Build unit info list
        List<FactionInfoDTO.UnitInfo> units = new ArrayList<>();
        for (UnitType unitType : UnitType.values()) {
            if (definition.getTechTree().canBuildUnit(unitType)) {
                units.add(buildUnitInfo(unitType, definition));
            }
        }
        
        // Build building info list
        List<FactionInfoDTO.BuildingInfo> buildings = new ArrayList<>();
        for (BuildingType buildingType : BuildingType.values()) {
            if (definition.getTechTree().canBuildBuilding(buildingType)) {
                buildings.add(buildBuildingInfo(buildingType, definition));
            }
        }
        
        // Build bonuses/penalties list
        List<String> bonuses = new ArrayList<>();
        List<String> penalties = new ArrayList<>();
        buildBonusesPenalties(faction, definition, bonuses, penalties);
        
        return FactionInfoDTO.builder()
                .factionId(faction.name())
                .displayName(faction.getDisplayName())
                .description(faction.getDescription())
                .themeColor(faction.getThemeColor())
                .icon(getFactionIcon(faction))
                .availableUnits(units)
                .availableBuildings(buildings)
                .powerEfficiencyModifier(definition.getPowerEfficiencyMultiplier())
                .upkeepLimitModifier(definition.getUpkeepMultiplier())
                .bonuses(bonuses)
                .penalties(penalties)
                .build();
    }
    
    /**
     * Build unit information with faction modifiers
     */
    private FactionInfoDTO.UnitInfo buildUnitInfo(UnitType unitType, FactionDefinition definition) {
        // Get modifiers
        Double costMod = definition.getUnitCostModifiers().get(unitType);
        FactionDefinition.UnitStatModifier statMod = definition.getUnitStatModifiers().get(unitType);
        Double speedMod = statMod != null ? statMod.getSpeedMultiplier() : null;
        
        // Calculate faction-modified values
        int baseCost = unitType.getResourceCost();
        int factionCost = definition.getUnitCost(unitType);
        
        double baseSpeed = unitType.getMovementSpeed();
        double factionSpeed = speedMod != null ? baseSpeed * speedMod : baseSpeed;
        
        return FactionInfoDTO.UnitInfo.builder()
                .unitType(unitType.name())
                .displayName(unitType.getDisplayName())
                // Base stats
                .baseCost(baseCost)
                .baseUpkeep(unitType.getUpkeepCost())
                .baseHealth(unitType.getMaxHealth())
                .baseSpeed(baseSpeed)
                .baseDamage(unitType.getDamage())
                .baseAttackRate(unitType.getAttackRate())
                .baseAttackRange(unitType.getAttackRange())
                // Faction-modified stats
                .cost(factionCost)
                .upkeep(unitType.getUpkeepCost()) // Upkeep not modified per-unit (only global limit)
                .health(unitType.getMaxHealth()) // Health not modified for units currently
                .speed(factionSpeed)
                .damage(unitType.getDamage()) // Damage not modified currently
                .attackRate(unitType.getAttackRate()) // Attack rate not modified currently
                .attackRange(unitType.getAttackRange()) // Range not modified currently
                // Build info
                .buildTimeSeconds(unitType.getBuildTimeSeconds())
                .producedBy(unitType.getProducedBy().name())
                .sides(unitType.getSides())
                .color(unitType.getColor())
                .size(unitType.getSize())
                // Capabilities
                .canAttack(unitType.canAttack())
                .canBuild(unitType.canBuild())
                .canHarvest(unitType.canHarvest())
                .canMine(unitType.canMine())
                .isSupport(unitType.isSupport())
                // Modifiers (for display)
                .costModifier(costMod)
                .speedModifier(speedMod)
                .healthModifier(null) // Not implemented yet
                .damageModifier(null) // Not implemented yet
                .build();
    }
    
    /**
     * Build building information with faction modifiers
     */
    private FactionInfoDTO.BuildingInfo buildBuildingInfo(BuildingType buildingType, FactionDefinition definition) {
        // Get modifiers
        FactionDefinition.BuildingStatModifier statMod = definition.getBuildingStatModifiers().get(buildingType);
        Double healthMod = statMod != null ? statMod.getHealthMultiplier() : null;
        
        // Calculate faction-modified values
        int baseCost = buildingType.getResourceCost();
        int factionCost = definition.getBuildingCost(buildingType);
        
        // Cost modifier is calculated based on actual vs base cost
        Double costMod = baseCost != factionCost ? (double) factionCost / baseCost : null;
        
        double baseHealth = buildingType.getMaxHealth();
        double factionHealth = definition.getBuildingHealth(buildingType);
        
        int basePower = buildingType.getPowerValue();
        int factionPower = definition.getPowerValue(basePower);
        
        // Get produced units (filtered by faction tech tree)
        List<String> producedUnits = new ArrayList<>();
        if (buildingType.isCanProduceUnits()) {
            UnitType[] producibleArray = buildingType.getProducibleUnits();
            System.out.println("Building " + buildingType + " can produce units.");
            System.out.println("  producibleUnits array: " + (producibleArray == null ? "NULL" : "length=" + producibleArray.length));
            
            if (producibleArray != null) {
                for (int i = 0; i < producibleArray.length; i++) {
                    UnitType unitType = producibleArray[i];
                    System.out.println("  [" + i + "] Checking if " + buildingType + " can produce " + unitType);
                    
                    if (unitType == null) {
                        System.out.println("    WARNING: UnitType at index " + i + " is NULL!");
                        continue;
                    }
                    
                    boolean canProduce = definition.getTechTree().canBuildingProduceUnit(buildingType, unitType);
                    System.out.println("    Result: " + canProduce);
                    if (canProduce) {
                        producedUnits.add(unitType.name());
                    }
                }
            }
            System.out.println("Final produced units for " + buildingType + ": " + producedUnits);
        }
        
        // Get tech requirements (buildings required before this can be built)
        List<String> techReqs = new ArrayList<>();
        // For now, we'll determine tech requirements based on tier
        int tier = buildingType.getRequiredTechTier();
        if (tier >= 2) {
            techReqs.add("POWER_PLANT"); // Tier 2+ requires power
        }
        if (tier >= 3) {
            techReqs.add("TECH_CENTER"); // Tier 3 requires tech center
        }
        
        return FactionInfoDTO.BuildingInfo.builder()
                .buildingType(buildingType.name())
                .displayName(buildingType.getDisplayName())
                // Base stats
                .baseCost(baseCost)
                .baseHealth(baseHealth)
                .basePowerValue(basePower)
                // Faction-modified stats
                .cost(factionCost)
                .health(factionHealth)
                .powerValue(factionPower)
                // Build info
                .buildTimeSeconds(buildingType.getBuildTimeSeconds())
                .sides(buildingType.getSides())
                .color(buildingType.getColor())
                .size(buildingType.getSize())
                // Capabilities
                .canProduceUnits(buildingType.isCanProduceUnits())
                .producedUnits(producedUnits)
                .requiredTechTier(buildingType.getRequiredTechTier())
                .techRequirements(techReqs)
                // Modifiers (for display)
                .costModifier(costMod)
                .healthModifier(healthMod)
                .powerModifier(basePower != factionPower ? (double) factionPower / basePower : null)
                .build();
    }
    
    /**
     * Build human-readable bonuses and penalties
     */
    private void buildBonusesPenalties(Faction faction, FactionDefinition definition, 
                                       List<String> bonuses, List<String> penalties) {
        switch (faction) {
            case TERRAN:
                bonuses.add("Access to all standard units and buildings");
                bonuses.add("+10% health for all buildings");
                bonuses.add("Balanced and versatile");
                break;
                
            case NOMADS:
                bonuses.add("Vehicles (Jeep, Tank, Stealth Tank) cost 20% less");
                bonuses.add("Vehicles are 15-20% faster");
                bonuses.add("+50% upkeep limit (can field more units)");
                penalties.add("All buildings have 20% less health");
                penalties.add("Cannot build: Mammoth Tank, Artillery, Gigantonaut");
                break;
                
            case SYNTHESIS:
                bonuses.add("30% better power efficiency (buildings consume less power)");
                bonuses.add("+15% health for all buildings");
                bonuses.add("Advanced technology and shields");
                penalties.add("All units cost 30% more");
                penalties.add("Cannot build: Infantry, Medic");
                break;
        }
    }
    
    /**
     * Get faction icon emoji
     */
    private String getFactionIcon(Faction faction) {
        switch (faction) {
            case TERRAN:
                return "🛡️";
            case NOMADS:
                return "🏎️";
            case SYNTHESIS:
                return "⚡";
            default:
                return "❓";
        }
    }
}

