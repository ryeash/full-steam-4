package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
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
    private UnitType housedAircraftType;
    
    // Current health of the housed aircraft (for repairs between sorties)
    private double housedAircraftHealth;
    
    // Whether the aircraft is currently deployed on a sortie
    private boolean isOnSortie;
    
    // The ID of the deployed unit (only valid when isOnSortie = true)
    private Integer deployedUnitId;
    
    // Whether the hangar is ready for sortie (aircraft housed, not damaged, not already deployed)
    public boolean isReadyForSortie() {
        return housedAircraftType != null && !isOnSortie && housedAircraftHealth > 0;
    }
    
    /**
     * House a new aircraft in this hangar
     * @param aircraftType The type of aircraft to house
     * @return true if successful, false if hangar is already occupied
     */
    public boolean houseAircraft(UnitType aircraftType) {
        if (housedAircraftType != null) {
            log.warn("Hangar {} already has aircraft housed: {}", building.getId(), housedAircraftType);
            return false;
        }
        
        if (!aircraftType.isSortieBased()) {
            log.warn("Cannot house non-sortie unit {} in hangar {}", aircraftType, building.getId());
            return false;
        }
        
        this.housedAircraftType = aircraftType;
        this.housedAircraftHealth = aircraftType.getMaxHealth();
        this.isOnSortie = false;
        this.deployedUnitId = null;
        
        log.info("Hangar {} now houses {}", building.getId(), aircraftType);
        return true;
    }
    
    /**
     * Mark the aircraft as deployed on a sortie
     * @param unitId The ID of the spawned unit
     */
    public void deployOnSortie(int unitId) {
        if (housedAircraftType == null) {
            log.error("Cannot deploy from empty hangar {}", building.getId());
            return;
        }
        
        if (isOnSortie) {
            log.warn("Hangar {} already has aircraft deployed on sortie", building.getId());
            return;
        }
        
        this.isOnSortie = true;
        this.deployedUnitId = unitId;
        log.info("Hangar {} deployed {} on sortie (unit ID: {})", building.getId(), housedAircraftType, unitId);
    }
    
    /**
     * Aircraft has returned from sortie
     * @param returnedUnit The unit that returned (for health tracking)
     */
    public void returnFromSortie(Unit returnedUnit) {
        if (!isOnSortie) {
            log.warn("Hangar {} received return but no sortie was active", building.getId());
            return;
        }
        
        if (returnedUnit != null && returnedUnit.getId() == deployedUnitId) {
            // Update aircraft health based on damage taken during sortie
            this.housedAircraftHealth = returnedUnit.getHealth();
            log.info("Hangar {} aircraft returned with {}/{} health", 
                building.getId(), housedAircraftHealth, housedAircraftType.getMaxHealth());
        } else {
            // Aircraft was destroyed during sortie
            log.warn("Hangar {} aircraft was destroyed during sortie", building.getId());
            this.housedAircraftHealth = 0;
        }
        
        this.isOnSortie = false;
        this.deployedUnitId = null;
    }
    
    /**
     * Repair the housed aircraft (when not on sortie)
     * @param repairAmount Amount of health to repair
     */
    public void repairAircraft(double repairAmount) {
        if (housedAircraftType == null) {
            return; // No aircraft to repair
        }
        
        if (isOnSortie) {
            return; // Cannot repair while deployed
        }
        
        double maxHealth = housedAircraftType.getMaxHealth();
        housedAircraftHealth = Math.min(housedAircraftHealth + repairAmount, maxHealth);
    }
    
    /**
     * Remove the aircraft from the hangar (e.g., if destroyed or scrapped)
     */
    public void clearAircraft() {
        log.info("Hangar {} aircraft cleared", building.getId());
        this.housedAircraftType = null;
        this.housedAircraftHealth = 0;
        this.isOnSortie = false;
        this.deployedUnitId = null;
    }
    
    @Override
    public void update(boolean hasLowPower) {
        // Passive repair over time (slow)
        if (!hasLowPower && housedAircraftType != null && !isOnSortie) {
            double repairRate = 2.0; // 2 HP per second when not on low power
            double deltaTime = gameEntities.getWorld().getTimeStep().getDeltaTime();
            repairAircraft(repairRate * deltaTime);
        }
    }
    
    @Override
    public void onDestroy() {
        if (isOnSortie && deployedUnitId != null) {
            // If hangar is destroyed while aircraft is on sortie, aircraft is lost
            Unit deployedUnit = gameEntities.getUnits().get(deployedUnitId);
            if (deployedUnit != null && deployedUnit.isActive()) {
                log.warn("Hangar {} destroyed while aircraft on sortie - aircraft will be lost when it tries to return", 
                    building.getId());
                // The aircraft will be destroyed when it tries to return (handled in SortieCommand)
            }
        }
    }
}


