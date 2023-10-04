package org.openl.rules.openapi;

import java.io.InputStream;
import java.io.Reader;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.mixin.SchemaMixin;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.XML;
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

        // Remove needless xml namesapce attribute from the generated OpenAPI schema
        Json.mapper().addMixIn(Schema.class, OpenApiXmlIgnoreMixIn.class);
        Yaml.mapper().addMixIn(Schema.class, OpenApiXmlIgnoreMixIn.class);

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

    public static abstract class OpenApiXmlIgnoreMixIn extends SchemaMixin {
        @JsonIgnore
        public abstract XML getXml();
    }

}
