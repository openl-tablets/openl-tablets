package org.openl.core;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
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

public class NegativeTest {
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
        System.out.println(">>> Negative tests...");
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
                System.out.println("!!! Cannot read file [" + sourceFile + "]. Because: " + ex.getMessage());
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
                    if (!isHasMessages(sourceDir, sourceFile, actualMessages, severity)) {
                        hasAllMessages = false;
                    }
                }

                if (hasAllMessages) {
                    System.out.println("+++ OK in [" + sourceFile + "]. ");
                } else {
                    hasErrors = true;
                }
            } else {
                if (!compiledOpenClass.hasErrors()) {
                    System.out.println("!!! No errors in [" + sourceFile + "].");
                    hasErrors = true;
                } else {
                    System.out.println("+++ OK in [" + sourceFile + "]. ");
                }
            }
        }
        assertFalse("Failed test", hasErrors);
    }

    private boolean anyMessageFileExists(File sourceDir, String sourceFile) {
        for (Severity severity : Severity.values()) {
            File messagesFile = new File(sourceDir, sourceFile + "." + severity.name().toLowerCase() + ".txt");
            if (messagesFile.exists()) {
                return true;
            }
        }

        return false;
    }

    private boolean isHasMessages(File sourceDir, String sourceFile, List<OpenLMessage> actualMessages, Severity severity) {
        boolean hasAllMessages = true;

        File messagesFile = new File(sourceDir, sourceFile + "." + severity.name().toLowerCase() + ".txt");
        try {
            for (String expectedMessage : getExpectedMessages(messagesFile)) {
                if (!isMessageExists(actualMessages, expectedMessage, severity)) {
                    System.out.println("!!! The message \"" + expectedMessage + "\" with severity " + severity.name() + " is not found in [" + sourceFile + "].");
                    hasAllMessages = false;
                }
            }
        } catch (IOException e) {
            System.out.println("!!! Cannot read file [" + messagesFile + "]. Because: " + e.getMessage());
            hasAllMessages = false;
        }
        return hasAllMessages;
    }

    private List<String> getExpectedMessages(File messagesFile) throws IOException {
        List<String> result = new ArrayList<>();

        if (!messagesFile.exists()) {
            return result;
        }

        String content = IOUtils.toStringAndClose(new FileInputStream(messagesFile));
        for (String message : content.split("\\R")) {
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
