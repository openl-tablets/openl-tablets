package org.openl.rules.calc.element;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.openl.rules.table.ICell;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.Invokable;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

@RequiredArgsConstructor
public class SpreadsheetCell implements Invokable {

    @Getter
    private final int rowIndex;
    @Getter
    private final int columnIndex;
    @Getter
    private final ICell sourceCell;

    @Getter
    private final SpreadsheetCellType spreadsheetCellType;
    @Getter
    private Object value;
    @Getter
    private IOpenClass type;

    @Getter
    @Setter
    private IOpenMethod method;

    @Getter
    @Setter
    private boolean returnCell;

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
        if (value instanceof IOpenMethod openMethod) {
            this.method = openMethod;
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

}
