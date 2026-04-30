package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Player;
import com.fullsteam.model.RTSPlayerInput;
import com.fullsteam.model.ResourceType;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.component.ProductionComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ProduceUnitsBehavior implements SkirmishAiBehavior {

    @Override
    public boolean isReadyThisFrame(SkirmishAiTickContext ctx) {
        return AiTickPhasing.every(ctx.frameCount(), ctx.playerId(), ctx.aiProfile().producePeriod(), ctx.aiProfile().produceSalt());
    }

    @Override
    public boolean isApplicable(GameEntities entities, SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        return !ctx.faction().isHasLowPower();
    }

    @Override
    public Optional<RTSPlayerInput> propose(GameEntities entities, SkirmishAiTickContext ctx) {
        var faction = ctx.faction();
        int playerId = ctx.playerId();
        if (faction.isHasLowPower()) {
            return Optional.empty();
        }
        var constructed = entities.getConstructedBuildingTypes(playerId);

        List<Building> producers = new ArrayList<>();
        for (Building building : entities.getBuildings().values()) {
            if (!building.belongsTo(playerId) || !building.isActive() || building.isUnderConstruction()) {
                continue;
            }
            Optional<ProductionComponent> pcOpt = building.getComponent(ProductionComponent.class);
            if (pcOpt.isEmpty()) {
                continue;
            }
            ProductionComponent pc = pcOpt.get();
            if (pc.getCurrentProductionUnitType() != null || building.getProductionQueueSize() > 0) {
                continue;
            }
            BuildingType bt = building.getBuildingType();
            if (bt != BuildingType.BARRACKS && bt != BuildingType.FACTORY && bt != BuildingType.HEADQUARTERS) {
                continue;
            }
            producers.add(building);
        }

        producers.sort(Comparator.comparingInt(b -> productionBuildingPriority(b.getBuildingType())));

        int workersCommitted = countWorkersOnFieldAndInHqPipeline(entities, playerId);

        for (Building building : producers) {
            ProductionComponent pc = building.getComponent(ProductionComponent.class).orElseThrow();
            BuildingType bt = building.getBuildingType();
            UnitType toTrain = null;
            if (bt == BuildingType.BARRACKS && faction.canProduceUnit(UnitType.INFANTRY)) {
                toTrain = UnitType.INFANTRY;
            } else if (bt == BuildingType.FACTORY) {
                toTrain = pickFactoryUnit(faction, constructed, faction.getResourceAmount(ResourceType.CREDITS));
            } else if (bt == BuildingType.HEADQUARTERS && faction.canProduceUnit(UnitType.WORKER)) {
                if (workersCommitted >= ctx.aiProfile().maxWorkers()) {
                    continue;
                }
                toTrain = UnitType.WORKER;
            }
            if (toTrain == null) {
                continue;
            }
            if (!faction.hasRequiredTechBuildings(toTrain, constructed)) {
                continue;
            }
            int cost = faction.getUnitCost(toTrain);
            if (faction.getResourceAmount(ResourceType.CREDITS) < cost) {
                continue;
            }
            RTSPlayerInput in = new RTSPlayerInput();
            in.setProduceUnitOrder(toTrain);
            in.setProduceBuildingId(building.getId());
            return Optional.of(in);
        }
        return Optional.empty();
    }

    /**
     * Barracks, then factory, then HQ so cheap infantry and vehicles are not starved by worker production.
     */
    private static int productionBuildingPriority(BuildingType bt) {
        if (bt == BuildingType.BARRACKS) {
            return 0;
        }
        if (bt == BuildingType.FACTORY) {
            return 1;
        }
        if (bt == BuildingType.HEADQUARTERS) {
            return 2;
        }
        return 3;
    }

    /** Prefer main battle line, then AA, then cheap scout — must match {@link com.fullsteam.ai.AiSkirmishFaction} roster. */
    private static final UnitType[] FACTORY_PRODUCTION_ORDER = {
            UnitType.TANK, UnitType.FLAK_TANK, UnitType.JEEP
    };

    private static UnitType pickFactoryUnit(Player faction, Set<BuildingType> constructed, int credits) {
        for (UnitType candidate : FACTORY_PRODUCTION_ORDER) {
            if (!faction.canProduceUnit(candidate)) {
                continue;
            }
            if (!faction.hasRequiredTechBuildings(candidate, constructed)) {
                continue;
            }
            if (credits < faction.getUnitCost(candidate)) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private static int countWorkersOnFieldAndInHqPipeline(GameEntities entities, int playerId) {
        int field = 0;
        for (var unit : entities.getUnits().values()) {
            if (!unit.belongsTo(playerId) || !unit.isActive() || unit.isGarrisoned()) {
                continue;
            }
            if (unit.getUnitType() == UnitType.WORKER) {
                field++;
            }
        }
        int pipeline = 0;
        for (Building b : entities.getBuildings().values()) {
            if (!b.belongsTo(playerId) || b.getBuildingType() != BuildingType.HEADQUARTERS) {
                continue;
            }
            Optional<ProductionComponent> pcOpt = b.getComponent(ProductionComponent.class);
            if (pcOpt.isEmpty()) {
                continue;
            }
            pipeline += pcOpt.get().countPipelineUnits(UnitType.WORKER);
        }
        return field + pipeline;
    }
}
