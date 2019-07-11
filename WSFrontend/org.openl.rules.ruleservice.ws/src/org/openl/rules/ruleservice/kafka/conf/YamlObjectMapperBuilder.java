package org.openl.rules.ruleservice.kafka.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public final class YamlObjectMapperBuilder {

    private YamlObjectMapperBuilder() {
    }

    public static ObjectMapper newInstance() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper;
    }

}
