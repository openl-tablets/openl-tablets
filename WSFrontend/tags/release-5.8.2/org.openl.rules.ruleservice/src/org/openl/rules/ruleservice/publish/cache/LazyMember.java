package org.openl.rules.ruleservice.publish.cache;

import org.openl.dependency.IDependencyManager;
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
    private Module module;
    private IDependencyManager dependencyManager;
    private boolean executionMode;
    private T original;
    /**
     * ClassLoader used in "lazy" compilation. It should be reused because it
     * contains generated classes for datatypes.(If we use different
     * ClassLoaders we can get ClassCastException because generated classes for
     * datatypes have been loaded by different ClassLoaders).
     */
    private ClassLoader classLoader;

    public LazyMember(Module module, IDependencyManager dependencyManager,
			boolean executionMode, ClassLoader classLoader, T original) {
		this.module = module;
		this.dependencyManager = dependencyManager;
		this.executionMode = executionMode;
		this.classLoader = classLoader;
		this.original = original;
	}

	/**
     * Compiles method declaring the member and returns it.
     * 
     * @return Real member in compiled module.
     */
    public abstract T getMember();

    protected ModulesCache getCache() {
        return ModulesCache.getInstance();
    }

    /**
     * @return Module containing current member.
     */
    protected Module getModule() {
        return module;
    }

    /**
     * @return DependencyManager used for lazy compiling. 
     */
    protected IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    protected boolean isExecutionMode() {
        return executionMode;
    }
    
    /**
     * @return ClassLoader used for lazy compiling. 
     */
    public ClassLoader getClassLoader() {
		return classLoader;
	}

    public T getOriginal() {
        return original;
    }

    public String getDisplayName(int mode) {
        return original.getDisplayName(mode);
    }

    public String getName() {
        return original.getName();
    }

    public IOpenClass getType() {
        return original.getType();
    }

    public boolean isStatic() {
        return original.isStatic();
    }

    public IMemberMetaInfo getInfo() {
        return original.getInfo();
    }

    public IOpenClass getDeclaringClass() {
        return original.getDeclaringClass();
    }
}
