package org.openl.rules.calc.element;

import org.openl.rules.calc.ASpreadsheetField;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetCellField extends ASpreadsheetField {

    private SpreadsheetCell cell;

    public SpreadsheetCellField(IOpenClass declaringClass, String name, SpreadsheetCell cell) {
        super(declaringClass, name, cell.getType());
        
        this.cell = cell;
    }

//    @Override
//    public Object calculate(SpreadsheetResultCalculator spreadsheetResult, Object targetModule, Object[] params, IRuntimeEnv env) {
//        return cell.calculate(spreadsheetResult, targetModule, params, env);
//    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {

        SpreadsheetResultCalculator result = (SpreadsheetResultCalculator) target;

        return result.getValue(cell.getRowIndex(), cell.getColumnIndex());
    }

    public SpreadsheetCell getCell() {
        return cell;
    }

    @Override
    public IOpenClass getType() {
        return cell.getType();
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException("Can not write to spreadsheet cell result");
    }

}
