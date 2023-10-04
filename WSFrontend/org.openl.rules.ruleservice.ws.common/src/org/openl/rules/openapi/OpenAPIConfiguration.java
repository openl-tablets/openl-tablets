package org.openl.rules.openapi;

import io.swagger.v3.core.util.PrimitiveType;

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
    }
}
