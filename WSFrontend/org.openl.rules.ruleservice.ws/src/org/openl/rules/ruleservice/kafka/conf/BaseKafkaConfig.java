package org.openl.rules.ruleservice.kafka.conf;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BaseKafkaConfig {
    @JsonProperty(value = "consumer.configs")
    private Properties consumerConfigs = new Properties();

    @JsonProperty(value = "producer.configs")
    private Properties producerConfigs = new Properties();

    @JsonProperty(value = "dlt.producer.configs")
    private Properties dltProducerConfigs = new Properties();

    @JsonProperty(value = "in.topic.name", required = true)
    private String inTopic;

    @JsonProperty(value = "out.topic.name")
    private String outTopic;

    @JsonProperty(value = "dlt.topic.name")
    private String dltTopic;

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

    public String getInTopic() {
        return inTopic;
    }

    public void setInTopic(String inTopic) {
        this.inTopic = inTopic;
    }

    public String getOutTopic() {
        return outTopic;
    }

    public void setOutTopic(String outTopic) {
        this.outTopic = outTopic;
    }

    public String getDltTopic() {
        return dltTopic;
    }

    public void setDltTopic(String dltTopic) {
        this.dltTopic = dltTopic;
    }

}
