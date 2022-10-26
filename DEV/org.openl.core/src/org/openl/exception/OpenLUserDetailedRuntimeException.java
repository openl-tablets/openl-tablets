package org.openl.exception;

public class OpenLUserDetailedRuntimeException extends OpenLUserRuntimeException {

    private final Object body;

    public OpenLUserDetailedRuntimeException(Body body) {
        super(body.getMessage());
        this.body = body;
    }

    public OpenLUserDetailedRuntimeException(Object body) {
        this.body = body;
    }

    public Object getBody() {
        return body;
    }

    public static class Body {
        private final String message;
        private final String code;

        public Body(String code, String message) {
            this.message = message;
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public String getCode() {
            return code;
        }

        public String getFullMessage() {
            return String.format("%s: %s", code, message);
        }
    }
}
