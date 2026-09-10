package org.openl.rules.ruleservice.kafka.conf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class KafkaServiceConfig extends BaseKafkaConfig {

    @Getter
    @JsonProperty(value = "in.topic.name", required = true)
    @Setter
    private String inTopic;
    @Getter
    @JsonProperty("out.topic.name")
    @Setter
    private String outTopic;
    @Getter
    @JsonProperty("dlt.topic.name")
    @Setter
    private String dltTopic;
}
