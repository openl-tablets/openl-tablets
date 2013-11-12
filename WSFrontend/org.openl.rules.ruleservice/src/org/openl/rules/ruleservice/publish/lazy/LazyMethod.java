package org.openl.rules.ruleservice.publish.lazy;

import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.RuleServiceDeploymentRelatedDependencyManager;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.impl.IdentifierNode;
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
    

    public LazyMethod(IOpenClass lazyOpenClass, String methodName, Class<?>[] argTypes, IDependencyManager dependencyManager,
            boolean executionMode, ClassLoader classLoader, IOpenMethod original, Map<String, Object> externalParameters) {
        super(lazyOpenClass, dependencyManager, executionMode, classLoader, original, externalParameters);
        this.methodName = methodName;
        this.argTypes = argTypes;
    }

    protected IOpenMethod getMemberForModule(Module module) {
        IPrebindHandler prebindHandler = LazyBinderInvocationHandler.getPrebindHandler();
        IOpenClass openClass = null;
        if (getDependencyManager() instanceof RuleServiceDeploymentRelatedDependencyManager){
            openClass = RuleServiceDeploymentRelatedDependencyManager.getCurrentClassCompilation();
        }
        try {
            LazyBinderInvocationHandler.removePrebindHandler();
            if (getDependencyManager() instanceof RuleServiceDeploymentRelatedDependencyManager){
                RuleServiceDeploymentRelatedDependencyManager.setCurrentClassCompilation(getLazyOpenClass());
            }
            CompiledDependency compiledDependency = getDependencyManager().loadDependency(
                    new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, module.getName(), null)));
            CompiledOpenClass compiledOpenClass = compiledDependency.getCompiledOpenClass();
            IOpenClass[] argOpenTypes = OpenClassHelper.getOpenClasses(compiledOpenClass.getOpenClass(), argTypes);
            return compiledOpenClass.getOpenClass().getMatchingMethod(methodName, argOpenTypes);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to load lazy method", e);
        } finally {
            LazyBinderInvocationHandler.setPrebindHandler(prebindHandler);
            if (getDependencyManager() instanceof RuleServiceDeploymentRelatedDependencyManager){
                RuleServiceDeploymentRelatedDependencyManager.setCurrentClassCompilation(openClass);
            }
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
