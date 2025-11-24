package com.fullsteam.model.component;

import com.fullsteam.model.Building;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitType;
import com.fullsteam.util.IdGenerator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dyn4j.geometry.Vector2;

import java.util.HashSet;
import java.util.Set;

/**
 * Component that handles autonomous android production for Android Factory.
 * Automatically produces androids to maintain a maximum population.
 * When an android dies, a new one is automatically queued.
 * If the factory is destroyed, all androids are destroyed.
 * 
 * Used by: ANDROID_FACTORY
 */
@Slf4j
@Getter
public class AndroidFactoryComponent implements IBuildingComponent {
    private static final int MAX_ANDROIDS = 6; // Maximum number of androids per factory
    private static final UnitType ANDROID_TYPE = UnitType.ANDROID;
    
    private final Set<Integer> controlledAndroidIds = new HashSet<>();
    private double productionProgress = 0; // seconds
    private boolean producingAndroid = false;
    private Unit completedAndroid = null; // Holds completed android ready to spawn
    
    @Override
    public void update(double deltaTime, Building building, boolean hasLowPower) {
        // Don't produce while under construction or low power
        if (building.isUnderConstruction() || hasLowPower) {
            if (hasLowPower && producingAndroid) {
                log.debug("Android Factory {} production paused due to LOW POWER", building.getId());
            }
            return;
        }
        
        // Check if we need to produce more androids
        int currentCount = controlledAndroidIds.size();
        
        if (currentCount < MAX_ANDROIDS && !producingAndroid) {
            // Start production
            producingAndroid = true;
            productionProgress = 0;
            log.info("Android Factory {} started producing android ({}/{})", 
                    building.getId(), currentCount, MAX_ANDROIDS);
        }
        
        // Update production progress
        if (producingAndroid && !hasLowPower) {
            productionProgress += deltaTime;
            
            // Check if production is complete
            if (productionProgress >= ANDROID_TYPE.getBuildTimeSeconds() && completedAndroid == null) {
                // Spawn the android
                Vector2 spawnPos = findSpawnPosition(building);
                
                Unit android = new Unit(
                        IdGenerator.nextEntityId(),
                        ANDROID_TYPE,
                        spawnPos.x, spawnPos.y,
                        building.getOwnerId(),
                        building.getTeamNumber()
                );
                
                // Link android to its factory
                android.setAndroidFactoryId(building.getId());
                
                // Register android with factory
                registerAndroid(android.getId());
                
                // Store completed android for RTSGameManager to add to world
                completedAndroid = android;
                
                // Reset production state
                producingAndroid = false;
                productionProgress = 0;
                
                log.info("Android Factory {} completed android production - spawned Android {} ({}/{})", 
                        building.getId(), android.getId(), 
                        controlledAndroidIds.size(), MAX_ANDROIDS);
            }
        }
    }
    
    /**
     * Find a valid spawn position near the factory.
     * Simple approach: try positions around the factory in a circle.
     */
    private Vector2 findSpawnPosition(Building factory) {
        Vector2 factoryPos = factory.getPosition();
        double factorySize = factory.getBuildingType().getSize();
        double spawnDistance = factorySize + 50; // Spawn 50 units away from edge
        
        // Try 8 positions around the factory
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 * i) / 8;
            double x = factoryPos.x + Math.cos(angle) * spawnDistance;
            double y = factoryPos.y + Math.sin(angle) * spawnDistance;
            
            // For now, just return the first position
            // RTSGameManager will handle collision validation if needed
            return new Vector2(x, y);
        }
        
        // Fallback: spawn directly to the right
        return new Vector2(factoryPos.x + spawnDistance, factoryPos.y);
    }
    
    @Override
    public void onConstructionComplete(Building building) {
        log.info("Android Factory {} construction complete - starting android production", building.getId());
    }
    
    @Override
    public void onDestroy(Building building) {
        log.info("Android Factory {} destroyed - {} androids will be destroyed", 
                building.getId(), controlledAndroidIds.size());
        // Androids will be destroyed by RTSGameManager
    }
    
    /**
     * Check if an android is ready to spawn and return it.
     * This consumes the completed android.
     * 
     * @return Completed android unit, or null if none ready
     */
    public Unit getCompletedAndroid() {
        if (completedAndroid != null) {
            Unit android = completedAndroid;
            completedAndroid = null;
            return android;
        }
        return null;
    }
    
    /**
     * Check if an android is ready to spawn.
     * 
     * @return true if android is ready
     */
    public boolean hasCompletedAndroid() {
        return completedAndroid != null;
    }
    
    /**
     * Register an android as controlled by this factory.
     * 
     * @param androidId The ID of the android unit
     */
    public void registerAndroid(int androidId) {
        controlledAndroidIds.add(androidId);
        log.debug("Android Factory registered android {} ({}/{} androids)", 
                androidId, controlledAndroidIds.size(), MAX_ANDROIDS);
    }
    
    /**
     * Unregister an android (when it dies).
     * This will trigger automatic replacement production.
     * 
     * @param androidId The ID of the android unit
     */
    public void unregisterAndroid(int androidId) {
        if (controlledAndroidIds.remove(androidId)) {
            log.info("Android {} destroyed - Android Factory will produce replacement ({}/{} androids)", 
                    androidId, controlledAndroidIds.size(), MAX_ANDROIDS);
        }
    }
    
    /**
     * Get all controlled android IDs.
     * Used when factory is destroyed to destroy all androids.
     * 
     * @return Set of android unit IDs
     */
    public Set<Integer> getControlledAndroidIds() {
        return new HashSet<>(controlledAndroidIds);
    }
    
    /**
     * Get the current number of androids.
     */
    public int getAndroidCount() {
        return controlledAndroidIds.size();
    }
    
    /**
     * Get the maximum number of androids.
     */
    public int getMaxAndroids() {
        return MAX_ANDROIDS;
    }
    
    /**
     * Check if currently producing an android.
     */
    public boolean isProducing() {
        return producingAndroid;
    }
    
    /**
     * Get production progress (0.0 to 1.0).
     */
    public double getProductionProgressPercent() {
        if (!producingAndroid) {
            return 0.0;
        }
        return Math.min(1.0, productionProgress / ANDROID_TYPE.getBuildTimeSeconds());
    }
}


