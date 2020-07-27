package org.open.rules.project.validation.openapi;

import static org.junit.Assert.assertFalse;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.test.RulesInFolderTestRunner;

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
    public void testAll() {
        final RulesInFolderTestRunner rulesInFolderTestRunner = new RulesInFolderTestRunner(false, false);
        assertFalse("Test is failed.", rulesInFolderTestRunner.run(DIR));
    }

}
