package com.fullsteam.model;

import lombok.Getter;

@Getter
public enum Ordinance {
    BULLET(0, "Standard projectile", 2.0, 1.0, false, 0.3, 50.0, 0.0),
    ROCKET(20, "High-speed explosive projectile", 4.0, 1.2, true, 1.4, 50.0, 0.0),
    GRENADE(10, "Arcing explosive projectile", 3.0, 0.6, true, 1.2, 20.0, 0.0),
    SHELL(12, "Heavy artillery shell", 3.5, 0.9, true, 1.5, 30.0, 0.0),
    FLAK(15, "Anti-aircraft flak shell", 3.2, 1.0, true, 1.3, 40.0, 0.0),
    LASER(50, "Instant-hit beam weapon", 1.0, 2.0, false, 0.8, 0.0, 0.3);

    private final int pointCost;
    private final String description;
    private final double size; // Radius for rendering and physics
    private final double speedMultiplier; // Affects projectile speed
    private final boolean hasTrail; // Whether to render a trail effect
    private final double areaOfEffectModification; // how much the ordinance alters the size of the AOE bullet effect
    private final double minimumVelocity;
    private final double beamDuration; // How long the beam lasts (seconds)

    Ordinance(int pointCost,
              String description,
              double size,
              double speedMultiplier,
              boolean hasTrail,
              double areaOfEffectModification,
              double minimumVelocity,
              double beamDuration) {
        this.pointCost = pointCost;
        this.description = description;
        this.size = size;
        this.speedMultiplier = speedMultiplier;
        this.hasTrail = hasTrail;
        this.areaOfEffectModification = areaOfEffectModification;
        this.minimumVelocity = minimumVelocity;
        this.beamDuration = beamDuration;
    }
}
