package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.openl.binding.impl.CastToWiderType;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;

public class CastingCustomSpreadsheetResultField extends CustomSpreadsheetResultField {

    private List<Pair<IOpenClass, IOpenCast>> casts;
    private IOpenClass type;
    private CustomSpreadsheetResultField field1;
    private CustomSpreadsheetResultField field2;

    public CastingCustomSpreadsheetResultField(CustomSpreadsheetResultOpenClass declaringClass,
            String name,
            CustomSpreadsheetResultField field1,
            CustomSpreadsheetResultField field2) {
        super(declaringClass, name, null);
        this.field1 = Objects.requireNonNull(field1, "field1 cannot be null");
        this.field2 = Objects.requireNonNull(field2, "field2 cannot be null");
    }

    @Override
    public CustomSpreadsheetResultOpenClass getDeclaringClass() {
        return (CustomSpreadsheetResultOpenClass) super.getDeclaringClass();
    }

    @Override
    protected Object processResult(Object res) {
        if (this.type == null) {
            throw new IllegalStateException("Spreadsheet cell type is not resolved at compile time");
        }
        if (res == null) {
            return getType().nullObject();
        }
        for (Pair<IOpenClass, IOpenCast> cast : this.casts) {
            if (ClassUtils.isAssignable(res.getClass(), cast.getKey().getInstanceClass())) {
                return cast.getValue().convert(res);
            }
        }
        throw new IllegalStateException("This shouldn't happen");
    }

    private void initLazyFields() {
        if (this.type == null) {
            if (getDeclaringClass().getModule().getRulesModuleBindingContext() == null) {
                throw new IllegalStateException("Spreadsheet cell type is not resolved at compile time");
            }
            if (Objects.equals(field1.getType(), field2.getType())) {
                this.type = field1.getType();
            } else {
                CastToWiderType castToWiderType = CastToWiderType.create(getDeclaringClass().getModule()
                    .getRulesModuleBindingContext(), field1.getType(), field2.getType());
                this.type = castToWiderType.getWiderType();
            }
            Set<CustomSpreadsheetResultField> customSpreadsheetResultFields = new HashSet<>();
            extractAllTypes(this, customSpreadsheetResultFields);
            Set<IOpenClass> types = new HashSet<>();
            this.casts = new ArrayList<>();
            for (CustomSpreadsheetResultField f : customSpreadsheetResultFields) {
                if (!types.contains(f.getType())) {
                    IOpenCast cast = getDeclaringClass().getModule()
                        .getRulesModuleBindingContext()
                        .getCast(f.getType(), this.type);
                    types.add(f.getType());
                    this.casts.add(Pair.of(f.getType(), cast));
                }
            }
        }
    }

    private void extractAllTypes(CustomSpreadsheetResultField field,
            Set<CustomSpreadsheetResultField> customSpreadsheetResultFields) {
        if (field instanceof CastingCustomSpreadsheetResultField) {
            CastingCustomSpreadsheetResultField castingCustomSpreadsheetResultField = (CastingCustomSpreadsheetResultField) field;
            extractAllTypes(castingCustomSpreadsheetResultField.field1, customSpreadsheetResultFields);
            extractAllTypes(castingCustomSpreadsheetResultField.field2, customSpreadsheetResultFields);
        } else {
            customSpreadsheetResultFields.add(field);
        }
    }

    @Override
    public IOpenClass getType() {
        // Lazy compilation for recursive compilation
        initLazyFields();
        return type;
    }

}
