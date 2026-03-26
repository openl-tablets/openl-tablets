package org.openl.rules.ruleservice.kafka.conf;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public final class KafkaDeploy {


    @Getter
    @JsonProperty(value = "service")
    @Setter
    private KafkaServiceConfig serviceConfig;

    @Getter
    @JsonProperty(value = "methods")
    @Setter
    private List<KafkaMethodConfig> methodConfigs = new ArrayList<>();

}
