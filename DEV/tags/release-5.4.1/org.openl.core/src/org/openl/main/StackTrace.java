/*
 * Created on Sep 8, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.main;

import java.text.MessageFormat;

/**
 * Just a prototype for now to have one place for stack trace
 * formatting/parsing.
 *
 * @author sam
 *
 */
public class StackTrace {
    static public final String OPENL_FORMAT = "\t" + "at file: {0}(line:{1})";

    static public final String OPENL_REGEXP = "*file: *(line:*)";

    public String fileName;

    public int line;

    // Hyperlink string: "FFFFFFFF(line:NNN)"
    // File name relaxed format: "FFFFFFFF(*"
    // Line number relaxed format: "*:NNN?"

    public static StackTrace fromString(String s) {
        return new StackTrace(getFileName(s), getLineNumber(s));
    }

    static String getFileName(String linkText) {
        int index = linkText.lastIndexOf('(');
        return index >= 0 ? linkText.substring(0, index) : null;
    }

    /**
     * Returns zero-bazed line number associated with the stack trace line or
     * negative number if none.
     */
    static int getLineNumber(String s) {
        int index = s.lastIndexOf(':');
        if (index >= 0) {
            String numText = s.substring(index + 1, s.length() - 1);
            try {
                int line = Integer.parseInt(numText);
                // Note: line is one-based in printout.
                return line > 0 ? line - 1 : line;
            } catch (NumberFormatException e) {
            }
        }
        return -1;
    }

    public StackTrace(String fileName, int line) {
        this.fileName = fileName;
        this.line = line;
    }

    @Override
    public String toString() {
        // Note: line is one-based in printout.
        Object[] args = { fileName, new Integer(1 + line), };
        return MessageFormat.format(OPENL_FORMAT, args);
    }

}
