package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.listing.BaseApiListingResource;
import io.swagger.models.Swagger;

public final class SwaggerRulesRedeployWorkaround {

    private SwaggerRulesRedeployWorkaround() {
    }

    @SuppressWarnings("unchecked")
    public static void reset() throws NoSuchFieldException, IllegalAccessException {
        Field initializedScannerField = BaseApiListingResource.class.getDeclaredField("initializedScanner");
        Field initializedConfigField = BaseApiListingResource.class.getDeclaredField("initializedConfig");
        Field swaggerMapField = SwaggerConfigLocator.class.getDeclaredField("swaggerMap");

        swaggerMapField.setAccessible(true);
        ConcurrentMap<String, Swagger> swaggerMap = (ConcurrentMap<String, Swagger>) swaggerMapField
            .get(SwaggerConfigLocator.getInstance());
        swaggerMap.clear();

        initializedScannerField.setAccessible(true);
        ConcurrentMap<String, Boolean> initializedScanner = (ConcurrentMap<String, Boolean>) initializedScannerField
            .get(null);
        initializedScanner.clear();

        initializedConfigField.setAccessible(true);
        ConcurrentMap<String, Boolean> initializedConfig = (ConcurrentMap<String, Boolean>) initializedConfigField
            .get(null);
        initializedConfig.clear();
    }
}
