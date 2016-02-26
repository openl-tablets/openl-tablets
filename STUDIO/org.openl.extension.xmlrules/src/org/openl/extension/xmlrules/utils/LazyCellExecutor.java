package org.openl.extension.xmlrules.utils;

import java.util.*;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
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
    private final List<RulesTableReference> arrays = new ArrayList<RulesTableReference>();

    private final Map<String, RulesTableReference> referenceMap = new HashMap<String, RulesTableReference>();

    private final Map<CellKey, Object> executionCache = new HashMap<CellKey, Object>();

    public LazyCellExecutor(XlsModuleOpenClass xlsModuleOpenClass, Object target, IRuntimeEnv env) {
        this.xlsModuleOpenClass = xlsModuleOpenClass;
        this.target = target;

        this.env = env;

        for (IOpenMethod method : xlsModuleOpenClass.getMethods()) {
            if (method instanceof Spreadsheet) {
                RulesTableReference reference = new RulesTableReference(method.getName());
                if (reference.getReference() != null && reference.getEndReference() != null) {
                    arrays.add(reference);
                }
            }
        }
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

    public Object[][] getCellValues(String cell, int rows, int cols) {
        Object[][] result = new Object[rows][cols];
        RulesTableReference reference = new RulesTableReference(CellReference.parse(cell));
        int row = Integer.parseInt(reference.getRow());
        int col = Integer.parseInt(reference.getColumn());

        CellReference start = reference.getReference();
        String workbook = start.getWorkbook();
        String sheet = start.getSheet();

        for (int i = 0; i < rows; i++) {
            String currentRow = String.valueOf(row + i);
            for (int j = 0; j < cols; j++) {
                CellReference cr = new CellReference(workbook, sheet, currentRow, String.valueOf(col + j));
                result[i][j] = getCellUsingCache(cr.getStringValue());
            }
        }

        return result;
    }

    public Object getCellValue(String cell) {
        return getCellUsingCache(cell);
    }

    private Object getCellUsingCache(String cell) {
        if (!params.containsKey(cell)) {
            RulesTableReference tableReference = getTableReference(cell);

            if (tableReference.getEndReference() == null) {
                CellKey key = getCellKey(cell);
                if (executionCache.containsKey(key)) {
                    return executionCache.get(key);
                } else {
                    String rulesTable = tableReference.getTable();
                    String row = tableReference.getRow();
                    String column = tableReference.getColumn();

                    IOpenMethod cellsHolder = xlsModuleOpenClass.getMethod(rulesTable,
                            new IOpenClass[] { JavaOpenClass.STRING, JavaOpenClass.STRING });
                    Object result = cellsHolder.invoke(target, new Object[] { row, column }, env);
                    executionCache.put(key, result);
                    return result;
                }
            } else {
                String rulesTable = tableReference.getTable();

                SpreadsheetResult result;

                CellKey key = getCellKey(rulesTable);
                if (executionCache.containsKey(key)) {
                    result = (SpreadsheetResult) executionCache.get(key);
                } else {
                    Spreadsheet cellsHolder = (Spreadsheet) xlsModuleOpenClass.getMethod(rulesTable, new IOpenClass[] {});
                    result = (SpreadsheetResult) cellsHolder.invoke(target, new Object[] {}, env);
                    executionCache.put(key, result);
                }

                RulesTableReference reference = new RulesTableReference(CellReference.parse(cell));
                String row = reference.getRow();
                String column = reference.getColumn();

                return result.getFieldValue("$C" + column + "$R" + row);
            }
        }

        Deque<Object> objects = params.get(cell);
        return objects.getLast();
    }

    private CellKey getCellKey(String cell) {
        Map<String, Object> executionParams = new HashMap<String, Object>();
        for (Map.Entry<String, Deque<Object>> entry : params.entrySet()) {
            executionParams.put(entry.getKey(), entry.getValue().getLast());
        }
        return new CellKey(cell, executionParams);
    }

    public RulesTableReference getArrayReference(String cell) {
        CellReference cellReference = CellReference.parse(cell);

        for (RulesTableReference reference : arrays) {
            if (reference.contains(cellReference)) {
                return reference;
            }
        }

        return null;
    }

    public RulesTableReference getTableReference(String cell) {
        // TODO Make it clear, what returns the method: table reference or specific cell reference
        RulesTableReference rulesTableReference = referenceMap.get(cell);
        if (rulesTableReference == null) {
            rulesTableReference = getArrayReference(cell);
            referenceMap.put(cell, rulesTableReference);
        }
        if (rulesTableReference == null) {
            rulesTableReference = new RulesTableReference(CellReference.parse(cell));
            referenceMap.put(cell, rulesTableReference);
        }
        return rulesTableReference;
    }
}
