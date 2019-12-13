package org.openl.rules.ruleservice.databinding;

import java.text.DateFormat;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.ruleservice.databinding.util.ExtendedStdDateFormat;

public class ServiceConfigurationDefaultDateFormatFactoryBean extends ServiceConfigurationFactoryBean<DateFormat> {
    private static final String DEFAULT_DATE_FORMAT = "jacksondatabinding.defaultDateFormat";

    private String defaultDateFormat;

    public void setDefaultDateFormat(String defaultDateFormat) {
        this.defaultDateFormat = defaultDateFormat;
    }

    @Override
    protected DateFormat createInstance() throws Exception {
        String value = getValueAsString(DEFAULT_DATE_FORMAT);
        if (StringUtils.isNotBlank(value)) {
            try {
                return new ExtendedStdDateFormat(value);
            } catch (Exception e) {
                throw new ServiceConfigurationException(
                    String.format("Invalid date format is used for '%s' in the configuration for service '%s'.",
                        DEFAULT_DATE_FORMAT,
                        getServiceDescription().getName()),
                    e);
            }
        } else if (StringUtils.isNotBlank(defaultDateFormat)) {
            try {
                return new ExtendedStdDateFormat(defaultDateFormat);
            } catch (Exception e) {
                throw new ServiceConfigurationException(
                    String.format("Invalid date format is used in the service '%s' configuration.",
                        getServiceDescription().getName()),
                    e);
            }
        }
        return null;
    }

    @Override
    public Class<?> getObjectType() {
        return DateFormat.class;
    }

}