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
     * Get the weapon for a specific unit type.
     * Uses the unit type's base stats (damage, range, attack rate) from UnitType enum.
     */
    public static Weapon getWeaponForUnitType(UnitType unitType) {
        double damage = unitType.getDamage();
        double range = unitType.getAttackRange();
        double attackRate = unitType.getAttackRate();

        return switch (unitType) {
            case WORKER -> new ProjectileWeapon(
                    damage, range, attackRate,
                    300,  // projectile speed
                    0.5,  // linear damping
                    1.5,  // size (tiny)
                    Ordinance.BULLET,
                    Set.of()
            );

            case INFANTRY -> new ProjectileWeapon(
                    damage, range, attackRate,
                    500,  // projectile speed (baseline)
                    0.3,  // linear damping
                    2.0,  // size (baseline)
                    Ordinance.BULLET,
                    Set.of()
            );

            case ROCKET_SOLDIER -> new ProjectileWeapon(
                    damage, range, attackRate,
                    400,  // projectile speed (slower)
                    0.1,  // linear damping
                    3.0,  // size (larger rocket)
                    Ordinance.ROCKET,
                    Set.of(BulletEffect.EXPLOSIVE)
            );

            case SNIPER -> new ProjectileWeapon(
                    damage, range, attackRate,
                    800,  // projectile speed (high-velocity)
                    0.1,  // linear damping
                    2.5,  // size
                    Ordinance.BULLET,
                    Set.of()
            );

            case JEEP -> new ProjectileWeapon(
                    damage, range, attackRate,
                    600,  // projectile speed (fast)
                    0.2,  // linear damping
                    2.5,  // size
                    Ordinance.BULLET,
                    Set.of()
            );

            case TANK -> new ProjectileWeapon(
                    damage, range, attackRate,
                    450,  // projectile speed
                    0.05, // linear damping
                    4.0,  // size (noticeably larger)
                    Ordinance.GRENADE,
                    Set.of(BulletEffect.EXPLOSIVE)
            );

            case CLOAK_TANK -> new ProjectileWeapon(
                    damage, range, attackRate,
                    500,  // projectile speed
                    0.05, // linear damping
                    4.0,  // size
                    Ordinance.GRENADE,
                    Set.of(BulletEffect.EXPLOSIVE)
            );

            case MAMMOTH_TANK -> new ProjectileWeapon(
                    damage, range, attackRate,
                    400,  // projectile speed
                    0.03, // linear damping (heavy!)
                    6.0,  // size (BIG shells!)
                    Ordinance.SHELL,
                    Set.of(BulletEffect.EXPLOSIVE)
            );

            case ARTILLERY -> new ProjectileWeapon(
                    damage, range, attackRate,
                    350,  // projectile speed
                    0.02, // linear damping
                    5.0,  // size (large artillery shells)
                    Ordinance.GRENADE,
                    Set.of(BulletEffect.EXPLOSIVE)
            );

            case GIGANTONAUT -> new ProjectileWeapon(
                    damage, range, attackRate,
                    300,  // projectile speed (slow, heavy)
                    0.01, // linear damping (very little)
                    8.0,  // size (MASSIVE! largest in game)
                    Ordinance.SHELL,
                    Set.of(BulletEffect.EXPLOSIVE)
            );

            case CRAWLER -> new ProjectileWeapon(
                    damage, range, attackRate,
                    450,  // projectile speed
                    0.2,  // linear damping
                    5.0,  // size (large turret shells)
                    Ordinance.SHELL,
                    Set.of(BulletEffect.EXPLOSIVE)
            );

            case ANDROID -> new ProjectileWeapon(
                    damage, range, attackRate,
                    520,  // projectile speed
                    0.25, // linear damping
                    2.2,  // size
                    Ordinance.BULLET,
                    Set.of()
            );

            // ===== HERO UNITS =====

            case RAIDER -> new ProjectileWeapon(
                    damage, range, attackRate,
                    700,  // projectile speed (very fast, hit-and-run)
                    0.15, // linear damping
                    3.0,  // size (hero weapon)
                    Ordinance.BULLET,
                    Set.of() // Elite raider bullets
            );

            case COLOSSUS -> new MultiProjectileWeapon(
                    damage, range, attackRate,
                    500,  // projectile speed
                    0.1,  // linear damping
                    5.0,  // size (large projectiles)
                    Ordinance.SHELL,
                    Set.of(BulletEffect.EXPLOSIVE),
                    3,    // projectile count (3 parallel shots)
                    25.0  // spread distance (parallel barrels)
            );

            // ===== BEAM WEAPONS =====

            case LASER_INFANTRY -> new BeamWeapon(
                    damage, range, attackRate,
                    2.5,  // beam width
                    0.3, // duration (150ms)
                    Beam.BeamType.LASER,
                    Ordinance.LASER,
                    Set.of()
            );

            case PLASMA_TROOPER -> new BeamWeapon(
                    damage, range, attackRate,
                    3.0,  // beam width (thicker)
                    0.4,  // duration
                    Beam.BeamType.PLASMA,
                    Ordinance.LASER,
                    Set.of()
            );

            case ION_RANGER -> new BeamWeapon(
                    damage, range, attackRate,
                    2.8,  // beam width
                    0.36, // duration
                    Beam.BeamType.ION,
                    Ordinance.LASER,
                    Set.of()
            );

            case PHOTON_SCOUT -> new BeamWeapon(
                    damage, range, attackRate,
                    3.0,  // beam width
                    0.3, // duration
                    Beam.BeamType.LASER,
                    Ordinance.LASER,
                    Set.of()
            );

            case BEAM_TANK -> new BeamWeapon(
                    damage, range, attackRate,
                    4.0,  // beam width (thicker, vehicle-mounted)
                    0.4,  // duration
                    Beam.BeamType.LASER,
                    Ordinance.LASER,
                    Set.of()
            );

            case PULSE_ARTILLERY -> new BeamWeapon(
                    damage, range, attackRate,
                    6.0,  // beam width (very thick)
                    0.7, // duration
                    Beam.BeamType.PLASMA,
                    Ordinance.LASER,
                    Set.of(BulletEffect.ELECTRIC) // Area denial
            );

            case PHOTON_TITAN -> new BeamWeapon(
                    damage, range, attackRate,
                    8.0,  // beam width (massive hero beam)
                    1.0,  // duration
                    Beam.BeamType.LASER,
                    Ordinance.LASER,
                    Set.of()
            );

            // ===== NON-COMBAT UNITS =====

            case MEDIC, ENGINEER, MINER -> null; // These units don't have weapons

            default -> new ProjectileWeapon(
                    damage, range, attackRate,
                    400,  // default projectile speed
                    0.3,  // default linear damping
                    2.0,  // default size
                    Ordinance.BULLET,
                    Set.of()
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
                Set.of()
        );
    }

    /**
     * Get the weapon for rocket turret building (ROCKET_TURRET).
     * High damage, long range, slower fire rate, explosive.
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
                Set.of(BulletEffect.EXPLOSIVE)
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
                Set.of()
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
                Set.of()
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
                Set.of(BulletEffect.EXPLOSIVE)
        );
    }
}

