package org.openl.rules.ruleservice.databinding;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.OpenLServiceHolder;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public abstract class ServiceConfigurationFactoryBean<T> extends AbstractFactoryBean<T> {
    private T defaultValue;

    @Override
    public boolean isSingleton() {
        return false;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    private ServiceDescription serviceDescription;
    private OpenLService openLService;

    public final ServiceDescription getServiceDescription() throws ServiceConfigurationException {
        if (serviceDescription == null) {
            serviceDescription = ServiceDescriptionHolder.getInstance().get();
            if (serviceDescription == null) {
                throw new ServiceConfigurationException("Failed to locate a service description.");
            }
        }
        return serviceDescription;
    }

    public final OpenLService getOpenLService() throws ServiceConfigurationException {
        if (openLService == null) {
            openLService = OpenLServiceHolder.getInstance().get();
            if (openLService == null) {
                throw new ServiceConfigurationException("Failed to locate a service.");
            }
        }
        return openLService;
    }

    protected Object getValue(String property) throws Exception {
        return getServiceDescription().getConfiguration() == null ? null
                                                                  : getServiceDescription().getConfiguration()
                                                                      .get(property);
    }

}
