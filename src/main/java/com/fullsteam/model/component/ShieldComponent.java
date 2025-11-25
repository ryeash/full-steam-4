package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.GameEntities;
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
    private GameEntities gameEntities;

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
    public void update(GameEntities gameEntities, Building building, boolean hasLowPower) {
        this.gameEntities = gameEntities;
        // Update shield state (activate/deactivate based on power and construction)
        boolean shouldBeActive = !hasLowPower && !building.isUnderConstruction();
        if (shouldBeActive && !active) {
            activate(building);
        } else if (!shouldBeActive && active) {
            deactivate();
        }
    }

    @Override
    public void onConstructionComplete(Building building) {
        activate(building);
    }

    @Override
    public void onDestroy(Building building) {
        deactivate();
    }

    /**
     * Activate the shield.
     *
     * @param building The building this shield is attached to
     */
    public void activate(Building building) {
        if (sensorBody != null) {
            return;
        }
        Body sensor = new Body();
        BodyFixture bodyFixture = sensor.addFixture(Geometry.createCircle(radius), 0.0, 0.0, 0.0);
        bodyFixture.setSensor(true); // Make it a sensor (no collision response)
        sensor.setMass(MassType.INFINITE);
        sensor.getTransform().setTranslation(building.getPosition().x, building.getPosition().y);
        sensor.setUserData(new ShieldSensor(building)); // Wrap building in ShieldSensor
        gameEntities.getWorld().addBody(sensor);
        active = true;
        sensorBody = sensor;
    }

    /**
     * Deactivate the shield.
     */
    public void deactivate() {
        if (sensorBody == null) {
            return;
        }
        gameEntities.getWorld().removeBody(sensorBody);
        sensorBody = null;
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
     * Check if the shield is active
     */
    public boolean isShieldActive() {
        return sensorBody != null; // No shield component
    }
}

