package org.openl.excel.parser.markdown;

import org.openl.exception.OpenlNotCheckedException;

/**
 * A table cannot be written into its Markdown file: its layout has no Markdown form.
 */
public final class MarkdownWriteException extends OpenlNotCheckedException {

    MarkdownWriteException(String message) {
        super(message);
    }
}
