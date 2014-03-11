package org.openl.rules.ruleservice.publish.cache;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.instantiation.SingleModuleInstantiationStrategy;
import org.openl.rules.project.model.Module;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

/**
 * Lazy IOpenMember that contains info about module where it was declared. When
 * we try to do some operations with lazy member it will compile module and wrap
 * the compiled member.
 * 
 * @author PUdalau
 */
public abstract class LazyMember<T extends IOpenMember> implements IOpenMember {
    private IDependencyManager dependencyManager;
    private boolean executionMode;
    private T original;
    private static Object lock = new Object();
    /**
     * ClassLoader used in "lazy" compilation. It should be reused because it
     * contains generated classes for datatypes.(If we use different
     * ClassLoaders we can get ClassCastException because generated classes for
     * datatypes have been loaded by different ClassLoaders).
     */
    private ClassLoader classLoader;

    public LazyMember(IDependencyManager dependencyManager,
			boolean executionMode, ClassLoader classLoader, T original) {
		this.dependencyManager = dependencyManager;
		this.executionMode = executionMode;
		this.classLoader = classLoader;
		this.original = original;
	}
    
    protected abstract T getMember(SingleModuleInstantiationStrategy strategy);

	/**
     * Compiles method declaring the member and returns it.
     * 
     * @return Real member in compiled module.
     */
    protected final T getMember(IRuntimeEnv env) {
        final Module module = getModule(env);
        SingleModuleInstantiationStrategy strategy = getCache().getRulesInstantiationStrategyFromCache(module);
        if (strategy == null) {
            strategy = RulesInstantiationStrategyFactory.getStrategy(module, isExecutionMode(), getDependencyManager(),
                    getClassLoader());
            synchronized (getCache()) {
                SingleModuleInstantiationStrategy strategy1 = getCache().getRulesInstantiationStrategyFromCache(module);
                if (strategy1 == null) {
                    getCache().putToCache(module, strategy);
                } else {
                    strategy = strategy1;
                }
                //strategy.setExternalParameters(getExternalParameters());
            }
        }
        synchronized (lock) {
            return getMember(strategy);
        }
    }

    protected ModulesCache getCache() {
        return ModulesCache.getInstance();
    }

    /**
     * @return Module containing current member.
     */
    public abstract Module getModule(IRuntimeEnv env);

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
