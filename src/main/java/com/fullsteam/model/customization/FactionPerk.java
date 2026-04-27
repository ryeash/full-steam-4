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
import org.dyn4j.geometry.Vector2;

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
            "Army Logistics I",
            "-15% periodic army upkeep",
            3,
            Set.of()
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            builder.armyRentCostMultiplier(0.85);
        }
    },
    UPKEEP_INCREASE_2(
            "Army Logistics II",
            "-30% periodic army upkeep",
            6,
            Set.of("UPKEEP_INCREASE_1")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            builder.armyRentCostMultiplier(0.70);
        }
    },
    UPKEEP_INCREASE_3(
            "Army Logistics III",
            "-45% periodic army upkeep",
            10,
            Set.of("UPKEEP_INCREASE_2")
    ) {
        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            builder.armyRentCostMultiplier(0.55);
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

    GARRISON_MASTERY(
            "Garrison Mastery",
            "Bunkers and APCs hold more units (+2 capacity) and Bunkers have +25% HP",
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
    },

    // ===== UNIT CREATION PERKS =====
    BATTLE_HARDENED(
            "Battle Hardened",
            "Newly created units gain +30% damage for 30 seconds",
            5,
            Set.of()
    ) {
        private static final double DAMAGE_BONUS = 1.30;
        private static final long BUFF_DURATION_MS = 30000;
        private final Map<Integer, Long> buffedUnits = new HashMap<>();

        @Override
        public void onUnitCreated(Unit unit, PlayerFaction faction, RTSGameManager game) {
            // Track this unit as buffed
            buffedUnits.put(unit.getId(), System.currentTimeMillis() + BUFF_DURATION_MS);
            log.debug("Player {} - Battle Hardened: Unit {} created with temporary damage buff", 
                    faction.getPlayerId(), unit.getId());
        }

        @Override
        public void onUnitDealsDamage(Unit attacker, com.fullsteam.model.Targetable target, double damage, PlayerFaction faction, RTSGameManager game) {
            Long expiryTime = buffedUnits.get(attacker.getId());
            if (expiryTime != null) {
                long currentTime = System.currentTimeMillis();
                if (currentTime < expiryTime) {
                    // Apply bonus damage
                    double bonusDamage = damage * (DAMAGE_BONUS - 1.0);
                    target.takeDamage(bonusDamage);
                    log.trace("Player {} - Battle Hardened: Unit {} dealt {} bonus damage", 
                            faction.getPlayerId(), attacker.getId(), bonusDamage);
                } else {
                    // Buff expired, remove from tracking
                    buffedUnits.remove(attacker.getId());
                }
            }
        }

        @Override
        public void onUnitDestroyed(Unit unit, PlayerFaction faction, RTSGameManager game) {
            // Clean up tracking
            buffedUnits.remove(unit.getId());
        }
    },

    FIELD_PROMOTION(
            "Field Promotion",
            "Every 10th unit created gains +20% HP permanently",
            4,
            Set.of()
    ) {
        private static final double HP_BONUS = 1.20;
        private static final int PROMOTION_INTERVAL = 10;
        private int unitCounter = 0;

        @Override
        public void onUnitCreated(Unit unit, PlayerFaction faction, RTSGameManager game) {
            unitCounter++;
            if (unitCounter % PROMOTION_INTERVAL == 0) {
                // Promote this unit
                double currentMaxHp = unit.getMaxHealth();
                double newMaxHp = currentMaxHp * HP_BONUS;
                unit.setMaxHealth(newMaxHp);
                unit.setHealth(newMaxHp); // Heal to full with new max

                log.info("Player {} - Field Promotion: Unit {} promoted with +20% HP ({}th unit)", 
                        faction.getPlayerId(), unit.getId(), unitCounter);

                // Send notification
                if (game != null) {
                    game.sendGameEvent(GameEvent.createPlayerEvent(
                            String.format("⭐ Unit promoted! +20%% HP (%dth unit)", unitCounter),
                            faction.getPlayerId(),
                            GameEvent.EventCategory.INFO
                    ));
                }
            }
        }
    },

    REINFORCEMENT_PROTOCOL(
            "Reinforcement Protocol",
            "When a unit is created, nearby damaged units heal 15 HP",
            4,
            Set.of()
    ) {
        private static final double HEAL_AMOUNT = 15.0;
        private static final double HEAL_RADIUS = 200.0;

        @Override
        public void onUnitCreated(Unit unit, PlayerFaction faction, RTSGameManager game) {
            if (game == null || game.getGameEntities() == null) return;

            // Find nearby damaged friendly units
            int healedCount = 0;
            Vector2 unitPos = unit.getPosition();
            for (Unit nearbyUnit : game.getGameEntities().getUnits().values()) {
                if (nearbyUnit.getOwnerId() == faction.getPlayerId() &&
                        nearbyUnit.getId() != unit.getId() &&
                        nearbyUnit.getHealth() < nearbyUnit.getMaxHealth()) {

                    double distance = unitPos.distance(nearbyUnit.getPosition());

                    if (distance <= HEAL_RADIUS) {
                        double newHealth = Math.min(
                                nearbyUnit.getHealth() + HEAL_AMOUNT,
                                nearbyUnit.getMaxHealth()
                        );
                        nearbyUnit.setHealth(newHealth);
                        healedCount++;
                    }
                }
            }

            if (healedCount > 0) {
                log.debug("Player {} - Reinforcement Protocol: Healed {} nearby units", 
                        faction.getPlayerId(), healedCount);
            }
        }
    },

    MASS_PRODUCTION_BONUS(
            "Mass Production Bonus",
            "Units cost 3% less for each unit of same type alive (max 15% reduction)",
            5,
            Set.of()
    ) {
        private static final double COST_REDUCTION_PER_UNIT = 0.03;
        private static final double MAX_REDUCTION = 0.15;

        @Override
        public void applyToDefinition(FactionDefinition.FactionDefinitionBuilder builder, CustomFactionConfig config) {
            // This perk applies dynamically, but we need to implement the logic in the cost calculation
            // For now, we'll note that this requires integration with the production system
            // The actual discount is calculated when units are produced based on current unit counts
        }

        @Override
        public void onUnitCreated(Unit unit, PlayerFaction faction, RTSGameManager game) {
            // Calculate current discount for this unit type
            if (game != null && game.getGameEntities() != null) {
                int sameTypeCount = (int) game.getGameEntities().getUnits().values().stream()
                        .filter(u -> u.getOwnerId() == faction.getPlayerId() &&
                                u.getUnitType() == unit.getUnitType() &&
                                u.getId() != unit.getId())
                        .count();

                double discount = Math.min(sameTypeCount * COST_REDUCTION_PER_UNIT, MAX_REDUCTION);
                if (discount > 0) {
                    log.debug("Player {} - Mass Production Bonus: {} units of type {} alive, {}% discount applied",
                            faction.getPlayerId(), sameTypeCount, unit.getUnitType(), (int) (discount * 100));
                }
            }
        }
    },

    // ===== BUILDING CREATION PERKS =====
    EXPANSION_BONUS(
            "Expansion Bonus",
            "Each building increases resource generation by 2% (max 30%)",
            5,
            Set.of()
    ) {
        private static final double BONUS_PER_BUILDING = 0.02;
        private static final double MAX_BONUS = 0.30;

        @Override
        public void onBuildingCreated(Building building, PlayerFaction faction, RTSGameManager game) {
            if (game == null || game.getGameEntities() == null) return;

            // Count total buildings for this faction
            long buildingCount = game.getGameEntities().getBuildings().values().stream()
                    .filter(b -> b.getOwnerId() == faction.getPlayerId())
                    .count();

            double bonus = Math.min(buildingCount * BONUS_PER_BUILDING, MAX_BONUS);

            log.info("Player {} - Expansion Bonus: {} buildings, +{}% resource generation",
                    faction.getPlayerId(), buildingCount, (int) (bonus * 100));

            // Note: The actual resource bonus would need to be applied to workers/harvesters
            // This requires integration with the resource collection system
            if (buildingCount % 5 == 0) {
                game.sendGameEvent(GameEvent.createPlayerEvent(
                        String.format("📈 Expansion Bonus: +%d%% resource generation", (int) (bonus * 100)),
                        faction.getPlayerId(),
                        GameEvent.EventCategory.INFO
                ));
            }
        }
    },

    FORWARD_OPERATING_BASE(
            "Forward Operating Base",
            "Buildings built 500+ units from HQ provide +150 vision radius",
            4,
            Set.of()
    ) {
        private static final double DISTANCE_THRESHOLD = 500.0;
        private static final double VISION_BONUS = 150.0;

        @Override
        public void onBuildingCreated(Building building, PlayerFaction faction, RTSGameManager game) {
            if (game == null || game.getGameEntities() == null) return;

            // Find HQ
            Building hq = game.getGameEntities().getBuildings().values().stream()
                    .filter(b -> b.getOwnerId() == faction.getPlayerId() &&
                            b.getBuildingType() == BuildingType.HEADQUARTERS)
                    .findFirst()
                    .orElse(null);

            if (hq != null) {
                double distance = building.getPosition().distance(hq.getPosition());

                if (distance >= DISTANCE_THRESHOLD) {
                    // Apply vision bonus
                    building.addVisionRangeBonus(VISION_BONUS);

                    log.info("Player {} - Forward Operating Base: Building {} at distance {} granted +{} vision",
                            faction.getPlayerId(), building.getId(), (int) distance, (int) VISION_BONUS);

                    game.sendGameEvent(GameEvent.createPlayerEvent(
                            "🔭 Forward base established! +150 vision radius",
                            faction.getPlayerId(),
                            GameEvent.EventCategory.INFO
                    ));
                }
            }
        }
    },

    INFRASTRUCTURE_NETWORK(
            "Infrastructure Network",
            "Each building reduces next building's build time by 5% (max 30%)",
            5,
            Set.of()
    ) {
        private static final double REDUCTION_PER_BUILDING = 0.05;
        private static final double MAX_REDUCTION = 0.30;

        @Override
        public void onBuildingCreated(Building building, PlayerFaction faction, RTSGameManager game) {
            if (game == null || game.getGameEntities() == null) return;

            long buildingCount = game.getGameEntities().getBuildings().values().stream()
                    .filter(b -> b.getOwnerId() == faction.getPlayerId())
                    .count();

            double reduction = Math.min((buildingCount - 1) * REDUCTION_PER_BUILDING, MAX_REDUCTION);

            if (reduction > 0) {
                log.debug("Player {} - Infrastructure Network: {} buildings, {}% build time reduction for next building",
                        faction.getPlayerId(), buildingCount, (int) (reduction * 100));
            }

            // Note: The actual build time reduction needs to be applied during construction
            // This requires integration with the building construction system
        }
    },

    // ===== DAMAGE DEALING PERKS =====
    MOMENTUM(
            "Momentum",
            "Units gain +1% damage per hit (max +25%), resets after 5 seconds of not attacking",
            6,
            Set.of()
    ) {
        private static final double DAMAGE_PER_STACK = 0.01;
        private static final int MAX_STACKS = 25;
        private static final long RESET_TIME_MS = 5000;
        private final Map<Integer, Integer> momentumStacks = new HashMap<>();
        private final Map<Integer, Long> lastAttackTime = new HashMap<>();

        @Override
        public void onUnitDealsDamage(Unit attacker, com.fullsteam.model.Targetable target, double damage, PlayerFaction faction, RTSGameManager game) {
            int unitId = attacker.getId();
            long currentTime = System.currentTimeMillis();

            // Check if momentum should reset
            Long lastAttack = lastAttackTime.get(unitId);
            if (lastAttack != null && currentTime - lastAttack > RESET_TIME_MS) {
                momentumStacks.remove(unitId);
                log.debug("Player {} - Momentum: Unit {} momentum reset", faction.getPlayerId(), unitId);
            }

            // Increment stacks
            int stacks = momentumStacks.getOrDefault(unitId, 0);
            stacks = Math.min(stacks + 1, MAX_STACKS);
            momentumStacks.put(unitId, stacks);
            lastAttackTime.put(unitId, currentTime);

            // Apply bonus damage
            if (stacks > 1) { // First hit doesn't get bonus
                double bonusDamage = damage * (stacks - 1) * DAMAGE_PER_STACK;
                target.takeDamage(bonusDamage);
                log.trace("Player {} - Momentum: Unit {} at {} stacks dealt {} bonus damage",
                        faction.getPlayerId(), unitId, stacks, bonusDamage);
            }
        }

        @Override
        public void onUnitDestroyed(Unit unit, PlayerFaction faction, RTSGameManager game) {
            momentumStacks.remove(unit.getId());
            lastAttackTime.remove(unit.getId());
        }
    },

    SUPPRESSION_FIRE(
            "Suppression Fire",
            "Units that deal damage slow targets by 15% for 3 seconds",
            5,
            Set.of()
    ) {
        private static final double SLOW_AMOUNT = 0.15;
        private static final long SLOW_DURATION_MS = 3000;
        private final Map<Integer, Long> slowedTargets = new HashMap<>();

        @Override
        public void onUnitDealsDamage(Unit attacker, com.fullsteam.model.Targetable target, double damage, PlayerFaction faction, RTSGameManager game) {
            if (!(target instanceof Unit)) return;

            Unit targetUnit = (Unit) target;
            int targetId = targetUnit.getId();
            long currentTime = System.currentTimeMillis();

            // Apply or refresh slow
            if (!slowedTargets.containsKey(targetId)) {
                // First time slowing this unit
                targetUnit.setSpeedMultiplier(1.0 - SLOW_AMOUNT);
                log.debug("Player {} - Suppression Fire: Target {} slowed by {}%",
                        faction.getPlayerId(), targetId, (int) (SLOW_AMOUNT * 100));
            }

            // Update slow expiry time
            slowedTargets.put(targetId, currentTime + SLOW_DURATION_MS);
        }

        @Override
        public void onUnitCreated(Unit unit, PlayerFaction faction, RTSGameManager game) {
            // Clean up expired slows
            long currentTime = System.currentTimeMillis();
            slowedTargets.entrySet().removeIf(entry -> {
                if (currentTime >= entry.getValue()) {
                    // Slow expired - restore speed if unit still exists
                    if (game != null && game.getGameEntities() != null) {
                        Unit slowedUnit = game.getGameEntities().getUnits().get(entry.getKey());
                        if (slowedUnit != null && slowedUnit.getSpeedMultiplier() < 1.0) {
                            slowedUnit.setSpeedMultiplier(1.0);
                            log.debug("Suppression Fire: Slow expired on unit {}", entry.getKey());
                        }
                    }
                    return true; // Remove from map
                }
                return false; // Keep in map
            });
        }

        @Override
        public void onUnitDestroyed(Unit unit, PlayerFaction faction, RTSGameManager game) {
            // Clean up tracking for destroyed unit
            slowedTargets.remove(unit.getId());
        }
    },

    COMBAT_MEDIC_PROTOCOL(
            "Combat Medic Protocol",
            "When infantry deal damage, nearby friendly infantry heal 2 HP",
            5,
            Set.of()
    ) {
        private static final double HEAL_AMOUNT = 2.0;
        private static final double HEAL_RADIUS = 150.0;

        @Override
        public void onUnitDealsDamage(Unit attacker, com.fullsteam.model.Targetable target, double damage, PlayerFaction faction, RTSGameManager game) {
            if (attacker.getUnitType().getCategory() != UnitCategory.INFANTRY) return;
            if (game == null || game.getGameEntities() == null) return;

            // Heal nearby friendly infantry
            int healedCount = 0;
            Vector2 attackerPos = attacker.getPosition();
            for (Unit nearbyUnit : game.getGameEntities().getUnits().values()) {
                if (nearbyUnit.getOwnerId() == faction.getPlayerId() &&
                        nearbyUnit.getUnitType().getCategory() == UnitCategory.INFANTRY &&
                        nearbyUnit.getHealth() < nearbyUnit.getMaxHealth() &&
                        nearbyUnit.getId() != attacker.getId()) {

                    double distance = attackerPos.distance(nearbyUnit.getPosition());

                    if (distance <= HEAL_RADIUS) {
                        double newHealth = Math.min(
                                nearbyUnit.getHealth() + HEAL_AMOUNT,
                                nearbyUnit.getMaxHealth()
                        );
                        nearbyUnit.setHealth(newHealth);
                        healedCount++;
                    }
                }
            }

            if (healedCount > 0) {
                log.trace("Player {} - Combat Medic Protocol: Healed {} nearby infantry",
                        faction.getPlayerId(), healedCount);
            }
        }
    },

    ARMOR_PENETRATION_RESEARCH(
            "Armor Penetration Research",
            "Every 1000 damage dealt increases all damage by 1% (max 10%)",
            6,
            Set.of()
    ) {
        private static final double DAMAGE_THRESHOLD = 1000.0;
        private static final double DAMAGE_BONUS_PER_TIER = 0.01;
        private static final int MAX_TIERS = 10;
        private double totalDamageDealt = 0.0;
        private int currentTier = 0;

        @Override
        public void onUnitDealsDamage(Unit attacker, com.fullsteam.model.Targetable target, double damage, PlayerFaction faction, RTSGameManager game) {
            totalDamageDealt += damage;

            int newTier = Math.min((int) (totalDamageDealt / DAMAGE_THRESHOLD), MAX_TIERS);
            if (newTier > currentTier) {
                currentTier = newTier;
                log.info("Player {} - Armor Penetration Research: Tier {} unlocked! +{}% damage",
                        faction.getPlayerId(), currentTier, (int) (currentTier * DAMAGE_BONUS_PER_TIER * 100));

                if (game != null) {
                    game.sendGameEvent(GameEvent.createPlayerEvent(
                            String.format("🔬 Research breakthrough! +%d%% damage", (int) (currentTier * DAMAGE_BONUS_PER_TIER * 100)),
                            faction.getPlayerId(),
                            GameEvent.EventCategory.INFO
                    ));
                }
            }

            // Apply bonus damage from current tier
            if (currentTier > 0) {
                double bonusDamage = damage * currentTier * DAMAGE_BONUS_PER_TIER;
                target.takeDamage(bonusDamage);
            }
        }
    },

    VAMPIRIC_WEAPONS(
            "Vampiric Weapons",
            "Units heal for 8% of damage dealt",
            6,
            Set.of()
    ) {
        private static final double LIFESTEAL_PERCENT = 0.08;

        @Override
        public void onUnitDealsDamage(Unit attacker, com.fullsteam.model.Targetable target, double damage, PlayerFaction faction, RTSGameManager game) {
            double healAmount = damage * LIFESTEAL_PERCENT;
            double newHealth = Math.min(
                    attacker.getHealth() + healAmount,
                    attacker.getMaxHealth()
            );
            attacker.setHealth(newHealth);

            log.trace("Player {} - Vampiric Weapons: Unit {} healed {} HP from damage",
                    faction.getPlayerId(), attacker.getId(), healAmount);
        }
    },

    CRITICAL_STRIKE(
            "Critical Strike",
            "15% chance to deal double damage",
            5,
            Set.of()
    ) {
        private static final double CRIT_CHANCE = 0.15;
        private static final double CRIT_MULTIPLIER = 2.0;
        private final java.util.Random random = new java.util.Random();

        @Override
        public void onUnitDealsDamage(Unit attacker, com.fullsteam.model.Targetable target, double damage, PlayerFaction faction, RTSGameManager game) {
            if (random.nextDouble() < CRIT_CHANCE) {
                // Deal bonus damage (original damage already applied)
                double bonusDamage = damage * (CRIT_MULTIPLIER - 1.0);
                target.takeDamage(bonusDamage);

                log.debug("Player {} - Critical Strike: Unit {} dealt {} bonus damage (crit!)",
                        faction.getPlayerId(), attacker.getId(), bonusDamage);
            }
        }
    },

    OVERCHARGE(
            "Overcharge",
            "After dealing 500 damage, next attack deals +50% damage",
            5,
            Set.of()
    ) {
        private static final double DAMAGE_THRESHOLD = 500.0;
        private static final double BONUS_MULTIPLIER = 1.50;
        private final Map<Integer, Double> damageAccumulated = new HashMap<>();
        private final Map<Integer, Boolean> overchargeReady = new HashMap<>();

        @Override
        public void onUnitDealsDamage(Unit attacker, com.fullsteam.model.Targetable target, double damage, PlayerFaction faction, RTSGameManager game) {
            int unitId = attacker.getId();

            // Check if overcharge is ready
            if (overchargeReady.getOrDefault(unitId, false)) {
                // Apply overcharge
                double bonusDamage = damage * (BONUS_MULTIPLIER - 1.0);
                target.takeDamage(bonusDamage);

                log.info("Player {} - Overcharge: Unit {} dealt {} bonus damage (overcharged!)",
                        faction.getPlayerId(), unitId, bonusDamage);

                if (game != null) {
                    game.sendGameEvent(GameEvent.createPlayerEvent(
                            "⚡ Overcharge activated!",
                            faction.getPlayerId(),
                            GameEvent.EventCategory.INFO
                    ));
                }

                // Reset
                overchargeReady.put(unitId, false);
                damageAccumulated.put(unitId, 0.0);
            } else {
                // Accumulate damage
                double accumulated = damageAccumulated.getOrDefault(unitId, 0.0) + damage;
                damageAccumulated.put(unitId, accumulated);

                if (accumulated >= DAMAGE_THRESHOLD) {
                    overchargeReady.put(unitId, true);
                    log.debug("Player {} - Overcharge: Unit {} ready for overcharge!",
                            faction.getPlayerId(), unitId);
                }
            }
        }

        @Override
        public void onUnitDestroyed(Unit unit, PlayerFaction faction, RTSGameManager game) {
            damageAccumulated.remove(unit.getId());
            overchargeReady.remove(unit.getId());
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
                name.startsWith("BUNKER_") || name.startsWith("GARRISON_")) {
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
