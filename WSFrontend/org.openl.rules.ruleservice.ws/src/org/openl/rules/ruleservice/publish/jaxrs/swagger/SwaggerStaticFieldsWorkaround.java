package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.listing.BaseApiListingResource;
import io.swagger.models.Swagger;

public class SwaggerStaticFieldsWorkaround {

    @SuppressWarnings("unchecked")
    public static void reset() throws NoSuchFieldException, SecurityException, IllegalAccessException {
        Field initializedScannerField = BaseApiListingResource.class.getDeclaredField("initializedScanner");
        Field initializedConfigField = BaseApiListingResource.class.getDeclaredField("initializedConfig");
        boolean initializedScannerFieldAccessible = initializedScannerField.isAccessible();
        boolean initializedConfigFieldAccessible = initializedScannerField.isAccessible();

        Field swaggerMapField = SwaggerConfigLocator.class.getDeclaredField("swaggerMap");
        boolean swaggerMapFieldAccessible = swaggerMapField.isAccessible();
        
        try {
            swaggerMapField.setAccessible(true);
            ConcurrentMap<String, Swagger> swaggerMap = (ConcurrentMap<String, Swagger>) swaggerMapField.get(SwaggerConfigLocator.getInstance());
            swaggerMap.clear();
            
            initializedScannerField.setAccessible(true);
            ConcurrentMap<String, Boolean> initializedScanner = (ConcurrentMap<String, Boolean>) initializedScannerField.get(null);
            initializedScanner.clear();

            initializedConfigField.setAccessible(true);
            ConcurrentMap<String, Boolean> initializedConfig = (ConcurrentMap<String, Boolean>) initializedConfigField.get(null);
            initializedConfig.clear();
        } finally {
            initializedConfigField.setAccessible(initializedConfigFieldAccessible);
            initializedScannerField.setAccessible(initializedScannerFieldAccessible);
            swaggerMapField.setAccessible(swaggerMapFieldAccessible);
        }
    }
}
