package org.openl.rules.ruleservice.databinding;

import java.util.Objects;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public abstract class ServiceDescriptionConfigurationFactoryBean<T> extends AbstractFactoryBean<T> {
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

    public final ServiceDescription getServiceDescription() throws Exception {
        if (serviceDescription == null) {
            serviceDescription = ServiceDescriptionHolder.getInstance().getServiceDescription();
            Objects.requireNonNull(serviceDescription, "Failed to locate service description.");
        }
        return serviceDescription;
    }

    protected Object getValue(String property) throws Exception {
        return getServiceDescription().getConfiguration() == null ? null
                                                                  : getServiceDescription().getConfiguration()
                                                                      .get(property);
    }

}
