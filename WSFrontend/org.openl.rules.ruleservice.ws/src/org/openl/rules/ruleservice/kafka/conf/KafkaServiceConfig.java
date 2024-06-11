package org.openl.rules.ruleservice.kafka.conf;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KafkaServiceConfig extends BaseKafkaConfig {

    @JsonProperty(value = "in.topic.name", required = true)
    private String inTopic;
    @JsonProperty(value = "out.topic.name")
    private String outTopic;
    @JsonProperty(value = "dlt.topic.name")
    private String dltTopic;

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
