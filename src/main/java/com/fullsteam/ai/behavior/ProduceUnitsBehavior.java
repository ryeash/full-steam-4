package com.fullsteam.ai.behavior;

import com.fullsteam.ai.context.AiConcernSnapshot;
import com.fullsteam.ai.context.SkirmishAiTickContext;
import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.InputAction;
import com.fullsteam.model.Player;
import com.fullsteam.model.RTSPlayerInput;
import com.fullsteam.model.ResourceType;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.component.ProductionComponent;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Queues production in <em>every</em> idle production building each tick, one unit per building.
 *
 * <p>Key improvements over the original single-building version:
 * <ul>
 *   <li>All idle barracks, factories, and the HQ fill their queues in the same tick.</li>
 *   <li>Unit selection is driven by the faction's actual {@code getUnitTypes()} roster instead of
 *       a hard-coded list, so custom AI factions work correctly.</li>
 *   <li>Falls back to cheaper units when the AI cannot afford the preferred choice, so production
 *       never stalls on insufficient credits.</li>
 * </ul>
 */
@Slf4j
public final class ProduceUnitsBehavior implements SkirmishAiBehavior {

    @Override
    public boolean isReadyThisFrame(SkirmishAiTickContext ctx) {
        return AiTickPhasing.every(ctx.frameCount(), ctx.playerId(),
                ctx.aiProfile().producePeriod(), ctx.aiProfile().produceSalt());
    }

    @Override
    public boolean isApplicable(GameEntities entities, SkirmishAiTickContext ctx, AiConcernSnapshot concerns) {
        return !ctx.faction().isHasLowPower();
    }

    @Override
    public List<RTSPlayerInput> propose(GameEntities entities, SkirmishAiTickContext ctx) {
        Player faction = ctx.faction();
        if (faction.isHasLowPower()) {
            return List.of();
        }

        int playerId = ctx.playerId();
        Set<BuildingType> constructed = entities.getConstructedBuildingTypes(playerId);
        int workerCount = countWorkersOnFieldAndInPipeline(entities, playerId);
        int credits = faction.getResourceAmount(ResourceType.CREDITS);

        List<RTSPlayerInput> results = new ArrayList<>();

        for (Building building : entities.getBuildings().values()) {
            if (!building.belongsTo(playerId) || !building.isActive() || building.isUnderConstruction()) {
                continue;
            }
            Optional<ProductionComponent> pcOpt = building.getComponent(ProductionComponent.class);
            if (pcOpt.isEmpty()) {
                continue;
            }
            ProductionComponent pc = pcOpt.get();
            // Skip buildings already producing or queued
            if (pc.getCurrentProductionUnitType() != null || building.getProductionQueueSize() > 0) {
                continue;
            }

            BuildingType bt = building.getBuildingType();
            UnitType toTrain = pickUnitForBuilding(faction, bt, constructed, credits, workerCount, ctx);
            if (toTrain == null) {
                continue;
            }

            int cost = faction.getUnitCost(toTrain);
            if (credits < cost) {
                continue;
            }

            RTSPlayerInput in = new RTSPlayerInput();
            in.setAction(InputAction.PRODUCE_UNIT);
            in.setUnitType(toTrain);
            in.setTargetEntityId(building.getId());
            results.add(in);

            // Deduct credits speculatively so later buildings in the same tick don't over-commit
            credits -= cost;

            log.debug("AI produce: player {} queuing {} at {} (cost={}, remaining credits={})",
                    playerId, toTrain, bt, cost, credits);
        }

        return results;
    }

    // -------------------------------------------------------------------------
    // Unit selection
    // -------------------------------------------------------------------------

    /**
     * Picks the best unit the AI can afford for {@code building}, using the faction's actual roster.
     * Candidates are sorted cost-descending so the AI prefers powerful units and falls back to
     * cheaper ones when funds are tight.  Returns {@code null} if nothing is affordable/available.
     */
    private static UnitType pickUnitForBuilding(Player faction, BuildingType bt,
                                                Set<BuildingType> constructed, int credits,
                                                int workerCount, SkirmishAiTickContext ctx) {
        if (bt == BuildingType.HEADQUARTERS) {
            return pickWorker(faction, constructed, credits, workerCount, ctx);
        }

        // Gather all units this building type can produce, that the faction has selected
        List<UnitType> candidates = new ArrayList<>();
        for (UnitType ut : faction.getFactionDefinition().getUnitTypes()) {
            if (ut == UnitType.WORKER) {
                continue; // workers only from HQ
            }
            if (ut.getProducedBy() != bt) {
                continue;
            }
            if (!faction.hasRequiredTechBuildings(ut, constructed)) {
                continue;
            }
            if (faction.getUnitCost(ut) > credits) {
                continue;
            }
            candidates.add(ut);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Prefer higher-cost (more powerful) units; secondary sort by ordinal for stability
        candidates.sort(Comparator.comparingInt(faction::getUnitCost).reversed().thenComparingInt(UnitType::ordinal));

        // 25% chance to build the second-best affordable unit — adds composition variety so the
        // AI doesn't always spam the single most expensive unit it can produce.
        if (candidates.size() > 1 && ThreadLocalRandom.current().nextInt(4) == 0) {
            return candidates.get(1);
        }
        return candidates.get(0);
    }

    /**
     * Decides whether to produce another WORKER from the HQ, respecting the worker cap.
     */
    private static UnitType pickWorker(Player faction, Set<BuildingType> constructed,
                                       int credits, int workerCount, SkirmishAiTickContext ctx) {
        if (workerCount >= ctx.aiProfile().maxWorkers()) {
            return null;
        }
        if (!faction.canProduceUnit(UnitType.WORKER)) {
            return null;
        }
        if (!faction.hasRequiredTechBuildings(UnitType.WORKER, constructed)) {
            return null;
        }
        if (faction.getUnitCost(UnitType.WORKER) > credits) {
            return null;
        }
        return UnitType.WORKER;
    }

    // -------------------------------------------------------------------------
    // Worker count
    // -------------------------------------------------------------------------

    private static int countWorkersOnFieldAndInPipeline(GameEntities entities, int playerId) {
        int count = 0;
        for (var unit : entities.getUnits().values()) {
            if (unit.belongsTo(playerId) && unit.isActive() && !unit.isGarrisoned()
                    && unit.getUnitType() == UnitType.WORKER) {
                count++;
            }
        }
        for (Building b : entities.getBuildings().values()) {
            if (!b.belongsTo(playerId) || b.getBuildingType() != BuildingType.HEADQUARTERS) {
                continue;
            }
            Optional<ProductionComponent> pcOpt = b.getComponent(ProductionComponent.class);
            if (pcOpt.isPresent()) {
                count += pcOpt.get().countPipelineUnits(UnitType.WORKER);
            }
        }
        return count;
    }
}
