package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.binding.impl.module.DeferredMethod;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.prebind.LazyMethodWrapper;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.runtime.OpenLInvocationHandler;
import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.MethodDelegator;
import org.openl.vm.IRuntimeEnv;

public final class WrapperLogic {

    private WrapperLogic() {
    }

    public static IOpenMethod getTopClassMethod(IOpenMethodWrapper wrapper, IRuntimeEnv env) {
        SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = extractSimpleRulesRuntimeEnv(env);
        IOpenClass topClass = simpleRulesRuntimeEnv.getTopClass();
        if (topClass != null && topClass != wrapper.getXlsModuleOpenClass()) {
            IOpenMethod method = wrapper.getTopOpenClassMethod(topClass);
            if (method != null) {
                method = extractMethod(simpleRulesRuntimeEnv, method);
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

    private static IOpenMethod extractMethod(SimpleRulesRuntimeEnv simpleRulesRuntimeEnv, IOpenMethod method) {
        while (method instanceof LazyMethodWrapper || method instanceof MethodDelegator) {
            if (method instanceof LazyMethodWrapper) {
                method = ((LazyMethodWrapper) method).getCompiledMethod(simpleRulesRuntimeEnv);
            }
            if (method instanceof MethodDelegator) {
                MethodDelegator methodDelegator = (MethodDelegator) method;
                method = methodDelegator.getMethod();
            }
        }
        return method;
    }

    public static IOpenMethod wrapOpenMethod(final IOpenMethod openMethod, final XlsModuleOpenClass module) {
        if (openMethod instanceof IOpenMethodWrapper || openMethod instanceof TestSuiteMethod) {
            return openMethod;
        }

        ContextPropertiesInjector contextPropertiesInjector = new ContextPropertiesInjector(
            openMethod.getSignature().getParameterTypes(),
            module.getRulesModuleBindingContext());

        if (openMethod instanceof OverloadedMethodsDispatcherTable) {
            return new OverloadedMethodsDispatcherTableWrapper(module,
                (OverloadedMethodsDispatcherTable) openMethod,
                contextPropertiesInjector);
        }
        if (openMethod instanceof MatchingOpenMethodDispatcher) {
            return new MatchingOpenMethodDispatcherWrapper(module,
                (MatchingOpenMethodDispatcher) openMethod,
                contextPropertiesInjector);
        }
        if (openMethod instanceof DeferredMethod) {
            return new DeferredMethodWrapper(module, (DeferredMethod) openMethod, contextPropertiesInjector);
        }
        if (openMethod instanceof CompositeMethod) {
            return new CompositeMethodWrapper(module, (CompositeMethod) openMethod, contextPropertiesInjector);
        }
        if (openMethod instanceof Algorithm) {
            return new AlgorithmWrapper(module, (Algorithm) openMethod, contextPropertiesInjector);
        }
        if (openMethod instanceof AlgorithmSubroutineMethod) {
            return new AlgorithmSubroutineMethodWrapper(module,
                (AlgorithmSubroutineMethod) openMethod,
                contextPropertiesInjector);
        }
        if (openMethod instanceof DecisionTable) {
            return new DecisionTable2Wrapper(module, (DecisionTable) openMethod, contextPropertiesInjector);
        }
        if (openMethod instanceof ColumnMatch) {
            return new ColumnMatchWrapper(module, (ColumnMatch) openMethod, contextPropertiesInjector);
        }
        if (openMethod instanceof Spreadsheet) {
            return new SpreadsheetWrapper(module, (Spreadsheet) openMethod, contextPropertiesInjector);
        }
        if (openMethod instanceof TableMethod) {
            return new TableMethodWrapper(module, (TableMethod) openMethod, contextPropertiesInjector);
        }
        return openMethod;
    }

    public static Object invoke(IOpenMethodWrapper wrapper, Object target, Object[] params, IRuntimeEnv env) {
        SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = extractSimpleRulesRuntimeEnv(env);
        IOpenClass topClass = simpleRulesRuntimeEnv.getTopClass();
        if (topClass == null) {
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                IOpenClass typeClass;
                if (target instanceof IDynamicObject) {
                    IDynamicObject dynamicObject = (IDynamicObject) target;
                    typeClass = dynamicObject.getType();
                } else if (java.lang.reflect.Proxy.isProxyClass(target.getClass())) {
                    java.lang.reflect.InvocationHandler invocationHandler = java.lang.reflect.Proxy
                        .getInvocationHandler(target);
                    if (invocationHandler instanceof OpenLInvocationHandler) {
                        OpenLInvocationHandler openLInvocationHandler = (OpenLInvocationHandler) invocationHandler;
                        Object openlInstance = openLInvocationHandler.getInstance();
                        if (openlInstance instanceof IDynamicObject) {
                            IDynamicObject dynamicObject = (IDynamicObject) openlInstance;
                            typeClass = dynamicObject.getType();
                        } else {
                            throw new IllegalStateException("Cannot define openl class from target object.");
                        }
                    } else {
                        throw new IllegalStateException("Cannot define openl class from target object.");
                    }
                } else {
                    throw new IllegalStateException("Cannot define openl class from target object.");
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
                    method = extractMethod(simpleRulesRuntimeEnv, method);
                    if (method != wrapper) {
                        return method.invoke(target, params, env);
                    }
                }
            }
        }
        return wrapper.invokeDelegate(target, params, env, simpleRulesRuntimeEnv);
    }
}
