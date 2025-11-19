package com.fullsteam.model;

import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;

/**
 * Represents a resource deposit that workers can harvest from.
 */
@Getter
@Setter
public class ResourceDeposit extends GameEntity {
    private final ResourceType resourceType;
    private double remainingResources;
    private final double maxResources;
    private static final double HARVEST_RANGE = 50.0;
    
    public ResourceDeposit(int id, ResourceType resourceType, double x, double y, double initialResources) {
        super(id, createDepositBody(x, y), 1); // Deposits have 1 health (indestructible)
        this.resourceType = resourceType;
        this.remainingResources = initialResources;
        this.maxResources = initialResources;
    }
    
    private static Body createDepositBody(double x, double y) {
        Body body = new Body();
        body.addFixture(Geometry.createCircle(40.0)); // 40 unit radius
        body.setMass(MassType.INFINITE); // Deposits don't move
        body.getTransform().setTranslation(x, y);
        return body;
    }
    
    /**
     * Harvest resources from this deposit
     * @param amount Amount to harvest
     * @return Actual amount harvested (may be less if deposit is depleted)
     */
    public double harvest(double amount) {
        if (remainingResources <= 0) {
            active = false;
            return 0;
        }
        double harvested = Math.min(amount, remainingResources);
        remainingResources -= harvested;
        if (remainingResources <= 0) {
            active = false; // Deposit is depleted
        }
        return harvested;
    }
    
    /**
     * Get remaining resources as a percentage
     */
    public double getResourcePercent() {
        return (double) remainingResources / maxResources;
    }
    
    /**
     * Get harvest range
     */
    public double getHarvestRange() {
        return HARVEST_RANGE;
    }
}

