package org.openl.rules.validation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;

/**
 * Provides information about data bean validation.
 * 
 */
public class BeanValidationResult extends ValidationResult {

    /**
     * Validated bean type.
     */
    private Class<?> beanType;

    /**
     * Set of property constraint violations.
     */
    private Set<PropertyConstraintViolation> propertyConstraintViolations = new HashSet<PropertyConstraintViolation>();

    public BeanValidationResult(ValidationStatus status, Class<?> beanType) {
        super(status);
        this.beanType = beanType;
    }

    public Class<?> getBeanType() {
        return beanType;
    }

    public Set<PropertyConstraintViolation> getPropertyConstraintViolations() {
        return Collections.unmodifiableSet(propertyConstraintViolations);
    }

    public void addPropertyConstraintViolation(PropertyConstraintViolation propertyConstraintViolation) {
        this.propertyConstraintViolations.add(propertyConstraintViolation);
    }

}
