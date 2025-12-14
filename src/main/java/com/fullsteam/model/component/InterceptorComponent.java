package com.fullsteam.model.component;

import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Unit;
import com.fullsteam.model.command.ReturnToHangarCommand;
import com.fullsteam.model.research.ResearchModifier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Component for Interceptor aircraft - manages fuel, ammunition, and sortie lifecycle.
 * <p>
 * Interceptors have limited operational time and ammunition:
 * - Limited fuel: Can stay airborne for a set duration before returning to hangar
 * - Limited ammo: Can fire 4 missiles before needing to reload
 * - Auto-return: Automatically returns to hangar when fuel or ammo is depleted
 * <p>
 * This creates strategic depth where interceptors must be carefully deployed and managed.
 */
@Slf4j
@Getter
public class InterceptorComponent extends AbstractUnitComponent {

    // Fuel system
    private static final double MAX_FUEL = 60.0; // seconds of flight time
    private double currentFuel;
    private long sortieStartTime = 0; // Absolute timestamp when sortie started
    private boolean lowFuelWarning = false;

    // Ammunition system
    private static final int MAX_AMMO = 12; // 4 seeking missiles
    private int currentAmmo;
    private boolean lowAmmoWarning = false;

    /**
     * -- GETTER --
     * Check if the interceptor is currently on a sortie mission.
     */
    // Mission state
    private boolean onSortie = false;
    /**
     * -- GETTER --
     * Get the ID of the interceptor's home hangar.
     */
    private Integer hangarId; // ID of home hangar

    public InterceptorComponent(Unit unit) {
        // Start with full fuel and ammo when created
        this.currentFuel = MAX_FUEL;
        this.currentAmmo = MAX_AMMO;
    }

    @Override
    public void update(GameEntities gameEntities) {
        if (!onSortie || unit.getCurrentCommand() instanceof ReturnToHangarCommand) {
            return; // Not deployed, no fuel consumption
        }

        // Calculate elapsed time since sortie start (in seconds)
        long currentTime = System.currentTimeMillis();
        double elapsedSeconds = (currentTime - sortieStartTime) / 1000.0;

        // Update current fuel based on elapsed time
        currentFuel = Math.max(0, MAX_FUEL - elapsedSeconds);

        // Low fuel warning at 25%
        if (currentFuel <= MAX_FUEL * 0.25 && currentFuel > 0 && !lowFuelWarning) {
            lowFuelWarning = true;
            log.info("Interceptor {} - LOW FUEL WARNING ({} seconds remaining)",
                    unit.getId(), (int) currentFuel);
        }

        // Out of fuel - must return immediately
        if (currentFuel <= 0) {
            currentFuel = 0;
            log.warn("Interceptor {} - OUT OF FUEL! Returning to hangar.", unit.getId());
            returnToHangar();
        }
    }

    /**
     * Deploy the interceptor on a sortie mission.
     * Starts fuel consumption using absolute timestamp.
     */
    public void deploy(int hangarId) {
        this.hangarId = hangarId;
        this.onSortie = true;
        this.sortieStartTime = System.currentTimeMillis(); // Record absolute start time
        this.currentFuel = MAX_FUEL; // Reset to full
        this.lowFuelWarning = false;
        this.lowAmmoWarning = false;
        log.info("Interceptor {} deployed from hangar {} at {} - Fuel: {}s, Ammo: {}",
                unit.getId(), hangarId, sortieStartTime, (int) currentFuel, currentAmmo);
    }

    /**
     * Called when the interceptor fires a weapon.
     * Decrements ammo count and checks if RTB (return to base) is needed.
     */
    public void onWeaponFired() {
        if (currentAmmo > 0) {
            currentAmmo--;
            log.debug("Interceptor {} fired missile - {} remaining", unit.getId(), currentAmmo);

            // Low ammo warning at 1 missile left
            if (currentAmmo == 1 && !lowAmmoWarning) {
                lowAmmoWarning = true;
                log.info("Interceptor {} - LOW AMMO WARNING (1 missile remaining)", unit.getId());
            }

            // Out of ammo - must return
            if (currentAmmo == 0) {
                log.warn("Interceptor {} - OUT OF AMMO! Returning to hangar.", unit.getId());
                returnToHangar();
            }
        }
    }

    /**
     * Check if the interceptor should return to base.
     * Returns true if fuel or ammo is depleted.
     */
    public boolean shouldReturnToBase() {
        return currentFuel <= 0 || currentAmmo <= 0;
    }

    /**
     * Check if the interceptor is low on fuel (< 25%).
     */
    public boolean isLowFuel() {
        return currentFuel <= MAX_FUEL * 0.25;
    }

    /**
     * Check if the interceptor is low on ammo (<= 1 missile).
     */
    public boolean isLowAmmo() {
        return currentAmmo <= 1;
    }

    /**
     * Get fuel percentage (0-100).
     */
    public int getFuelPercent() {
        return (int) ((currentFuel / MAX_FUEL) * 100);
    }

    /**
     * Return the interceptor to its home hangar.
     * Issues a ReturnToHangarCommand to fly back to the hangar location.
     */
    private void returnToHangar() {
        if (hangarId == null) {
            log.error("Interceptor {} has no hangar ID! Cannot return.", unit.getId());
            return;
        }

        // Don't issue another return command if already returning
        if (unit.getCurrentCommand() instanceof ReturnToHangarCommand) {
            return;
        }

        log.info("Interceptor {} returning to hangar {}", unit.getId(), hangarId);
        ReturnToHangarCommand returnCommand = new ReturnToHangarCommand(unit, hangarId, false);
        unit.issueCommand(returnCommand, gameEntities);
    }

    /**
     * Refuel and rearm the interceptor (called when back in hangar).
     */
    public void refuelAndRearm() {
        currentFuel = MAX_FUEL;
        currentAmmo = MAX_AMMO;
        onSortie = false;
        lowFuelWarning = false;
        lowAmmoWarning = false;
        log.info("Interceptor {} refueled and rearmed - Ready for deployment", unit.getId());
    }

    /**
     * Get remaining flight time in seconds.
     */
    public int getRemainingFlightTime() {
        return (int) Math.max(0, currentFuel);
    }

    @Override
    public void applyResearchModifiers(ResearchModifier modifier) {
        // Future: Could apply research bonuses to fuel capacity or ammo count
        // For now, interceptor stats are fixed
    }
}

