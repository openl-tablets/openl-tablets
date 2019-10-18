package org.openl.rules.ruleservice.databinding;

import com.fasterxml.jackson.annotation.JsonInclude;

public class ServiceConfigurationSerializationInclusionFactoryBean extends ServiceConfigurationFactoryBean<JsonInclude.Include> {

    private static final String SERIALIZATION_INCLUSION = "jackson.serializationInclusion";

    private String defaultSerializationInclusion;

    @Override
    protected JsonInclude.Include createInstance() throws Exception {
        JsonInclude.Include serializationInclusion = null;
        Object value = getValue(SERIALIZATION_INCLUSION);
        if (value != null) {
            if (value instanceof String) {
                String stringValue = (String) value;
                try {
                    serializationInclusion = JsonInclude.Include.valueOf(stringValue);
                    return serializationInclusion;
                } catch (IllegalArgumentException e) {
                    throw new ServiceConfigurationException(String.format(
                        "Invalid serializationInclusion value is used for '%s' in the configuration for service '%s'.",
                        SERIALIZATION_INCLUSION,
                        getServiceDescription().getName()), e);
                }
            } else {
                throw new ServiceConfigurationException(
                    String.format("Expected string value for '%s' in the configuration for service '%s'.",
                        SERIALIZATION_INCLUSION,
                        getServiceDescription().getName()));
            }
        } 
        try {
            serializationInclusion = JsonInclude.Include.valueOf(defaultSerializationInclusion);
            return serializationInclusion;
        } catch (IllegalArgumentException e) {
            throw new ServiceConfigurationException("Invalid serializationInclusion value", e);
        }
    }

    @Override
    public Class<?> getObjectType() {
        return JsonInclude.Include.class;
    }

    public void setDefaultSerializationInclusion(String defaultSerializationInclusion) {
        this.defaultSerializationInclusion = defaultSerializationInclusion;
    }
}