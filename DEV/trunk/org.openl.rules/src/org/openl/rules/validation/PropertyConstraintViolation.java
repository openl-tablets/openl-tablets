package org.openl.rules.validation;

/**
 * Provides information about property which doesn't satisfy validation
 * constraints.
 */
public class PropertyConstraintViolation {

    private String message;
    private String propertyName;
    private Object invalidValue;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }

    public void setInvalidValue(Object invalidValue) {
        this.invalidValue = invalidValue;
    }

}
