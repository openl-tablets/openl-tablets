package org.openl.rules.calc.element;

import java.lang.reflect.Array;
import java.util.Objects;

import lombok.Getter;

import org.openl.binding.impl.NodeDescriptionHolder;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.calc.ASpreadsheetField;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetRangeField extends ASpreadsheetField implements NodeDescriptionHolder {

    /** First column index of the range within the spreadsheet. */
    @Getter
    private final int startColumnIndex;
    /** First row index of the range within the spreadsheet. */
    @Getter
    private final int startRowIndex;
    /** Last column index of the range within the spreadsheet, inclusive. */
    @Getter
    private final int endColumnIndex;
    /** Last row index of the range within the spreadsheet, inclusive. */
    @Getter
    private final int endRowIndex;
    private final IOpenCast[][] casts;
    private final Class<?> rangeType;
    private final String rangeName;

    public SpreadsheetRangeField(String name,
                                 String rangeName,
                                 int startColumnIndex,
                                 int startRowIndex,
                                 int endColumnIndex,
                                 int endRowIndex,
                                 IOpenClass rangeType,
                                 IOpenCast[][] casts,
                                 IOpenClass declaringClass) {
        super(declaringClass, name, rangeType.getArrayType(1));
        this.rangeName = Objects.requireNonNull(rangeName, "rangeName cannot be null");
        this.startColumnIndex = startColumnIndex;
        this.startRowIndex = startRowIndex;
        this.endColumnIndex = endColumnIndex;
        this.endRowIndex = endRowIndex;
        this.casts = casts;
        this.rangeType = rangeType.getInstanceClass();
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return getType().nullObject();
        }

        var w = endColumnIndex - startColumnIndex + 1;
        var h = endRowIndex - startRowIndex + 1;

        var size = w * h;

        var calc = (SpreadsheetResultCalculator) target;
        Object array = Array.newInstance(rangeType, size);
        var i = 0;
        for (var x = startColumnIndex; x <= endColumnIndex; ++x) {
            for (var y = startRowIndex; y <= endRowIndex; ++y) {
                var v = calc.getValue(y, x);
                var openCast = casts[x - startColumnIndex][y - startRowIndex];
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
}
