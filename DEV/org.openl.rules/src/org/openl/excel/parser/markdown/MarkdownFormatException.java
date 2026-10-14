package org.openl.excel.parser.markdown;

import java.io.Serial;

import org.openl.excel.parser.ExcelParseException;

/**
 * A Markdown rules module breaks the format.
 *
 * <p>The message names the file and the line of the file where the problem is.
 */
public class MarkdownFormatException extends ExcelParseException {

    @Serial
    private static final long serialVersionUID = 1L;

    MarkdownFormatException(String fileName, int line, String message) {
        super("%s, line %d: %s".formatted(fileName, line, message));
    }
}
