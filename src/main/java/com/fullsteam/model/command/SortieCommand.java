package com.fullsteam.model.command;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Building;
import com.fullsteam.model.FieldEffect;
import com.fullsteam.model.FieldEffectType;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Command for sortie-based aircraft (Bomber, Gunship, etc.)
 * The aircraft flies to target, executes bombing run, then returns to hangar.
 * <p>
 * Mission Phases:
 * 1. OUTBOUND - Fly to target location
 * 2. ATTACK - Execute bombing run (drop payload)
 * 3. INBOUND - Return to home hangar
 * 4. LANDING - Command completes, aircraft returns to hangar component
 * <p>
 * Unlike regular units, sortie aircraft:
 * - Cannot be directly controlled during mission
 * - Automatically return to base after attack
 * - Are destroyed if hangar is destroyed while deployed
 */
@Slf4j
@Getter
public class SortieCommand extends UnitCommand {

    private enum SortiePhase {
        OUTBOUND,   // Flying to target
        ATTACK,     // Executing attack
        INBOUND,    // Returning to base
        LANDING     // Approaching hangar (mission complete)
    }

    private final Vector2 targetLocation;
    private final int homeHangarId; // Building ID of the hangar this aircraft launched from
    private SortiePhase currentPhase;
    private boolean payloadDelivered;
    private double attackTimer; // Tracks time spent in attack phase

    public SortieCommand(Unit unit, Vector2 targetLocation, int homeHangarId, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.targetLocation = targetLocation.copy();
        this.homeHangarId = homeHangarId;
        this.currentPhase = SortiePhase.OUTBOUND;
        this.payloadDelivered = false;
        this.attackTimer = 0;

        log.info("Bomber {} beginning sortie to ({}, {}), home hangar: {}",
                unit.getId(), targetLocation.x, targetLocation.y, homeHangarId);
    }

    @Override
    public boolean update(double deltaTime) {
        // Check if home hangar still exists
        Building homeHangar = gameEntities.getBuildings().get(homeHangarId);
        if (homeHangar == null || !homeHangar.isActive()) {
            log.warn("Bomber {} home hangar destroyed - aircraft lost", unit.getId());
            unit.setActive(false); // Destroy unit (no base to return to)
            return false; // Command complete (unit destroyed)
        }

        Vector2 currentPos = unit.getPosition();

        switch (currentPhase) {
            case OUTBOUND:
                double distanceToTarget = currentPos.distance(targetLocation);
                if (distanceToTarget < 50.0) { // Within bombing range
                    log.info("Bomber {} reached target, beginning attack run", unit.getId());
                    currentPhase = SortiePhase.ATTACK;
                    attackTimer = 0;
                }
                break;

            case ATTACK:
                attackTimer += deltaTime;
                // Execute bombing run (bombs drop after 1 second in attack phase)
                if (!payloadDelivered && attackTimer >= 1.0) {
                    deliverPayload();
                    payloadDelivered = true;
                }
                // Stay in attack phase for 2 seconds total (simulating flyover)
                if (attackTimer >= 2.0) {
                    log.info("Bomber {} payload delivered, returning to base", unit.getId());
                    currentPhase = SortiePhase.INBOUND;
                }
                break;

            case INBOUND:
                double distanceToHangar = currentPos.distance(homeHangar.getPosition());
                if (distanceToHangar < 60.0) { // Close to hangar
                    log.info("Bomber {} landing at hangar {}", unit.getId(), homeHangarId);
                    currentPhase = SortiePhase.LANDING;
                }
                break;

            case LANDING:
                // Mission complete - return to hangar component
                log.info("Bomber {} sortie complete", unit.getId());
                return false; // Command complete
        }

        return true; // Command continues
    }

    @Override
    public void updateMovement(double deltaTime, List<Unit> nearbyUnits) {
        switch (currentPhase) {
            case OUTBOUND:
                // Fly towards target
                unit.applySteeringForces(targetLocation, nearbyUnits, deltaTime);
                break;

            case ATTACK:
                // Continue flying forward (maintain forward momentum during bombing)
                Vector2 currentVelocity = unit.getBody().getLinearVelocity();
                if (currentVelocity.getMagnitude() < unit.getUnitType().getMovementSpeed() * 0.5) {
                    // Maintain at least 50% speed during attack run
                    Vector2 direction = currentVelocity.getNormalized();
                    unit.getBody().setLinearVelocity(direction.product(unit.getUnitType().getMovementSpeed() * 0.6));
                }
                break;

            case INBOUND:
                // Return to hangar
                Building homeHangar = gameEntities.getBuildings().get(homeHangarId);
                if (homeHangar != null) {
                    unit.applySteeringForces(homeHangar.getPosition(), nearbyUnits, deltaTime);
                }
                break;

            case LANDING:
                // Slow down for landing
                Vector2 velocity = unit.getBody().getLinearVelocity();
                unit.getBody().setLinearVelocity(velocity.product(0.9)); // Decelerate
                break;
        }
    }

    @Override
    public List<AbstractOrdinance> updateCombat(double deltaTime) {
        // Bombers don't use standard combat - payload is delivered via field effect
        return List.of();
    }

    /**
     * Deliver the bomber's payload (create explosion field effects)
     */
    private void deliverPayload() {
        log.info("Bomber {} delivering payload at ({}, {})", unit.getId(), targetLocation.x, targetLocation.y);

        // Create multiple explosive field effects in a pattern
        // Main impact (center)
        FieldEffect mainBomb = new FieldEffect(
                unit.getId(),
                FieldEffectType.EXPLOSION,
                targetLocation.copy(),
                80.0, // Large radius
                unit.getUnitType().getDamage() * 2.0, // High damage (200 for bomber)
                FieldEffectType.EXPLOSION.getDefaultDuration(),
                unit.getTeamNumber()
        );
        gameEntities.add(mainBomb);

        // Secondary impacts (carpet bombing pattern)
        double[] offsets = {-40, -20, 20, 40}; // Spread along flight path
        for (double offset : offsets) {
            Vector2 velocity = unit.getBody().getLinearVelocity().getNormalized();
            Vector2 bombPos = targetLocation.copy().add(velocity.product(offset));

            FieldEffect secondaryBomb = new FieldEffect(
                    unit.getId(),
                    FieldEffectType.EXPLOSION,
                    bombPos,
                    60.0, // Smaller radius
                    unit.getUnitType().getDamage(), // Normal damage (100 for bomber)
                    FieldEffectType.EXPLOSION.getDefaultDuration(),
                    unit.getTeamNumber()
            );
            gameEntities.add(secondaryBomb);
        }
    }

    @Override
    public Vector2 getTargetPosition() {
        return switch (currentPhase) {
            case OUTBOUND, ATTACK -> targetLocation;
            case INBOUND, LANDING -> {
                Building hangar = gameEntities.getBuildings().get(homeHangarId);
                yield hangar != null ? hangar.getPosition() : targetLocation;
            }
        };
    }

    @Override
    public boolean isMoving() {
        return currentPhase != SortiePhase.LANDING;
    }

    @Override
    public String getDescription() {
        return switch (currentPhase) {
            case OUTBOUND -> String.format("Bombing run to (%.1f, %.1f)", targetLocation.x, targetLocation.y);
            case ATTACK -> "Delivering payload";
            case INBOUND -> "Returning to base";
            case LANDING -> "Landing";
        };
    }

    /**
     * Get the current sortie phase (for UI display)
     */
    public String getPhaseDescription() {
        return currentPhase.toString();
    }
}


