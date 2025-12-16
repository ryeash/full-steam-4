package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.PlayerFaction;
import com.fullsteam.model.ResourceDeposit;
import com.fullsteam.model.ResourceType;
import com.fullsteam.model.research.ResearchModifier;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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

    @Override
    public void update(com.fullsteam.model.GameEntities gameEntities) {
        // Component doesn't do passive harvesting - harvesting is command-driven
        // This method is here for future enhancements (e.g., auto-return when full)
    }

    /**
     * Harvest from a resource deposit.
     *
     * @param deposit The deposit to harvest from
     * @return true if harvest is complete (full or deposit depleted), false to continue
     */
    public boolean harvestFrom(ResourceDeposit deposit) {
        if (carriedResources >= maxCarriedResources) {
            return true; // Full, should return to refinery
        }

        double harvestAmount = HARVEST_RATE * getDeltaTime();
        double actualHarvested = deposit.harvest(harvestAmount);
        carriedResources += actualHarvested;

        // Return true if full or deposit is depleted
        return carriedResources >= maxCarriedResources || actualHarvested == 0;
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

        // Accept both REFINERY and HEADQUARTERS as valid dropoff points
        if (dropoff.getBuildingType() != BuildingType.REFINERY &&
                dropoff.getBuildingType() != BuildingType.HEADQUARTERS) {
            log.warn("Unit {} attempted to deposit to invalid building type {} (building {})",
                    unit.getId(), dropoff.getBuildingType(), dropoff.getId());
            return false;
        }

        // Add resources to the building owner's faction
        PlayerFaction faction = gameEntities.getPlayerFactions().get(dropoff.getOwnerId());
        if (faction != null) {
            faction.addResources(ResourceType.CREDITS, (int) carriedResources);
            log.debug("Unit {} deposited {} resources to {} {}",
                    unit.getId(), (int) carriedResources, dropoff.getBuildingType(), dropoff.getId());
        }

        carriedResources = 0;
        return true;
    }

    /**
     * Set the target dropoff building for this harvester.
     *
     * @param dropoff The building to return resources to (REFINERY or HEADQUARTERS)
     */
    public void setTargetRefinery(Building dropoff) {
        if (dropoff != null &&
                (dropoff.getBuildingType() == BuildingType.REFINERY ||
                        dropoff.getBuildingType() == BuildingType.HEADQUARTERS) &&
                dropoff.isActive()) {
            this.targetRefinery = dropoff;
        } else {
            log.warn("Attempted to set invalid dropoff building for unit {} (type: {})",
                    unit.getId(), dropoff != null ? dropoff.getBuildingType() : "null");
        }
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

    /**
     * Get the percentage of capacity filled (0.0 to 1.0).
     */
    public double getCapacityPercent() {
        return carriedResources / maxCarriedResources;
    }

    @Override
    public void applyResearchModifiers(ResearchModifier modifier) {
        // Apply worker capacity research
        maxCarriedResources = BASE_MAX_CARRIED_RESOURCES + modifier.getWorkerCapacityBonus();
        log.debug("Unit {} harvest capacity updated to {}", unit.getId(), maxCarriedResources);
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

