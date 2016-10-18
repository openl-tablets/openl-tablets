package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.List;

import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public class CustomSpreadsheetResultOpenClass extends JavaOpenClass {
    public CustomSpreadsheetResultOpenClass(Class<?> type) {
        super(type);
    }

    private Iterable<IOpenClass> superClasses = null;

    private static IOpenClass spreadsheetResultOpenClass = JavaOpenClass.createNewOpenClass(SpreadsheetResult.class);

    public synchronized Iterable<IOpenClass> superClasses() {
        if (superClasses == null) {
            Class<?>[] interfaces = instanceClass.getInterfaces();
            Class superClass = instanceClass.getSuperclass();
            List<IOpenClass> superClasses = new ArrayList<IOpenClass>(interfaces.length + 1);
            if (superClass != null) {
                superClasses.add(spreadsheetResultOpenClass);
            }
            for (Class<?> interf : interfaces) {
                superClasses.add(getOpenClass(interf));
            }
            this.superClasses = superClasses;
        }

        return superClasses;
    }
}