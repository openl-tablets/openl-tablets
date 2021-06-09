package org.openl.rules.table.constraints;

public class UniqueInModuleConstraint extends AbstractConstraint {

    public static final String CONSTRAINT_MATCH = "^\\s*unique\\s+in\\s*:\\s*module\\s*$";
    private static final Object[] NO_PARAMS = new Object[0];

    public UniqueInModuleConstraint(String value) {
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
