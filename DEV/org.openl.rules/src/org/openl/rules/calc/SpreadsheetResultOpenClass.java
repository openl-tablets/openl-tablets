package org.openl.rules.calc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

//Dont extend this class
public final class SpreadsheetResultOpenClass extends JavaOpenClass {
    private final IOpenField RESOLVING_IN_PROGRESS = new SpreadsheetResultField(this,
        "IN_PROGRESS",
        JavaOpenClass.OBJECT);

    private XlsModuleOpenClass module;
    private final Map<String, IOpenField> strictMatchCache = new HashMap<>();
    private final Map<String, IOpenField> noStrictMatchCache = new HashMap<>();
    private volatile CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass;

    public SpreadsheetResultOpenClass(Class<?> type) {
        super(type);
    }

    public SpreadsheetResultOpenClass(XlsModuleOpenClass module) {
        super(SpreadsheetResult.class);
        this.module = Objects.requireNonNull(module, "module cannot be null");
    }

    @Override
    public IOpenField getField(String fieldName, boolean strictMatch) {
        if (module == null) {
            return new SpreadsheetResultField(this, fieldName, JavaOpenClass.OBJECT);
        }
        IOpenField openField = null;
        if (strictMatchCache.containsKey(fieldName)) {
            openField = strictMatchCache.get(fieldName);
        }
        if (noStrictMatchCache.containsKey(fieldName.toLowerCase())) {
            openField = noStrictMatchCache.get(fieldName.toLowerCase());
        }
        if (openField != null && openField != RESOLVING_IN_PROGRESS) {
            return openField;
        }
        if (module.getRulesModuleBindingContext() == null) {
            return null;
        }
        if (openField == RESOLVING_IN_PROGRESS) {
            openField = new SpreadsheetResultField(this, fieldName, JavaOpenClass.OBJECT);
            if (strictMatch) {
                strictMatchCache.put(fieldName, openField);
            } else {
                noStrictMatchCache.put(fieldName.toLowerCase(), openField);
            }
        } else {
            if (strictMatch) {
                strictMatchCache.put(fieldName, RESOLVING_IN_PROGRESS);
            } else {
                noStrictMatchCache.put(fieldName.toLowerCase(), RESOLVING_IN_PROGRESS);
            }
            openField = super.getField(fieldName, strictMatch);
            boolean g = SpreadsheetStructureBuilder.preventCellsLoopingOnThis.get() == null;
            if (openField == null && fieldName.startsWith("$")) {
                if (module == null) {
                    openField = new SpreadsheetResultField(this, fieldName, JavaOpenClass.OBJECT);
                } else {
                    CustomSpreadsheetResultField mergedField = null;
                    for (IOpenClass openClass : module.getTypes()) {
                        if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                            try {
                                if (g) {
                                    SpreadsheetStructureBuilder.preventCellsLoopingOnThis.set(new Stack<>());
                                }
                                SpreadsheetStructureBuilder.preventCellsLoopingOnThis.get().push(new HashMap<>());
                                module.getRulesModuleBindingContext()
                                    .findType(ISyntaxConstants.THIS_NAMESPACE, openClass.getName());
                            } finally {
                                SpreadsheetStructureBuilder.preventCellsLoopingOnThis.get().pop();
                                if (g) {
                                    SpreadsheetStructureBuilder.preventCellsLoopingOnThis.remove();
                                }
                            }
                            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) openClass;
                            IOpenField f = customSpreadsheetResultOpenClass.getField(fieldName, strictMatch);
                            if (f instanceof CustomSpreadsheetResultField) {
                                if (mergedField == null) {
                                    mergedField = (CustomSpreadsheetResultField) f;
                                } else {
                                    mergedField = new CastingCustomSpreadsheetResultField(
                                        customSpreadsheetResultOpenClass,
                                        fieldName,
                                        (CustomSpreadsheetResultField) f,
                                        mergedField);
                                }
                            }
                        }
                    }
                    if (mergedField != null) {
                        try {
                            if (g) {
                                SpreadsheetStructureBuilder.preventCellsLoopingOnThis.set(new Stack<>());
                            }
                            SpreadsheetStructureBuilder.preventCellsLoopingOnThis.get().push(new HashMap<>());
                            mergedField.getType(); // Fires compilation
                            openField = mergedField;
                        } finally {
                            SpreadsheetStructureBuilder.preventCellsLoopingOnThis.get().pop();
                            if (g) {
                                SpreadsheetStructureBuilder.preventCellsLoopingOnThis.remove();
                            }
                        }
                    }
                }
            }
            if (strictMatch) {
                if (strictMatchCache.putIfAbsent(fieldName, openField) == RESOLVING_IN_PROGRESS) {
                    strictMatchCache.put(fieldName, openField);
                }
                return strictMatchCache.get(fieldName);
            } else {
                if (noStrictMatchCache.putIfAbsent(fieldName.toLowerCase(), openField) == RESOLVING_IN_PROGRESS) {
                    noStrictMatchCache.put(fieldName.toLowerCase(), openField);
                }
                return noStrictMatchCache.get(fieldName.toLowerCase());
            }
        }
        return openField;
    }

    public CustomSpreadsheetResultOpenClass toCustomSpreadsheetResultOpenClass() {
        if (this.customSpreadsheetResultOpenClass == null) {
            synchronized (this) {
                if (this.customSpreadsheetResultOpenClass == null) {
                    // HERE
                    CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = new CustomSpreadsheetResultOpenClass(
                        "AnySpreadsheetResult",
                        this.module,
                        null);
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

    @Override
    public IAggregateInfo getAggregateInfo() {
        if (module == null) {
            return super.getAggregateInfo();
        } else {
            return DynamicArrayAggregateInfo.aggregateInfo;
        }
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        // Only used for tests
        return new StubSpreadSheetResult();
    }

}
