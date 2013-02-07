package org.openl.rules.ruleservice.databinding;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class ServiceDescriptionConfigurationSupportVariationsFactoryBean extends AbstractFactoryBean<Boolean> {

    private String supportVariations = null;

    private String defaultValue = Boolean.FALSE.toString();

    public String getSupportVariations() {
        return supportVariations;
    }

    public void setSupportVariations(String supportVariations) {
        this.supportVariations = supportVariations;
    }

    @Override
    public Class<?> getObjectType() {
        return Boolean.class;
    }

    @Override
    protected Boolean createInstance() throws Exception {
        ServiceDescription serviceDescription = ServiceDescriptionHolder.getInstance().getServiceDescription();
        if (serviceDescription != null) {
            return serviceDescription.isProvideVariations();
        }
        if (this.supportVariations != null) {
            return Boolean.parseBoolean(supportVariations);
        } else {
            return Boolean.parseBoolean(defaultValue);
        }
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
