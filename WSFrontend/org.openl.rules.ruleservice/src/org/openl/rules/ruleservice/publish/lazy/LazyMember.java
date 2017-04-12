package org.openl.rules.ruleservice.publish.lazy;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

/**
 * Lazy IOpenMember that contains info about module where it was declared. When
 * we try to do some operations with lazy member it will compile module and wrap
 * the compiled member.
 *
 * @author Marat Kamalov
 */
public abstract class LazyMember<T extends IOpenMember> implements IOpenMember {
    private final Logger log = LoggerFactory.getLogger(LazyMember.class);

    private IDependencyManager dependencyManager;
    private boolean executionMode;
    private T original;
    private Map<String, Object> externalParameters;
    private CompiledOpenClass compiledOpenClass;
    
    /**
     * ClassLoader used in "lazy" compilation. It should be reused because it
     * contains generated classes for datatypes.(If we use different
     * ClassLoaders we can get ClassCastException because generated classes for
     * datatypes have been loaded by different ClassLoaders).
     */
    private ClassLoader classLoader;

    public LazyMember(IDependencyManager dependencyManager,
                      boolean executionMode,
                      ClassLoader classLoader,
                      T original,
                      Map<String, Object> externalParameters) {
        this.dependencyManager = dependencyManager;
        this.executionMode = executionMode;
        this.classLoader = classLoader;
        this.original = original;
        this.externalParameters = externalParameters;
    }

    protected abstract T getMemberForModule(DeploymentDescription deployment, Module module);

    protected CompiledOpenClass getCompiledOpenClass(final DeploymentDescription deployment, final Module module) throws Exception {
        Dependency dependency = new Dependency(DependencyType.MODULE, new IdentifierNode(null,
                null,
                module.getName(),
                null));
        String dependencyName = dependency.getNode().getIdentifier();
        if (compiledOpenClass == null){
            synchronized (this) {
                if (compiledOpenClass == null){
                    compiledOpenClass = CompiledOpenClassCache.getInstance().get(deployment, dependencyName);
                }
            }
        }
        if (compiledOpenClass != null) {
            return compiledOpenClass;
        }
        synchronized (CompiledOpenClassCache.getInstance()) {
            compiledOpenClass = CompiledOpenClassCache.getInstance().get(deployment, dependencyName);
            if (compiledOpenClass != null) {
                return compiledOpenClass;
            }
            IPrebindHandler prebindHandler = LazyBinderInvocationHandler.getPrebindHandler();
            try {
                LazyBinderInvocationHandler.removePrebindHandler();
                RulesInstantiationStrategy rulesInstantiationStrategy = RulesInstantiationStrategyFactory.getStrategy(module,
                        true,
                        getDependencyManager(),
                        getClassLoader());
                rulesInstantiationStrategy.setServiceClass(EmptyInterface.class);// Prevent
                // generation
                // interface
                // and
                // Virtual
                // module
                // double
                // (instantiate
                // method).
                // Improve
                // performance.
                Map<String, Object> parameters = ProjectExternalDependenciesHelper.getExternalParamsWithProjectDependencies(dependencyManager.getExternalParameters(),
                        new ArrayList<Module>() {
                            private static final long serialVersionUID = 1L;

                            {
                                add(module);
                            }
                        });
                rulesInstantiationStrategy.setExternalParameters(parameters);
                compiledOpenClass = rulesInstantiationStrategy.compile();
                CompiledOpenClassCache.getInstance().putToCache(deployment, dependencyName, compiledOpenClass);
                if (log.isDebugEnabled()){
                    log.debug("CompiledOpenClass for deploymentName='{}', deploymentVersion='{}', dependencyName='{}' was stored to cache.", deployment.getName(), deployment.getVersion().getVersionName(), dependencyName);
                }
                return compiledOpenClass;
            } catch (Exception ex) {
                OpenLMessagesUtils.addError("Failed to load dependency '" + dependencyName + "'.");
                return compiledOpenClass;
            } finally {
                LazyBinderInvocationHandler.setPrebindHandler(prebindHandler);
            }
        }
    }

    /**
     * Compiles method declaring the member and returns it.
     *
     * @return Real member in compiled module.
     */
    protected final T getMember(IRuntimeEnv env) {
        final Module module = getModule(env);
        final DeploymentDescription deployment = getDeployment(env);
        return getMemberForModule(deployment, module);
    }

    /**
     * @return Module containing current member.
     */
    public abstract Module getModule(IRuntimeEnv env);

    /**
     * @return Deployment containing current module.
     */
    public abstract DeploymentDescription getDeployment(IRuntimeEnv env);

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

    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

    public static interface EmptyInterface {
    }
}
