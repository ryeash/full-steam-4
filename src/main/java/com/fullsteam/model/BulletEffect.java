package com.fullsteam.model;

/**
 * Bullet effects for projectiles (minimal enum for RTS compatibility).
 */
public enum BulletEffect {
    EXPLOSIVE,
    PIERCING,
    SEEKING,     // Homing/heat-seeking projectiles (e.g., interceptor missiles)
    ELECTRIC,
    FLAK,        // Anti-aircraft flak explosion (targets air units only)
    TRACKER_BUG     // Spy tracker device - attaches TrackerBug to target (0 damage)
}



