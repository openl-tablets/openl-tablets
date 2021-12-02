package org.openl.rules.calc;

import org.openl.binding.impl.method.AOpenMethodDelegator;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.vm.IRuntimeEnv;

public class AnySpreadsheetResultOpenClass extends JavaOpenClass {

    public static final AnySpreadsheetResultOpenClass INSTANCE = new AnySpreadsheetResultOpenClass(null);

    // Do not remove parameter
    public AnySpreadsheetResultOpenClass(Class<?> type) {
        super(SpreadsheetResult.class);
    }

    @Override
    public String getName() {
        return AnySpreadsheetResult.class.getName();
    }

    @Override
    public String getPackageName() {
        return AnySpreadsheetResult.class.getPackage().getName();
    }

    @Override
    public IOpenField getField(String fieldName, boolean strictMatch) {
        IOpenField field = super.getField(fieldName, strictMatch);
        if (field == null && fieldName.startsWith("$")) {
            field = new SpreadsheetResultField(this, fieldName, JavaOpenClass.OBJECT);
        }
        return field;
    }

    @Override
    protected IOpenMethod processConstructor(JavaOpenConstructor constructor) {
        return new AOpenMethodDelegator(super.processConstructor(constructor)) {
            @Override
            public IOpenClass getType() {
                return AnySpreadsheetResultOpenClass.this;
            }
        };
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        if (SpreadsheetResult.class.equals(getInstanceClass())) {
            return new StubSpreadSheetResult();
        } else {
            return super.newInstance(env);
        }
    }
}
