package com.fullsteam.games;

import com.fullsteam.dto.FactionInfoDTO;
import com.fullsteam.dto.ResearchInfoDTO;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.Faction;
import com.fullsteam.model.factions.FactionDefinition;
import com.fullsteam.model.factions.FactionRegistry;
import com.fullsteam.model.research.ResearchType;
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
            // Include HEADQUARTERS even though it's not buildable (needed for unit production info)
            // Also include any building that can be built by this faction
            if (buildingType == BuildingType.HEADQUARTERS || definition.getTechTree().canBuildBuilding(buildingType)) {
                buildings.add(buildBuildingInfo(buildingType, definition));
            }
        }

        // Build research info list (all research available - client filters based on requirements)
        List<ResearchInfoDTO> research = new ArrayList<>();
        for (ResearchType researchType : ResearchType.values()) {
            research.add(ResearchInfoDTO.fromResearchType(researchType));
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
                .availableResearch(research)
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

        // Get produced units directly from faction tech tree (single source of truth)
        List<String> producedUnits = new ArrayList<>();
        if (buildingType.isCanProduceUnits()) {
            List<UnitType> factionUnits = definition.getTechTree().getUnitsProducedBy(buildingType);
            for (UnitType unitType : factionUnits) {
                producedUnits.add(unitType.name());
            }
        }

        // Get tech requirements (buildings required before this can be built)
        List<String> techReqs = getTechRequirements(buildingType);

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
     * Get tech requirements for a building type
     * This is the single source of truth for building dependencies
     */
    private List<String> getTechRequirements(BuildingType buildingType) {
        return switch (buildingType) {
            // T1 - Always available (no requirements)
            case HEADQUARTERS, POWER_PLANT, BARRACKS, REFINERY, BUNKER, WALL -> List.of();

            // T2 - Requires Power Plant
            case RESEARCH_LAB, FACTORY, WEAPONS_DEPOT, TURRET, SHIELD_GENERATOR -> List.of("POWER_PLANT");

            // T3 - Requires Power Plant + Research Lab
            case TECH_CENTER, ADVANCED_FACTORY, BANK -> List.of("POWER_PLANT", "RESEARCH_LAB");

            // Monument Buildings - Requires Power Plant + Research Lab (T3)
            case SANDSTORM_GENERATOR, ANDROID_FACTORY, PHOTON_SPIRE, COMMAND_CITADEL -> List.of("POWER_PLANT", "RESEARCH_LAB");
        };
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

            case TECH_ALLIANCE:
                bonuses.add("All units use instant-hit beam weapons (no projectile travel time)");
                bonuses.add("Buildings cost 10% less");
                bonuses.add("Photon Spire monument amplifies beam damage by 35%");
                penalties.add("All units cost 15% more");
                penalties.add("Buildings consume 20% more power");
                penalties.add("Cannot build: Standard projectile units");
                break;
        }
    }

    /**
     * Get faction icon emoji
     */
    private String getFactionIcon(Faction faction) {
        return switch (faction) {
            case TERRAN -> "🛡️";
            case NOMADS -> "🏎️";
            case SYNTHESIS -> "⚡";
            case TECH_ALLIANCE -> "🔬";
        };
    }
}

