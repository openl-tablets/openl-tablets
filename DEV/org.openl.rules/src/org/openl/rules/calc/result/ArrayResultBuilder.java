package org.openl.rules.calc.result;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;

public class ArrayResultBuilder implements IResultBuilder {

    private IOpenClass type;
    private SpreadsheetCell[] cells;
    private IOpenCast[] openCasts;
    private boolean calculateAllCells;

    public ArrayResultBuilder(SpreadsheetCell[] cells,
            IOpenCast[] openCasts,
            IOpenClass type,
            boolean calculateAllCells) {
        this.cells = cells;
        this.openCasts = openCasts;
        this.type = type;
        this.calculateAllCells = calculateAllCells;
    }

    @Override
    public Object makeResult(SpreadsheetResultCalculator resultCalculator) {
        int size = cells.length;
        IAggregateInfo aggregateInfo = type.getAggregateInfo();
        Object array = aggregateInfo.makeIndexedAggregate(aggregateInfo.getComponentType(type), size);

        IOpenIndex index = aggregateInfo.getIndex(type);
        Object[][] result = null;
        if (calculateAllCells) {
            result = resultCalculator.getValues();
        }
        for (int i = 0; i < size; ++i) {
            SpreadsheetCell cell = cells[i];
            Object value;
            if (calculateAllCells) {
                value = result[cell.getRowIndex()][cell.getColumnIndex()];
            } else {
                value = resultCalculator.getValue(cell.getRowIndex(), cell.getColumnIndex());
            }

            if (value == null) {
                value = type.nullObject();
            } else {
                value = openCasts[i].convert(value);
            }

            index.setValue(array, i, value);
        }

        return array;
    }

}
