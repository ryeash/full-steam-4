package com.fullsteam.model.customization;

import com.fullsteam.model.UnitType;
import com.fullsteam.model.UnitCategory;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Template for a unit that can be selected during faction customization.
 * Contains metadata and stats for display in the UI.
 */
@Getter
@Builder
public class UnitTemplate implements CustomizableEntity {
    private final UnitType unitType;
    private final String displayName;
    private final String description;
    private final int pointCost;
    private final EntityCategory category;
    private final UnitCategory unitCategory; // INFANTRY, VEHICLE, FLYER, WORKER
    @Builder.Default
    private final List<String> tags = new ArrayList<>();
    private final boolean isHeroUnit;
    private final String iconPath;
    
    // Stats for display
    private final int maxHealth;
    private final int damage;
    private final double speed;
    private final int range;
    private final int baseCost;
    private final int upkeep;
    
    @Override
    public String getId() {
        return unitType.name();
    }
    
    /**
     * Create a UnitTemplate from a UnitType
     */
    public static UnitTemplate fromUnitType(UnitType unitType) {
        return UnitTemplate.builder()
            .unitType(unitType)
            .displayName(unitType.getDisplayName())
            .description(generateDescription(unitType))
            .pointCost(calculatePointCost(unitType))
            .category(mapToEntityCategory(unitType.getCategory()))
            .unitCategory(unitType.getCategory())
            .tags(generateTags(unitType))
            .isHeroUnit(isHero(unitType))
            .iconPath("/icons/units/" + unitType.name().toLowerCase() + ".png")
            .maxHealth((int) unitType.getMaxHealth())
            .damage((int) unitType.getDamage())
            .speed(unitType.getMovementSpeed())
            .range((int) unitType.getAttackRange())
            .baseCost(unitType.getResourceCost())
            .upkeep(unitType.getUpkeepCost())
            .build();
    }
    
    /**
     * Calculate point cost based on unit stats and power level.
     * Rebalanced for 100-point budget to allow ~10-15 units + buildings + perks.
     * 
     * Target distribution:
     * - Basic units (Infantry, Jeep, Scout): 1-2 points
     * - Advanced units (Tank, Helicopter): 2-4 points
     * - Elite units (Bomber, Cloak Tank): 4-6 points
     * - Hero units: 8-10 points
     */
    private static int calculatePointCost(UnitType unitType) {
        // WORKER is required and free
        if (unitType == UnitType.WORKER) {
            return 0;
        }
        
        // ANDROID is bundled with ANDROID_FACTORY (free, auto-included)
        if (unitType == UnitType.ANDROID) {
            return 0;
        }
        
        // Hero units get fixed high cost (8-10 points)
        if (isHero(unitType)) {
            // Differentiate heroes by power level
            return switch (unitType) {
                case GIGANTONAUT, COLOSSUS, CRAWLER -> 10; // Strongest heroes
                case PHOTON_TITAN, GUNSHIP -> 9;           // Strong heroes
                case RAIDER -> 8;                          // Fast hero
                default -> 10;
            };
        }
        
        // Calculate base power from stats (much lower multipliers)
        double power = 0;
        power += unitType.getMaxHealth() * 0.002;   // HP contribution
        power += unitType.getDamage() * 0.03;       // Damage contribution
        power += unitType.getMovementSpeed() * 0.003; // Speed contribution
        power += unitType.getAttackRange() * 0.002; // Range contribution
        
        // Category multipliers (adjusted for balance)
        switch (unitType.getCategory()) {
            case INFANTRY -> power *= 0.9;  // Infantry are slightly cheaper
            case VEHICLE -> power *= 1.1;   // Vehicles are slightly more expensive
            case FLYER -> power *= 1.2;     // Flyers are more expensive
            case WORKER -> power *= 0.0;    // Workers are free
        }
        
        // Support units are cheaper
        if (unitType.isSupport()) {
            power *= 0.6;
        }
        
        // Minimum cost of 1 point for basic units, round up
        return Math.max(1, (int) Math.ceil(power));
    }
    
    /**
     * Check if this is a hero unit
     */
    private static boolean isHero(UnitType unitType) {
        return List.of(
            UnitType.CRAWLER,
            UnitType.RAIDER,
            UnitType.COLOSSUS,
            UnitType.PHOTON_TITAN,
            UnitType.GUNSHIP,
            UnitType.GIGANTONAUT
        ).contains(unitType);
    }
    
    /**
     * Map UnitCategory to EntityCategory
     */
    private static EntityCategory mapToEntityCategory(UnitCategory unitCategory) {
        return switch (unitCategory) {
            case INFANTRY -> EntityCategory.INFANTRY;
            case VEHICLE -> EntityCategory.VEHICLE;
            case FLYER -> EntityCategory.FLYER;
            case WORKER -> EntityCategory.SUPPORT;
        };
    }
    
    /**
     * Generate descriptive tags for filtering
     */
    private static List<String> generateTags(UnitType unitType) {
        List<String> tags = new ArrayList<>();
        
        // Category tag
        tags.add(unitType.getCategory().name());
        
        // Combat tags
        if (unitType.canAttack()) {
            tags.add("COMBAT");
            if (unitType.getDamage() > 30) {
                tags.add("HEAVY_DAMAGE");
            }
        }
        
        // Support tags
        if (unitType.isSupport()) {
            tags.add("SUPPORT");
        }
        if (unitType.canHarvest()) {
            tags.add("HARVESTER");
        }
        if (unitType.canBuild()) {
            tags.add("BUILDER");
        }
        
        // Range tags
        if (unitType.getAttackRange() > 150) {
            tags.add("LONG_RANGE");
        }
        
        // Hero tag
        if (isHero(unitType)) {
            tags.add("HERO");
        }
        
        // Elevation tag
        tags.add(unitType.getElevation().name());
        
        return tags;
    }
    
    /**
     * Generate a description for the unit
     */
    private static String generateDescription(UnitType unitType) {
        if (unitType.canHarvest()) {
            return "Harvests resources and constructs buildings";
        }
        if (unitType.isSupport()) {
            return "Support unit that heals nearby allies";
        }
        if (isHero(unitType)) {
            return "Powerful hero unit with unique abilities";
        }
        if (unitType.canAttack()) {
            String damageType = unitType.getDamage() > 30 ? "Heavy" : "Standard";
            String rangeType = unitType.getAttackRange() > 150 ? "long-range" : "close-range";
            return String.format("%s %s %s combat unit", 
                damageType, rangeType, unitType.getCategory().name().toLowerCase());
        }
        return unitType.getDisplayName();
    }
}
