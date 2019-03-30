package org.openl.rules.calc.element;

import org.openl.base.INamedThing;
import org.openl.binding.impl.NodeDescriptionHolder;
import org.openl.rules.calc.ASpreadsheetField;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetCellField extends ASpreadsheetField implements NodeDescriptionHolder {

    protected SpreadsheetCell cell;
    private SpreadsheetStructureBuilderHolder structureBuilderContainer;

    public SpreadsheetCellField(SpreadsheetStructureBuilderHolder structureBuilderContainer,
            IOpenClass declaringClass,
            String name,
            SpreadsheetCell cell) {
        super(declaringClass, name, cell.getType());

        this.cell = cell;
        this.structureBuilderContainer = structureBuilderContainer;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        SpreadsheetResultCalculator result = (SpreadsheetResultCalculator) target;

        if (result == null) {
            return getType().nullObject();
        }

        return result.getValue(cell.getRowIndex(), cell.getColumnIndex());
    }

    public SpreadsheetCell getCell() {
        return cell;
    }

    @Override
    public IOpenClass getType() {
        IOpenClass t = cell.getType();
        if (t == null) {
            t = structureBuilderContainer.getSpreadsheetStructureBuilder().makeType(cell);
        }
        return t;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException("Can not write to spreadsheet cell result");
    }

    @Override
    public String getDescription() {
        // This class is always used for internal spreadsheet cell references, so no need to show Spreadsheet name
        return getType().getDisplayName(INamedThing.SHORT) + " " + getName();
    }

    public static class ConstSpreadsheetCellField extends SpreadsheetCellField {

        public ConstSpreadsheetCellField(SpreadsheetStructureBuilderHolder structureBuilderContainer,
                IOpenClass declaringClass,
                String name,
                SpreadsheetCell cell) {
            super(structureBuilderContainer, declaringClass, name, cell);
        }

        @Override
        public Object get(Object target, IRuntimeEnv env) {
            return cell.getValue();
        }

    }

}
