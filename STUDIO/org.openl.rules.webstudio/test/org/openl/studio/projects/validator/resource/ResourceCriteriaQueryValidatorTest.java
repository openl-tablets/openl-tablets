package org.openl.studio.projects.validator.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.validation.BindingResult;

import org.openl.studio.common.validation.AbstractConstraintValidatorTest;
import org.openl.studio.projects.service.resources.ResourceCriteriaQuery;
import org.openl.studio.projects.validator.MockConfiguration;

@SpringJUnitConfig(classes = MockConfiguration.class)
class ResourceCriteriaQueryValidatorTest extends AbstractConstraintValidatorTest {

    @Autowired
    private ResourceCriteriaQueryValidator validator;

    // --- valid queries ---

    @Test
    void validate_defaultQuery_noErrors() {
        var query = ResourceCriteriaQuery.builder().build();
        assertNull(validateAndGetResult(query, validator));
    }

    @Test
    void validate_validBasePath_noErrors() {
        var query = ResourceCriteriaQuery.builder().basePath("rules/UK").build();
        assertNull(validateAndGetResult(query, validator));
    }

    @Test
    void validate_validExtensions_noErrors() {
        var query = ResourceCriteriaQuery.builder().extensions(Set.of("xlsx", "xml")).build();
        assertNull(validateAndGetResult(query, validator));
    }

    @Test
    void validate_validNamePattern_noErrors() {
        var query = ResourceCriteriaQuery.builder().namePattern("Model").build();
        assertNull(validateAndGetResult(query, validator));
    }

    // --- basePath validation ---

    @Test
    void validate_basePathWithNullByte_rejectsBasePath() {
        var query = ResourceCriteriaQuery.builder().basePath("rules/\0evil").build();
        var result = validateAndGetResult(query, validator);
        assertFieldError("basePath", "The base path 'rules/\0evil' is not valid.", "rules/\0evil",
                result.getFieldError("basePath"));
    }

    @Test
    void validate_basePathStartingWithSlash_rejectsBasePath() {
        var query = ResourceCriteriaQuery.builder().basePath("/etc/passwd").build();
        assertBasePathInvalid(query, "/etc/passwd");
    }

    @Test
    void validate_basePathStartingWithBackslash_rejectsBasePath() {
        var query = ResourceCriteriaQuery.builder().basePath("\\windows").build();
        assertBasePathInvalid(query, "\\windows");
    }

    @Test
    void validate_basePathWithTraversal_rejectsBasePath() {
        var query = ResourceCriteriaQuery.builder().basePath("rules/../../../etc").build();
        assertBasePathInvalid(query, "rules/../../../etc");
    }

    @Test
    void validate_basePathWithDotDot_rejectsBasePath() {
        var query = ResourceCriteriaQuery.builder().basePath("..").build();
        assertBasePathInvalid(query, "..");
    }

    // --- namePattern validation ---

    @Test
    void validate_namePatternTooLong_rejectsNamePattern() {
        var longPattern = "a".repeat(256);
        var query = ResourceCriteriaQuery.builder().namePattern(longPattern).build();
        var result = validateAndGetResult(query, validator);
        assertEquals(1, result.getFieldErrorCount());
        assertNotNull(result.getFieldError());
        assertEquals("namePattern", result.getFieldError().getField());
    }

    @Test
    void validate_namePatternMaxLength_noErrors() {
        var query = ResourceCriteriaQuery.builder().namePattern("a".repeat(255)).build();
        assertNull(validateAndGetResult(query, validator));
    }

    @Test
    void validate_namePatternWithForwardSlash_rejectsNamePattern() {
        var query = ResourceCriteriaQuery.builder().namePattern("foo/bar").build();
        var result = validateAndGetResult(query, validator);
        assertEquals(1, result.getFieldErrorCount());
        assertNotNull(result.getFieldError());
        assertEquals("namePattern", result.getFieldError().getField());
    }

    @Test
    void validate_namePatternWithBackslash_rejectsNamePattern() {
        var query = ResourceCriteriaQuery.builder().namePattern("foo\\bar").build();
        var result = validateAndGetResult(query, validator);
        assertEquals(1, result.getFieldErrorCount());
        assertNotNull(result.getFieldError());
        assertEquals("namePattern", result.getFieldError().getField());
    }

    // --- extensions validation ---

    @Test
    void validate_extensionTooLong_rejectsExtension() {
        var query = ResourceCriteriaQuery.builder().extension("a".repeat(21)).build();
        var result = validateAndGetResult(query, validator);
        assertEquals(1, result.getFieldErrorCount());
        assertNotNull(result.getFieldError());
        assertEquals("extensions", result.getFieldError().getField());
    }

    @Test
    void validate_extensionWithSpecialChars_rejectsExtension() {
        var query = ResourceCriteriaQuery.builder().extension("xl$x").build();
        var result = validateAndGetResult(query, validator);
        assertEquals(1, result.getFieldErrorCount());
        assertNotNull(result.getFieldError());
        assertEquals("extensions", result.getFieldError().getField());
    }

    @Test
    void validate_extensionWithDot_rejectsExtension() {
        var query = ResourceCriteriaQuery.builder().extension(".xlsx").build();
        var result = validateAndGetResult(query, validator);
        assertEquals(1, result.getFieldErrorCount());
    }

    // --- conflicting filters ---

    @Test
    void validate_foldersOnlyWithExtensions_rejectsConflict() {
        var query = ResourceCriteriaQuery.builder()
                .foldersOnly(true)
                .extension("xlsx")
                .build();
        var result = validateAndGetResult(query, validator);
        assertEquals(1, result.getFieldErrorCount());
        assertNotNull(result.getFieldError());
        assertEquals("extensions", result.getFieldError().getField());
    }

    @Test
    void validate_foldersOnlyWithoutExtensions_noErrors() {
        var query = ResourceCriteriaQuery.builder().foldersOnly(true).build();
        assertNull(validateAndGetResult(query, validator));
    }

    @Test
    void validate_extensionsWithoutFoldersOnly_noErrors() {
        var query = ResourceCriteriaQuery.builder().extension("xlsx").build();
        assertNull(validateAndGetResult(query, validator));
    }

    // --- helpers ---

    private void assertBasePathInvalid(ResourceCriteriaQuery query, String basePath) {
        BindingResult result = validateAndGetResult(query, validator);
        assertEquals(1, result.getFieldErrorCount());
        assertFieldError("basePath",
                "The base path '%s' is not valid.".formatted(basePath),
                basePath,
                result.getFieldError("basePath"));
    }
}
