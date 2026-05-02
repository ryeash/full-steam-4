package com.fullsteam.model;

import lombok.Data;
import org.dyn4j.geometry.Vector2;

import java.util.List;

/**
 * Carries a single player action to the server each tick.
 *
 * <p>Every message must have an {@link #action}.  The remaining fields are
 * contextual: which fields are meaningful depends entirely on which action is
 * set.  See each {@link InputAction} constant's Javadoc for the contract.
 *
 * <p>When {@link #unitIds} is present on a <em>non-SELECT</em> action it acts
 * as an ephemeral unit scope for that command only — the player's persistent
 * server-side selection is not updated.  This is used by AI behaviors that
 * must select and command units in a single message.
 */
@Data
public class RTSPlayerInput {

    /**
     * The action being requested. Must not be null.
     */
    private InputAction action;

    // -------------------------------------------------------------------------
    // Unit scope
    // -------------------------------------------------------------------------

    /**
     * For {@link InputAction#SELECT}: the unit IDs to select.
     * For any other action: optional ephemeral scope (AI use).
     */
    private List<Integer> unitIds;

    /**
     * Only meaningful for {@link InputAction#SELECT}.
     */
    private boolean addToSelection;

    // -------------------------------------------------------------------------
    // Generic entity references
    // -------------------------------------------------------------------------

    /**
     * Primary target entity (unit, building, or obstacle).
     * Semantics depend on {@link #action}; see {@link InputAction} Javadoc.
     */
    private Integer targetEntityId;

    /**
     * Secondary entity reference.
     * Used for: housed unit in SORTIE/RTB/SCRAP; specific unit in UNGARRISON;
     * source building in COMMAND_ABILITY.
     */
    private Integer auxiliaryEntityId;

    // -------------------------------------------------------------------------
    // World position
    // -------------------------------------------------------------------------

    /**
     * World-space position target.
     * Used as move destination, build location, rally point, sortie strike point, etc.
     */
    private Vector2 targetPosition;

    // -------------------------------------------------------------------------
    // Typed enum values (at most one populated per action)
    // -------------------------------------------------------------------------

    /**
     * For {@link InputAction#BUILD}.
     */
    private BuildingType buildingType;

    /**
     * For {@link InputAction#PRODUCE_UNIT}.
     */
    private UnitType unitType;

    /**
     * For {@link InputAction#SET_STANCE}.
     */
    private AIStance aiStance;

    /**
     * For {@link InputAction#COMMAND_ABILITY}.
     */
    private CommandAbilityType commandAbilityType;

    // -------------------------------------------------------------------------
    // Modifier flags
    // -------------------------------------------------------------------------

    /**
     * For {@link InputAction#UNGARRISON}: ungarrison every occupant.
     */
    private boolean ungarrisonAll;
}
