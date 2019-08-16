package org.openl.rules.ruleservice.databinding;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ServiceConfigurationDefaultDateFormatFactoryBean extends ServiceConfigurationFactoryBean<DateFormat> {
    private static final String DEFAULT_DATE_FORMAT = "jacksondatabinding.defaultDateFormat";

    @Override
    protected DateFormat createInstance() throws Exception {
        Object value = getValue(DEFAULT_DATE_FORMAT);
        if (value instanceof String) {
            String v = (String) value;
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(v.trim());
                return simpleDateFormat;
            } catch (Exception e) {
                throw new ServiceConfigurationException(
                    String.format("Invalid date format is used for '%s' in the configuration for service '%s'.",
                        DEFAULT_DATE_FORMAT,
                        getServiceDescription().getName()),
                    e);
            }
        } else {
            if (value != null) {
                throw new ServiceConfigurationException(
                    String.format("Expected string value for '%s' in the configuration for service '%s'.",
                        DEFAULT_DATE_FORMAT,
                        getServiceDescription().getName()));
            }
        }
        return null;
    }

    @Override
    public Class<?> getObjectType() {
        return DateFormat.class;
    }

}