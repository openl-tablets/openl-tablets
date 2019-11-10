package org.openl.rules.calc;

import java.util.Objects;

import org.openl.binding.impl.CastToWiderType;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;

public class CastingCustomSpreadsheetResultField extends CustomSpreadsheetResultField {

    private CustomSpreadsheetResultField field1;
    private CustomSpreadsheetResultField field2;
    private volatile IOpenCast cast1;
    private volatile IOpenCast cast2;
    private volatile IOpenClass type;

    public CastingCustomSpreadsheetResultField(XlsModuleOpenClass declaringClass,
            String name,
            CustomSpreadsheetResultField field1,
            CustomSpreadsheetResultField field2) {
        super(declaringClass, name, null);
        this.field1 = Objects.requireNonNull(field1, "field1 cannot be null");
        this.field2 = Objects.requireNonNull(field2, "field2 cannot be null");
    }

    @Override
    public XlsModuleOpenClass getDeclaringClass() {
        return (XlsModuleOpenClass) super.getDeclaringClass();
    }

    @Override
    protected Object processResult(Object res) {
        initLazyFields();
        if (this.cast1 == null && this.cast2 == null) {
            return res;
        }
        if (field1.getType().getInstanceClass() != null && ClassUtils.isAssignable(res.getClass(),
            field1.getType().getInstanceClass())) {
            return cast1 != null ? cast1.convert(res) : res;
        } else {
            res = field2.processResult(res);
            return cast2 != null ? cast2.convert(res) : res;
        }
    }

    protected void initLazyFields() {
        if (this.type == null) {
            synchronized (this) {
                if (this.type == null) {
                    if (Objects.equals(field1.getType(), field2.getType())) {
                        this.type = field1.getType();
                    } else {
                        CastToWiderType castToWiderType = CastToWiderType.create(getDeclaringClass()
                            .getRulesModuleBindingContext(), field1.getType(), field2.getType());
                        this.cast1 = castToWiderType.getCast1();
                        this.cast2 = castToWiderType.getCast2();
                        this.type = castToWiderType.getWiderType();
                    }
                }
            }
        }
    }

    @Override
    public IOpenClass getType() {
        // Lazy compilation for recursive compilation
        initLazyFields();
        return type;
    }

}
