package org.openl.rules.activiti;

import java.io.File;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.el.Expression;
import org.activiti.engine.repository.ProcessDefinition;
import org.openl.rules.activiti.util.ReflectionUtils;
import org.openl.rules.activiti.util.ResourceUtils;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

public abstract class AbstractOpenLResourceServiceTask<T> implements JavaDelegate {

    protected Expression provideRuntimeContext;
    protected Expression module;
    protected Expression resource;

    protected Class<T> interfaceClass;

    public AbstractOpenLResourceServiceTask() {
        initInterfaceClass();
    }

    public AbstractOpenLResourceServiceTask(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    @SuppressWarnings("unchecked")
    private void initInterfaceClass() {
        interfaceClass = ReflectionUtils.getGenericParameterClass(this.getClass(), 0);
    }

    private volatile SimpleProjectEngineFactory<T> simpleProjectEngineFactory;
    private volatile T instance;

    protected IRulesRuntimeContext buildRuntimeContext(DelegateExecution execution) {
        return new DefaultRulesRuntimeContext();
    }

    protected boolean isProvideRuntimeContext(DelegateExecution execution) {
        boolean isProvideRuntimeContext = false;
        if (provideRuntimeContext != null) {
            Object isProvideRuntimeContextValue = provideRuntimeContext.getValue(execution);
            if (isProvideRuntimeContextValue instanceof String) {
                isProvideRuntimeContext = Boolean.valueOf((String) isProvideRuntimeContextValue);
            }
            if (isProvideRuntimeContextValue instanceof Boolean) {
                isProvideRuntimeContext = ((Boolean) isProvideRuntimeContextValue).booleanValue();
            }
        }
        return isProvideRuntimeContext;
    }

    private synchronized SimpleProjectEngineFactory<T> initSimpleProjectEngineFactory(
            DelegateExecution execution) throws Exception {
        String processDefinitionId = execution.getProcessDefinitionId();
        String resourceValue = (String) resource.getValue(execution);
        RepositoryService repositoryService = execution.getEngineServices().getRepositoryService();
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);

        File projectWorkspace = ResourceUtils.prepareDeploymentOpenLResource(processDefinition.getDeploymentId(),
            resourceValue);

        boolean isProvideRuntimeContext = isProvideRuntimeContext(execution);

        SimpleProjectEngineFactoryBuilder<T> simpleProjectEngineFactoryBuilder = new SimpleProjectEngineFactoryBuilder<T>()
            .setExecutionMode(true)
            .setProject(projectWorkspace.getCanonicalPath())
            .setWorkspace(projectWorkspace.getCanonicalPath())
            .setProvideRuntimeContext(isProvideRuntimeContext);

        if (interfaceClass != null && !interfaceClass.equals(Object.class)) {
            simpleProjectEngineFactoryBuilder.setInterfaceClass(interfaceClass);
        }

        if (module != null) {
            String moduleValue = (String) module.getValue(execution);
            if (moduleValue != null) {
                simpleProjectEngineFactoryBuilder.setModule(moduleValue);
            }
        }

        return simpleProjectEngineFactoryBuilder.build();
    }

    protected final SimpleProjectEngineFactory<T> getSimpleProjectEngineFactory(
            DelegateExecution execution) throws Exception {
        if (simpleProjectEngineFactory == null) {
            synchronized (this) {
                if (simpleProjectEngineFactory == null) {
                    simpleProjectEngineFactory = initSimpleProjectEngineFactory(execution);
                }
            }
        }
        return simpleProjectEngineFactory;
    }

    protected final T getInstance(DelegateExecution execution) throws Exception {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    SimpleProjectEngineFactory<T> simpleProjectEngineFactory = getSimpleProjectEngineFactory(execution);
                    instance = simpleProjectEngineFactory.newInstance();
                }
            }
        }

        return instance;
    }

    protected final Class<?> getInterfaceClass(DelegateExecution execution) throws Exception {
        if (interfaceClass == null) {
            return getSimpleProjectEngineFactory(execution).getInterfaceClass();
        } else {
            return interfaceClass;
        }
    }
}
