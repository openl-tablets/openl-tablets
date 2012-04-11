package org.openl.rules.calc;

import java.lang.reflect.Array;
import java.util.List;

import org.openl.types.IOpenClass;

public class ArrayResultBuilder implements IResultBuilder {

    List<SCell> cells;
    IOpenClass type;

    public ArrayResultBuilder(List<SCell> notEmpty, IOpenClass type) {
        cells = notEmpty;
        this.type = type;
    }

    public Object makeResult(SpreadsheetResult res) {
        int size = cells.size();
        Object ary = type.getAggregateInfo().makeIndexedAggregate(type, new int[] { size });

        for (int i = 0; i < size; ++i) {
            SCell cell = cells.get(i);

            Object value = res.getValue(cell.getRow(), cell.getColumn());

            if (value == null) {
                value = type.nullObject();
            }

            Array.set(ary, i, value);
        }

        return ary;
    }

}
