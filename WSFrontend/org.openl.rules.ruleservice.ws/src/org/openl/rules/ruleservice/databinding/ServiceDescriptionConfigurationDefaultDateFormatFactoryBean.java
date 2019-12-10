package org.openl.rules.ruleservice.databinding;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.databinding.util.ExtendedStdDateFormat;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class ServiceDescriptionConfigurationDefaultDateFormatFactoryBean extends AbstractFactoryBean<DateFormat> {
    private final Logger log = LoggerFactory
        .getLogger(ServiceDescriptionConfigurationDefaultDateFormatFactoryBean.class);

    private static final String DEFAULT_DATE_FORMAT = "jacksondatabinding.defaultDateFormat";

    private String defaultDateFormat;

    public void setDefaultDateFormat(String defaultDateFormat) {
        this.defaultDateFormat = defaultDateFormat;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    protected DateFormat createInstance() {
        String value = getValueAsString(DEFAULT_DATE_FORMAT);
        if (StringUtils.isNotBlank(value)) {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(value);
                log.info("Service '{}' uses default date format '{}'.", getServiceDescription().getName(), value);
                return simpleDateFormat;
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(
                        "Error in service '{}' configuration. Invalid date format is used in '{}'! Default value is used!",
                        getServiceDescription().getName(),
                        DEFAULT_DATE_FORMAT);
                }
            }
        } else if (StringUtils.isNotBlank(defaultDateFormat)) {
            try {
                return new ExtendedStdDateFormat(defaultDateFormat);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("Invalid date format is used in the rule service's default configuration.", e);
                }
            }
        }

        return null;
    }

    private ServiceDescription getServiceDescription() {
        return ServiceDescriptionHolder.getInstance().getServiceDescription();
    }

    private String getValueAsString(String property) {
        ServiceDescription serviceDescription = getServiceDescription();
        if (serviceDescription == null || serviceDescription.getConfiguration() == null) {
            return null;
        }
        Object value = serviceDescription.getConfiguration().get(property);
        if (value instanceof String) {
            return ((String) value).trim();
        }
        if (value != null) {
            if (log.isErrorEnabled()) {
                log.error(
                    "Error in service '{}' configuration. Unsupported value is used in '{}'! Default value is used!",
                    serviceDescription.getName(),
                    property);
            }
        }
        return null;
    }

    @Override
    public Class<?> getObjectType() {
        return DateFormat.class;
    }

}