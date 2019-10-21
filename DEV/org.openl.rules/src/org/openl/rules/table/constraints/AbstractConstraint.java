package org.openl.rules.table.constraints;

import java.util.Objects;

/**
 * @author Andrei Astrouski
 */
public abstract class AbstractConstraint implements Constraint {

    private String value;

    public AbstractConstraint(String value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    @Override
    public String getValue() {
        return value;
    }

}
