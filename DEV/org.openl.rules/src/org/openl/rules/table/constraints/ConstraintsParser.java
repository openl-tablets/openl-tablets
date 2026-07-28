package org.openl.rules.table.constraints;

import java.util.ArrayList;
import java.util.List;

import org.openl.util.StringUtils;

public final class ConstraintsParser {

    private ConstraintsParser() {
    }

    public static final String CONSTRAINTS_SEPARATOR = "&";

    public static List<Constraint> parse(String value) {
        var constraints = new ArrayList<Constraint>();
        var constraintFactory = new ConstraintFactory();

        if (StringUtils.isNotBlank(value)) {
            for (String constraintExpression : value.split(CONSTRAINTS_SEPARATOR)) {
                var constraint = constraintFactory.getConstraint(constraintExpression);
                if (constraint != null) {
                    constraints.add(constraint);
                }
            }
        }

        return constraints;
    }
}
