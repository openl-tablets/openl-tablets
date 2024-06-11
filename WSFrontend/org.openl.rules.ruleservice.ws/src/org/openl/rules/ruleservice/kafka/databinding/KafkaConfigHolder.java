package org.openl.rules.ruleservice.kafka.databinding;

import java.util.function.Function;

public final class KafkaConfigHolder {
    private static final KafkaConfigHolder INSTANCE = new KafkaConfigHolder();

    private final ThreadLocal<Function<String, String>> kafkaConfigHolder = new ThreadLocal<>();

    private KafkaConfigHolder() {
    }

    public static KafkaConfigHolder getInstance() {
        return INSTANCE;
    }

    public Function<String, String> getKafkaConfig() {
        return kafkaConfigHolder.get();
    }

    public void setKafkaConfig(Function<String, String> kafkaMethodConfig) {
        kafkaConfigHolder.set(kafkaMethodConfig);
    }

    public void remove() {
        kafkaConfigHolder.remove();
    }

}