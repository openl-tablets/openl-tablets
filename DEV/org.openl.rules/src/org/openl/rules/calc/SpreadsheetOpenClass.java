package org.openl.rules.calc;

import org.openl.OpenL;
import org.openl.binding.impl.component.ComponentOpenClass;

public class SpreadsheetOpenClass extends ComponentOpenClass {

    public SpreadsheetOpenClass(String name, OpenL openl) {
        super(name, openl);
    }

    @Override
    public Class<?> getInstanceClass() {
        return SpreadsheetResult.class;
    }
}
