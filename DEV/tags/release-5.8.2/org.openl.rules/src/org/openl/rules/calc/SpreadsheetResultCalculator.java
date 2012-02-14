package org.openl.rules.calc;

import java.util.Map;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.calc.element.SpreadsheetCellType;
import org.openl.rules.calc.trace.SpreadsheetTraceObject;
import org.openl.rules.calc.trace.SpreadsheetTracerLeaf;
import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

public class SpreadsheetResultCalculator implements IDynamicObject {

    private Spreadsheet spreadsheet;

    private boolean cacheResult = true;
    private SpreadsheetTraceObject spreadsheetTraceObject;
    /**
     * OpenL module
     */
    private IDynamicObject targetModule;
    /**
     * Copy of the spreadsheet call parameters.
     */
    private Object[] params; 
    /**
     * Copy of the call environment.
     */
    private IRuntimeEnv env;

    private Object[][] results;

    public SpreadsheetResultCalculator(Spreadsheet spreadsheet, IDynamicObject targetModule, Object[] params,
            IRuntimeEnv env) {
        super();

        this.spreadsheet = spreadsheet;
        this.targetModule = targetModule;
        this.params = params;
        this.env = env;
        this.results = new Object[spreadsheet.getHeight()][spreadsheet.getWidth()];
    }

    public SpreadsheetResultCalculator(Spreadsheet spreadsheet, IDynamicObject targetModule, Object[] params,
            IRuntimeEnv env, SpreadsheetTraceObject spreadsheetTraceObject) {
        super();

        this.spreadsheet = spreadsheet;
        this.targetModule = targetModule;
        this.params = params;
        this.env = env;
        this.results = new Object[spreadsheet.getHeight()][spreadsheet.getWidth()];
        this.spreadsheetTraceObject = spreadsheetTraceObject;
    }

    public Object getColumn(int column, IRuntimeEnv env2) {
        return null;
    }

    public String getColumnName(int column) {
        return spreadsheet.getColumnNames()[column];
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

    public String getRowName(int row) {
        return spreadsheet.getRowNames()[row];
    }

    public int getRowIndex(String name) {
        
        String[] names = spreadsheet.getRowNames();
        
        for (int i = 0; i < names.length; i++) {
            if (name.equals(names[i]))
                return i;
        }
        
        throw new OpenLRuntimeException("Row name <" + name + "> not found", spreadsheet.getBoundNode());
    }


    public int getColumnIndex(String name) {
        
        String[] names = spreadsheet.getColumnNames();
        
        for (int i = 0; i < names.length; i++) {
            if (name.equals(names[i]))
                return i;
        }
        
        throw new OpenLRuntimeException("Column name <" + name + "> not found", spreadsheet.getBoundNode());
    }
    
    
    public Spreadsheet getSpreadsheet() {
        return spreadsheet;
    }

    public IOpenClass getType() {
        return spreadsheet.getSpreadsheetType();
    }
    
    private boolean isTraceOn(){
        return spreadsheetTraceObject != null;
    }

    public Object getValue(int row, int column) {
        SpreadsheetCell spreadsheetCell = spreadsheet.getCells()[row][column];
        if (isTraceOn() && spreadsheetCell.getKind() != SpreadsheetCellType.EMPTY) {
            getValueTraced(row, column);
        }
        
        Object result = null;
        
        if (cacheResult) {

            result = results[row][column];
        
            if (result != null) {
                return result;
            }
        }

        result = spreadsheetCell.calculate(this, targetModule, params, env);
        results[row][column] = result;

        return result;
    }

    public Object getValueTraced(int row, int column) {
        SpreadsheetCell spreadsheetCell = spreadsheet.getCells()[row][column];

        Tracer tracer = Tracer.getTracer();
        SpreadsheetTracerLeaf spreadsheetTraceLeaf = new SpreadsheetTracerLeaf(spreadsheetTraceObject, spreadsheetCell);
        tracer.push(spreadsheetTraceLeaf);

        Object result = null;
        
        try {
            
        if (cacheResult) {

            result = results[row][column];
        
            if (result != null) {
                spreadsheetTraceLeaf.setValue(result);
                return result;
            }
        }

        result = spreadsheetCell.calculate(this, targetModule, params, env);
        results[row][column] = result;

        spreadsheetTraceLeaf.setValue(result);
        return result;
    } finally {
        tracer.pop();
    }
    }

    public final int height() {
        return spreadsheet.getHeight();
    }

    public void setFieldValue(String name, Object value) {
    }

    @Override
    public String toString() {
        return "Spreadsheet[" + width() + " x " + height() + "]";
    }

    public final int width() {
        return spreadsheet.getWidth();
    }
}
