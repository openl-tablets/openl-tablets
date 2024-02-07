package org.openl.rules.ruleservice.publish.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.ruleservice.kafka.conf.KafkaDeploy;
import org.openl.rules.ruleservice.kafka.conf.YamlObjectMapperBuilder;

@SpringJUnitConfig(locations = {"classpath:openl-ruleservice-property-placeholder.xml"})
public class KafkaConfigYamlTest {
    @Autowired
    ResourceLoader resourceLoader;

    @Test
    public void defaultKafkaConfigTest() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:default-kafka-deploy.yaml");
        ObjectMapper mapper = YamlObjectMapperBuilder.newInstance();
        KafkaDeploy kafkaConfig = mapper.readValue(resource.getInputStream(), KafkaDeploy.class);
        assertNotNull(kafkaConfig);
        assertNotNull(kafkaConfig.getConsumerConfigs());
        assertNotNull(kafkaConfig.getProducerConfigs());
        assertEquals("all", kafkaConfig.getProducerConfigs().getProperty("acks"));
    }

    @Test
    public void immutableKafkaConfigTest() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:immutable-kafka-deploy.yaml");
        ObjectMapper mapper = YamlObjectMapperBuilder.newInstance();
        KafkaDeploy kafkaConfig = mapper.readValue(resource.getInputStream(), KafkaDeploy.class);
        assertNotNull(kafkaConfig);
        assertNotNull(kafkaConfig.getConsumerConfigs());
    }

    @Test
    public void serviceKafkaConfigTest() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:kafka-deploy.yaml");
        ObjectMapper mapper = YamlObjectMapperBuilder.newInstance();
        KafkaDeploy kafkaConfig = mapper.readValue(resource.getInputStream(), KafkaDeploy.class);
        assertNotNull(kafkaConfig);
    }
}
