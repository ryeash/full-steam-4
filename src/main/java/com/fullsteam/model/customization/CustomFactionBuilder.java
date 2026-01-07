package com.fullsteam.model.customization;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.FactionDefinition;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds a runtime FactionDefinition from a CustomFactionConfig.
 * Applies all perk effects and creates the tech tree.
 */
@Singleton
@Slf4j
public class CustomFactionBuilder {

    /**
     * Build a FactionDefinition from a custom configuration
     */
    public FactionDefinition buildFromConfig(CustomFactionConfig config) {
        log.info("Building custom faction: {}", config.getDisplayName());

        // Start with default values
        FactionDefinition.FactionDefinitionBuilder builder = FactionDefinition.builder()
                .unitTypes(new HashSet<>(config.getSelectedUnits()))
                .buildingTypes(new HashSet<>(config.getSelectedBuildings()))
                .activePerks(config.getEffectivePerks()); // Store selected units

        // Get effective perks (highest tier only in each chain)
        Set<FactionPerk> effectivePerks = config.getEffectivePerks();

        log.info("Applying {} effective perks (filtered from {} selected)",
                effectivePerks.size(), config.getSelectedPerks().size());

        // Apply each perk's effects directly (FactionPerk implements PerkEffect)
        for (FactionPerk perk : effectivePerks) {
            perk.applyToDefinition(builder, config);
            log.debug("Applied perk: {}", perk.getDisplayName());
        }

        FactionDefinition definition = builder.build();

        log.info("Custom faction built: {} with {} units, {} buildings, {} perks ({}effective)",
                config.getDisplayName(),
                config.getSelectedUnits().size(),
                config.getSelectedBuildings().size(),
                config.getSelectedPerks().size(),
                effectivePerks.size());

        return definition;
    }

    /**
     * Find the hero unit (if any) in the selected units
     */
    private UnitType findHeroUnit(CustomFactionConfig config) {
        List<UnitType> heroes = Arrays.asList(
                UnitType.CRAWLER,
                UnitType.RAIDER,
                UnitType.COLOSSUS,
                UnitType.PHOTON_TITAN,
                UnitType.GUNSHIP,
                UnitType.GIGANTONAUT
        );

        return config.getSelectedUnits().stream()
                .filter(heroes::contains)
                .findFirst()
                .orElse(null);
    }

    /**
     * Find the monument building (if any) in the selected buildings
     */
    private BuildingType findMonument(CustomFactionConfig config) {
        List<BuildingType> monuments = Arrays.asList(
                BuildingType.SANDSTORM_GENERATOR,
                BuildingType.ANDROID_FACTORY,
                BuildingType.PHOTON_SPIRE,
                BuildingType.COMMAND_CITADEL,
                BuildingType.TEMPEST_SPIRE
        );

        return config.getSelectedBuildings().stream()
                .filter(monuments::contains)
                .findFirst()
                .orElse(null);
    }

}
