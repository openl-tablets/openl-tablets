package org.openl.rules.calc;

import java.util.Objects;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;

public class CastingCustomSpreadsheetResultField extends CustomSpreadsheetResultField {

    private CustomSpreadsheetResultField field1;
    private IOpenCast cast1;
    private CustomSpreadsheetResultField field2;
    private IOpenCast cast2;

    public CastingCustomSpreadsheetResultField(IOpenClass declaringClass,
            String name,
            CustomSpreadsheetResultField field1,
            IOpenCast cast1,
            CustomSpreadsheetResultField field2,
            IOpenCast cast2,
            IOpenClass type) {
        super(declaringClass, name, type);
        this.field1 = Objects.requireNonNull(field1);
        this.cast1 = cast1;

        this.field2 = Objects.requireNonNull(field2);
        this.cast2 = cast2;
    }

    @Override
    protected Object processResult(Object res) {
        if (field1.getType().getInstanceClass() != null && ClassUtils.isAssignable(res.getClass(),
            field1.getType().getInstanceClass())) {
            return cast1 != null ? cast1.convert(res) : res;
        } else {
            res = field2.processResult(res);
            return cast2 != null ? cast2.convert(res) : res;
        }
    }

}
