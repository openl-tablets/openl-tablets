package org.openl.rules.binding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import org.openl.binding.IBindingContext;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.DuplicatedTypeException;
import org.openl.binding.exception.TypesCombinationNotSupportedException;
import org.openl.binding.impl.method.AOpenMethodDelegator;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.binding.impl.module.ModuleSpecificType;
import org.openl.meta.TableMetaInfo;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.data.DataOpenField;
import org.openl.rules.data.IDataBase;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.util.MessageUtils;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleRuntimeEnv;

/**
 * Binding context for xls rules.
 *
 * @author DLiauchuk
 */
public class RulesModuleBindingContext extends ModuleBindingContext {
    private static final String CIRCULAR_REFERENCE = "Type '%s' compilation failed with circular reference issue.";

    public static final String GLOBAL_PROPERTIES_KEY = "Properties:Global";
    public static final String MODULE_PROPERTIES_KEY = "Properties:Module";
    public static final String CATEGORY_PROPERTIES_KEY = "Properties:Category:";
    private final Map<String, TableSyntaxNode> bindedTables = new HashMap<>();
    private final Map<String, IOpenField> internalVariables = new HashMap<>();
    private final Map<String, IOpenField> noStrictInternalVariables = new HashMap<>();

    /**
     * Internal OpenL service methods.
     */
    private final List<IOpenMethod> internalMethods;

    private final PreBinderMethods preBinderMethods = new PreBinderMethods();

    /** The spreadsheet result type whose tables bind the bodies that build it, none while other tables are bound. */
    private @Nullable CustomSpreadsheetResultOpenClass typeInBinding;

    /** The tables of the spreadsheet result types being built, by their syntax nodes. */
    private final Map<ISyntaxNode, RecursiveOpenMethodPreBinder> tablesInBinding = new IdentityHashMap<>();

    private final Map<String, Runnable> dataTableBinders = new HashMap<>();

    @Getter(AccessLevel.PRIVATE)
    @Setter
    private boolean ignoreCustomSpreadsheetResultCompilation;

    public RulesModuleBindingContext(IBindingContext delegate, XlsModuleOpenClass module) {
        this(delegate, module, true);
    }

    /**
     * A context binding the tables of a module.
     *
     * @param delegate          the context this one delegates to
     * @param module            the module the tables are bound in
     * @param keepsMessageCalls whether the module keeps the calls of {@code msg} the tables write a key in; a table
     *                          bound alone against a module compiled before leaves the module as it was
     */
    protected RulesModuleBindingContext(IBindingContext delegate,
                                        XlsModuleOpenClass module,
                                        boolean keepsMessageCalls) {
        super(delegate, module);
        internalMethods = new ArrayList<>();
        internalMethods.add(new CurrentRuntimeContextMethod());
        internalMethods.add(new EmptyRuntimeContextMethod());
        internalMethods.add(new RestoreRuntimeContextMethod());
        internalMethods.add(new SetRuntimeContextMethod());
        internalMethods.add(new ModifyRuntimeContextMethod());
        internalMethods.add(new MessageSourceResourceMethod(module, keepsMessageCalls));

        var tablePropertiesOpenField = new TablePropertiesOpenField();
        internalVariables.put(tablePropertiesOpenField.getName(), tablePropertiesOpenField);
        noStrictInternalVariables.put(tablePropertiesOpenField.getName().toLowerCase(), tablePropertiesOpenField);
        var dispatchingTablePropertiesOpenField = new DispatchingTablePropertiesOpenField();
        internalVariables.put(dispatchingTablePropertiesOpenField.getName(), dispatchingTablePropertiesOpenField);
        noStrictInternalVariables.put(dispatchingTablePropertiesOpenField.getName().toLowerCase(),
                dispatchingTablePropertiesOpenField);
    }

    /**
     * Registers the tsn by specified key.
     *
     * @param key Key that have to be same for equivalent tables.
     * @param tsn TableSyntaxNode to register.
     */
    public void registerTableSyntaxNode(String key, TableSyntaxNode tsn) {
        this.bindedTables.put(key, tsn);
    }

    /**
     * @return <code>true</code> if key TableSyntaxNode with specified key has already been registered.
     */
    public boolean isTableSyntaxNodePresented(String key) {
        return this.bindedTables.containsKey(key);
    }

    public @Nullable TableSyntaxNode getTableSyntaxNode(String key) {
        return this.bindedTables.get(key);
    }

    @Override
    public @Nullable IOpenField findVar(String namespace, String name, boolean strictMatch)
            throws AmbiguousFieldException {
        var openField = super.findVar(namespace, name, strictMatch);
        if (openField instanceof DataOpenField dataField && !dataTableBinders.isEmpty()) {
            var binder = dataTableBinders.remove(dataField.getUri());
            if (binder != null) {
                binder.run();
                openField = super.findVar(namespace, name, strictMatch);
            }
        }
        if (openField != null) {
            return openField;
        }
        if (strictMatch) {
            return internalVariables.get(name);
        }
        if (name != null) {
            return noStrictInternalVariables.get(name.toLowerCase());
        }
        return null;
    }

    @Override
    public @Nullable IMethodCaller findMethodCaller(String namespace, final String methodName, IOpenClass[] parTypes) {
        var select = CollectionUtils.findAll(
                preBinderMethods.values().stream().map(IOpenMethod.class::cast).toList(),
                e -> Objects.equals(methodName, e.getName()));
        IMethodCaller method;
        try {
            method = MethodSearch.findMethod(methodName, parTypes, this, select, true);
            if (method != null) {
                var openMethodBinder = extractOpenMethodPrebinder(method);
                if (openMethodBinder.isPreBindStarted()) {
                    return findMethodInBinding(namespace, methodName, parTypes, openMethodBinder);
                }
                preBindMethod(openMethodBinder.getHeader());
            }
        } catch (AmbiguousMethodException e) {
            e.getMatchingMethods()
                    .stream()
                    .map(m -> extractOpenMethodPrebinder(m).getHeader())
                    .forEach(this::preBindMethod);
        }
        return checkTableInBinding(findAddedMethod(namespace, methodName, parTypes));
    }

    /**
     * Checks a call that reaches a table being bound by another name, such as the id of the table, as a call by its
     * name is checked.
     */
    private @Nullable IMethodCaller checkTableInBinding(@Nullable IMethodCaller method) {
        if (!tablesInBinding.isEmpty() && method != null && method.getMethod() instanceof ExecutableRulesMethod table) {
            var openMethodBinder = tablesInBinding.get(table.getInfo().getSyntaxNode());
            if (openMethodBinder != null && !isTypeInBinding(openMethodBinder)) {
                throw circularReference(openMethodBinder);
            }
        }
        return method;
    }

    /**
     * Finds a method whose table is being bound, for a call written in a table bound meanwhile.
     *
     * <p>A table whose return type is a spreadsheet result built from its own body may call itself: the call takes the
     * type being built, which adds nothing to it. Every table of that type is in the module by then, so a versioned
     * table calls its dispatcher.
     *
     * <p>Any other call of such a table is a circular reference: a table bound meanwhile would take the type before
     * it has all its steps.
     */
    private IMethodCaller findMethodInBinding(String namespace,
                                              String methodName,
                                              IOpenClass[] parTypes,
                                              RecursiveOpenMethodPreBinder openMethodBinder) {
        var allowed = !openMethodBinder.isSpreadsheetWithCustomSpreadsheetResult()
                || isTypeInBinding(openMethodBinder);
        var method = allowed ? findAddedMethod(namespace, methodName, parTypes) : null;
        if (method == null) {
            throw circularReference(openMethodBinder);
        }
        return method;
    }

    /** Whether the table builds the spreadsheet result type whose bodies are bound now. */
    private boolean isTypeInBinding(RecursiveOpenMethodPreBinder openMethodBinder) {
        return typeInBinding != null && typeInBinding == openMethodBinder.getCustomSpreadsheetResultOpenClass();
    }

    /** The error of a call that reaches a table being bound, which would take its type before it is complete. */
    private static RuntimeException circularReference(RecursiveOpenMethodPreBinder openMethodBinder) {
        var type = openMethodBinder.getCustomSpreadsheetResultOpenClass();
        if (type == null) {
            return new IllegalStateException(
                    "Method '%s' compilation failed with circular reference issue.".formatted(openMethodBinder.getName()));
        }
        return new RecursiveSpreadsheetMethodPreBindingException(CIRCULAR_REFERENCE.formatted(type.getName()));
    }

    /** Finds a method the module or the internal methods of OpenL already have. */
    private @Nullable IMethodCaller findAddedMethod(String namespace, String methodName, IOpenClass[] parTypes) {
        var method = super.findMethodCaller(namespace, methodName, parTypes);
        if (method == null) {
            var internalSelect = CollectionUtils.findAll(internalMethods,
                    e -> Objects.equals(methodName, e.getName()));
            method = MethodSearch.findMethod(methodName, parTypes, this, internalSelect, false);
        }
        return method;
    }

    private RecursiveOpenMethodPreBinder extractOpenMethodPrebinder(IMethodCaller method) {
        if (method instanceof RecursiveOpenMethodPreBinder binder) {
            return binder;
        } else if (method instanceof CastingMethodCaller) {
            return extractOpenMethodPrebinder(method.getMethod());
        } else if (method instanceof AOpenMethodDelegator delegator) {
            return extractOpenMethodPrebinder(delegator.getDelegate());
        }
        throw new IllegalStateException("It should not happen.");
    }

    /**
     * The database the tables bound in this context are registered in: the database of the module.
     *
     * @return the database of the module
     */
    public IDataBase getDataBase() {
        return getModule().getDataBase();
    }

    @Override
    public XlsModuleOpenClass getModule() {
        return (XlsModuleOpenClass) super.getModule();
    }

    @Override
    public IOpenClass addType(IOpenClass type) throws DuplicatedTypeException {
        final var typeName = type.getName();
        if (type instanceof ModuleSpecificType moduleRelatedType) {
            var openClass = super.findType(typeName);
            if (openClass == moduleRelatedType) {
                return openClass;
            }
            if (openClass == null) {
                return moduleRelatedType.convertToModuleTypeAndRegister(getModule());
            }
            var existingModuleRelatedOpenClass = (ModuleSpecificType) openClass;
            existingModuleRelatedOpenClass.updateWithType(type);
            return openClass;
        }
        return super.addType(type);
    }

    @Override
    public IOpenClass findType(String typeName) {
        var openClass = super.findType(typeName);
        // We found some type which can be CSR
        // So there additional action is required for CSR
        if (openClass instanceof SpreadsheetResultOpenClass class1 && class1
                .getModule() == null) {
            return getModule().getSpreadsheetResultOpenClassWithResolvedFieldTypes();
        } else if (openClass instanceof CustomSpreadsheetResultOpenClass csrOpenClass
                && !csrOpenClass.isIgnoreCompilation()) {
            preBindSpreadsheetsOf(csrOpenClass);
        }
        return openClass;
    }

    public void addDataTableBinder(String uri, Runnable binder) {
        dataTableBinders.put(uri, binder);
    }

    public boolean removeDataTableBinder(String uri) {
        return dataTableBinders.remove(uri) != null;
    }

    public void preBindSpreadsheetsOf(CustomSpreadsheetResultOpenClass type) {
        // CSR class name is a conjunction of "SpreadsheetResult" and "MethodName"
        // If a class is CSR, then extract a method from the class name and process the Spreadsheet method
        final var methodName = type.getName()
                .substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length());
        preBinderMethods.findByMethodName(methodName).forEach(openMethodBinder -> {
            if (openMethodBinder.isSpreadsheetWithCustomSpreadsheetResult()) {
                preBindMethod(openMethodBinder.getHeader());
            }
        });
    }

    @Override
    public IOpenClass combineTypes(IOpenClass... openClasses) throws TypesCombinationNotSupportedException {
        if (openClasses == null || Arrays.stream(openClasses).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("openClass cannot be null");
        }
        var customSpreadsheetResultOpenClasses = Arrays.stream(openClasses)
                .filter(CustomSpreadsheetResultOpenClass.class::isInstance)
                .map(CustomSpreadsheetResultOpenClass.class::cast)
                .toArray(CustomSpreadsheetResultOpenClass[]::new);
        if (customSpreadsheetResultOpenClasses.length != openClasses.length) {
            throw new TypesCombinationNotSupportedException(Arrays.stream(openClasses)
                    .filter(e -> !(e instanceof CustomSpreadsheetResultOpenClass))
                    .toList());
        }
        return getModule().buildOrGetCombinedSpreadsheetResult(customSpreadsheetResultOpenClasses);
    }

    public void addBinderMethod(OpenMethodHeader openMethodHeader, RecursiveOpenMethodPreBinder method) {
        if (!isExecutionMode() && method.isSpreadsheetWithCustomSpreadsheetResult()) {
            final var sprTypeName = Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + method.getName();
            var openClass = findType(sprTypeName);
            if (openClass instanceof CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
                customSpreadsheetResultOpenClass.setMetaInfo(
                        new TableMetaInfo("Spreadsheet", method.getName(), method.getTableSyntaxNode().getUri()));
            } else {
                throw new IllegalStateException(MessageUtils.getTypeNotFoundMessage(sprTypeName));
            }
        }
        preBinderMethods.put(openMethodHeader, method);
    }

    public void preBindMethod(@Nullable OpenMethodHeader openMethodHeader) {
        if (openMethodHeader == null) {
            return;
        }
        var openMethodBinder = preBinderMethods.get(openMethodHeader);
        if (openMethodBinder == null) {
            // No need to compile, because it is already compiled.
            return;
        }

        // All custom spreadsheet methods compiles at once
        List<RecursiveOpenMethodPreBinder> openMethodBinders;
        @Nullable CustomSpreadsheetResultOpenClass type = null;
        if (openMethodBinder.isSpreadsheetWithCustomSpreadsheetResult()) {
            if (isIgnoreCustomSpreadsheetResultCompilation()) {
                return;
            }
            openMethodBinders = preBinderMethods.findByMethodName(openMethodHeader.getName())
                    .stream()
                    .filter(RecursiveOpenMethodPreBinder::isSpreadsheetWithCustomSpreadsheetResult)
                    .toList();
            final var customSpreadsheetResultTypeName = Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + openMethodHeader
                    .getName();
            var openClass = super.findType(customSpreadsheetResultTypeName);
            if (openClass instanceof CustomSpreadsheetResultOpenClass csroc) {
                csroc.setIgnoreCompilation(true);
                type = csroc;
            } else {
                throw new IllegalStateException(MessageUtils.getTypeNotFoundMessage(customSpreadsheetResultTypeName));
            }
        } else {
            compileCustomSpreadsheetResultType(openMethodBinder);
            openMethodBinders = List.of(openMethodBinder);
        }
        Optional<RecursiveOpenMethodPreBinder> prebindingOpenMethodPreBinder = openMethodBinders.stream()
                .filter(RecursiveOpenMethodPreBinder::isPreBindStarted)
                .findAny();
        if (prebindingOpenMethodPreBinder.isPresent()) {
            if (isTypeInBinding(prebindingOpenMethodPreBinder.get())) {
                // a call of the tables of the type being built: they are in the module, a versioned one dispatches
                return;
            }
            throw circularReference(prebindingOpenMethodPreBinder.get());
        }
        if (type == null) {
            preBind(openMethodBinders, null);
        } else {
            bindType(type, openMethodBinders);
        }
        openMethodBinders.forEach(e -> preBinderMethods.remove(e.getHeader()));
        openMethodBinders.forEach(RecursiveOpenMethodPreBinder::finishPreBind);
    }

    /**
     * Binds the tables of a spreadsheet result type: they are added to the module first, then their bodies are bound
     * and the type takes their steps.
     */
    private void bindType(CustomSpreadsheetResultOpenClass type, List<RecursiveOpenMethodPreBinder> openMethodBinders) {
        var outerTypeInBinding = typeInBinding;
        // a rule adds its steps after the tables that follow it, yet they keep the place of the rule
        var keepTableOrder = openMethodBinders.size() > 1;
        if (keepTableOrder) {
            type.keepTableOrder();
        }
        openMethodBinders.forEach(e -> tablesInBinding.put(e.getTableSyntaxNode(), e));
        try {
            preBind(openMethodBinders, type);
            // all tables of the type are in the module: a call of them takes the type being built
            typeInBinding = type;
            openMethodBinders.forEach(RecursiveOpenMethodPreBinder::bindBody);
            forEachInTableOrder(type, openMethodBinders, RecursiveOpenMethodPreBinder::completeType);
        } finally {
            typeInBinding = outerTypeInBinding;
            openMethodBinders.forEach(e -> tablesInBinding.remove(e.getTableSyntaxNode()));
            if (keepTableOrder) {
                type.restoreTableOrder();
            }
        }
    }

    /** Adds the tables to the module: the tables they call are bound meanwhile, none of them takes a type being built. */
    private void preBind(List<RecursiveOpenMethodPreBinder> openMethodBinders,
                         @Nullable CustomSpreadsheetResultOpenClass type) {
        var outerTypeInBinding = typeInBinding;
        try {
            typeInBinding = null;
            openMethodBinders.forEach(RecursiveOpenMethodPreBinder::startPreBind);
            forEachInTableOrder(type, openMethodBinders, RecursiveOpenMethodPreBinder::preBind);
        } finally {
            typeInBinding = outerTypeInBinding;
        }
    }

    /** Runs a step of binding on each table, telling the type the place of the table whose steps it adds. */
    private static void forEachInTableOrder(@Nullable CustomSpreadsheetResultOpenClass type,
                                            List<RecursiveOpenMethodPreBinder> openMethodBinders,
                                            Consumer<RecursiveOpenMethodPreBinder> step) {
        for (var i = 0; i < openMethodBinders.size(); i++) {
            if (type != null) {
                type.setTablePlace(i);
            }
            step.accept(openMethodBinders.get(i));
        }
    }

    /**
     * Compiles the custom spreadsheet result type that the method returns, directly or as an array component type.
     */
    private void compileCustomSpreadsheetResultType(RecursiveOpenMethodPreBinder openMethodBinder) {
        var t = openMethodBinder.getType();
        if (t != null) {
            while (t.isArray()) {
                t = t.getComponentClass();
            }
            if (t instanceof CustomSpreadsheetResultOpenClass) {
                // Fires type compilation
                findType(t.getName());
            }
        }
    }

    public static final class DispatchingTablePropertiesOpenField implements IOpenField {

        @Override
        public @Nullable Object get(Object target, IRuntimeEnv env) {
            if (env instanceof SimpleRuntimeEnv simpleRuntimeEnv) {
                var method = simpleRuntimeEnv.getMethodWrapper()
                        .getTopOpenClassMethod(simpleRuntimeEnv.getTopClass());
                if (method instanceof ExecutableRulesMethod executableRulesMethod) {
                    return new TableProperties[]{new TableProperties(executableRulesMethod.getMethodProperties())};
                } else if (method instanceof OpenMethodDispatcher openMethodDispatcher) {
                    return openMethodDispatcher.getCandidates().stream()
                            .filter(ExecutableRulesMethod.class::isInstance)
                            .map(candidate -> new TableProperties(
                                    ((ExecutableRulesMethod) candidate).getMethodProperties()))
                            .toArray(TableProperties[]::new);
                }
            }
            return null;
        }

        @Override
        public void set(Object target, Object value, IRuntimeEnv env) {
            // The field is read-only.
        }

        @Override
        public boolean isConst() {
            return false;
        }

        @Override
        public boolean isReadable() {
            return true;
        }

        @Override
        public boolean isContextProperty() {
            return false;
        }

        @Override
        public @Nullable String getContextProperty() {
            return null;
        }

        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return false;
        }

        @Override
        public String getDisplayName(int mode) {
            return getName();
        }

        @Override
        public String getName() {
            return "$dispatchingProperties";
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.getOpenClass(TableProperties.class).getArrayType(1);
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public @Nullable IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenClass getDeclaringClass() {
            return NullOpenClass.the;
        }
    }

    public static final class TablePropertiesOpenField implements IOpenField {

        @Override
        public @Nullable Object get(Object target, IRuntimeEnv env) {
            if (env instanceof SimpleRuntimeEnv simpleRuntimeEnv) {
                if (simpleRuntimeEnv.getMethodWrapper()
                        .getDelegate() instanceof ExecutableRulesMethod executableRulesMethod) {
                    return new TableProperties(executableRulesMethod.getMethodProperties());
                } else if (simpleRuntimeEnv.getMethodWrapper()
                        .getDelegate() instanceof OpenMethodDispatcher openMethodDispatcher) {
                    var method = openMethodDispatcher.findMatchingMethod(env);
                    if (method instanceof ExecutableRulesMethod executableRulesMethod) {
                        return new TableProperties(executableRulesMethod.getMethodProperties());
                    }
                }
            }
            return null;
        }

        @Override
        public void set(Object target, Object value, IRuntimeEnv env) {
            // The field is read-only.
        }

        @Override
        public boolean isConst() {
            return false;
        }

        @Override
        public boolean isReadable() {
            return true;
        }

        @Override
        public boolean isContextProperty() {
            return false;
        }

        @Override
        public @Nullable String getContextProperty() {
            return null;
        }

        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return false;
        }

        @Override
        public String getDisplayName(int mode) {
            return getName();
        }

        @Override
        public String getName() {
            return "$properties";
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.getOpenClass(TableProperties.class);
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public @Nullable IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenClass getDeclaringClass() {
            return NullOpenClass.the;
        }
    }

    public static final class CurrentRuntimeContextMethod implements IOpenMethod {
        static final String CURRENT_CONTEXT_METHOD_NAME = "getContext";

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            var context = (IRulesRuntimeContext) env.getContext();
            try {
                return context.clone();
            } catch (CloneNotSupportedException e) {
                LoggerFactory.getLogger(RulesModuleBindingContext.class)
                        .warn("Failed to clone runtime context. Runtime context managing may work incorrectly.", e);
                return context;
            }
        }

        @Override
        public IOpenMethod getMethod() {
            return this;
        }

        @Override
        public String getName() {
            return CURRENT_CONTEXT_METHOD_NAME;
        }

        @Override
        public String getDisplayName(int mode) {
            return CURRENT_CONTEXT_METHOD_NAME;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.getOpenClass(IRulesRuntimeContext.class);
        }

        @Override
        public @Nullable IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public @Nullable IOpenClass getDeclaringClass() {
            return null;
        }

        @Override
        public IMethodSignature getSignature() {
            return IMethodSignature.VOID;
        }

        @Override
        public boolean isConstructor() {
            return false;
        }
    }

    public static final class EmptyRuntimeContextMethod implements IOpenMethod {
        static final String EMPTY_CONTEXT_METHOD_NAME = "emptyContext";

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            return RulesRuntimeContextFactory.buildRulesRuntimeContext();
        }

        @Override
        public IOpenMethod getMethod() {
            return this;
        }

        @Override
        public String getName() {
            return EMPTY_CONTEXT_METHOD_NAME;
        }

        @Override
        public String getDisplayName(int mode) {
            return EMPTY_CONTEXT_METHOD_NAME;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.getOpenClass(IRulesRuntimeContext.class);
        }

        @Override
        public @Nullable IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public @Nullable IOpenClass getDeclaringClass() {
            return null;
        }

        @Override
        public IMethodSignature getSignature() {
            return IMethodSignature.VOID;
        }

        @Override
        public boolean isConstructor() {
            return false;
        }
    }

    public static final class RestoreRuntimeContextMethod implements IOpenMethod {
        static final String RESTORE_CONTEXT_METHOD_NAME = "restoreContext";

        @Override
        public @Nullable Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            if (env.isContextManagingSupported()) {
                env.popContext();
            } else {
                LoggerFactory.getLogger(RulesModuleBindingContext.class)
                        .warn("Failed to restore runtime context. Runtime context does not support context modifications.");
            }
            return null;
        }

        @Override
        public IOpenMethod getMethod() {
            return this;
        }

        @Override
        public String getName() {
            return RESTORE_CONTEXT_METHOD_NAME;
        }

        @Override
        public String getDisplayName(int mode) {
            return RESTORE_CONTEXT_METHOD_NAME;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.VOID;
        }

        @Override
        public @Nullable IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public @Nullable IOpenClass getDeclaringClass() {
            return null;
        }

        @Override
        public IMethodSignature getSignature() {
            return IMethodSignature.VOID;
        }

        @Override
        public boolean isConstructor() {
            return false;
        }
    }

    public static final class SetRuntimeContextMethod implements IOpenMethod {
        static final String SET_CONTEXT_METHOD_NAME = "setContext";

        @Override
        public @Nullable Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            if (env.isContextManagingSupported()) {
                var runtimeContext = (IRulesRuntimeContext) params[0];
                env.pushContext(runtimeContext);
            } else {
                LoggerFactory.getLogger(RulesModuleBindingContext.class)
                        .warn("Failed to set runtime context. Runtime context does not support context modifications.");
            }
            return null;
        }

        @Override
        public IOpenMethod getMethod() {
            return this;
        }

        @Override
        public String getName() {
            return SET_CONTEXT_METHOD_NAME;
        }

        @Override
        public String getDisplayName(int mode) {
            return SET_CONTEXT_METHOD_NAME;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.VOID;
        }

        @Override
        public @Nullable IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public @Nullable IOpenClass getDeclaringClass() {
            return null;
        }

        @Override
        public IMethodSignature getSignature() {
            return new MethodSignature(
                    new ParameterDeclaration(JavaOpenClass.getOpenClass(IRulesRuntimeContext.class), "context"));
        }

        @Override
        public boolean isConstructor() {
            return false;
        }

    }

    public static final class ModifyRuntimeContextMethod implements IOpenMethod {
        static final String MODIFY_CONTEXT_METHOD_NAME = "modifyContext";

        /**
         * A text given for a property of another type, such as a state or a date, is read as a value of that type,
         * the way a cell of a table is.
         */
        private static @Nullable Object propertyValue(String propertyName, @Nullable Object value) {
            var type = DefaultRulesRuntimeContext.CONTEXT_PROPERTIES.get(propertyName);
            return value instanceof String text && type != null && type != String.class
                    ? String2DataConvertorFactory.parse(type, text, null)
                    : value;
        }

        @Override
        public @Nullable Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            if (env.isContextManagingSupported()) {
                try {
                    var runtimeContext = ((IRulesRuntimeContext) env.getContext()).clone();
                    var propertyName = (String) params[0];
                    runtimeContext.setValue(propertyName, propertyValue(propertyName, params[1]));
                    env.pushContext(runtimeContext);
                } catch (CloneNotSupportedException ignored) {
                    // a runtime context is Cloneable (see IRuntimeContext), so cloning it does not fail
                }
            } else {
                LoggerFactory.getLogger(RulesModuleBindingContext.class)
                        .warn("Failed to modify runtime context. Runtime context does not support context modifications.");
            }
            return null;
        }

        @Override
        public IOpenMethod getMethod() {
            return this;
        }

        @Override
        public String getName() {
            return MODIFY_CONTEXT_METHOD_NAME;
        }

        @Override
        public String getDisplayName(int mode) {
            return MODIFY_CONTEXT_METHOD_NAME;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.VOID;
        }

        @Override
        public @Nullable IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public @Nullable IOpenClass getDeclaringClass() {
            return null;
        }

        @Override
        public IMethodSignature getSignature() {
            return new MethodSignature(new ParameterDeclaration(JavaOpenClass.STRING, "property"),
                    new ParameterDeclaration(JavaOpenClass.OBJECT, "value"));
        }

        @Override
        public boolean isConstructor() {
            return false;
        }
    }

    private final IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache = new IdentityHashMap<>();

    @Override
    protected boolean isComponentSpecificOpenClass(IOpenClass componentOpenClass) {
        return isComponentSpecificOpenClass(componentOpenClass, getModule(), cache);
    }

    public static boolean isComponentSpecificOpenClass(IOpenClass componentOpenClass,
                                                       XlsModuleOpenClass xlsModuleOpenClass,
                                                       IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
        if (componentOpenClass instanceof CustomSpreadsheetResultOpenClass class2) {
            return xlsModuleOpenClass
                    .isDependencyModule(class2.getModule(), cache);
        } else if (componentOpenClass instanceof SpreadsheetResultOpenClass class1) {
            return xlsModuleOpenClass
                    .isDependencyModule(class1.getModule(), cache);
        }
        return false;
    }

}
