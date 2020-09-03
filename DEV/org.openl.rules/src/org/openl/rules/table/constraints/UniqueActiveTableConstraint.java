package org.openl.rules.table.constraints;

public class UniqueActiveTableConstraint extends AbstractConstraint {

    public static final String CONSTRAINT_MATCH = "^\\s*unique\\s+in\\s*:\\s*TableGroup\\s*$";
    private static final Object[] NO_PARAMS = new Object[0];

    public UniqueActiveTableConstraint(String value) {
        super(value);
    }

    @Override
    public boolean check(Object... valuesToCheck) {
        return false;
    }

    @Override
    public Object[] getParams() {
        return NO_PARAMS;
    }

}
