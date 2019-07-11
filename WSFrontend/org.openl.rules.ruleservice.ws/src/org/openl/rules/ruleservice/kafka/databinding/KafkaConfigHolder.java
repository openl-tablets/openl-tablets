package org.openl.rules.ruleservice.kafka.databinding;

import org.openl.rules.ruleservice.kafka.conf.BaseKafkaConfig;

public final class KafkaConfigHolder {
    private static final KafkaConfigHolder INSTANCE = new KafkaConfigHolder();

    private ThreadLocal<BaseKafkaConfig> kafkaConfigHolder = new ThreadLocal<>();

    private KafkaConfigHolder() {
    }

    public static KafkaConfigHolder getInstance() {
        return INSTANCE;
    }

    public BaseKafkaConfig getKafkaConfig() {
        return kafkaConfigHolder.get();
    }

    public void setKafkaConfig(BaseKafkaConfig kafkaMethodConfig) {
        kafkaConfigHolder.set(kafkaMethodConfig);
    }

    public void remove() {
        kafkaConfigHolder.remove();
    }

}