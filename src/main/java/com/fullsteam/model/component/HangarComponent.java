package com.fullsteam.model.component;

import com.fullsteam.games.IdGenerator;
import com.fullsteam.model.Building;
import com.fullsteam.model.PlayerFaction;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.research.ResearchModifier;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

/**
 * Component for Hangar buildings that produce and house sortie-based aircraft.
 * Each hangar produces ONE aircraft at a time that is linked to it (similar to AndroidFactory pattern).
 * The aircraft doesn't exist in the game world until deployed on a sortie.
 * <p>
 * Key Concepts:
 * - Hangar produces aircraft on player order (costs resources + build time)
 * - Once produced, aircraft is "housed" - persists between sorties
 * - Only one aircraft per hangar at a time
 * - Aircraft can be repaired while housed
 * - When sortie is ordered, aircraft is spawned, executes mission, then returns
 * - If aircraft is destroyed on sortie, hangar can produce a new one
 * - Future: Could support multiple aircraft types (Bomber, Gunship, etc.)
 * <p>
 * Used by: HANGAR
 */
@Slf4j
@Getter
public class HangarComponent extends AbstractBuildingComponent {

    // The aircraft unit housed in this hangar (null if empty or not yet produced)
    private Unit housedAircraft;

    // Whether the aircraft is currently deployed on a sortie
    private boolean isOnSortie;
    
    // SCRAMBLE mode - auto-deploy interceptors when enemy aircraft detected
    @Setter
    private boolean scrambleEnabled = false;
    private static final double SCRAMBLE_DETECTION_RANGE = 600.0; // Range to detect enemy aircraft

    // Production state
    private boolean producingAircraft = false;
    private UnitType producingType = null; // Type currently being produced
    private double productionProgress = 0; // seconds
    private ResearchModifier modifier = new ResearchModifier();
    
    @Setter
    private Vector2 rallyPoint;

    // Whether the hangar is ready for sortie (aircraft housed, not damaged, not already deployed)
    public boolean isReadyForSortie() {
        return housedAircraft != null && !isOnSortie && housedAircraft.getHealth() > 0;
    }
    
    /**
     * Check if hangar can start producing an aircraft.
     * Requires: no existing aircraft AND not currently producing
     */
    public boolean canProduceAircraft() {
        return housedAircraft == null && !producingAircraft;
    }
    
    /**
     * Start producing an aircraft (called by RTSGameManager when player orders production)
     * @param unitType The type of aircraft to produce (must be sortie-based)
     * @return true if production started, false if already has aircraft or is producing
     */
    public boolean startAircraftProduction(UnitType unitType) {
        if (!canProduceAircraft()) {
            log.warn("Hangar {} cannot start production (already has aircraft or is producing)", building.getId());
            return false;
        }
        
        if (!unitType.isSortieBased()) {
            log.warn("Hangar {} cannot produce non-sortie unit type {}", building.getId(), unitType);
            return false;
        }
        
        producingAircraft = true;
        producingType = unitType;
        productionProgress = 0;
        log.info("Hangar {} started producing {}", building.getId(), unitType.getDisplayName());
        return true;
    }
    
    /**
     * Get production progress (0.0 to 1.0).
     */
    public double getProductionProgressPercent() {
        if (!producingAircraft || producingType == null) {
            return 0.0;
        }
        return Math.min(1.0, productionProgress / (producingType.getBuildTimeSeconds() / modifier.getProductionSpeedMultiplier()));
    }
    
    @Override
    public void update(boolean hasLowPower) {
        double deltaTime = gameEntities.getWorld().getTimeStep().getDeltaTime();
        
        // Don't produce while under construction or low power
        if (building.isUnderConstruction() || hasLowPower) {
            if (hasLowPower && producingAircraft) {
                log.debug("Hangar {} production paused due to LOW POWER", building.getId());
            }
            return;
        }
        
        // Update production progress (only if player has ordered production)
        if (producingAircraft && producingType != null) {
            productionProgress += deltaTime;
            
            // Check if production is complete
            if ((productionProgress * modifier.getProductionSpeedMultiplier()) >= producingType.getBuildTimeSeconds()) {
                // Create the aircraft unit (but keep it housed, not in world)
                Vector2 hangarPos = building.getPosition();
                
                Unit aircraft = new Unit(
                        IdGenerator.nextEntityId(),
                        producingType,
                        hangarPos.x,
                        hangarPos.y,
                        building.getOwnerId(),
                        building.getTeamNumber()
                );
                
                // Initialize components
                aircraft.initializeComponents(gameEntities);
                
                // Apply research modifiers
                PlayerFaction faction = gameEntities.getPlayerFactions().get(building.getOwnerId());
                if (faction != null && faction.getResearchManager() != null) {
                    aircraft.applyResearchModifiers(faction.getResearchManager().getCumulativeModifier());
                }
                
                // House the aircraft (keeps it out of game world until sortie)
                houseAircraft(aircraft);
                
                // Reset production state
                producingAircraft = false;
                producingType = null;
                productionProgress = 0;
                
                log.info("Hangar {} completed {} production - aircraft housed and ready for sortie", 
                        building.getId(), aircraft.getUnitType().getDisplayName());
            }
        }
        
        // Passive repair over time (slow) when aircraft is housed
        if (!hasLowPower && housedAircraft != null && !isOnSortie) {
            double repairRate = 2.0; // 2 HP per second when not on low power
            repairAircraft(repairRate * deltaTime);
        }
        
        // SCRAMBLE: Auto-deploy interceptors when enemy aircraft detected
        if (scrambleEnabled && isReadyForSortie() && housedAircraft.getUnitType() == UnitType.INTERCEPTOR) {
            checkAndScramble();
        }
    }

    /**
     * House a bomber in this hangar (called after production completes)
     *
     * @param aircraft The bomber to house
     * @return true if successful, false if hangar is already occupied
     */
    private boolean houseAircraft(Unit aircraft) {
        if (housedAircraft != null) {
            log.warn("Hangar {} already has aircraft housed: {}", building.getId(), housedAircraft);
            return false;
        }

        if (!aircraft.getUnitType().isSortieBased()) {
            log.warn("Cannot house non-sortie unit {} in hangar {}", aircraft.getUnitType(), building.getId());
            return false;
        }

        this.housedAircraft = aircraft;
        this.isOnSortie = false;
        
        // Don't add to game world yet - bomber stays housed
        aircraft.getBody().setEnabled(false);
        aircraft.setGarrisoned(true);

        log.info("Hangar {} now houses bomber {}", building.getId(), aircraft.getId());
        return true;
    }

    /**
     * Launch the housed bomber for a sortie.
     * Spawns the bomber into the game world.
     *
     * @return The launched bomber unit, or null if no bomber available
     */
    public Unit launchBomber() {
        if (housedAircraft == null) {
            log.warn("Hangar {} has no bomber to launch", building.getId());
            return null;
        }
        if (isOnSortie) {
            log.warn("Bomber in Hangar {} is already deployed", building.getId());
            return null;
        }
        
        // Enable physics and add to game world
        housedAircraft.getBody().setEnabled(true);
        housedAircraft.setGarrisoned(false);
        gameEntities.getUnits().put(housedAircraft.getId(), housedAircraft);
        gameEntities.getWorld().addBody(housedAircraft.getBody());
        
        this.isOnSortie = true;
        log.info("Hangar {} launched bomber {} for sortie", building.getId(), housedAircraft.getId());
        return housedAircraft;
    }

    /**
     * Mark the aircraft as deployed on a sortie
     *
     * @param unitId The ID of the spawned unit
     */
    public void deployOnSortie(int unitId) {
        if (housedAircraft == null) {
            log.error("Cannot deploy from empty hangar {}", building.getId());
            return;
        }

        if (isOnSortie) {
            log.warn("Hangar {} already has aircraft deployed on sortie", building.getId());
            return;
        }

        this.isOnSortie = true;
        log.info("Hangar {} deployed bomber {} on sortie", building.getId(), unitId);
    }

    /**
     * Aircraft has returned from sortie
     *
     * @param returnedUnit The unit that returned (for health tracking)
     */
    public void returnFromSortie(Unit returnedUnit) {
        if (!isOnSortie) {
            log.warn("Hangar {} received return but no sortie was active", building.getId());
            return;
        }

        if (returnedUnit != null && housedAircraft != null && returnedUnit.getId() == housedAircraft.getId()) {
            // Update housed aircraft with current health
            this.housedAircraft = returnedUnit;
            
            // Remove from game world
            returnedUnit.getBody().setEnabled(false);
            returnedUnit.setGarrisoned(true);
            gameEntities.getUnits().remove(returnedUnit.getId());
            gameEntities.getWorld().removeBody(returnedUnit.getBody());
            
            log.info("Hangar {} bomber returned from sortie with {} health", building.getId(), returnedUnit.getHealth());
        } else {
            // Aircraft was destroyed during sortie
            log.warn("Hangar {} bomber was destroyed during sortie - can produce new one", building.getId());
            this.housedAircraft = null;
        }
        this.isOnSortie = false;
    }

    /**
     * Repair the housed aircraft (when not on sortie)
     *
     * @param repairAmount Amount of health to repair
     */
    public void repairAircraft(double repairAmount) {
        if (housedAircraft == null) {
            return; // No aircraft to repair
        }

        if (isOnSortie) {
            return; // Cannot repair while deployed
        }

        housedAircraft.setHealth(Math.min(housedAircraft.getHealth() + repairAmount, housedAircraft.getMaxHealth()));
    }

    /**
     * Remove the aircraft from the hangar (e.g., if destroyed or scrapped)
     */
    public void clearAircraft() {
        log.info("Hangar {} bomber cleared - can produce new one", building.getId());
        this.housedAircraft = null;
        this.isOnSortie = false;
    }

    @Override
    public void onDestroy() {
        if (isOnSortie && housedAircraft != null) {
            // Bomber is on sortie - it will be destroyed when it tries to return
            log.warn("Hangar {} destroyed while bomber on sortie - bomber will be lost when it tries to return",
                    building.getId());
        } else if (housedAircraft != null) {
            // Bomber is housed - destroy it
            log.warn("Hangar {} destroyed with housed bomber - bomber destroyed", building.getId());
            housedAircraft.setActive(false);
        }
    }
    
    @Override
    public void applyResearchModifiers(ResearchModifier modifier) {
        this.modifier = modifier;
    }
    
    /**
     * Check for nearby enemy aircraft and scramble interceptor if detected.
     * Only called when SCRAMBLE is enabled and interceptor is ready.
     */
    private void checkAndScramble() {
        Vector2 hangarPos = building.getPosition();
        int teamNumber = building.getTeamNumber();
        
        // Scan for enemy air units within detection range
        for (Unit enemyUnit : gameEntities.getUnits().values()) {
            if (enemyUnit.getTeamNumber() == teamNumber || !enemyUnit.isActive()) {
                continue;
            }
            
            // Only scramble for airborne threats
            if (!enemyUnit.getUnitType().getElevation().isAirborne()) {
                continue;
            }
            
            double distance = hangarPos.distance(enemyUnit.getPosition());
            if (distance <= SCRAMBLE_DETECTION_RANGE) {
                log.info("SCRAMBLE! Hangar {} detected enemy {} at range {} - launching interceptor", 
                        building.getId(), enemyUnit.getUnitType().getDisplayName(), (int)distance);
                
                // Launch interceptor to attack the detected aircraft
                launchInterceptorToAttack(enemyUnit);
                break; // Only launch once per detection
            }
        }
    }
    
    /**
     * Launch the housed interceptor to attack a specific target.
     */
    private void launchInterceptorToAttack(Unit target) {
        if (housedAircraft == null || isOnSortie) {
            return;
        }
        
        // Add aircraft to world
        housedAircraft.setGarrisoned(false);
        housedAircraft.getBody().setEnabled(true);
        housedAircraft.getBody().translate(building.getPosition().x, building.getPosition().y);
        gameEntities.getUnits().put(housedAircraft.getId(), housedAircraft);
        gameEntities.getWorld().addBody(housedAircraft.getBody());
        
        // Mark as on sortie
        isOnSortie = true;
        
        // Deploy interceptor component
        housedAircraft.getComponent(com.fullsteam.model.component.InterceptorComponent.class)
                .ifPresent(comp -> comp.deploy(building.getId()));
        
        // Issue attack command
        housedAircraft.issueCommand(
                new com.fullsteam.model.command.AttackUnitCommand(housedAircraft, target, false),
                gameEntities
        );
        
        log.info("Interceptor {} scrambled to engage {}", housedAircraft.getId(), target.getId());
    }
}


