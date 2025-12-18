package com.fullsteam.model.component;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.BulletEffect;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Ordinance;
import com.fullsteam.model.Targetable;
import com.fullsteam.model.command.OnStationCommand;
import com.fullsteam.model.command.ReturnToHangarCommand;
import com.fullsteam.model.research.ResearchModifier;
import com.fullsteam.model.weapon.ElevationTargeting;
import com.fullsteam.model.weapon.ProjectileWeapon;
import com.fullsteam.model.weapon.Weapon;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Getter
public class GunshipComponent extends AbstractUnitComponent {

    private Weapon groundWeapon;
    private Weapon airWeapon;

    private static final double MAX_FUEL = 30.0;
    private double currentFuel;
    private long sortieStartTime = 0;
    private boolean lowFuelWarning = false;

    private static final int MAX_AMMO_GROUND = 200;
    private static final int MAX_AMMO_AIR = 30;
    private int currentAmmoGround;
    private int currentAmmoAir;

    private boolean onSortie = false;
    private Integer hangarId;

    public GunshipComponent() {
        // Create dual weapons
        // Ground weapon: Heavy machine gun
        this.groundWeapon = new ProjectileWeapon(
                20,    // damage
                280,   // range
                8.0,   // attack rate (rapid fire MG)
                600,   // projectile speed
                0.3,   // damping
                2.5,   // size
                Ordinance.BULLET,
                Set.of(),
                ElevationTargeting.GROUND_ONLY
        );

        // Air weapon: Flak cannon
        this.airWeapon = new ProjectileWeapon(
                55,    // damage
                280,   // range
                1.2,   // attack rate (slower flak)
                400,   // projectile speed
                0.2,   // damping
                4.0,   // larger flak burst
                Ordinance.FLAK,
                Set.of(BulletEffect.FLAK),
                ElevationTargeting.LOW_AND_HIGH
        );

        // Initialize fuel and ammo
        this.currentFuel = MAX_FUEL;
        this.currentAmmoGround = MAX_AMMO_GROUND;
        this.currentAmmoAir = MAX_AMMO_AIR;
    }

    @Override
    public void update(GameEntities gameEntities) {
        if (!onSortie || unit.getCurrentCommand() instanceof ReturnToHangarCommand) {
            return;
        }

        // Fuel consumption
        long currentTime = System.currentTimeMillis();
        double elapsedSeconds = (currentTime - sortieStartTime) / 1000.0;
        currentFuel = Math.max(0, MAX_FUEL - elapsedSeconds);

        // Low fuel warning at 25%
        if (currentFuel <= MAX_FUEL * 0.25 && currentFuel > 0 && !lowFuelWarning) {
            lowFuelWarning = true;
            log.info("Gunship {} - LOW FUEL WARNING ({} seconds remaining)",
                    unit.getId(), (int) currentFuel);
        }

        // Out of fuel
        if (currentFuel <= 0) {
            currentFuel = 0;
            log.warn("Gunship {} - OUT OF FUEL! Returning to hangar.", unit.getId());
            returnToHangar();
        }

        // Check if out of all ammo
        if (currentAmmoGround == 0 && currentAmmoAir == 0) {
            log.warn("Gunship {} - OUT OF AMMO! Returning to hangar.", unit.getId());
            returnToHangar();
        }

        if (unit.getCurrentCommand() instanceof OnStationCommand) {
            attackEnemies();
        }
    }

    /**
     * Deploy the gunship on a sortie mission.
     */
    public void deploy(int hangarId) {
        this.hangarId = hangarId;
        this.onSortie = true;
        this.sortieStartTime = System.currentTimeMillis();
        this.currentFuel = MAX_FUEL;
        this.currentAmmoGround = MAX_AMMO_GROUND;
        this.currentAmmoAir = MAX_AMMO_AIR;
        this.lowFuelWarning = false;
        log.info("Gunship {} deployed from hangar {} - Fuel: {}s, MG Ammo: {}, Flak Ammo: {}",
                unit.getId(), hangarId, (int) currentFuel, currentAmmoGround, currentAmmoAir);
    }

    /**
     * Refuel and rearm the gunship.
     */
    public void refuelAndRearm() {
        currentFuel = MAX_FUEL;
        currentAmmoGround = MAX_AMMO_GROUND;
        currentAmmoAir = MAX_AMMO_AIR;
        onSortie = false;
        lowFuelWarning = false;
        log.info("Gunship {} refueled and rearmed - Ready for deployment", unit.getId());
    }

    /**
     * Return the gunship to its home hangar.
     */
    private void returnToHangar() {
        if (hangarId == null) {
            log.error("Gunship {} has no hangar ID! Cannot return.", unit.getId());
            return;
        }

        // Don't issue another return command if already returning
        if (unit.getCurrentCommand() instanceof ReturnToHangarCommand) {
            return;
        }

        log.info("Gunship {} returning to hangar {}", unit.getId(), hangarId);
        ReturnToHangarCommand returnCommand = new ReturnToHangarCommand(unit, hangarId, false);
        unit.issueCommand(returnCommand, gameEntities);
    }

    @Override
    public void applyResearchModifiers(ResearchModifier modifier) {
        this.airWeapon = airWeapon.copyWithModifiers(modifier);
        this.groundWeapon = groundWeapon.copyWithModifiers(modifier);
    }

    private void attackEnemies() {
        Targetable airUnit = gameEntities.findNearestEnemyTargetable(unit.getPosition(), unit.getTeamNumber(), airWeapon);
        List<AbstractOrdinance> ordinances = new ArrayList<>();
        if (airUnit != null) {
            List<AbstractOrdinance> list = airWeapon.fire(
                    unit.getPosition(),
                    airUnit.getPosition(),
                    airUnit.getElevation(),
                    unit.getOwnerId(),
                    unit.getTeamNumber(),
                    unit.getBody(),
                    gameEntities);
            ordinances.addAll(list);
            currentAmmoAir -= list.size();
        }

        Targetable groundUnit = gameEntities.findNearestEnemyTargetable(unit.getPosition(), unit.getTeamNumber(), groundWeapon);
        if (groundUnit != null) {
            List<AbstractOrdinance> list = groundWeapon.fire(
                    unit.getPosition(),
                    groundUnit.getPosition(),
                    groundUnit.getElevation(),
                    unit.getOwnerId(),
                    unit.getTeamNumber(),
                    unit.getBody(),
                    gameEntities);
            ordinances.addAll(list);
            currentAmmoGround -= list.size();
        }

        for (AbstractOrdinance ordinance : ordinances) {
            gameEntities.add(ordinance);
        }
    }
}
