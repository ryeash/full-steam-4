package com.fullsteam.model.component;

import com.fullsteam.model.Turret;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * Component for units that can deploy (Crawler).
 * When deployed, the unit becomes immobile but gains multiple turrets with enhanced stats.
 */
@Slf4j
@Getter
public class DeployComponent extends AbstractUnitComponent {
    
    private static final long TOGGLE_COOLDOWN_MS = 2000; // 2 seconds
    private static final double RANGE_MULTIPLIER = 1.5;   // +50% range when deployed
    private static final double DAMAGE_MULTIPLIER = 1.5;   // +50% damage when deployed
    private static final double TURRET_OFFSET_PERCENT = 0.6; // Turret placement at 60% of unit size
    
    private boolean deployed = false;
    private long lastToggleTime = 0;
    private List<Turret> turrets = new ArrayList<>();
    private double originalMovementSpeed;

    @Override
    public void init(com.fullsteam.model.Unit unit, com.fullsteam.model.GameEntities gameEntities) {
        super.init(unit, gameEntities);
        this.originalMovementSpeed = unit.getMovementSpeed();
    }

    @Override
    public void update(com.fullsteam.model.GameEntities gameEntities) {
        // Turret updates are handled in RTSGameManager
        // This is just a placeholder for component lifecycle
    }

    /**
     * Toggle deployment state.
     *
     * @return true if successfully toggled, false if on cooldown
     */
    public boolean toggleDeploy() {
        long now = System.currentTimeMillis();
        if (now - lastToggleTime < TOGGLE_COOLDOWN_MS) {
            return false; // Still on cooldown
        }

        deployed = !deployed;
        lastToggleTime = now;

        if (deployed) {
            deploy();
        } else {
            undeploy();
        }

        return true;
    }

    /**
     * Deploy the unit (immobile, multi-turret mode).
     */
    private void deploy() {
        // Stop movement
        unit.setMovementSpeed(0);
        unit.getBody().setLinearVelocity(0, 0);

        // Create 4 turrets at corners
        turrets.clear();
        
        double baseRange = unit.getWeapon() != null ? unit.getWeapon().getRange() : unit.getUnitType().getAttackRange();
        double baseDamage = unit.getWeapon() != null ? unit.getWeapon().getDamage() : unit.getUnitType().getDamage();
        double baseRate = unit.getWeapon() != null ? unit.getWeapon().getAttackRate() : unit.getUnitType().getAttackRate();
        
        double enhancedRange = baseRange * RANGE_MULTIPLIER;
        double enhancedDamage = baseDamage * DAMAGE_MULTIPLIER;
        double turretOffset = unit.getUnitType().getSize() * TURRET_OFFSET_PERCENT;

        // Create 4 turrets at corners (relative offsets from unit center)
        turrets.add(new Turret(0, new Vector2(turretOffset, turretOffset), enhancedDamage, enhancedRange, baseRate));     // Top-right
        turrets.add(new Turret(1, new Vector2(-turretOffset, turretOffset), enhancedDamage, enhancedRange, baseRate));    // Top-left
        turrets.add(new Turret(2, new Vector2(-turretOffset, -turretOffset), enhancedDamage, enhancedRange, baseRate));   // Bottom-left
        turrets.add(new Turret(3, new Vector2(turretOffset, -turretOffset), enhancedDamage, enhancedRange, baseRate));    // Bottom-right

        log.info("Crawler {} deployed - {} turrets created with {:.1f} range and {:.1f} damage", 
                unit.getId(), turrets.size(), enhancedRange, enhancedDamage);
    }

    /**
     * Undeploy the unit (restore mobility, clear turrets).
     */
    private void undeploy() {
        // Restore movement
        unit.setMovementSpeed(originalMovementSpeed);
        
        // Clear turrets
        turrets.clear();

        log.info("Crawler {} undeployed - returning to mobile mode", unit.getId());
    }

    /**
     * Check if this unit is currently deployed.
     *
     * @return true if deployed
     */
    public boolean isDeployed() {
        return deployed;
    }

    /**
     * Check if the unit can currently attack.
     * Crawlers can only attack when deployed.
     *
     * @return true if deployed (turrets active)
     */
    public boolean canAttack() {
        return deployed && !turrets.isEmpty();
    }

    /**
     * Get the turrets (for combat/rendering).
     *
     * @return List of turrets, empty if not deployed
     */
    public List<Turret> getTurrets() {
        return turrets;
    }

    @Override
    public void onDestroy() {
        // Clean up turrets
        turrets.clear();
        log.debug("DeployComponent destroyed for unit {}", unit.getId());
    }

    @Override
    public void onGarrison() {
        // Force undeploy if garrisoned
        if (deployed) {
            deployed = false;
            undeploy();
            log.debug("Crawler {} undeployed due to garrison", unit.getId());
        }
    }
}

