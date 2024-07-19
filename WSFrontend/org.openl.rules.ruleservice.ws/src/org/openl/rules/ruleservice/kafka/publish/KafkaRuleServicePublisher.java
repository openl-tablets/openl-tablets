package org.openl.rules.ruleservice.kafka.publish;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;

import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.kafka.RequestMessage;
import org.openl.rules.ruleservice.kafka.conf.BaseKafkaConfig;
import org.openl.rules.ruleservice.kafka.conf.KafkaDeploy;
import org.openl.rules.ruleservice.kafka.conf.KafkaMethodConfig;
import org.openl.rules.ruleservice.kafka.conf.KafkaServiceConfig;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.openl.rules.ruleservice.publish.jaxrs.storelogdata.JacksonObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.StoreLogDataManager;
import org.openl.rules.serialization.JacksonObjectMapperFactory;

public class KafkaRuleServicePublisher implements RuleServicePublisher {

    private final Logger log = LoggerFactory.getLogger(KafkaRuleServicePublisher.class);

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private final Map<OpenLService, Triple<Collection<KafkaService>, Collection<KafkaProducer<?, ?>>, Collection<KafkaConsumer<?, ?>>>> runningServices = new HashMap<>();

    private BaseKafkaConfig defaultKafkaDeploy;
    private BaseKafkaConfig immutableKafkaDeploy;

    @Autowired
    private StoreLogDataManager storeLogDataManager;

    @Autowired
    private Environment env;

    @Autowired
    @Qualifier("serviceDescriptionInProcess")
    private ObjectFactory<ServiceDescription> serviceDescriptionObjectFactory;

    public void setStoreLogDataManager(StoreLogDataManager storeLogDataManager) {
        this.storeLogDataManager = storeLogDataManager;
    }

    public StoreLogDataManager getStoreLogDataManager() {
        return storeLogDataManager;
    }

    @Autowired
    @Qualifier("jaxrsServiceObjectMapper")
    private ObjectFactory<JacksonObjectMapperFactory> kafkaJacksonObjectMapperFactoryBean;

    public ObjectFactory<ServiceDescription> getServiceDescriptionObjectFactory() {
        return serviceDescriptionObjectFactory;
    }

    public void setServiceDescriptionObjectFactory(ObjectFactory<ServiceDescription> serviceDescriptionObjectFactory) {
        this.serviceDescriptionObjectFactory = serviceDescriptionObjectFactory;
    }

    private BaseKafkaConfig getDefaultKafkaDeploy() throws IOException {
        if (defaultKafkaDeploy == null) {
            defaultKafkaDeploy = YAML.readValue(getClass().getResource("/kafka-deploy-default.yaml"), BaseKafkaConfig.class);
        }
        return defaultKafkaDeploy;
    }

    private BaseKafkaConfig getImmutableKafkaDeploy() throws IOException {
        if (immutableKafkaDeploy == null) {
            immutableKafkaDeploy = YAML.readValue(getClass().getResource("/kafka-deploy-override.yaml"), BaseKafkaConfig.class);
        }
        return immutableKafkaDeploy;
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

    private KafkaServiceConfig makeMergedKafkaConfig(Environment environment, KafkaServiceConfig kafkaConfig) throws IOException {
        var config = new KafkaServiceConfig();
        config.setDltTopic(kafkaConfig.getDltTopic());
        config.setInTopic(kafkaConfig.getInTopic());
        config.setOutTopic(kafkaConfig.getOutTopic());

        substituteAndPut(environment, config.getConsumerConfigs(), getDefaultKafkaDeploy().getConsumerConfigs());
        substituteAndPut(environment, config.getConsumerConfigs(), kafkaConfig.getConsumerConfigs());
        substituteAndPut(environment, config.getConsumerConfigs(), getImmutableKafkaDeploy().getConsumerConfigs());

        substituteAndPut(environment, config.getProducerConfigs(), getDefaultKafkaDeploy().getProducerConfigs());
        substituteAndPut(environment, config.getProducerConfigs(), kafkaConfig.getProducerConfigs());
        substituteAndPut(environment, config.getProducerConfigs(), getImmutableKafkaDeploy().getProducerConfigs());

        substituteAndPut(environment, config.getDltProducerConfigs(), getDefaultKafkaDeploy().getDltProducerConfigs());
        substituteAndPut(environment, config.getDltProducerConfigs(), kafkaConfig.getDltProducerConfigs());
        substituteAndPut(environment, config.getDltProducerConfigs(), getImmutableKafkaDeploy().getDltProducerConfigs());

        return config;
    }

    private static void substituteAndPut(Environment environment, Properties target, Properties source) {
        if (source != null) {
            for (String key : source.stringPropertyNames()) {
                String property = source.getProperty(key);
                if (property != null) {
                    property = environment.resolvePlaceholders(property);
                    target.put(key, property);
                }
            }
        }
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

    private void createKafkaService(OpenLService service,
                                                                Collection<KafkaService> kafkaServices,
                                                                Collection<KafkaConsumer<?, ?>> kafkaConsumers,
                                                                Collection<KafkaProducer<?, ?>> kafkaProducers,
                                                                ServiceDeployContext context,
                                                                KafkaServiceConfig mergedKafkaConfig,
                                                                KafkaServiceConfig config,
                                                                Method method,
                                                                RulesDeploy rulesDeploy) throws KafkaServiceException {
        // Build Kafka Consumer
        final ObjectMapper consumerObjectMapper = createJacksonObjectMapper(mergedKafkaConfig.getConsumerConfigs());
        final KafkaConsumer<String, RequestMessage> consumer = buildConsumer( service,
                consumerObjectMapper,
                method,
                mergedKafkaConfig.getConsumerConfigs());
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
            ObjectMapper producerObjectMapper = createJacksonObjectMapper(mergedKafkaConfig.getProducerConfigs());
            objectSerializer = new JacksonObjectSerializer(producerObjectMapper);
            producer = buildProducer(service,
                    producerObjectMapper,
                    mergedKafkaConfig.getProducerConfigs());
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
            dltProducer = buildDltProducer(mergedKafkaConfig.getDltProducerConfigs());
            if (possibleToReuseShared) {
                context.setDltProducer(dltProducer);
            }
            kafkaProducers.add(dltProducer);
        }
        var requestIdHeaderKey = org.openl.util.StringUtils.trimToNull(env.getProperty("log.request-id.header"));
        final KafkaService kafkaService = KafkaService.createService(service,
                requestIdHeaderKey,
                mergedKafkaConfig.getInTopic(),
                mergedKafkaConfig.getOutTopic(),
                mergedKafkaConfig.getDltTopic(),
                consumer,
                producer,
                dltProducer,
                objectSerializer,
                getStoreLogDataManager(),
                getStoreLogDataManager().isEnabled(),
                rulesDeploy);
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

            var serviceDescription = getServiceDescriptionObjectFactory().getObject();
            var resourceLoader = serviceDescription.getResourceLoader();
            var resource = resourceLoader.getResource("kafka-deploy.yaml");
            if (!resource.exists()) {
                throw new FileNotFoundException("File 'kafka-deploy.yaml' is not found.");
            }
            var kafkaDeploy = YAML.readValue(resource.getResourceAsStream(), KafkaDeploy.class);

            List<KafkaMethodConfig> kafkaMethodConfigs = kafkaDeploy.getMethodConfigs();
            Collection<KafkaService> kafkaServices = new HashSet<>();
            Collection<KafkaProducer<?, ?>> kafkaProducers = new HashSet<>();
            Collection<KafkaConsumer<?, ?>> kafkaConsumers = new HashSet<>();
            if (kafkaDeploy.getServiceConfig() != null) {
                validate(kafkaDeploy.getServiceConfig());
            }
            validateConfiguration(service, kafkaMethodConfigs);

            Map<KafkaMethodConfig, KafkaServiceConfig> kafkaMethodConfigsMap = new HashMap<>();
            Map<KafkaMethodConfig, Method> methodsMap = new HashMap<>();
            var serviceEnv = service.getServiceContext().getEnvironment();
            for (KafkaMethodConfig kmc : kafkaMethodConfigs) {
                var kafkaMethodConfig = makeMergedKafkaConfig(serviceEnv, kmc);
                validate(kafkaMethodConfig);
                kafkaMethodConfigsMap.put(kmc, kafkaMethodConfig);
                final Method method = KafkaHelpers.findMethodInService(service,
                        kmc.getMethodName(),
                        kmc.getMethodParameters());
                methodsMap.put(kmc, method);
            }

            try {
                ServiceDeployContext sharedProducersContext = new ServiceDeployContext();
                if (kafkaDeploy.getServiceConfig() != null) {
                    var kafkaServiceConfig = makeMergedKafkaConfig(serviceEnv, kafkaDeploy.getServiceConfig());
                    createKafkaService(service,
                            kafkaServices,
                            kafkaConsumers,
                            kafkaProducers,
                            sharedProducersContext,
                            kafkaServiceConfig,
                            kafkaDeploy.getServiceConfig(),
                            null,
                            serviceDescription.getRulesDeploy());
                }
                for (KafkaMethodConfig kmc : kafkaMethodConfigs) {
                    final Method method = methodsMap.get(kmc);
                    final var kafkaMethodConfig = kafkaMethodConfigsMap.get(kmc);
                    createKafkaService(service,
                            kafkaServices,
                            kafkaConsumers,
                            kafkaProducers,
                            sharedProducersContext,
                            kafkaMethodConfig,
                            kmc,
                            method,
                            serviceDescription.getRulesDeploy());
                }
            } catch (Exception e) {
                stopAndClose(Triple.of(kafkaServices, kafkaProducers, kafkaConsumers));
                throw e;
            }

            if (!kafkaServices.isEmpty()) {
                runningServices.put(service, Triple.of(kafkaServices, kafkaProducers, kafkaConsumers));
                log.info("Service '{}' has been successfully deployed.", service.getDeployPath());
            } else {
                throw new KafkaServiceConfigurationException(String.format(
                        "Failed to deploy service '%s'. Kafka method configs are not found in the configuration.",
                        service.getDeployPath()));
            }
        } catch (Exception t) {
            throw new RuleServiceDeployException(
                    String.format("Failed to deploy service '%s'.", service.getDeployPath()),
                    t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private ObjectMapper createJacksonObjectMapper(Properties config) throws KafkaServiceException {
        var jacksonObjectMapperFactory = kafkaJacksonObjectMapperFactoryBean.getObject();
        try {
            return jacksonObjectMapperFactory.createJacksonObjectMapper();
        } catch (Exception e) {
            throw new KafkaServiceException("Failed to build 'ObjectMapper' for kafka consumer.", e);
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
                Pair<String, String> p = Pair.of(kmc.getInTopic(), kmc.getConsumerConfigs().getProperty("group.id"));
                Integer t = w.get(p);
                w1.computeIfAbsent(p, e -> new ArrayList<>()).add(kmc.getMethodName());
                if (t == null) {
                    w.put(p, 1);
                } else {
                    if (t == 1 && log.isWarnEnabled()) {
                        log.warn(
                                "Service '{}' uses the input topic name '{}' and group id '{}' for different methods {}.",
                                service.getDeployPath(),
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
                    String.format("There is no running service with name '%s'", service.getDeployPath()));
        }
        try {
            if (stopAndClose(triple)) {
                log.info("Service '{}' has been undeployed successfully.", service.getDeployPath());
            } else {
                log.info("Service '{}' has been undeployed with errors.", service.getDeployPath());
            }
            runningServices.remove(service);
        } catch (Exception t) {
            throw new RuleServiceUndeployException(
                    String.format("Failed to undeploy service '%s'.", service.getDeployPath()),
                    t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenLService getServiceByDeploy(String deployPath) {
        Objects.requireNonNull(deployPath, "deployPath cannot null.");
        for (OpenLService service : runningServices.keySet()) {
            if (service.getDeployPath().equals(deployPath)) {
                return service;
            }
        }
        return null;
    }

    @Override
    public String getUrl(OpenLService service) {
        return null;
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

    @Override
    public String name() {
        return "KAFKA";
    }
}
