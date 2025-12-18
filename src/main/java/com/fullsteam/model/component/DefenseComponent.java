package com.fullsteam.model.component;

import com.fullsteam.model.AbstractOrdinance;
import com.fullsteam.model.Building;
import com.fullsteam.model.GameEntities;
import com.fullsteam.model.Targetable;
import com.fullsteam.model.research.ResearchModifier;
import com.fullsteam.model.weapon.Weapon;
import lombok.Getter;
import lombok.Setter;
import org.dyn4j.geometry.Vector2;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public class DefenseComponent extends AbstractBuildingComponent {
    private Weapon weapon;
    private Targetable target = null;

    public DefenseComponent(Weapon weapon) {
        this.weapon = weapon;
    }

    @Override
    public void init(GameEntities gameEntities, Building building) {
        super.init(gameEntities, building);
        building.setRotation(ThreadLocalRandom.current().nextDouble(Math.PI));
    }

    @Override
    public void update(boolean hasLowPower) {
        if (!building.isActive() || hasLowPower) {
            return;
        }
        acquireTurretTarget(gameEntities, building);

        if (target != null && target.isActive()) {
            Vector2 turretPos = building.getPosition();
            Vector2 targetPos = target.getPosition();
            double distance = turretPos.distance(targetPos);

            // Check if target is in range
            if (distance <= weapon.getRange()) {
                Vector2 direction = targetPos.copy().subtract(turretPos);
                building.setRotation(Math.atan2(direction.y, direction.x));
                fireAtTarget(gameEntities, building, turretPos, targetPos);
            } else {
                // Target out of range
                target = null;
            }
        }
    }

    @Override
    public void applyResearchModifiers(ResearchModifier modifier) {
        this.weapon = weapon.copyWithModifiers(modifier);
    }

    /**
     * Fire ordinance at the current target using the weapon system.
     *
     * @param building  The building firing
     * @param turretPos Position of the turret
     * @param targetPos Position of the target
     */
    private void fireAtTarget(GameEntities gameEntities, Building building, Vector2 turretPos, Vector2 targetPos) {
        if (target == null || !target.isActive()) {
            return;
        }

        // Fire weapon
        List<AbstractOrdinance> ordinances = weapon.fire(
                turretPos,
                targetPos,
                target.getElevation(),
                building.getId(),
                building.getTeamNumber(),
                building.getBody(),
                gameEntities
        );

        // Add ordinances to game world (defensive buildings can fire projectiles or beams)
        for (AbstractOrdinance ordinance : ordinances) {
            gameEntities.add(ordinance);
        }
    }

    private void acquireTurretTarget(GameEntities gameEntities, Building turret) {
        if (target != null && target.isValidTargetFor(weapon, turret.getTeamNumber(), turret.getPosition())) {
            return;
        }
        this.target = gameEntities.findNearestEnemyTargetable(turret.getPosition(), turret.getTeamNumber(), weapon);
    }
}




