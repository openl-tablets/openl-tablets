package org.openl.rules.ruleservice.kafka.conf;

import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class KafkaDeploy {
    @JsonProperty(value = "consumer.configs")
    private Properties consumerConfigs = new Properties();

    @JsonProperty(value = "producer.configs")
    private Properties producerConfigs = new Properties();

    @JsonProperty(value = "dlt.producer.configs")
    private Properties dltProducerConfigs = new Properties();

    @JsonProperty(value = "service")
    private KafkaServiceConfig serviceConfig;

    @JsonProperty(value = "methods")
    private List<KafkaMethodConfig> methodConfigs;

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

    public Properties getConsumerConfigs() {
        return consumerConfigs;
    }

    public void setConsumerConfigs(Properties consumerConfigs) {
        this.consumerConfigs = consumerConfigs;
    }

    public Properties getProducerConfigs() {
        return producerConfigs;
    }

    public void setProducerConfigs(Properties producerConfigs) {
        this.producerConfigs = producerConfigs;
    }

    public Properties getDltProducerConfigs() {
        return dltProducerConfigs;
    }

    public void setDltProducerConfigs(Properties dltProducerConfigs) {
        this.dltProducerConfigs = dltProducerConfigs;
    }

}
