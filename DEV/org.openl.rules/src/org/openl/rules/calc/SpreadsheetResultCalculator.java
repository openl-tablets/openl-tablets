package org.openl.rules.calc;

import java.util.Map;

import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.calc.element.SpreadsheetCellType;
import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

public class SpreadsheetResultCalculator implements IDynamicObject {
    public static final Object NEED_TO_CALCULATE_VALUE = new Object();
    public static final Object EMPTY_CELL = new Object();

    private Spreadsheet spreadsheet;
    /**
     * OpenL module
     */
    protected IDynamicObject targetModule;
    /**
     * Copy of the spreadsheet call parameters.
     */
    protected Object[] params;
    /**
     * Copy of the call environment.
     */
    protected IRuntimeEnv env;

    private Object[][] results;

    public SpreadsheetResultCalculator(Spreadsheet spreadsheet,
            IDynamicObject targetModule,
            Object[] params,
            IRuntimeEnv env,
            Object[][] preCalculatedResult) {
        super();

        this.spreadsheet = spreadsheet;
        this.targetModule = targetModule;
        this.params = params;
        this.env = env;
        if (preCalculatedResult == null)
            this.results = new Object[spreadsheet.getHeight()][spreadsheet.getWidth()];
        else
            this.results = clonePrecalculatedResults(preCalculatedResult);
    }

    private Object[][] clonePrecalculatedResults(Object[][] preCalculatedResult) {
        Object[][] res = preCalculatedResult.clone();
        for (int i = 0; i < res.length; i++) {
            res[i] = preCalculatedResult[i].clone();
        }

        return res;
    }

    public Object getFieldValue(String name) {

        IOpenField field = spreadsheet.getSpreadsheetType().getField(name);

        if (field == null) {
            return targetModule.getFieldValue(name);
        }

        SpreadsheetCellField cellField = (SpreadsheetCellField) field;

        int row = cellField.getCell().getRowIndex();
        int column = cellField.getCell().getColumnIndex();

        return getValue(row, column);
    }

    public Map<String, Object> getFieldValues() {
        throw new UnsupportedOperationException("Should not be called, this is only used in NicePrinter");
    }

    public Object getRow(int row, IRuntimeEnv env) {
        return null;
    }

    public Spreadsheet getSpreadsheet() {
        return spreadsheet;
    }

    public IOpenClass getType() {
        return spreadsheet.getSpreadsheetType();
    }

    public Object getValue(int row, int column) {
        Object result = results[row][column];
        if (result == EMPTY_CELL) {
            return null;
        }
        SpreadsheetCell spreadsheetCell = spreadsheet.getCells()[row][column];
        if (result != NEED_TO_CALCULATE_VALUE) {
            if (spreadsheetCell.getSpreadsheetCellType() == SpreadsheetCellType.METHOD) {
                Tracer.resolveTraceNode(spreadsheetCell, this, params, env, this);
            } else {
                Tracer.put(spreadsheetCell, "cell", result);
            }
            return result;
        }
        result = Tracer.invoke(spreadsheetCell, this, params, env, this);
        results[row][column] = result;
        return result;
    }

    public void setValue(int row, int column, Object res) {
        results[row][column] = res;
    }

    public final int height() {
        return spreadsheet.getHeight();
    }

    public void setFieldValue(String name, Object value) {
        targetModule.setFieldValue(name, value);
    }

    @Override
    public String toString() {
        return "Spreadsheet[" + width() + " x " + height() + "]";
    }

    public final int width() {
        return spreadsheet.getWidth();
    }
}
