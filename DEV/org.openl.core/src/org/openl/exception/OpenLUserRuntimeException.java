package org.openl.exception;

import jakarta.xml.bind.annotation.XmlRootElement;

import org.openl.util.print.NicePrinter;

public class OpenLUserRuntimeException extends OpenLRuntimeException {

    private static final long serialVersionUID = -6327856390127472929L;
    protected final Object body;

    public OpenLUserRuntimeException(String message) {
        this.body = new UserMessage(message);
    }

    public OpenLUserRuntimeException(String code, String message) {
        this.body = new UserCodedMessage(code, message);
    }

    public OpenLUserRuntimeException(Object body) {
        this.body = body;
    }

    public Object getBody() {
        return body;
    }

    @Override
    public String getMessage() {
        return NicePrinter.print(body);
    }

    @XmlRootElement
    public static final class UserMessage {
        public final String message;
        public final MessageType type = MessageType.USER_ERROR;

        public UserMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }

        enum MessageType {
            USER_ERROR
        }
    }

    @XmlRootElement
    public static final class UserCodedMessage {
        public final String message;
        public final String code;

        public UserCodedMessage(String code, String message) {
            this.message = message;
            this.code = code;
        }

        @Override
        public String toString() {
            return String.format("%s: %s", code, message);
        }
    }
}
