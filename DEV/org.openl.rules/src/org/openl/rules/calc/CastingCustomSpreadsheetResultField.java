package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
    private final Collection<IOpenField> fields;
    private final IOpenClass[] declaringClasses;

    public CastingCustomSpreadsheetResultField(CustomSpreadsheetResultOpenClass declaringClass,
            String name,
            IOpenField field1,
            IOpenField field2) {
        super(declaringClass, name, null);
        Objects.requireNonNull(field1, "field1 cannot be null");
        Objects.requireNonNull(field2, "field2 cannot be null");
        this.fields = new ArrayList<>(extractFields(field1));
        this.fields.addAll(extractFields(field2));

        List<IOpenClass> declaringClasses = new ArrayList<>();
        extractFieldDeclaringClasses(field1, declaringClasses);
        extractFieldDeclaringClasses(field2, declaringClasses);
        this.declaringClasses = declaringClasses.toArray(IOpenClass.EMPTY);
    }

    private Collection<IOpenField> extractFields(IOpenField field) {
        Collection<IOpenField> ret = new ArrayList<>();
        if (field instanceof CastingCustomSpreadsheetResultField) {
            CastingCustomSpreadsheetResultField castingCustomSpreadsheetResultField = (CastingCustomSpreadsheetResultField) field;
            ret.addAll(castingCustomSpreadsheetResultField.fields);
        } else {
            ret.add(field);
        }
        return ret;
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
        if (!ClassUtils.isAssignable(res.getClass(), getType().getInstanceClass())) {
            return convertWithFailSafeCast(res);
        }
        return res;
    }

    private void initLazyFields() {
        if (this.type == null) {
            if (getDeclaringClass().getModule().getRulesModuleBindingContext() == null) {
                throw new IllegalStateException("Spreadsheet cell type is not resolved at compile time");
            }
            Set<IOpenClass> types = new HashSet<>();
            for (IOpenField f : fields) {
                types.add(f.getType());
            }
            if (types.size() == 1) {
                this.type = types.iterator().next();
                this.casts = null;
            } else {
                Iterator<IOpenClass> itr = types.iterator();
                IOpenClass t = itr.next();
                while (itr.hasNext()) {
                    IOpenClass t1 = itr.next();
                    CastToWiderType castToWiderType = CastToWiderType
                        .create(getDeclaringClass().getModule().getRulesModuleBindingContext(), t, t1);
                    t = castToWiderType.getWiderType();
                }
                this.casts = new ArrayList<>();
                this.type = t;
                for (IOpenClass type : types) {
                    IOpenCast cast = getDeclaringClass().getModule()
                        .getRulesModuleBindingContext()
                        .getCast(type, this.type);
                    this.casts.add(Pair.of(type, cast));
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

    @Override
    public IOpenClass[] getDeclaringClasses() {
        return declaringClasses.clone();
    }

    private void extractFieldDeclaringClasses(IOpenField field, List<IOpenClass> declaringClasses) {
        if (declaringClasses.contains(field.getDeclaringClass())) {
            return;
        }
        if (field instanceof IOriginalDeclaredClassesOpenField) {
            IOpenClass[] fieldDeclaringClasses = ((IOriginalDeclaredClassesOpenField) field).getDeclaringClasses();
            declaringClasses.addAll(Arrays.asList(fieldDeclaringClasses));
        } else {
            declaringClasses.add(field.getDeclaringClass());
        }
    }

}
