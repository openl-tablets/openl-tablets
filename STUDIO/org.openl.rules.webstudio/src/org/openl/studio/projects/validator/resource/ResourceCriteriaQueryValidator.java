package org.openl.studio.projects.validator.resource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.openl.studio.projects.service.resources.ResourceCriteriaQuery;
import org.openl.util.StringUtils;

/**
 * Validator for {@link ResourceCriteriaQuery}.
 * Validates query parameters for security and correctness.
 */
@Component
public class ResourceCriteriaQueryValidator implements Validator {

    private static final int MAX_NAME_PATTERN_LENGTH = 255;
    private static final int MAX_EXTENSION_LENGTH = 20;

    @Override
    public boolean supports(Class<?> clazz) {
        return ResourceCriteriaQuery.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        var query = (ResourceCriteriaQuery) target;

        validateBasePath(query.basePath(), errors);
        validateNamePattern(query.namePattern(), errors);
        validateExtensions(query.extensions(), errors);
        validateConflictingFilters(query, errors);
    }

    /**
     * Validates base path for path traversal attacks.
     */
    private void validateBasePath(String basePath, Errors errors) {
        if (StringUtils.isBlank(basePath)) {
            return;
        }

        // Null byte injection check
        if (basePath.contains("\0")) {
            errors.rejectValue("basePath", "resource.base-path.invalid.message", new Object[]{basePath}, null);
            return;
        }

        // Quick check for obvious attacks
        if (basePath.startsWith("/") || basePath.startsWith("\\") || basePath.contains("..")) {
            errors.rejectValue("basePath", "resource.base-path.invalid.message", new Object[]{basePath}, null);
            return;
        }

        try {
            // Normalize and validate
            Path normalized = Paths.get(basePath).normalize();

            // Check if normalization changed the path (traversal attempt detected)
            String normalizedStr = normalized.toString().replace('\\', '/');
            String originalNormalized = basePath.replace('\\', '/');
            if (!originalNormalized.equals(normalizedStr)) {
                errors.rejectValue("basePath", "resource.base-path.invalid.message", new Object[]{basePath}, null);
                return;
            }

            // Verify it's not absolute
            if (normalized.isAbsolute()) {
                errors.rejectValue("basePath", "resource.base-path.invalid.message", new Object[]{basePath}, null);
            }
        } catch (IllegalArgumentException e) {
            errors.rejectValue("basePath", "resource.base-path.invalid.message", new Object[]{basePath}, null);
        }
    }

    private void validateNamePattern(String namePattern, Errors errors) {
        if (namePattern == null) {
            return;
        }

        if (namePattern.length() > MAX_NAME_PATTERN_LENGTH) {
            errors.rejectValue("namePattern", "resource.name-pattern.too-long.message");
            return;
        }

        if (namePattern.contains("/") || namePattern.contains("\\")) {
            errors.rejectValue("namePattern", "resource.name-pattern.invalid.message");
        }
    }

    private void validateExtensions(Set<String> extensions, Errors errors) {
        if (extensions == null || extensions.isEmpty()) {
            return;
        }

        for (String ext : extensions) {
            if (ext == null || ext.length() > MAX_EXTENSION_LENGTH || !ext.matches("^[a-zA-Z0-9]+$")) {
                errors.rejectValue("extensions", "resource.extension.invalid.message", new Object[]{ext}, null);
                return;
            }
        }
    }

    private void validateConflictingFilters(ResourceCriteriaQuery query, Errors errors) {
        if (query.foldersOnly() && !query.extensions().isEmpty()) {
            errors.rejectValue("extensions", "resource.filters.conflict.message");
        }
    }
}
