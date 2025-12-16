package com.fullsteam.model.weapon;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Beam;
import com.fullsteam.model.BulletEffect;
import com.fullsteam.model.Elevation;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Obstacle;
import com.fullsteam.model.Ordinance;
import com.fullsteam.model.ResourceDeposit;
import com.fullsteam.model.ShieldSensor;
import com.fullsteam.model.Unit;
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

        // Determine the elevation for this beam based on what we're targeting
        Elevation beamElevation = determineOrdinanceElevation(targetPosition, gameEntities);

        // Perform raycast to find actual beam endpoint (respecting elevation)
        Vector2 end = performRaycast(world, position, direction, effectiveRange, ignoredBody, ownerTeam, beamElevation);

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
                elevationTargeting,
                beamElevation
        );

        return List.of(beam);
    }

    /**
     * Determine what elevation the ordinance should fly at based on the target position.
     * This allows beams fired at aircraft to fly at aircraft elevation and not collide with ground obstacles.
     */
    private Elevation determineOrdinanceElevation(Vector2 targetPosition, GameEntities gameEntities) {
        // Check if we're targeting an airborne unit
        double searchRadius = 50.0; // Search for units near the target position

        for (Unit unit : gameEntities.getUnits().values()) {
            if (!unit.isActive()) {
                continue;
            }

            double distance = unit.getPosition().distance(targetPosition);
            if (distance < searchRadius) {
                // Found a unit near target - use its elevation
                Elevation targetElevation = unit.getUnitType().getElevation();
                if (targetElevation.isAirborne() && elevationTargeting.canTarget(targetElevation)) {
                    // Targeting an airborne unit - beam fires at that elevation
                    return targetElevation;
                }
            }
        }

        // Default to GROUND elevation (for hitting ground units, buildings, obstacles)
        return Elevation.GROUND;
    }

    /**
     * Perform raycast using dyn4j's built-in raycast functionality.
     * This determines where the beam actually ends (may hit obstacles before max range).
     * Now respects elevation - only hits obstacles at the same elevation.
     */
    private Vector2 performRaycast(World<Body> world, Vector2 start, Vector2 direction,
                                   double maxRange, Body ignoredBody, int ownerTeam,
                                   Elevation beamElevation) {
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
            // Obstacles are at GROUND elevation - only hit them if beam is also at GROUND
            if (result.getBody().getUserData() instanceof Obstacle) {
                if (beamElevation != Elevation.GROUND) {
                    continue; // Beam at higher elevation passes over obstacles
                }

                double distance = result.getRaycast().getDistance();

                // Check if this is the closest hit so far
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestHit = result;
                }
            }
            // Resource deposits are at GROUND elevation - only hit them if beam is also at GROUND
            else if (result.getBody().getUserData() instanceof ResourceDeposit) {
                if (beamElevation != Elevation.GROUND) {
                    continue; // Beam at higher elevation passes over deposits
                }

                double distance = result.getRaycast().getDistance();

                // Check if this is the closest hit so far
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestHit = result;
                }
            }
            // Shields are at GROUND elevation (they protect buildings)
            // Only block beams at GROUND elevation
            else if (result.getBody().getUserData() instanceof ShieldSensor s) {
                if (beamElevation != Elevation.GROUND) {
                    continue; // Beam at higher elevation passes over shields
                }

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

