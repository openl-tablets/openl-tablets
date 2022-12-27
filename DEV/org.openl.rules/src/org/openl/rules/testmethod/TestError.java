package org.openl.rules.testmethod;

import java.util.Objects;

import org.openl.exception.OpenLUserRuntimeException;
import org.openl.util.print.NicePrinter;

public class TestError {

    private String message;
    private String code;
    private Object body;
    private boolean oldStyle;

    public TestError(String message) {
        this.message = message;
        this.oldStyle = true;
    }

    public TestError() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestError testError = (TestError) o;
        return Objects.equals(message, testError.message) && Objects.equals(code, testError.code) && Objects
            .equals(body, testError.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, code, body);
    }

    @Override
    public String toString() {
        if (oldStyle) {
            return message;
        } else if (body != null){
            return NicePrinter.print(body);
        } else {
            return String.format("%s: %s", code, message);
        }
    }

    public static TestError from(OpenLUserRuntimeException ex) {
        var error = new TestError();
        if (ex.getBody() instanceof OpenLUserRuntimeException.UserCodedMessage) {
            var body = (OpenLUserRuntimeException.UserCodedMessage) ex.getBody();
            error.setMessage(body.message);
            error.setCode(body.code);
        } else {
            error.setBody(ex.getBody());
        }
        return error;
    }

    public static TestError from(Throwable ex) {
        return new TestError(ex.getMessage());
    }
}
