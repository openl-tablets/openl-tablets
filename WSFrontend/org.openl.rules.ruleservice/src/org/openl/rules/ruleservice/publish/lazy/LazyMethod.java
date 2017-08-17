package org.openl.rules.ruleservice.publish.lazy;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.lang.xls.prebind.LazyMethodWrapper;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.method.TableUriMethod;
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
public abstract class LazyMethod extends LazyMember<IOpenMethod> implements IOpenMethod, TableUriMethod, LazyMethodWrapper {
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

    protected CompiledOpenClass lastCompiledOpenClass;
    protected IOpenMethod lastOpenMethod;
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock(); 
    
    protected IOpenMethod getMemberForModule(DeploymentDescription deployment, Module module) {
        try {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClass(deployment, module);
            if (compiledOpenClass.hasErrors()){
            	compiledOpenClass.throwErrorExceptionsIfAny();
            }
            readWriteLock.readLock().lock();
            try{
                if (lastCompiledOpenClass == compiledOpenClass){
                    return lastOpenMethod;
                }
            }finally{
                readWriteLock.readLock().unlock();
            }
            readWriteLock.writeLock().lock();
            try{
                lastCompiledOpenClass = compiledOpenClass;
                IOpenClass[] argOpenTypes = OpenClassHelper.getOpenClasses(compiledOpenClass.getOpenClass(), argTypes);
                lastOpenMethod = compiledOpenClass.getOpenClass().getMethod(methodName, argOpenTypes);
                return lastOpenMethod;
            }finally{
                readWriteLock.writeLock().unlock();
            }
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to load lazy method", e);
        }
    }

    public IMethodSignature getSignature() {
        return getOriginal().getSignature();
    }
    
    @Override
    public IOpenMethod getCompiledMethod(IRuntimeEnv env) {
        return getMember(env);
    }

    @Override
    public String getTableUri() {
        if (getOriginal() instanceof ExecutableRulesMethod) {
            return ((ExecutableRulesMethod) getOriginal()).getTableUri();
        } else {
            throw new IllegalStateException("Implementation doesn't support methods other than ExecutableRulesMethod!");
        }
    }

    public IOpenMethod getMethod() {
        return this;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return getMember(env).invoke(target, params, env);
    }
}
