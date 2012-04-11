package org.openl.rules.table.constraints;

public class UniqueInModuleConstraint extends AbstractConstraint {

    public static final String CONSTRAINT_MATCH = "^\\s*unique\\s+in\\s*:\\s*module\\s*$";

    public UniqueInModuleConstraint(String value) {
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
