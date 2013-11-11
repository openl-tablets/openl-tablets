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
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * Lazy field that will compile module declaring it and will get real field to
 * do operations with it.
 * 
 * @author PUdalau
 */
public abstract class LazyField extends LazyMember<IOpenField> implements IOpenField {
    private String fieldName;

    public LazyField(IOpenClass lazyOpenClass, String fieldName, IDependencyManager dependencyManager, boolean executionMode,
            ClassLoader classLoader, IOpenField original, Map<String, Object> externalParameters) {
        super(lazyOpenClass, dependencyManager, executionMode, classLoader, original, externalParameters);
        this.fieldName = fieldName;
    }

    public IOpenField getMemberForModule(Module module) {
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
            return compiledOpenClass.getOpenClass().getField(fieldName);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to load lazy field.", e);
        } finally {
            LazyBinderInvocationHandler.setPrebindHandler(prebindHandler);
            if (getDependencyManager() instanceof RuleServiceDeploymentRelatedDependencyManager){
                RuleServiceDeploymentRelatedDependencyManager.setCurrentClassCompilation(openClass);
            }
        }
    }

    public Object get(Object target, IRuntimeEnv env) {
        return getMember(env).get(target, env);
    }

    public void set(Object target, Object value, IRuntimeEnv env) {
        getMember(env).set(target, value, env);
    }

    public boolean isConst() {
        return getOriginal().isConst();
    }

    public boolean isReadable() {
        return getOriginal().isReadable();
    }

    public boolean isWritable() {
        return getOriginal().isWritable();
    }

}
