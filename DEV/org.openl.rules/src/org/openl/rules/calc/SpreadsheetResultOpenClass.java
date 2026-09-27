package org.openl.rules.calc;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.impl.method.AOpenMethodDelegator;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
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
    private static final String ANY_SPREADSHEET_RESULT = "AnySpreadsheetResult";

    private final IOpenField resolvingInProgress = new SpreadsheetResultField(this,
            "IN_PROGRESS",
            JavaOpenClass.OBJECT);

    @Getter
    private XlsModuleOpenClass module;
    private final Map<String, IOpenField> strictMatchCache = new HashMap<>();
    private final Map<String, IOpenField> noStrictMatchCache = new HashMap<>();
    private final AtomicReference<CustomSpreadsheetResultOpenClass> customSpreadsheetResultOpenClass =
            new AtomicReference<>();
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
        return Set.of(AnySpreadsheetResultOpenClass.INSTANCE);
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
        if (openField != null && openField != resolvingInProgress) {
            return openField;
        }
        if (module != null && module.getRulesModuleBindingContext() == null) {
            return null;
        }
        if (openField == resolvingInProgress) {
            return getBlankField(fieldName, strictMatch);
        } else {
            return resolveField(fieldName, strictMatch);
        }
    }

    /**
     * Returns a field of the {@code Object} type for a field that is being resolved now.
     */
    private IOpenField getBlankField(String fieldName, boolean strictMatch) {
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
    }

    /**
     * Resolves a field and caches it. A field that is found in the cache after resolving takes precedence.
     */
    private IOpenField resolveField(String fieldName, boolean strictMatch) {
        if (strictMatch) {
            strictMatchCache.put(fieldName, resolvingInProgress);
        } else {
            noStrictMatchCache.put(fieldName.toLowerCase(), resolvingInProgress);
        }
        var openField = super.getField(fieldName, strictMatch);
        var g = SpreadsheetStructureBuilder.preventCellsLoopingOnThis.get() == null;
        if (openField == null && fieldName.startsWith("$")) {
            openField = findCellField(fieldName, strictMatch, g);
        }
        IOpenField f = strictMatch ? strictMatchCache.get(fieldName)
                : noStrictMatchCache.get(fieldName.toLowerCase());
        if (f == null || f == resolvingInProgress) {
            if (strictMatch) {
                strictMatchCache.put(fieldName, openField);
            } else {
                noStrictMatchCache.put(fieldName.toLowerCase(), openField);
            }
            return openField;
        }
        return f;
    }

    /**
     * Finds the cell field of the given name in the spreadsheets of the module and compiles it. The cell fields found
     * in several spreadsheets are merged into one field.
     *
     * <p>Without a module, a cell field of the {@code Object} type is created.
     *
     * @return the found cell field, or {@code null} if no spreadsheet of the module has it
     */
    private IOpenField findCellField(String fieldName, boolean strictMatch, boolean g) {
        if (module == null) {
            return new SpreadsheetResultField(this, fieldName, JavaOpenClass.OBJECT);
        }
        CustomSpreadsheetResultField mergedField = null;
        for (IOpenClass openClass : module.getTypes()) {
            if (openClass instanceof CustomSpreadsheetResultOpenClass spreadsheetType && spreadsheetType
                    .isSpreadsheet()) {
                try {
                    startCellsLoopingPrevention(g);
                    module.getRulesModuleBindingContext()
                            .findType(openClass.getName());
                } finally {
                    stopCellsLoopingPrevention(g);
                }
                var f = spreadsheetType.getField(fieldName, strictMatch);
                if (f instanceof CustomSpreadsheetResultField field) {
                    if (mergedField == null) {
                        mergedField = field;
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
            try {
                startCellsLoopingPrevention(g);
                mergedField.getType(); // Fires compilation
            } finally {
                stopCellsLoopingPrevention(g);
            }
        }
        return mergedField;
    }

    private static void startCellsLoopingPrevention(boolean g) {
        if (g) {
            SpreadsheetStructureBuilder.preventCellsLoopingOnThis.set(new Stack<>());
        }
        SpreadsheetStructureBuilder.preventCellsLoopingOnThis.get().push(new HashSet<>());
    }

    private static void stopCellsLoopingPrevention(boolean g) {
        SpreadsheetStructureBuilder.preventCellsLoopingOnThis.get().pop();
        if (g) {
            SpreadsheetStructureBuilder.preventCellsLoopingOnThis.remove();
        }
    }

    public CustomSpreadsheetResultOpenClass toCustomSpreadsheetResultOpenClass() {
        var result = customSpreadsheetResultOpenClass.get();
        if (result == null) {
            synchronized (this) {
                result = customSpreadsheetResultOpenClass.get();
                if (result == null) {
                    // HERE
                    var anySpreadsheetResult = new CustomAnySpreadsheetResultOpenClass(
                            getAnySpreadsheetResultName(),
                            this.module,
                            null,
                            false);
                    for (IOpenClass openClass : module.getTypes()) {
                        if (openClass instanceof CustomSpreadsheetResultOpenClass csrop
                                && customSpreadsheetResultOpenClass.get() == null) {
                            anySpreadsheetResult.updateWithType(csrop);
                        }
                    }
                    result = customSpreadsheetResultOpenClass.get();
                    if (result == null) {
                        result = anySpreadsheetResult;
                        customSpreadsheetResultOpenClass.set(result);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns the name of the AnySpreadsheetResult type, numbered when a type of the module has the name already.
     */
    private String getAnySpreadsheetResultName() {
        var anySpreadsheetResultName = ANY_SPREADSHEET_RESULT;
        var i = 0;
        var nameExists = this.module.getTypes()
                .stream()
                .anyMatch(t -> t.getName()
                        .equals(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + ANY_SPREADSHEET_RESULT));
        while (nameExists) {
            anySpreadsheetResultName = ANY_SPREADSHEET_RESULT + i++;
            var anySpreadsheetResultName0 = anySpreadsheetResultName;
            nameExists = this.module.getTypes()
                    .stream()
                    .anyMatch(t -> t.getName()
                            .equals(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + anySpreadsheetResultName0));
        }
        return anySpreadsheetResultName;
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        if (getModule() != null) {
            return toCustomSpreadsheetResultOpenClass().newInstance(env);
        } else {
            // Only used for tests
            return new StubSpreadSheetResult();
        }
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        if (ioc instanceof AnySpreadsheetResultOpenClass) {
            return false;
        }
        if (getModule() != null) {
            if (ioc instanceof SpreadsheetResultOpenClass class2) {
                return class2.getModule() == getModule();
            } else if (ioc instanceof CustomSpreadsheetResultOpenClass class1) {
                return class1.getModule() == getModule();
            }
        }
        return super.isAssignableFrom(ioc);
    }

    @Override
    public boolean isInstance(Object instance) {
        if (instance instanceof SpreadsheetResult spreadsheetResult) {
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
            public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
                return SpreadsheetResultOpenClass.this.newInstance(env);
            }

            @Override
            public IOpenClass getType() {
                return SpreadsheetResultOpenClass.this.getModule() == null
                        ? JavaOpenClass
                        .getOpenClass(SpreadsheetResult.class)
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

        var that = (SpreadsheetResultOpenClass) o;

        return Objects.equals(module, that.module);
    }

    @Override
    public int hashCode() {
        var result = super.hashCode();
        result = 31 * result + (module != null ? module.hashCode() : 0);
        return result;
    }
}
