package com.fullsteam.model.component;

import com.fullsteam.model.GameEntities;
import com.fullsteam.model.ShieldSensor;
import com.fullsteam.model.Unit;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

/**
 * Component that handles shield projection for mobile units (Shield Tank).
 * Similar to ShieldComponent for buildings, but attached to a moving unit.
 * Shield moves with the unit and blocks incoming projectiles for nearby allies.
 * Damage to the shield is fed back to the unit (10% of blocked damage).
 */
@Slf4j
@Getter
@Setter
public class ShieldTankComponent implements IUnitComponent {
    public static final double SHIELD_RADIUS = ShieldComponent.DEFAULT_SHIELD_RADIUS * 0.6;

    private Unit unit;
    private GameEntities gameEntities;
    private Body sensorBody = null;
    private final double radius;

    /**
     * Create a shield tank component with default radius.
     */
    public ShieldTankComponent() {
        this(SHIELD_RADIUS);
    }

    /**
     * Create a shield tank component with custom radius.
     *
     * @param radius Shield projection radius
     */
    public ShieldTankComponent(double radius) {
        this.radius = radius;
    }

    @Override
    public void init(Unit unit, GameEntities gameEntities) {
        this.unit = unit;
        this.gameEntities = gameEntities;
        // Shield activates immediately when component is initialized
        activate();
    }

    @Override
    public void update(GameEntities gameEntities) {
        // Update shield position to follow unit
        if (shieldActive() && unit.isActive()) {
            Vector2 unitPos = unit.getPosition();
            sensorBody.getTransform().setTranslation(unitPos.x, unitPos.y);
        }

        // Deactivate shield if unit is destroyed
        if (!unit.isActive() && shieldActive()) {
            deactivate();
        }
    }

    @Override
    public void onDestroy() {
        deactivate();
    }

    /**
     * Check if shield is currently active.
     */
    public boolean shieldActive() {
        return sensorBody != null;
    }

    /**
     * Activate the shield.
     */
    private void activate() {
        if (shieldActive()) {
            return;
        }

        Body sensor = new Body();
        BodyFixture bodyFixture = sensor.addFixture(Geometry.createCircle(radius));
        bodyFixture.setSensor(true); // Make it a sensor (no collision response)
        sensor.setMass(MassType.INFINITE);

        Vector2 unitPos = unit.getPosition();
        sensor.getTransform().setTranslation(unitPos.x, unitPos.y);
        sensor.setUserData(new ShieldSensor(unit)); // Wrap unit in ShieldSensor

        gameEntities.getWorld().addBody(sensor);
        sensorBody = sensor;

        log.debug("Shield Tank {} activated shield with radius {}", unit.getId(), radius);
    }

    /**
     * Deactivate the shield.
     */
    private void deactivate() {
        if (!shieldActive()) {
            return;
        }

        gameEntities.getWorld().removeBody(sensorBody);
        sensorBody = null;

        log.debug("Shield Tank {} deactivated shield", unit.getId());
    }

    /**
     * Check if a position is inside this shield's radius.
     *
     * @param position Position to check
     * @return true if position is inside shield radius
     */
    public boolean isPositionInside(Vector2 position) {
        if (!shieldActive()) {
            return false;
        }
        double distance = unit.getPosition().distance(position);
        return distance <= radius;
    }

    /**
     * Apply damage to the shield tank when the shield blocks a projectile.
     * This is called by the collision processor.
     *
     * @param projectileDamage The damage of the blocked projectile
     */
    public void applyShieldDamage(double projectileDamage) {
        // Shield tank takes 10% of blocked damage (same as building shields)
        double reducedDamage = projectileDamage * 0.10;
        unit.takeDamage(reducedDamage);

        log.debug("Shield Tank {} absorbed {} damage, took {} feedback damage",
                unit.getId(), projectileDamage, reducedDamage);
    }
}
