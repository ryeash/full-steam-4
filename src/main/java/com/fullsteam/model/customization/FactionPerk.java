package com.fullsteam.model.customization;

import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.GameEvent;
import com.fullsteam.model.PlayerFaction;
import com.fullsteam.model.RTSGameManager;
import com.fullsteam.model.ResourceType;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitCategory;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.FactionDefinition;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Perks that can be selected during faction customization.
 * Each perk implements PerkEffect to define its behavior.
 * <p>
 * Perks are organized into tiers (e.g., POWER_EFFICIENCY_1/2/3).
 * Players must select all lower tiers to unlock higher tiers, but only
 * the highest tier in each chain is actually applied (see getEffectivePerks()).
 */
@Slf4j
@Getter
public enum FactionPerk implements PerkEffect {
    // ===== ECONOMIC PERKS =====
    POWER_EFFICIENCY_1(
            "Power Efficiency I",
            "-15% power consumption",
            3,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            builder.powerEfficiencyMultiplier(0.85);
        }
    },
    POWER_EFFICIENCY_2(
            "Power Efficiency II",
            "-30% power consumption",
            6,
            Set.of("POWER_EFFICIENCY_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            builder.powerEfficiencyMultiplier(0.70);
        }
    },
    POWER_EFFICIENCY_3(
            "Power Efficiency III",
            "-45% power consumption",
            10,
            Set.of("POWER_EFFICIENCY_2")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            builder.powerEfficiencyMultiplier(0.55);
        }
    },

    RESOURCE_BOOST_1(
            "Resource Boost I",
            "+15% harvesting rate",
            4,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            // Apply to WORKER unit (the only harvester)
            Map<UnitType, FactionDefinition.UnitStatModifier> unitMods = new HashMap<>();
            unitMods.put(UnitType.WORKER, FactionDefinition.UnitStatModifier.builder()
                    .resourceCollectionMultiplier(1.15)
                    .build());
            builder.unitStatModifiers(unitMods);
        }
    },
    RESOURCE_BOOST_2(
            "Resource Boost II",
            "+30% harvesting rate",
            8,
            Set.of("RESOURCE_BOOST_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            // Apply to WORKER unit (the only harvester)
            Map<UnitType, FactionDefinition.UnitStatModifier> unitMods = new HashMap<>();
            unitMods.put(UnitType.WORKER, FactionDefinition.UnitStatModifier.builder()
                    .resourceCollectionMultiplier(1.30)
                    .build());
            builder.unitStatModifiers(unitMods);
        }
    },

    COST_REDUCTION_1(
            "Cost Reduction I",
            "-10% all costs",
            5,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            builder.unitCostMultiplier(0.90);
            builder.buildingCostMultiplier(0.90);
        }
    },
    COST_REDUCTION_2(
            "Cost Reduction II",
            "-20% all costs",
            9,
            Set.of("COST_REDUCTION_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            builder.unitCostMultiplier(0.80);
            builder.buildingCostMultiplier(0.80);
        }
    },

    // ===== MILITARY PERKS =====
    UPKEEP_INCREASE_1(
            "Increased Upkeep I",
            "+25% upkeep limit",
            3,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            builder.upkeepMultiplier(1.25);
        }
    },
    UPKEEP_INCREASE_2(
            "Increased Upkeep II",
            "+50% upkeep limit",
            6,
            Set.of("UPKEEP_INCREASE_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            builder.upkeepMultiplier(1.50);
        }
    },
    UPKEEP_INCREASE_3(
            "Increased Upkeep III",
            "+75% upkeep limit",
            10,
            Set.of("UPKEEP_INCREASE_2")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            builder.upkeepMultiplier(1.75);
        }
    },

    VETERAN_UNITS_1(
            "Veteran Units I",
            "+10% HP for all units",
            3,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            // Apply to all selected units
            Map<UnitType, FactionDefinition.UnitStatModifier> unitMods = new HashMap<>();
            for (UnitType unit : config.getSelectedUnits()) {
                unitMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                        .healthMultiplier(1.10)
                        .build());
            }
            builder.unitStatModifiers(unitMods);
        }
    },
    VETERAN_UNITS_2(
            "Veteran Units II",
            "+20% HP for all units",
            6,
            Set.of("VETERAN_UNITS_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<UnitType, FactionDefinition.UnitStatModifier> unitMods = new HashMap<>();
            for (UnitType unit : config.getSelectedUnits()) {
                unitMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                        .healthMultiplier(1.20)
                        .build());
            }
            builder.unitStatModifiers(unitMods);
        }
    },

    RAPID_DEPLOYMENT_1(
            "Rapid Deployment I",
            "-15% build time",
            3,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            // Apply to all selected buildings
            Map<BuildingType, FactionDefinition.BuildingStatModifier> buildingMods = new HashMap<>();
            for (BuildingType building : config.getSelectedBuildings()) {
                buildingMods.put(building, FactionDefinition.BuildingStatModifier.builder()
                        .buildTimeMultiplier(0.85)
                        .build());
            }
            builder.buildingStatModifiers(buildingMods);
        }
    },
    RAPID_DEPLOYMENT_2(
            "Rapid Deployment II",
            "-30% build time",
            6,
            Set.of("RAPID_DEPLOYMENT_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<BuildingType, FactionDefinition.BuildingStatModifier> buildingMods = new HashMap<>();
            for (BuildingType building : config.getSelectedBuildings()) {
                buildingMods.put(building, FactionDefinition.BuildingStatModifier.builder()
                        .buildTimeMultiplier(0.70)
                        .build());
            }
            builder.buildingStatModifiers(buildingMods);
        }
    },

    DAMAGE_BOOST_1(
            "Damage Boost I",
            "+10% damage for all units",
            4,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<UnitType, FactionDefinition.UnitStatModifier> unitMods = new HashMap<>();
            for (UnitType unit : config.getSelectedUnits()) {
                unitMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                        .damageMultiplier(1.10)
                        .build());
            }
            builder.unitStatModifiers(unitMods);
        }
    },
    DAMAGE_BOOST_2(
            "Damage Boost II",
            "+20% damage for all units",
            8,
            Set.of("DAMAGE_BOOST_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<UnitType, FactionDefinition.UnitStatModifier> unitMods = new HashMap<>();
            for (UnitType unit : config.getSelectedUnits()) {
                unitMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                        .damageMultiplier(1.20)
                        .build());
            }
            builder.unitStatModifiers(unitMods);
        }
    },

    // ===== DEFENSIVE PERKS =====
    FORTIFIED_1(
            "Fortified Structures I",
            "+15% building HP",
            3,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            builder.buildingHealthMultiplier(1.15);
        }
    },
    FORTIFIED_2(
            "Fortified Structures II",
            "+30% building HP",
            6,
            Set.of("FORTIFIED_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            builder.buildingHealthMultiplier(1.30);
        }
    },

    TURRET_EFFICIENCY(
            "Turret Efficiency",
            "-25% turret cost, +10% turret damage",
            4,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<BuildingType, FactionDefinition.BuildingStatModifier> buildingMods = new HashMap<>();
            buildingMods.put(BuildingType.TURRET, FactionDefinition.BuildingStatModifier.builder()
                    .costMultiplier(0.75)
                    .damageMultiplier(1.10)
                    .build());
            builder.buildingStatModifiers(buildingMods);
        }
    },

    BUNKER_MASTERY(
            "Bunker Mastery",
            "Bunkers hold 6 units (up from 4) and have +25% HP",
            4,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<BuildingType, FactionDefinition.BuildingStatModifier> buildingMods = new HashMap<>();
            buildingMods.put(BuildingType.BUNKER, FactionDefinition.BuildingStatModifier.builder()
                    .healthMultiplier(1.25)
                    .garrisonCapacityBonus(2)
                    .build());
            builder.buildingStatModifiers(buildingMods);
        }
    },

    // ===== SPECIALIZED PERKS =====
    AIR_SUPERIORITY_1(
            "Air Superiority I",
            "-15% air unit costs, +10% air unit speed",
            4,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<UnitType, Double> costMods = new HashMap<>();
            Map<UnitType, FactionDefinition.UnitStatModifier> statMods = new HashMap<>();
            for (UnitType unit : config.getSelectedUnits()) {
                if (unit.getCategory() == UnitCategory.FLYER) {
                    costMods.put(unit, 0.85);
                    statMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                            .speedMultiplier(1.10)
                            .build());
                }
            }
            builder.unitCostModifiers(costMods);
            builder.unitStatModifiers(statMods);
        }
    },
    AIR_SUPERIORITY_2(
            "Air Superiority II",
            "-25% air unit costs, +20% air unit speed",
            7,
            Set.of("AIR_SUPERIORITY_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<UnitType, Double> costMods = new HashMap<>();
            Map<UnitType, FactionDefinition.UnitStatModifier> statMods = new HashMap<>();
            for (UnitType unit : config.getSelectedUnits()) {
                if (unit.getCategory() == UnitCategory.FLYER) {
                    costMods.put(unit, 0.75);
                    statMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                            .speedMultiplier(1.20)
                            .build());
                }
            }
            builder.unitCostModifiers(costMods);
            builder.unitStatModifiers(statMods);
        }
    },

    MECHANIZED_WARFARE_1(
            "Mechanized Warfare I",
            "-15% vehicle costs, +10% vehicle HP",
            4,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<UnitType, Double> costMods = new HashMap<>();
            Map<UnitType, FactionDefinition.UnitStatModifier> statMods = new HashMap<>();
            for (UnitType unit : config.getSelectedUnits()) {
                if (unit.getCategory() == UnitCategory.VEHICLE) {
                    costMods.put(unit, 0.85);
                    statMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                            .healthMultiplier(1.10)
                            .build());
                }
            }
            builder.unitCostModifiers(costMods);
            builder.unitStatModifiers(statMods);
        }
    },
    MECHANIZED_WARFARE_2(
            "Mechanized Warfare II",
            "-25% vehicle costs, +20% vehicle HP",
            7,
            Set.of("MECHANIZED_WARFARE_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<UnitType, Double> costMods = new HashMap<>();
            Map<UnitType, FactionDefinition.UnitStatModifier> statMods = new HashMap<>();
            for (UnitType unit : config.getSelectedUnits()) {
                if (unit.getCategory() == UnitCategory.VEHICLE) {
                    costMods.put(unit, 0.75);
                    statMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                            .healthMultiplier(1.20)
                            .build());
                }
            }
            builder.unitCostModifiers(costMods);
            builder.unitStatModifiers(statMods);
        }
    },

    INFANTRY_DOCTRINE_1(
            "Infantry Doctrine I",
            "-15% infantry costs, +10% infantry damage",
            4,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<UnitType, Double> costMods = new HashMap<>();
            Map<UnitType, FactionDefinition.UnitStatModifier> statMods = new HashMap<>();
            for (UnitType unit : config.getSelectedUnits()) {
                if (unit.getCategory() == UnitCategory.INFANTRY) {
                    costMods.put(unit, 0.85);
                    statMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                            .damageMultiplier(1.10)
                            .build());
                }
            }
            builder.unitCostModifiers(costMods);
            builder.unitStatModifiers(statMods);
        }
    },
    INFANTRY_DOCTRINE_2(
            "Infantry Doctrine II",
            "-25% infantry costs, +20% infantry damage",
            7,
            Set.of("INFANTRY_DOCTRINE_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<UnitType, Double> costMods = new HashMap<>();
            Map<UnitType, FactionDefinition.UnitStatModifier> statMods = new HashMap<>();
            for (UnitType unit : config.getSelectedUnits()) {
                if (unit.getCategory() == UnitCategory.INFANTRY) {
                    costMods.put(unit, 0.75);
                    statMods.put(unit, FactionDefinition.UnitStatModifier.builder()
                            .damageMultiplier(1.20)
                            .build());
                }
            }
            builder.unitCostModifiers(costMods);
            builder.unitStatModifiers(statMods);
        }
    },

    SALVAGE_OPERATIONS(
            "Salvage Operations",
            "Recover 30% of unit/building cost when destroyed",
            5,
            Set.of()
    ) {
        private static final double SALVAGE_RATE = 0.30;

        @Override
        public void onUnitDestroyed(Unit unit, PlayerFaction faction, RTSGameManager game) {
            int baseCost = unit.getUnitType().getResourceCost();

            // Apply faction cost modifiers to get the actual cost paid
            double costMultiplier = faction.getFactionDefinition() != null
                    ? faction.getFactionDefinition().getUnitCostMultiplier()
                    : 1.0;

            // Check for unit-specific cost modifiers
            if (faction.getFactionDefinition() != null &&
                    faction.getFactionDefinition().getUnitCostModifiers() != null) {
                Double unitMod = faction.getFactionDefinition().getUnitCostModifiers().get(unit.getUnitType());
                if (unitMod != null) {
                    costMultiplier *= unitMod;
                }
            }

            int actualCost = (int) (baseCost * costMultiplier);
            int refund = (int) (actualCost * SALVAGE_RATE);

            if (refund > 0) {
                faction.addResources(ResourceType.CREDITS, refund);
                log.info("Player {} - Salvage Operations: Refunded {} credits for destroyed {} ({}% of {} cost)",
                        faction.getPlayerId(), refund, unit.getUnitType(),
                        (int) (SALVAGE_RATE * 100), actualCost);

                // Send notification to player
                if (game != null) {
                    game.sendGameEvent(GameEvent.createPlayerEvent(
                            String.format("♻️ Salvaged %d credits from destroyed unit", refund),
                            faction.getPlayerId(),
                            GameEvent.EventCategory.INFO
                    ));
                }
            }
        }

        @Override
        public void onBuildingDestroyed(Building building, PlayerFaction faction, RTSGameManager game) {
            int baseCost = building.getBuildingType().getResourceCost();

            // Apply faction cost modifiers to get the actual cost paid
            double costMultiplier = faction.getFactionDefinition() != null
                    ? faction.getFactionDefinition().getBuildingCostMultiplier()
                    : 1.0;

            int actualCost = (int) (baseCost * costMultiplier);
            int refund = (int) (actualCost * SALVAGE_RATE);

            if (refund > 0) {
                faction.addResources(ResourceType.CREDITS, refund);
                log.info("Player {} - Salvage Operations: Refunded {} credits for destroyed {} ({}% of {} cost)",
                        faction.getPlayerId(), refund, building.getBuildingType(),
                        (int) (SALVAGE_RATE * 100), actualCost);

                // Send notification to player
                if (game != null) {
                    game.sendGameEvent(GameEvent.createPlayerEvent(
                            String.format("♻️ Salvaged %d credits from destroyed building", refund),
                            faction.getPlayerId(),
                            GameEvent.EventCategory.INFO
                    ));
                }
            }
        }
    },

    INFANTRY_TRAINING_1(
            "Infantry Training I",
            "Barracks train infantry 20% faster",
            3,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<BuildingType, FactionDefinition.BuildingStatModifier> buildingMods = new HashMap<>();
            buildingMods.put(BuildingType.BARRACKS, FactionDefinition.BuildingStatModifier.builder()
                    .productionSpeedMultiplier(1.20) // 20% faster
                    .build());
            builder.buildingStatModifiers(buildingMods);
        }
    },
    INFANTRY_TRAINING_2(
            "Infantry Training II",
            "Barracks train infantry 40% faster",
            6,
            Set.of("INFANTRY_TRAINING_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<BuildingType, FactionDefinition.BuildingStatModifier> buildingMods = new HashMap<>();
            buildingMods.put(BuildingType.BARRACKS, FactionDefinition.BuildingStatModifier.builder()
                    .productionSpeedMultiplier(1.40) // 40% faster
                    .build());
            builder.buildingStatModifiers(buildingMods);
        }
    },
    
    VEHICLE_PRODUCTION_1(
            "Vehicle Production I",
            "Factories produce vehicles 20% faster",
            3,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<BuildingType, FactionDefinition.BuildingStatModifier> buildingMods = new HashMap<>();
            buildingMods.put(BuildingType.FACTORY, FactionDefinition.BuildingStatModifier.builder()
                    .productionSpeedMultiplier(1.20) // 20% faster
                    .build());
            builder.buildingStatModifiers(buildingMods);
        }
    },
    VEHICLE_PRODUCTION_2(
            "Vehicle Production II",
            "Factories produce vehicles 40% faster",
            6,
            Set.of("VEHICLE_PRODUCTION_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            Map<BuildingType, FactionDefinition.BuildingStatModifier> buildingMods = new HashMap<>();
            buildingMods.put(BuildingType.FACTORY, FactionDefinition.BuildingStatModifier.builder()
                    .productionSpeedMultiplier(1.40) // 40% faster
                    .build());
            builder.buildingStatModifiers(buildingMods);
        }
    },
    
    LOGISTICS_NETWORK(
            "Logistics Network",
            "Buildings cost 15% less, build 20% faster",
            6,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            // Apply cost reduction
            double currentCost = 0.85; // -15%
            builder.buildingCostMultiplier(currentCost);

            // Apply build time reduction to all buildings
            Map<BuildingType, FactionDefinition.BuildingStatModifier> buildingMods = new HashMap<>();
            for (BuildingType building : config.getSelectedBuildings()) {
                buildingMods.put(building, FactionDefinition.BuildingStatModifier.builder()
                        .buildTimeMultiplier(0.80) // -20%
                        .build());
            }
            builder.buildingStatModifiers(buildingMods);
        }
    };

    private final String displayName;
    private final String description;
    private final int pointCost;
    private final Set<String> dependsOnNames; // Store as names to avoid forward reference issues

    FactionPerk(String displayName, String description, int pointCost, Set<String> dependsOnNames) {
        this.displayName = displayName;
        this.description = description;
        this.pointCost = pointCost;
        this.dependsOnNames = dependsOnNames;
    }

    // ===== PerkEffect Implementation =====

    @Override
    public FactionPerk getPerk() {
        return this;
    }

    // Default: no static modifiers (overridden by enum constants that need them)
    @Override
    public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
        // Default no-op - overridden by constants that need it
    }

    // All lifecycle hooks use defaults from PerkEffect interface
    // (overridden by constants that need them)

    // ===== Existing Methods =====

    /**
     * Get the perks this perk depends on (resolved at runtime)
     */
    public Set<FactionPerk> getDependsOn() {
        return dependsOnNames.stream()
                .map(FactionPerk::valueOf)
                .collect(Collectors.toSet());
    }

    /**
     * Check if this perk's dependencies are satisfied
     */
    public boolean canSelect(Set<FactionPerk> selectedPerks) {
        return selectedPerks.containsAll(getDependsOn());
    }

    /**
     * Get all perks that depend on this one (for UI warnings)
     */
    public Set<FactionPerk> getDependents() {
        Set<FactionPerk> dependents = new HashSet<>();
        for (FactionPerk perk : values()) {
            if (perk.getDependsOn().contains(this)) {
                dependents.add(perk);
            }
        }
        return dependents;
    }

    /**
     * Get the category of this perk (for UI grouping)
     */
    public PerkCategory getCategory() {
        String name = this.name();
        if (name.startsWith("POWER_") || name.startsWith("RESOURCE_") ||
                name.startsWith("COST_") || name.equals("BANK_EFFICIENCY")) {
            return PerkCategory.ECONOMIC;
        }
        if (name.startsWith("UPKEEP_") || name.startsWith("VETERAN_") ||
                name.startsWith("RAPID_") || name.startsWith("DAMAGE_")) {
            return PerkCategory.MILITARY;
        }
        if (name.startsWith("FORTIFIED_") || name.startsWith("SHIELD_") ||
                name.startsWith("TURRET_") || name.startsWith("POINT_") ||
                name.startsWith("BUNKER_")) {
            return PerkCategory.DEFENSIVE;
        }
        if (name.startsWith("ADVANCED_") || name.startsWith("PARALLEL_")) {
            return PerkCategory.TECH;
        }
        return PerkCategory.SPECIALIZED;
    }

    public enum PerkCategory {
        ECONOMIC,
        MILITARY,
        DEFENSIVE,
        TECH,
        SPECIALIZED
    }
}
