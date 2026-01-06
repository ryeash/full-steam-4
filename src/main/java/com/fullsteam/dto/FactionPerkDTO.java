package com.fullsteam.dto;

import com.fullsteam.model.customization.FactionPerk;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO for faction perk information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactionPerkDTO {
    private String id;
    private String displayName;
    private String description;
    private int pointCost;
    private String category;
    private List<String> dependsOn;
    
    public static FactionPerkDTO fromPerk(FactionPerk perk) {
        return FactionPerkDTO.builder()
            .id(perk.name())
            .displayName(perk.getDisplayName())
            .description(perk.getDescription())
            .pointCost(perk.getPointCost())
            .category(perk.getCategory().name())
            .dependsOn(perk.getDependsOn().stream()
                .map(Enum::name)
                .collect(Collectors.toList()))
            .build();
    }
    
    public static List<FactionPerkDTO> fromPerks(Set<FactionPerk> perks) {
        return perks.stream()
            .map(FactionPerkDTO::fromPerk)
            .collect(Collectors.toList());
    }
}
