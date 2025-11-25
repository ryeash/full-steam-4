package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.FieldEffect;
import com.fullsteam.model.FieldEffectType;
import com.fullsteam.model.GameEntities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Component that handles sandstorm field effect for Sandstorm Generator.
 * Creates a persistent damage field that lasts as long as the building is active and powered.
 * The field effect is automatically added to the game world and removed when the building is destroyed.
 * <p>
 * Used by: SANDSTORM_GENERATOR
 */
@Slf4j
@Getter
public class SandstormComponent implements IBuildingComponent {
    private static final double SANDSTORM_RADIUS = 300.0; // Sandstorm damage radius
    private static final double SANDSTORM_DPS = 15.0; // Damage per second (continuous)

    private FieldEffect sandstormFieldEffect = null;
    private boolean wasActiveLastFrame = false;

    @Override
    public void update(GameEntities gameEntities, Building building, boolean hasLowPower) {
        boolean shouldBeActive = !hasLowPower && !building.isUnderConstruction();

        // Activate sandstorm when conditions are met
        if (shouldBeActive && !wasActiveLastFrame) {
            activateSandstorm(building);
        }

        // Deactivate sandstorm when conditions are no longer met
        if (!shouldBeActive && wasActiveLastFrame) {
            deactivateSandstorm();
        }

        wasActiveLastFrame = shouldBeActive;
    }

    @Override
    public void onDestroy(Building building) {
        deactivateSandstorm();
    }

    /**
     * Activate the sandstorm field effect
     */
    private void activateSandstorm(Building building) {
        if (sandstormFieldEffect != null && sandstormFieldEffect.isActive()) {
            return; // Already have an active sandstorm
        }

        // Create a persistent field effect with very long duration
        sandstormFieldEffect = new FieldEffect(
                building.getOwnerId(),
                FieldEffectType.SANDSTORM,
                building.getPosition(),
                SANDSTORM_RADIUS,
                SANDSTORM_DPS,
                FieldEffectType.SANDSTORM.getDefaultDuration(),
                building.getTeamNumber()
        );
    }

    /**
     * Deactivate the sandstorm field effect
     */
    private void deactivateSandstorm() {
        if (sandstormFieldEffect != null) {
            sandstormFieldEffect.setActive(false);
            log.debug("Deactivated sandstorm field effect {}", sandstormFieldEffect.getId());
            sandstormFieldEffect = null;
        }
    }

    /**
     * Check if sandstorm is currently active
     */
    public boolean isSandstormActive() {
        return sandstormFieldEffect != null && sandstormFieldEffect.isActive();
    }
}


