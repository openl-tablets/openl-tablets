package org.openl.validation;

import org.openl.types.IOpenClass;

/**
 * The <code>IOpenValidator</code> interface defines OpenL validator abstraction.
 *
 */
public interface IOpenLValidator {

    /**
     * Validates open class instance.
     *
     * @param openClass open class to validate
     * @return validation result
     */
    ValidationResult validate(IOpenClass openClass);
}
