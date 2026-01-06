package com.fullsteam.dto;

import com.fullsteam.model.customization.CustomFactionConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for custom faction configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFactionConfigDTO {
    private String factionId;
    private String displayName;
    private String themeColor;
    private String icon;
    private List<String> selectedUnits;
    private List<String> selectedBuildings;
    private List<String> selectedPerks;
    private int totalPointsSpent;
    private int maxPoints;
    private String basedOnPreset;
    private int remainingPoints;
    private boolean isValid;
    private List<String> validationErrors;
    
    public static CustomFactionConfigDTO fromConfig(CustomFactionConfig config) {
        var validation = config.validate();
        
        return CustomFactionConfigDTO.builder()
            .factionId(config.getFactionId())
            .displayName(config.getDisplayName())
            .themeColor(config.getThemeColor())
            .icon(config.getIcon())
            .selectedUnits(config.getSelectedUnits().stream()
                .map(Enum::name)
                .collect(Collectors.toList()))
            .selectedBuildings(config.getSelectedBuildings().stream()
                .map(Enum::name)
                .collect(Collectors.toList()))
            .selectedPerks(config.getSelectedPerks().stream()
                .map(Enum::name)
                .collect(Collectors.toList()))
            .totalPointsSpent(config.getTotalPointsSpent())
            .maxPoints(config.getMaxPoints())
            .basedOnPreset(config.getBasedOnPreset())
            .remainingPoints(config.getRemainingPoints())
            .isValid(validation.isValid())
            .validationErrors(validation.getErrors())
            .build();
    }
    
    public static List<CustomFactionConfigDTO> fromConfigs(List<CustomFactionConfig> configs) {
        return configs.stream()
            .map(CustomFactionConfigDTO::fromConfig)
            .collect(Collectors.toList());
    }
}
