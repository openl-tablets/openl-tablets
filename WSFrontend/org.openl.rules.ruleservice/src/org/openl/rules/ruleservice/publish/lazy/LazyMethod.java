package org.openl.rules.ruleservice.publish.lazy;

import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.OpenClassHelper;
import org.openl.vm.IRuntimeEnv;

/**
 * Lazy method that will return real object from dependency manager. Dependency
 * Manager is responsible for loading/unloading modules.
 * 
 * @author PUdalau, Marat Kamalov
 */
public abstract class LazyMethod extends LazyMember<IOpenMethod> implements IOpenMethod {
    private String methodName;

    private Class<?>[] argTypes;

    public LazyMethod(String methodName,
            Class<?>[] argTypes,
            IOpenMethod original,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        super(dependencyManager, executionMode, classLoader, original, externalParameters);
        this.methodName = methodName;
        this.argTypes = argTypes;
    }

    protected IOpenMethod getMemberForModule(DeploymentDescription deployment, Module module) {
        try {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClass(deployment, module);
            IOpenClass[] argOpenTypes = OpenClassHelper.getOpenClasses(compiledOpenClass.getOpenClass(), argTypes);
            return compiledOpenClass.getOpenClass().getMatchingMethod(methodName, argOpenTypes);
        } catch (Exception e) {
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
