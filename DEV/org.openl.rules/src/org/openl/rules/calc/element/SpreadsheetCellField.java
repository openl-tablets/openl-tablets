package org.openl.rules.calc.element;

import org.openl.rules.calc.ASpreadsheetField;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.table.Point;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetCellField extends ASpreadsheetField {

    protected SpreadsheetCell cell;
    private SpreadsheetStructureBuilderHolder structureBuilderContainer;

    public static SpreadsheetCellField createSpreadsheetCellField(SpreadsheetStructureBuilderHolder structureBuilderContainer,
            IOpenClass declaringClass,
            String name,
            SpreadsheetCell cell) {
        if (cell.getKind() == SpreadsheetCellType.METHOD)
            return new SpreadsheetCellField(structureBuilderContainer, declaringClass, name, cell);
        return new ConstSpreadsheetCellField(structureBuilderContainer, declaringClass, name, cell);
    }

    SpreadsheetCellField(SpreadsheetStructureBuilderHolder structureBuilderContainer,
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

    public Point getRelativeCoordinates() {
        return new Point(getCell().getColumnIndex(), getCell().getRowIndex());
    }

    public Point getAbsoluteCoordinates() {
        return new Point(getCell().getSourceCell().getAbsoluteColumn(), getCell().getSourceCell().getAbsoluteRow());
    }
    
    static class ConstSpreadsheetCellField extends SpreadsheetCellField {

        ConstSpreadsheetCellField(SpreadsheetStructureBuilderHolder structureBuilderContainer,
                IOpenClass declaringClass,
                String name,
                SpreadsheetCell cell) {
            super(structureBuilderContainer, declaringClass, name, cell);
        }

        @Override
        public Object get(Object target, IRuntimeEnv env) {
            // if (cell.getKind() == SpreadsheetCellType.METHOD)
            // return super.get(target, env);

            return cell.getValue();
        }

    }

}
