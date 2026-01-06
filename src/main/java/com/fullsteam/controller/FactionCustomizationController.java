package com.fullsteam.controller;

import com.fullsteam.dto.*;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.customization.*;
import com.fullsteam.model.factions.FactionDefinition;
import com.fullsteam.model.RTSGameManager;
import com.fullsteam.RTSLobby;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API controller for faction customization
 */
@Controller("/api/rts/customization")
@Singleton
public class FactionCustomizationController {
    
    private static final Logger log = LoggerFactory.getLogger(FactionCustomizationController.class);
    
    private final CustomFactionBuilder factionBuilder;
    private final RTSLobby rtsLobby;
    
    public FactionCustomizationController(
            CustomFactionBuilder factionBuilder,
            RTSLobby rtsLobby) {
        this.factionBuilder = factionBuilder;
        this.rtsLobby = rtsLobby;
    }
    
    /**
     * Get all available faction presets
     */
    @Get("/presets")
    public List<CustomFactionConfigDTO> getPresets() {
        return FactionPresetRegistry.getAllPresets().stream()
            .map(CustomFactionConfigDTO::fromConfig)
            .collect(Collectors.toList());
    }
    
    /**
     * Get a specific preset by ID
     */
    @Get("/presets/{presetId}")
    public HttpResponse<CustomFactionConfigDTO> getPreset(String presetId) {
        CustomFactionConfig preset = FactionPresetRegistry.getPreset(presetId);
        
        if (preset == null) {
            return HttpResponse.notFound();
        }
        
        return HttpResponse.ok(CustomFactionConfigDTO.fromConfig(preset));
    }
    
    /**
     * Get all available unit templates
     */
    @Get("/templates/units")
    public List<UnitTemplateDTO> getUnitTemplates() {
        return Arrays.stream(UnitType.values())
            .map(UnitTemplate::fromUnitType)
            .map(UnitTemplateDTO::fromTemplate)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all available building templates
     */
    @Get("/templates/buildings")
    public List<BuildingTemplateDTO> getBuildingTemplates() {
        return Arrays.stream(BuildingType.values())
            .map(BuildingTemplate::fromBuildingType)
            .map(BuildingTemplateDTO::fromTemplate)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all available faction perks
     */
    @Get("/perks")
    public List<FactionPerkDTO> getPerks() {
        return Arrays.stream(FactionPerk.values())
            .map(FactionPerkDTO::fromPerk)
            .collect(Collectors.toList());
    }
    
    /**
     * Validate a faction configuration
     */
    @Post("/validate")
    public HttpResponse<Map<String, Object>> validateConfiguration(
            @Body CustomFactionConfigDTO configDTO) {
        
        try {
            // Convert DTO to config
            CustomFactionConfig config = dtoToConfig(configDTO);
            
            // Validate
            ValidationResult validation = config.validate();
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", validation.isValid());
            response.put("errors", validation.getErrors());
            response.put("totalPoints", config.calculateTotalPoints());
            response.put("remainingPoints", config.getRemainingPoints());
            
            return HttpResponse.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("valid", false);
            error.put("errors", List.of("Invalid configuration: " + e.getMessage()));
            return HttpResponse.badRequest(error);
        }
    }
    
    /**
     * Convert DTO to CustomFactionConfig
     */
    private CustomFactionConfig dtoToConfig(CustomFactionConfigDTO dto) {
        Set<UnitType> units = dto.getSelectedUnits().stream()
            .map(UnitType::valueOf)
            .collect(Collectors.toSet());
        
        Set<BuildingType> buildings = dto.getSelectedBuildings().stream()
            .map(BuildingType::valueOf)
            .collect(Collectors.toSet());
        
        Set<FactionPerk> perks = dto.getSelectedPerks().stream()
            .map(FactionPerk::valueOf)
            .collect(Collectors.toSet());
        
        CustomFactionConfig config = CustomFactionConfig.builder()
            .factionId(dto.getFactionId())
            .displayName(dto.getDisplayName())
            .themeColor(dto.getThemeColor())
            .icon(dto.getIcon())
            .selectedUnits(units)
            .selectedBuildings(buildings)
            .selectedPerks(perks)
            .basedOnPreset(dto.getBasedOnPreset())
            .build();
        
        config.setTotalPointsSpent(config.calculateTotalPoints());
        
        return config;
    }
    
    /**
     * Get perk dependency tree for UI
     */
    @Get("/perks/dependencies")
    public Map<String, List<String>> getPerkDependencies() {
        Map<String, List<String>> dependencies = new HashMap<>();
        
        for (FactionPerk perk : FactionPerk.values()) {
            dependencies.put(
                perk.name(),
                perk.getDependsOn().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList())
            );
        }
        
        return dependencies;
    }
    
    /**
     * Get perks grouped by category
     */
    @Get("/perks/categories")
    public Map<String, List<FactionPerkDTO>> getPerksByCategory() {
        Map<String, List<FactionPerkDTO>> grouped = new HashMap<>();
        
        for (FactionPerk perk : FactionPerk.values()) {
            String category = perk.getCategory().name();
            grouped.computeIfAbsent(category, k -> new ArrayList<>())
                .add(FactionPerkDTO.fromPerk(perk));
        }
        
        return grouped;
    }
    
    /**
     * Apply a custom faction configuration to a player in a game
     */
    @Post("/games/{gameId}/players/{playerId}/apply")
    public HttpResponse<Map<String, Object>> applyFactionToPlayer(
            String gameId,
            int playerId,
            @Body CustomFactionConfigDTO configDTO) {
        
        log.info("Received request to apply custom faction for game {} player {}", gameId, playerId);
        log.info("Config: {} units, {} buildings, {} perks", 
                configDTO.getSelectedUnits().size(),
                configDTO.getSelectedBuildings().size(), 
                configDTO.getSelectedPerks().size());
        
        try {
            // Get the game
            RTSGameManager game = rtsLobby.getGame(gameId);
            if (game == null) {
                log.warn("Game not found: {}", gameId);
                return HttpResponse.notFound();
            }
            
            // Convert DTO to config
            CustomFactionConfig config = dtoToConfig(configDTO);
            
            // Validate
            ValidationResult validation = config.validate();
            if (!validation.isValid()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("errors", validation.getErrors());
                return HttpResponse.badRequest(error);
            }
            
            // Build FactionDefinition from config
            FactionDefinition customDefinition = factionBuilder.buildFromConfig(config);
            
            // Apply to player
            com.fullsteam.model.PlayerFaction playerFaction = game.getPlayerFactions().get(playerId);
            if (playerFaction == null) {
                log.warn("Player {} not found in game {}. Available players: {}", 
                        playerId, gameId, game.getPlayerFactions().keySet());
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("errors", List.of("Player not found in game"));
                return HttpResponse.badRequest(error);
            }
            
            log.info("Applying custom faction '{}' to player {} in game {}", 
                    config.getDisplayName(), playerId, gameId);
            playerFaction.applyCustomFaction(customDefinition);
            log.info("Custom faction applied successfully. Player now has {} units available", 
                    playerFaction.getResearchManager().getAllAvailableUnits().values().stream()
                            .mapToInt(Set::size).sum());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Custom faction applied successfully");
            response.put("factionName", config.getDisplayName());
            
            return HttpResponse.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("errors", List.of("Failed to apply faction: " + e.getMessage()));
            return HttpResponse.serverError(error);
        }
    }
}
