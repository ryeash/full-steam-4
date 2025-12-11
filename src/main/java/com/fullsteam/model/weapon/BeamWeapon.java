package com.fullsteam.model.weapon;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Beam;
import com.fullsteam.model.BulletEffect;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Obstacle;
import com.fullsteam.model.Ordinance;
import com.fullsteam.model.ShieldSensor;
import com.fullsteam.model.component.ShieldComponent;
import com.fullsteam.model.research.ResearchModifier;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.DetectFilter;
import org.dyn4j.world.World;
import org.dyn4j.world.result.RaycastResult;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Weapon that fires instant-hit beams (lasers, plasma, ion, etc.).
 * Beams use raycasting to determine impact point and have a visual duration.
 */
@Getter
@Setter
public class BeamWeapon extends Weapon {
    private double beamWidth; // Visual thickness
    private double beamDuration; // How long the beam is visible (seconds)
    private Beam.BeamType beamType; // Visual type (LASER, PLASMA, ION, PARTICLE)
    private Ordinance ordinanceType; // For damage calculation purposes
    private Set<BulletEffect> bulletEffects; // Special effects (ELECTRIC, etc.)

    /**
     * Create a beam weapon with full configuration.
     */
    public BeamWeapon(double damage,
                      double range,
                      double attackRate,
                      double beamWidth,
                      double beamDuration,
                      Beam.BeamType beamType,
                      Ordinance ordinanceType,
                      Set<BulletEffect> bulletEffects,
                      ElevationTargeting elevationTargeting) {
        super(damage, range, attackRate, elevationTargeting);
        this.beamWidth = beamWidth;
        this.beamDuration = beamDuration;
        this.beamType = beamType;
        this.ordinanceType = ordinanceType;
        this.bulletEffects = bulletEffects != null ? Set.copyOf(bulletEffects) : Set.of();
    }

    @Override
    protected List<AbstractOrdinance> createOrdinances(Vector2 position,
                                                       Vector2 targetPosition,
                                                       int ownerId,
                                                       int ownerTeam,
                                                       Body ignoredBody,
                                                       GameEntities gameEntities) {
        // Beams require the world for raycasting
        World<Body> world = gameEntities.getWorld();
        if (world == null) {
            return List.of();
        }

        // Calculate direction to target
        Vector2 direction = targetPosition.copy().subtract(position);
        double distanceToTarget = direction.getMagnitude();
        direction.normalize();

        // Use the minimum of weapon range and distance to target
        double effectiveRange = Math.min(range, distanceToTarget);

        // Perform raycast to find actual beam endpoint
        Vector2 end = performRaycast(world, position, direction, effectiveRange, ignoredBody, ownerTeam);

        // Create and return beam with raycast results in a list (single beam for standard weapons)
        Beam beam = new Beam(
                position.copy(),
                end,
                range,
                ownerId,
                ownerTeam,
                damage,
                bulletEffects,
                ordinanceType,
                beamType,
                beamWidth,
                beamDuration,
                elevationTargeting
        );
        
        return List.of(beam);
    }

    /**
     * Perform raycast using dyn4j's built-in raycast functionality.
     * This determines where the beam actually ends (may hit obstacles before max range).
     */
    private Vector2 performRaycast(World<Body> world, Vector2 start, Vector2 direction,
                                   double maxRange, Body ignoredBody, int ownerTeam) {
        // Create a ray for the raycast
        Ray ray = new Ray(start, direction);

        // Create a filter that excludes the firing body and friendly units/buildings
        DetectFilter<Body, BodyFixture> filter = new DetectFilter<>(false, true, null);

        // Perform the raycast and get all results
        List<RaycastResult<Body, BodyFixture>> results = world.raycast(ray, maxRange, filter);

        // Find the closest hit
        RaycastResult<Body, BodyFixture> closestHit = null;
        double closestDistance = maxRange;

        for (RaycastResult<Body, BodyFixture> result : results) {
            // Get the raycast distance to obstacles
            if (result.getBody().getUserData() instanceof Obstacle) {
                double distance = result.getRaycast().getDistance();

                // Check if this is the closest hit so far
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestHit = result;
                }
            }
            // the SHIELD_GENERATOR should terminate beams entering
            else if (result.getBody().getUserData() instanceof ShieldSensor s) {
                Optional<ShieldComponent> component = s.getBuilding().getComponent(ShieldComponent.class);
                if (component.isPresent() && !component.get().isPositionInside(start)) {
                    double distance = result.getRaycast().getDistance();
                    // Check if this is the closest hit so far
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestHit = result;
                    }
                    // pass damage on to the building
                    s.getBuilding().takeDamage(getDamage());
                }
            }
        }

        // If we hit something, calculate the hit point
        if (closestHit != null) {
            // Get the hit point from the raycast result
            return closestHit.getRaycast().getPoint();
        }

        // No hit - beam travels full distance
        return start.copy().add(direction.copy().multiply(maxRange));
    }

    @Override
    public Weapon copy() {
        return new BeamWeapon(
                damage,
                range,
                attackRate,
                beamWidth,
                beamDuration,
                beamType,
                ordinanceType,
                Set.copyOf(bulletEffects),
                elevationTargeting
        );
    }

    @Override
    public Weapon copyWithModifiers(ResearchModifier modifier) {
        return new BeamWeapon(
                damage * modifier.getBeamDamageMultiplier(),
                range * modifier.getAttackRangeMultiplier(),
                attackRate * modifier.getAttackRateMultiplier(),
                beamWidth,
                beamDuration,
                beamType,
                ordinanceType,
                Set.copyOf(bulletEffects),
                elevationTargeting
        );
    }
}

