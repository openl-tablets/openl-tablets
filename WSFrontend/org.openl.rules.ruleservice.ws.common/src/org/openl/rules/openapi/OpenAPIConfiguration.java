package org.openl.rules.openapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.mixin.OpenAPIMixin;
import io.swagger.v3.core.jackson.mixin.SchemaMixin;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.XML;

/**
 * Customization of the OpenAPI schema generation with the converters of common types.
 *
 * @apiNote Yury Molchan
 */
public class OpenAPIConfiguration {

    public static void configure() {
        // Stub method to trigger static initialization
    }

    static {
        PrimitiveType.customClasses().put("java.util.Locale", PrimitiveType.STRING);


        Json.mapper().addMixIn(Schema.class, OpenApiXmlIgnoreMixIn.class);
        Yaml.mapper().addMixIn(Schema.class, OpenApiXmlIgnoreMixIn.class);
        Json.mapper().addMixIn(OpenAPI.class, SortedOpenAPIMixin.class);
        Yaml.mapper().addMixIn(OpenAPI.class, SortedOpenAPIMixin.class);

        Json.mapper().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        Json.mapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Json.mapper().enable(SerializationFeature.INDENT_OUTPUT);
        Json.mapper().setTimeZone(TimeZone.getDefault());

        Yaml.mapper().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        Yaml.mapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Yaml.mapper().enable(SerializationFeature.INDENT_OUTPUT);
        Yaml.mapper().setTimeZone(TimeZone.getDefault());

        ModelConverters.getInstance().addConverter(BinarySchemaConverter.INSTANCE);
    }

    @JsonPropertyOrder(value = {"openapi", "info", "externalDocs", "servers", "security", "tags", "paths", "components"}, alphabetic = true)
    static abstract class SortedOpenAPIMixin extends OpenAPIMixin {

        @JsonPropertyOrder(alphabetic = true)
        public abstract Map<String, Object> getExtensions();

    }

    @JsonPropertyOrder(value = {"type", "format"}, alphabetic = true)
    static abstract class OpenApiXmlIgnoreMixIn extends SchemaMixin {

        @JsonPropertyOrder(alphabetic = true)
        public abstract Map<String, Object> getExtensions();

        @JsonIgnore
        public abstract XML getXml();  // Remove needless xml namespace attribute from the generated OpenAPI schema
    }

    public static OpenAPI generateOpenAPI(Class<?> clazz, ObjectMapper objectMapper) {
        return generateOpenAPI(new OpenAPI(), clazz, objectMapper);
    }

    public static synchronized OpenAPI generateOpenAPI(OpenAPI openAPI, Class<?> clazz, ObjectMapper objectMapper) {
        var singleton = ModelConverters.getInstance();
        var converters = clearConverters(singleton);

        try {
            // Configure converters in the needed order
            singleton.addConverter(getConverters(objectMapper));

            var result = new Reader(openAPI).read(clazz);

            // Order alphabetically
            if (result.getComponents() != null) {
                var schemas = result.getComponents().getSchemas();
                if (schemas != null) {
                    result.getComponents().setSchemas(new TreeMap<>(schemas));
                }
            }

            // Order paths alphabetically
            var paths = result.getPaths();
            if (paths != null) {
                for (var pathItem : paths.values()) {
                    for (var operation : pathItem.readOperations()) {
                        // Order response codes alphabetically
                        var responses = operation.getResponses();
                        if (responses != null) {
                            var ordered = new TreeMap<>(responses);
                            responses.clear();
                            responses.putAll(ordered);
                        }
                    }
                }
                var ordered = new TreeMap<>(paths);
                paths.clear();
                paths.putAll(ordered);
            }

            return result;
        } finally {
            // Restore the previous converters
            var ignore = clearConverters(singleton);
            var itr = converters.listIterator(converters.size());
            while (itr.hasPrevious()) {
                var converter = itr.previous();
                // Restore in the revers order due addConverter put the element in the beginning of the list.
                singleton.addConverter(converter);
            }
        }
    }

    public static ModelConverter getConverters(ObjectMapper objectMapper) {
        var hackedConverters = new ArrayList<ModelConverter>();
        hackedConverters.add(OpenApiSupportConverter.INSTANCE);
        hackedConverters.add(BinarySchemaConverter.INSTANCE);
        hackedConverters.add(new ObjectMapperSupportModelResolver(objectMapper));
        return new InheritanceFixConverter(hackedConverters);
    }

    private static List<ModelConverter> clearConverters(ModelConverters singleton) {
        // Copy converters in the separate instance
        var converters = new ArrayList<>(singleton.getConverters());

        // Clean up the converters stored in the singleton.
        for (var converter : converters) {
            singleton.removeConverter(converter);
        }
        return converters;
    }

}
