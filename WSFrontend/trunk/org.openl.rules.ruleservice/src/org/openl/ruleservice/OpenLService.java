package org.openl.ruleservice;

import java.util.List;

import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.model.Module;

public class OpenLService {
    private String name;
    private String url;
    private List<Module> modules;
    private String serviceClassName;
    private RulesInstantiationStrategy instantiationStrategy;
    private Class<?> serviceClass;
    private Object serviceBean;

    public OpenLService(String name, String url, List<Module> modules, String serviceClassName) {
        this.name = name;
        this.url = url;
        this.modules = modules;
        this.serviceClassName = serviceClassName;
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

    public RulesInstantiationStrategy getInstantiationStrategy() {
        return instantiationStrategy;
    }

    public void setInstantiationStrategy(RulesInstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
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
