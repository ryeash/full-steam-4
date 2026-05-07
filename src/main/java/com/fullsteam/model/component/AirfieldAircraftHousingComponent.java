package com.fullsteam.model.component;

import com.fullsteam.model.Targetable;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.command.AttackTargetableCommand;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Houses sortie-based aircraft at an Airfield (multiple berths). Production is handled by {@link ProductionComponent}.
 */
@Slf4j
@Getter
public class AirfieldAircraftHousingComponent extends AbstractBuildingComponent {

    public static final int MAX_BERTHS = 4;

    private final List<Berth> berths = new ArrayList<>();

    @Setter
    private boolean scrambleEnabled = false;

    @Setter
    private Vector2 rallyPoint;

    public AirfieldAircraftHousingComponent() {
        for (int i = 0; i < MAX_BERTHS; i++) {
            berths.add(new Berth());
        }
    }

    /**
     * True if at least one berth has no housed aircraft (room for a newly produced sortie unit).
     */
    public boolean hasEmptyBerthForHousing() {
        return berths.stream().anyMatch(b -> b.getHousedUnit() == null);
    }

    /**
     * Place a completed sortie aircraft into the first free berth (not in world).
     */
    public boolean houseProducedAircraft(Unit aircraft) {
        for (Berth berth : berths) {
            if (berth.getHousedUnit() == null) {
                berth.assign(aircraft, building.getPosition());
                log.info("Airfield {} housed {} (id {}) in berth",
                        building.getId(), aircraft.getUnitType().getDisplayName(), aircraft.getId());
                return true;
            }
        }
        log.error("Airfield {} has no empty berth to house {}", building.getId(), aircraft.getId());
        return false;
    }

    @Override
    public void update(boolean hasLowPower) {
        double deltaTime = gameEntities.getWorld().getTimeStep().getDeltaTime();

        if (building.isUnderConstruction() || hasLowPower) {
            return;
        }

        for (Berth berth : berths) {
            if (berth.getHousedUnit() != null && !berth.isDeployed()) {
                double repairRate = 2.0;
                berth.getHousedUnit().setHealth(Math.min(
                        berth.getHousedUnit().getHealth() + repairRate * deltaTime,
                        berth.getHousedUnit().getMaxHealth()));
            }
        }

        if (scrambleEnabled) {
            for (Berth berth : berths) {
                if (berth.isReadyForSortie()
                        && berth.getHousedUnit().getUnitType() == UnitType.INTERCEPTOR) {
                    checkAndScramble(berth);
                }
            }
        }
    }

    public boolean isReadyForSortie(int unitId) {
        Berth berth = findBerthByUnitId(unitId);
        return berth != null && berth.isReadyForSortie();
    }

    public boolean isDeployed(int unitId) {
        Berth berth = findBerthByUnitId(unitId);
        return berth != null && berth.isDeployed();
    }

    public Unit launchAircraft(int unitId) {
        Berth berth = findBerthByUnitId(unitId);
        if (berth == null || berth.getHousedUnit() == null) {
            log.warn("Airfield {} no housed aircraft for unit id {}", building.getId(), unitId);
            return null;
        }
        if (berth.isDeployed()) {
            log.warn("Airfield {} unit {} already deployed", building.getId(), unitId);
            return null;
        }
        Unit housed = berth.getHousedUnit();
        if (gameEntities.getUnits().containsKey(housed.getId())) {
            log.error("CRITICAL: Aircraft {} already in world — resetting berth", housed.getId());
            berth.reset();
            return null;
        }
        housed.getBody().setEnabled(true);
        housed.setGarrisoned(false);
        gameEntities.getUnits().put(housed.getId(), housed);
        gameEntities.getWorld().addBody(housed.getBody());
        berth.setDeployed(true);
        log.info("Airfield {} launched aircraft {} ({})", building.getId(), housed.getId(), housed.getUnitType());
        return housed;
    }

    public void returnFromSortie(Unit returnedUnit) {
        if (returnedUnit == null) {
            return;
        }
        Berth berth = findBerthByUnitId(returnedUnit.getId());
        if (berth == null) {
            log.warn("Airfield {} returnFromSortie: no berth for unit {}", building.getId(), returnedUnit.getId());
            return;
        }
        if (!berth.isDeployed()) {
            log.warn("Airfield {} return for unit {} but berth not marked deployed", building.getId(), returnedUnit.getId());
            return;
        }
        berth.setHousedUnit(returnedUnit);
        returnedUnit.getBody().setEnabled(false);
        returnedUnit.setGarrisoned(true);
        gameEntities.getUnits().remove(returnedUnit.getId());
        gameEntities.getWorld().removeBody(returnedUnit.getBody());
        berth.setDeployed(false);
        log.info("Airfield {} aircraft {} returned to berth", building.getId(), returnedUnit.getId());
    }

    public void clearBerthForUnit(int unitId) {
        Berth berth = findBerthByUnitId(unitId);
        if (berth != null) {
            log.info("Airfield {} clearing berth for destroyed unit {}", building.getId(), unitId);
            berth.reset();
        }
    }

    public boolean scrapHousedAircraft(int unitId) {
        Berth berth = findBerthByUnitId(unitId);
        if (berth == null || berth.isDeployed()) {
            return false;
        }
        if (berth.getHousedUnit() != null) {
            berth.getHousedUnit().setActive(false);
            berth.reset();
            log.info("Airfield {} scrapped housed aircraft {} (no refund)", building.getId(), unitId);
            return true;
        }
        return false;
    }

    private Berth findBerthByUnitId(int unitId) {
        for (Berth berth : berths) {
            if (berth.getHousedUnit() != null && berth.getHousedUnit().getId() == unitId) {
                return berth;
            }
        }
        return null;
    }

    @Override
    public void onDestroy() {
        for (Berth berth : berths) {
            if (berth.getHousedUnit() != null) {
                if (berth.isDeployed()) {
                    log.warn("Airfield {} destroyed with aircraft {} deployed — unit lost on return", building.getId(), berth.getHousedUnit().getId());
                } else {
                    berth.getHousedUnit().setActive(false);
                }
            }
        }
    }

    private void checkAndScramble(Berth berth) {
        Vector2 pos = building.getPosition();
        int team = building.getTeamNumber();
        Targetable enemy = gameEntities.findNearestEnemyTargetable(pos, team, berth.getHousedUnit().getWeapon());
        if (enemy != null) {
            launchInterceptorToAttack(berth, enemy);
        }
    }

    private void launchInterceptorToAttack(Berth berth, Targetable target) {
        if (berth.getHousedUnit() == null || berth.isDeployed()) {
            return;
        }
        Unit housed = berth.getHousedUnit();
        housed.setGarrisoned(false);
        housed.getBody().setEnabled(true);
        housed.getBody().translate(building.getPosition().x, building.getPosition().y);
        gameEntities.getUnits().put(housed.getId(), housed);
        gameEntities.getWorld().addBody(housed.getBody());
        berth.setDeployed(true);
        housed.getComponent(InterceptorComponent.class)
                .ifPresent(comp -> comp.deploy(building.getId()));
        housed.issueCommand(new AttackTargetableCommand(housed, target, false), gameEntities);
        log.info("Interceptor {} scrambled from airfield {}", housed.getId(), building.getId());
    }

    public List<Berth> getBerthsView() {
        return Collections.unmodifiableList(berths);
    }

    @Getter
    public static final class Berth {
        private Unit housedUnit;
        private boolean deployed;

        boolean isReadyForSortie() {
            return housedUnit != null && !deployed && housedUnit.getHealth() > 0;
        }

        void assign(Unit aircraft, Vector2 buildingPos) {
            this.housedUnit = aircraft;
            this.deployed = false;
            aircraft.setPosition(buildingPos);
            aircraft.getBody().setEnabled(false);
            aircraft.setGarrisoned(true);
        }

        void setHousedUnit(Unit u) {
            this.housedUnit = u;
        }

        void setDeployed(boolean deployed) {
            this.deployed = deployed;
        }

        void reset() {
            housedUnit = null;
            deployed = false;
        }
    }
}
