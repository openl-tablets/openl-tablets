package org.openl.rules.webstudio.services;

/**
 * ServiceResult
 *
 * @author Andrey Naumenko
 */
public class ServiceResult {
    private String logMessages;

    /**
     * Service log messages, divided by new line
     *
     * @return
     */
    public String getLogMessages() {
        return logMessages;
    }

    public void setLogMessages(String logMessages) {
        this.logMessages = logMessages;
    }
}
