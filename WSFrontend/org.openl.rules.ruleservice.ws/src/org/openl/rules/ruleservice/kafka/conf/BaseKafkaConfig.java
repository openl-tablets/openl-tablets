package org.openl.rules.ruleservice.kafka.conf;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class BaseKafkaConfig {
    @Getter
    @JsonProperty(value = "consumer.configs")
    @Setter
    private Properties consumerConfigs = new Properties();

    @Getter
    @JsonProperty(value = "producer.configs")
    @Setter
    private Properties producerConfigs = new Properties();

    @Getter
    @JsonProperty(value = "dlt.producer.configs")
    @Setter
    private Properties dltProducerConfigs = new Properties();

}
