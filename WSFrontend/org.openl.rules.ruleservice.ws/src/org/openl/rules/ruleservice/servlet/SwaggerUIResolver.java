package org.openl.rules.ruleservice.servlet;

import java.io.InputStream;

import org.apache.cxf.jaxrs.openapi.SwaggerUi;
import org.apache.cxf.resource.ResourceResolver;

/**
 * Resolver for static resources of swagger-ui.
 *
 * @author ybiruk
 * @author Yury Molchan
 */
public class SwaggerUIResolver implements ResourceResolver {

    public static final String SWAGGER_UI = "/swagger-ui/";
    private static final String SWAGGER_UI_PATH;

    static {
        // From SwaggerUiResolver.UI_RESOURCES_ROOT_START
        String UI_RESOURCES_ROOT_START = "META-INF/resources/webjars/swagger-ui/";
        String swaggerUiRoot = SwaggerUi.findSwaggerUiRoot(null, null);
        int basePathIndex = swaggerUiRoot.lastIndexOf(UI_RESOURCES_ROOT_START);
        SWAGGER_UI_PATH = swaggerUiRoot.substring(basePathIndex);
    }

    @Override
    public <T> T resolve(String resourceName, Class<T> resourceType) {
        InputStream stream = getAsStream(resourceName);
        if (resourceType.isInstance(stream)) {
            return resourceType.cast(stream);
        }
        return null;
    }

    @Override
    public InputStream getAsStream(String name) {
        if (name.startsWith(SWAGGER_UI)) {
            String resource = name.substring(SWAGGER_UI.length());
            return getClass().getClassLoader().getResourceAsStream(SWAGGER_UI_PATH + resource);
        }
        return null;
    }
}
