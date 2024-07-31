package org.openl.rules.calc.element;

import org.openl.base.INamedThing;
import org.openl.binding.impl.NodeDescriptionHolder;
import org.openl.rules.calc.ASpreadsheetField;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetCellField extends ASpreadsheetField implements NodeDescriptionHolder {

    protected final SpreadsheetCell cell;
    private final SpreadsheetStructureBuilderHolder structureBuilderContainer;
    private IOpenClass type;

    public SpreadsheetCellField(SpreadsheetStructureBuilderHolder structureBuilderContainer,
                                IOpenClass declaringClass,
                                String columnName, String rowName,
                                SpreadsheetCell cell) {
        super(declaringClass, columnName, rowName, cell.getType());

        this.cell = cell;
        this.structureBuilderContainer = structureBuilderContainer;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        SpreadsheetResultCalculator spreadsheetResultCalculator = (SpreadsheetResultCalculator) target;

        if (spreadsheetResultCalculator == null) {
            return getType().nullObject();
        }

        return spreadsheetResultCalculator.getValue(cell.getRowIndex(), cell.getColumnIndex());
    }

    public SpreadsheetCell getCell() {
        return cell;
    }

    @Override
    public IOpenClass getType() {
        if (this.type == null) {
            IOpenClass t = cell.getType();
            if (t == null) {
                if (structureBuilderContainer.getSpreadsheetStructureBuilder() == null) {
                    throw new IllegalStateException("Spreadsheet cell type is not resolved at compile time");
                }
                t = structureBuilderContainer.getSpreadsheetStructureBuilder().makeType(cell);
            }
            this.type = t;
        }
        return this.type;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException("Cannot write to spreadsheet cell result");
    }

    @Override
    public String getDescription() {
        // This class is always used for internal spreadsheet cell references, so no need to show Spreadsheet name
        return getType().getDisplayName(INamedThing.SHORT) + " " + getName();
    }

    public static class ConstSpreadsheetCellField extends SpreadsheetCellField {

        public ConstSpreadsheetCellField(SpreadsheetStructureBuilderHolder structureBuilderContainer,
                                         IOpenClass declaringClass,
                                         String columnName, String rowName,
                                         SpreadsheetCell cell) {
            super(structureBuilderContainer, declaringClass, columnName, rowName, cell);
        }

        @Override
        public Object get(Object target, IRuntimeEnv env) {
            return cell.getValue();
        }

    }

}
