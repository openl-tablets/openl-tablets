package org.openl.studio.common.projection;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.openl.util.StringUtils;

/**
 * Configuration of the REST response field projection feature (the {@code ?fields=} query parameter).
 *
 * <p>All settings are optional and have backward-compatible defaults: projection only kicks in when a
 * client explicitly sends the {@code fields} parameter for a response DTO located in one of the
 * {@link #getBasePackages() base packages}.
 *
 * @author Vladyslav Pikus
 */
@Component
public class FieldProjectionProperties {

    /**
     * Response DTO packages projection is allowed to touch. Everything outside these packages
     * (error responses, actuator, Spring internals, binary payloads, ...) is never projected.
     */
    static final List<String> DEFAULT_BASE_PACKAGES = List.of(
            "org.openl.rules.rest.model",
            "org.openl.rules.rest.acl.model",
            "org.openl.studio.settings.model",
            "org.openl.studio.projects.model",
            "org.openl.studio.repositories.model",
            "org.openl.studio.tags.model",
            "org.openl.studio.deployment.model",
            "org.openl.studio.security.pat.model");

    static final String DEFAULT_PARAMETER_NAME = "fields";

    private final boolean enabled;
    private final boolean failOnUnknownField;
    private final String parameterName;
    private final List<String> basePackages;

    public FieldProjectionProperties(
            @Value("${openl.rest.field-projection.enabled:true}") boolean enabled,
            @Value("${openl.rest.field-projection.fail-on-unknown-field:false}") boolean failOnUnknownField,
            @Value("${openl.rest.field-projection.parameter-name:" + DEFAULT_PARAMETER_NAME + "}") String parameterName,
            @Value("${openl.rest.field-projection.base-packages:}") List<String> basePackages) {
        this.enabled = enabled;
        this.failOnUnknownField = failOnUnknownField;
        this.parameterName = StringUtils.isBlank(parameterName) ? DEFAULT_PARAMETER_NAME : parameterName.trim();
        this.basePackages = normalize(basePackages);
    }

    private static List<String> normalize(List<String> basePackages) {
        if (basePackages == null) {
            return DEFAULT_BASE_PACKAGES;
        }
        var cleaned = basePackages.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
        return cleaned.isEmpty() ? DEFAULT_BASE_PACKAGES : cleaned;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * When {@code true}, requesting a field that does not exist on the target DTO results in a
     * {@code 400 Bad Request}. When {@code false} (default), unknown fields are silently ignored.
     */
    public boolean isFailOnUnknownField() {
        return failOnUnknownField;
    }

    public String getParameterName() {
        return parameterName;
    }

    public List<String> getBasePackages() {
        return basePackages;
    }
}
