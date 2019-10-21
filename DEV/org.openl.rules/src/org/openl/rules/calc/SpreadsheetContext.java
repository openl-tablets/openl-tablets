package org.openl.rules.calc;

import org.openl.binding.IBindingContext;
import org.openl.binding.exception.FieldNotFoundException;
import org.openl.binding.impl.CastToWiderType;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.component.ComponentBindingContext;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.rules.calc.element.SpreadsheetRangeField;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class SpreadsheetContext extends ComponentBindingContext {

    public SpreadsheetContext(IBindingContext delegate, ComponentOpenClass type) {
        super(delegate, type);
    }

    @Override
    public IOpenField findRange(String namespace,
            String rangeStartName,
            String rangeEndName) throws OpenLCompilationException {

        String key = namespace + ":" + rangeStartName + ":" + rangeEndName;
        IOpenField fstart = findVar(namespace, rangeStartName, true);

        if (fstart == null) {
            throw new FieldNotFoundException("Cannot find range start: ", rangeStartName, null);
        }

        IOpenField fend = findVar(namespace, rangeEndName, true);

        if (fend == null) {
            throw new FieldNotFoundException("Cannot find range end: ", rangeEndName, null);
        }

        if (!(fstart instanceof SpreadsheetCellField)) {
            throw new FieldNotFoundException("Range start must point to the cell: ", rangeStartName, null);
        }

        if (!(fend instanceof SpreadsheetCellField)) {
            throw new FieldNotFoundException("Range end must point to the cell: ", rangeEndName, null);
        }

        int sx = ((SpreadsheetCellField) fstart).getCell().getColumnIndex();
        int sy = ((SpreadsheetCellField) fstart).getCell().getRowIndex();
        int ex = ((SpreadsheetCellField) fend).getCell().getColumnIndex();
        int ey = ((SpreadsheetCellField) fend).getCell().getRowIndex();

        int w = ex - sx + 1;
        int h = ey - sy + 1;

        RangeTypeCollector rangeTypeCollector = new RangeTypeCollector(fstart.getType());
        iterateThroughTheRange(sx, sy, w, h, rangeTypeCollector);
        IOpenClass rangeType = rangeTypeCollector.getRangeType();

        CastsCollector castsCollector = new CastsCollector(rangeType, w, h);
        iterateThroughTheRange(sx, sy, w, h, castsCollector);

        if (castsCollector.isImplicitCastNotSupported()) {
            throw new OpenLCompilationException(
                "Types in range " + rangeStartName + ":" + rangeEndName + " cannot be implicit casted to '" + rangeType
                    .getDisplayName(0) + "'.");
        }

        return new SpreadsheetRangeField(key,
            (SpreadsheetCellField) fstart,
            (SpreadsheetCellField) fend,
            rangeType,
            castsCollector.getCasts());
    }

    private void iterateThroughTheRange(int startColumn,
            int startRow,
            int columnsInRange,
            int rowsInRange,
            SpreadsheetFieldCollector collector) {
        ComponentOpenClass componentOpenClass = getComponentOpenClass();
        ComponentBindingContext componentBindingContext = this;
        while (componentOpenClass != null) {
            for (IOpenField f : componentOpenClass.getDeclaredFields().values()) {
                if (f instanceof SpreadsheetCellField) {
                    SpreadsheetCellField field = (SpreadsheetCellField) f;
                    int columnInRange = field.getCell().getColumnIndex() - startColumn;
                    int rowInRange = field.getCell().getRowIndex() - startRow;

                    if (columnInRange >= 0 && columnInRange < columnsInRange && rowInRange >= 0 && rowInRange < rowsInRange) {
                        collector.collect(columnInRange, rowInRange, field);
                    }
                }
            }
            if (componentBindingContext.getDelegate() instanceof ComponentBindingContext) {
                componentBindingContext = (ComponentBindingContext) componentBindingContext.getDelegate();
                componentOpenClass = componentBindingContext.getComponentOpenClass();
            } else {
                componentOpenClass = null;
            }
        }
    }

    private interface SpreadsheetFieldCollector {
        void collect(int columnInRange, int rowInRange, SpreadsheetCellField field);
    }

    private class CastsCollector implements SpreadsheetFieldCollector {
        private final IOpenClass rangeType;
        private final IOpenCast[][] casts;
        private boolean implicitCastNotSupported = false;

        private CastsCollector(IOpenClass rangeType, int columnsInRange, int rowsInRange) {
            this.rangeType = rangeType;
            this.casts = new IOpenCast[columnsInRange][rowsInRange];
        }

        @Override
        public void collect(int columnInRange, int rowInRange, SpreadsheetCellField field) {
            if (casts[columnInRange][rowInRange] == null && !rangeType.equals(field.getType())) {
                casts[columnInRange][rowInRange] = getCast(field.getType(), rangeType);
                if (!casts[columnInRange][rowInRange].isImplicit()) {
                    casts[columnInRange][rowInRange] = null;
                }
                if (casts[columnInRange][rowInRange] == null) {
                    implicitCastNotSupported = true;
                }
            }
        }

        IOpenCast[][] getCasts() {
            return casts;
        }

        boolean isImplicitCastNotSupported() {
            return implicitCastNotSupported;
        }
    }

    private class RangeTypeCollector implements SpreadsheetFieldCollector {
        private IOpenClass rangeType;

        private RangeTypeCollector(IOpenClass initialRangeType) {
            this.rangeType = initialRangeType;
        }

        @Override
        public void collect(int columnInRange, int rowInRange, SpreadsheetCellField field) {
            rangeType = CastToWiderType.create(SpreadsheetContext.this, rangeType, field.getType()).getWiderType();
        }

        IOpenClass getRangeType() {
            return rangeType;
        }
    }
}
