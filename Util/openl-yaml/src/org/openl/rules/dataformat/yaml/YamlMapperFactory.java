package org.openl.rules.dataformat.yaml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * Simple YAML ObjectMapper factory
 *
 * @author Vladyslav Pikus
 */
public final class YamlMapperFactory {

    private static class Holder {
        private static final YAMLMapper INSTANCE = createObjectMapper();
    }

    public static YAMLMapper getYamlMapper() {
        return Holder.INSTANCE;
    }

    private YamlMapperFactory() {
    }

    private static YAMLMapper createObjectMapper() {
        var factory = new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        return YAMLMapper.builder(factory)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .deactivateDefaultTyping()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    }

}
