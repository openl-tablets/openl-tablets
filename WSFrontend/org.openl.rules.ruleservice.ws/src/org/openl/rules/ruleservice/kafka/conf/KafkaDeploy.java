package org.openl.rules.ruleservice.kafka.conf;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class KafkaDeploy {


    @JsonProperty(value = "service")
    private KafkaServiceConfig serviceConfig;

    @JsonProperty(value = "methods")
    private List<KafkaMethodConfig> methodConfigs = new ArrayList<>();

    public KafkaServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(KafkaServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public List<KafkaMethodConfig> getMethodConfigs() {
        return methodConfigs;
    }

    public void setMethodConfigs(List<KafkaMethodConfig> methodConfigs) {
        this.methodConfigs = methodConfigs;
    }

}
