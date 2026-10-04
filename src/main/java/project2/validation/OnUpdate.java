package project2.validation;

import jakarta.validation.groups.Default;

/**
 * Validation group applied when updating an existing record. Extends
 * {@link Default} so that ungrouped constraints run, while create-only
 * constraints (e.g. password required) are skipped.
 */
public interface OnUpdate extends Default {
}
