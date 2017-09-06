package org.openl.core;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NegativeProjectsTest {
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

    private boolean anyMessageFileExists(File sourceDir, String projectFile) {
        for (Severity severity : Severity.values()) {
            File messagesFile = new File(sourceDir, projectFile + "." + severity.name().toLowerCase() + ".txt");
            if (messagesFile.exists()) {
                return true;
            }
        }

        return false;
    }

    private boolean isHasMessages(File sourceDir,
            String projectFile,
            List<OpenLMessage> actualMessages,
            Severity severity) {
        boolean hasAllMessages = true;

        File file = new File(sourceDir, projectFile + "." + severity.name().toLowerCase() + ".txt");
        try {
            for (String expectedMessage : getExpectedMessages(file)) {
                if (!isMessageExists(actualMessages, expectedMessage, severity)) {
                    LOG.error("The message \"" + expectedMessage + "\" with severity " + severity
                        .name() + " is expected for [" + projectFile + "].");
                    hasAllMessages = false;
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to read file [" + file + "].", e);
            hasAllMessages = false;
        }
        return hasAllMessages;
    }

    private List<String> getExpectedMessages(File file) throws IOException {
        List<String> result = new ArrayList<>();

        if (!file.exists()) {
            return result;
        }

        String content = IOUtils.toStringAndClose(new FileInputStream(file));
        for (String message : content.split("\\u000D\\u000A|[\\u000A\\u000B\\u000C\\u000D\\u0085\\u2028\\u2029]")) {
            if (!StringUtils.isBlank(message)) {
                result.add(message.trim());
            }
        }

        return result;
    }

    private boolean isMessageExists(List<OpenLMessage> actualMessages, String expectedMessage, Severity severity) {
        for (OpenLMessage message : actualMessages) {
            if (message.getSummary().equals(expectedMessage) && message.getSeverity() == severity) {
                return true;
            }
        }
        return false;
    }
}
