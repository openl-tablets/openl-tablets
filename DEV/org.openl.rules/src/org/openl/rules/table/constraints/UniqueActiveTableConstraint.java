package org.openl.rules.table.constraints;

public class UniqueActiveTableConstraint extends AbstractConstraint {

    public static final String CONSTRAINT_MATCH = "^\\s*unique\\s+in\\s*:\\s*TableGroup\\s*$";

    public UniqueActiveTableConstraint(String value) {
        super(value);
    }

    @Override
    public boolean check(Object... valuesToCheck) {
        return false;
    }

    @Override
    public Object[] getParams() {
        return new Object[0];
    }

}
