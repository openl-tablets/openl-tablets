package org.openl.syntax.exception.formatter;

public class ClassCastExceptionFormatter implements ExceptionMessageFormatter {

    @Override
    public String format(Throwable error) {
        if (error instanceof ClassCastException) {
            ClassCastException classCastException = (ClassCastException) error;
            String msg = classCastException.getMessage();
            if (msg.startsWith("class ")) {
                msg = msg.substring("class ".length());
            }
            if (msg.contains(" (")) {
                msg = msg.substring(0, msg.indexOf(" ("));
            }
            String classFrom = msg.substring(0, msg.indexOf(" "));
            String classTo = msg.substring(msg.lastIndexOf(" ") + 1);
            return String.format("Class '%s' cannot be cast to class '%s'.", classFrom, classTo);
        }
        return error.getMessage();

    }

}
