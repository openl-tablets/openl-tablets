package org.openl.example.auth;

import java.util.Set;

public class OpenAPIUrlMatcher {
    private static final Set<String> OPEN_API_URLS = Set.of(
            "/admin/swagger-ui.json",
            "/admin/ui/info");
    private static final String OPEN_API_JSON_PATTERN = "^/[^/]+/openapi\\.json$";
    
    public static boolean isOpenApiUrl(String uri) {
        return OPEN_API_URLS.stream().anyMatch(openApiUri -> openApiUri.equals(uri))
                || uri.matches(OPEN_API_JSON_PATTERN);
        
    }
}
