package org.openl.rules.rest.model;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;

public class NotificationModel {

    @Parameter(description = "Localized notification message")
    private String message;

    @Parameter(description = "Notification code")
    private String code;

    @Parameter(description = "Message arguments")
    private List<Object> args;

    public NotificationModel(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public List<Object> getArgs() {
        return args;
    }
}
