package com.fullsteam.model.command;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.Unit;
import com.fullsteam.model.component.GunshipComponent;
import com.fullsteam.model.component.HangarComponent;
import com.fullsteam.model.component.InterceptorComponent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Command for sortie-based aircraft to return to their home hangar.
 * Used by both Bombers and Interceptors when their mission is complete
 * or they need to refuel/rearm.
 * <p>
 * The aircraft will:
 * 1. Fly back to the hangar location
 * 2. Land at the hangar (disable physics body)
 * 3. Trigger hangar component to house the aircraft
 * 4. Refuel/rearm if applicable
 */
@Slf4j
@Getter
public class ReturnToHangarCommand extends UnitCommand {

    private final int hangarId;
    private Building hangar;
    private boolean initialized = false;

    public ReturnToHangarCommand(Unit unit, int hangarId, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.hangarId = hangarId;
    }

    @Override
    public boolean update(double deltaTime) {
        // Initialize hangar reference
        if (!initialized) {
            hangar = gameEntities.getBuildings().get(hangarId);
            if (hangar == null) {
                log.error("Hangar {} not found! Aircraft {} cannot return.", hangarId, unit.getId());
                unit.setActive(false);
                return false; // Command failed
            }
            if (hangar.getBuildingType() != BuildingType.HANGAR) {
                log.error("Building {} is not a hangar! Aircraft {} cannot land.", hangarId, unit.getId());
                unit.setActive(false);
                return false;
            }
            initialized = true;
            log.info("Aircraft {} returning to hangar {} at ({}, {})",
                    unit.getId(), hangarId, (int) hangar.getPosition().x, (int) hangar.getPosition().y);
        }

        // Check if hangar is still valid
        if (!hangar.isActive()) {
            log.warn("Hangar {} destroyed! Aircraft {} has nowhere to land.", hangarId, unit.getId());
            // TODO: Find alternative hangar or crash-land
            return false; // Command failed
        }

        // Check if we've reached the hangar
        Vector2 currentPos = unit.getPosition();
        Vector2 hangarPos = hangar.getPosition();
        double distance = currentPos.distance(hangarPos);

        if (distance < 30.0) { // Landing threshold
            landAtHangar();
            return false; // Command complete
        }

        return true; // Continue flying back
    }

    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        if (hangar == null) {
            return;
        }

        // Fly towards hangar
        Vector2 currentPos = unit.getPosition();
        Vector2 hangarPos = hangar.getPosition();
        Vector2 direction = hangarPos.copy().subtract(currentPos);
        direction.normalize();

        // Set velocity towards hangar
        double speed = unit.getUnitType().getMovementSpeed();
        unit.getBody().setLinearVelocity(
                direction.x * speed,
                direction.y * speed
        );

        // Face movement direction
        unit.setRotation(Math.atan2(direction.y, direction.x));
    }

    /**
     * Land the aircraft at the hangar and trigger housing logic.
     */
    private void landAtHangar() {
        log.info("Aircraft {} landing at hangar {}", unit.getId(), hangarId);

        // Get hangar component
        HangarComponent hangarComponent = hangar.getComponent(HangarComponent.class).orElse(null);
        if (hangarComponent == null) {
            log.error("Hangar {} has no HangarComponent!", hangarId);
            return;
        }

        // House the aircraft
        hangarComponent.returnFromSortie(unit);

        // Refuel and rearm if this is an interceptor
        unit.getComponent(InterceptorComponent.class).ifPresent(comp -> {
            comp.refuelAndRearm();
            log.info("Interceptor {} refueled and rearmed", unit.getId());
        });

        // Refuel and rearm if this is a gunship
        unit.getComponent(GunshipComponent.class).ifPresent(comp -> {
            comp.refuelAndRearm();
            log.info("Gunship {} refueled and rearmed", unit.getId());
        });

        log.info("Aircraft {} successfully housed in hangar {}", unit.getId(), hangarId);
    }

    @Override
    public List<AbstractOrdinance> updateCombat(double deltaTime) {
        // No combat while returning to base
        return List.of();
    }

    @Override
    public String getDescription() {
        if (hangar != null) {
            return String.format("Returning to Hangar (%.0f, %.0f)",
                    hangar.getPosition().x, hangar.getPosition().y);
        }
        return "Returning to Hangar";
    }
}

