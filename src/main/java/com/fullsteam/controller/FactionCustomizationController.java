package com.fullsteam.controller;

import com.fullsteam.dto.BuildingTemplateDTO;
import com.fullsteam.dto.CustomFactionConfigDTO;
import com.fullsteam.dto.FactionPerkDTO;
import com.fullsteam.dto.UnitTemplateDTO;
import com.fullsteam.model.BuildingType;
import com.fullsteam.model.UnitType;
import com.fullsteam.model.customization.CustomFactionConfig;
import com.fullsteam.model.customization.FactionPerk;
import com.fullsteam.model.customization.FactionPresetRegistry;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API controller for faction customization
 */
@Controller("/api/rts/customization")
@Singleton
public class FactionCustomizationController {

    /**
     * Get all available faction presets
     */
    @Get("/presets")
    public List<CustomFactionConfigDTO> getPresets() {
        return FactionPresetRegistry.getAllPresets()
                .stream()
                .map(CustomFactionConfigDTO::fromConfig)
                .collect(Collectors.toList());
    }

    /**
     * Get preset metadata (lightweight) for dropdown menus
     */
    @Get("/presets/list")
    public List<Map<String, String>> getPresetList() {
        return FactionPresetRegistry.getAllPresets().stream()
                .map(preset -> {
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("id", preset.getFactionId());
                    metadata.put("displayName", preset.getDisplayName());
                    metadata.put("icon", preset.getIcon());
                    metadata.put("themeColor", preset.getThemeColor());
                    return metadata;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get a specific preset by ID
     */
    @Get("/presets/{presetId}")
    public HttpResponse<CustomFactionConfigDTO> getPreset(String presetId) {
        CustomFactionConfig preset = FactionPresetRegistry.getPreset(presetId);
        return preset == null
                ? HttpResponse.notFound()
                : HttpResponse.ok(CustomFactionConfigDTO.fromConfig(preset));
    }

    /**
     * Get all available unit templates (excludes zero-point required units)
     */
    @Get("/templates/units")
    public List<UnitTemplateDTO> getUnitTemplates() {
        return Arrays.stream(UnitType.values())
                .filter(template -> template.getPointCost() > 0) // Exclude zero-point units (WORKER, ANDROID)
                .map(UnitTemplateDTO::fromType)
                .collect(Collectors.toList());
    }

    /**
     * Get all available building templates (excludes zero-point required buildings)
     */
    @Get("/templates/buildings")
    public List<BuildingTemplateDTO> getBuildingTemplates() {
        return Arrays.stream(BuildingType.values())
                .filter(template -> template.getPointCost() > 0) // Exclude zero-point buildings (HEADQUARTERS, POWER_PLANT)
                .map(BuildingTemplateDTO::fromType)
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
}
