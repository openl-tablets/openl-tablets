package org.openl.rules.openapi;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.mixin.OpenAPIMixin;
import io.swagger.v3.core.jackson.mixin.SchemaMixin;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.core.util.Yaml;
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

        Json.mapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        Json.mapper().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        Json.mapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Json.mapper().enable(SerializationFeature.INDENT_OUTPUT);

        Yaml.mapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        Yaml.mapper().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        Yaml.mapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Yaml.mapper().enable(SerializationFeature.INDENT_OUTPUT);

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

}
