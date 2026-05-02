package com.fullsteam.model;

/**
 * Identifies the single player action carried by an {@link RTSPlayerInput}.
 * Each value maps 1-to-1 to a private handler method in RTSGameManager.
 */
public enum InputAction {
    /**
     * Update the server-side unit selection for this player.
     */
    SELECT,

    /**
     * Move selected units to {@code targetPosition}.
     */
    MOVE,

    /**
     * Attack-move selected units to {@code targetPosition}.
     */
    ATTACK_MOVE,

    /**
     * Order selected combat units to attack the unit at {@code targetEntityId}.
     */
    ATTACK_UNIT,

    /**
     * Order selected combat units to attack the building at {@code targetEntityId}.
     */
    ATTACK_BUILDING,

    /**
     * Force-attack ground at {@code targetPosition}.
     */
    FORCE_ATTACK,

    /**
     * Order selected workers to harvest the obstacle at {@code targetEntityId}.
     */
    HARVEST,

    /**
     * Order selected workers to assist constructing the building at {@code targetEntityId}.
     */
    CONSTRUCT,

    /**
     * Place a new building of {@code buildingType} at {@code targetPosition}.
     */
    BUILD,

    /**
     * Cancel the under-construction building at {@code targetEntityId} (full refund).
     */
    CANCEL_CONSTRUCTION,

    /**
     * Queue {@code unitType} for production at the building {@code targetEntityId}.
     */
    PRODUCE_UNIT,

    /**
     * Cancel the last queued/active production at the building {@code targetEntityId} (LIFO).
     */
    CANCEL_PRODUCTION,

    /**
     * Set the rally point of building {@code targetEntityId} to {@code targetPosition}.
     */
    SET_RALLY,

    /**
     * Halt all current commands on selected units.
     */
    STOP,

    /**
     * Scatter selected units away from their collective centre.
     */
    SCATTER,

    /**
     * Change AI stance of selected units to {@code aiStance}.
     */
    SET_STANCE,

    /**
     * Activate the special ability of selected units.
     * {@code targetEntityId} holds the target unit or building id when required.
     */
    SPECIAL_ABILITY,

    /**
     * Trigger a strategic command ability.
     * {@code commandAbilityType} names the ability; {@code targetPosition} and
     * {@code auxiliaryEntityId} (source building) are optional.
     */
    COMMAND_ABILITY,

    /**
     * Garrison selected infantry into the transport/bunker at {@code targetEntityId}
     * (may be either a unit id or a building id).
     */
    GARRISON,

    /**
     * Ungarrison from the carrier at {@code targetEntityId}.
     * {@code auxiliaryEntityId} holds the specific unit to ungarrison (omit for FIFO).
     * {@code ungarrisonAll} ungarrisons every occupant.
     */
    UNGARRISON,

    /**
     * Launch a housed aircraft on a sortie.
     * {@code targetEntityId} = airfield id, {@code auxiliaryEntityId} = housed unit id,
     * {@code targetPosition} = strike location.
     */
    SORTIE,

    /**
     * Recall a deployed aircraft back to its airfield.
     * {@code targetEntityId} = airfield id, {@code auxiliaryEntityId} = unit id.
     */
    RTB,

    /**
     * Scrap a housed (non-deployed) aircraft at an airfield — no refund.
     * {@code targetEntityId} = airfield id, {@code auxiliaryEntityId} = unit id.
     */
    SCRAP
}
