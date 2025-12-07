package com.fullsteam.model.component;

import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Component for Hangar buildings that house sortie-based aircraft.
 * Each hangar can hold one aircraft unit (Bomber, Gunship, etc.)
 * The aircraft doesn't exist in the game world until deployed on a sortie.
 * <p>
 * Key Concepts:
 * - Aircraft are "housed" not "produced" - they persist between sorties
 * - Only one aircraft can be housed per hangar
 * - Aircraft can be repaired and rearmed while housed
 * - When sortie is ordered, aircraft is spawned, executes mission, then returns
 * <p>
 * Used by: HANGAR
 */
@Slf4j
@Getter
public class HangarComponent extends AbstractBuildingComponent {

    // The type of aircraft housed in this hangar (null if empty)
    private Unit housedAircraft;

    // Whether the aircraft is currently deployed on a sortie
    private boolean isOnSortie;

    // Whether the hangar is ready for sortie (aircraft housed, not damaged, not already deployed)
    public boolean isReadyForSortie() {
        return housedAircraft != null && !isOnSortie && housedAircraft.getHealth() > 0;
    }

    /**
     * House a new aircraft in this hangar
     *
     * @param aircraft The type of aircraft to house
     * @return true if successful, false if hangar is already occupied
     */
    public boolean houseAircraft(Unit aircraft) {
        if (housedAircraft != null) {
            log.warn("Hangar {} already has aircraft housed: {}", building.getId(), housedAircraft);
            return false;
        }

        if (!aircraft.getUnitType().isSortieBased()) {
            log.warn("Cannot house non-sortie unit {} in hangar {}", aircraft.getUnitType(), building.getId());
            return false;
        }

        this.housedAircraft = aircraft;
        this.isOnSortie = false;
        this.housedAircraft.getBody().setEnabled(false);
        this.housedAircraft.setGarrisoned(true);

        log.info("Hangar {} now houses {}", building.getId(), aircraft);
        return true;
    }

    /**
     * Mark the aircraft as deployed on a sortie
     *
     * @param unitId The ID of the spawned unit
     */
    public void deployOnSortie(int unitId) {
        if (housedAircraft == null) {
            log.error("Cannot deploy from empty hangar {}", building.getId());
            return;
        }

        if (isOnSortie) {
            log.warn("Hangar {} already has aircraft deployed on sortie", building.getId());
            return;
        }

        this.isOnSortie = true;
        log.info("Hangar {} deployed {} on sortie (unit ID: {})", building.getId(), housedAircraft, unitId);
    }

    /**
     * Aircraft has returned from sortie
     *
     * @param returnedUnit The unit that returned (for health tracking)
     */
    public void returnFromSortie(Unit returnedUnit) {
        if (!isOnSortie) {
            log.warn("Hangar {} received return but no sortie was active", building.getId());
            return;
        }

        if (returnedUnit != null && returnedUnit.getId() == housedAircraft.getId()) {
            this.housedAircraft = returnedUnit;
        } else {
            // Aircraft was destroyed during sortie
            log.warn("Hangar {} aircraft was destroyed during sortie", building.getId());
            this.housedAircraft = null;
        }
        this.isOnSortie = false;
    }

    /**
     * Repair the housed aircraft (when not on sortie)
     *
     * @param repairAmount Amount of health to repair
     */
    public void repairAircraft(double repairAmount) {
        if (housedAircraft == null) {
            return; // No aircraft to repair
        }

        if (isOnSortie) {
            return; // Cannot repair while deployed
        }

        housedAircraft.setHealth(Math.min(housedAircraft.getHealth() + repairAmount, housedAircraft.getMaxHealth()));
    }

    /**
     * Remove the aircraft from the hangar (e.g., if destroyed or scrapped)
     */
    public void clearAircraft() {
        log.info("Hangar {} aircraft cleared", building.getId());
        this.housedAircraft = null;
        this.isOnSortie = false;
    }

    @Override
    public void update(boolean hasLowPower) {
        // Passive repair over time (slow)
        if (!hasLowPower && housedAircraft != null && !isOnSortie) {
            double repairRate = 2.0; // 2 HP per second when not on low power
            double deltaTime = gameEntities.getWorld().getTimeStep().getDeltaTime();
            repairAircraft(repairRate * deltaTime);
        }
    }

    @Override
    public void onDestroy() {
        if (isOnSortie && housedAircraft != null) {
            if (housedAircraft.isActive()) {
                log.warn("Hangar {} destroyed while aircraft on sortie - aircraft will be lost when it tries to return",
                        building.getId());
                // The aircraft will be destroyed when it tries to return (handled in SortieCommand)
            }
            // TODO: what if the unit is NOT on sortie?
        }
    }
}


