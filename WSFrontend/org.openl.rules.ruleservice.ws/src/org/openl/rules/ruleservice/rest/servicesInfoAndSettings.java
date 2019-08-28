package org.openl.rules.ruleservice.rest;

import org.openl.rules.ruleservice.servlet.ServiceInfo;

import java.util.Collection;

public class servicesInfoAndSettings {

    private Collection<ServiceInfo> serviceInfo;

    private ConfigInfoBean config;

    public servicesInfoAndSettings(Collection<ServiceInfo> serviceInfo, ConfigInfoBean config) {
        this.serviceInfo = serviceInfo;
        this.config = config;
    }

    public Collection<ServiceInfo> getServiceInfo() {
        return serviceInfo;
    }

    public ConfigInfoBean getConfig() {
        return config;
    }

}
