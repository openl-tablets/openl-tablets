package org.openl.rules.ruleservice.publish.lazy;

import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * Lazy field that will compile module declaring it and will get real field to
 * do operations with it.
 * 
 * @author PUdalau, Marat Kamalov
 */
public abstract class LazyField extends LazyMember<IOpenField> implements IOpenField {
    private String fieldName;

    private LazyField(String fieldName,
            IOpenField original,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        super(dependencyManager, executionMode, classLoader, original, externalParameters);
        this.fieldName = fieldName;
    }

    public static final LazyField getLazyField(final DeploymentDescription deployment,
            final Module module,
            IOpenField original,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        LazyField lazyField = new LazyField(original.getName(),
            original,
            dependencyManager,
            classLoader,
            executionMode,
            externalParameters) {
            @Override
            public DeploymentDescription getDeployment() {
                return deployment;
            }

            @Override
            public Module getModule() {
                return module;
            }
        };
        CompiledOpenClassCache.getInstance().registerLazyMember(lazyField);
        return lazyField;
    }

    public IOpenField getMember() {
        IOpenField cachedMember = getCachedMember();
        if (cachedMember != null) {
            return cachedMember;
        }

        try {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClassWithThrowErrorExceptionsIfAny();
            IOpenField openField = compiledOpenClass.getOpenClass().getField(fieldName);
            setCachedMember(openField);
            return openField;
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to load lazy field.", e);
        }
    }

    public Object get(Object target, IRuntimeEnv env) {
        return getMember().get(target, env);
    }

    public void set(Object target, Object value, IRuntimeEnv env) {
        getMember().set(target, value, env);
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
