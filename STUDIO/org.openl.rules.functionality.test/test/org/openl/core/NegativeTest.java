package org.openl.core;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.runtime.RulesEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NegativeTest extends AbstractNegativeTest {
    private static final Logger LOG = LoggerFactory.getLogger(NegativeTest.class);

    public static final String DIR = "test-resources/negative-tests/";
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
    public void testAllExcelFiles() throws NoSuchMethodException {
        LOG.info(">>> Negative tests...");

        boolean hasErrors = false;
        final File sourceDir = new File(DIR);
        final String[] files = sourceDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.endsWith(".xlsx") || name.endsWith(".xls");
            }
        });
        for (String sourceFile : files) {

            try {
                new FileInputStream(new File(sourceDir, sourceFile)).close();
            } catch (Exception ex) {
                LOG.error("Failed to read file [" + sourceFile + "]", ex);
                hasErrors = true;
                continue;
            }

            RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(DIR + sourceFile);
            engineFactory.setExecutionMode(false);
            final CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();

            if (anyMessageFileExists(sourceDir, sourceFile)) {
                List<OpenLMessage> actualMessages = compiledOpenClass.getMessages();
                boolean hasAllMessages = true;
                for (Severity severity : Severity.values()) {
                    if (!assertMessages(sourceDir, sourceFile, actualMessages, severity)) {
                        hasAllMessages = false;
                    }
                }

                if (hasAllMessages) {
                    LOG.info("OK in [" + sourceFile + "].");
                } else {
                    hasErrors = true;
                }
            } else {
                if (!compiledOpenClass.hasErrors()) {
                    LOG.error("Expected compilation errors in [" + sourceFile + "].");
                    hasErrors = true;
                } else {
                    LOG.info("OK in [" + sourceFile + "].");
                }
            }
        }

        assertFalse("Some tests have been failed!", hasErrors);
    }
}
