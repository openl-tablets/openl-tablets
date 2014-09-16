package org.openl.rules.calc;

import org.openl.binding.IBindingContext;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.exception.FieldNotFoundException;
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
    public IOpenField findRange(String namespace, String rangeStartName, String rangeEndName) throws AmbiguousVarException,
                                                                                             FieldNotFoundException,
                                                                                             OpenLCompilationException {

        String key = namespace + ":" + rangeStartName + ":" + rangeEndName;
        IOpenField fstart = findVar(namespace, rangeStartName, true);

        if (fstart == null) {
            throw new FieldNotFoundException("Can not find range start: ", rangeStartName, null);
        }

        IOpenField fend = findVar(namespace, rangeEndName, true);

        if (fend == null) {
            throw new FieldNotFoundException("Can not find range end: ", rangeEndName, null);
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

        IOpenCast[][] casts = new IOpenCast[w][h];

        IOpenClass rangeType = ((SpreadsheetCellField) fstart).getType();

        ComponentOpenClass componentOpenClass = getComponentOpenClass();
        ComponentBindingContext componentBindingContext = this;
        boolean implicitCastNotSupported = false;
        while (componentOpenClass != null) {
            for (IOpenField f : componentOpenClass.getDeclaredFields().values()) {
                if (f instanceof SpreadsheetCellField) {
                    SpreadsheetCellField field = (SpreadsheetCellField) f;
                    int x = field.getCell().getColumnIndex() - sx;
                    int y = field.getCell().getRowIndex() - sy;

                    if (x >= 0 && x < w && y >= 0 && y < h && casts[x][y] == null) {
                        if (!rangeType.equals(f.getType())){
                            casts[x][y] = getCast(f.getType(), rangeType);
                            if (!casts[x][y].isImplicit()){
                                casts[x][y] = null;
                            }
                            if (casts[x][y] == null) {
                                implicitCastNotSupported = true;
                            }
                        }
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

        if (implicitCastNotSupported) {
            throw new OpenLCompilationException("Types in range " + rangeStartName + ":" + rangeEndName + " can't be implicit casted to '" + rangeType.getDisplayName(0) + "'.");
        }

        IOpenField res = new SpreadsheetRangeField(key,
            (SpreadsheetCellField) fstart,
            (SpreadsheetCellField) fend,
            casts);

        return res;
    }

}
