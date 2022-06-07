package org.openl.rules.testmethod;

import java.util.Objects;

import org.openl.exception.OpenLUserLocalizedRuntimeException;

public class TestError {

    private String message;
    private String code;
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
        return Objects.equals(message, testError.message) && Objects.equals(code, testError.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, code);
    }

    @Override
    public String toString() {
        if (oldStyle) {
            return message;
        } else {
            return String.format("%s: %s", code, message);
        }
    }

    public static TestError from(OpenLUserLocalizedRuntimeException ex) {
        var error = new TestError();
        error.setMessage(ex.getMessage());
        error.setCode(ex.getCode());
        return error;
    }

    public static TestError from(Throwable ex) {
        return new TestError(ex.getMessage());
    }
}
