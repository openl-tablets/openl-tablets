package org.openl.exception;

import java.io.PrintWriter;

import org.apache.commons.lang.StringUtils;
import org.openl.main.SourceCodeURLTool;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

public class OpenLExceptionUtils {

    private OpenLExceptionUtils() {
    }

    public static void printError(OpenLException error, PrintWriter writer) {

        Throwable cause = error.getOriginalCause();

        String message;

        if (cause != null && cause instanceof CompositeSyntaxNodeException) {

            CompositeSyntaxNodeException syntaxErrorException = (CompositeSyntaxNodeException) cause;

            for (int i = 0; i < syntaxErrorException.getErrors().length; i++) {
                printError(syntaxErrorException.getErrors()[i], writer);
            }

            return;

        } else {
            message = error.getOriginalMessage();
        }

        writer.println("Error: " + message);

        SourceCodeURLTool.printCodeAndError(error.getLocation(), error.getSourceModule(), writer);
        SourceCodeURLTool.printSourceLocation(error, writer);

        if (error.getOriginalCause() != null) {
            error.getOriginalCause().printStackTrace(writer);
        }
    }

    public static String[] getErrorCode(OpenLException error) {
        ILocation location = error.getLocation();
        IOpenSourceCodeModule sourceModule = error.getSourceModule();

        String code = null;
        if (sourceModule != null) {
            code = sourceModule.getCode();
            if (StringUtils.isBlank(code)) {
                code = StringUtils.EMPTY;
            }
        }

        int pstart = 0;
        int pend = 0;

        if (StringUtils.isNotBlank(code)
                && location != null && location.isTextLocation()) {
            TextInfo info = new TextInfo(code);
            pstart = location.getStart().getAbsolutePosition(info);
            pend = Math.min(location.getEnd().getAbsolutePosition(info) + 1, code.length());
        }

        if (pend != 0) {
            return new String[] {
                    code.substring(0, pstart),
                    code.substring(pstart, pend),
                    code.substring(pend, code.length())};
        }

        return new String[0];
    }

}
