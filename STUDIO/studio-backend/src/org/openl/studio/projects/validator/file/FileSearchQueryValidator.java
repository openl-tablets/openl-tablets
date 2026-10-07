package org.openl.studio.projects.validator.file;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.openl.studio.projects.service.files.FileSearchQuery;

/**
 * Validator for {@link FileSearchQuery}.
 *
 * <p>Bounds the free-text criteria so a search request cannot drive unbounded work: the path
 * pattern and the content needle are length-limited, and each extension must be a short alphanumeric
 * token. Path safety of the {@code from} anchor is enforced downstream by the lookup that consumes it.
 *
 * @author Yury Molchan
 */
@Component
public class FileSearchQueryValidator implements Validator {

    private static final int MAX_PATTERN_LENGTH = 255;
    private static final int MAX_CONTENT_LENGTH = 255;
    private static final int MAX_EXTENSION_LENGTH = 20;

    @Override
    public boolean supports(Class<?> clazz) {
        return FileSearchQuery.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        var query = (FileSearchQuery) target;

        validatePattern(query.pattern(), errors);
        validateContent(query.content(), errors);
        validateExtensions(query.extensions(), errors);
    }

    private void validatePattern(String pattern, Errors errors) {
        if (pattern != null && pattern.length() > MAX_PATTERN_LENGTH) {
            errors.rejectValue("pattern", "file.search.pattern.too-long.message");
        }
    }

    private void validateContent(String content, Errors errors) {
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            errors.rejectValue("content", "file.search.content.too-long.message");
        }
    }

    private void validateExtensions(Set<String> extensions, Errors errors) {
        if (extensions == null || extensions.isEmpty()) {
            return;
        }
        for (String ext : extensions) {
            if (ext == null || ext.length() > MAX_EXTENSION_LENGTH || !ext.matches("^[a-zA-Z0-9]+$")) {
                errors.rejectValue("extensions", "file.extension.invalid.message", new Object[]{ext}, null);
                return;
            }
        }
    }
}
