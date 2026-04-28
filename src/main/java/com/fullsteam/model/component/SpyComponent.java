package com.fullsteam.model.component;

import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Component for Spy units. Handles permanent automatic cloaking and detection mechanics.
 */
@Slf4j
@Getter
public class SpyComponent extends AbstractUnitComponent {

    @Override
    public void init(Unit unit, GameEntities gameEntities) {
        super.init(unit, gameEntities);
        enableCloak();
    }

    @Override
    public void update(GameEntities gameEntities) {
        if (unit.isActive()) {
            enableCloak();
        }
    }

    private void enableCloak() {
        // Auto-cloak system: Check if we should re-cloak after detection
        if (!unit.isCloaked()) {
            unit.getComponent(CloakComponent.class).ifPresent(CloakComponent::toggleCloak);
        }
    }
}
