package org.openl.rules.calc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.impl.method.AOpenMethodDelegator;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.vm.IRuntimeEnv;

// Do not extend this class
public final class SpreadsheetResultOpenClass extends JavaOpenClass {
    private final IOpenField RESOLVING_IN_PROGRESS = new SpreadsheetResultField(this,
        "IN_PROGRESS",
        JavaOpenClass.OBJECT);

    private XlsModuleOpenClass module;
    private final Map<String, IOpenField> strictMatchCache = new HashMap<>();
    private final Map<String, IOpenField> noStrictMatchCache = new HashMap<>();
    private volatile CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass;
    private final Map<String, IOpenField> strictBlankCache = new HashMap<>();
    private final Map<String, IOpenField> noStrictBlankCache = new HashMap<>();

    public SpreadsheetResultOpenClass(Class<?> type) {
        super(SpreadsheetResult.class);
    }

    public SpreadsheetResultOpenClass(XlsModuleOpenClass module) {
        super(SpreadsheetResult.class);
        this.module = Objects.requireNonNull(module, "module cannot be null");
    }

    @Override
    public Collection<IOpenClass> superClasses() {
        return Collections.singleton(AnySpreadsheetResultOpenClass.INSTANCE);
    }

    @Override
    protected IOpenField searchFieldFromSuperClass(String fname, boolean strictMatch) throws AmbiguousFieldException {
        return null;
    }

    @Override
    public IOpenField getField(String fieldName, boolean strictMatch) {
        IOpenField openField = null;
        if (strictMatch && strictMatchCache.containsKey(fieldName)) {
            openField = strictMatchCache.get(fieldName);
        }
        if (!strictMatch && noStrictMatchCache.containsKey(fieldName.toLowerCase())) {
            openField = noStrictMatchCache.get(fieldName.toLowerCase());
        }
        if (openField != null && openField != RESOLVING_IN_PROGRESS) {
            return openField;
        }
        if (module != null && module.getRulesModuleBindingContext() == null) {
            return null;
        }
        if (openField == RESOLVING_IN_PROGRESS) {
            IOpenField f = strictMatch ? strictBlankCache.get(fieldName)
                                       : noStrictBlankCache.get(fieldName.toLowerCase());
            if (f == null) {
                f = new SpreadsheetResultField(this,
                    strictMatch ? fieldName : fieldName.toLowerCase(),
                    JavaOpenClass.OBJECT);
                if (strictMatch) {
                    strictBlankCache.put(fieldName, f);
                } else {
                    noStrictBlankCache.put(fieldName.toLowerCase(), f);
                }
            }
            return f;
        } else {
            if (strictMatch) {
                strictMatchCache.put(fieldName, RESOLVING_IN_PROGRESS);
            } else {
                noStrictMatchCache.put(fieldName.toLowerCase(), RESOLVING_IN_PROGRESS);
            }
            openField = super.getField(fieldName, strictMatch);

            if (openField == null && fieldName.startsWith("$")) {
                if (module == null) {
                    openField = new SpreadsheetResultField(this, fieldName, JavaOpenClass.OBJECT);
                } else {
                    CustomSpreadsheetResultField mergedField = null;
                    for (IOpenClass openClass : module.getTypes()) {
                        if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                            module.getRulesModuleBindingContext()
                                .findType(ISyntaxConstants.THIS_NAMESPACE, openClass.getName());
                            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) openClass;
                            IOpenField f = customSpreadsheetResultOpenClass.getField(fieldName, strictMatch);
                            if (f instanceof CustomSpreadsheetResultField) {
                                if (mergedField == null) {
                                    mergedField = (CustomSpreadsheetResultField) f;
                                } else {
                                    mergedField = new CastingCustomSpreadsheetResultField(this,
                                        fieldName,
                                        f,
                                        mergedField);
                                }
                            }
                        }
                    }
                    if (mergedField != null) {
                        mergedField.getType(); // Fires compilation
                        openField = mergedField;
                    }
                }
            }
            IOpenField f = strictMatch ? strictMatchCache.get(fieldName)
                                       : noStrictMatchCache.get(fieldName.toLowerCase());
            if (f == null || f == RESOLVING_IN_PROGRESS) {
                if (strictMatch) {
                    strictMatchCache.put(fieldName, openField);
                } else {
                    noStrictMatchCache.put(fieldName.toLowerCase(), openField);
                }
                return openField;
            }
            return f;
        }
    }

    public CustomSpreadsheetResultOpenClass toCustomSpreadsheetResultOpenClass() {
        if (this.customSpreadsheetResultOpenClass == null) {
            synchronized (this) {
                if (this.customSpreadsheetResultOpenClass == null) {
                    // HERE
                    String anySpreadsheetResultName = "AnySpreadsheetResult";
                    int i = 0;
                    boolean nameExists = this.module.getTypes()
                        .stream()
                        .anyMatch(t -> t.getName()
                            .equals(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + "AnySpreadsheetResult"));
                    while (nameExists) {
                        anySpreadsheetResultName = "AnySpreadsheetResult" + i++;
                        String anySpreadsheetResultName0 = anySpreadsheetResultName;
                        nameExists = this.module.getTypes()
                            .stream()
                            .anyMatch(t -> t.getName()
                                .equals(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + anySpreadsheetResultName0));
                    }
                    CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = new CustomSpreadsheetResultOpenClass(
                        anySpreadsheetResultName,
                        this.module,
                        null,
                        true);
                    for (IOpenClass openClass : module.getTypes()) {
                        if (openClass instanceof CustomSpreadsheetResultOpenClass && this.customSpreadsheetResultOpenClass == null) {
                            CustomSpreadsheetResultOpenClass csrop = (CustomSpreadsheetResultOpenClass) openClass;
                            customSpreadsheetResultOpenClass.updateWithType(csrop);
                        }
                    }
                    if (this.customSpreadsheetResultOpenClass == null) {
                        this.customSpreadsheetResultOpenClass = customSpreadsheetResultOpenClass;
                    }
                }
            }
        }
        return this.customSpreadsheetResultOpenClass;
    }

    public XlsModuleOpenClass getModule() {
        return module;
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        // Only used for tests
        return new StubSpreadSheetResult();
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        if (ioc instanceof AnySpreadsheetResultOpenClass) {
            return false;
        }
        if (getModule() != null) {
            if (ioc instanceof SpreadsheetResultOpenClass) {
                return ((SpreadsheetResultOpenClass) ioc).getModule() == getModule();
            } else if (ioc instanceof CustomSpreadsheetResultOpenClass) {
                return ((CustomSpreadsheetResultOpenClass) ioc).getModule() == getModule();
            }
        }
        return super.isAssignableFrom(ioc);
    }

    @Override
    public boolean isInstance(Object instance) {
        if (instance instanceof SpreadsheetResult) {
            SpreadsheetResult spreadsheetResult = (SpreadsheetResult) instance;
            if (getModule() == null) {
                return spreadsheetResult.getCustomSpreadsheetResultOpenClass() == null;
            } else {
                return spreadsheetResult.getCustomSpreadsheetResultOpenClass() == toCustomSpreadsheetResultOpenClass();
            }
        }
        return false;
    }

    @Override
    protected IOpenMethod processConstructor(JavaOpenConstructor constructor) {
        return new AOpenMethodDelegator(super.processConstructor(constructor)) {
            @Override
            public IOpenClass getType() {
                return SpreadsheetResultOpenClass.this.getModule() == null ? AnySpreadsheetResultOpenClass.INSTANCE
                                                                           : SpreadsheetResultOpenClass.this;
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        SpreadsheetResultOpenClass that = (SpreadsheetResultOpenClass) o;

        return Objects.equals(module, that.module);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (module != null ? module.hashCode() : 0);
        return result;
    }
}
