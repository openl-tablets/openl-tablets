package org.openl.rules.calc;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class SCell {

    public enum CellKind {
        VALUE,
        METHOD,
        EMPTY
    }

    int row, column;;

    CellKind kind = CellKind.EMPTY;
    Object value;
    IOpenClass type;

    IOpenMethod method;

    public SCell(int row, int column) {
        super();
        this.row = row;
        this.column = column;
    }

    public Object calculate(SpreadsheetResult spreadsheetResult, Object targetModule, Object[] params, IRuntimeEnv env) {
        if (isValueCell()) {
            Object value = getValue();
            if (value instanceof AnyCellValue) {
                return ((AnyCellValue) value).getValue();
            }
            return value;
        }

        else if (isMethodCell()) {
            return getMethod().invoke(spreadsheetResult, params, env);
        } else {
            return null;
        }
    }

    public int getColumn() {
        return column;
    }

    public CellKind getKind() {
        return kind;
    }

    public IOpenMethod getMethod() {
        return method;
    }

    public int getRow() {
        return row;
    }

    public IOpenClass getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public boolean isEmpty() {
        return kind == CellKind.EMPTY;
    }

    public boolean isMethodCell() {
        return kind == CellKind.METHOD;
    }

    public boolean isValueCell() {
        return kind == CellKind.VALUE;
    }

    public void setKind(CellKind kind) {
        this.kind = kind;
    }

    public void setMethod(IOpenMethod method) {
        this.method = method;
    }

    public void setType(IOpenClass type) {
        this.type = type;
    }

    public void setValue(Object value) {
        if (value == null) {
            kind = CellKind.EMPTY;
        } else if (value instanceof IOpenMethod) {
            kind = CellKind.METHOD;
            method = (IOpenMethod) value;
        } else {
            this.value = value;
            kind = CellKind.VALUE;
        }
    }

}
