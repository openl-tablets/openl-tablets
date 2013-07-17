package org.openl.rules.ruleservice.publish.cache;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
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

    public LazyMethod(String methodName, Class<?>[] argTypes, IDependencyManager dependencyManager,
            boolean executionMode, ClassLoader classLoader, IOpenMethod original) {
        super(dependencyManager, executionMode, classLoader, original);
        this.methodName = methodName;
        this.argTypes = argTypes;
    }

    protected IOpenMethod getMember(SingleModuleInstantiationStrategy strategy) {
        try {
            CompiledOpenClass compiledOpenClass = strategy.compile();
            IOpenClass[] argOpenTypes = OpenClassHelper.getOpenClasses(compiledOpenClass.getOpenClass(), argTypes);
            return compiledOpenClass.getOpenClass().getMatchingMethod(methodName, argOpenTypes);
        } catch (RulesInstantiationException e) {
            throw new OpenlNotCheckedException("Failed to load lazy method", e);
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
