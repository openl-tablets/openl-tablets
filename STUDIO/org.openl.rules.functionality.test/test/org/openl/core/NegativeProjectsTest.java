package org.openl.core;

import static org.junit.Assert.assertFalse;

import java.io.File;
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

public final class NegativeProjectsTest extends AbstractNegativeTest {
    private static final Logger LOG = LoggerFactory.getLogger(NegativeProjectsTest.class);

    public static final String DIR = "test-resources/negative-projects/";
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
        LOG.info(">>> Negative project tests...");

        boolean hasErrors = false;
        final File sourceDir = new File(DIR);

        if (!sourceDir.exists()) {
            LOG.warn("Tests directory doesn't exist!");
            return;
        }

        for (File file : sourceDir.listFiles()) {
            if (file.isDirectory()) {
                RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(DIR + file.getName());
                engineFactory.setExecutionMode(false);
                final CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();

                if (anyMessageFileExists(sourceDir, file.getName())) {
                    List<OpenLMessage> actualMessages = compiledOpenClass.getMessages();
                    boolean hasAllMessages = true;
                    for (Severity severity : Severity.values()) {
                        if (!isHasMessages(sourceDir, file.getName(), actualMessages, severity)) {
                            hasAllMessages = false;
                        }
                    }

                    if (hasAllMessages) {
                        LOG.info("OK in [" + file.getName() + "].");
                    } else {
                        hasErrors = true;
                    }
                } else {
                    if (!compiledOpenClass.hasErrors()) {
                        LOG.error("Expected compilation errors in [" + file.getName() + "].");
                        hasErrors = true;
                    } else {
                        LOG.info("OK in [" + file.getName() + "].");
                    }
                }
            }
        }

        assertFalse("Some tests have been failed!", hasErrors);
    }
}
