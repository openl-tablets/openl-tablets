package org.openl.rules.lang.xls.binding.wrapper;

import java.util.IdentityHashMap;

import org.openl.binding.impl.module.ModuleSpecificType;
import org.openl.dependency.DependencyBindingContext;
import org.openl.dependency.DependencyOpenClass;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.binding.impl.module.ModuleSpecificType;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.prebind.ILazyMethod;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.runtime.ASMProxyFactory;
import org.openl.runtime.ASMProxyHandler;
import org.openl.runtime.OpenLMethodHandler;
import org.openl.types.IDynamicObject;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.MethodDelegator;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.vm.IRuntimeEnv;

public final class WrapperLogic {

    static {
        DependencyOpenClass.dependencyWrapperLogicToMethod = (openMethod, dependencyOpenClass) -> wrapOpenMethod(
            openMethod,
            (XlsModuleOpenClass) dependencyOpenClass.getDelegate(),
            true);
        // Can be refactored by returning the SpreadsheetResultOpenClass by XlsModuleOpenClass, but may introduce
        // another problems.
        DependencyBindingContext.additionalSearchTypesInModule = (name, module) -> {
            if (SpreadsheetResult.class.getName().equals(name) || SpreadsheetResult.class.getSimpleName()
                .equals(name)) {
                return ((XlsModuleOpenClass) module).getSpreadsheetResultOpenClassWithResolvedFieldTypes();
            }
            return null;
        };
    }

    private WrapperLogic() {
    }

    public static SimpleRulesRuntimeEnv extractSimpleRulesRuntimeEnv(IRuntimeEnv env) {
        IRuntimeEnv env1 = env;
        if (env instanceof TBasicContextHolderEnv) {
            TBasicContextHolderEnv tBasicContextHolderEnv = (TBasicContextHolderEnv) env;
            env1 = tBasicContextHolderEnv.getEnv();
            while (env1 instanceof TBasicContextHolderEnv) {
                tBasicContextHolderEnv = (TBasicContextHolderEnv) env1;
                env1 = tBasicContextHolderEnv.getEnv();
            }
        }
        return (SimpleRulesRuntimeEnv) env1;
    }

    public static IOpenMethod extractNonLazyMethod(IOpenMethod method) {
        if (method instanceof IRulesMethodWrapper) {
            IRulesMethodWrapper rulesMethodWrapper = (IRulesMethodWrapper) method;
            if (rulesMethodWrapper.getDelegate() instanceof ILazyMethod) {
                method = rulesMethodWrapper.getDelegate();
            }
        }
        while (method instanceof ILazyMethod || method instanceof MethodDelegator) {
            if (method instanceof ILazyMethod) {
                method = ((ILazyMethod) method).getMember();
            } else {
                MethodDelegator methodDelegator = (MethodDelegator) method;
                method = methodDelegator.getMethod();
            }
        }
        return method;
    }

    public static IOpenMethod unwrapOpenMethod(IOpenMethod method) {
        if (method instanceof IRulesMethodWrapper) {
            IRulesMethodWrapper wrapper = (IRulesMethodWrapper) method;
            return wrapper.getDelegate();
        }
        return method;
    }

    private static IOpenClass toModuleType(IOpenClass type,
            XlsModuleOpenClass xlsModuleOpenClass,
            IdentityHashMap<XlsModuleOpenClass, IdentityHashMap<XlsModuleOpenClass, Boolean>> cache) {
        int dim = 0;
        IOpenClass g = type;
        while (g.isArray()) {
            g = g.getComponentClass();
            dim++;
        }
        if (g instanceof ModuleSpecificType && !xlsModuleOpenClass
            .isExternalModule((XlsModuleOpenClass) ((ModuleSpecificType) g).getModule(), cache)) {
            IOpenClass t = xlsModuleOpenClass.findType(g.getName());
            return t != null ? (dim > 0 ? t.getArrayType(dim) : t) : g;
        } else if (g instanceof SpreadsheetResultOpenClass && xlsModuleOpenClass
            .getSpreadsheetResultOpenClassWithResolvedFieldTypes() != null && !xlsModuleOpenClass
                .isExternalModule(((SpreadsheetResultOpenClass) g).getModule(), cache)) {
            IOpenClass t = xlsModuleOpenClass.getSpreadsheetResultOpenClassWithResolvedFieldTypes();
            return t != null ? (dim > 0 ? t.getArrayType(dim) : t) : g;
        } else {
            return type;
        }
    }

    public static IOpenClass buildMethodReturnType(IOpenMethod openMethod, XlsModuleOpenClass xlsModuleOpenClass) {
        return toModuleType(openMethod.getType(), xlsModuleOpenClass, new IdentityHashMap<>());
    }

    public static IMethodSignature buildMethodSignature(IOpenMethod openMethod, XlsModuleOpenClass xlsModuleOpenClass) {
        IOpenClass[] parameterTypes = openMethod.getSignature().getParameterTypes();
        IParameterDeclaration[] parameterDeclarations = new IParameterDeclaration[parameterTypes.length];
        IdentityHashMap<XlsModuleOpenClass, IdentityHashMap<XlsModuleOpenClass, Boolean>> cache = null;
        for (int i = 0; i < parameterTypes.length; i++) {
            if (cache == null) {
                cache = new IdentityHashMap<>();
            }
            IOpenClass t = toModuleType(parameterTypes[i], xlsModuleOpenClass, cache);
            parameterDeclarations[i] = new ParameterDeclaration(t, openMethod.getSignature().getParameterName(i));
        }
        return new MethodSignature(parameterDeclarations);
    }

    public static IOpenMethod wrapOpenMethod(IOpenMethod openMethod,
            final XlsModuleOpenClass xlsModuleOpenClass,
            boolean externalMethodCall) {
        openMethod = unwrapOpenMethod(openMethod);

        if (openMethod instanceof TestSuiteMethod) {
            return openMethod;
        }

        ContextPropertiesInjector contextPropertiesInjector = null;
        if (!(openMethod instanceof ILazyMethod)) {
            contextPropertiesInjector = new ContextPropertiesInjector(openMethod.getSignature().getParameterTypes(),
                xlsModuleOpenClass.getRulesModuleBindingContext());
        }

        if (openMethod instanceof OverloadedMethodsDispatcherTable) {
            return new OverloadedMethodsDispatcherTableWrapper(xlsModuleOpenClass,
                (OverloadedMethodsDispatcherTable) openMethod,
                contextPropertiesInjector,
                externalMethodCall);
        }
        if (openMethod instanceof MatchingOpenMethodDispatcher) {
            return new MatchingOpenMethodDispatcherWrapper(xlsModuleOpenClass,
                (MatchingOpenMethodDispatcher) openMethod,
                contextPropertiesInjector,
                externalMethodCall);
        }
        if (openMethod instanceof Algorithm) {
            return new AlgorithmWrapper(xlsModuleOpenClass,
                (Algorithm) openMethod,
                contextPropertiesInjector,
                externalMethodCall);
        }
        if (openMethod instanceof AlgorithmSubroutineMethod) {
            return new AlgorithmSubroutineMethodWrapper(xlsModuleOpenClass,
                (AlgorithmSubroutineMethod) openMethod,
                contextPropertiesInjector,
                externalMethodCall);
        }
        if (openMethod instanceof DecisionTable) {
            return new DecisionTableWrapper(xlsModuleOpenClass,
                (DecisionTable) openMethod,
                contextPropertiesInjector,
                externalMethodCall);
        }
        if (openMethod instanceof ColumnMatch) {
            return new ColumnMatchWrapper(xlsModuleOpenClass,
                (ColumnMatch) openMethod,
                contextPropertiesInjector,
                externalMethodCall);
        }
        if (openMethod instanceof Spreadsheet) {
            return new SpreadsheetWrapper(xlsModuleOpenClass,
                (Spreadsheet) openMethod,
                contextPropertiesInjector,
                externalMethodCall);
        }
        if (openMethod instanceof TableMethod) {
            return new TableMethodWrapper(xlsModuleOpenClass,
                (TableMethod) openMethod,
                contextPropertiesInjector,
                externalMethodCall);
        }

        return openMethod;
    }

    private static Object invokeExternalMethod(IRulesMethodWrapper wrapper,
            Object target,
            Object[] params,
            IRuntimeEnv env) {
        SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = WrapperLogic.extractSimpleRulesRuntimeEnv(env);
        IOpenClass topClass = simpleRulesRuntimeEnv.getTopClass();
        if (topClass != null) {
            try {
                simpleRulesRuntimeEnv.setTopClass(wrapper.getXlsModuleOpenClass());
                return WrapperLogic.invoke(wrapper, target, params, env);
            } finally {
                simpleRulesRuntimeEnv.setTopClass(topClass);
            }
        }
        return WrapperLogic.invoke(wrapper, target, params, env);
    }

    private static Object invoke(IRulesMethodWrapper wrapper, Object target, Object[] params, IRuntimeEnv env) {
        if (wrapper.getDelegate() instanceof ILazyMethod) {
            return wrapper.getDelegate().invoke(target, params, env);
        }
        SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = extractSimpleRulesRuntimeEnv(env);
        IOpenClass topClass = simpleRulesRuntimeEnv.getTopClass();
        if (topClass == null) {
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                IOpenClass typeClass;
                if (target instanceof IDynamicObject) {
                    IDynamicObject dynamicObject = (IDynamicObject) target;
                    typeClass = dynamicObject.getType();
                } else if (ASMProxyFactory.isProxy(target)) {
                    ASMProxyHandler invocationHandler = ASMProxyFactory.getProxyHandler(target);
                    if (invocationHandler instanceof OpenLMethodHandler) {
                        OpenLMethodHandler openLMethodHandler = (OpenLMethodHandler) invocationHandler;
                        Object openlInstance = openLMethodHandler.getInstance();
                        if (openlInstance instanceof IDynamicObject) {
                            IDynamicObject dynamicObject = (IDynamicObject) openlInstance;
                            typeClass = dynamicObject.getType();
                        } else {
                            throw new IllegalStateException("Cannot define OpenL class from target object.");
                        }
                    } else {
                        throw new IllegalStateException("Cannot define OpenL class from target object.");
                    }
                } else {
                    throw new IllegalStateException("Cannot define OpenL class from target object.");
                }
                simpleRulesRuntimeEnv.setTopClass(typeClass);
                Thread.currentThread().setContextClassLoader(wrapper.getXlsModuleOpenClass().getClassLoader());
                return wrapper.invokeDelegateWithContextPropertiesInjector(target, params, env, simpleRulesRuntimeEnv);
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
                simpleRulesRuntimeEnv.setTopClass(null);
            }
        } else {
            if (topClass != wrapper.getXlsModuleOpenClass()) {
                IOpenMethod method = wrapper.getTopOpenClassMethod(topClass);
                if (method != null) {
                    method = extractNonLazyMethod(method);
                    if (method != wrapper) {
                        return method.invoke(target, params, env);
                    }
                }
            }
        }
        return wrapper.invokeDelegateWithContextPropertiesInjector(target, params, env, simpleRulesRuntimeEnv);
    }

    public static Object invoke(IRulesMethodWrapper wrapper,
            Object target,
            Object[] params,
            IRuntimeEnv env,
            boolean externalMethodCall) {
        Object ret = externalMethodCall ? WrapperLogic.invokeExternalMethod(wrapper, target, params, env)
                                        : WrapperLogic.invoke(wrapper, target, params, env);
        // Set custom spreadsheet type to work in the correct classloader with spreadsheet result been types
        if (ret != null) {
            if (wrapper.getType() instanceof CustomSpreadsheetResultOpenClass) {
                if (((SpreadsheetResult) ret).getCustomSpreadsheetResultOpenClass() == null) {
                    ((SpreadsheetResult) ret)
                        .setCustomSpreadsheetResultOpenClass((CustomSpreadsheetResultOpenClass) wrapper.getType());
                }
            } else if (wrapper
                .getType() instanceof SpreadsheetResultOpenClass && ((SpreadsheetResultOpenClass) wrapper.getType())
                    .getModule() != null) {
                if (((SpreadsheetResult) ret).getCustomSpreadsheetResultOpenClass() == null) {
                    ((SpreadsheetResult) ret).setCustomSpreadsheetResultOpenClass(
                        ((SpreadsheetResultOpenClass) wrapper.getType()).toCustomSpreadsheetResultOpenClass());
                }
            }
        }
        return ret;
    }
}
