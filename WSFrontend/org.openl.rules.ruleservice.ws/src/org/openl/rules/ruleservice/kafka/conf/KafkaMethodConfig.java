package org.openl.rules.ruleservice.kafka.conf;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KafkaMethodConfig extends BaseKafkaConfig {
    @JsonProperty(value = "method.name", required = true)
    private String methodName;

    @JsonProperty(value = "method.parameters")
    private String methodParameters;

    public String getMethodParameters() {
        return methodParameters;
    }

    public void setMethodParameters(String methodParameters) {
        this.methodParameters = methodParameters;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
