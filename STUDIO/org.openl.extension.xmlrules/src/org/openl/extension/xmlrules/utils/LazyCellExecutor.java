package org.openl.extension.xmlrules.utils;

import java.util.*;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class LazyCellExecutor {
    private static ThreadLocal<LazyCellExecutor> instanceHolder = new ThreadLocal<LazyCellExecutor>();
    private final Map<String, Deque<Object>> params = new HashMap<String, Deque<Object>>();
    private final Object target;
    private final IRuntimeEnv env;
    private final XlsModuleOpenClass xlsModuleOpenClass;

    private final Map<String, RulesTableReference> referenceMap = new HashMap<String, RulesTableReference>();

    public LazyCellExecutor(XlsModuleOpenClass xlsModuleOpenClass, Object target, IRuntimeEnv env) {
        this.xlsModuleOpenClass = xlsModuleOpenClass;
        this.target = target;

        this.env = env;
    }

    public static LazyCellExecutor getInstance() {
        return instanceHolder.get();
    }

    public static void setInstance(LazyCellExecutor instance) {
        instanceHolder.set(instance);
    }

    public static void reset() {
        instanceHolder.remove();
    }

    public void push(String cell, Object value) {
        Deque<Object> objects = params.get(cell);
        if (objects == null) {
            objects = new ArrayDeque<Object>();
            params.put(cell, objects);
        }
        objects.push(value);
    }

    public void pop(String cell) {
        Deque<Object> objects = params.get(cell);
        if (objects != null) {
            objects.pop();
            if (objects.isEmpty()) {
                params.remove(cell);
            }
        }
    }

    public Object getCellValue(String cell) {
        if (!params.containsKey(cell)) {
            RulesTableReference reference = getReference(cell);

            String rulesTable = reference.getTable();
            String row = reference.getRow();
            String column = reference.getColumn();

            IOpenMethod cellsHolder = xlsModuleOpenClass.getMethod(rulesTable,
                    new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.STRING });
            return cellsHolder.invoke(target, new Object[] {row, column}, env);
        }

        Deque<Object> objects = params.get(cell);
        return objects.getLast();
    }

    public RulesTableReference getReference(String cell) {
        RulesTableReference rulesTableReference = referenceMap.get(cell);
        if (rulesTableReference == null) {
            rulesTableReference = new RulesTableReference(CellReference.parse(cell));
            referenceMap.put(cell, rulesTableReference);
        }
        return rulesTableReference;
    }
}
