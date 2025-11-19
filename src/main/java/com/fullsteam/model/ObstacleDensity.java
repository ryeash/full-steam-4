package com.fullsteam.model;

import lombok.Getter;

@Getter
public enum ObstacleDensity {
    SPARSE(0.5),
    MEDIUM(1.0),
    DENSE(1.5),
    VERY_DENSE(2.0);

    private final double multiplier;

    ObstacleDensity(double multiplier) {
        this.multiplier = multiplier;
    }
}
