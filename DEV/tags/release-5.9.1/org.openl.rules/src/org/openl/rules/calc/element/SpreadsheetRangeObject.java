package org.openl.rules.calc.element;

import java.util.ArrayList;
import java.util.List;

import org.openl.meta.DoubleValue;
import org.openl.rules.calc.SpreadsheetResultCalculator;

public class SpreadsheetRangeObject {

    private final SpreadsheetResultCalculator calc;
    private final SpreadsheetCellField fstart;
    private final SpreadsheetCellField fend;

    public SpreadsheetRangeObject(SpreadsheetResultCalculator target,
            SpreadsheetCellField fstart,
            SpreadsheetCellField fend) {
                this.calc = target;
                this.fstart = fstart;
                this.fend = fend;
    }
    
    
    
    public static DoubleValue[] autocast(SpreadsheetRangeObject from, DoubleValue[] to)
    {
        int sx = from.fstart.getCell().getColumnIndex();
        int sy = from.fstart.getCell().getRowIndex();
        int ex = from.fend.getCell().getColumnIndex();
        int ey = from.fend.getCell().getRowIndex();
        
        int w = ex - sx + 1;
        int h = ey - sy + 1;
        
        int size = w * h;
        
        List<DoubleValue> list = new ArrayList<DoubleValue>(size);
        
        for(int x = 0; x < w; ++x)
            for(int y = 0; y < h; ++y)
            {
                Object v = from.calc.getValue(sy + y, sx + x);
                if (v == null || !(v instanceof DoubleValue))
                    continue;
                list.add((DoubleValue)v);
            }        
            
        return list.toArray(new DoubleValue[list.size()]);
        
    }

}
