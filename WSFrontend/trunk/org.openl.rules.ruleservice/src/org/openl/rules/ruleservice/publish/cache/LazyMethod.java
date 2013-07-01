package org.openl.rules.ruleservice.publish.cache;

import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.SingleModuleInstantiationStrategy;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.OpenClassHelper;
import org.openl.vm.IRuntimeEnv;

/**
 * Lazy method that will compile module declaring it and will get real method to
 * do operations with it.
 * 
 * @author PUdalau
 */
public abstract class LazyMethod extends LazyMember<IOpenMethod> implements IOpenMethod {
    private String methodName;

    private Class<?>[] argTypes;
    private ModuleOpenClass topModule;

    public ModuleOpenClass getTopModule() {
        return topModule;
    }

    public void setTopModule(ModuleOpenClass topModule) {
        this.topModule = topModule;
    }

    public LazyMethod(String methodName, Class<?>[] argTypes, IDependencyManager dependencyManager,
            boolean executionMode, ClassLoader classLoader, IOpenMethod original, Map<String, Object> externalParameters) {
        super(dependencyManager, executionMode, classLoader, original, externalParameters);
        this.methodName = methodName;
        this.argTypes = argTypes;
    }

    protected IOpenMethod getMember(SingleModuleInstantiationStrategy strategy) {
        try {
            XlsModuleOpenClass.setTopOpenClass(topModule);
            CompiledOpenClass compiledOpenClass = strategy.compile();
            IOpenClass[] argOpenTypes = OpenClassHelper.getOpenClasses(compiledOpenClass.getOpenClass(), argTypes);
            return compiledOpenClass.getOpenClass().getMatchingMethod(methodName, argOpenTypes);
        } catch (RulesInstantiationException e){
            throw new OpenlNotCheckedException("Failed to load lazy method", e);
        } finally {
            XlsModuleOpenClass.setTopOpenClass(null); // prevent memory
                                                      // leak
        }
    }

    public IMethodSignature getSignature() {
        return getOriginal().getSignature();
    }

    public IOpenMethod getMethod() {
        return this;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return getMember(env).invoke(target, params, env);
    }

}
