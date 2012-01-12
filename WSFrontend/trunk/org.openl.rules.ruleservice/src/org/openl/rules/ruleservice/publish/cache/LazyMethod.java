package org.openl.rules.ruleservice.publish.cache;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
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
public class LazyMethod extends LazyMember<IOpenMethod> implements IOpenMethod {
    private String methodName;

    private Class<?>[] argTypes;

    public LazyMethod(String methodName, Class<?>[] argTypes, Module module, IDependencyManager dependencyManager,
            boolean executionMode, ClassLoader classLoader, IOpenMethod original) {
        super(module, dependencyManager, executionMode, classLoader, original);
        this.methodName = methodName;
        this.argTypes = argTypes;
    }

    public IOpenMethod getMember() {
        try {
            CompiledOpenClass compiledOpenClass = getCache().getInstantiationStrategy(getModule(),
                isExecutionMode(),
                getDependencyManager(),
                getClassLoader()).compile(ReloadType.NO);
            IOpenClass[] argOpenTypes = OpenClassHelper.getOpenClasses(compiledOpenClass.getOpenClass(), argTypes);
            return compiledOpenClass.getOpenClass().getMatchingMethod(methodName, argOpenTypes);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to load lazy field.", e);
        }
    }

    public IMethodSignature getSignature() {
        return getOriginal().getSignature();
    }

    public IOpenMethod getMethod() {
        return this;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return getMember().invoke(target, params, env);
    }

}
