package com.fullsteam.model;

import lombok.Getter;

/**
 * AI stance for units - controls automatic behavior
 */
@Getter
public enum AIStance {
    AGGRESSIVE(
            "Aggressive",
            "Automatically attacks any enemy in vision range and pursues them",
            true,
            true
    ),
    
    DEFENSIVE(
            "Defensive",
            "Automatically attacks enemies in vision range but stays near original position",
            true,
            false
    ),
    
    PASSIVE(
            "Passive",
            "Does not automatically attack, only responds to direct commands",
            false,
            false
    );
    
    private final String displayName;
    private final String description;
    private final boolean autoAttack;
    private final boolean pursuEnemies;
    
    AIStance(String displayName, String description, boolean autoAttack, boolean pursuEnemies) {
        this.displayName = displayName;
        this.description = description;
        this.autoAttack = autoAttack;
        this.pursuEnemies = pursuEnemies;
    }
}

