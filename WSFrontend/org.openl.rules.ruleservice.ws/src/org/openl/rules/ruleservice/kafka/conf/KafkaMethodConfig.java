package org.openl.rules.ruleservice.kafka.conf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class KafkaMethodConfig extends KafkaServiceConfig {
    @Getter
    @JsonProperty(value = "method.name", required = true)
    @Setter
    private String methodName;

    @Getter
    @JsonProperty(value = "method.parameters")
    @Setter
    private String methodParameters;
}
