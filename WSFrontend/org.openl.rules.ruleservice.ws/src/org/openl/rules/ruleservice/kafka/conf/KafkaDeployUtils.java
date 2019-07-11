package org.openl.rules.ruleservice.kafka.conf;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.openl.rules.ruleservice.core.ServiceDescription;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class KafkaDeployUtils {
    public static final String KAFKA_DEPLOY_FILE_NAME = "kafka-deploy.yaml";

    private KafkaDeployUtils() {
    }

    public static KafkaDeploy getKafkaDeploy(ServiceDescription serviceDescription) throws IOException {
        org.openl.rules.ruleservice.core.ResourceLoader resourceLoader = serviceDescription.getResourceLoader();
        org.openl.rules.ruleservice.core.Resource resource = resourceLoader.getResource(KAFKA_DEPLOY_FILE_NAME);
        if (!resource.exists()) {
            throw new FileNotFoundException("File '" + KAFKA_DEPLOY_FILE_NAME + "' is not found.");
        }
        ObjectMapper mapper = YamlObjectMapperBuilder.newInstance();
        return mapper.readValue(resource.getResourceAsStream(), KafkaDeploy.class);
    }
}
