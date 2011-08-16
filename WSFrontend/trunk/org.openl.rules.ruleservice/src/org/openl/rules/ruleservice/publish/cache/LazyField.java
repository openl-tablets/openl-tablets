package org.openl.rules.ruleservice.publish.cache;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * Lazy field that will compile module declaring it and will get real field to
 * do operations with it.
 * 
 * @author PUdalau
 */
public class LazyField extends LazyMember<IOpenField> implements IOpenField {
    private String fieldName;

    public LazyField(String fieldName, Module module, IDependencyManager dependencyManager,
            boolean executionMode) {
        super(module, dependencyManager, executionMode);
        this.fieldName = fieldName;
    }

    public IOpenField getMember() {
        try {
            CompiledOpenClass compiledOpenClass = getCache().getInstantiationStrategy(getModule(), isExecutionMode(),
                    getDependencyManager()).compile(ReloadType.NO);
            return compiledOpenClass.getOpenClass().getField(fieldName);
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
        return getMember().isConst();
    }

    public boolean isReadable() {
        return getMember().isReadable();
    }

    public boolean isWritable() {
        return getMember().isWritable();
    }

}
