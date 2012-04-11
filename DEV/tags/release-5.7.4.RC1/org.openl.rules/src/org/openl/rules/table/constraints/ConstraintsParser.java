package org.openl.rules.table.constraints;

import java.util.ArrayList;
import java.util.List;

public class ConstraintsParser {

    public static List<Constraint> parse(String value) {
        List<Constraint> constraints = new ArrayList<Constraint>();
        ConstraintFactory constraintFactory = new ConstraintFactory();

        // TODO split string value with constraints by some symbol
        Constraint constraint = constraintFactory.getConstraint(value);
        if (constraint != null) {
            constraints.add(constraint);
        }

        return constraints;
    }
}
