package org.openl.excel.grid;

import java.io.Serial;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.util.TextRange;
import org.openl.util.text.ILocation;

/**
 * An error of a Markdown file that breaks the format.
 *
 * <p>The error points at what shows the problem, the cell of the table or the line of the file, as any error of a
 * module does. It tells the place of the problem in the file as well, so an editor of the file marks it even when the
 * error points at a cell.
 */
@Getter
public final class MarkdownFormatError extends SyntaxNodeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Where the problem is in the file: the text of its line, the indent left out. */
    private final transient TextRange place;

    MarkdownFormatError(String message,
                        @Nullable ILocation location,
                        IOpenSourceCodeModule source,
                        TextRange place) {
        super(message, null, location, source);
        this.place = place;
    }
}
