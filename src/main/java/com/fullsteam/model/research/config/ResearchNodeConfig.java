package com.fullsteam.model.research.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for deserializing research node JSON.
 * Matches the research node structure in tech tree JSON files.
 */
@Data
public class ResearchNodeConfig {
    private String id;
    private String displayName;
    private String description;
    private String unitToUnlock;
    private String unitToReplace;
    private int creditCost;
    private int researchTimeSeconds;
    private String tier;
    private String category;
    private List<String> prerequisiteIds = new ArrayList<>();
    private List<String> mutuallyExclusiveIds = new ArrayList<>();
}
