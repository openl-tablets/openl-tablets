package org.openl.rules.binding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
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

/**
 * Binding context for xls rules.
 *
 * @author DLiauchuk
 */
public class RulesModuleBindingContext extends ModuleBindingContext {
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

    private boolean ignoreCustomSpreadsheetResultCompilation = false;

    public RulesModuleBindingContext(IBindingContext delegate, XlsModuleOpenClass module) {
        super(delegate, module);
        internalMethods = new ArrayList<>();
        internalMethods.add(new CurrentRuntimeContextMethod());
        internalMethods.add(new EmptyRuntimeContextMethod());
        internalMethods.add(new RestoreRuntimeContextMethod());
        internalMethods.add(new SetRuntimeContextMethod());
        internalMethods.add(new ModifyRuntimeContextMethod());
        internalMethods.add(new MessageSourceResourceMethod());

        TablePropertiesOpenField tablePropertiesOpenField = new TablePropertiesOpenField();
        internalVariables.put(tablePropertiesOpenField.getName(), tablePropertiesOpenField);
        noStrictInternalVariables.put(tablePropertiesOpenField.getName().toLowerCase(), tablePropertiesOpenField);
        DispatchingTablePropertiesOpenField dispatchingTablePropertiesOpenField = new DispatchingTablePropertiesOpenField();
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

    public TableSyntaxNode getTableSyntaxNode(String key) {
        return this.bindedTables.get(key);
    }

    @Override
    public IOpenField findVar(String namespace, String name, boolean strictMatch) throws AmbiguousFieldException {
        IOpenField openField = super.findVar(namespace, name, strictMatch);
        if (openField != null) {
            return openField;
        }
        if (strictMatch) {
            return internalVariables.get(name);
        } else {
            if (name != null) {
                return noStrictInternalVariables.get(name.toLowerCase());
            }
        }
        return null;
    }

    @Override
    public IMethodCaller findMethodCaller(String namespace, final String methodName, IOpenClass[] parTypes) {
        Iterable<IOpenMethod> select = CollectionUtils.findAll(
                preBinderMethods.values().stream().map(IOpenMethod.class::cast).collect(Collectors.toList()),
                e -> Objects.equals(methodName, e.getName()));
        IMethodCaller method;
        try {
            method = MethodSearch.findMethod(methodName, parTypes, this, select, true);
            if (method != null) {
                RecursiveOpenMethodPreBinder openMethodBinder = extractOpenMethodPrebinder(method);
                if (openMethodBinder.isPreBindStarted()) {
                    if (openMethodBinder.isSpreadsheetWithCustomSpreadsheetResult()) {
                        throw new RecursiveSpreadsheetMethodPreBindingException(
                                String.format("Type '%s' compilation failed with circular reference issue.",
                                        openMethodBinder.getCustomSpreadsheetResultOpenClass().getName()));
                    }
                    method = super.findMethodCaller(namespace, methodName, parTypes);
                    if (method == null) {
                        Iterable<IOpenMethod> internalSelect = CollectionUtils.findAll(internalMethods,
                                e -> Objects.equals(methodName, e.getName()));
                        method = MethodSearch.findMethod(methodName, parTypes, this, internalSelect, false);
                    }
                    if (method != null) {
                        return method;
                    }
                    throw new IllegalStateException(
                            String.format("Type '%s' compilation failed with circular reference issue.",
                                    openMethodBinder.getCustomSpreadsheetResultOpenClass().getName()));
                }
                preBindMethod(openMethodBinder.getHeader());
            }
        } catch (AmbiguousMethodException e) {
            e.getMatchingMethods()
                    .stream()
                    .map(m -> extractOpenMethodPrebinder(m).getHeader())
                    .forEach(this::preBindMethod);
        }
        method = super.findMethodCaller(namespace, methodName, parTypes);
        if (method == null) {
            Iterable<IOpenMethod> internalSelect = CollectionUtils.findAll(internalMethods,
                    e -> Objects.equals(methodName, e.getName()));
            method = MethodSearch.findMethod(methodName, parTypes, this, internalSelect, false);
        }
        return method;
    }

    private RecursiveOpenMethodPreBinder extractOpenMethodPrebinder(IMethodCaller method) {
        if (method instanceof RecursiveOpenMethodPreBinder) {
            return (RecursiveOpenMethodPreBinder) method;
        } else if (method instanceof CastingMethodCaller) {
            return extractOpenMethodPrebinder(method.getMethod());
        } else if (method instanceof AOpenMethodDelegator) {
            return extractOpenMethodPrebinder(((AOpenMethodDelegator) method).getDelegate());
        }
        throw new IllegalStateException("It should not happen.");
    }

    @Override
    public XlsModuleOpenClass getModule() {
        return (XlsModuleOpenClass) super.getModule();
    }

    @Override
    public IOpenClass addType(IOpenClass type) throws DuplicatedTypeException {
        final String typeName = type.getName();
        if (type instanceof ModuleSpecificType) {
            ModuleSpecificType moduleRelatedType = (ModuleSpecificType) type;
            IOpenClass openClass = super.findType(typeName);
            if (openClass == moduleRelatedType) {
                return openClass;
            }
            if (openClass == null) {
                return moduleRelatedType.convertToModuleTypeAndRegister(getModule());
            } else {
                ModuleSpecificType existingModuleRelatedOpenClass = (ModuleSpecificType) openClass;
                existingModuleRelatedOpenClass.updateWithType(type);
                return openClass;
            }
        } else {
            return super.addType(type);
        }
    }

    @Override
    public IOpenClass findType(String typeName) {
        IOpenClass openClass = super.findType(typeName);
        // We found some type which can be CSR
        // So there additional action is required for CSR
        if (openClass instanceof SpreadsheetResultOpenClass && ((SpreadsheetResultOpenClass) openClass)
                .getModule() == null) {
            return getModule().getSpreadsheetResultOpenClassWithResolvedFieldTypes();
        } else if (openClass instanceof CustomSpreadsheetResultOpenClass) {
            CustomSpreadsheetResultOpenClass csrOpenClass = (CustomSpreadsheetResultOpenClass) openClass;
            if (!csrOpenClass.isIgnoreCompilation()) {
                // CSR class name is a conjunction of "SpreadsheetResult" and "MethodName"
                // If a class is CSR, then extract a method from the class name and process the Spreadsheet method
                final String methodName = csrOpenClass.getName()
                        .substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length());
                preBinderMethods.findByMethodName(methodName).forEach(openMethodBinder -> {
                    if (openMethodBinder.isSpreadsheetWithCustomSpreadsheetResult()) {
                        preBindMethod(openMethodBinder.getHeader());
                    }
                });
            }
        }
        return openClass;
    }

    @Override
    public IOpenClass combineTypes(IOpenClass... openClasses) throws TypesCombinationNotSupportedException {
        if (openClasses == null || Arrays.stream(openClasses).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("openClass cannot be null");
        }
        CustomSpreadsheetResultOpenClass[] customSpreadsheetResultOpenClasses = Arrays.stream(openClasses)
                .filter(e -> e instanceof CustomSpreadsheetResultOpenClass)
                .map(CustomSpreadsheetResultOpenClass.class::cast)
                .toArray(CustomSpreadsheetResultOpenClass[]::new);
        if (customSpreadsheetResultOpenClasses.length != openClasses.length) {
            throw new TypesCombinationNotSupportedException(Arrays.stream(openClasses)
                    .filter(e -> !(e instanceof CustomSpreadsheetResultOpenClass))
                    .collect(Collectors.toList()));
        } else {
            return getModule().buildOrGetCombinedSpreadsheetResult(customSpreadsheetResultOpenClasses);
        }
    }

    public void addBinderMethod(OpenMethodHeader openMethodHeader, RecursiveOpenMethodPreBinder method) {
        if (!isExecutionMode() && method.isSpreadsheetWithCustomSpreadsheetResult()) {
            final String sprTypeName = Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + method.getName();
            IOpenClass openClass = findType(sprTypeName);
            if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) openClass;
                customSpreadsheetResultOpenClass.setMetaInfo(
                        new TableMetaInfo("Spreadsheet", method.getName(), method.getTableSyntaxNode().getUri()));
            } else {
                throw new IllegalStateException(MessageUtils.getTypeNotFoundMessage(sprTypeName));
            }
        }
        preBinderMethods.put(openMethodHeader, method);
    }

    public void preBindMethod(OpenMethodHeader openMethodHeader) {
        if (openMethodHeader == null) {
            return;
        }
        RecursiveOpenMethodPreBinder openMethodBinder = preBinderMethods.get(openMethodHeader);
        if (openMethodBinder == null) {
            // No need to compile, because it is already compiled.
            return;
        }

        // All custom spreadsheet methods compiles at once
        Collection<RecursiveOpenMethodPreBinder> openMethodBinders;
        if (openMethodBinder.isSpreadsheetWithCustomSpreadsheetResult()) {
            if (isIgnoreCustomSpreadsheetResultCompilation()) {
                return;
            }
            openMethodBinders = preBinderMethods.findByMethodName(openMethodHeader.getName());
            openMethodBinders = openMethodBinders.stream()
                    .filter(RecursiveOpenMethodPreBinder::isSpreadsheetWithCustomSpreadsheetResult)
                    .collect(Collectors.toList());
            final String customSpreadsheetResultTypeName = Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + openMethodHeader
                    .getName();
            IOpenClass openClass = super.findType(customSpreadsheetResultTypeName);
            if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                CustomSpreadsheetResultOpenClass csroc = (CustomSpreadsheetResultOpenClass) openClass;
                csroc.setIgnoreCompilation(true);
            } else {
                throw new IllegalStateException(MessageUtils.getTypeNotFoundMessage(customSpreadsheetResultTypeName));
            }
        } else {
            IOpenClass t = openMethodBinder.getType();
            if (t != null) {
                while (t.isArray()) {
                    t = t.getComponentClass();
                }
                if (t instanceof CustomSpreadsheetResultOpenClass) {
                    // Fires type compilation
                    findType(t.getName());
                }
            }
            openMethodBinders = Collections.singletonList(openMethodBinder);
        }
        Optional<RecursiveOpenMethodPreBinder> prebindingOpenMethodPreBinder = openMethodBinders.stream()
                .filter(RecursiveOpenMethodPreBinder::isPreBindStarted)
                .findAny();
        if (prebindingOpenMethodPreBinder.isPresent()) {
            if (prebindingOpenMethodPreBinder.get().isSpreadsheetWithCustomSpreadsheetResult()) {
                throw new RecursiveSpreadsheetMethodPreBindingException(
                        String.format("Type '%s' compilation failed with circular reference issue.",
                                prebindingOpenMethodPreBinder.get().getCustomSpreadsheetResultOpenClass().getName()));
            } else {
                throw new IllegalStateException(
                        String.format("Type '%s' compilation failed with circular reference issue.",
                                prebindingOpenMethodPreBinder.get().getCustomSpreadsheetResultOpenClass().getName()));
            }
        }
        openMethodBinders.forEach(RecursiveOpenMethodPreBinder::startPreBind);
        openMethodBinders.forEach(RecursiveOpenMethodPreBinder::preBind);
        openMethodBinders.forEach(e -> preBinderMethods.remove(e.getHeader()));
        openMethodBinders.forEach(RecursiveOpenMethodPreBinder::finishPreBind);
    }

    public static final class DispatchingTablePropertiesOpenField implements IOpenField {

        @Override
        public Object get(Object target, IRuntimeEnv env) {
            if (env instanceof SimpleRulesRuntimeEnv) {
                SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = (SimpleRulesRuntimeEnv) env;
                IOpenMethod method = simpleRulesRuntimeEnv.getMethodWrapper()
                        .getTopOpenClassMethod(simpleRulesRuntimeEnv.getTopClass());
                if (method instanceof ExecutableRulesMethod) {
                    ExecutableRulesMethod executableRulesMethod = (ExecutableRulesMethod) method;
                    return new TableProperties[]{new TableProperties(executableRulesMethod.getMethodProperties())};
                } else if (method instanceof OpenMethodDispatcher) {
                    OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) method;
                    List<TableProperties> tableProperties = new ArrayList<>();
                    for (IOpenMethod method1 : openMethodDispatcher.getCandidates()) {
                        if (method1 instanceof ExecutableRulesMethod) {
                            tableProperties
                                    .add(new TableProperties(((ExecutableRulesMethod) method1).getMethodProperties()));
                        }
                    }
                    return tableProperties.toArray(new TableProperties[]{});
                }
            }
            return null;
        }

        @Override
        public void set(Object target, Object value, IRuntimeEnv env) {
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
        public String getContextProperty() {
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
        public IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenClass getDeclaringClass() {
            return NullOpenClass.the;
        }
    }

    public static final class TablePropertiesOpenField implements IOpenField {

        @Override
        public Object get(Object target, IRuntimeEnv env) {
            if (env instanceof SimpleRulesRuntimeEnv) {
                SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = (SimpleRulesRuntimeEnv) env;
                if (simpleRulesRuntimeEnv.getMethodWrapper().getDelegate() instanceof ExecutableRulesMethod) {
                    ExecutableRulesMethod executableRulesMethod = (ExecutableRulesMethod) simpleRulesRuntimeEnv
                            .getMethodWrapper()
                            .getDelegate();
                    return new TableProperties(executableRulesMethod.getMethodProperties());
                } else if (simpleRulesRuntimeEnv.getMethodWrapper().getDelegate() instanceof OpenMethodDispatcher) {
                    OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) simpleRulesRuntimeEnv
                            .getMethodWrapper()
                            .getDelegate();
                    IOpenMethod method = openMethodDispatcher.findMatchingMethod(env);
                    if (method instanceof ExecutableRulesMethod) {
                        ExecutableRulesMethod executableRulesMethod = (ExecutableRulesMethod) method;
                        return new TableProperties(executableRulesMethod.getMethodProperties());
                    }
                }
            }
            return null;
        }

        @Override
        public void set(Object target, Object value, IRuntimeEnv env) {
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
        public String getContextProperty() {
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
        public IMemberMetaInfo getInfo() {
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
            IRulesRuntimeContext context = (IRulesRuntimeContext) env.getContext();
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
        public IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenClass getDeclaringClass() {
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
        public IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenClass getDeclaringClass() {
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
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
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
        public IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenClass getDeclaringClass() {
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
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            if (env.isContextManagingSupported()) {
                IRulesRuntimeContext runtimeContext = (IRulesRuntimeContext) params[0];
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
        public IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenClass getDeclaringClass() {
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

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            if (env.isContextManagingSupported()) {
                try {
                    IRulesRuntimeContext runtimeContext = ((IRulesRuntimeContext) env.getContext()).clone();
                    runtimeContext.setValue((String) params[0], params[1]);
                    env.pushContext(runtimeContext);
                } catch (CloneNotSupportedException ignored) {
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
        public IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenClass getDeclaringClass() {
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

    private boolean isIgnoreCustomSpreadsheetResultCompilation() {
        return ignoreCustomSpreadsheetResultCompilation;
    }

    public void setIgnoreCustomSpreadsheetResultCompilation(boolean ignoreCustomSpreadsheetResultCompilation) {
        this.ignoreCustomSpreadsheetResultCompilation = ignoreCustomSpreadsheetResultCompilation;
    }

    private final IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache = new IdentityHashMap<>();

    protected boolean isComponentSpecificOpenClass(IOpenClass componentOpenClass) {
        return isComponentSpecificOpenClass(this, componentOpenClass, getModule(), cache);
    }

    public static boolean isComponentSpecificOpenClass(IBindingContext bindingContext,
                                                       IOpenClass componentOpenClass,
                                                       XlsModuleOpenClass xlsModuleOpenClass,
                                                       IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
        if (componentOpenClass instanceof CustomSpreadsheetResultOpenClass) {
            return xlsModuleOpenClass
                    .isDependencyModule(((CustomSpreadsheetResultOpenClass) componentOpenClass).getModule(), cache);
        } else if (componentOpenClass instanceof SpreadsheetResultOpenClass) {
            return xlsModuleOpenClass
                    .isDependencyModule(((SpreadsheetResultOpenClass) componentOpenClass).getModule(), cache);
        }
        return false;
    }

}
