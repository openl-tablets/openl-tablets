package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.ModuleSpecificType;
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

    private WrapperLogic() {
    }

    public static IOpenMethod getTopClassMethod(IRulesMethodWrapper wrapper, IRuntimeEnv env) {
        SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = extractSimpleRulesRuntimeEnv(env);
        IOpenClass topClass = simpleRulesRuntimeEnv.getTopClass();
        if (topClass != null && topClass != wrapper.getXlsModuleOpenClass()) {
            IOpenMethod method = wrapper.getTopOpenClassMethod(topClass);
            if (method != null) {
                method = extractMethod(method);
                if (method != wrapper) {
                    return method;
                }
            }
        }
        return wrapper.getDelegate();
    }

    private static SimpleRulesRuntimeEnv extractSimpleRulesRuntimeEnv(IRuntimeEnv env) {
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

    public static IOpenMethod extractMethod(IOpenMethod method) {
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

    public static IMethodSignature buildMethodSignature(IOpenMethod openMethod, XlsModuleOpenClass xlsModuleOpenClass) {
        IOpenClass[] parameterTypes = openMethod.getSignature().getParameterTypes();
        IParameterDeclaration[] parameterDeclarations = new IParameterDeclaration[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            IOpenClass t;
            if (parameterTypes[i] instanceof ModuleSpecificType) {
                t = xlsModuleOpenClass.findType(parameterTypes[i].getName());
                t = t != null ? t : parameterTypes[i];
            } else {
                t = parameterTypes[i];
            }
            parameterDeclarations[i] = new ParameterDeclaration(t, openMethod.getSignature().getParameterName(i));
        }
        return new MethodSignature(parameterDeclarations);
    }

    public static IOpenMethod wrapOpenMethod(IOpenMethod openMethod, final XlsModuleOpenClass xlsModuleOpenClass) {
        if (openMethod instanceof IRulesMethodWrapper) {
            openMethod = ((IRulesMethodWrapper) openMethod).getDelegate();
        }
        if (openMethod instanceof TestSuiteMethod) {
            return openMethod;
        }
        ContextPropertiesInjector contextPropertiesInjector = new ContextPropertiesInjector(
            openMethod.getSignature().getParameterTypes(),
            xlsModuleOpenClass.getRulesModuleBindingContext());

        if (openMethod instanceof OverloadedMethodsDispatcherTable) {
            return new OverloadedMethodsDispatcherTableWrapper(xlsModuleOpenClass,
                (OverloadedMethodsDispatcherTable) openMethod,
                contextPropertiesInjector);
        }
        if (openMethod instanceof MatchingOpenMethodDispatcher) {
            return new MatchingOpenMethodDispatcherWrapper(xlsModuleOpenClass,
                (MatchingOpenMethodDispatcher) openMethod,
                contextPropertiesInjector);
        }
        if (openMethod instanceof Algorithm) {
            return new AlgorithmWrapper(xlsModuleOpenClass, (Algorithm) openMethod, contextPropertiesInjector);
        }
        if (openMethod instanceof AlgorithmSubroutineMethod) {
            return new AlgorithmSubroutineMethodWrapper(xlsModuleOpenClass,
                (AlgorithmSubroutineMethod) openMethod,
                contextPropertiesInjector);
        }
        if (openMethod instanceof DecisionTable) {
            return new DecisionTableWrapper(xlsModuleOpenClass, (DecisionTable) openMethod, contextPropertiesInjector);
        }
        if (openMethod instanceof ColumnMatch) {
            return new ColumnMatchWrapper(xlsModuleOpenClass, (ColumnMatch) openMethod, contextPropertiesInjector);
        }
        if (openMethod instanceof Spreadsheet) {
            return new SpreadsheetWrapper(xlsModuleOpenClass, (Spreadsheet) openMethod, contextPropertiesInjector);
        }
        if (openMethod instanceof TableMethod) {
            return new TableMethodWrapper(xlsModuleOpenClass, (TableMethod) openMethod, contextPropertiesInjector);
        }

        return openMethod;
    }

    public static Object invoke(IRulesMethodWrapper wrapper, Object target, Object[] params, IRuntimeEnv env) {
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
                return wrapper.invokeDelegate(target, params, env, simpleRulesRuntimeEnv);
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
                simpleRulesRuntimeEnv.setTopClass(null);
            }
        } else {
            if (topClass != wrapper.getXlsModuleOpenClass()) {
                IOpenMethod method = wrapper.getTopOpenClassMethod(topClass);
                if (method != null) {
                    method = extractMethod(method);
                    if (method != wrapper) {
                        return method.invoke(target, params, env);
                    }
                }
            }
        }
        return wrapper.invokeDelegate(target, params, env, simpleRulesRuntimeEnv);
    }
}
