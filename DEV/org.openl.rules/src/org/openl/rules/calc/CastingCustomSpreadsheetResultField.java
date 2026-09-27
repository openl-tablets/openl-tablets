package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import org.openl.binding.impl.CastToWiderType;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;

public class CastingCustomSpreadsheetResultField extends CustomSpreadsheetResultField {

    private List<Pair<IOpenClass, IOpenCast>> casts;
    private IOpenClass type;
    private final Collection<IOpenField> fields;
    private final IOpenClass[] declaringClasses;

    public CastingCustomSpreadsheetResultField(IOpenClass declaringClass,
                                               String name,
                                               IOpenField field1,
                                               IOpenField field2) {
        super(declaringClass, name, null);
        Objects.requireNonNull(field1, "field1 cannot be null");
        Objects.requireNonNull(field2, "field2 cannot be null");
        this.fields = new HashSet<>(extractFields(field1));
        this.fields.addAll(extractFields(field2));

        List<IOpenClass> combinedDeclaringClasses = new ArrayList<>();
        extractFieldDeclaringClasses(field1, combinedDeclaringClasses);
        extractFieldDeclaringClasses(field2, combinedDeclaringClasses);
        this.declaringClasses = combinedDeclaringClasses.toArray(IOpenClass.EMPTY);
    }

    private Collection<IOpenField> extractFields(IOpenField field) {
        Collection<IOpenField> ret = new ArrayList<>();
        if (field instanceof CastingCustomSpreadsheetResultField castingCustomSpreadsheetResultField) {
            ret.addAll(castingCustomSpreadsheetResultField.fields);
        } else {
            ret.add(field);
        }
        return ret;
    }

    private XlsModuleOpenClass getModule() {
        if (getDeclaringClass() instanceof CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
            return customSpreadsheetResultOpenClass.getModule();
        } else if (getDeclaringClass() instanceof SpreadsheetResultOpenClass spreadsheetResultOpenClass) {
            return spreadsheetResultOpenClass.getModule();
        }
        return null;
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
            return super.processResult(res);
        }
        return res;
    }

    private void initLazyFields() {
        if (this.type == null) {
            XlsModuleOpenClass xlsModuleOpenClass = getModule();
            if (xlsModuleOpenClass == null || xlsModuleOpenClass.getRulesModuleBindingContext() == null) {
                throw new IllegalStateException("Spreadsheet cell type is not resolved at compile time");
            }
            Set<IOpenClass> types = new HashSet<>();
            for (IOpenField f : fields) {
                types.add(f.getType());
            }
            if (types.size() == 1) {
                this.type = types.iterator().next();
                this.casts = null;
            } else if (isCustomSpreadsheetResultsOf(types, xlsModuleOpenClass)) {
                this.type = combineCustomSpreadsheetResults(types, xlsModuleOpenClass);
                this.casts = null;
            } else {
                initWiderTypeAndCasts(types, xlsModuleOpenClass);
            }
        }
    }

    /**
     * Checks whether all the types are custom spreadsheet results of the given module.
     */
    private static boolean isCustomSpreadsheetResultsOf(Set<IOpenClass> types, XlsModuleOpenClass xlsModuleOpenClass) {
        Set<XlsModuleOpenClass> modules = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IOpenClass openClass : types) {
            if (!(openClass instanceof CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass)) {
                return false;
            }
            modules.add(customSpreadsheetResultOpenClass.getModule());
        }
        return modules.size() == 1 && modules.iterator().next() == xlsModuleOpenClass;
    }

    private static IOpenClass combineCustomSpreadsheetResults(Set<IOpenClass> types,
                                                              XlsModuleOpenClass xlsModuleOpenClass) {
        Set<CustomSpreadsheetResultOpenClass> customSpreadsheetResultOpenClasses = types.stream()
                .map(CustomSpreadsheetResultOpenClass.class::cast)
                .collect(Collectors.toSet());
        if (customSpreadsheetResultOpenClasses.size() > 1) {
            return xlsModuleOpenClass.buildOrGetCombinedSpreadsheetResult(
                    customSpreadsheetResultOpenClasses.toArray(new CustomSpreadsheetResultOpenClass[0]));
        }
        return customSpreadsheetResultOpenClasses.iterator().next();
    }

    /**
     * Resolves the type as the widest of the given types, with a cast to it from each type that is not null.
     */
    private void initWiderTypeAndCasts(Set<IOpenClass> types, XlsModuleOpenClass xlsModuleOpenClass) {
        Iterator<IOpenClass> itr = types.iterator();
        IOpenClass t = itr.next();
        while (itr.hasNext()) {
            IOpenClass t1 = itr.next();
            CastToWiderType castToWiderType = CastToWiderType
                    .create(xlsModuleOpenClass.getRulesModuleBindingContext(), t, t1);
            t = castToWiderType.getWiderType();
        }
        this.casts = new ArrayList<>();
        this.type = t;
        for (IOpenClass fieldType : types) {
            if (!NullOpenClass.isAnyNull(fieldType)) {
                IOpenCast cast = xlsModuleOpenClass.getRulesModuleBindingContext()
                        .getCast(fieldType, this.type);
                IOpenClass x = fieldType;
                if (fieldType.getInstanceClass() != null && fieldType.getInstanceClass().isPrimitive()) {
                    x = JavaOpenClass
                            .getOpenClass(ClassUtils.primitiveToWrapper(fieldType.getInstanceClass()));
                }
                this.casts.add(Pair.of(x, cast));
            }
        }
        if (this.casts.isEmpty()) {
            this.casts = null;
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
        if (field instanceof IOriginalDeclaredClassesOpenField openField) {
            IOpenClass[] fieldDeclaringClasses = openField.getDeclaringClasses();
            declaringClasses.addAll(Arrays.asList(fieldDeclaringClasses));
        } else {
            declaringClasses.add(field.getDeclaringClass());
        }
    }

}
