package org.openl.rules.ruleservice.kafka.databinding;

import java.io.IOException;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.databinding.ServiceConfigurationException;
import org.openl.rules.ruleservice.kafka.conf.BaseKafkaConfig;
import org.openl.rules.ruleservice.kafka.conf.KafkaDeploy;
import org.openl.rules.ruleservice.kafka.conf.KafkaDeployUtils;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;

public class KafkaProjectJacksonObjectMapperFactoryBean extends ProjectJacksonObjectMapperFactoryBean {

    private Type type;
    private KafkaDeploy kafkaDeploy;
    private BaseKafkaConfig kafkaConfig;
    private ServiceDescription serviceDescription;

    private ServiceDescription getServiceDescription() {
        if (serviceDescription == null) {
            serviceDescription = ServiceDescriptionHolder.getInstance().get();
            if (serviceDescription == null) {
                throw new ServiceConfigurationException("Failed to locate a service description.");
            }
        }
        return serviceDescription;
    }

    private KafkaDeploy getKafkaDeploy() {
        if (kafkaDeploy == null) {
            try {
                kafkaDeploy = KafkaDeployUtils.getKafkaDeploy(getServiceDescription());
            } catch (IOException e) {
                throw new ServiceConfigurationException("Failed to load configuration for Kafka publisher.", e);
            }
        }
        return kafkaDeploy;
    }

    private BaseKafkaConfig getKafkaConfig() {
        if (kafkaConfig == null) {
            kafkaConfig = KafkaConfigHolder.getInstance().getKafkaConfig();
            if (kafkaConfig == null) {
                throw new ServiceConfigurationException("Failed to find configuration for Kafka publisher.");
            }
        }
        return kafkaConfig;
    }

    @Override
    protected void applyProjectConfiguration() {
        super.applyProjectConfiguration();
        applyKafkaConfiguration();
        processRootClassNamesBindingSetting(getConfigurationValue(ROOT_CLASS_NAMES_BINDING));
        processCaseInsensitivePropertiesSetting(getConfigurationValue(JACKSON_CASE_INSENSITIVE_PROPERTIES));
        processFailOnUnknownPropertiesSetting(getConfigurationValue(JACKSON_FAIL_ON_UNKNOWN_PROPERTIES));
        processJacksonDefaultDateFormatSetting(getConfigurationValue(JACKSON_DEFAULT_DATE_FORMAT));
        processJacksonDefaultTypingModeSetting(getConfigurationValue(JACKSON_DEFAULT_TYPING_MODE));
        processJacksonSerializationInclusionSetting(getConfigurationValue(JACKSON_SERIALIZATION_INCLUSION));
    }

    private Object getConfigurationValue(String configurationProperty) {
        Object value;
        if (Type.PRODUCER.equals(getType())) {
            value = getKafkaConfig().getProducerConfigs().get(configurationProperty);
        } else if (Type.CONSUMER.equals(getType())) {
            value = getKafkaConfig().getConsumerConfigs().get(configurationProperty);
        } else {
            value = null;
        }
        if (value == null) {
            if (Type.PRODUCER.equals(getType())) {
                value = getKafkaDeploy().getProducerConfigs().getProperty(configurationProperty);
            } else if (Type.CONSUMER.equals(getType())) {
                value = getKafkaDeploy().getConsumerConfigs().getProperty(configurationProperty);
            }
            if (value == null && getServiceDescription().getConfiguration() != null) {
                value = getServiceDescription().getConfiguration().get(configurationProperty);
            }
        }
        return value;
    }

    protected void applyKafkaConfiguration() {

    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
