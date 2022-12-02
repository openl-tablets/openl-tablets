package org.openl.exception;

import javax.xml.bind.annotation.XmlRootElement;

public class OpenLUserDetailedRuntimeException extends OpenLUserRuntimeException {

    private final Object body;

    public OpenLUserDetailedRuntimeException(String code, String message) {
        super(message);
        this.body = new Body(code, message);
    }

    public OpenLUserDetailedRuntimeException(Object body) {
        this.body = body;
    }

    public Object getBody() {
        return body;
    }

    @XmlRootElement
    public static final class Body {
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

        @Override
        public String toString() {
            return String.format("%s: %s", code, message);
        }
    }
}
