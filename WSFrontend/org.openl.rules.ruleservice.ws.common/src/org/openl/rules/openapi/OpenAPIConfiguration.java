package org.openl.rules.openapi;

import java.io.InputStream;
import java.io.Reader;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.oas.models.media.BinarySchema;
import org.springframework.core.io.InputStreamSource;

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

        ModelConverters.getInstance().addConverter((type, context, chain) -> {
            var clazz = Json.mapper().constructType(type.getType()).getRawClass();

            // Binary file format
            if (InputStream.class.isAssignableFrom(clazz)
                    || Reader.class.isAssignableFrom(clazz)
                    || InputStreamSource.class.isAssignableFrom(clazz)) {
                return new BinarySchema();
            }

            return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        });
    }
}
