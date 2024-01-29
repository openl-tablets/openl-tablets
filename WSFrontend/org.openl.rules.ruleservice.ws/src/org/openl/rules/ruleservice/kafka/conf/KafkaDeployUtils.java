package org.openl.rules.ruleservice.kafka.conf;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.openl.rules.ruleservice.core.Resource;
import org.openl.rules.ruleservice.core.ResourceLoader;
import org.openl.rules.ruleservice.core.ServiceDescription;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class KafkaDeployUtils {
    private static final String KAFKA_DEPLOY_FILE_NAME = "kafka-deploy.yaml";

    private static final String KAFKA_DEPLOY_ALTERNATIVE_FILE_NAME = "kafka-deploy.yml";

    private KafkaDeployUtils() {
    }

    public static KafkaDeploy getKafkaDeploy(ServiceDescription serviceDescription) throws IOException {
        ResourceLoader resourceLoader = serviceDescription.getResourceLoader();
        Resource resource = resourceLoader.getResource(KAFKA_DEPLOY_FILE_NAME);
        if (!resource.exists()) {
            resource = resourceLoader.getResource(KAFKA_DEPLOY_ALTERNATIVE_FILE_NAME);
            if (!resource.exists()) {
                throw new FileNotFoundException(String.format("File '%s' or '%s' is not found.",
                    KAFKA_DEPLOY_FILE_NAME,
                    KAFKA_DEPLOY_ALTERNATIVE_FILE_NAME));
            }
        }
        ObjectMapper mapper = YamlObjectMapperBuilder.newInstance();
        return mapper.readValue(resource.getResourceAsStream(), KafkaDeploy.class);
    }
}
