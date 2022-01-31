package org.openl.rules.rest.model;

import java.util.List;

public class NotificationModel {

    private String message;

    private String code;

    private List<Object> args;

    public NotificationModel(String message, String code, List<Object> args) {
        this.message = message;
        this.code = code;
        this.args = args;
    }

    public NotificationModel(String message) {
        this.message = message;
    }

    public NotificationModel() {
    }

    public String getMessage() {
        return message;
    }

    public NotificationModel setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getCode() {
        return code;
    }

    public NotificationModel setCode(String code) {
        this.code = code;
        return this;
    }

    public List<Object> getArgs() {
        return args;
    }

    public NotificationModel setArgs(List<Object> args) {
        this.args = args;
        return this;
    }
}
