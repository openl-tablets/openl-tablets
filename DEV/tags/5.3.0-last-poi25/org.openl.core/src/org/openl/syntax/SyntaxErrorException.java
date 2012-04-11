/*
 * Created on Jun 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.syntax;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author snshor
 *
 */
public class SyntaxErrorException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 6239517302604363701L;
    ISyntaxError[] syntaxErrors;
    String msg;

    public SyntaxErrorException(String msg, ISyntaxError[] syntaxErrors) {
        this.syntaxErrors = syntaxErrors;
    }

    @Override
    public String getMessage() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        if (msg != null) {
            pw.println(msg);
        }

        for (int i = 0; i < syntaxErrors.length; ++i) {
            pw.println(syntaxErrors[i]);
        }

        pw.close();
        // sw.close();
        return sw.toString();

    }

    /**
     * @return
     */
    public ISyntaxError[] getSyntaxErrors() {
        return syntaxErrors;
    }

}
