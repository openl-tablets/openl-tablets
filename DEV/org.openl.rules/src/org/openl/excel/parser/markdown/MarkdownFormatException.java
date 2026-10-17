package org.openl.excel.parser.markdown;

import java.io.Serial;

import lombok.Getter;

import org.openl.excel.parser.ExcelParseException;

/**
 * A Markdown file breaks the format so that it holds no table.
 *
 * <p>The message tells what is wrong, in the words an error of a workbook would use. The line of the file where the
 * problem is comes with the problem, apart from the message.
 */
@Getter
public class MarkdownFormatException extends ExcelParseException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The line of the file and what is wrong there. */
    private final transient MarkdownProblem problem;

    MarkdownFormatException(int line, String message) {
        super(message);
        this.problem = new MarkdownProblem(line, message, null);
    }
}
