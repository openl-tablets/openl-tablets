package org.openl.rules.calc.element;

import java.lang.reflect.Array;
import java.util.Objects;

import org.openl.binding.impl.NodeDescriptionHolder;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.calc.ASpreadsheetField;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetRangeField extends ASpreadsheetField implements NodeDescriptionHolder {

    private final int sx;
    private final int sy;
    private final int ex;
    private final int ey;
    private final IOpenCast[][] casts;
    private final Class<?> rangeType;
    private final String rangeName;

    public SpreadsheetRangeField(String name,
                                 String rangeName,
                                 int sx,
                                 int sy,
                                 int ex,
                                 int ey,
                                 IOpenClass rangeType,
                                 IOpenCast[][] casts,
                                 IOpenClass declaringClass) {
        super(declaringClass, name, rangeType.getArrayType(1));
        this.rangeName = Objects.requireNonNull(rangeName, "rangeName cannot be null");
        this.sx = sx;
        this.sy = sy;
        this.ex = ex;
        this.ey = ey;
        this.casts = casts;
        this.rangeType = rangeType.getInstanceClass();
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return getType().nullObject();
        }

        var w = ex - sx + 1;
        var h = ey - sy + 1;

        var size = w * h;

        var calc = (SpreadsheetResultCalculator) target;
        Object array = Array.newInstance(rangeType, size);
        var i = 0;
        for (var x = sx; x <= ex; ++x) {
            for (var y = sy; y <= ey; ++y) {
                var v = calc.getValue(y, x);
                var openCast = casts[x - sx][y - sy];
                if (openCast != null && openCast.isImplicit()) {
                    v = openCast.convert(v);
                }
                Array.set(array, i, v);
                i++;
            }
        }

        return array;
    }

    @Override
    public String getDescription() {
        return getType().getDisplayName(SHORT) + " " + rangeName;
    }

    /** First column index of the range within the spreadsheet. */
    public int getStartColumnIndex() {
        return sx;
    }

    /** First row index of the range within the spreadsheet. */
    public int getStartRowIndex() {
        return sy;
    }

    /** Last column index of the range within the spreadsheet, inclusive. */
    public int getEndColumnIndex() {
        return ex;
    }

    /** Last row index of the range within the spreadsheet, inclusive. */
    public int getEndRowIndex() {
        return ey;
    }
}
