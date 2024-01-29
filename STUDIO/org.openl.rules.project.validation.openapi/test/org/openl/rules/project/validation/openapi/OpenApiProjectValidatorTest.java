package org.openl.rules.project.validation.openapi;

import static org.junit.jupiter.api.Assertions.assertFalse;


import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class OpenApiProjectValidatorTest {

    public static final String DIR = "test-resources/functionality/";
    private Locale defaultLocale;
    private TimeZone defaultTimeZone;

    @BeforeEach
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        defaultTimeZone = TimeZone.getDefault();
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void testOpenApiValidator() {
        final RulesInFolderTestRunnerWithOpenApiValidator rulesInFolderTestRunnerWithOpenApiValidator = new RulesInFolderTestRunnerWithOpenApiValidator(
            false,
            false);
        assertFalse(rulesInFolderTestRunnerWithOpenApiValidator.run(DIR), "Test is failed.");
    }

}
