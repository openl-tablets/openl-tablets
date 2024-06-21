package org.openl.rules.ruleservice.publish.jaxrs;

import java.util.Map;

class MethodDescription {
    private final String description;
    private final Map<String, String> parameterDescriptions;

    public MethodDescription(String description, Map<String, String> parameterDescriptions) {
        this.description = description;
        this.parameterDescriptions = parameterDescriptions;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getParameterDescriptions() {
        return parameterDescriptions;
    }
}
