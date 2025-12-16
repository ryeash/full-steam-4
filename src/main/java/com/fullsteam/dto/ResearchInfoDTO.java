package com.fullsteam.dto;

import com.fullsteam.model.research.ResearchCategory;
import com.fullsteam.model.research.ResearchType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Data Transfer Object for research information.
 * Sent to client with faction info so UI can display research tree.
 */
@Data
@Builder
public class ResearchInfoDTO {

    private String researchId;           // e.g., "PROJECTILE_DAMAGE_1"
    private String displayName;          // e.g., "Improved Ballistics I"
    private String description;          // e.g., "Increases projectile weapon damage by 15%"
    private String category;             // e.g., "COMBAT"
    private int creditCost;              // e.g., 500
    private int researchTimeSeconds;     // e.g., 60
    private String requiredBuilding;     // e.g., "RESEARCH_LAB"
    private List<String> prerequisites;  // e.g., ["PROJECTILE_DAMAGE_1"]

    // Modifier information (for display)
    private String effectSummary;        // e.g., "+15% Projectile Damage"
    private String icon;                 // e.g., "🗡️"

    /**
     * Build DTO from ResearchType enum
     */
    public static ResearchInfoDTO fromResearchType(ResearchType researchType) {
        return ResearchInfoDTO.builder()
                .researchId(researchType.name())
                .displayName(researchType.getDisplayName())
                .description(researchType.getDescription())
                .category(researchType.getCategory().name())
                .creditCost(researchType.getCreditCost())
                .researchTimeSeconds(researchType.getResearchTimeSeconds())
                .requiredBuilding(researchType.getRequiredBuilding().name())
                .prerequisites(researchType.getPrerequisites().stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .effectSummary(buildEffectSummary(researchType))
                .icon(getCategoryIcon(researchType.getCategory()))
                .build();
    }

    /**
     * Build a human-readable effect summary
     */
    private static String buildEffectSummary(ResearchType researchType) {
        // Special cases for non-modifier research
        if (researchType == ResearchType.PARALLEL_RESEARCH_1) {
            return "+1 Simultaneous Research";
        }
        if (researchType == ResearchType.PARALLEL_RESEARCH_2) {
            return "+1 Simultaneous Research";
        }

        var modifier = researchType.getModifier();

        // Check each modifier and build summary
        if (modifier.getProjectileDamageMultiplier() != 1.0) {
            double percent = (modifier.getProjectileDamageMultiplier() - 1.0) * 100;
            return String.format("+%.0f%% Projectile Damage", percent);
        }
        if (modifier.getBeamDamageMultiplier() != 1.0) {
            double percent = (modifier.getBeamDamageMultiplier() - 1.0) * 100;
            return String.format("+%.0f%% Beam Damage", percent);
        }
        if (modifier.getAttackRangeMultiplier() != 1.0) {
            double percent = (modifier.getAttackRangeMultiplier() - 1.0) * 100;
            return String.format("+%.0f%% Attack Range", percent);
        }
        if (modifier.getAttackRateMultiplier() != 1.0) {
            double percent = (modifier.getAttackRateMultiplier() - 1.0) * 100;
            return String.format("+%.0f%% Attack Rate", percent);
        }
        if (modifier.getUnitHealthMultiplier() != 1.0) {
            double percent = (modifier.getUnitHealthMultiplier() - 1.0) * 100;
            return String.format("+%.0f%% Unit Health", percent);
        }
        if (modifier.getBuildingHealthMultiplier() != 1.0) {
            double percent = (modifier.getBuildingHealthMultiplier() - 1.0) * 100;
            return String.format("+%.0f%% Building Health", percent);
        }
        if (modifier.getInfantrySpeedMultiplier() != 1.0) {
            double percent = (modifier.getInfantrySpeedMultiplier() - 1.0) * 100;
            return String.format("+%.0f%% Infantry Speed", percent);
        }
        if (modifier.getVehicleSpeedMultiplier() != 1.0) {
            double percent = (modifier.getVehicleSpeedMultiplier() - 1.0) * 100;
            return String.format("+%.0f%% Vehicle Speed", percent);
        }
        if (modifier.getWorkerCapacityBonus() != 0) {
            return String.format("+%.0f Worker Capacity", modifier.getWorkerCapacityBonus());
        }
        if (modifier.getHarvestingSpeedMultiplier() != 1.0) {
            double percent = (modifier.getHarvestingSpeedMultiplier() - 1.0) * 100;
            return String.format("+%.0f%% Harvesting Speed", percent);
        }
        if (modifier.getProductionSpeedMultiplier() != 1.0) {
            double percent = (1.0 - modifier.getProductionSpeedMultiplier()) * 100;
            return String.format("-%.0f%% Production Time", percent);
        }
        if (modifier.getConstructionSpeedMultiplier() != 1.0) {
            double percent = (1.0 - modifier.getConstructionSpeedMultiplier()) * 100;
            return String.format("-%.0f%% Construction Time", percent);
        }
        if (modifier.getHealingPowerMultiplier() != 1.0) {
            double percent = (modifier.getHealingPowerMultiplier() - 1.0) * 100;
            return String.format("+%.0f%% Healing Power", percent);
        }
        if (modifier.getRepairPowerMultiplier() != 1.0) {
            double percent = (modifier.getRepairPowerMultiplier() - 1.0) * 100;
            return String.format("+%.0f%% Repair Power", percent);
        }
        if (modifier.getVisionRangeMultiplier() != 1.0) {
            double percent = (modifier.getVisionRangeMultiplier() - 1.0) * 100;
            return String.format("+%.0f%% Vision Range", percent);
        }

        return "Unknown Effect";
    }

    /**
     * Get icon for research category
     */
    private static String getCategoryIcon(ResearchCategory category) {
        return switch (category) {
            case COMBAT -> "🗡️";
            case DEFENSE -> "🛡️";
            case MOBILITY -> "⚡";
            case ECONOMY -> "💰";
            case SPECIAL -> "✨";
        };
    }
}

