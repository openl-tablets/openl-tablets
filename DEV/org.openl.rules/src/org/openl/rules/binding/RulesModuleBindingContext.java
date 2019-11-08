package org.openl.rules.binding;

import java.util.*;
import java.util.stream.Collectors;

import org.openl.binding.IBindingContext;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.DuplicatedTypeException;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.binding.impl.method.VarArgsOpenMethod;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextDelegator;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.*;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.LoggerFactory;

/**
 * Binding context for xls rules.
 *
 * @author DLiauchuk
 *
 */
public class RulesModuleBindingContext extends ModuleBindingContext {

    public static final String MODULE_PROPERTIES_KEY = "Properties:Module";
    public static final String CATEGORY_PROPERTIES_KEY = "Properties:Category:";
    private Map<String, TableSyntaxNode> bindedTables = new HashMap<>();

    /**
     * Internal OpenL service methods.
     */
    private List<IOpenMethod> internalMethods;

    private PreBinderMethods preBinderMethods = new PreBinderMethods();

    public RulesModuleBindingContext(IBindingContext delegate, XlsModuleOpenClass module) {
        super(delegate, module);
        internalMethods = new ArrayList<>();
        internalMethods.add(new CurrentRuntimeContextMethod());
        internalMethods.add(new EmptyRuntimeContextMethod());
        internalMethods.add(new RestoreRuntimeContextMethod());
        internalMethods.add(new SetRuntimeContextMethod());
        internalMethods.add(new ModifyRuntimeContextMethod());
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
    public boolean isTableSyntaxNodeExist(String key) {
        return this.bindedTables.containsKey(key);
    }

    public TableSyntaxNode getTableSyntaxNode(String key) {
        return this.bindedTables.get(key);
    }

    @Override
    public IMethodCaller findMethodCaller(String namespace, final String methodName, IOpenClass[] parTypes) {
        Iterable<IOpenMethod> select = CollectionUtils.findAll(
            preBinderMethods.values().stream().map(IOpenMethod.class::cast).collect(Collectors.toList()),
            e -> Objects.equals(methodName, e.getName()));
        IMethodCaller method = null;
        try {
            method = MethodSearch.findMethod(methodName, parTypes, this, select);
            if (method != null) {
                RecursiveOpenMethodPreBinder openMethodBinder = extractOpenMethodPrebinder(method);
                if (openMethodBinder.isPreBindStarted()) {
                    if (OpenLSystemProperties.isCustomSpreadsheetType(getExternalParams()) && isReturnTypeSpreadsheet(
                        openMethodBinder.getHeader())) {
                        throw new RecursiveSpreadsheetMethodPreBindingException();
                    }
                    method = super.findMethodCaller(namespace, methodName, parTypes);
                    if (method == null) {
                        Iterable<IOpenMethod> internalselect = CollectionUtils.findAll(internalMethods,
                            e -> Objects.equals(methodName, e.getName()));
                        method = MethodSearch.findMethod(methodName, parTypes, this, internalselect);
                    }
                    if (method != null) {
                        return method;
                    }
                    throw new IllegalStateException(
                        "Method compilaiton is failed with the circular reference to itself.");
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
            Iterable<IOpenMethod> internalselect = CollectionUtils.findAll(internalMethods,
                e -> Objects.equals(methodName, e.getName()));
            method = MethodSearch.findMethod(methodName, parTypes, this, internalselect);
        }
        return method;
    }

    private RecursiveOpenMethodPreBinder extractOpenMethodPrebinder(IMethodCaller method) {
        if (method instanceof RecursiveOpenMethodPreBinder) {
            return (RecursiveOpenMethodPreBinder) method;
        } else if (method instanceof CastingMethodCaller) {
            return (RecursiveOpenMethodPreBinder) ((CastingMethodCaller) method).getMethod();
        } else if (method instanceof VarArgsOpenMethod) {
            return (RecursiveOpenMethodPreBinder) ((VarArgsOpenMethod) method).getDelegate();
        }
        throw new IllegalStateException("It should not happen.");
    }

    @Override
    public XlsModuleOpenClass getModule() {
        return (XlsModuleOpenClass) super.getModule();
    }

    @Override
    public IOpenClass addType(String namespace, IOpenClass type) throws DuplicatedTypeException {
        final String typeName = type.getName();
        if (type instanceof CustomSpreadsheetResultOpenClass) {
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) type;
            IOpenClass openClass = super.findType(namespace, typeName);
            if (openClass == null) {
                IOpenClass copyOfCustomType = customSpreadsheetResultOpenClass.makeCopyForModule(getModule());
                getModule().addType(copyOfCustomType);
                return super.addType(namespace, copyOfCustomType);
            } else {
                CustomSpreadsheetResultOpenClass csroc = (CustomSpreadsheetResultOpenClass) openClass;
                csroc.extendWith((CustomSpreadsheetResultOpenClass) type, this);
                return csroc;
            }
        } else {
            return super.addType(namespace, type);
        }
    }

    @Override
    public IOpenClass findType(String namespace, String typeName) {
        if (OpenLSystemProperties.isCustomSpreadsheetType(getExternalParams()) && ISyntaxConstants.THIS_NAMESPACE
            .equals(namespace) && typeName.startsWith(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX) && typeName
                .length() > Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length()) {
            final String methodName = typeName.substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length());
            preBinderMethods.findByMethodName(methodName).forEach(openMethodBinder -> {
                if (isReturnTypeSpreadsheet(openMethodBinder.getHeader())) {
                    preBindMethod(openMethodBinder.getHeader());
                }
            });
        }
        return super.findType(namespace, typeName);
    }

    public void addBinderMethod(OpenMethodHeader openMethodHeader, RecursiveOpenMethodPreBinder method) {
        preBinderMethods.put(openMethodHeader, method);
    }

    public void preBindMethod(OpenMethodHeader openMethodHeader) {
        if (openMethodHeader == null) {
            return;
        }
        // All custom spreadsheet methods compiles at once
        Collection<RecursiveOpenMethodPreBinder> openMethodBinders;
        if (OpenLSystemProperties
            .isCustomSpreadsheetType(getExternalParams()) && isReturnTypeSpreadsheet(openMethodHeader)) {
            openMethodBinders = preBinderMethods.findByMethodName(openMethodHeader.getName());
            openMethodBinders = openMethodBinders.stream()
                .filter(e -> isReturnTypeSpreadsheet(e.getHeader()))
                .collect(Collectors.toList());
        } else {
            RecursiveOpenMethodPreBinder openMethodBinder = preBinderMethods.get(openMethodHeader);
            if (openMethodBinder == null) {
                return;
            }
            openMethodBinders = Collections.singletonList(openMethodBinder);
        }
        if (openMethodBinders.stream().anyMatch(RecursiveOpenMethodPreBinder::isPreBindStarted)) {
            if (OpenLSystemProperties
                .isCustomSpreadsheetType(getExternalParams()) && isReturnTypeSpreadsheet(openMethodHeader)) {
                throw new RecursiveSpreadsheetMethodPreBindingException();
            } else {
                throw new IllegalStateException("Method compilaiton is failed with the circular reference to itself.");
            }
        }
        openMethodBinders.stream().forEach(RecursiveOpenMethodPreBinder::startPreBind);
        openMethodBinders.stream().forEach(RecursiveOpenMethodPreBinder::preBind);
        openMethodBinders.stream().forEach(e -> preBinderMethods.remove(e.getHeader()));
        openMethodBinders.stream().forEach(RecursiveOpenMethodPreBinder::finishPreBind);
    }

    private boolean isReturnTypeSpreadsheet(OpenMethodHeader openMethodHeader) {
        return openMethodHeader.getType().getInstanceClass() != null && SpreadsheetResult.class
            .isAssignableFrom(openMethodHeader.getType().getInstanceClass());
    }

    public static final class CurrentRuntimeContextMethod implements IOpenMethod {
        public static final String CURRENT_CONTEXT_METHOD_NAME = "getContext";

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
        public static final String EMPTY_CONTEXT_METHOD_NAME = "emptyContext";

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
        public static final String RESTORE_CONTEXT_METHOD_NAME = "restoreContext";

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
        public static final String SET_CONTEXT_METHOD_NAME = "setContext";

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
        public static final String MODIFY_CONTEXT_METHOD_NAME = "modifyContext";

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            if (env.isContextManagingSupported()) {
                IRulesRuntimeContext runtimeContext = new RulesRuntimeContextDelegator(
                    (IRulesRuntimeContext) env.getContext());
                runtimeContext.setValue((String) params[0], params[1]);
                env.pushContext(runtimeContext);
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
}
