package org.openl.rules.ruleservice.kafka.conf;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class BaseKafkaConfig {
    @Getter
    @JsonProperty("consumer.configs")
    @Setter
    private Properties consumerConfigs = new Properties();

    @Getter
    @JsonProperty("producer.configs")
    @Setter
    private Properties producerConfigs = new Properties();

    @Getter
    @JsonProperty("dlt.producer.configs")
    @Setter
    private Properties dltProducerConfigs = new Properties();

}
