package org.openl.rules.ruleservice.kafka.databinding;

import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.databinding.ServiceConfigurationException;
import org.openl.rules.ruleservice.kafka.conf.BaseKafkaConfig;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;

public class KafkaProjectJacksonObjectMapperFactoryBean extends ProjectJacksonObjectMapperFactoryBean {

    private Type type;
    private BaseKafkaConfig kafkaConfig;

    private ServiceDescription serviceDescription;

    public ServiceDescription getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(ServiceDescription serviceDescription) {
        this.serviceDescription = serviceDescription;
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

        processJacksonPropertiesSettingBoolean(getConfigurationValue(JACKSON_CASE_INSENSITIVE_PROPERTIES),
                JACKSON_CASE_INSENSITIVE_PROPERTIES,
                this.getDelegate()::setCaseInsensitiveProperties);
        processJacksonPropertiesSettingBoolean(getConfigurationValue(JACKSON_FAIL_ON_UNKNOWN_PROPERTIES),
                JACKSON_FAIL_ON_UNKNOWN_PROPERTIES,
                this.getDelegate()::setFailOnUnknownProperties);
        processJacksonPropertiesSettingBoolean(getConfigurationValue(JACKSON_FAIL_ON_EMPTY_BEANS),
                JACKSON_FAIL_ON_EMPTY_BEANS,
                this.getDelegate()::setFailOnEmptyBeans);

        processJacksonDefaultDateFormatSetting(getConfigurationValue(JACKSON_DEFAULT_DATE_FORMAT));
        processJacksonDefaultTypingModeSetting(getConfigurationValue(JACKSON_DEFAULT_TYPING_MODE));
        processJacksonSerializationInclusionSetting(getConfigurationValue(JACKSON_SERIALIZATION_INCLUSION));
        processJacksonJsonTypeInfoIdSetting(getConfigurationValue(JACKSON_JSON_TYPE_INFO_ID));
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
