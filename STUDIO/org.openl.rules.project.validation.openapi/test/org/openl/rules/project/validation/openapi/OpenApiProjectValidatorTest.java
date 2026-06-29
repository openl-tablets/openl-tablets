package org.openl.rules.project.validation.openapi;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class OpenApiProjectValidatorTest {

    private static final String DIR = "test-resources/functionality/";
    private Locale defaultLocale;
    private TimeZone defaultTimeZone;

    @BeforeEach
    void setupLocale() {
        defaultLocale = Locale.getDefault();
        defaultTimeZone = TimeZone.getDefault();
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(defaultLocale);
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    void testOpenApiValidator() {
        final RulesInFolderTestRunnerWithOpenApiValidator rulesInFolderTestRunnerWithOpenApiValidator = new RulesInFolderTestRunnerWithOpenApiValidator(
                false,
                false);
        assertFalse(rulesInFolderTestRunnerWithOpenApiValidator.run(DIR), "Test is failed.");
    }

}
