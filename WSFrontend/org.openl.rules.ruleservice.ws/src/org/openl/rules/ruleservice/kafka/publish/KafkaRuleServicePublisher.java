package org.openl.rules.ruleservice.kafka.publish;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.databinding.JacksonObjectMapperEnhancerFactoryBean;
import org.openl.rules.ruleservice.kafka.RequestMessage;
import org.openl.rules.ruleservice.kafka.conf.BaseKafkaConfig;
import org.openl.rules.ruleservice.kafka.conf.ClientIDGenerator;
import org.openl.rules.ruleservice.kafka.conf.KafkaDeploy;
import org.openl.rules.ruleservice.kafka.conf.KafkaDeployUtils;
import org.openl.rules.ruleservice.kafka.conf.KafkaMethodConfig;
import org.openl.rules.ruleservice.kafka.conf.KafkaServiceConfig;
import org.openl.rules.ruleservice.kafka.conf.YamlObjectMapperBuilder;
import org.openl.rules.ruleservice.kafka.databinding.KafkaConfigHolder;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.openl.rules.ruleservice.publish.jaxrs.storelogdata.JacksonObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rits.cloning.Cloner;

public class KafkaRuleServicePublisher implements RuleServicePublisher, ResourceLoaderAware {

    private final Logger log = LoggerFactory.getLogger(KafkaRuleServicePublisher.class);

    private static final String CLIENT_ID_GENERATOR = "client.id.generator";
    private static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
    private static final String GROUP_ID = "group.id";
    private static final String CLIENT_ID = "client.id";

    private static final String[] CLEAN_UP_PROPERTIES = { "jackson.defaultTypingMode",
            "rootClassNamesBinding",
            CLIENT_ID_GENERATOR };

    private final Map<OpenLService, Triple<Collection<KafkaService>, Collection<KafkaProducer<?, ?>>, Collection<KafkaConsumer<?, ?>>>> runningServices = new HashMap<>();

    private ResourceLoader resourceLoader;

    private KafkaDeploy defaultKafkaDeploy;
    private KafkaDeploy immutableKafkaDeploy;

    private String defaultBootstrapServers;
    private String defaultGroupId;

    private final Cloner cloner = new Cloner();

    private StoreLogDataManager storeLogDataManager;

    private boolean storeLogDataEnabled = false;

    public void setStoreLogDataManager(StoreLogDataManager storeLogDataManager) {
        this.storeLogDataManager = storeLogDataManager;
    }

    public StoreLogDataManager getStoreLogDataManager() {
        return storeLogDataManager;
    }

    public boolean isStoreLogDataEnabled() {
        return storeLogDataEnabled;
    }

    public void setStoreLogDataEnabled(boolean storeLogDataEnabled) {
        this.storeLogDataEnabled = storeLogDataEnabled;
    }

    @Autowired
    @Qualifier("serviceKafkaConsumerJacksonDatabindingFactoryBean")
    private ObjectFactory<JacksonObjectMapperEnhancerFactoryBean> consumerJacksonObjectMapperFactoryBeanObjectFactory;

    @Autowired
    @Qualifier("serviceKafkaProducerJacksonDatabindingFactoryBean")
    private ObjectFactory<JacksonObjectMapperEnhancerFactoryBean> producerJacksonObjectMapperFactoryBeanObjectFactory;

    public ObjectFactory<JacksonObjectMapperEnhancerFactoryBean> getConsumerJacksonObjectMapperFactoryBeanObjectFactory() {
        return consumerJacksonObjectMapperFactoryBeanObjectFactory;
    }

    public void setConsumerJacksonObjectMapperFactoryBeanObjectFactory(
            ObjectFactory<JacksonObjectMapperEnhancerFactoryBean> consumerJacksonObjectMapperFactoryBeanObjectFactory) {
        this.consumerJacksonObjectMapperFactoryBeanObjectFactory = consumerJacksonObjectMapperFactoryBeanObjectFactory;
    }

    public ObjectFactory<JacksonObjectMapperEnhancerFactoryBean> getProducerJacksonObjectMapperFactoryBeanObjectFactory() {
        return producerJacksonObjectMapperFactoryBeanObjectFactory;
    }

    public void setProducerJacksonObjectMapperFactoryBeanObjectFactory(
            ObjectFactory<JacksonObjectMapperEnhancerFactoryBean> producerJacksonObjectMapperFactoryBeanObjectFactory) {
        this.producerJacksonObjectMapperFactoryBeanObjectFactory = producerJacksonObjectMapperFactoryBeanObjectFactory;
    }

    private KafkaDeploy getDefaultKafkaDeploy() throws IOException {
        if (defaultKafkaDeploy == null) {
            Resource resource = resourceLoader.getResource("classpath:default-kafka-deploy.yaml");
            if (!resource.exists()) {
                resource = resourceLoader.getResource("classpath:default-kafka-deploy.yml");
                if (!resource.exists()) {
                    throw new FileNotFoundException(
                        "File 'default-kafka-deploy.yaml' or 'default-kafka-deploy.yml' is not found.");
                }
            }
            ObjectMapper mapper = YamlObjectMapperBuilder.newInstance();
            defaultKafkaDeploy = mapper.readValue(resource.getInputStream(), KafkaDeploy.class);
        }
        return defaultKafkaDeploy;
    }

    private KafkaDeploy getImmutableKafkaDeploy() throws IOException {
        if (immutableKafkaDeploy == null) {
            Resource resource = resourceLoader.getResource("classpath:immutable-kafka-deploy.yaml");
            if (!resource.exists()) {
                resource = resourceLoader.getResource("classpath:immutable-kafka-deploy.yml");
                if (!resource.exists()) {
                    throw new FileNotFoundException(
                        "File 'immutable-kafka-deploy.yaml' or 'immutable-kafka-deploy.yml' is not found.");
                }
            }
            ObjectMapper mapper = YamlObjectMapperBuilder.newInstance();
            immutableKafkaDeploy = mapper.readValue(resource.getInputStream(), KafkaDeploy.class);
        }
        return immutableKafkaDeploy;
    }

    private void setBootstrapServers(OpenLService service,
            BaseKafkaConfig kafkaConfig,
            String logPrefix,
            Properties configs) {
        if (configs.containsKey(BOOTSTRAP_SERVERS)) {
            if (kafkaConfig instanceof KafkaMethodConfig) {
                KafkaMethodConfig kafkaMethodConfig = (KafkaMethodConfig) kafkaConfig;
                log.warn("{} '{}' property is overridden in service '{}' for method '{}'.",
                    logPrefix,
                    BOOTSTRAP_SERVERS,
                    service.getName(),
                    kafkaMethodConfig.getMethodName());
            } else {
                log.warn("{} '{}' property is overridden in service '{}'.",
                    logPrefix,
                    BOOTSTRAP_SERVERS,
                    service.getName());
            }
        } else {
            configs.setProperty(BOOTSTRAP_SERVERS, getDefaultBootstrapServers());
        }
    }

    private Properties getProducerConfigs(OpenLService service,
            BaseKafkaConfig baseKafkaDeploy,
            KafkaDeploy kafkaDeploy) throws IOException {
        Properties configs = new Properties();
        if (getDefaultKafkaDeploy().getProducerConfigs() != null) {
            configs.putAll(getDefaultKafkaDeploy().getProducerConfigs());
        }
        if (kafkaDeploy.getProducerConfigs() != null) {
            configs.putAll(kafkaDeploy.getProducerConfigs());
        }
        if (baseKafkaDeploy.getProducerConfigs() != null) {
            configs.putAll(baseKafkaDeploy.getProducerConfigs());
        }
        if (getImmutableKafkaDeploy().getProducerConfigs() != null) {
            configs.putAll(getImmutableKafkaDeploy().getProducerConfigs());
        }
        useClientIdGeneratorIfPropertyIsNotSet(configs, service, baseKafkaDeploy);
        setBootstrapServers(service, baseKafkaDeploy, "Producer", configs);
        return configs;
    }

    private Properties getDltProducerConfigs(OpenLService service,
            BaseKafkaConfig kafkaConfig,
            KafkaDeploy kafkaDeploy) throws IOException {
        Properties configs = new Properties();
        if (getDefaultKafkaDeploy().getDltProducerConfigs() != null) {
            configs.putAll(getDefaultKafkaDeploy().getDltProducerConfigs());
        }
        if (kafkaDeploy.getDltProducerConfigs() != null) {
            configs.putAll(kafkaDeploy.getDltProducerConfigs());
        }
        if (kafkaConfig.getDltProducerConfigs() != null) {
            configs.putAll(kafkaConfig.getDltProducerConfigs());
        }
        if (getImmutableKafkaDeploy().getDltProducerConfigs() != null) {
            configs.putAll(getImmutableKafkaDeploy().getDltProducerConfigs());
        }
        useClientIdGeneratorIfPropertyIsNotSet(configs, service, kafkaConfig);
        setBootstrapServers(service, kafkaConfig, "DLT producer", configs);
        return configs;
    }

    private Properties getConsumerConfigs(OpenLService service,
            BaseKafkaConfig kafkaConfig,
            KafkaDeploy kafkaDeploy) throws IOException {
        Properties configs = new Properties();
        if (getDefaultKafkaDeploy().getConsumerConfigs() != null) {
            configs.putAll(getDefaultKafkaDeploy().getConsumerConfigs());
        }
        if (kafkaDeploy.getConsumerConfigs() != null) {
            configs.putAll(kafkaDeploy.getConsumerConfigs());
        }
        if (kafkaConfig.getConsumerConfigs() != null) {
            configs.putAll(kafkaConfig.getConsumerConfigs());
        }
        if (getImmutableKafkaDeploy().getConsumerConfigs() != null) {
            configs.putAll(getImmutableKafkaDeploy().getConsumerConfigs());
        }
        useClientIdGeneratorIfPropertyIsNotSet(configs, service, kafkaConfig);
        setBootstrapServers(service, kafkaConfig, "Consumer", configs);
        if (!configs.containsKey(GROUP_ID)) {
            configs.setProperty(GROUP_ID, getDefaultGroupId());
        }
        return configs;
    }

    private void useClientIdGeneratorIfPropertyIsNotSet(Properties configs,
            OpenLService service,
            BaseKafkaConfig kafkaConfig) {
        if (configs.getProperty(CLIENT_ID) == null && configs.getProperty(CLIENT_ID_GENERATOR) != null) {
            String clientIDGeneratorClassName = configs.getProperty(CLIENT_ID_GENERATOR);
            try {
                Class<?> clientIDGeneratorClass = Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass(clientIDGeneratorClassName);
                ClientIDGenerator clientIDGenerator = (ClientIDGenerator) clientIDGeneratorClass.newInstance();
                configs.put(CLIENT_ID, clientIDGenerator.generate(service, kafkaConfig));
            } catch (Exception e) {
                log.error("Failed to generate 'client.id' property for kafka consumer/producer.", e);
            }
        }
    }

    private boolean requiredFieldIsMissed(Object value) {
        if (value instanceof String) {
            String v = (String) value;
            return StringUtils.isEmpty(v.trim());
        }
        return value == null;
    }

    private void validate(Object config) throws IllegalAccessException, KafkaServiceConfigurationException {
        List<String> missedRequiredFields = new ArrayList<>();
        for (Field field : config.getClass().getFields()) {
            JsonProperty jsonPropertyAnnotation = field.getAnnotation(JsonProperty.class);
            if (jsonPropertyAnnotation != null && jsonPropertyAnnotation.required() && requiredFieldIsMissed(
                field.get(config))) {
                missedRequiredFields.add(jsonPropertyAnnotation.value());
            }
        }
        if (!missedRequiredFields.isEmpty()) {
            String missedRequiredFieldsString = missedRequiredFields.stream()
                .collect(Collectors.joining(",", "[", "]"));
            if (config instanceof KafkaMethodConfig) {
                KafkaMethodConfig kafkaMethodConfig = (KafkaMethodConfig) config;
                throw new KafkaServiceConfigurationException(
                    String.format("Missed mandatory configs %s in method '%s' configuration.",
                        missedRequiredFieldsString,
                        kafkaMethodConfig.getMethodName()));
            } else {
                throw new KafkaServiceConfigurationException(String
                    .format("Missed mandatory config %s in the service configuration.", missedRequiredFieldsString));
            }
        }
    }

    private <T extends BaseKafkaConfig> T makeMergedKafkaConfig(OpenLService service,
            T kafkaConfig,
            KafkaDeploy kafkaDeploy) throws IOException {
        T config = cloner.deepClone(kafkaConfig);
        config.setProducerConfigs(getProducerConfigs(service, kafkaConfig, kafkaDeploy));
        config.setConsumerConfigs(getConsumerConfigs(service, kafkaConfig, kafkaDeploy));
        config.setDltProducerConfigs(getDltProducerConfigs(service, kafkaConfig, kafkaDeploy));
        return config;
    }

    protected Properties cleanupConfigs(Properties config) {
        Properties props = new Properties();
        props.putAll(config);
        for (String propertyName : CLEAN_UP_PROPERTIES) {
            props.remove(propertyName);
        }
        return props;
    }

    private <T> T getConfiguredValueDeserializer(OpenLService service,
            ObjectMapper objectMapper,
            Method method,
            String className) throws RuleServiceInstantiationException,
                              ClassNotFoundException,
                              IllegalAccessException,
                              InstantiationException,
                              InvocationTargetException {
        if (className == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) service.getClassLoader().loadClass(className);
        try {
            Constructor<T> constructor = clazz.getConstructor(OpenLService.class, ObjectMapper.class, Method.class);
            return constructor.newInstance(service, objectMapper, method);
        } catch (NoSuchMethodException e) {
            return clazz.newInstance();
        }
    }

    private <T> T getConfiguredValueSerializer(OpenLService service,
            ObjectMapper objectMapper,
            String className) throws RuleServiceInstantiationException,
                              ClassNotFoundException,
                              IllegalAccessException,
                              InstantiationException,
                              InvocationTargetException {
        if (className == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) service.getClassLoader().loadClass(className);
        try {
            Constructor<T> constructor = clazz.getConstructor(OpenLService.class, ObjectMapper.class);
            return constructor.newInstance(service, objectMapper);
        } catch (NoSuchMethodException e) {
            return clazz.newInstance();
        }
    }

    private <T> T getConfiguredKeySerializerOrDeserializer(OpenLService service,
            String className) throws RuleServiceInstantiationException,
                              ClassNotFoundException,
                              IllegalAccessException,
                              InstantiationException {
        if (className == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) service.getClassLoader().loadClass(className);
        return clazz.newInstance();
    }

    private KafkaProducer<String, Object> buildProducer(OpenLService service,
            ObjectMapper objectMapper,
            Properties configs) throws KafkaServiceConfigurationException {
        Serializer<String> keySerializer;
        Serializer<Object> valueSerializer;
        try {
            keySerializer = getConfiguredKeySerializerOrDeserializer(service, configs.getProperty("key.serializer"));
            valueSerializer = getConfiguredValueSerializer(service,
                objectMapper,
                configs.getProperty("value.serializer"));
        } catch (Exception e) {
            throw new KafkaServiceConfigurationException("Failed to constuct key/value deserializer for producer.", e);
        }
        return new KafkaProducer<>(configs, keySerializer, valueSerializer);
    }

    private KafkaProducer<String, byte[]> buildDltProducer(Properties configs) {
        return new KafkaProducer<>(configs);
    }

    private KafkaConsumer<String, RequestMessage> buildConsumer(OpenLService service,
            ObjectMapper objectMapper,
            Method method,
            Properties configs) throws KafkaServiceConfigurationException {
        Deserializer<String> keyDeserializer;
        Deserializer<RequestMessage> valueDeserializer;
        try {
            keyDeserializer = getConfiguredKeySerializerOrDeserializer(service,
                configs.getProperty("key.deserializer"));
            valueDeserializer = getConfiguredValueDeserializer(service,
                objectMapper,
                method,
                configs.getProperty("value.deserializer"));
        } catch (Exception e) {
            throw new KafkaServiceConfigurationException("Failed to constuct key/value deserializer for consumer.", e);
        }
        return new KafkaConsumer<>(configs, keyDeserializer, valueDeserializer);
    }

    private <T extends BaseKafkaConfig> void createKafkaService(OpenLService service,
            Collection<KafkaService> kafkaServices,
            Collection<KafkaConsumer<?, ?>> kafkaConsumers,
            Collection<KafkaProducer<?, ?>> kafkaProducers,
            ServiceDeployContext context,
            T mergedKafkaConfig,
            T config,
            Method method) throws KafkaServiceException {
        final JacksonObjectMapperEnhancerFactoryBean consumerJacksonObjectMapperFactoryBean = createConsumerJacksonObjectMapperFactoryBeanFactory(
            mergedKafkaConfig);
        final JacksonObjectMapperEnhancerFactoryBean producerJacksonObjectMapperFactoryBean = createProducerJacksonObjectMapperFactoryBeanFactory(
            mergedKafkaConfig);

        // Build Method Kafka Consumer
        ObjectMapper consumerObjectMapper;
        try {
            consumerObjectMapper = consumerJacksonObjectMapperFactoryBean.getObject();
        } catch (Exception e) {
            throw new KafkaServiceException("Failed to build 'ObjectMapper' for kafka consumer.", e);
        }
        final KafkaConsumer<String, RequestMessage> consumer = buildConsumer(service,
            consumerObjectMapper,
            method,
            cleanupConfigs(mergedKafkaConfig.getConsumerConfigs()));
        kafkaConsumers.add(consumer);

        // Build Method Kafka Producer or reuse shared
        boolean possibleToReuseShared = config.getProducerConfigs() == null || config.getProducerConfigs().isEmpty();
        ObjectSerializer objectSerializer = null;
        KafkaProducer<String, Object> producer = null;
        if (possibleToReuseShared) {
            producer = context.getProducer();
            objectSerializer = context.getObjectSerializer();
        }
        if (producer == null) {
            ObjectMapper producerObjectMapper;
            try {
                producerObjectMapper = producerJacksonObjectMapperFactoryBean.getObject();
            } catch (Exception e) {
                throw new KafkaServiceException("Failed to build 'ObjectMapper' for kafka producer.", e);
            }
            objectSerializer = new JacksonObjectSerializer(producerObjectMapper);
            producer = buildProducer(service,
                producerObjectMapper,
                cleanupConfigs(mergedKafkaConfig.getProducerConfigs()));
            if (possibleToReuseShared) {
                context.setProducerAndObjectSerializer(producer, objectSerializer);
            }
            kafkaProducers.add(producer);
        }
        // Build Method DLT Kafka Producer or reuse service shared
        KafkaProducer<String, byte[]> dltProducer = null;
        possibleToReuseShared = config.getDltProducerConfigs() == null || config.getDltProducerConfigs().isEmpty();
        if (possibleToReuseShared) {
            dltProducer = context.getDltProducer();
        }
        if (dltProducer == null) {
            dltProducer = buildDltProducer(cleanupConfigs(mergedKafkaConfig.getDltProducerConfigs()));
            if (possibleToReuseShared) {
                context.setDltProducer(dltProducer);
            }
            kafkaProducers.add(dltProducer);
        }

        final KafkaService kafkaService = KafkaService.createService(service,
            mergedKafkaConfig.getInTopic(),
            mergedKafkaConfig.getOutTopic(),
            mergedKafkaConfig.getDltTopic(),
            consumer,
            producer,
            dltProducer,
            objectSerializer,
            getStoreLogDataManager(),
            isStoreLogDataEnabled());
        kafkaServices.add(kafkaService);

        kafkaService.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deploy(OpenLService service) throws RuleServiceDeployException {
        Objects.requireNonNull(service, "service cannot be null");
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(service.getClassLoader());
            KafkaDeploy kafkaDeploy = KafkaDeployUtils.getKafkaDeploy(ServiceDescriptionHolder.getInstance().get());
            List<KafkaMethodConfig> kafkaMethodConfigs = kafkaDeploy.getMethodConfigs() == null ? Collections
                .emptyList() : kafkaDeploy.getMethodConfigs();
            Collection<KafkaService> kafkaServices = new HashSet<>();
            Collection<KafkaProducer<?, ?>> kafkaProducers = new HashSet<>();
            Collection<KafkaConsumer<?, ?>> kafkaConsumers = new HashSet<>();
            if (kafkaDeploy.getServiceConfig() != null) {
                validate(kafkaDeploy.getServiceConfig());
            }
            validateConfiguration(service, kafkaMethodConfigs);

            Map<KafkaMethodConfig, KafkaMethodConfig> kafkaMethodConfigsMap = new HashMap<>();
            Map<KafkaMethodConfig, Method> methodsMap = new HashMap<>();
            for (KafkaMethodConfig kmc : kafkaMethodConfigs) {
                KafkaMethodConfig kafkaMethodConfig = makeMergedKafkaConfig(service, kmc, kafkaDeploy);
                validate(kafkaMethodConfig);
                kafkaMethodConfigsMap.put(kmc, kafkaMethodConfig);
                final Method method = KafkaHelpers.findMethodInService(service,
                    kafkaMethodConfig.getMethodName(),
                    kafkaMethodConfig.getMethodParameters());
                methodsMap.put(kmc, method);
            }

            try {
                ServiceDeployContext sharedProducersContext = new ServiceDeployContext();
                if (kafkaDeploy.getServiceConfig() != null) {
                    KafkaServiceConfig kafkaServiceConfig = makeMergedKafkaConfig(service,
                        kafkaDeploy.getServiceConfig(),
                        kafkaDeploy);
                    createKafkaService(service,
                        kafkaServices,
                        kafkaConsumers,
                        kafkaProducers,
                        sharedProducersContext,
                        kafkaServiceConfig,
                        kafkaDeploy.getServiceConfig(),
                        null);
                }
                for (KafkaMethodConfig kmc : kafkaMethodConfigs) {
                    final Method method = methodsMap.get(kmc);
                    final KafkaMethodConfig kafkaMethodConfig = kafkaMethodConfigsMap.get(kmc);
                    createKafkaService(service,
                        kafkaServices,
                        kafkaConsumers,
                        kafkaProducers,
                        sharedProducersContext,
                        kafkaMethodConfig,
                        kmc,
                        method);
                }
            } catch (Exception e) {
                stopAndClose(Triple.of(kafkaServices, kafkaProducers, kafkaConsumers));
                throw e;
            }

            if (!kafkaServices.isEmpty()) {
                runningServices.put(service, Triple.of(kafkaServices, kafkaProducers, kafkaConsumers));
                log.info("Service '{}' has been successfully deployed.", service.getName());
            } else {
                throw new KafkaServiceConfigurationException(String.format(
                    "Failed to deploy service '%s'. Kafka method configs are not found in the configuration.",
                    service.getName()));
            }
        } catch (Exception t) {
            throw new RuleServiceDeployException(String.format("Failed to deploy service '%s'.", service.getName()), t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private JacksonObjectMapperEnhancerFactoryBean createConsumerJacksonObjectMapperFactoryBeanFactory(
            BaseKafkaConfig config) {
        try {
            KafkaConfigHolder.getInstance().setKafkaConfig(config);
            return getConsumerJacksonObjectMapperFactoryBeanObjectFactory().getObject();
        } finally {
            KafkaConfigHolder.getInstance().remove();
        }
    }

    private JacksonObjectMapperEnhancerFactoryBean createProducerJacksonObjectMapperFactoryBeanFactory(
            BaseKafkaConfig config) {
        try {
            KafkaConfigHolder.getInstance().setKafkaConfig(config);
            return getProducerJacksonObjectMapperFactoryBeanObjectFactory().getObject();
        } finally {
            KafkaConfigHolder.getInstance().remove();
        }
    }

    private boolean stopAndClose(
            Triple<Collection<KafkaService>, Collection<KafkaProducer<?, ?>>, Collection<KafkaConsumer<?, ?>>> t) {
        boolean ret = true;
        for (KafkaService kafkaService : t.getLeft()) {
            try {
                kafkaService.stop();
            } catch (Exception e1) {
                ret = false;
                log.error("Failed to stop kafka service.", e1);
            }
        }
        for (KafkaProducer<?, ?> kafkaProducer : t.getMiddle()) {
            try {
                kafkaProducer.close();
            } catch (Exception e1) {
                ret = false;
                log.error("Failed to close kafka producer.", e1);
            }
        }
        for (KafkaConsumer<?, ?> kafkaConsumer : t.getRight()) {
            try {
                kafkaConsumer.close();
            } catch (Exception e1) {
                ret = false;
                log.error("Failed to close kafka consumer.", e1);
            }
        }
        return ret;
    }

    private void validateConfiguration(OpenLService service, List<KafkaMethodConfig> kafkaMethodConfigs) {
        Map<Pair<String, String>, Integer> w = new HashMap<>();
        Map<Pair<String, String>, List<String>> w1 = new HashMap<>();
        for (KafkaMethodConfig kmc : kafkaMethodConfigs) {
            if (kmc.getInTopic() != null && kmc.getMethodName() != null) {
                Pair<String, String> p = Pair.of(kmc.getInTopic(), kmc.getConsumerConfigs().getProperty(GROUP_ID));
                Integer t = w.get(p);
                w1.computeIfAbsent(p, e -> new ArrayList<>()).add(kmc.getMethodName());
                if (t == null) {
                    w.put(p, 1);
                } else {
                    if (t == 1 && log.isWarnEnabled()) {
                        log.warn(
                            "Service '{}' uses the input topic name '{}' and group id '{}' for different methods {}.",
                            service.getName(),
                            p.getLeft(),
                            p.getRight(),
                            w1.get(p).stream().collect(Collectors.joining(",", "[", "]")));
                    }
                    w.put(p, t + 1);
                }
            }
        }
    }

    @Override
    public void undeploy(OpenLService service) throws RuleServiceUndeployException {
        Objects.requireNonNull(service, "service cannot be null");
        Triple<Collection<KafkaService>, Collection<KafkaProducer<?, ?>>, Collection<KafkaConsumer<?, ?>>> triple = runningServices
            .get(service);
        if (triple == null) {
            throw new RuleServiceUndeployException(
                String.format("There is no running service with name '%s'", service.getName()));
        }
        try {
            if (stopAndClose(triple)) {
                log.info("Service '{}' has been undeployed successfully.", service.getName());
            } else {
                log.info("Service '{}' has been undeployed with errors.", service.getName());
            }
            runningServices.remove(service);
        } catch (Exception t) {
            throw new RuleServiceUndeployException(String.format("Failed to undeploy service '%s'.", service.getName()),
                t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenLService getServiceByName(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName cannot null.");
        for (OpenLService service : runningServices.keySet()) {
            if (service.getName().equals(serviceName)) {
                return service;
            }
        }
        return null;
    }

    @Override
    public String getUrl(OpenLService service) {
        return null;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String getDefaultGroupId() {
        return defaultGroupId;
    }

    public void setDefaultGroupId(String defaultGroupId) {
        this.defaultGroupId = defaultGroupId;
    }

    public String getDefaultBootstrapServers() {
        return defaultBootstrapServers;
    }

    public void setDefaultBootstrapServers(String defaultBootstrapServers) {
        this.defaultBootstrapServers = defaultBootstrapServers;
    }

    private static final class ServiceDeployContext {
        private KafkaProducer<String, Object> producer;
        private KafkaProducer<String, byte[]> dltProducer;
        private ObjectSerializer objectSerializer;

        public KafkaProducer<String, byte[]> getDltProducer() {
            return dltProducer;
        }

        public void setDltProducer(KafkaProducer<String, byte[]> dltProducer) {
            this.dltProducer = Objects.requireNonNull(dltProducer);
        }

        public KafkaProducer<String, Object> getProducer() {
            return producer;
        }

        public void setProducerAndObjectSerializer(KafkaProducer<String, Object> producer,
                ObjectSerializer objectSerializer) {
            this.producer = Objects.requireNonNull(producer);
            this.objectSerializer = Objects.requireNonNull(objectSerializer);
        }

        public ObjectSerializer getObjectSerializer() {
            return objectSerializer;
        }

    }
}
