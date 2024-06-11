package org.openl.rules.ruleservice.kafka.databinding;

import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;

public class KafkaProjectJacksonObjectMapperFactoryBean extends ProjectJacksonObjectMapperFactoryBean {

    @Override
    protected Object getProperty(String name) {
        var kafkaConfig = KafkaConfigHolder.getInstance().getKafkaConfig();

        Object value = kafkaConfig != null ? kafkaConfig.apply(name) : null;
        if (value == null) {
            return super.getProperty(name);
        }
        return value;
    }
}
