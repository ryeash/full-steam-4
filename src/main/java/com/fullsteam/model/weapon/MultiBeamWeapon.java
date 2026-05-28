package com.fullsteam.model.weapon;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Beam;
import com.fullsteam.model.Building;
import com.fullsteam.model.BulletEffect;
import com.fullsteam.model.Elevation;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Obstacle;
import com.fullsteam.model.Ordinance;
import com.fullsteam.model.ShieldSensor;
import com.fullsteam.model.Unit;
import com.fullsteam.model.component.ShieldComponent;
import com.fullsteam.model.component.ShieldTankComponent;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.DetectFilter;
import org.dyn4j.world.World;
import org.dyn4j.world.result.RaycastResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Weapon that fires multiple beams simultaneously in a spread pattern.
 * Useful for units with multiple beam emitters or wide-area beam weapons.
 */
@Getter
@Setter
public class MultiBeamWeapon extends Weapon {
    private double beamWidth;
    private double beamDuration;
    private Beam.BeamType beamType;
    private Ordinance ordinanceType;
    private Set<BulletEffect> bulletEffects;
    private int beamCount; // Number of beams to fire
    private double spreadDistance; // Distance between parallel beams (0 for point source)
    private double spreadAngle; // Angle spread in radians (0 for parallel)

    /**
     * Create a multi-beam weapon with angular spread.
     * Beams fire in a cone pattern from the source.
     */
    public MultiBeamWeapon(double damage,
                           double range,
                           double attackRate,
                           double beamWidth,
                           double beamDuration,
                           Beam.BeamType beamType,
                           Ordinance ordinanceType,
                           Set<BulletEffect> bulletEffects,
                           int beamCount,
                           double spreadDistance,
                           double spreadAngle,
                           ElevationTargeting elevationTargeting) {
        super(damage, range, attackRate, elevationTargeting);
        this.beamWidth = beamWidth;
        this.beamDuration = beamDuration;
        this.beamType = beamType;
        this.ordinanceType = ordinanceType;
        this.bulletEffects = bulletEffects != null ? Set.copyOf(bulletEffects) : Set.of();
        this.beamCount = Math.max(1, beamCount);
        this.spreadDistance = spreadDistance;
        this.spreadAngle = spreadAngle;
    }

    @Override
    protected List<AbstractOrdinance> createOrdinances(Vector2 position,
                                                       Vector2 targetPosition,
                                                       Elevation targetElevation,
                                                       int ownerId,
                                                       int ownerTeam,
                                                       Body ignoredBody,
                                                       GameEntities gameEntities) {
        // Beams require the world for raycasting
        World<Body> world = gameEntities.getWorld();
        if (world == null) {
            return List.of();
        }

        List<AbstractOrdinance> ordinances = new ArrayList<>();

        // Calculate direction to target
        Vector2 direction = targetPosition.copy().subtract(position);
        double distanceToTarget = direction.getMagnitude();
        direction.normalize();

        // Use the minimum of weapon range and distance to target
        double raycastRange = Math.min(range, distanceToTarget);

        // Damage per beam (split among beams)
        double damagePerBeam = damage / beamCount;

        // If parallel spread (like multiple beam emitters)
        if (spreadDistance > 0) {
            // Calculate perpendicular vector for spreading
            Vector2 perpendicular = new Vector2(-direction.y, direction.x);

            // Calculate starting offset for centered spread
            double totalWidth = (beamCount - 1) * spreadDistance;
            double startOffset = -totalWidth / 2.0;

            for (int i = 0; i < beamCount; i++) {
                // Calculate offset position for this beam
                double offset = startOffset + (i * spreadDistance);
                Vector2 spawnPos = position.copy().add(perpendicular.copy().multiply(offset));

                // All beams travel in the same direction (parallel)
                Vector2 end = performRaycast(world, spawnPos, direction, raycastRange,
                        ignoredBody, ownerTeam, targetElevation, damagePerBeam);

                Beam beam = new Beam(
                        spawnPos,
                        end,
                        range,
                        ownerId,
                        ownerTeam,
                        damagePerBeam,
                        bulletEffects,
                        ordinanceType,
                        beamType,
                        beamWidth,
                        beamDuration,
                        elevationTargeting,
                        targetElevation
                );
                ordinances.add(beam);
            }
        }
        // If angular spread (like wide-area beam weapon)
        else if (spreadAngle > 0) {
            // Calculate angle step between beams
            double totalAngle = spreadAngle;
            double angleStep = totalAngle / Math.max(1, beamCount - 1);
            double startAngle = -totalAngle / 2.0;

            for (int i = 0; i < beamCount; i++) {
                // Calculate angle for this beam
                double angle = startAngle + (i * angleStep);

                // Rotate the direction vector by the angle
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                Vector2 rotatedDir = new Vector2(
                        direction.x * cos - direction.y * sin,
                        direction.x * sin + direction.y * cos
                );
                rotatedDir.normalize();

                // Perform raycast for this beam
                Vector2 end = performRaycast(world, position, rotatedDir, raycastRange,
                        ignoredBody, ownerTeam, targetElevation, damagePerBeam);

                Beam beam = new Beam(
                        position.copy(),
                        end,
                        range,
                        ownerId,
                        ownerTeam,
                        damagePerBeam,
                        bulletEffects,
                        ordinanceType,
                        beamType,
                        beamWidth,
                        beamDuration,
                        elevationTargeting,
                        targetElevation
                );
                ordinances.add(beam);
            }
        }
        // Fallback: single beam
        else {
            Vector2 end = performRaycast(world, position, direction, raycastRange,
                    ignoredBody, ownerTeam, targetElevation, damage);

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
                    targetElevation
            );
            ordinances.add(beam);
        }

        return ordinances;
    }

    /**
     * Perform raycast using dyn4j's built-in raycast functionality.
     * This determines where the beam actually ends (may hit obstacles before max range).
     * Respects elevation - only hits obstacles at the same elevation.
     */
    private Vector2 performRaycast(World<Body> world, Vector2 start, Vector2 direction,
                                   double maxRange, Body ignoredBody, int ownerTeam,
                                   Elevation beamElevation, double beamDamage) {
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
            // Shields are at GROUND elevation (they protect buildings and units)
            // Only block beams at GROUND elevation
            else if (result.getBody().getUserData() instanceof ShieldSensor s) {
                if (beamElevation != Elevation.GROUND) {
                    continue; // Beam at higher elevation passes over shields
                }

                // Handle building shields
                if (s.shieldOwner() instanceof Building building) {
                    Optional<ShieldComponent> component = building.getComponent(ShieldComponent.class);
                    if (component.isPresent() && !component.get().isPositionInside(start)) {
                        double distance = result.getRaycast().getDistance();
                        // Check if this is the closest hit so far
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestHit = result;
                        }
                        // pass damage on to the building (via armor matrix; no shield on buildings)
                        building.takeDamage(building.absorbDamage(beamDamage, com.fullsteam.model.DamageType.ENERGY));
                    }
                }
                // Handle unit shields (Shield Tank)
                else if (s.shieldOwner() instanceof Unit unit) {
                    Optional<ShieldTankComponent> component = unit.getComponent(ShieldTankComponent.class);
                    if (component.isPresent() && !component.get().isPositionInside(start)) {
                        double distance = result.getRaycast().getDistance();
                        // Check if this is the closest hit so far
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestHit = result;
                        }
                        // Apply damage feedback to the shield tank
                        component.get().applyShieldDamage(beamDamage);
                    }
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
}
