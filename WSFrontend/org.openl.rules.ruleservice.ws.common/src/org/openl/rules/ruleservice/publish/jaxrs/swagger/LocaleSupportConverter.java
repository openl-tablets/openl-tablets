package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.util.Iterator;
import java.util.Locale;

import com.fasterxml.jackson.databind.JavaType;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

/**
 * String swagger schema for {@link java.util.Locale}. Because {@link com.fasterxml.jackson.databind.ObjectMapper}
 * serializes and deserializes as string values
 */
public class LocaleSupportConverter implements ModelConverter {

    @Override
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        JavaType javaType = Json.mapper().constructType(type.getType());
        if (javaType != null) {
            Class<?> cls = javaType.getRawClass();
            if (cls == Locale.class) {
                return new StringSchema();
            }
        }
        return (chain.hasNext()) ? chain.next().resolve(type, context, chain) : null;
    }
}
