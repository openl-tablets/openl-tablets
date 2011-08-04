package org.openl.rules.ruleservice.publish.cache;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.ModulesCache;
import org.openl.rules.project.model.Module;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;

/**
 * Lazy IOpenMember that contains info about module where it was declared. When
 * we try to do some operations with lazy member it will compile module and wrap
 * the compiled member.
 * 
 * @author PUdalau
 */
public abstract class LazyMember<T extends IOpenMember> implements IOpenMember {
    private ModulesCache cache;
    private Module module;
    private IDependencyManager dependencyManager;
    private boolean executionMode;

    public LazyMember(ModulesCache cache, Module module, IDependencyManager dependencyManager, boolean executionMode) {
        this.cache = cache;
        this.module = module;
        this.dependencyManager = dependencyManager;
        this.executionMode = executionMode;
    }

    /**
     * Compiles method declaring the member and returns it.
     * 
     * @return Real member in compiled module.
     */
    public abstract T getMember();

    protected ModulesCache getCache() {
        return cache;
    }

    protected Module getModule() {
        return module;
    }

    protected IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    protected boolean isExecutionMode() {
        return executionMode;
    }

    public String getDisplayName(int mode) {
        return getMember().getDisplayName(mode);
    }

    public String getName() {
        return getMember().getName();
    }

    public IOpenClass getType() {
        return getMember().getType();
    }

    public boolean isStatic() {
        return getMember().isStatic();
    }

    public IMemberMetaInfo getInfo() {
        return getMember().getInfo();
    }

    public IOpenClass getDeclaringClass() {
        return getMember().getDeclaringClass();
    }
}
