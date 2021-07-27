package org.openl.rules.project.validation.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.MapperFeature;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;

public final class OpenApiSerializer {

    private OpenApiSerializer() {
    }

    public static String toJson(OpenAPI api) throws JsonProcessingException {
        if (api == null) {
            return null;
        }

        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter() {
            @Override
            public DefaultPrettyPrinter withSeparators(Separators separators) {
                this._separators = separators;
                this._objectFieldValueSeparatorWithSpaces = separators.getObjectFieldValueSeparator() + " ";
                return this;
            }
        }.withSpacesInObjectEntries().withArrayIndenter(new DefaultIndenter());
        return Json.mapper()
            .copy()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .writer()
            .with(prettyPrinter)
            .writeValueAsString(api)
            .replace(": { }", ": {}")
            .replace("\r\n", "\n");
    }

}
