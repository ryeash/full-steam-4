package com.fullsteam.model.research.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for deserializing tech tree JSON files.
 * Matches the JSON structure in resources/tech-trees/*.json
 */
@Data
public class TechTreeConfig {
    private String faction;
    private List<String> starterUnits = new ArrayList<>();
    private List<ResearchNodeConfig> infantryNodes = new ArrayList<>();
    private List<ResearchNodeConfig> vehicleNodes = new ArrayList<>();
    private List<ResearchNodeConfig> flyerNodes = new ArrayList<>();
}
