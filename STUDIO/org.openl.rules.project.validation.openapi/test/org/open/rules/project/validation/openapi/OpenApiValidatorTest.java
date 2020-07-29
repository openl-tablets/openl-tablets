package org.open.rules.project.validation.openapi;

import static org.junit.Assert.assertFalse;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class OpenApiValidatorTest {

    public static final String DIR = "test-resources/functionality/";
    private Locale defaultLocale;
    private TimeZone defaultTimeZone;

    @Before
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        defaultTimeZone = TimeZone.getDefault();
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void testOpenApiValidator() {
        final RulesInFolderTestRunnerWithOpenApiValidator rulesInFolderTestRunnerWithOpenApiValidator = new RulesInFolderTestRunnerWithOpenApiValidator(
            false,
            false);
        assertFalse("Test is failed.", rulesInFolderTestRunnerWithOpenApiValidator.run(DIR));
    }

}
