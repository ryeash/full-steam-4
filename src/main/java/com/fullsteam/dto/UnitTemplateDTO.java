package com.fullsteam.dto;

import com.fullsteam.model.customization.UnitTemplate;
import com.fullsteam.model.customization.EntityCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for unit template information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitTemplateDTO {
    private String id;
    private String unitType;
    private String displayName;
    private String description;
    private int pointCost;
    private String category;
    private String unitCategory;
    private List<String> tags;
    private boolean isHeroUnit;
    private String iconPath;
    
    // Stats
    private int maxHealth;
    private int damage;
    private double speed;
    private int range;
    private int baseCost;
    private int upkeep;
    
    public static UnitTemplateDTO fromTemplate(UnitTemplate template) {
        return UnitTemplateDTO.builder()
            .id(template.getId())
            .unitType(template.getUnitType().name())
            .displayName(template.getDisplayName())
            .description(template.getDescription())
            .pointCost(template.getPointCost())
            .category(template.getCategory().name())
            .unitCategory(template.getUnitCategory().name())
            .tags(template.getTags())
            .isHeroUnit(template.isHeroUnit())
            .iconPath(template.getIconPath())
            .maxHealth(template.getMaxHealth())
            .damage(template.getDamage())
            .speed(template.getSpeed())
            .range(template.getRange())
            .baseCost(template.getBaseCost())
            .upkeep(template.getUpkeep())
            .build();
    }
    
    public static List<UnitTemplateDTO> fromTemplates(List<UnitTemplate> templates) {
        return templates.stream()
            .map(UnitTemplateDTO::fromTemplate)
            .collect(Collectors.toList());
    }
}
