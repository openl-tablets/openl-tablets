package org.openl.studio.projects.validator.file;

import java.nio.file.Path;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.openl.studio.projects.service.files.FileCriteriaQuery;
import org.openl.util.StringUtils;

/**
 * Validator for {@link FileCriteriaQuery}.
 * Validates query parameters for security and correctness.
 */
@Component
public class FileCriteriaQueryValidator implements Validator {

    private static final int MAX_NAME_PATTERN_LENGTH = 255;
    private static final int MAX_EXTENSION_LENGTH = 20;
    private static final String BASE_PATH_FIELD = "basePath";
    private static final String BASE_PATH_INVALID = "file.base-path.invalid.message";
    private static final String EXTENSIONS_FIELD = "extensions";

    @Override
    public boolean supports(Class<?> clazz) {
        return FileCriteriaQuery.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        var query = (FileCriteriaQuery) target;

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
            errors.rejectValue(BASE_PATH_FIELD, BASE_PATH_INVALID, new Object[]{basePath}, null);
            return;
        }

        // Quick check for obvious attacks
        if (basePath.startsWith("/") || basePath.startsWith("\\") || basePath.contains("..")) {
            errors.rejectValue(BASE_PATH_FIELD, BASE_PATH_INVALID, new Object[]{basePath}, null);
            return;
        }

        try {
            // Normalize and validate
            Path normalized = Path.of(basePath).normalize();

            // Check if normalization changed the path (traversal attempt detected)
            String normalizedStr = normalized.toString().replace('\\', '/');
            String originalNormalized = basePath.replace('\\', '/');
            if (!originalNormalized.equals(normalizedStr)) {
                errors.rejectValue(BASE_PATH_FIELD, BASE_PATH_INVALID, new Object[]{basePath}, null);
                return;
            }

            // Verify it's not absolute
            if (normalized.isAbsolute()) {
                errors.rejectValue(BASE_PATH_FIELD, BASE_PATH_INVALID, new Object[]{basePath}, null);
            }
        } catch (IllegalArgumentException e) {
            errors.rejectValue(BASE_PATH_FIELD, BASE_PATH_INVALID, new Object[]{basePath}, null);
        }
    }

    private void validateNamePattern(String namePattern, Errors errors) {
        if (namePattern == null) {
            return;
        }

        if (namePattern.length() > MAX_NAME_PATTERN_LENGTH) {
            errors.rejectValue("namePattern", "file.name-pattern.too-long.message");
            return;
        }

        if (namePattern.contains("/") || namePattern.contains("\\")) {
            errors.rejectValue("namePattern", "file.name-pattern.invalid.message");
        }
    }

    private void validateExtensions(Set<String> extensions, Errors errors) {
        if (extensions == null || extensions.isEmpty()) {
            return;
        }

        for (String ext : extensions) {
            if (ext == null || ext.length() > MAX_EXTENSION_LENGTH || !ext.matches("^[a-zA-Z0-9]+$")) {
                errors.rejectValue(EXTENSIONS_FIELD, "file.extension.invalid.message", new Object[]{ext}, null);
                return;
            }
        }
    }

    private void validateConflictingFilters(FileCriteriaQuery query, Errors errors) {
        if (query.foldersOnly() && !query.extensions().isEmpty()) {
            errors.rejectValue(EXTENSIONS_FIELD, "file.filters.conflict.message");
        }
    }
}
