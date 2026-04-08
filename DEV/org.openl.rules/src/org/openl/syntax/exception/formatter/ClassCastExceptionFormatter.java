package org.openl.syntax.exception.formatter;

public class ClassCastExceptionFormatter implements ExceptionMessageFormatter {

    @Override
    public String format(Throwable error) {
        if (error instanceof ClassCastException classCastException) {
            String msg = classCastException.getMessage();
            if (msg.startsWith("class ")) {
                msg = msg.substring("class ".length());
            }
            if (msg.contains(" (")) {
                msg = msg.substring(0, msg.indexOf(" ("));
            }
            String classFrom = msg.substring(0, msg.indexOf(' '));
            String classTo = msg.substring(msg.lastIndexOf(' ') + 1);
            return "Class '%s' cannot be cast to class '%s'.".formatted(classFrom, classTo);
        }
        return error.getMessage();

    }

}
