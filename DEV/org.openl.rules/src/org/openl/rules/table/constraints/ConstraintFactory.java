package org.openl.rules.table.constraints;

/**
 * @author Andrei Astrouski
 */
public class ConstraintFactory {

    public Constraint getConstraint(String value) {
        Constraint constraint = null;
        value = value == null ? "" : value;
        if (value.matches(LessThanConstraint.CONSTRAINT_MATCH)) {
            constraint = new LessThanConstraint(value);
        } else if (value.matches(MoreThanConstraint.CONSTRAINT_MATCH)) {
            constraint = new MoreThanConstraint(value);
        } else if (value.matches(UniqueInModuleConstraint.CONSTRAINT_MATCH)) {
            constraint = new UniqueInModuleConstraint(value);
        } else if (value.matches(UniqueActiveTableConstraint.CONSTRAINT_MATCH)) {
            constraint = new UniqueActiveTableConstraint(value);
        } else if (value.matches(DataEnumConstraint.CONSTRAINT_MATCH)) {
            constraint = new DataEnumConstraint(value);
        } else if (value.matches(RegexpValueConstraint.CONSTRAINT_MATCH)) {
            constraint = new RegexpValueConstraint(value);
        }
        // to be continued...
        return constraint;
    }

}
