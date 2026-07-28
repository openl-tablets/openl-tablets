package org.openl.rules.table.constraints;

import java.util.Objects;

import lombok.Getter;

/**
 * @author Andrei Astrouski
 */
public abstract class AbstractConstraint implements Constraint {

    @Getter
    private final String value;

    public AbstractConstraint(String value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

}
