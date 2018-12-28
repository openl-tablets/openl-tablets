package org.openl.rules.ruleservice.databinding;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Set;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class ServiceDescriptionConfigurationDefaultDateFormatFactoryBean extends AbstractFactoryBean<DateFormat> {
    private final Logger log = LoggerFactory
        .getLogger(ServiceDescriptionConfigurationDefaultDateFormatFactoryBean.class);

    private static final String DEFAULT_DATE_FORMAT = "jacksondatabinding.defaultDateFormat";

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    protected DateFormat createInstance() {
        ServiceDescription serviceDescription = ServiceDescriptionHolder.getInstance().getServiceDescription();
        if (serviceDescription != null && serviceDescription.getConfiguration() != null) {
            Object value = serviceDescription.getConfiguration().get(DEFAULT_DATE_FORMAT);
            if (value instanceof String) {
                String v = (String) value;
                try {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(v);
                    log.info("Service '{}' uses default date format '{}'.", serviceDescription.getName(), v);
                    return simpleDateFormat;
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error(
                            "Error in service '{}' configuration. Invalid date format is used in '" + DEFAULT_DATE_FORMAT + "'! Default value is used!",
                            serviceDescription.getName());
                    }
                }
            } else {
                if (value != null) {
                    if (log.isErrorEnabled()) {
                        log.error(
                            "Error in service '{}' configuration. Unsupported value is used in '" + DEFAULT_DATE_FORMAT + "'! Default value is used!",
                            serviceDescription.getName());
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Class<?> getObjectType() {
        return Set.class;
    }

}