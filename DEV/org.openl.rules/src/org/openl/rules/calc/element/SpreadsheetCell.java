package org.openl.rules.calc.element;

import org.openl.rules.table.ICell;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.Invokable;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetCell implements Invokable {

    private int rowIndex;
    private int columnIndex;
    private ICell sourceCell;

    private SpreadsheetCellType spreadsheetCellType;
    private Object value;
    private IOpenClass type;

    private IOpenMethod method;

    private boolean typeUnknown = false;

    public SpreadsheetCell(int rowIndex, int columnIndex, ICell sourceCell, SpreadsheetCellType spreadsheetCellType) {
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
        this.sourceCell = sourceCell;
        this.spreadsheetCellType = spreadsheetCellType;
    }

    public ICell getSourceCell() {
        return sourceCell;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public SpreadsheetCellType getSpreadsheetCellType() {
        return spreadsheetCellType;
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
        return spreadsheetCellType == SpreadsheetCellType.EMPTY;
    }

    public boolean isMethodCell() {
        return spreadsheetCellType == SpreadsheetCellType.METHOD;
    }

    public boolean isValueCell() {
        return spreadsheetCellType == SpreadsheetCellType.VALUE;
    }

    public boolean isConstantCell() {
        return spreadsheetCellType == SpreadsheetCellType.CONSTANT;
    }

    public boolean isDefaultPrimitiveCell() {
        return type != null && !(type instanceof DomainOpenClass) && type.getInstanceClass() != null && type
            .getInstanceClass()
            .isPrimitive() && isEmpty();
    }

    public void setMethod(IOpenMethod method) {
        this.method = method;
    }

    public void setType(IOpenClass type) {
        if (type != null) {
            if (type.equals(NullOpenClass.the)) {
                this.type = NullOpenClass.the;
            } else {
                this.type = type == JavaOpenClass.VOID ? JavaOpenClass.getOpenClass(Void.class) : type;
            }
        }
    }

    public void setValue(Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof IOpenMethod) {
            this.method = (IOpenMethod) value;
        } else {
            this.value = value;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object spreadsheetResult, Object[] params, IRuntimeEnv env) {
        if (isValueCell() || isConstantCell() || isDefaultPrimitiveCell()) {
            return getValue();
        } else if (isMethodCell()) {
            return getMethod().invoke(spreadsheetResult, params, env);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "R" + getRowIndex() + "C" + getColumnIndex();
    }

    public void setTypeUnknown(boolean typeUnknown) {
        this.typeUnknown = typeUnknown;
    }

    public boolean isTypeUnknown() {
        return typeUnknown;
    }
}
