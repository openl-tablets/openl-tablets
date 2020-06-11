package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.openl.binding.impl.CastToWiderType;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.ClassUtils;

public class CastingCustomSpreadsheetResultField extends CustomSpreadsheetResultField {

    private List<Pair<IOpenClass, IOpenCast>> casts;
    private IOpenClass type;
    private final IOpenField field1;
    private final IOpenField field2;

    public CastingCustomSpreadsheetResultField(CustomSpreadsheetResultOpenClass declaringClass,
            String name,
            IOpenField field1,
            IOpenField field2) {
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
        if (this.casts != null) {
            for (Pair<IOpenClass, IOpenCast> cast : this.casts) {
                if (ClassUtils.isAssignable(res.getClass(), cast.getKey().getInstanceClass())) {
                    return cast.getValue().convert(res);
                }
            }
        }
        return res;
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
            Set<IOpenField> resultFields = new HashSet<>();
            extractAllTypes(this, resultFields);
            Set<IOpenClass> types = new HashSet<>();
            this.casts = new ArrayList<>();
            for (IOpenField f : resultFields) {
                if (!types.contains(f.getType())) {
                    IOpenCast cast = getDeclaringClass().getModule()
                        .getRulesModuleBindingContext()
                        .getCast(f.getType(), this.type);
                    types.add(f.getType());
                    this.casts.add(Pair.of(f.getType(), cast));
                }
            }
            if (types.size() == 1) {
                this.casts = null;
            }
        }
    }

    private void extractAllTypes(IOpenField field, Set<IOpenField> resultFields) {
        if (field instanceof CastingCustomSpreadsheetResultField) {
            CastingCustomSpreadsheetResultField castingCustomSpreadsheetResultField = (CastingCustomSpreadsheetResultField) field;
            extractAllTypes(castingCustomSpreadsheetResultField.field1, resultFields);
            extractAllTypes(castingCustomSpreadsheetResultField.field2, resultFields);
        } else {
            resultFields.add(field);
        }
    }

    @Override
    public IOpenClass getType() {
        // Lazy compilation for recursive compilation
        initLazyFields();
        return type;
    }

    @Override
    public IOpenClass[] getDeclaredClasses() {
        List<IOpenClass> declaredClasses = new ArrayList();
        getFieldDeclaredClasses(field1, declaredClasses);
        getFieldDeclaredClasses(field2, declaredClasses);
        return declaredClasses.toArray(new IOpenClass[] {});
    }

    private void getFieldDeclaredClasses(IOpenField field, List<IOpenClass> declaredClasses) {
        if (declaredClasses.contains(field.getDeclaringClass())) {
            return;
        }
        if (field instanceof IOriginalDeclaredClassesOpenField) {
            IOpenClass[] fieldDeclaredClasses = ((IOriginalDeclaredClassesOpenField) field).getDeclaredClasses();
            declaredClasses.addAll(Arrays.asList(fieldDeclaredClasses));
        } else if (field instanceof CustomSpreadsheetResultField && ((CustomSpreadsheetResultField) field).field != null) {
            CustomSpreadsheetResultField customSpreadsheetResultField = (CustomSpreadsheetResultField) field;
            declaredClasses.add(customSpreadsheetResultField.field.getDeclaringClass());
        } else {
            declaredClasses.add(field.getDeclaringClass());
        }
    }

}
