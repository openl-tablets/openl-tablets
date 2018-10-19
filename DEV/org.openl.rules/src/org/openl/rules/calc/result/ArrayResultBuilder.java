package org.openl.rules.calc.result;

import java.util.List;

import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;

public class ArrayResultBuilder implements IResultBuilder {

    private IOpenClass type;
    private List<SpreadsheetCell> cells;

    public ArrayResultBuilder(List<SpreadsheetCell> notEmpty, IOpenClass type) {
        this.cells = notEmpty;
        this.type = type;
    }
    
    public Object makeResult(SpreadsheetResultCalculator resultCalculator) {
        
        int size = cells.size();
        IAggregateInfo aggregateInfo = type.getAggregateInfo();
        Object array = aggregateInfo.makeIndexedAggregate(aggregateInfo.getComponentType(type), size);

        IOpenIndex index = aggregateInfo.getIndex(type);
        for (int i = 0; i < size; ++i) {
        
            SpreadsheetCell cell = cells.get(i);
            Object value = resultCalculator.getValue(cell.getRowIndex(), cell.getColumnIndex());

            if (value == null) {
                value = type.nullObject();
            }

            index.setValue(array, i, value);
        }

        return array;
    }

}
