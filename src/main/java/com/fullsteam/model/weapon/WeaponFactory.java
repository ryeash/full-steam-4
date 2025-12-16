package com.fullsteam.model.weapon;

import com.fullsteam.model.Beam;
import com.fullsteam.model.BulletEffect;
import com.fullsteam.model.Ordinance;
import com.fullsteam.model.UnitType;

import java.util.Set;

/**
 * Factory for creating predefined weapon instances.
 * Centralizes all weapon configurations for easy balancing and maintenance.
 * <p>
 * Each unit type, building type, and turret has a corresponding weapon definition here.
 * This eliminates the need for scattered weapon logic throughout the codebase.
 */
public class WeaponFactory {

    /**
     * Determine the elevation targeting capability for a unit's weapon.
     * This defines the strategic rock-paper-scissors of the elevation system:
     * - Most ground units can only hit GROUND targets
     * - Rocket soldiers/turrets can hit GROUND + LOW (basic anti-air)
     * - Flak Tank can hit LOW + HIGH (specialized anti-air, no ground targets)
     * - Interceptor can hit ALL elevations (air-to-air combat specialist)
     * - Other air units can only hit GROUND targets
     */
    private static ElevationTargeting getElevationTargetingForUnit(UnitType unitType) {
        return switch (unitType) {
            // Rocket weapons can hit low-altitude aircraft (VTOLs like Scout Drone, Helicopter)
            case ROCKET_SOLDIER -> ElevationTargeting.GROUND_AND_LOW;

            // Flak Tank can ONLY hit aircraft (LOW + HIGH elevations, NOT ground)
            case FLAK_TANK -> ElevationTargeting.LOW_AND_HIGH;

            // Interceptor can hit all air units (air-to-air specialist)
            case INTERCEPTOR -> ElevationTargeting.LOW_AND_HIGH;

            // All other units can only hit ground targets
            // This includes most air units (Scout Drone, Helicopter, Bomber - no air-to-air)
            default -> ElevationTargeting.GROUND_ONLY;
        };
    }

    /**
     * Get the weapon for a specific unit type.
     * Uses the unit type's base stats (damage, range, attack rate) from UnitType enum.
     */
    public static Weapon getWeaponForUnitType(UnitType unitType) {
        double damage = unitType.getDamage();
        double range = unitType.getAttackRange();
        double attackRate = unitType.getAttackRate();
        ElevationTargeting elevationTargeting = getElevationTargetingForUnit(unitType);

        return switch (unitType) {
            case WORKER -> new ProjectileWeapon(
                    damage, range, attackRate,
                    300,  // projectile speed
                    0.5,  // linear damping
                    1.5,  // size (tiny)
                    Ordinance.BULLET,
                    Set.of(),
                    elevationTargeting
            );

            case INFANTRY -> new ProjectileWeapon(
                    damage, range, attackRate,
                    500,  // projectile speed (baseline)
                    0.3,  // linear damping
                    2.0,  // size (baseline)
                    Ordinance.BULLET,
                    Set.of(),
                    elevationTargeting
            );

            case ROCKET_SOLDIER -> new ProjectileWeapon(
                    damage, range, attackRate,
                    400,  // projectile speed (slower)
                    0.1,  // linear damping
                    3.0,  // size (larger rocket)
                    Ordinance.ROCKET,
                    Set.of(BulletEffect.EXPLOSIVE),
                    elevationTargeting
            );

            case SNIPER -> new ProjectileWeapon(
                    damage, range, attackRate,
                    800,  // projectile speed (high-velocity)
                    0.1,  // linear damping
                    2.5,  // size
                    Ordinance.BULLET,
                    Set.of(),
                    elevationTargeting
            );

            case JEEP -> new ProjectileWeapon(
                    damage, range, attackRate,
                    600,  // projectile speed (fast)
                    0.2,  // linear damping
                    2.5,  // size
                    Ordinance.BULLET,
                    Set.of(),
                    elevationTargeting
            );

            case TANK -> new ProjectileWeapon(
                    damage, range, attackRate,
                    450,  // projectile speed
                    0.05, // linear damping
                    4.0,  // size (noticeably larger)
                    Ordinance.GRENADE,
                    Set.of(BulletEffect.EXPLOSIVE),
                    elevationTargeting
            );

            case FLAK_TANK -> new ProjectileWeapon(
                    damage, range, attackRate,
                    550,  // projectile speed (faster than tank shells)
                    0.08, // linear damping (moderate air resistance)
                    3.5,  // size (medium flak shells)
                    Ordinance.FLAK,
                    Set.of(BulletEffect.FLAK), // Creates FLAK_EXPLOSION field effects
                    elevationTargeting
            );

            case CLOAK_TANK -> new ProjectileWeapon(
                    damage, range, attackRate,
                    500,  // projectile speed
                    0.05, // linear damping
                    4.0,  // size
                    Ordinance.GRENADE,
                    Set.of(BulletEffect.EXPLOSIVE),
                    elevationTargeting
            );

            case ARTILLERY -> new ProjectileWeapon(
                    damage, range, attackRate,
                    350,  // projectile speed
                    0.02, // linear damping
                    5.0,  // size (large artillery shells)
                    Ordinance.GRENADE,
                    Set.of(BulletEffect.EXPLOSIVE),
                    elevationTargeting
            );

            case GIGANTONAUT -> new ProjectileWeapon(
                    damage, range, attackRate,
                    300,  // projectile speed (slow, heavy)
                    0.01, // linear damping (very little)
                    8.0,  // size (MASSIVE! largest in game)
                    Ordinance.SHELL,
                    Set.of(BulletEffect.EXPLOSIVE),
                    elevationTargeting
            );

            case CRAWLER -> new ProjectileWeapon(
                    damage, range, attackRate,
                    450,  // projectile speed
                    0.2,  // linear damping
                    5.0,  // size (large turret shells)
                    Ordinance.SHELL,
                    Set.of(BulletEffect.EXPLOSIVE),
                    elevationTargeting
            );

            case ANDROID -> new ProjectileWeapon(
                    damage, range, attackRate,
                    520,  // projectile speed
                    0.25, // linear damping
                    2.2,  // size
                    Ordinance.BULLET,
                    Set.of(),
                    elevationTargeting
            );

            // ===== HERO UNITS =====

            case RAIDER -> new ProjectileWeapon(
                    damage, range, attackRate,
                    700,  // projectile speed (very fast, hit-and-run)
                    0.15, // linear damping
                    3.0,  // size (hero weapon)
                    Ordinance.BULLET,
                    Set.of(), // Elite raider bullets
                    elevationTargeting
            );

            case COLOSSUS -> new MultiProjectileWeapon(
                    damage, range, attackRate,
                    500,  // projectile speed
                    0.1,  // linear damping
                    5.0,  // size (large projectiles)
                    Ordinance.SHELL,
                    Set.of(BulletEffect.EXPLOSIVE),
                    3,    // projectile count (3 parallel shots)
                    25.0, // spread distance (parallel barrels)
                    elevationTargeting
            );

            // ===== BEAM WEAPONS =====

            case LASER_INFANTRY -> new BeamWeapon(
                    damage, range, attackRate,
                    2.5,  // beam width
                    0.3, // duration (150ms)
                    Beam.BeamType.LASER,
                    Ordinance.LASER,
                    Set.of(),
                    elevationTargeting
            );

            case PLASMA_TROOPER -> new BeamWeapon(
                    damage, range, attackRate,
                    3.0,  // beam width (thicker)
                    0.4,  // duration
                    Beam.BeamType.PLASMA,
                    Ordinance.LASER,
                    Set.of(),
                    elevationTargeting
            );

            case ION_RANGER -> new BeamWeapon(
                    damage, range, attackRate,
                    2.8,  // beam width
                    0.36, // duration
                    Beam.BeamType.ION,
                    Ordinance.LASER,
                    Set.of(),
                    elevationTargeting
            );

            case PHOTON_SCOUT -> new BeamWeapon(
                    damage, range, attackRate,
                    3.0,  // beam width
                    0.3, // duration
                    Beam.BeamType.LASER,
                    Ordinance.LASER,
                    Set.of(),
                    elevationTargeting
            );

            case BEAM_TANK -> new BeamWeapon(
                    damage, range, attackRate,
                    4.0,  // beam width (thicker, vehicle-mounted)
                    0.4,  // duration
                    Beam.BeamType.LASER,
                    Ordinance.LASER,
                    Set.of(),
                    elevationTargeting
            );

            case PULSE_ARTILLERY -> new BeamWeapon(
                    damage, range, attackRate,
                    6.0,  // beam width (very thick)
                    0.7, // duration
                    Beam.BeamType.PLASMA,
                    Ordinance.LASER,
                    Set.of(BulletEffect.ELECTRIC), // Area denial
                    elevationTargeting
            );

            case PHOTON_TITAN -> new BeamWeapon(
                    damage, range, attackRate,
                    8.0,  // beam width (massive hero beam)
                    1.0,  // duration
                    Beam.BeamType.LASER,
                    Ordinance.LASER,
                    Set.of(),
                    elevationTargeting
            );

            // ===== AIR UNITS =====

            case HELICOPTER -> new MultiProjectileWeapon(
                    damage, range, attackRate,
                    600,  // projectile speed (fast rockets)
                    0.15, // linear damping
                    3.5,  // size (medium rockets)
                    Ordinance.ROCKET,
                    Set.of(BulletEffect.EXPLOSIVE),
                    2,    // projectile count (dual rockets)
                    8.0,  // spread distance (mounted on sides of helicopter)
                    elevationTargeting
            );

            case INTERCEPTOR -> new ProjectileWeapon(
                    damage, range, attackRate,
                    800,  // projectile speed (very fast seeking missiles)
                    0.05, // linear damping (minimal, long range)
                    3.0,  // size (air-to-air missiles)
                    Ordinance.ROCKET,
                    Set.of(BulletEffect.SEEKING, BulletEffect.EXPLOSIVE), // Heat-seeking air-to-air missiles
                    elevationTargeting
            );

            // Note: SCOUT_DRONE and BOMBER use default weapons (defined in default case)
            // Scout Drone: Light machine guns (default bullets)
            // Bomber: Doesn't use weapon system, creates explosions directly via SortieCommand
            // Gunship: Uses dual weapons managed by GunshipComponent (not standard weapon system)

            // ===== NON-COMBAT UNITS =====

            case MEDIC, ENGINEER, MINER, GUNSHIP -> null; // These units don't have weapons (or manage their own)

            default -> new ProjectileWeapon(
                    damage, range, attackRate,
                    400,  // default projectile speed
                    0.3,  // default linear damping
                    2.0,  // default size
                    Ordinance.BULLET,
                    Set.of(),
                    elevationTargeting
            );
        };
    }

    // ========================================
    // BUILDING WEAPONS
    // ========================================

    /**
     * Get the weapon for a standard turret building (TURRET).
     */
    public static Weapon getTurretWeapon() {
        return new ProjectileWeapon(
                25.0,  // damage
                300.0, // range
                2.0,   // attack rate
                600.0, // projectile speed
                0.2,   // linear damping
                3.5,   // size (medium-sized projectiles)
                Ordinance.BULLET,
                Set.of(),
                ElevationTargeting.GROUND_ONLY // Basic turret - ground only
        );
    }

    /**
     * Get the weapon for rocket turret building (ROCKET_TURRET).
     * High damage, long range, slower fire rate, explosive.
     * Can target low-altitude aircraft (anti-air capable).
     */
    public static Weapon getRocketTurretWeapon() {
        return new ProjectileWeapon(
                60.0,  // damage (high single-target damage)
                400.0, // range (longer than basic turret)
                0.8,   // attack rate (slow fire rate)
                450.0, // projectile speed (slower rockets)
                0.05,  // linear damping (low - rockets maintain speed)
                5.0,   // size (large rockets)
                Ordinance.ROCKET,
                Set.of(BulletEffect.EXPLOSIVE),
                ElevationTargeting.GROUND_AND_LOW // Anti-air capable!
        );
    }

    /**
     * Get the weapon for laser turret building (LASER_TURRET).
     * Moderate damage, instant hit, high fire rate.
     */
    public static Weapon getLaserTurretWeapon() {
        return new BeamWeapon(
                35.0,  // damage (moderate per shot, but high DPS due to fire rate)
                350.0, // range (good range)
                2.5,   // attack rate (fast fire rate)
                4.0,   // beam width (visible laser)
                0.4,   // duration (visible beam)
                Beam.BeamType.LASER,
                Ordinance.LASER,
                Set.of(),
                ElevationTargeting.GROUND_ONLY // Lasers - ground only for now
        );
    }

    /**
     * Get the weapon for Photon Spire (Obelisk of Light style).
     */
    public static Weapon getPhotonSpireWeapon() {
        return new BeamWeapon(
                250.0, // damage (massive)
                400.0, // range (very long)
                0.286, // attack rate (1 shot every 3.5 seconds)
                3.0,   // beam width (thick, powerful beam)
                0.3,   // duration (visible for 0.3 seconds)
                Beam.BeamType.LASER,
                Ordinance.LASER,
                Set.of(),
                ElevationTargeting.GROUND_ONLY // Photon Spire - ground only
        );
    }
    
    /**
     * Get the weapon for Tempest Spire (Storm Wings monument).
     * Heavy anti-aircraft flak cannon for air superiority.
     */
    public static Weapon getTempestSpireWeapon() {
        return new ProjectileWeapon(
                120.0, // damage (heavy anti-air)
                450.0, // range (very long - weather radar targeting)
                0.5,   // attack rate (2 shots per second - rapid flak)
                500.0, // projectile speed (fast flak)
                0.2,   // damping
                6.0,   // size (large flak burst)
                Ordinance.FLAK,
                Set.of(BulletEffect.FLAK, BulletEffect.EXPLOSIVE),
                ElevationTargeting.LOW_AND_HIGH // Anti-air only (LOW and HIGH altitude)
        );
    }

    /**
     * Get the weapon for Crawler turrets (when deployed).
     * This is used by the Turret class for multi-turret units.
     */
    public static Weapon getCrawlerTurretWeapon(double damage, double range, double attackRate) {
        return new ProjectileWeapon(
                damage,
                range,
                attackRate,
                450.0, // projectile speed
                0.2,   // linear damping
                5.0,   // size (large turret shells)
                Ordinance.SHELL,
                Set.of(BulletEffect.EXPLOSIVE),
                ElevationTargeting.GROUND_ONLY // Crawler turrets - ground only
        );
    }
}

