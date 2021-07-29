package org.openl.rules.project.openapi;

import java.io.IOException;
import java.util.Map;

import org.openl.util.StringTool;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;

public final class OpenApiSerializationUtils {

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

        SimpleModule openApiModule = new SimpleModule();
        openApiModule.addSerializer(OpenAPI.class, new OpenApiSerializer());

        return Json.mapper()
            .copy()
            .registerModule(openApiModule)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .writer()
            .with(prettyPrinter)
            .writeValueAsString(api)
            .replace(": { }", ": {}");
    }

    private static final class OpenApiSerializer extends JsonSerializer<OpenAPI> {

        @Override
        public void serialize(OpenAPI src, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (src == null) {
                return;
            }
            gen.writeStartObject();
            gen.writeStringField("openapi", src.getOpenapi());
            if(src.getInfo() != null) {
                gen.writeObjectField("info", src.getInfo());
            }
            if (src.getExternalDocs() != null) {
                gen.writeObjectField("externalDocs", src.getExternalDocs());
            }
            if (src.getServers() != null) {
                gen.writeObjectField("servers", src.getServers());
            }
            if (src.getSecurity() != null) {
                gen.writeObjectField("security", src.getSecurity());
            }
            if (src.getTags() != null) {
                gen.writeObjectField("tags", src.getTags());
            }
            if (src.getPaths() != null) {
                gen.writeObjectField("paths", src.getPaths());
            }
            if (src.getComponents() != null) {
                gen.writeObjectField("components", src.getComponents());
            }
            if (src.getExtensions() != null) {
                for (Map.Entry<String, Object> e : src.getExtensions().entrySet()) {
                    gen.writeObjectField(e.getKey(), e.getValue());
                }
            }
            gen.writeEndObject();
        }
    }

}
