package org.openl.rules.table.constraints;

/**
 * @author Andrei Astrouski
 */
public abstract class AbstractConstraint implements Constraint {

    private String value;

    public AbstractConstraint(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Param 'value' can not be null");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
