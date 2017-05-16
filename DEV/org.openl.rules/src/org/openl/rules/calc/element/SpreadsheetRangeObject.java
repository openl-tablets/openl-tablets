package org.openl.rules.calc.element;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.util.StringUtils;

public class SpreadsheetRangeObject {

    protected SpreadsheetResultCalculator calc;
    protected SpreadsheetCellField fstart;
    protected SpreadsheetCellField fend;
    protected IOpenCast[][] casts;
    
    public SpreadsheetRangeObject(SpreadsheetResultCalculator target,
            SpreadsheetCellField start,
            SpreadsheetCellField end, IOpenCast[][] casts) {
        this.calc = target;
        this.fstart = start;
        this.fend = end;
        this.casts = casts;
    }

    
    public Object test(){
        return this.casts;
    }
    
    // Do not change signature of this method. This method is used from
    // generated in runtime classes.
    public static Object cast(SpreadsheetRangeObject from, String componentType, IOpenCast[][] cast) {
        int sx = from.fstart.getCell().getColumnIndex();
        int sy = from.fstart.getCell().getRowIndex();
        int ex = from.fend.getCell().getColumnIndex();
        int ey = from.fend.getCell().getRowIndex();

        int w = ex - sx + 1;
        int h = ey - sy + 1;

        int size = w * h;

        List<Object> list = new ArrayList<Object>(size);
        try {
            Class<?> to = Thread.currentThread().getContextClassLoader().loadClass(componentType);
            for (int x = 0; x < w; ++x) {
                for (int y = 0; y < h; ++y) {
                    Object v = from.calc.getValue(sy + y, sx + x);
                    IOpenCast openCast = cast[x][y];
                    if (openCast != null && openCast.isImplicit()) {
                        v = openCast.convert(v);
                    }
                    list.add(v);
                }
            }

            Object array = Array.newInstance(to, list.size());
            int i = 0;
            for (Object v : list) {
                Array.set(array, i, v);
                i++;
            }
            return array;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public String toString() {
        int sx = fstart.getCell().getColumnIndex();
        int sy = fstart.getCell().getRowIndex();
        int ex = fend.getCell().getColumnIndex();
        int ey = fend.getCell().getRowIndex();

        int w = ex - sx + 1;
        int h = ey - sy + 1;

        int size = w * h;

        List<Object> list = new ArrayList<Object>(size);
        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                Object v = calc.getValue(sy + y, sx + x);
                IOpenCast openCast = casts[x][y];
                if (openCast != null && openCast.isImplicit()) {
                    v = openCast.convert(v);
                }
                list.add(v);
            }
        }

        return "[" + StringUtils.join(list, ",") + "]";
    }
}
