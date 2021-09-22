package org.openl.rules.project.openapi;

import org.openl.rules.ruleservice.publish.jaxrs.swagger.jackson.OpenApiObjectMapperConfigurationHelper;
import org.openl.util.StringTool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.ObjectMapperFactory;

public final class OpenApiSerializationUtils {

    private static final ObjectMapper JSON_MAPPER;
    private static final ObjectMapper YAML_MAPPER;

    static {
        JSON_MAPPER = ObjectMapperFactory.createJson();
        OpenApiObjectMapperConfigurationHelper.configure(JSON_MAPPER);
        YAML_MAPPER = ObjectMapperFactory.createYaml();
        ((YAMLFactory) YAML_MAPPER.getFactory()).disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        OpenApiObjectMapperConfigurationHelper.configure(YAML_MAPPER);
    }

    private OpenApiSerializationUtils() {
    }

    public static String toJson(OpenAPI api) throws JsonProcessingException {
        if (api == null) {
            return null;
        }

        DefaultIndenter indenter = new DefaultIndenter().withLinefeed(StringTool.NEW_LINE);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter() {

            @Override
            public DefaultPrettyPrinter withSeparators(Separators separators) {
                this._separators = separators;
                this._objectFieldValueSeparatorWithSpaces = separators.getObjectFieldValueSeparator() + " ";
                return this;
            }

        }.withObjectIndenter(indenter).withArrayIndenter(indenter);

        return JSON_MAPPER.writer(prettyPrinter).writeValueAsString(api).replace(": { }", ": {}");
    }

    public static String toYaml(OpenAPI api) throws JsonProcessingException {
        return api != null ? YAML_MAPPER.writeValueAsString(api) : null;
    }

}
