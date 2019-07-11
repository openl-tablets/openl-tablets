package org.openl.rules.ruleservice.kafka.conf;

import java.util.UUID;

import org.openl.rules.ruleservice.core.OpenLService;

public final class RandomClientIDGenerator implements ClientIDGenerator {
    @Override
    public String generate(OpenLService service, BaseKafkaConfig kafkaConfig) {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
