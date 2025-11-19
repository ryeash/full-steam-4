package com.fullsteam.model;

import lombok.Data;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Represents player input for RTS gameplay.
 * Contains commands for unit selection, movement, attacks, building, etc.
 */
@Data
public class RTSPlayerInput {
    // Unit selection
    private List<Integer> selectUnits; // List of unit IDs to select
    private boolean addToSelection; // If true, add to existing selection instead of replacing
    
    // Movement orders
    private Vector2 moveOrder; // Destination for selected units
    private Vector2 attackMoveOrder; // Attack-move destination (units attack enemies while moving)
    
    // Attack orders
    private Integer attackUnitOrder; // Target unit ID
    private Integer attackBuildingOrder; // Target building ID
    private Integer attackWallSegmentOrder; // Target wall segment ID
    private Vector2 forceAttackOrder; // Force attack ground at position (CMD/CTRL + right click)
    
    // Resource gathering
    private Integer harvestOrder; // Resource deposit ID
    
    // Obstacle mining
    private Integer mineOrder; // Obstacle ID to mine/destroy
    
    // Building construction
    private BuildingType buildOrder; // Type of building to construct
    private Vector2 buildLocation; // Where to place the building
    private Integer constructOrder; // Building ID to help construct (for workers)
    
    // Unit production
    private UnitType produceUnitOrder; // Type of unit to produce
    private Integer produceBuildingId; // Building ID to produce from
    
    // Rally point
    private Integer setRallyBuildingId; // Building to set rally point for
    private Vector2 rallyPoint; // Rally point location
    
    // Stop command
    private boolean stopCommand; // Stop all selected units
    
    // AI stance
    private AIStance setStance; // Change AI stance for selected units
    
    // Special abilities
    private boolean activateSpecialAbility; // Activate special ability for selected units
    private Integer specialAbilityTargetUnit; // Target unit ID for heal/repair
    private Integer specialAbilityTargetBuilding; // Target building ID for repair
    
    // Camera/viewport
    private double cameraX;
    private double cameraY;
    private double cameraZoom;
}

