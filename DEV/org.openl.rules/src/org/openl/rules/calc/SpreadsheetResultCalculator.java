package org.openl.rules.calc;

import java.util.Map;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.calc.element.SpreadsheetCellType;
import org.openl.rules.calc.trace.SpreadsheetTracerLeaf;
import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

public class SpreadsheetResultCalculator implements IDynamicObject {

	public static final Object NEED_TO_CALCULATE_VALUE = new Object();

	private Spreadsheet spreadsheet;

    private boolean cacheResult = true;
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

    public SpreadsheetResultCalculator(Spreadsheet spreadsheet, IDynamicObject targetModule, Object[] params,
            IRuntimeEnv env, Object[][] preCalculatedResult) {
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
    
    public Object getValue(int row, int column) {
        SpreadsheetCell spreadsheetCell = spreadsheet.getCells()[row][column];
        if (Tracer.isTracerOn() && spreadsheetCell.getKind() != SpreadsheetCellType.EMPTY) {
            getValueTraced(row, column);        
        } else {
        	Object result = null;
            
            if (cacheResult) {

                result = results[row][column];
            
                if (result != NEED_TO_CALCULATE_VALUE) {
                    return result;
                }
            }

            result = spreadsheetCell.calculate(this, targetModule, params, env);
            results[row][column] = result;            
        }
        return results[row][column];
    }
    
	public void setValue(int row, int column, Object res) {
		results[row][column] = res;
	}
   
    

    public Object getValueTraced(int row, int column) {
        SpreadsheetCell spreadsheetCell = spreadsheet.getCells()[row][column];

        SpreadsheetTracerLeaf spreadsheetTraceLeaf = new SpreadsheetTracerLeaf(spreadsheetCell);

        Tracer.begin(spreadsheetTraceLeaf);
        try {
            Object result;
	        if (cacheResult) {
	
	            result = results[row][column];
	        
	            if (result != null) {
	                spreadsheetTraceLeaf.setResult(result);
	                return result;
	            }
	        }
	
	        result = spreadsheetCell.calculate(this, targetModule, params, env);
	        results[row][column] = result;
	
	        spreadsheetTraceLeaf.setResult(result);
	        return result;
        } finally {
            Tracer.end();
        }
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
