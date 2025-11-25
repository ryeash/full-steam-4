package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.ShieldSensor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

/**
 * Component that handles shield projection for buildings.
 * Shield generators project a protective shield in a radius around themselves.
 * Shields block incoming projectiles and can be deactivated by low power.
 * <p>
 * Used by: SHIELD_GENERATOR
 */
@Slf4j
@Getter
@Setter
public class ShieldComponent implements IBuildingComponent {
    private static final double DEFAULT_SHIELD_RADIUS = 200.0;

    private Body sensorBody = null;
    private boolean active = false;
    private final double radius;

    /**
     * Create a shield component with default radius.
     */
    public ShieldComponent() {
        this(DEFAULT_SHIELD_RADIUS);
    }

    /**
     * Create a shield component with custom radius.
     *
     * @param radius Shield projection radius
     */
    public ShieldComponent(double radius) {
        this.radius = radius;
    }

    @Override
    public void update(double deltaTime, Building building, boolean hasLowPower) {
        // Update shield state (activate/deactivate based on power and construction)
        boolean shouldBeActive = !hasLowPower && !building.isUnderConstruction();

        if (shouldBeActive && !active) {
            activate(building);
        } else if (!shouldBeActive && active) {
            deactivate(building);
        }
    }

    @Override
    public void onConstructionComplete(Building building) {
        // Shield activates when construction completes (if there's power)
        if (!active) {
            activate(building);
        }
    }

    @Override
    public void onDestroy(Building building) {
        // Shield deactivates when building is destroyed
        if (active) {
            deactivate(building);
        }
    }

    /**
     * Create the shield sensor body for collision detection.
     * This body is used to detect projectiles entering the shield radius.
     *
     * @param building The building this shield is attached to
     * @return Shield sensor body
     */
    public Body createSensorBody(Building building) {
        Body sensor = new Body();
        BodyFixture bodyFixture = sensor.addFixture(Geometry.createCircle(radius), 0.0, 0.0, 0.0);
        bodyFixture.setSensor(true); // Make it a sensor (no collision response)
        sensor.setMass(MassType.INFINITE);
        sensor.getTransform().setTranslation(building.getPosition().x, building.getPosition().y);
        sensor.setUserData(new ShieldSensor(building)); // Wrap building in ShieldSensor
        return sensor;
    }

    /**
     * Activate the shield.
     *
     * @param building The building this shield is attached to
     */
    public void activate(Building building) {
        if (active) {
            return;
        }

        active = true;
        log.debug("Shield activated for building {}", building.getId());
    }

    /**
     * Deactivate the shield.
     *
     * @param building The building this shield is attached to
     */
    public void deactivate(Building building) {
        if (!active) {
            return;
        }

        active = false;
        log.debug("Shield deactivated for building {}", building.getId());
    }

    /**
     * Check if a position is inside this shield's radius.
     *
     * @param position Position to check
     * @param building The building this shield is attached to
     * @return true if position is inside shield radius
     */
    public boolean isPositionInside(Vector2 position, Building building) {
        if (!active) {
            return false;
        }

        double distance = building.getPosition().distance(position);
        return distance <= radius;
    }

    /**
     * Check if the shield is currently active.
     *
     * @return true if shield is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Get the shield coverage as a percentage (always 100% for binary shields).
     * This could be extended in the future for shields with energy/strength.
     *
     * @return 1.0 if active, 0.0 if inactive
     */
    public double getCoveragePercent() {
        return active ? 1.0 : 0.0;
    }
}

