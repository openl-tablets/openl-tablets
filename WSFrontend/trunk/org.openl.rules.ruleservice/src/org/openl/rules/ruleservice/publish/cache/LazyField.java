package org.openl.rules.ruleservice.publish.cache;

import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.instantiation.SingleModuleInstantiationStrategy;
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

    public LazyField(String fieldName, IDependencyManager dependencyManager,
            boolean executionMode, ClassLoader classLoader, IOpenField original, Map<String, Object> externalParameters) {
        super(dependencyManager, executionMode, classLoader, original, externalParameters);
        this.fieldName = fieldName;
    }

    public IOpenField getMember(IRuntimeEnv env) {
        try {
            SingleModuleInstantiationStrategy instantiationStrategy = getCache().getInstantiationStrategy(getModule(env), isExecutionMode(),
                    getDependencyManager(), getClassLoader());
            instantiationStrategy.setExternalParameters(getExternalParameters());
            CompiledOpenClass compiledOpenClass = instantiationStrategy.compile();
            return compiledOpenClass.getOpenClass().getField(fieldName);
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to load lazy field.", e);
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
