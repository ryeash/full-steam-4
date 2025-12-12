package com.fullsteam.model;

/**
 * Bullet effects for projectiles (minimal enum for RTS compatibility).
 */
public enum BulletEffect {
    EXPLOSIVE,
    PIERCING,
    INCENDIARY,
    BOUNCING,
    SEEKING,     // Homing/heat-seeking projectiles (e.g., interceptor missiles)
    TOXIC,
    FREEZING,
    ELECTRIC,
    FLAK         // Anti-aircraft flak explosion (targets air units only)
}



