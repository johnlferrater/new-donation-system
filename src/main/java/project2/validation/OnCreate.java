package project2.validation;

import jakarta.validation.groups.Default;

/**
 * Validation group applied when creating a new record. Extends {@link Default}
 * so that ungrouped constraints are validated too, plus create-only constraints
 * such as "password is required".
 */
public interface OnCreate extends Default {
}
