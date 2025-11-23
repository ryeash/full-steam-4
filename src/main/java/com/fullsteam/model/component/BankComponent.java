package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import lombok.Getter;

/**
 * Component that handles banking and interest generation for buildings.
 * Banks accumulate interest over time and pay it out at regular intervals.
 * Interest generation is paused when the building has low power or is under construction.
 * 
 * Used by: BANK
 */
@Getter
public class BankComponent implements IBuildingComponent {
    private static final double DEFAULT_INTEREST_INTERVAL = 30.0; // Pay interest every 30 seconds
    private static final double DEFAULT_INTEREST_RATE = 0.02; // 2% interest per interval
    
    private double interestTimer = 0; // Time accumulator for interest payments
    private final double interestInterval;
    private final double interestRate;
    
    /**
     * Create a bank component with default interest settings.
     */
    public BankComponent() {
        this(DEFAULT_INTEREST_INTERVAL, DEFAULT_INTEREST_RATE);
    }
    
    /**
     * Create a bank component with custom interest settings.
     * 
     * @param interestInterval Time in seconds between interest payments
     * @param interestRate Interest rate as a decimal (0.02 = 2%)
     */
    public BankComponent(double interestInterval, double interestRate) {
        this.interestInterval = interestInterval;
        this.interestRate = interestRate;
    }
    
    @Override
    public void update(double deltaTime, Building building, boolean hasLowPower) {
        // Don't accumulate interest if under construction or low power
        if (building.isUnderConstruction() || hasLowPower) {
            return;
        }
        
        // Accumulate interest timer
        interestTimer += deltaTime;
    }
    
    /**
     * Check if bank is ready to pay interest and reset timer if so.
     * 
     * @return The interest rate to apply (0.0 if not ready)
     */
    public double checkAndResetInterest() {
        if (interestTimer >= interestInterval) {
            interestTimer = 0;
            return interestRate;
        }
        return 0.0;
    }
    
    /**
     * Get the progress towards next interest payment as a percentage (0.0 to 1.0).
     * 
     * @return Interest timer progress
     */
    public double getInterestProgress() {
        return Math.min(1.0, interestTimer / interestInterval);
    }
    
    /**
     * Get the time remaining until next interest payment.
     * 
     * @return Time in seconds until next interest payment
     */
    public double getTimeUntilNextInterest() {
        return Math.max(0, interestInterval - interestTimer);
    }
}

