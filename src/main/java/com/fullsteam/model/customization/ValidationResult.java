package com.fullsteam.model.customization;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Result of validating a faction configuration or perk selection
 */
@Getter
@AllArgsConstructor
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;

    /**
     * Create a successful validation result
     */
    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }

    /**
     * Create a failed validation result
     */
    public static ValidationResult failure(String... errors) {
        return new ValidationResult(false, List.of(errors));
    }

    /**
     * Create a failed validation result
     */
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    /**
     * Get the first error message (if any)
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }

    /**
     * Get all errors as a single string
     */
    public String getAllErrors() {
        return String.join("; ", errors);
    }
}
