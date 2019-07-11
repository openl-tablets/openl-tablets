package org.openl.rules.ruleservice.kafka.databinding;

import java.io.IOException;

import org.openl.rules.ruleservice.databinding.ServiceDescriptionConfigurationException;
import org.openl.rules.ruleservice.databinding.ServiceDescriptionConfigurationRootClassNamesBindingFactoryBean;
import org.openl.rules.ruleservice.kafka.conf.BaseKafkaConfig;
import org.openl.rules.ruleservice.kafka.conf.KafkaDeploy;
import org.openl.rules.ruleservice.kafka.conf.KafkaDeployUtils;

public class KafkaDeployConfigurationRootClassNamesBindingFactoryBean extends ServiceDescriptionConfigurationRootClassNamesBindingFactoryBean {
    private Type type;
    private KafkaDeploy kafkaDeploy;
    private BaseKafkaConfig kafkaConfig;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public KafkaDeploy getKafkaDeploy() throws Exception {
        if (kafkaDeploy == null) {
            try {
                kafkaDeploy = KafkaDeployUtils.getKafkaDeploy(getServiceDescription());
            } catch (IOException e) {
                throw new ServiceDescriptionConfigurationException("Failed to load kafka configuration.", e);
            }
        }
        return kafkaDeploy;
    }

    public BaseKafkaConfig getKafkaConfig() throws Exception {
        if (kafkaConfig == null) {
            kafkaConfig = KafkaConfigHolder.getInstance().getKafkaConfig();
            if (kafkaConfig == null) {
                throw new ServiceDescriptionConfigurationException("Failed to locate KafkaMethodConfig.");
            }
        }
        return kafkaConfig;
    }

    @Override
    protected Object getValue(String property) throws Exception {
        Object value;
        if (Type.PRODUCER.equals(getType())) {
            value = getKafkaConfig().getProducerConfigs().get(property);
        } else if (Type.CONSUMER.equals(getType())) {
            value = getKafkaConfig().getConsumerConfigs().get(property);
        } else {
            value = null;
        }
        if (value == null) {
            if (Type.PRODUCER.equals(getType())) {
                value = getKafkaDeploy().getProducerConfigs().getProperty(property);
            } else if (Type.CONSUMER.equals(getType())) {
                value = getKafkaDeploy().getConsumerConfigs().getProperty(property);
            } else {
                value = null;
            }
            if (value == null && getServiceDescription().getConfiguration() != null) {
                value = getServiceDescription().getConfiguration().get(property);
            }
        }
        return value;
    }

}