package org.openl.rules.dt.element;

import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class DTColumnSpreadsheetResultOpenClass extends JavaOpenClass {

    public static final DTColumnSpreadsheetResultOpenClass INSTANCE = new DTColumnSpreadsheetResultOpenClass();

    public DTColumnSpreadsheetResultOpenClass() {
        super(SpreadsheetResult.class);
    }

    @Override
    public String getName() {
        return SpreadsheetResult.class.getName();
    }

    @Override
    public String getPackageName() {
        return SpreadsheetResult.class.getPackage().getName();
    }

    @Override
    public IOpenField getField(String fieldName, boolean strictMatch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        return ioc instanceof CustomSpreadsheetResultOpenClass || ioc instanceof SpreadsheetResultOpenClass;
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
