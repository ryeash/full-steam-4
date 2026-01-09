package com.fullsteam.dto;

import com.fullsteam.model.customization.BuildingTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for building template information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingTemplateDTO {
    private String id;
    private String buildingType;
    private String displayName;
    private String description;
    private int pointCost;
    private String category;
    private boolean isMonument;
    private List<String> producesUnitCategories;
    private List<String> tags;
    private String iconPath;
    private List<String> techRequirements; // NEW: Required buildings to unlock this
    
    // Stats
    private int maxHealth;
    private int baseCost;
    private int powerValue;
    
    public static BuildingTemplateDTO fromTemplate(BuildingTemplate template) {
        // Get tech requirements from BuildingType
        List<String> techReqs = template.getBuildingType().getTechRequirements().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        
        return BuildingTemplateDTO.builder()
            .id(template.getId())
            .buildingType(template.getBuildingType().name())
            .displayName(template.getDisplayName())
            .description(template.getDescription())
            .pointCost(template.getPointCost())
            .category(template.getCategory().name())
            .isMonument(template.isMonument())
            .producesUnitCategories(template.getProducesUnitCategories().stream()
                .map(Enum::name)
                .collect(Collectors.toList()))
            .tags(template.getTags())
            .iconPath(template.getIconPath())
            .techRequirements(techReqs)
            .maxHealth(template.getMaxHealth())
            .baseCost(template.getBaseCost())
            .powerValue(template.getPowerValue())
            .build();
    }
    
    public static List<BuildingTemplateDTO> fromTemplates(List<BuildingTemplate> templates) {
        return templates.stream()
            .map(BuildingTemplateDTO::fromTemplate)
            .collect(Collectors.toList());
    }
}
