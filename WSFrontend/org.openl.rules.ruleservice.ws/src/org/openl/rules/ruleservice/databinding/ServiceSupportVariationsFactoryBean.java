package org.openl.rules.ruleservice.databinding;

import java.util.Objects;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class ServiceSupportVariationsFactoryBean extends AbstractFactoryBean<Boolean> {

    private ServiceDescription serviceDescription;

    public ServiceDescription getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(ServiceDescription serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    @Override
    public Class<?> getObjectType() {
        return Boolean.class;
    }

    @Override
    protected Boolean createInstance() {
        ServiceDescription serviceDescription = getServiceDescription();
        Objects.requireNonNull(serviceDescription, "Failed to locate service description.");
        return serviceDescription.isProvideVariations();
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
