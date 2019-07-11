package org.openl.rules.ruleservice.publish.kafka;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.kafka.conf.KafkaDeploy;
import org.openl.rules.ruleservice.kafka.conf.YamlObjectMapperBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:openl-ruleservice-property-placeholder.xml" })
public class KafkaConfigYamlTest {
    @Autowired
    ResourceLoader resourceLoader;

    @Test
    public void defaultKafkaConfigTest() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:default-kafka-deploy.yaml");
        ObjectMapper mapper = YamlObjectMapperBuilder.newInstance();
        KafkaDeploy kafkaConfig = mapper.readValue(resource.getInputStream(), KafkaDeploy.class);
        Assert.assertNotNull(kafkaConfig);
        Assert.assertNotNull(kafkaConfig.getConsumerConfigs());
        Assert.assertNotNull(kafkaConfig.getProducerConfigs());
        Assert.assertEquals("all", kafkaConfig.getProducerConfigs().getProperty("acks"));
    }

    @Test
    public void immutableKafkaConfigTest() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:immutable-kafka-deploy.yaml");
        ObjectMapper mapper = YamlObjectMapperBuilder.newInstance();
        KafkaDeploy kafkaConfig = mapper.readValue(resource.getInputStream(), KafkaDeploy.class);
        Assert.assertNotNull(kafkaConfig);
        Assert.assertNotNull(kafkaConfig.getConsumerConfigs());
    }

    @Test
    public void serviceKafkaConfigTest() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:kafka-deploy.yaml");
        ObjectMapper mapper = YamlObjectMapperBuilder.newInstance();
        KafkaDeploy kafkaConfig = mapper.readValue(resource.getInputStream(), KafkaDeploy.class);
        Assert.assertNotNull(kafkaConfig);
    }
}
