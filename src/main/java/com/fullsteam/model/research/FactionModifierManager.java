package com.fullsteam.model.research;

import com.fullsteam.model.Building;
import com.fullsteam.model.PlayerFaction;
import com.fullsteam.model.RTSGameManager;
import com.fullsteam.model.Targetable;
import com.fullsteam.model.Unit;
import com.fullsteam.model.UnitCategory;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.customization.FactionPerk;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages available units and active perks for a faction.
 * Replaces the old research system - units are now selected during faction customization
 * instead of being unlocked through research.
 * Also manages perk lifecycle hooks.
 */
@Slf4j
@Getter
public class FactionModifierManager {

    private final int playerId;

    // Available units (set by custom faction configuration)
    private Map<UnitCategory, Set<UnitType>> availableUnits = new HashMap<>();

    // Active perks (FactionPerk enum implements PerkEffect)
    private Set<FactionPerk> activePerks = new HashSet<>();

    public FactionModifierManager(int playerId) {
        this.playerId = playerId;
        log.info("Player {} - Initialized FactionModifierManager", playerId);
    }

    /**
     * Set the available units for this faction (from custom faction configuration)
     */
    public void setAvailableUnits(Set<UnitType> units) {
        // Group units by category
        Map<UnitCategory, Set<UnitType>> unitsByCategory = new HashMap<>();
        for (UnitType unit : units) {
            unitsByCategory.computeIfAbsent(unit.getCategory(), k -> new HashSet<>())
                    .add(unit);
        }

        this.availableUnits = unitsByCategory;
        log.info("Player {} - Set available units: {}", playerId, unitsByCategory);
    }

    /**
     * Get available units for a category
     */
    public Set<UnitType> getAvailableUnits(UnitCategory category) {
        return availableUnits.getOrDefault(category, Set.of());
    }

    /**
     * Get all available units across all categories
     */
    public Map<UnitCategory, Set<UnitType>> getAllAvailableUnits() {
        return new HashMap<>(availableUnits);
    }

    /**
     * Check if a unit type is available for production
     */
    public boolean isUnitAvailable(UnitType unitType) {
        Set<UnitType> categoryUnits = availableUnits.get(unitType.getCategory());
        return categoryUnits != null && categoryUnits.contains(unitType);
    }

    /**
     * Set active perks (from custom faction configuration).
     * FactionPerk enum implements PerkEffect, so no separate registry needed.
     */
    public void setActivePerks(Set<FactionPerk> perks) {
        this.activePerks = new HashSet<>(perks);
        log.info("Player {} - Set {} active perks", playerId, activePerks.size());
    }

    /**
     * Check if a perk is active
     */
    public boolean hasPerk(FactionPerk perk) {
        return activePerks.contains(perk);
    }

    // ============================================================================
    // Lifecycle Hook Methods
    // ============================================================================

    /**
     * Called when a unit is created for this faction
     */
    public void onUnitCreated(Unit unit, PlayerFaction faction, RTSGameManager game) {
        for (FactionPerk perk : activePerks) {
            try {
                perk.onUnitCreated(unit, faction, game);
            } catch (Exception e) {
                log.error("Error in perk {} onUnitCreated", perk, e);
            }
        }
    }

    /**
     * Called when a unit belonging to this faction is destroyed
     */
    public void onUnitDestroyed(Unit unit, PlayerFaction faction, RTSGameManager game) {
        for (FactionPerk perk : activePerks) {
            try {
                perk.onUnitDestroyed(unit, faction, game);
            } catch (Exception e) {
                log.error("Error in perk {} onUnitDestroyed", perk, e);
            }
        }
    }

    /**
     * Called when a building is created for this faction
     */
    public void onBuildingCreated(Building building, PlayerFaction faction, RTSGameManager game) {
        for (FactionPerk perk : activePerks) {
            try {
                perk.onBuildingCreated(building, faction, game);
            } catch (Exception e) {
                log.error("Error in perk {} onBuildingCreated", perk, e);
            }
        }
    }

    /**
     * Called when a building belonging to this faction is destroyed
     */
    public void onBuildingDestroyed(Building building, PlayerFaction faction, RTSGameManager game) {
        for (FactionPerk perk : activePerks) {
            try {
                perk.onBuildingDestroyed(building, faction, game);
            } catch (Exception e) {
                log.error("Error in perk {} onBuildingDestroyed", perk, e);
            }
        }
    }

    /**
     * Called when a unit belonging to this faction deals damage
     */
    public void onUnitDealsDamage(Unit attacker, Targetable target, double damage, PlayerFaction faction, RTSGameManager game) {
        for (FactionPerk perk : activePerks) {
            try {
                perk.onUnitDealsDamage(attacker, target, damage, faction, game);
            } catch (Exception e) {
                log.error("Error in perk {} onUnitDealsDamage", perk, e);
            }
        }
    }

    /**
     * Modify income produced by a building
     */
    public double modifyBuildingIncome(Building building, PlayerFaction faction, double baseIncome) {
        double modified = baseIncome;
        for (FactionPerk perk : activePerks) {
            try {
                modified = perk.modifyBuildingIncome(building, faction, modified);
            } catch (Exception e) {
                log.error("Error in perk {} modifyBuildingIncome", perk, e);
            }
        }
        return modified;
    }

    /**
     * Modify monument buff strength
     */
    public double modifyMonumentBuffStrength(Building monument, PlayerFaction faction, double baseStrength) {
        double modified = baseStrength;
        for (FactionPerk perk : activePerks) {
            try {
                modified = perk.modifyMonumentBuffStrength(monument, faction, modified);
            } catch (Exception e) {
                log.error("Error in perk {} modifyMonumentBuffStrength", perk, e);
            }
        }
        return modified;
    }
}
