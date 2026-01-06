package com.fullsteam.model.research;

import com.fullsteam.model.UnitCategory;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.Faction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages available units for a faction.
 * Replaces the old research system - units are now selected during faction customization
 * instead of being unlocked through research.
 */
@Slf4j
@Getter
public class FactionModifierManager {

    private final int playerId;
    private final Faction faction;
    
    // Available units (set by custom faction configuration)
    private Map<UnitCategory, Set<UnitType>> availableUnits = new HashMap<>();

    public FactionModifierManager(int playerId, Faction faction) {
        this.playerId = playerId;
        this.faction = faction;
        log.info("Player {} - Initialized FactionModifierManager for faction: {}", playerId, faction);
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
}
