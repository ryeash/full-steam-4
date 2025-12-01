package com.fullsteam.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Wrapper for shield sensor body user data.
 * This allows us to distinguish shield sensors from regular building bodies
 * in collision detection.
 */
@Getter
@RequiredArgsConstructor
public class ShieldSensor {
    private final Building building;
}

