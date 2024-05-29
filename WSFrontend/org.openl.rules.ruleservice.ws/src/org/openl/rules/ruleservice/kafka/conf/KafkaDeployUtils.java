package org.openl.rules.ruleservice.kafka.conf;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.openl.rules.ruleservice.core.Resource;
import org.openl.rules.ruleservice.core.ResourceLoader;
import org.openl.rules.ruleservice.core.ServiceDescription;

public final class KafkaDeployUtils {

    private KafkaDeployUtils() {
    }

    public static KafkaDeploy getKafkaDeploy(ServiceDescription serviceDescription) throws IOException {
        ResourceLoader resourceLoader = serviceDescription.getResourceLoader();
        Resource resource = resourceLoader.getResource("kafka-deploy.yaml");
        if (!resource.exists()) {
            throw new FileNotFoundException("File 'kafka-deploy.yaml' is not found.");
        }
        ObjectMapper mapper = YamlObjectMapperBuilder.newInstance();
        return mapper.readValue(resource.getResourceAsStream(), KafkaDeploy.class);
    }
}
