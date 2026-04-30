package org.openl.rules.ruleservice.jaxrs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.UnaryOperator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.SneakyThrows;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Applies custom OpenAPI schema overrides loaded from {@code openapi-configuration*.json} resources on the service
 * classpath. {@code openapi-configuration.json} is always applied first; suffixed variants
 * ({@code openapi-configuration-1.json}, {@code openapi-configuration-alpha.json}, ...) follow in natural filename
 * order. Each subsequent file overrides matching fields from the previous ones; non-overlapping fields are merged.
 *
 * <p>Spring property placeholders ({@code ${name}} and {@code ${name:default}}) inside each file body are resolved
 * against the supplied {@link PropertyResolver} prior to JSON parsing.</p>
 *
 * @see <a href=
 * "https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---Integration-and-Configuration#configuration">Swagger
 * configuration</a>
 */
final class CustomOpenApiProcessor implements UnaryOperator<OpenAPI> {

    private static final String[] LOCATION_PATTERNS = {
            "classpath*:openapi-configuration.json",
            "classpath*:openapi-configuration-*.json"
    };

    private static final Comparator<Resource> RESOURCE_COMPARATOR = Comparator
            .comparing(Resource::getFilename, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Resource::toString);

    private final ObjectMapper mapper;
    private final ClassLoader classLoader;
    private final PropertyResolver propertyResolver;

    CustomOpenApiProcessor(ObjectMapper mapper, ClassLoader classLoader, PropertyResolver propertyResolver) {
        this.mapper = mapper;
        this.classLoader = classLoader;
        this.propertyResolver = propertyResolver;
    }

    @Override
    @SneakyThrows
    public OpenAPI apply(OpenAPI openAPI) {
        var resolver = new PathMatchingResourcePatternResolver(classLoader);
        for (String pattern : LOCATION_PATTERNS) {
            Resource[] resources = resolver.getResources(pattern);
            Arrays.sort(resources, RESOURCE_COMPARATOR);
            for (Resource resource : resources) {
                String source = readString(resource);
                String resolved = propertyResolver.resolvePlaceholders(source);
                openAPI = mapper.readerForUpdating(new ConfigWrapper(openAPI)).readValue(resolved, ConfigWrapper.class).openAPI;
            }
        }
        return openAPI;
    }

    private String readString(Resource resource) throws IOException {
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConfigWrapper {
        public final OpenAPI openAPI;

        public ConfigWrapper(OpenAPI openAPI) {
            this.openAPI = openAPI;
        }
    }
}
