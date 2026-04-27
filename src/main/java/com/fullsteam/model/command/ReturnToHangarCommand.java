package com.fullsteam.model.command;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Building;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.Unit;
import com.fullsteam.model.component.AirfieldAircraftHousingComponent;
import com.fullsteam.model.component.GunshipComponent;
import com.fullsteam.model.component.InterceptorComponent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Sortie aircraft fly back to their home airfield and re-enter housing.
 */
@Slf4j
@Getter
public class ReturnToHangarCommand extends UnitCommand {

    private final int homeBaseBuildingId;
    private Building homeBase;
    private boolean initialized = false;

    public ReturnToHangarCommand(Unit unit, int homeBaseBuildingId, boolean isPlayerOrder) {
        super(unit, isPlayerOrder);
        this.homeBaseBuildingId = homeBaseBuildingId;
    }

    @Override
    public boolean update(double deltaTime) {
        if (!initialized) {
            homeBase = gameEntities.getBuildings().get(homeBaseBuildingId);
            if (homeBase == null) {
                log.error("Home building {} not found! Aircraft {} cannot return.", homeBaseBuildingId, unit.getId());
                unit.setActive(false);
                return false;
            }
            if (homeBase.getBuildingType() != BuildingType.AIRFIELD) {
                log.error("Building {} is not an airfield! Aircraft {} cannot land.", homeBaseBuildingId, unit.getId());
                unit.setActive(false);
                return false;
            }
            initialized = true;
            log.info("Aircraft {} returning to airfield {} at ({}, {})",
                    unit.getId(), homeBaseBuildingId, (int) homeBase.getPosition().x, (int) homeBase.getPosition().y);
        }

        if (!homeBase.isActive()) {
            log.warn("Airfield {} destroyed! Aircraft {} has nowhere to land.", homeBaseBuildingId, unit.getId());
            return false;
        }

        Vector2 currentPos = unit.getPosition();
        Vector2 basePos = homeBase.getPosition();
        double distance = currentPos.distance(basePos);

        if (distance < 30.0) {
            landAtAirfield();
            return false;
        }

        return true;
    }

    @Override
    public void updateMovement(double deltaTime) {
        if (homeBase == null) {
            return;
        }

        Vector2 currentPos = unit.getPosition();
        Vector2 basePos = homeBase.getPosition();
        Vector2 direction = basePos.copy().subtract(currentPos);
        direction.normalize();

        double speed = unit.getUnitType().getMovementSpeed();
        unit.getBody().setLinearVelocity(
                direction.x * speed,
                direction.y * speed
        );

        unit.setRotation(Math.atan2(direction.y, direction.x));
    }

    private void landAtAirfield() {
        log.info("Aircraft {} landing at airfield {}", unit.getId(), homeBaseBuildingId);

        AirfieldAircraftHousingComponent housing = homeBase.getComponent(AirfieldAircraftHousingComponent.class).orElse(null);
        if (housing == null) {
            log.error("Airfield {} has no AirfieldAircraftHousingComponent!", homeBaseBuildingId);
            return;
        }

        housing.returnFromSortie(unit);

        unit.getComponent(InterceptorComponent.class).ifPresent(comp -> {
            comp.refuelAndRearm();
            log.info("Interceptor {} refueled and rearmed", unit.getId());
        });

        unit.getComponent(GunshipComponent.class).ifPresent(comp -> {
            comp.refuelAndRearm();
            log.info("Gunship {} refueled and rearmed", unit.getId());
        });

        log.info("Aircraft {} successfully housed at airfield {}", unit.getId(), homeBaseBuildingId);
    }

    @Override
    public List<AbstractOrdinance> updateCombat(double deltaTime) {
        return List.of();
    }

    @Override
    public String getDescription() {
        if (homeBase != null) {
            return String.format("Returning to Airfield (%.0f, %.0f)",
                    homeBase.getPosition().x, homeBase.getPosition().y);
        }
        return "Returning to Airfield";
    }
}
