package org.openl.rules.ruleservice.kafka.conf;

import org.openl.rules.ruleservice.core.OpenLService;

public interface ClientIDGenerator {
    String generate(OpenLService service, BaseKafkaConfig kafkaConfig);
}
