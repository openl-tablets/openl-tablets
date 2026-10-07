package org.openl.studio.projects.validator.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.studio.common.validation.AbstractConstraintValidatorTest;
import org.openl.studio.projects.service.files.FileSearchQuery;
import org.openl.studio.projects.validator.MockConfiguration;

@SpringJUnitConfig(classes = MockConfiguration.class)
class FileSearchQueryValidatorTest extends AbstractConstraintValidatorTest {

    @Autowired
    private FileSearchQueryValidator validator;

    // --- valid queries ---

    @Test
    void validate_defaultQuery_noErrors() {
        var query = FileSearchQuery.builder().build();
        assertNull(validateAndGetResult(query, validator));
    }

    @Test
    void validate_antGlobPattern_noErrors() {
        var query = FileSearchQuery.builder().pattern("**/sub").build();
        assertNull(validateAndGetResult(query, validator));
    }

    @Test
    void validate_pathPattern_noErrors() {
        var query = FileSearchQuery.builder().pattern("services/rating/AGENTS.md").build();
        assertNull(validateAndGetResult(query, validator));
    }

    @Test
    void validate_validContent_noErrors() {
        var query = FileSearchQuery.builder().content("Beta nested").build();
        assertNull(validateAndGetResult(query, validator));
    }

    @Test
    void validate_validExtensions_noErrors() {
        var query = FileSearchQuery.builder().extension("properties").build();
        assertNull(validateAndGetResult(query, validator));
    }

    // --- pattern length ---

    @Test
    void validate_patternTooLong_rejectsPattern() {
        var longPattern = "a".repeat(256);
        var query = FileSearchQuery.builder().pattern(longPattern).build();
        var result = validateAndGetResult(query, validator);
        assertFieldError("pattern", "The search pattern is too long. Maximum length is 255 characters.",
                longPattern, result.getFieldError("pattern"));
    }

    @Test
    void validate_patternMaxLength_noErrors() {
        var query = FileSearchQuery.builder().pattern("a".repeat(255)).build();
        assertNull(validateAndGetResult(query, validator));
    }

    // --- content length ---

    @Test
    void validate_contentTooLong_rejectsContent() {
        var longContent = "a".repeat(256);
        var query = FileSearchQuery.builder().content(longContent).build();
        var result = validateAndGetResult(query, validator);
        assertFieldError("content", "The search content is too long. Maximum length is 255 characters.",
                longContent, result.getFieldError("content"));
    }

    @Test
    void validate_contentMaxLength_noErrors() {
        var query = FileSearchQuery.builder().content("a".repeat(255)).build();
        assertNull(validateAndGetResult(query, validator));
    }

    // --- extensions ---

    @Test
    void validate_extensionTooLong_rejectsExtension() {
        var query = FileSearchQuery.builder().extension("a".repeat(21)).build();
        var result = validateAndGetResult(query, validator);
        assertEquals(1, result.getFieldErrorCount());
        assertNotNull(result.getFieldError());
        assertEquals("extensions", result.getFieldError().getField());
    }

    @Test
    void validate_extensionWithSpecialChars_rejectsExtension() {
        var query = FileSearchQuery.builder().extension("xl$x").build();
        var result = validateAndGetResult(query, validator);
        assertEquals(1, result.getFieldErrorCount());
        assertNotNull(result.getFieldError());
        assertEquals("extensions", result.getFieldError().getField());
    }
}
