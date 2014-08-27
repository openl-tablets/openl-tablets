package org.openl.rules.calc.element;

import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.table.ICell;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.NumberUtils;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetCell {

    private int rowIndex;
    private int columnIndex;
    private ICell sourceCell;


	private SpreadsheetCellType kind = SpreadsheetCellType.EMPTY;
    private Object value;
    private IOpenClass type;

    private IOpenMethod method;

    public SpreadsheetCell(int rowIndex, int columnIndex, ICell sourceCell) {
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
        this.sourceCell = sourceCell;
    }

    public ICell getSourceCell() {
        return sourceCell;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public SpreadsheetCellType getKind() {
        return kind;
    }

    public IOpenMethod getMethod() {
        return method;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public IOpenClass getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public boolean isEmpty() {
        return kind == SpreadsheetCellType.EMPTY;
    }

    public boolean isMethodCell() {
        return kind == SpreadsheetCellType.METHOD;
    }

    public boolean isValueCell() {
        return kind == SpreadsheetCellType.VALUE;
    }

    public void setKind(SpreadsheetCellType kind) {
        this.kind = kind;
    }

    public void setMethod(IOpenMethod method) {
        this.method = method;
    }

    public void setType(IOpenClass type) {
    	if (type == null)
    		return;
    	if (type == JavaOpenClass.VOID)
    		type = JavaOpenClass.getOpenClass(Void.class);
    	else if (type.getInstanceClass().isPrimitive())
    	{
    		Class<?> wrapper = NumberUtils.getWrapperType(type.getInstanceClass().getName());
    		type = JavaOpenClass.getOpenClass(wrapper);
    	}	
        this.type = type;
    }

    public void setValue(Object value) {

        if (value == null) {
//            this.kind = SpreadsheetCellType.EMPTY;
        } else if (value instanceof IOpenMethod) {
//            this.kind = SpreadsheetCellType.METHOD;
            this.method = (IOpenMethod) value;
        } else {
            this.value = value;
//            this.kind = SpreadsheetCellType.VALUE;
        }
    }

    public Object calculate(SpreadsheetResultCalculator spreadsheetResult, Object targetModule, Object[] params, IRuntimeEnv env) {

        if (isValueCell()) {
            return getValue();
        } else if (isMethodCell()) {
            IOpenMethod cellMethod = getMethod();

            if (cellMethod == null) {
                throw new IllegalStateException("Cell isn't initialized properly");
            }

            return cellMethod.invoke(spreadsheetResult, params, env);
        } else {
            return null;
        }
    }
    
    @Override
	public String toString() {
		return "R" + getRowIndex() + "C" + getColumnIndex() ;
	}


}
