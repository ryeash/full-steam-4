package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.Obstacle;
import com.fullsteam.model.Player;
import com.fullsteam.model.ResourceType;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.FactionDefinition;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Component for units that can harvest resources (Workers).
 * Handles the harvesting cycle: finding deposits, gathering resources, returning to refinery.
 */
@Slf4j
@Getter
@Setter
public class HarvestComponent extends AbstractUnitComponent {

    private static final double BASE_MAX_CARRIED_RESOURCES = 100.0;
    private static final double HARVEST_RATE = 10.0; // Resources per second

    private double carriedResources = 0;
    private double maxCarriedResources = BASE_MAX_CARRIED_RESOURCES;
    private Building targetRefinery = null;

    /**
     * Harvest from a harvestable obstacle.
     *
     * @param obstacle The obstacle to harvest from
     * @return true if harvest is complete (full or obstacle depleted), false to continue
     */
    public boolean harvestFromObstacle(Obstacle obstacle) {
        if (carriedResources >= maxCarriedResources) {
            return true;
        }
        if (!obstacle.isHarvestable()) {
            log.warn("Unit {} attempted to harvest from non-harvestable obstacle {}", unit.getId(), obstacle.getId());
            return true;
        }
        double effectiveHarvestRate = HARVEST_RATE;
        if (unit.getFaction() != null && unit.getFaction().getFactionDefinition() != null) {
            Map<UnitType, FactionDefinition.UnitStatModifier> unitMods = unit.getFaction().getFactionDefinition().getUnitStatModifiers();
            if (unitMods != null && unitMods.containsKey(unit.getUnitType())) {
                effectiveHarvestRate *= unitMods.get(unit.getUnitType()).getResourceCollectionMultiplier();
            }
            effectiveHarvestRate *= unit.getFaction().getDynamicResourceMultiplier();
        }
        double harvestAmount = effectiveHarvestRate * getDeltaTime();
        double actualHarvested = obstacle.harvest(harvestAmount);
        carriedResources += actualHarvested;
        return carriedResources >= maxCarriedResources || obstacle.getRemainingResources() <= 0;
    }

    /**
     * Deposit carried resources to a refinery or headquarters.
     *
     * @param dropoff The building to deposit to (REFINERY or HEADQUARTERS)
     * @return true if deposit was successful
     */
    public boolean depositResources(Building dropoff) {
        if (carriedResources <= 0) {
            return false; // Nothing to deposit
        }

        if (dropoff == null || !dropoff.isActive()) {
            log.warn("Unit {} attempted to deposit to inactive/null building", unit.getId());
            return false;
        }

        if (dropoff.getBuildingType() != BuildingType.REFINERY &&
                dropoff.getBuildingType() != BuildingType.HEADQUARTERS) {
            log.warn("Unit {} attempted to deposit to invalid building type {} (building {})",
                    unit.getId(), dropoff.getBuildingType(), dropoff.getId());
            return false;
        }

        // Add resources to the building owner's faction
        Player faction = gameEntities.getPlayerFactions().get(dropoff.getOwnerId());
        if (faction != null) {
            faction.addResources(ResourceType.CREDITS, (int) carriedResources);
            log.debug("Unit {} deposited {} resources to {} {}",
                    unit.getId(), (int) carriedResources, dropoff.getBuildingType(), dropoff.getId());
        }

        carriedResources = 0;
        return true;
    }

    /**
     * Check if this harvester is currently carrying resources.
     *
     * @return true if carrying any resources
     */
    public boolean hasResources() {
        return carriedResources > 0;
    }

    /**
     * Check if this harvester is full.
     *
     * @return true if at max capacity
     */
    public boolean isFull() {
        return carriedResources >= maxCarriedResources;
    }

    @Override
    public void onDestroy() {
        // Drop carried resources (lost on death)
        if (carriedResources > 0) {
            log.debug("Unit {} destroyed while carrying {} resources (lost)", unit.getId(), (int) carriedResources);
            carriedResources = 0;
        }
    }
}

