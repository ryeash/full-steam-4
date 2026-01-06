package com.fullsteam.model.customization;

import lombok.Getter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Perks that can be selected during faction customization.
 * Each perk has a point cost and may depend on other perks.
 */
@Getter
public enum FactionPerk {
    // ===== ECONOMIC PERKS =====
    POWER_EFFICIENCY_1(
        "Power Efficiency I", 
        "-15% power consumption", 
        3,
        Set.of()
    ),
    POWER_EFFICIENCY_2(
        "Power Efficiency II", 
        "-30% power consumption", 
        6,
        Set.of("POWER_EFFICIENCY_1")
    ),
    POWER_EFFICIENCY_3(
        "Power Efficiency III", 
        "-45% power consumption", 
        10,
        Set.of("POWER_EFFICIENCY_2")
    ),
    
    RESOURCE_BOOST_1(
        "Resource Boost I", 
        "+15% harvesting rate", 
        4,
        Set.of()
    ),
    RESOURCE_BOOST_2(
        "Resource Boost II", 
        "+30% harvesting rate", 
        8,
        Set.of("RESOURCE_BOOST_1")
    ),
    
    COST_REDUCTION_1(
        "Cost Reduction I", 
        "-10% all costs", 
        5,
        Set.of()
    ),
    COST_REDUCTION_2(
        "Cost Reduction II", 
        "-20% all costs", 
        9,
        Set.of("COST_REDUCTION_1")
    ),
    
    BANK_EFFICIENCY(
        "Banking Efficiency", 
        "+25% credit income from Banks", 
        4,
        Set.of()
    ),
    
    // ===== MILITARY PERKS =====
    UPKEEP_INCREASE_1(
        "Increased Upkeep I", 
        "+25% upkeep limit", 
        3,
        Set.of()
    ),
    UPKEEP_INCREASE_2(
        "Increased Upkeep II", 
        "+50% upkeep limit", 
        6,
        Set.of("UPKEEP_INCREASE_1")
    ),
    UPKEEP_INCREASE_3(
        "Increased Upkeep III", 
        "+75% upkeep limit", 
        10,
        Set.of("UPKEEP_INCREASE_2")
    ),
    
    VETERAN_UNITS_1(
        "Veteran Units I", 
        "+10% HP for all units", 
        3,
        Set.of()
    ),
    VETERAN_UNITS_2(
        "Veteran Units II", 
        "+20% HP for all units", 
        6,
        Set.of("VETERAN_UNITS_1")
    ),
    
    RAPID_DEPLOYMENT_1(
        "Rapid Deployment I", 
        "-15% build time", 
        3,
        Set.of()
    ),
    RAPID_DEPLOYMENT_2(
        "Rapid Deployment II", 
        "-30% build time", 
        6,
        Set.of("RAPID_DEPLOYMENT_1")
    ),
    
    DAMAGE_BOOST_1(
        "Damage Boost I",
        "+10% damage for all units",
        4,
        Set.of()
    ),
    DAMAGE_BOOST_2(
        "Damage Boost II",
        "+20% damage for all units",
        8,
        Set.of("DAMAGE_BOOST_1")
    ),
    
    // ===== DEFENSIVE PERKS =====
    FORTIFIED_1(
        "Fortified Structures I", 
        "+15% building HP", 
        3,
        Set.of()
    ),
    FORTIFIED_2(
        "Fortified Structures II", 
        "+30% building HP", 
        6,
        Set.of("FORTIFIED_1")
    ),
    
    SHIELD_MASTERY_1(
        "Shield Mastery I", 
        "+20% shield regen rate", 
        3,
        Set.of()
    ),
    SHIELD_MASTERY_2(
        "Shield Mastery II", 
        "+40% shield regen rate", 
        6,
        Set.of("SHIELD_MASTERY_1")
    ),
    
    TURRET_EFFICIENCY(
        "Turret Efficiency", 
        "-25% turret cost, +10% turret damage", 
        4,
        Set.of()
    ),
    
    POINT_DEFENSE(
        "Point Defense System",
        "Turrets have 15% chance to shoot down incoming projectiles",
        6,
        Set.of("TURRET_EFFICIENCY")
    ),
    
    BUNKER_MASTERY(
        "Bunker Mastery",
        "Bunkers hold 6 units (up from 4) and have +25% HP",
        4,
        Set.of()
    ),
    
    // ===== TECH PERKS =====
    ADVANCED_RESEARCH_1(
        "Advanced Research I", 
        "+20% research speed", 
        3,
        Set.of()
    ),
    ADVANCED_RESEARCH_2(
        "Advanced Research II", 
        "+40% research speed", 
        6,
        Set.of("ADVANCED_RESEARCH_1")
    ),
    
    PARALLEL_RESEARCH_1(
        "Multi-tasking I", 
        "2 simultaneous research projects", 
        3,
        Set.of()
    ),
    PARALLEL_RESEARCH_2(
        "Multi-tasking II", 
        "3 simultaneous research projects", 
        6,
        Set.of("PARALLEL_RESEARCH_1")
    ),
    PARALLEL_RESEARCH_3(
        "Multi-tasking III", 
        "4 simultaneous research projects", 
        10,
        Set.of("PARALLEL_RESEARCH_2")
    ),
    
    // ===== SPECIALIZED PERKS =====
    AIR_SUPERIORITY_1(
        "Air Superiority I", 
        "-15% air unit costs, +10% air unit speed", 
        4,
        Set.of()
    ),
    AIR_SUPERIORITY_2(
        "Air Superiority II", 
        "-25% air unit costs, +20% air unit speed", 
        7,
        Set.of("AIR_SUPERIORITY_1")
    ),
    
    MECHANIZED_WARFARE_1(
        "Mechanized Warfare I",
        "-15% vehicle costs, +10% vehicle HP",
        4,
        Set.of()
    ),
    MECHANIZED_WARFARE_2(
        "Mechanized Warfare II",
        "-25% vehicle costs, +20% vehicle HP",
        7,
        Set.of("MECHANIZED_WARFARE_1")
    ),
    
    INFANTRY_DOCTRINE_1(
        "Infantry Doctrine I",
        "-15% infantry costs, +10% infantry damage",
        4,
        Set.of()
    ),
    INFANTRY_DOCTRINE_2(
        "Infantry Doctrine II",
        "-25% infantry costs, +20% infantry damage",
        7,
        Set.of("INFANTRY_DOCTRINE_1")
    ),
    
    MONUMENT_MASTERY(
        "Monument Mastery",
        "Monument buildings provide 50% stronger buffs",
        8,
        Set.of()
    ),
    
    SALVAGE_OPERATIONS(
        "Salvage Operations",
        "Recover 30% of unit/building cost when destroyed",
        5,
        Set.of()
    ),
    
    LOGISTICS_NETWORK(
        "Logistics Network",
        "Buildings cost 15% less, build 20% faster",
        6,
        Set.of()
    );
    
    private final String displayName;
    private final String description;
    private final int pointCost;
    private final Set<String> dependsOnNames; // Store as names to avoid forward reference issues
    
    FactionPerk(String displayName, String description, int pointCost, Set<String> dependsOnNames) {
        this.displayName = displayName;
        this.description = description;
        this.pointCost = pointCost;
        this.dependsOnNames = dependsOnNames;
    }
    
    /**
     * Get the perks this perk depends on (resolved at runtime)
     */
    public Set<FactionPerk> getDependsOn() {
        return dependsOnNames.stream()
            .map(FactionPerk::valueOf)
            .collect(Collectors.toSet());
    }
    
    /**
     * Check if this perk's dependencies are satisfied
     */
    public boolean canSelect(Set<FactionPerk> selectedPerks) {
        return selectedPerks.containsAll(getDependsOn());
    }
    
    /**
     * Get all perks that depend on this one (for UI warnings)
     */
    public Set<FactionPerk> getDependents() {
        Set<FactionPerk> dependents = new HashSet<>();
        for (FactionPerk perk : values()) {
            if (perk.getDependsOn().contains(this)) {
                dependents.add(perk);
            }
        }
        return dependents;
    }
    
    /**
     * Get the category of this perk (for UI grouping)
     */
    public PerkCategory getCategory() {
        String name = this.name();
        if (name.startsWith("POWER_") || name.startsWith("RESOURCE_") || 
            name.startsWith("COST_") || name.equals("BANK_EFFICIENCY")) {
            return PerkCategory.ECONOMIC;
        }
        if (name.startsWith("UPKEEP_") || name.startsWith("VETERAN_") || 
            name.startsWith("RAPID_") || name.startsWith("DAMAGE_")) {
            return PerkCategory.MILITARY;
        }
        if (name.startsWith("FORTIFIED_") || name.startsWith("SHIELD_") || 
            name.startsWith("TURRET_") || name.startsWith("POINT_") || 
            name.startsWith("BUNKER_")) {
            return PerkCategory.DEFENSIVE;
        }
        if (name.startsWith("ADVANCED_") || name.startsWith("PARALLEL_")) {
            return PerkCategory.TECH;
        }
        return PerkCategory.SPECIALIZED;
    }
    
    public enum PerkCategory {
        ECONOMIC,
        MILITARY,
        DEFENSIVE,
        TECH,
        SPECIALIZED
    }
}
