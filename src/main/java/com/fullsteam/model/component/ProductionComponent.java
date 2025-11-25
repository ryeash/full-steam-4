package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.UnitType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Component that handles unit production for buildings.
 * Buildings with this component can queue and produce units over time.
 * Production is paused when the building has low power.
 * 
 * Used by: HEADQUARTERS, BARRACKS, FACTORY, WEAPONS_DEPOT, ADVANCED_FACTORY
 */
@Slf4j
@Getter
public class ProductionComponent implements IBuildingComponent {
    private final Queue<ProductionOrder> productionQueue = new LinkedList<>();
    private ProductionOrder currentProduction = null;
    private double productionProgress = 0; // seconds
    private Vector2 rallyPoint;
    
    /**
     * Create a production component with a default rally point.
     * 
     * @param initialRallyPoint The initial rally point for produced units
     */
    public ProductionComponent(Vector2 initialRallyPoint) {
        this.rallyPoint = initialRallyPoint != null ? initialRallyPoint.copy() : null;
    }
    
    @Override
    public void update(double deltaTime, Building building, boolean hasLowPower) {
        // Start next production if none active
        if (currentProduction == null && !productionQueue.isEmpty()) {
            currentProduction = productionQueue.poll();
            productionProgress = 0;
            log.info("Building {} started producing {}", building.getId(), currentProduction.unitType);
        }
        
        // Update current production (only if not low power)
        if (currentProduction != null && !hasLowPower) {
            productionProgress += deltaTime;
            
            // Check if production is complete
            if (productionProgress >= currentProduction.unitType.getBuildTimeSeconds()) {
                log.info("Building {} completed producing {} (progress: {}/{})", 
                        building.getId(), currentProduction.unitType, productionProgress, 
                        currentProduction.unitType.getBuildTimeSeconds());
                // Production complete - unit will be spawned by RTSGameManager
                // Don't clear it here - let getCompletedUnitType() handle it
            }
        } else if (hasLowPower && currentProduction != null) {
            // Production is paused due to low power
            log.debug("Building {} production paused due to LOW POWER", building.getId());
        }
    }
    
    /**
     * Queue a unit for production.
     * Note: Validation should be done by the caller (RTSGameManager) using faction tech tree
     * 
     * @param unitType The type of unit to produce
     * @return true if successfully queued
     */
    public boolean queueUnitProduction(UnitType unitType) {
        productionQueue.add(new ProductionOrder(unitType));
        return true;
    }
    
    /**
     * Cancel current production.
     * 
     * @return The unit type that was cancelled, or null if no production active
     */
    public UnitType cancelCurrentProduction() {
        if (currentProduction != null) {
            UnitType cancelled = currentProduction.unitType;
            currentProduction = null;
            productionProgress = 0;
            return cancelled;
        }
        return null;
    }
    
    /**
     * Get production progress as a percentage (0.0 to 1.0).
     * 
     * @return Production progress, or 0 if no production active
     */
    public double getProductionPercent() {
        if (currentProduction == null) {
            return 0;
        }
        return productionProgress / currentProduction.unitType.getBuildTimeSeconds();
    }
    
    /**
     * Get the number of units in the production queue (excluding current production).
     * 
     * @return Number of queued units
     */
    public int getProductionQueueSize() {
        return productionQueue.size();
    }
    
    /**
     * Check if a unit is ready to be spawned.
     * 
     * @return true if production is complete and ready to spawn
     */
    public boolean hasCompletedUnit() {
        return currentProduction != null && 
               productionProgress >= currentProduction.unitType.getBuildTimeSeconds();
    }
    
    /**
     * Get the completed unit type and clear production.
     * This method consumes the completed production.
     * 
     * @return The completed unit type, or null if no production complete
     */
    public UnitType getCompletedUnitType() {
        if (hasCompletedUnit()) {
            UnitType completed = currentProduction.unitType;
            currentProduction = null;
            productionProgress = 0;
            return completed;
        }
        return null;
    }
    
    /**
     * Set the rally point for produced units.
     * 
     * @param point The new rally point
     */
    public void setRallyPoint(Vector2 point) {
        this.rallyPoint = point != null ? point.copy() : null;
    }
    
    /**
     * Represents a unit production order in the queue.
     */
    @Getter
    public static class ProductionOrder {
        private final UnitType unitType;
        private final long queueTime;
        
        public ProductionOrder(UnitType unitType) {
            this.unitType = unitType;
            this.queueTime = System.currentTimeMillis();
        }
    }
}



