package org.openl.rules.openapi;

import java.io.InputStream;
import java.io.Reader;
import java.util.Iterator;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.core.io.InputStreamSource;

/**
 * Convert streamed-like types in {@linkplain  BinarySchema}.
 *
 * @author Yury Molchan
 */
class BinarySchemaConverter implements ModelConverter {

    static final BinarySchemaConverter INSTANCE = new BinarySchemaConverter();

    @Override
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        var clazz = Json.mapper().constructType(type.getType()).getRawClass();

        // Binary file format
        if (InputStream.class.isAssignableFrom(clazz)
                || Reader.class.isAssignableFrom(clazz)
                || InputStreamSource.class.isAssignableFrom(clazz)) {
            return new BinarySchema();
        }

        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    }
}
