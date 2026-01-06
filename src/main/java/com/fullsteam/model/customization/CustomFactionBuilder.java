package com.fullsteam.model.customization;

import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.factions.Faction;
import com.fullsteam.model.factions.FactionDefinition;
import com.fullsteam.model.factions.FactionTechTree;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
                .faction(Faction.CUSTOM)
                .techTree(buildTechTree(config))
                .heroUnit(findHeroUnit(config))
                .monumentBuilding(findMonument(config))
                .customSelectedUnits(new HashSet<>(config.getSelectedUnits())); // Store selected units

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
     * Build a FactionTechTree from selected units and buildings
     */
    private FactionTechTree buildTechTree(CustomFactionConfig config) {
        Map<BuildingType, List<UnitType>> buildingsAndUnits = new LinkedHashMap<>();

        // Group units by their production building
        // Include ALL selected buildings, even if they don't produce units
        for (BuildingType building : config.getSelectedBuildings()) {
            List<UnitType> unitsForBuilding = config.getSelectedUnits().stream()
                    .filter(unit -> unit.getProducedBy() == building)
                    .collect(Collectors.toList());

            buildingsAndUnits.put(building, unitsForBuilding);
            log.info("Tech tree: {} -> {} units", building, unitsForBuilding.size());
        }

        log.info("Built tech tree with {} buildings", buildingsAndUnits.size());

        return FactionTechTree.builder()
                .buildingsAndUnits(buildingsAndUnits)
                .build();
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
