package com.eb.script.interpreter.screen.data;

import com.eb.script.interpreter.screen.DisplayItem;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Handles data-level validation for screen variables.
 * Validates values against constraints like min/max, pattern, mandatory, and maxLength.
 * 
 * This class is part of the DATA layer and has no JavaFX dependencies.
 * It validates data values independently of UI controls.
 * 
 * @author Earl Bosch
 */
public class DataValidator {

    /**
     * Result of a validation check containing validity status and error message.
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;
        
        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }

    /**
     * Validates a value against all constraints in the DisplayItem metadata.
     * 
     * @param value The value to validate
     * @param metadata The DisplayItem containing validation constraints
     * @return ValidationResult indicating if validation passed
     */
    public static ValidationResult validate(Object value, DisplayItem metadata) {
        if (metadata == null) {
            return ValidationResult.valid();
        }
        
        // Check mandatory constraint
        ValidationResult mandatoryResult = validateMandatory(value, metadata.mandatory);
        if (!mandatoryResult.isValid) {
            return mandatoryResult;
        }
        
        // If value is null/empty and not mandatory, skip other validations
        if (value == null || (value instanceof String && ((String) value).isEmpty())) {
            return ValidationResult.valid();
        }
        
        // Check min constraint
        if (metadata.min != null) {
            ValidationResult minResult = validateMin(value, metadata.min);
            if (!minResult.isValid) {
                return minResult;
            }
        }
        
        // Check max constraint
        if (metadata.max != null) {
            ValidationResult maxResult = validateMax(value, metadata.max);
            if (!maxResult.isValid) {
                return maxResult;
            }
        }
        
        // Check maxLength constraint
        if (metadata.maxLength != null) {
            ValidationResult lengthResult = validateMaxLength(value, metadata.maxLength);
            if (!lengthResult.isValid) {
                return lengthResult;
            }
        }
        
        // Check pattern constraint
        if (metadata.pattern != null && !metadata.pattern.isEmpty()) {
            ValidationResult patternResult = validatePattern(value, metadata.pattern);
            if (!patternResult.isValid) {
                return patternResult;
            }
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates the mandatory constraint.
     * 
     * @param value The value to check
     * @param mandatory Whether the field is mandatory
     * @return ValidationResult indicating if validation passed
     */
    public static ValidationResult validateMandatory(Object value, boolean mandatory) {
        if (!mandatory) {
            return ValidationResult.valid();
        }
        
        if (value == null) {
            return ValidationResult.invalid("This field is required");
        }
        
        if (value instanceof String && ((String) value).trim().isEmpty()) {
            return ValidationResult.invalid("This field is required");
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates the minimum value constraint.
     * Supports numeric values and string length.
     * 
     * @param value The value to check
     * @param min The minimum allowed value
     * @return ValidationResult indicating if validation passed
     */
    public static ValidationResult validateMin(Object value, Object min) {
        if (min == null) {
            return ValidationResult.valid();
        }
        
        try {
            double minValue = toDouble(min);
            double actualValue;
            
            if (value instanceof Number) {
                actualValue = ((Number) value).doubleValue();
            } else if (value instanceof String) {
                // For strings, try to parse as number first
                try {
                    actualValue = Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    // If not a number, use string length
                    actualValue = ((String) value).length();
                }
            } else {
                return ValidationResult.valid(); // Can't validate non-numeric types
            }
            
            if (actualValue < minValue) {
                return ValidationResult.invalid("Value must be at least " + formatNumber(minValue));
            }
        } catch (NumberFormatException e) {
            // If min is not a number, skip validation
            return ValidationResult.valid();
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates the maximum value constraint.
     * Supports numeric values and string length.
     * 
     * @param value The value to check
     * @param max The maximum allowed value
     * @return ValidationResult indicating if validation passed
     */
    public static ValidationResult validateMax(Object value, Object max) {
        if (max == null) {
            return ValidationResult.valid();
        }
        
        try {
            double maxValue = toDouble(max);
            double actualValue;
            
            if (value instanceof Number) {
                actualValue = ((Number) value).doubleValue();
            } else if (value instanceof String) {
                // For strings, try to parse as number first
                try {
                    actualValue = Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    // If not a number, use string length
                    actualValue = ((String) value).length();
                }
            } else {
                return ValidationResult.valid(); // Can't validate non-numeric types
            }
            
            if (actualValue > maxValue) {
                return ValidationResult.invalid("Value must be at most " + formatNumber(maxValue));
            }
        } catch (NumberFormatException e) {
            // If max is not a number, skip validation
            return ValidationResult.valid();
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates the maximum length constraint.
     * 
     * @param value The value to check
     * @param maxLength The maximum allowed length
     * @return ValidationResult indicating if validation passed
     */
    public static ValidationResult validateMaxLength(Object value, Integer maxLength) {
        if (maxLength == null || maxLength <= 0) {
            return ValidationResult.valid();
        }
        
        String stringValue = value != null ? String.valueOf(value) : "";
        
        if (stringValue.length() > maxLength) {
            return ValidationResult.invalid("Maximum length is " + maxLength + " characters");
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates the pattern constraint using regex.
     * 
     * @param value The value to check
     * @param pattern The regex pattern to match
     * @return ValidationResult indicating if validation passed
     */
    public static ValidationResult validatePattern(Object value, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return ValidationResult.valid();
        }
        
        String stringValue = value != null ? String.valueOf(value) : "";
        
        try {
            Pattern compiledPattern = Pattern.compile(pattern);
            if (!compiledPattern.matcher(stringValue).matches()) {
                return ValidationResult.invalid("Value does not match the required pattern");
            }
        } catch (PatternSyntaxException e) {
            System.err.println("Warning: Invalid validation pattern: " + pattern);
            return ValidationResult.valid(); // Don't fail on invalid pattern
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates that a value is within a range.
     * 
     * @param value The value to check
     * @param min The minimum allowed value (inclusive)
     * @param max The maximum allowed value (inclusive)
     * @return ValidationResult indicating if validation passed
     */
    public static ValidationResult validateRange(Object value, Object min, Object max) {
        if (min != null) {
            ValidationResult minResult = validateMin(value, min);
            if (!minResult.isValid) {
                return minResult;
            }
        }
        
        if (max != null) {
            ValidationResult maxResult = validateMax(value, max);
            if (!maxResult.isValid) {
                return maxResult;
            }
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Converts an Object to double for comparison.
     */
    private static double toDouble(Object value) throws NumberFormatException {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        throw new NumberFormatException("Cannot convert to double: " + value);
    }
    
    /**
     * Formats a number for display in error messages.
     * Shows integers without decimals.
     */
    private static String formatNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
