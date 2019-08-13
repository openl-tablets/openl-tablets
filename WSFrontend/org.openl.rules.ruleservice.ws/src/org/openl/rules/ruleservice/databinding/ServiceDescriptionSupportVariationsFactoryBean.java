package org.openl.rules.ruleservice.databinding;

import java.util.Objects;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class ServiceDescriptionSupportVariationsFactoryBean extends AbstractFactoryBean<Boolean> {

    @Override
    public Class<?> getObjectType() {
        return Boolean.class;
    }

    @Override
    protected Boolean createInstance() throws Exception {
        ServiceDescription serviceDescription = ServiceDescriptionHolder.getInstance().get();
        Objects.requireNonNull(serviceDescription, "Failed to locate service description.");
        return serviceDescription.isProvideVariations();
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
