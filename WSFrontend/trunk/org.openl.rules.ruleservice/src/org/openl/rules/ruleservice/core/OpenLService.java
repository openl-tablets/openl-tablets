package org.openl.rules.ruleservice.core;

import java.util.List;

import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesServiceEnhancer;
import org.openl.rules.project.model.Module;

public class OpenLService {
    private String name;
    
    private String url;

    private String serviceClassName;
    
    private RulesInstantiationStrategy instantiationStrategy;

    private RulesServiceEnhancer enhancer;
    
    private Class<?> serviceClass;
    
    private Object serviceBean;
    
    private boolean provideRuntimeContext;
    
    private List<Module> modules;

    public OpenLService(String name, String url, List<Module> modules, String serviceClassName,
            boolean provideRuntimeContext) {
        this.name = name;
        this.url = url;
        this.modules = modules;
        this.serviceClassName = serviceClassName;
        this.provideRuntimeContext = provideRuntimeContext;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public List<Module> getModules() {
        return modules;
    }

    public String getServiceClassName() {
        return serviceClassName;
    }

    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    public RulesInstantiationStrategy getInstantiationStrategy() {
        return instantiationStrategy;
    }

    public void setInstantiationStrategy(RulesInstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }

    public RulesServiceEnhancer getEnhancer() {
        return enhancer;
    }

    public void setEnhancer(RulesServiceEnhancer enhancer) {
        this.enhancer = enhancer;
    }

    public Class<?> getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    public Object getServiceBean() {
        return serviceBean;
    }

    public void setServiceBean(Object serviceBean) {
        this.serviceBean = serviceBean;
    }
}
