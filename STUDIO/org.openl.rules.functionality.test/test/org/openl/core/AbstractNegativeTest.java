package org.openl.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNegativeTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNegativeTest.class);
    
    protected boolean anyMessageFileExists(File sourceDir, String projectFile) {
        for (Severity severity : Severity.values()) {
            File messagesFile = new File(sourceDir, projectFile + "." + severity.name().toLowerCase() + ".txt");
            if (messagesFile.exists()) {
                return true;
            }
        }

        return false;
    }

    protected boolean isHasMessages(File sourceDir,
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

    protected List<String> getExpectedMessages(File file) throws IOException {
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

    protected boolean isMessageExists(List<OpenLMessage> actualMessages, String expectedMessage, Severity severity) {
        for (OpenLMessage message : actualMessages) {
            if (message.getSummary().equals(expectedMessage) && message.getSeverity() == severity) {
                return true;
            }
        }
        return false;
    }
}
